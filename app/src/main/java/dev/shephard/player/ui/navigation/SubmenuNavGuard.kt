package dev.shephard.player.ui.navigation

/**
 * InstallerX Revived'ın `Navigator.push()` / `Navigator.pop()` mantığının birebir portu
 * (`com.rosan.installer.ui.navigation.Navigator`, MiuixNavWrapper'ın kullandığı gerçek
 * navigation sınıfı). Theme/Player/About Settings gibi alt menülere VE playlist detay
 * ekranına (görsel olarak aynı submenu animasyonunu paylaşıyor, ama backStack yerine kendi
 * local state'iyle yönetiliyor) girip çıkarken, bir önceki geçiş animasyonu tam bitmeden
 * ikinci bir navigasyon tetiklenirse eski sahnenin animasyon ortasında (ör. sola kayarak)
 * arka planda asılı kalması / ekranın aşırı sola kayması gibi görsel bozulmalara yol
 * açıyordu. InstallerX bunu ÇÖZMÜYOR, kaynağında ENGELLİYOR: aynı sayfaya tekrar push
 * görmezden gelinir, ve pop çağrıları art arda 100ms içinde gelirse ikincisi yok sayılır.
 *
 * NOT: Bu guard SADECE Theme/Player/About ve playlist detail (submenu tarzı) geçişlere
 * uygulanır. Music/Playlists/Settings ana sekmeleri arasındaki spring tabanlı geçiş
 * (`originalTabTransition`) BUNA DAHİL DEĞİL — kullanıcı o geçişte sorun olmadığını
 * belirtti, dokunulmadı.
 */
class SubmenuNavGuard {
    private var lastPopTimeMs = 0L

    /**
     * Bir alt menüye/detaya giriş isteğini uygular, EĞER zaten aynı hedefte değilsek veya
     * bir önceki pop'un hemen ardından gelmiyorsa. [currentKey] hedefle eşitse (zaten o
     * sayfadaysak) ya da çok yakın zamanda bir pop olduysa `push` yok sayılır — InstallerX'in
     * `backStack.lastOrNull() == key` kontrolünün birebir eşdeğeri.
     *
     * @param currentKey Şu an gösterilen sayfayı temsil eden değer (null = liste/ana görünüm).
     * @param targetKey Girilmek istenen sayfayı temsil eden değer.
     * @param onPush Guard geçerse çağrılır; gerçek state/backStack mutasyonunu burada yap.
     */
    fun <T> push(currentKey: T, targetKey: T, onPush: () -> Unit) {
        if (currentKey == targetKey) return
        val now = System.currentTimeMillis()
        if (now - lastPopTimeMs < 100) return
        onPush()
    }

    /**
     * Bir alt menüden/detaydan geri dönüş isteğini uygular — art arda 100ms içinde birden
     * fazla pop çağrısı gelirse yalnızca ilki işlenir. InstallerX'teki `lastPopTime` debounce
     * mantığının birebir eşdeğeri; "animasyon bitmeden hızlıca geri + yeni sayfaya giriş"
     * kombinasyonunda eski sahnenin yarım kalmış animasyonla arka planda takılı kalmasını
     * (ya da ekranın aşırı sola kaymasını) önler.
     */
    fun pop(onPop: () -> Unit) {
        val now = System.currentTimeMillis()
        if (now - lastPopTimeMs < 100) return
        lastPopTimeMs = now
        onPop()
    }
}
