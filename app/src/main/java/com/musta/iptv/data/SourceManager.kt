package com.musta.iptv.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

sealed interface AddState {
    object Idle : AddState
    data class Loading(val name: String) : AddState
    data class Done(val sourceId: Long, val count: Int) : AddState
    data class Failed(val message: String) : AddState
    /** Many sources at once (TXT list). */
    data class Progress(val done: Int, val total: Int, val failed: Int) : AddState
    data class BulkDone(val added: Int, val failed: Int) : AddState
}

/** Single entry point for adding, refreshing, deleting and playing sources. */
object SourceManager {
    lateinit var db: Db
        private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val insertLock = Any()
    private val stalkerClients = ConcurrentHashMap<Long, StalkerClient>()

    val addState = MutableStateFlow<AddState>(AddState.Idle)
    /** Bumped whenever sources change, so screens reload. */
    val version = MutableStateFlow(0)

    fun init(context: Context) {
        if (!::db.isInitialized) db = Db(context)
    }

    fun resetAdd() {
        addState.value = AddState.Idle
    }

    private fun bump() {
        version.value = version.value + 1
    }

    /** Saves and loads one source. Returns the channel count, or throws. A failed source is not kept. */
    private fun addOne(draft: SourceDraft): Pair<Long, Int> {
        var id = -1L
        try {
            synchronized(insertLock) {
                val limit = Limits.forType(draft.type)
                if (db.countByType(draft.type) >= limit) throw IOException("Limit reached: $limit servers of this type")
                id = db.insertSource(draft)
            }
            val source = db.getSource(id) ?: throw IOException("The source could not be saved")
            return id to SourceLoader.load(db, source)
        } catch (e: Exception) {
            if (id != -1L) db.deleteSource(id)
            throw e
        }
    }

    fun add(draft: SourceDraft) {
        addState.value = AddState.Loading(draft.name)
        scope.launch {
            try {
                val (id, count) = addOne(draft)
                addState.value = AddState.Done(id, count)
            } catch (e: Exception) {
                addState.value = AddState.Failed(e.message ?: e.javaClass.simpleName)
            }
            bump()
        }
    }

    /** Adds many sources from a list, three at a time. Sources that fail are skipped and counted. */
    fun addMany(drafts: List<SourceDraft>, skippedLines: Int = 0) {
        if (drafts.isEmpty()) {
            addState.value = AddState.Failed("No valid lines were found")
            return
        }
        addState.value = AddState.Progress(0, drafts.size, 0)
        scope.launch {
            val permits = Semaphore(3)
            val finished = AtomicInteger(0)
            val failed = AtomicInteger(0)
            coroutineScope {
                drafts.map { draft ->
                    async {
                        permits.withPermit {
                            try {
                                addOne(draft)
                            } catch (e: Exception) {
                                failed.incrementAndGet()
                            }
                            val n = finished.incrementAndGet()
                            addState.value = AddState.Progress(n, drafts.size, failed.get())
                            bump()
                        }
                    }
                }.awaitAll()
            }
            addState.value = AddState.BulkDone(drafts.size - failed.get(), failed.get() + skippedLines)
            bump()
        }
    }

    fun refresh(sourceId: Long) {
        scope.launch {
            val source = db.getSource(sourceId) ?: return@launch
            addState.value = AddState.Loading(source.name)
            try {
                val count = SourceLoader.load(db, source)
                stalkerClients.remove(sourceId)
                addState.value = AddState.Done(sourceId, count)
            } catch (e: Exception) {
                addState.value = AddState.Failed(e.message ?: e.javaClass.simpleName)
            }
            bump()
        }
    }

    fun delete(sourceId: Long) {
        scope.launch {
            stalkerClients.remove(sourceId)
            db.deleteSource(sourceId)
            bump()
        }
    }

    /** Turns a channel into a playable URL. Blocking: call from a background thread. */
    fun resolve(channel: Channel): String {
        val source = db.getSource(channel.sourceId) ?: throw IOException("This source no longer exists")
        if (source.type != SourceType.STALKER) return channel.streamUrl
        val client = stalkerClients.getOrPut(source.id) { StalkerClient(source.url, source.mac) }
        return client.createLink(channel.cmd)
    }
}
