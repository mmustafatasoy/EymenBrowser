package com.eymen.browser.telefon

import android.Manifest
import android.app.AlertDialog
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.eymen.browser.motor.GeckoMotorDenemesi
import java.io.File

/** Capture one QR image using Android's camera; decode locally and erase the temporary photo. */
internal class BrowserQrScanner(private val activity: ComponentActivity, private val onOpen: (String) -> Unit,
    private val notice: (Int) -> Unit) {
    private var photo: File? = null
    private var photoUri: Uri? = null
    private val capture = activity.registerForActivityResult(object : ActivityResultContracts.TakePicture() {
        override fun createIntent(context: Context, input: Uri): Intent = super.createIntent(context, input).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            clipData = ClipData.newRawUri("QR", input)
        }
    }) { ok ->
        val file = photo
        photoUri?.let { activity.revokeUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION) }
        photoUri = null
        if (!ok || file == null) { file?.delete(); photo = null }
        else Thread {
            val raw = try { decode(file) } catch (_: Exception) { null } catch (_: OutOfMemoryError) { null }
            finally { file.delete(); photo = null }
            activity.runOnUiThread {
                if (activity.isFinishing || activity.isDestroyed) return@runOnUiThread
                val url = GeckoMotorDenemesi.verifiedHttpsUrl(raw)?.takeIf { it == raw?.trim() }
                if (url == null) notice(R.string.qr_not_https)
                else BrowserDialogs.builder(activity).setTitle(R.string.qr_open)
                    .setMessage(url).setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok) { _, _ -> onOpen(url) }.show()
            }
        }.start()
    }
    private val cameraPermission = activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) capturePhoto() else notice(R.string.qr_camera_permission)
    }
    fun start() {
        if (photo != null) return
        if (activity.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) capturePhoto()
        else cameraPermission.launch(Manifest.permission.CAMERA)
    }
    private fun capturePhoto() {
        try {
            val folder = File(activity.cacheDir, "qr").apply { mkdirs() }
            folder.listFiles()?.forEach { it.delete() }
            val file = File.createTempFile("scan-", ".jpg", folder)
            photo = file
            val uri = FileProvider.getUriForFile(activity, "${activity.packageName}.updateprovider", file)
            photoUri = uri; capture.launch(uri)
        } catch (_: Exception) {
            photo?.delete(); photo = null; photoUri = null; notice(R.string.qr_camera_unavailable)
        }
    }
    companion object {
        @JvmStatic fun decode(file: File): String? {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            if (bounds.outWidth < 1 || bounds.outHeight < 1) return null
            var sample = 1
            while (bounds.outWidth / sample > 1200 || bounds.outHeight / sample > 1200) sample *= 2
            val image = BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
            return try {
                val pixels = IntArray(image.width * image.height)
                image.getPixels(pixels, 0, image.width, 0, 0, image.width, image.height)
                val source = RGBLuminanceSource(image.width, image.height, pixels)
                MultiFormatReader().decode(BinaryBitmap(HybridBinarizer(source)),
                    mapOf(DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE), DecodeHintType.TRY_HARDER to true)).text
            } finally { image.recycle() }
        }
    }
}
