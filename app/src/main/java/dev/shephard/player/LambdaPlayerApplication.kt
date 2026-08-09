package dev.shephard.player

import android.app.Application
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
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
 *
 * MADDE — GIF KAPAK DESTEĞİ: kullanıcı bir şarkının/playlist'in kapağı olarak bir .gif
 * seçerse (örn. cihazdan galeri ile kapak değiştirme), Coil'in varsayılan decoder'ları
 * GIF'i SADECE İLK KAREYİ (statik) çözer, animasyon oynamaz. `ImageDecoderDecoder`
 * (Android 9/API 28+, donanım hızlandırmalı, `Movie`/`AnimatedImageDrawable` kullanır) ve
 * ondan önceki cihazlar için `GifDecoder` (yazılım tabanlı) eklenince, `AsyncImage` bir GIF
 * URI'si aldığında OTOMATİK olarak animasyonlu oynatır — bu değişiklik, Now Playing Sheet'in
 * arka planı, Mini Player'ın kapak resmi ve tüm track/playlist kapak gösterimleri dahil,
 * `AsyncImage` kullanılan HER yerde tek bir merkezi noktadan geçerli olur (başka hiçbir
 * dosyada değişiklik gerekmez, çünkü decoder seçimi ImageLoader seviyesinde yapılır).
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
        .components {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                add(ImageDecoderDecoder.Factory())
            } else {
                add(GifDecoder.Factory())
            }
        }
        .build()
}
