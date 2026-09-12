package com.example.gpsmapcameraapp

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.LruCache
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.util.concurrent.Executors

class CollectionActivity : AppCompatActivity() {

    private lateinit var rvPhotos: RecyclerView
    private lateinit var tvPhotoCount: TextView
    private lateinit var emptyLayout: View
    private lateinit var progressBar: ProgressBar
    private val photosList = mutableListOf<File>()
    private lateinit var adapter: PhotoAdapter
    private val executor = Executors.newFixedThreadPool(4)
    private val mainHandler = Handler(Looper.getMainLooper())

    companion object {
        private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
        private val cacheSize = maxMemory / 8
        val memoryCache: LruCache<String, Bitmap> = object : LruCache<String, Bitmap>(cacheSize) {
            override fun sizeOf(key: String, bitmap: Bitmap): Int {
                return bitmap.byteCount / 1024
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_collection)

        findViewById<ImageView>(R.id.btn_back_collection).setOnClickListener {
            finish()
        }

        tvPhotoCount = findViewById(R.id.tv_photo_count)
        emptyLayout = findViewById(R.id.layout_empty_collection)
        progressBar = findViewById(R.id.collection_progress_bar)
        rvPhotos = findViewById(R.id.rv_photos)

        adapter = PhotoAdapter(photosList) { clickedFile ->
            showPhotoDetailDialog(clickedFile)
        }

        rvPhotos.layoutManager = GridLayoutManager(this, 3)
        rvPhotos.adapter = adapter

        loadPhotos()
    }

    override fun onResume() {
        super.onResume()
        loadPhotos()
    }

    private fun loadPhotos() {
        progressBar.visibility = View.VISIBLE
        executor.execute {
            val foundFiles = LinkedHashSet<File>()

            // 1. App default DCIM Camera directory
            val dcimCamera = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
                "Camera"
            )
            scanDirectory(dcimCamera, foundFiles)

            // 2. Site 1 folder
            val site1 = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
                "GPS Map Camera App/Site 1"
            )
            scanDirectory(site1, foundFiles)

            // 3. Site 2 folder
            val site2 = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
                "GPS Map Camera App/Site 2"
            )
            scanDirectory(site2, foundFiles)

            // 4. MediaStore for any recently taken DCIM camera images
            try {
                val projection = arrayOf(MediaStore.Images.Media.DATA)
                val cursor = contentResolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    "${MediaStore.Images.Media.DATA} LIKE ?",
                    arrayOf("%DCIM%"),
                    "${MediaStore.Images.Media.DATE_ADDED} DESC"
                )
                cursor?.use {
                    val dataColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                    var count = 0
                    while (it.moveToNext() && count < 100) {
                        val path = it.getString(dataColumn)
                        if (!path.isNullOrBlank()) {
                            val f = File(path)
                            if (f.exists() && f.isFile && isImageFile(f.name)) {
                                foundFiles.add(f)
                                count++
                            }
                        }
                    }
                }
            } catch (_: Exception) { }

            // Sort newest first
            val sortedList = foundFiles.toList().sortedByDescending { it.lastModified() }

            mainHandler.post {
                progressBar.visibility = View.GONE
                photosList.clear()
                photosList.addAll(sortedList)
                adapter.notifyDataSetChanged()

                tvPhotoCount.text = "${photosList.size} photos"
                emptyLayout.visibility = if (photosList.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun scanDirectory(dir: File, destination: LinkedHashSet<File>) {
        if (dir.exists() && dir.isDirectory) {
            dir.listFiles()?.forEach { file ->
                if (file.isFile && isImageFile(file.name)) {
                    destination.add(file)
                }
            }
        }
    }

    private fun isImageFile(name: String): Boolean {
        val lower = name.lowercase()
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp")
    }

    private fun showPhotoDetailDialog(photoFile: File) {
        val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_fullscreen_photo)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.BLACK))

        val ivFull = dialog.findViewById<ImageView>(R.id.iv_fullscreen_photo)
        val tvTitle = dialog.findViewById<TextView>(R.id.tv_photo_title)
        val btnClose = dialog.findViewById<ImageView>(R.id.btn_close_dialog)
        val btnShare = dialog.findViewById<View>(R.id.btn_share_photo)
        val btnDelete = dialog.findViewById<View>(R.id.btn_delete_photo)

        tvTitle.text = photoFile.name
        btnClose.setOnClickListener { dialog.dismiss() }

        // Load full image with safe downsampling
        executor.execute {
            val bitmap = decodeSampledBitmap(photoFile.absolutePath, 1440, 1440)
            mainHandler.post {
                if (bitmap != null) {
                    ivFull.setImageBitmap(bitmap)
                } else {
                    Toast.makeText(this, "Unable to load image", Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnShare.setOnClickListener {
            sharePhoto(photoFile)
        }

        btnDelete.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Delete Photo")
                .setMessage("Are you sure you want to delete this photo?")
                .setPositiveButton("Delete") { _, _ ->
                    if (photoFile.delete()) {
                        Toast.makeText(this, "Photo deleted", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        loadPhotos()
                    } else {
                        Toast.makeText(this, "Failed to delete photo", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        dialog.show()
    }

    private fun sharePhoto(file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.provider",
                file
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, "Share Photo"))
        } catch (e: Exception) {
            Toast.makeText(this, "Could not share photo: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun decodeSampledBitmap(path: String, reqWidth: Int, reqHeight: Int): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, options)
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            options.inJustDecodeBounds = false
            BitmapFactory.decodeFile(path, options)
        } catch (_: Exception) {
            null
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    // RecyclerView Adapter
    inner class PhotoAdapter(
        private val list: List<File>,
        private val onItemClick: (File) -> Unit
    ) : RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder>() {

        inner class PhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val ivThumbnail: ImageView = itemView.findViewById(R.id.iv_thumbnail)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_collection_photo, parent, false)
            return PhotoViewHolder(view)
        }

        override fun getItemCount(): Int = list.size

        override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
            val file = list[position]
            val path = file.absolutePath

            holder.ivThumbnail.setImageDrawable(ColorDrawable(Color.parseColor("#2A2A2A")))

            val cached = memoryCache.get(path)
            if (cached != null) {
                holder.ivThumbnail.setImageBitmap(cached)
            } else {
                holder.itemView.tag = path
                executor.execute {
                    val thumb = decodeSampledBitmap(path, 250, 250)
                    if (thumb != null) {
                        memoryCache.put(path, thumb)
                        mainHandler.post {
                            if (holder.itemView.tag == path) {
                                holder.ivThumbnail.setImageBitmap(thumb)
                            }
                        }
                    }
                }
            }

            holder.itemView.setOnClickListener {
                onItemClick(file)
            }
        }
    }
}
