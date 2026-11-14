package dev.shephard.player.ui.screens

import org.json.JSONArray

/** Change only the trackIds field, preserving other and future playlist metadata. */
internal fun updatePlaylistMembership(json: String, name: String, createdAt: Long, ids: Collection<Long>, operation: String): String {
    val array = JSONArray(json)
    // Prefer an exact stable identity. Old playlists may lack createdAt; permit that
    // fallback only when the name is unambiguous, without rewriting other metadata.
    var exactIndex = -1
    var legacyIndex = -1
    var nameMatches = 0
    for (i in 0 until array.length()) {
        val item = array.getJSONObject(i) // Fail without writing if storage is malformed.
        if (!item.optBoolean("isSystem", false) && item.optString("name") == name) {
            nameMatches++
            if (item.has("createdAt") && item.optLong("createdAt", -1L) == createdAt && exactIndex < 0) exactIndex = i
            if (!item.has("createdAt")) legacyIndex = i
        }
    }
    val selected = if (exactIndex >= 0) exactIndex else if (nameMatches == 1) legacyIndex else -1
    if (selected >= 0) {
        val item = array.getJSONObject(selected)
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
