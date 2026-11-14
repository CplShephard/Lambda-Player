package dev.shephard.player.data

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore

object MediaStoreScanner {

    fun queryAudioTracks(context: Context): List<AudioTrack> {
        val tracks = mutableListOf<AudioTrack>()

        val collection = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION
        )

        // Optional columns are read only when supported; OEM providers may reject
        // a projection, so retry the original safe projection below.
        val metadataProjection = projection + arrayOf(
            MediaStore.Audio.Media.ARTIST_ID,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATA
        )
        val extendedProjection = metadataProjection +
            (if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) arrayOf(MediaStore.Audio.Media.RELATIVE_PATH) else emptyArray()) +
            (if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) arrayOf(MediaStore.Audio.Media.GENRE) else emptyArray())
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} > 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        val cursor = try {
            context.contentResolver.query(collection, extendedProjection, selection, null, sortOrder)
        } catch (_: IllegalArgumentException) {
            try { context.contentResolver.query(collection, metadataProjection, selection, null, sortOrder) }
            catch (_: IllegalArgumentException) { context.contentResolver.query(collection, projection, selection, null, sortOrder) }
        } ?: throw IllegalStateException("MediaStore returned no cursor")

        cursor.use {
            val idCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val artistIdCol = it.getColumnIndex(MediaStore.Audio.Media.ARTIST_ID)
            val dateAddedCol = it.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)
            val dataCol = it.getColumnIndex(MediaStore.Audio.Media.DATA)
            val genreCol = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R)
                it.getColumnIndex(MediaStore.Audio.Media.GENRE) else -1
            val relativePathCol = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q)
                it.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH) else -1

            while (it.moveToNext()) {
                val id = it.getLong(idCol)
                val title = it.getString(titleCol) ?: "Unknown Title"
                val artist = it.getString(artistCol) ?: "Unknown Artist"
                val album = it.getString(albumCol) ?: "Unknown Album"
                val albumId = it.getLong(albumIdCol)
                val duration = it.getLong(durationCol)

                val contentUri = ContentUris.withAppendedId(collection, id)
                val albumArtUri = ContentUris.withAppendedId(
                    android.net.Uri.parse("content://media/external/audio/albumart"),
                    albumId
                )

                tracks.add(
                    AudioTrack(
                        id = id,
                        title = title,
                        artist = artist,
                        album = album,
                        durationMs = duration,
                        uri = contentUri,
                        albumArtUri = albumArtUri,
                        albumId = albumId.takeIf { id -> id > 0 },
                        artistId = if (artistIdCol >= 0 && !it.isNull(artistIdCol)) it.getLong(artistIdCol).takeIf { id -> id > 0 } else null,
                        genre = if (genreCol >= 0) it.getString(genreCol)?.takeIf(String::isNotBlank) else null,
                        folder = (if (dataCol >= 0) it.getString(dataCol)?.substringBeforeLast('/', "")?.takeIf(String::isNotBlank) else null)
                            ?: (if (relativePathCol >= 0) it.getString(relativePathCol)?.trimEnd('/')?.takeIf(String::isNotBlank) else null),
                        dateAdded = if (dateAddedCol >= 0 && !it.isNull(dateAddedCol)) it.getLong(dateAddedCol) else 0L
                    )
                )
            }
        }

        if (tracks.any { it.genre.isNullOrBlank() }) {
            val genreById = mutableMapOf<Long, String>()
            try {
                val resolver = context.contentResolver
                resolver.query(MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI,
                    arrayOf(MediaStore.Audio.Genres._ID, MediaStore.Audio.Genres.NAME), null, null, null)?.use { genres ->
                    while (genres.moveToNext()) {
                        val id = genres.getLong(0)
                        val name = genres.getString(1)?.takeIf(String::isNotBlank) ?: continue
                        val members = MediaStore.Audio.Genres.Members.getContentUri("external", id)
                        resolver.query(members, arrayOf(MediaStore.Audio.Genres.Members.AUDIO_ID), null, null, null)?.use { cursor ->
                            while (cursor.moveToNext()) genreById.putIfAbsent(cursor.getLong(0), name)
                        }
                    }
                }
            } catch (_: Exception) { /* Not all scoped-storage providers expose genre members. */ }
            if (genreById.isNotEmpty()) return tracks.map { track ->
                if (track.genre.isNullOrBlank()) track.copy(genre = genreById[track.id]) else track
            }
        }
        return tracks
    }
}
