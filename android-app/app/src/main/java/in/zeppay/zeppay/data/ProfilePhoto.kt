package `in`.zeppay.zeppay.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import java.io.File

/**
 * The profile photo: copied from the system photo picker into ZepPay's private files, shrunk
 * to at most [MAX] px. No storage permission: the picker grants read access to the one image.
 */
object ProfilePhoto {
    private const val MAX = 512
    private fun file(ctx: Context) = File(ctx.filesDir, "profile_photo.jpg")

    /** Copies the picked image in. Returns a new version stamp, or null if it couldn't be read. */
    fun save(ctx: Context, uri: Uri): String? = try {
        val bmp: Bitmap = if (Build.VERSION.SDK_INT >= 28) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(ctx.contentResolver, uri)) { d, info, _ ->
                val s = info.size; val k = maxOf(1f, maxOf(s.width, s.height) / MAX.toFloat())
                d.setTargetSize((s.width / k).toInt().coerceAtLeast(1), (s.height / k).toInt().coerceAtLeast(1))
                d.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX) sample *= 2
            ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) }
                ?: error("unreadable")
        }
        val tmp = File(ctx.filesDir, "profile_photo.tmp")
        tmp.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 88, it) }
        if (!tmp.renameTo(file(ctx))) { tmp.copyTo(file(ctx), overwrite = true); tmp.delete() }
        System.currentTimeMillis().toString(36)
    } catch (_: Exception) { null }

    fun load(ctx: Context): Bitmap? = file(ctx).takeIf { it.exists() }?.let { BitmapFactory.decodeFile(it.path) }

    fun delete(ctx: Context) { file(ctx).delete() }
}
