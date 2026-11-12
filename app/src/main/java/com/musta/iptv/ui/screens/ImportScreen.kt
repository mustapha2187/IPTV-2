@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.musta.iptv.ui.screens

import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.musta.iptv.R
import com.musta.iptv.data.AddState
import com.musta.iptv.data.BulkParser
import com.musta.iptv.data.SourceManager
import com.musta.iptv.ui.components.rememberInitialFocus
import com.musta.iptv.ui.enigma.Enigma
import com.musta.iptv.ui.enigma.EnigmaFrame
import com.musta.iptv.ui.enigma.Hint
import com.musta.iptv.ui.enigma.TextRow
import com.musta.iptv.ui.theme.Spacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream

private const val MAX_FILE_BYTES = 5 * 1024 * 1024

/** Reads a list of servers from a file on the box or a USB drive, then adds them like the phone TXT import. */
@Composable
fun ImportScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by SourceManager.addState.collectAsState()
    val folder = remember { context.getExternalFilesDir(null) ?: context.filesDir }
    var files by remember { mutableStateOf<List<File>>(emptyList()) }
    var message by remember { mutableStateOf("") }
    val first = rememberInitialFocus()

    val noLines = stringResource(R.string.import_no_lines)
    val noPicker = stringResource(R.string.import_no_picker)
    val failedTemplate = stringResource(R.string.import_failed, "%s")

    fun importFrom(open: () -> InputStream?) {
        scope.launch {
            SourceManager.resetAdd()
            message = ""
            val text = withContext<Pair<String?, String>>(Dispatchers.IO) {
                try {
                    val stream = open() ?: return@withContext null to "Cannot open the file"
                    stream.use { s ->
                        val bytes = s.readNBytesCompat(MAX_FILE_BYTES)
                        String(bytes, Charsets.UTF_8) to ""
                    }
                } catch (e: Exception) {
                    null to (e.message ?: e.javaClass.simpleName)
                }
            }
            val content = text.first
            if (content == null) {
                message = failedTemplate.replace("%s", text.second)
                return@launch
            }
            val result = BulkParser.parse(content)
            if (result.drafts.isEmpty()) {
                message = noLines
            } else {
                SourceManager.addMany(result.drafts, result.skipped)
            }
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) importFrom { context.contentResolver.openInputStream(uri) }
    }

    LaunchedEffect(Unit) {
        files = withContext(Dispatchers.IO) {
            folder.listFiles()?.filter { it.isFile && it.name.endsWith(".txt", ignoreCase = true) }?.sortedBy { it.name }
                ?: emptyList()
        }
    }
    LaunchedEffect(state) {
        val s = state
        if (s is AddState.BulkDone && s.added > 0) {
            delay(2500)
            onFinished()
        }
    }

    EnigmaFrame(
        title = stringResource(R.string.import_title),
        hints = listOf(
            Hint(null, "OK", stringResource(R.string.hint_select)),
            Hint(null, "Back", stringResource(R.string.hint_close)),
        ),
    ) {
        Column {
            TextRow(
                text = stringResource(R.string.import_pick),
                focusRequester = first,
                onClick = {
                    try {
                        picker.launch(arrayOf("text/plain", "text/*", "application/octet-stream"))
                    } catch (e: ActivityNotFoundException) {
                        message = noPicker
                    }
                },
            )
            Spacer(Modifier.height(Spacing.md))
            Text(stringResource(R.string.import_folder), style = MaterialTheme.typography.bodyMedium, color = Enigma.TextDim)
            Text(folder.absolutePath, style = MaterialTheme.typography.bodyMedium, color = Enigma.Yellow)
            Spacer(Modifier.height(Spacing.sm))
            if (files.isEmpty()) {
                Text(stringResource(R.string.import_none), style = MaterialTheme.typography.bodyMedium, color = Enigma.TextDim)
            } else {
                for (file in files) {
                    TextRow(text = file.name, onClick = { importFrom { file.inputStream() } })
                }
            }
            Spacer(Modifier.height(Spacing.md))
            Text(stringResource(R.string.import_format), style = MaterialTheme.typography.bodyMedium, color = Enigma.TextDim)

            val status = when (val s = state) {
                is AddState.Progress -> stringResource(R.string.add_progress, s.done, s.total, s.failed)
                is AddState.BulkDone -> stringResource(R.string.add_bulk_done, s.added, s.failed)
                is AddState.Failed -> stringResource(R.string.add_failed, s.message)
                else -> message
            }
            if (status.isNotEmpty()) {
                Spacer(Modifier.height(Spacing.md))
                Text(status, style = MaterialTheme.typography.titleMedium, color = Enigma.Yellow)
            }
        }
    }
}

/** InputStream.readNBytes needs API 33; this works everywhere. */
private fun InputStream.readNBytesCompat(limit: Int): ByteArray {
    val out = java.io.ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    var total = 0
    while (total < limit) {
        val n = read(buffer, 0, minOf(buffer.size, limit - total))
        if (n < 0) break
        out.write(buffer, 0, n)
        total += n
    }
    return out.toByteArray()
}
