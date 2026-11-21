package com.example.data.catalog

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object TurfPhotoCompressor {
  private const val MAX_DIMENSION_PX = 720
  private const val TARGET_MAX_BYTES = 72_000
  private const val DATA_URI_PREFIX = "data:image/jpeg;base64,"

  suspend fun compressUriToDataUri(context: Context, uri: Uri): String? =
    withContext(Dispatchers.IO) {
      runCatching {
        val resolver = context.contentResolver
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { stream ->
          BitmapFactory.decodeStream(stream, null, boundsOpts)
        }
        val origW = boundsOpts.outWidth
        val origH = boundsOpts.outHeight
        if (origW <= 0 || origH <= 0) return@runCatching null

        var sampleSize = 1
        val maxOrig = maxOf(origW, origH)
        while (maxOrig / (sampleSize * 2) >= MAX_DIMENSION_PX) {
          sampleSize *= 2
        }

        val decodeOpts =
          BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.RGB_565
          }
        val decoded =
          resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOpts)
          } ?: return@runCatching null

        val scaled = scaleDownIfNeeded(decoded)
        if (scaled !== decoded) decoded.recycle()

        val baos = ByteArrayOutputStream()
        var quality = 72
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, baos)
        while (baos.size() > TARGET_MAX_BYTES && quality > 30) {
          baos.reset()
          quality -= 10
          scaled.compress(Bitmap.CompressFormat.JPEG, quality, baos)
        }
        scaled.recycle()

        val bytes = baos.toByteArray()
        DATA_URI_PREFIX + Base64.encodeToString(bytes, Base64.NO_WRAP)
      }.getOrNull()
    }

  private fun scaleDownIfNeeded(source: Bitmap): Bitmap {
    val w = source.width
    val h = source.height
    val maxSide = maxOf(w, h)
    if (maxSide <= MAX_DIMENSION_PX) return source
    val scale = MAX_DIMENSION_PX.toFloat() / maxSide.toFloat()
    val targetW = (w * scale).toInt().coerceAtLeast(1)
    val targetH = (h * scale).toInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(source, targetW, targetH, true)
  }

  fun decodeDataUriToImageBitmap(value: String): ImageBitmap? {
    val commaIdx = value.indexOf(',')
    if (!value.startsWith("data:image/") || commaIdx < 0) return null
    val base64Part = value.substring(commaIdx + 1)
    return runCatching {
      val bytes = Base64.decode(base64Part, Base64.DEFAULT)
      BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
    }.getOrNull()
  }
}
