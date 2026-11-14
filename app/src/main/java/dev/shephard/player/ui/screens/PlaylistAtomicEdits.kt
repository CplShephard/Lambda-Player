package dev.shephard.player.ui.screens

import org.json.JSONArray

/** Change only the trackIds field, preserving other and future playlist metadata. */
internal fun updatePlaylistMembership(json: String, name: String, createdAt: Long, ids: Collection<Long>, operation: String): String {
    val array = JSONArray(json)
    for (i in 0 until array.length()) {
        val item = array.getJSONObject(i) // Fail without writing if storage is malformed.
        if (!item.optBoolean("isSystem", false) && item.optString("name") == name && item.optLong("createdAt", -1L) == createdAt) {
            val source = item.optJSONArray("trackIds") ?: JSONArray()
            val tracks = LinkedHashSet<Long>()
            for (j in 0 until source.length()) tracks.add(source.getLong(j))
            when (operation) {
                "toggle" -> ids.forEach { if (!tracks.add(it)) tracks.remove(it) }
                "remove" -> tracks.removeAll(ids.toSet())
                else -> tracks.addAll(ids)
            }
            val result = JSONArray(); tracks.forEach { result.put(it) }
            item.put("trackIds", result)
            break
        }
    }
    return array.toString()
}

internal fun appendPlaylist(json: String, name: String, ids: List<Long>): String {
    val array = JSONArray(json)
    // Validate existing entries before changing anything.
    for (i in 0 until array.length()) array.getJSONObject(i)
    array.put(JSONArray(encodePlaylists(listOf(LocalPlaylist(name, ids)))).getJSONObject(0))
    return array.toString()
}
