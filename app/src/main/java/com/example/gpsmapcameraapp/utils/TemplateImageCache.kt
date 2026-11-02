package com.example.gpsmapcameraapp.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.LruCache
import android.view.animation.AlphaAnimation
import android.widget.ImageView
import com.example.gpsmapcameraapp.models.StampTemplate
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

object TemplateImageCache {

    private const val TAG = "TemplateImageCache"
    private const val BASE_URL = "https://cdn.simatrixacademy.com/gps/templates/v1/"
    private const val CACHE_DIR_NAME = "template_images"

    private val executor = Executors.newFixedThreadPool(4)
    private val mainHandler = Handler(Looper.getMainLooper())

    // In-memory LRU Cache (max 1/8th of available memory)
    private val memoryCache: LruCache<String, Bitmap>

    init {
        val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
        val cacheSize = maxMemory / 8
        memoryCache = object : LruCache<String, Bitmap>(cacheSize) {
            override fun sizeOf(key: String, bitmap: Bitmap): Int {
                return bitmap.byteCount / 1024
            }
        }
    }

    private fun getCacheDir(context: Context): File {
        val dir = File(context.cacheDir, CACHE_DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun isOnline(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val network = cm?.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Loads a template image into an ImageView:
     * 1. Checks memory cache -> instant display.
     * 2. Checks disk cache -> decodes and displays.
     * 3. If missing and online -> downloads, saves to disk cache, and displays.
     */
    fun loadTemplateImage(
        context: Context,
        fileName: String,
        imageView: ImageView,
        targetWidth: Int = 400,
        targetHeight: Int = 500
    ) {
        val fullUrl = "$BASE_URL$fileName"
        imageView.tag = fileName

        // 1. Check memory cache
        val memBitmap = memoryCache.get(fileName)
        if (memBitmap != null && !memBitmap.isRecycled) {
            imageView.setImageBitmap(memBitmap)
            return
        }

        // 2. Asynchronous disk / network fetch
        executor.execute {
            try {
                val diskFile = File(getCacheDir(context), fileName)

                val bitmap: Bitmap? = if (diskFile.exists() && diskFile.length() > 0) {
                    // Load from Disk Cache
                    decodeSampledBitmap(diskFile.absolutePath, targetWidth, targetHeight)
                } else if (isOnline(context)) {
                    // Download & Cache to Disk
                    val downloaded = downloadFile(fullUrl, diskFile)
                    if (downloaded) {
                        decodeSampledBitmap(diskFile.absolutePath, targetWidth, targetHeight)
                    } else null
                } else {
                    null
                }

                if (bitmap != null) {
                    memoryCache.put(fileName, bitmap)

                    mainHandler.post {
                        if (imageView.tag == fileName) {
                            imageView.setImageBitmap(bitmap)
                            val fadeIn = AlphaAnimation(0f, 1f).apply { duration = 250 }
                            imageView.startAnimation(fadeIn)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading template image $fileName: ${e.message}")
            }
        }
    }

    /**
     * Pre-caches all 5 template preview images in the background when network is active.
     */
    fun preCacheAllTemplates(context: Context) {
        if (!isOnline(context)) return

        executor.execute {
            for (template in StampTemplate.entries) {
                val file = File(getCacheDir(context), template.cdnFileName)
                if (!file.exists() || file.length() == 0L) {
                    try {
                        val url = "$BASE_URL${template.cdnFileName}"
                        downloadFile(url, file)
                        Log.d(TAG, "Cached template image: ${template.cdnFileName}")
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to pre-cache ${template.cdnFileName}: ${e.message}")
                    }
                }
            }
        }
    }

    private fun downloadFile(urlString: String, destinationFile: File): Boolean {
        var connection: HttpURLConnection? = null
        val tempFile = File(destinationFile.parentFile, "${destinationFile.name}.tmp")

        return try {
            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 8000
            connection.readTimeout = 15000
            connection.instanceFollowRedirects = true

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return false
            }

            connection.inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }

            if (tempFile.length() > 0) {
                if (destinationFile.exists()) destinationFile.delete()
                tempFile.renameTo(destinationFile)
                true
            } else {
                tempFile.delete()
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Download failed for $urlString: ${e.message}")
            if (tempFile.exists()) tempFile.delete()
            false
        } finally {
            connection?.disconnect()
        }
    }

    private fun decodeSampledBitmap(filePath: String, reqWidth: Int, reqHeight: Int): Bitmap? {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(filePath, options)

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
        options.inJustDecodeBounds = false
        options.inPreferredConfig = Bitmap.Config.RGB_565 // Memory optimization

        return BitmapFactory.decodeFile(filePath, options)
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
