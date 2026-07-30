package dev.shephard.player

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory

/**
 * Uygulama genelinde tek bir Coil [ImageLoader] sağlar.
 *
 * Coil'in varsayılan davranışında crossfade KAPALI: yeni bir görsel (örn. şarkı değişince
 * albüm kapağı) her yüklendiğinde eski görsel aniden kaybolup yenisi aniden beliriyor —
 * cache'te değilse aradaki karede hiçbir şey (arkadaki rengin/siyahın) görünüyor. Bu hem
 * "şarkı geçişinde üstte siyah bir şey oluyor" hissinin hem de geçişin "laglı" görünmesinin
 * ana kaynağıydı. crossfade(true) ile Coil, eski görselden yeniye 200ms'lik yumuşak bir
 * fade yapıyor; ayrıca eldeki bitmap'i (rotasyon/resize sırasında) bellekte tutmak için
 * uygulamanın kullanılabilir belleğinin %25'ini ayırıyoruz.
 */
class LambdaPlayerApplication : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .crossfade(true)
        .crossfade(200)
        .build()
}
