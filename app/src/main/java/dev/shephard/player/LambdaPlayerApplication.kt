package dev.shephard.player

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy

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
 *
 * MADDE — Cover DISK CACHE: Coil varsayılan olarak yalnızca bellek cache'i kullanır; disk
 * cache'i açık değildir. Bu yüzden uygulama her yeniden başladığında (yeni process) cover'lar
 * bellekten düşer ve albüm kapağı MediaStore content-uri'den YENİDEN çözülüp yeniden
 * okunuyordu. Disk cache eklenince cover'lar ilk yüklemeden sonra diske yazılır ve sonraki
 * açılışlarda diskten anında gelir (ağ/kaynak isteği tekrarlanmaz).
 */
class LambdaPlayerApplication : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .crossfade(true)
        .crossfade(200)
        // Bellek cache'i: uygulamanın kullanılabilir belleğinin %25'i.
        .memoryCache {
            MemoryCache.Builder(this)
                .maxSizePercent(0.25)
                .build()
        }
        // Disk cache'i: cover'ların uygulama yeniden başlasa da diskten gelmesi için.
        .diskCache {
            DiskCache.Builder()
                .directory(cacheDir.resolve("image_cache"))
                .maxSizePercent(0.02) // cihaz depolamasının %2'si (default)
                .build()
        }
        // Her iki cache de tam kullanılsın (okuma + yazma).
        .memoryCachePolicy(CachePolicy.ENABLED)
        .diskCachePolicy(CachePolicy.ENABLED)
        .build()
}
