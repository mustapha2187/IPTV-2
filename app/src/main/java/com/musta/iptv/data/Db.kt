package com.musta.iptv.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/** Local database. Plain SQLite on purpose: no code generation, nothing to break the build. */
class Db(context: Context) : SQLiteOpenHelper(context.applicationContext, "musta.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "create table source (id integer primary key autoincrement, type text not null, name text not null, " +
                "url text not null, username text not null default '', password text not null default '', " +
                "mac text not null default '', loaded_at integer not null default 0)"
        )
        db.execSQL("create table category (id integer primary key autoincrement, source_id integer not null, name text not null)")
        db.execSQL("create index idx_category_source on category(source_id)")
        db.execSQL(
            "create table channel (id integer primary key autoincrement, source_id integer not null, " +
                "category_id integer not null, number integer not null, name text not null, " +
                "stream_url text not null default '', logo_url text not null default '', " +
                "tvg_id text not null default '', cmd text not null default '', fav_key text not null default '')"
        )
        db.execSQL("create index idx_channel_source on channel(source_id, category_id, number)")
        db.execSQL(
            "create table favorite (source_id integer not null, fav_key text not null, primary key (source_id, fav_key))"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    fun insertSource(d: SourceDraft): Long {
        val v = ContentValues().apply {
            put("type", d.type.key)
            put("name", d.name)
            put("url", d.url)
            put("username", d.username)
            put("password", d.password)
            put("mac", d.mac)
        }
        return writableDatabase.insertOrThrow("source", null, v)
    }

    fun listSources(onlyId: Long? = null): List<Source> {
        val out = ArrayList<Source>()
        readableDatabase.rawQuery(
            "select s.id, s.type, s.name, s.url, s.username, s.password, s.mac, s.loaded_at, " +
                "(select count(*) from channel c where c.source_id = s.id) from source s" +
                (if (onlyId != null) " where s.id = $onlyId" else "") + " order by s.id",
            null
        ).use { c ->
            while (c.moveToNext()) {
                val type = SourceType.from(c.getString(1)) ?: continue
                out.add(
                    Source(
                        id = c.getLong(0), type = type, name = c.getString(2), url = c.getString(3),
                        username = c.getString(4), password = c.getString(5), mac = c.getString(6),
                        loadedAt = c.getLong(7), channelCount = c.getInt(8),
                    )
                )
            }
        }
        return out
    }

    fun countByType(type: SourceType): Int =
        readableDatabase.rawQuery("select count(*) from source where type = ?", arrayOf(type.key)).use { c ->
            if (c.moveToFirst()) c.getInt(0) else 0
        }

    fun getSource(id: Long): Source? = listSources(id).firstOrNull()

    fun deleteSource(id: Long) {
        val db = writableDatabase
        val args = arrayOf(id.toString())
        db.beginTransaction()
        try {
            db.delete("channel", "source_id=?", args)
            db.delete("category", "source_id=?", args)
            db.delete("favorite", "source_id=?", args)
            db.delete("source", "id=?", args)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /** Replaces every channel of a source in one transaction. Favorites survive because they are keyed by stream, not by row id. */
    fun replaceChannels(sourceId: Long, rows: List<ChannelRow>): Int {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val args = arrayOf(sourceId.toString())
            db.delete("channel", "source_id=?", args)
            db.delete("category", "source_id=?", args)
            val insertCategory = db.compileStatement("insert into category(source_id, name) values (?, ?)")
            val insertChannel = db.compileStatement(
                "insert into channel(source_id, category_id, number, name, stream_url, logo_url, tvg_id, cmd, fav_key) " +
                    "values (?, ?, ?, ?, ?, ?, ?, ?, ?)"
            )
            val categoryIds = HashMap<String, Long>()
            for (r in rows) {
                val categoryId = categoryIds.getOrPut(r.categoryName) {
                    insertCategory.clearBindings()
                    insertCategory.bindLong(1, sourceId)
                    insertCategory.bindString(2, r.categoryName)
                    insertCategory.executeInsert()
                }
                insertChannel.clearBindings()
                insertChannel.bindLong(1, sourceId)
                insertChannel.bindLong(2, categoryId)
                insertChannel.bindLong(3, r.number.toLong())
                insertChannel.bindString(4, r.name)
                insertChannel.bindString(5, r.streamUrl)
                insertChannel.bindString(6, r.logoUrl)
                insertChannel.bindString(7, r.tvgId)
                insertChannel.bindString(8, r.cmd)
                insertChannel.bindString(9, if (r.cmd.isNotEmpty()) r.cmd else r.streamUrl)
                insertChannel.executeInsert()
            }
            db.execSQL("update source set loaded_at = ? where id = ?", arrayOf<Any>(System.currentTimeMillis(), sourceId))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return rows.size
    }

    fun categories(sourceId: Long): List<Category> {
        val out = ArrayList<Category>()
        readableDatabase.rawQuery(
            "select id, name from category where source_id = ? order by id", arrayOf(sourceId.toString())
        ).use { c ->
            while (c.moveToNext()) out.add(Category(c.getLong(0), sourceId, c.getString(1)))
        }
        return out
    }

    /** categoryId: [Cat.ALL], [Cat.FAVORITES], or a real category id. */
    fun channels(sourceId: Long, categoryId: Long): List<Channel> {
        val sql = StringBuilder(
            "select c.id, c.source_id, c.category_id, c.number, c.name, c.stream_url, c.logo_url, c.cmd, " +
                "(f.fav_key is not null) from channel c " +
                "left join favorite f on f.source_id = c.source_id and f.fav_key = c.fav_key where c.source_id = ?"
        )
        val args = arrayListOf(sourceId.toString())
        when (categoryId) {
            Cat.ALL -> Unit
            Cat.FAVORITES -> sql.append(" and f.fav_key is not null")
            else -> {
                sql.append(" and c.category_id = ?")
                args.add(categoryId.toString())
            }
        }
        sql.append(" order by c.number, c.id")
        val out = ArrayList<Channel>()
        readableDatabase.rawQuery(sql.toString(), args.toTypedArray()).use { c ->
            while (c.moveToNext()) {
                out.add(
                    Channel(
                        id = c.getLong(0), sourceId = c.getLong(1), categoryId = c.getLong(2), number = c.getInt(3),
                        name = c.getString(4), streamUrl = c.getString(5), logoUrl = c.getString(6),
                        cmd = c.getString(7), isFavorite = c.getInt(8) == 1,
                    )
                )
            }
        }
        return out
    }

    fun toggleFavorite(channelId: Long) {
        val db = writableDatabase
        db.rawQuery("select source_id, fav_key from channel where id = ?", arrayOf(channelId.toString())).use { c ->
            if (!c.moveToFirst()) return
            val sourceId = c.getLong(0)
            val key = c.getString(1)
            val removed = db.delete("favorite", "source_id = ? and fav_key = ?", arrayOf(sourceId.toString(), key))
            if (removed == 0) {
                val v = ContentValues().apply {
                    put("source_id", sourceId)
                    put("fav_key", key)
                }
                db.insertWithOnConflict("favorite", null, v, SQLiteDatabase.CONFLICT_IGNORE)
            }
        }
    }
}
