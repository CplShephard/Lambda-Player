package dev.shephard.player.ui.glass.bgeffect

/**
 * MADDE 9 — dinamik ışığın renk/konum ön ayarları.
 *
 * InstallerX'in `BgEffectConfig`'i ile aynı yapı (4 ışık noktası, 3 renk kümesi arasında
 * gidip gelen animasyon), TEK FARK renk paleti:
 *
 *   InstallerX  : morumsu / lacivert-mavimsi tonlar (pembe-mor-mavi)
 *   Lambda      : YEŞİLİMSİ / AÇIK MAVİMSİ tonlar (nane, çimen yeşili, camgöbeği,
 *                 açık gökyüzü mavisi) — uygulamanın lambda yeşili kimliğiyle uyumlu.
 *
 * Her renk kümesi 4 × RGBA (toplam 16 float). `colors1/2/3` arasında yumuşakça
 * geçilerek "canlı" bir arka plan elde ediliyor.
 */
internal object BgEffectConfig {

    class Config(
        val points: FloatArray,
        val colors1: FloatArray,
        val colors2: FloatArray,
        val colors3: FloatArray,
        val colorInterpPeriod: Float,
        val lightOffset: Float,
        val saturateOffset: Float,
        val pointOffset: Float,
    )

    // Dört ışık noktasının (x, y, yarıçap) yerleşimi — InstallerX ile birebir aynı.
    private val POINTS = floatArrayOf(
        0.8f, 0.2f, 1.0f,
        0.8f, 0.9f, 1.0f,
        0.2f, 0.9f, 1.0f,
        0.2f, 0.2f, 1.0f,
    )

    // ---- AÇIK TEMA: pastel nane / açık mavi / limon yeşili / camgöbeği --------------
    private val PHONE_LIGHT = Config(
        points = POINTS,
        colors1 = floatArrayOf(
            0.72f, 0.96f, 0.85f, 1.0f, // nane yeşili
            0.80f, 0.94f, 1.00f, 1.0f, // açık gökyüzü mavisi
            0.88f, 0.99f, 0.83f, 1.0f, // açık limon yeşili
            0.68f, 0.93f, 0.96f, 1.0f, // camgöbeği
        ),
        colors2 = floatArrayOf(
            0.80f, 0.94f, 1.00f, 1.0f,
            0.88f, 0.99f, 0.83f, 1.0f,
            0.68f, 0.93f, 0.96f, 1.0f,
            0.72f, 0.96f, 0.85f, 1.0f,
        ),
        colors3 = floatArrayOf(
            0.68f, 0.93f, 0.96f, 1.0f,
            0.72f, 0.96f, 0.85f, 1.0f,
            0.80f, 0.94f, 1.00f, 1.0f,
            0.88f, 0.99f, 0.83f, 1.0f,
        ),
        colorInterpPeriod = 5.0f,
        lightOffset = 0.1f,
        saturateOffset = 0.2f,
        pointOffset = 0.2f,
    )

    // ---- KOYU TEMA: zümrüt / okyanus mavisi / teal / orman yeşili -------------------
    private val PHONE_DARK = Config(
        points = POINTS,
        colors1 = floatArrayOf(
            0.05f, 0.62f, 0.38f, 0.50f, // zümrüt yeşili
            0.03f, 0.42f, 0.62f, 0.50f, // okyanus mavisi
            0.08f, 0.60f, 0.58f, 0.50f, // teal
            0.10f, 0.45f, 0.28f, 0.40f, // koyu orman yeşili
        ),
        colors2 = floatArrayOf(
            0.03f, 0.42f, 0.62f, 0.50f,
            0.08f, 0.60f, 0.58f, 0.50f,
            0.10f, 0.45f, 0.28f, 0.45f,
            0.05f, 0.62f, 0.38f, 0.50f,
        ),
        colors3 = floatArrayOf(
            0.08f, 0.60f, 0.58f, 0.50f,
            0.05f, 0.62f, 0.38f, 0.50f,
            0.03f, 0.42f, 0.62f, 0.55f,
            0.12f, 0.52f, 0.46f, 0.45f,
        ),
        colorInterpPeriod = 8.0f,
        lightOffset = 0.0f,
        saturateOffset = 0.17f,
        pointOffset = 0.4f,
    )

    fun get(isDark: Boolean): Config = if (isDark) PHONE_DARK else PHONE_LIGHT
}
