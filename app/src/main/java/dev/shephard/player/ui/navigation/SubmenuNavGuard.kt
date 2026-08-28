package dev.shephard.player.ui.navigation

class SubmenuNavGuard {
    private var lastPopTimeMs = 0L

    fun <T> push(currentKey: T, targetKey: T, onPush: () -> Unit) {
        if (currentKey == targetKey) return
        val now = System.currentTimeMillis()
        if (now - lastPopTimeMs < 100) return
        onPush()
    }

    fun pop(onPop: () -> Unit) {
        val now = System.currentTimeMillis()
        if (now - lastPopTimeMs < 100) return
        lastPopTimeMs = now
        onPop()
    }
}
