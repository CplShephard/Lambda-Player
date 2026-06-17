package dev.shephard.player.player

enum class RepeatMode {
    OFF,
    ONE,
    ALL
}

fun RepeatMode.next(): RepeatMode = when (this) {
    RepeatMode.OFF -> RepeatMode.ALL
    RepeatMode.ALL -> RepeatMode.ONE
    RepeatMode.ONE -> RepeatMode.OFF
}
