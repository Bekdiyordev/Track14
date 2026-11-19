package uz.beko404.track14.core.device

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uz.beko404.track14.domain.model.PlayLinks
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** What the add-app form can take from an APK the owner picked. */
data class ApkInfo(val name: String, val packageName: String, val versionName: String)

sealed interface ApkReadResult {
    data class Ok(val info: ApkInfo) : ApkReadResult
    data object TooLarge : ApkReadResult
    data object Unreadable : ApkReadResult
}

/**
 * Reads the label, package and version from a picked APK, the way Android's installer does,
 * without installing or running anything. The file is copied to the cache only for the
 * parser (it needs a path) and deleted right after. Nothing is uploaded.
 */
@Singleton
class ApkReader @Inject constructor(@ApplicationContext private val context: Context) {

    suspend fun read(uri: Uri): ApkReadResult = withContext(Dispatchers.IO) {
        val size = sizeOf(uri)
        if (size != null && size > MAX_BYTES) return@withContext ApkReadResult.TooLarge
        val dir = File(context.cacheDir, "apk").apply { mkdirs() }
        val file = File(dir, "picked.apk")
        try {
            val copied = context.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output -> copyLimited(input, output) }
            } ?: return@withContext ApkReadResult.Unreadable
            if (!copied) return@withContext ApkReadResult.TooLarge
            val info = parse(file) ?: return@withContext ApkReadResult.Unreadable
            ApkReadResult.Ok(info)
        } catch (e: Exception) {
            ApkReadResult.Unreadable
        } finally {
            file.delete()
        }
    }

    private fun parse(file: File): ApkInfo? {
        val pm = context.packageManager
        val pkg: PackageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageArchiveInfo(file.path, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageArchiveInfo(file.path, 0)
        } ?: return null
        val packageName = pkg.packageName?.takeIf(PlayLinks::isValidPackageName) ?: return null
        val app = pkg.applicationInfo
        val label = app?.let {
            // The parser needs the archive path to resolve the label resource.
            it.sourceDir = file.path
            it.publicSourceDir = file.path
            it.loadLabel(pm).toString()
        }.orEmpty()
        return ApkInfo(
            name = label.takeIf { it.isNotBlank() && it != packageName }.orEmpty().take(60),
            packageName = packageName,
            versionName = pkg.versionName.orEmpty().take(40),
        )
    }

    private fun sizeOf(uri: Uri): Long? = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { c ->
            if (c.moveToFirst() && !c.isNull(0)) c.getLong(0) else null
        }
    }.getOrNull()

    /** False when the stream is bigger than [MAX_BYTES] (size column can be missing or wrong). */
    private fun copyLimited(input: java.io.InputStream, output: java.io.OutputStream): Boolean {
        val buffer = ByteArray(64 * 1024)
        var total = 0L
        while (true) {
            val n = input.read(buffer)
            if (n < 0) return true
            total += n
            if (total > MAX_BYTES) return false
            output.write(buffer, 0, n)
        }
    }

    private companion object {
        const val MAX_BYTES = 300L * 1024 * 1024
    }
}
