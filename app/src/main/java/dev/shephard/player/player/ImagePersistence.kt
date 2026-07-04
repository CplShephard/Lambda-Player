package dev.shephard.player.player

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * Kullanıcının seçtiği (veya kırptığı) görselleri -- playlist kapakları, müzik kapak
 * override'ları, wallpaper -- uygulamanın KENDİ kalıcı depolamasına kopyalar.
 *
 * ÖNEMLİ: Daha önce bu görseller sadece seçilen `content://` URI'sine (ve
 * `takePersistableUriPermission` ile alınan kalıcı izne) güveniyordu. Bu, metin tercihleri
 * gibi kalıcı olması gerekirken bazı cihaz/galeri/dosya yöneticisi kombinasyonlarında
 * (özellikle bazı OEM galeri uygulamaları uygulama yeniden başlatıldığında ya da OS
 * güncellemesinden sonra verdiği izni geçersiz kılabiliyor, ya da paylaşılan albüm/bulut
 * senkronizasyonlu galerilerde kaynak dosya taşınabiliyor) görselin sessizce kaybolmasına
 * ("resim yok" haline dönmesine) yol açıyordu. Kalıcı metin tercihleri gibi davranması için
 * dosyayı DOĞRUDAN uygulamanın `filesDir` altına (cache değil, kalıcı depolama) kopyalayıp
 * DataStore'a bizim kendi `content://` (FileProvider) URI'mizi yazıyoruz -- kaynak URI'ye
 * bağımlılığımız kalmıyor.
 */
object ImagePersistence {

    private const val COVERS_DIR = "persisted_covers"
    private const val WALLPAPER_DIR = "persisted_wallpaper"

    /**
     * Verilen kaynak URI'nin (bir galeri/dosya yöneticisi seçiminden ya da sistem
     * cropper'ının çıktısından gelebilir) baytlarını uygulamanın kalıcı `filesDir` alanına
     * kopyalar ve bu kopyayı işaret eden kalıcı bir `content://` (FileProvider) URI döner.
     *
     * @param subDir hangi kalıcı alt klasöre kaydedileceği (kapaklar vs wallpaper için ayrı)
     * @param fileNamePrefix dosya adının başına eklenecek önek (ör. "cover_", "wallpaper_")
     * @return kalıcı FileProvider URI'si, kopyalama başarısız olursa null
     */
    suspend fun persistImage(
        context: Context,
        sourceUri: Uri,
        subDir: String,
        fileNamePrefix: String
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            val dir = File(context.filesDir, subDir).apply { mkdirs() }
            val destFile = File(dir, "${fileNamePrefix}${System.currentTimeMillis()}.jpg")

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext null

            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", destFile)
        } catch (_: IOException) {
            null
        } catch (_: SecurityException) {
            null
        }
    }

    /** Müzik/playlist kapak resimleri için [persistImage] kısayolu. */
    suspend fun persistCover(context: Context, sourceUri: Uri): Uri? =
        persistImage(context, sourceUri, COVERS_DIR, "cover_")

    /** Wallpaper için [persistImage] kısayolu. */
    suspend fun persistWallpaper(context: Context, sourceUri: Uri): Uri? =
        persistImage(context, sourceUri, WALLPAPER_DIR, "wallpaper_")
}
