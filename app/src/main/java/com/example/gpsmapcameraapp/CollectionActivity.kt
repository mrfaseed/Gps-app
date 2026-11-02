package com.example.gpsmapcameraapp

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.LruCache
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.gpsmapcameraapp.views.ZoomableImageView
import java.io.File
import java.util.concurrent.Executors

class CollectionActivity : AppCompatActivity() {

    private lateinit var normalToolbar: View
    private lateinit var selectionToolbar: View
    private lateinit var selectionBottomBar: View
    private lateinit var tvPhotoCount: TextView
    private lateinit var tvSelectionCount: TextView
    private lateinit var btnSelectAll: TextView
    private lateinit var tvDeleteSelectedLabel: TextView
    private lateinit var tvShareSelectedLabel: TextView
    private lateinit var emptyLayout: View
    private lateinit var progressBar: ProgressBar
    private lateinit var rvPhotos: RecyclerView

    private val photosList = mutableListOf<File>()
    private val selectedPhotos = LinkedHashSet<File>()
    private var isSelectionMode = false

    private lateinit var adapter: PhotoAdapter
    private val executor = Executors.newFixedThreadPool(4)
    private val mainHandler = Handler(Looper.getMainLooper())

    companion object {
        private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
        private val cacheSize = maxMemory / 8

        val thumbnailCache: LruCache<String, Bitmap> = object : LruCache<String, Bitmap>(cacheSize) {
            override fun sizeOf(key: String, bitmap: Bitmap): Int {
                return bitmap.byteCount / 1024
            }
        }

        val fullImageCache: LruCache<String, Bitmap> = object : LruCache<String, Bitmap>(cacheSize) {
            override fun sizeOf(key: String, bitmap: Bitmap): Int {
                return bitmap.byteCount / 1024
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_collection)

        initViews()
        setupListeners()
        setupBackHandler()

        adapter = PhotoAdapter(photosList)
        rvPhotos.layoutManager = GridLayoutManager(this, 3)
        rvPhotos.adapter = adapter

        loadPhotos()
    }

    override fun onResume() {
        super.onResume()
        if (!isSelectionMode) {
            loadPhotos()
        }
    }

    private fun initViews() {
        normalToolbar = findViewById(R.id.collection_toolbar)
        selectionToolbar = findViewById(R.id.selection_toolbar)
        selectionBottomBar = findViewById(R.id.selection_bottom_bar)

        tvPhotoCount = findViewById(R.id.tv_photo_count)
        tvSelectionCount = findViewById(R.id.tv_selection_count)
        btnSelectAll = findViewById(R.id.btn_select_all)
        tvDeleteSelectedLabel = findViewById(R.id.tv_delete_selected_label)
        tvShareSelectedLabel = findViewById(R.id.tv_share_selected_label)

        emptyLayout = findViewById(R.id.layout_empty_collection)
        progressBar = findViewById(R.id.collection_progress_bar)
        rvPhotos = findViewById(R.id.rv_photos)
    }

    private fun setupListeners() {
        findViewById<ImageView>(R.id.btn_back_collection).setOnClickListener {
            finish()
        }

        findViewById<ImageView>(R.id.btn_close_selection).setOnClickListener {
            exitSelectionMode()
        }

        btnSelectAll.setOnClickListener {
            if (selectedPhotos.size == photosList.size) {
                // Deselect all
                selectedPhotos.clear()
                exitSelectionMode()
            } else {
                // Select all
                selectedPhotos.clear()
                selectedPhotos.addAll(photosList)
                updateSelectionUI()
                adapter.notifyDataSetChanged()
            }
        }

        findViewById<View>(R.id.btn_delete_selected).setOnClickListener {
            deleteSelectedPhotos()
        }

        findViewById<View>(R.id.btn_share_selected).setOnClickListener {
            shareSelectedPhotos()
        }
    }

    private fun setupBackHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isSelectionMode) {
                    exitSelectionMode()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun enterSelectionMode(initialFile: File) {
        isSelectionMode = true
        selectedPhotos.clear()
        selectedPhotos.add(initialFile)
        updateSelectionUI()
        adapter.notifyDataSetChanged()
    }

    private fun exitSelectionMode() {
        isSelectionMode = false
        selectedPhotos.clear()
        updateSelectionUI()
        adapter.notifyDataSetChanged()
    }

    private fun togglePhotoSelection(file: File) {
        if (selectedPhotos.contains(file)) {
            selectedPhotos.remove(file)
            if (selectedPhotos.isEmpty()) {
                exitSelectionMode()
                return
            }
        } else {
            selectedPhotos.add(file)
        }
        updateSelectionUI()
        adapter.notifyDataSetChanged()
    }

    private fun updateSelectionUI() {
        if (isSelectionMode) {
            normalToolbar.visibility = View.GONE
            selectionToolbar.visibility = View.VISIBLE
            selectionBottomBar.visibility = View.VISIBLE

            val count = selectedPhotos.size
            tvSelectionCount.text = "$count Selected"
            tvDeleteSelectedLabel.text = "Delete ($count)"
            tvShareSelectedLabel.text = "Share ($count)"
            btnSelectAll.text = if (count == photosList.size && photosList.isNotEmpty()) "Deselect All" else "Select All"
        } else {
            normalToolbar.visibility = View.VISIBLE
            selectionToolbar.visibility = View.GONE
            selectionBottomBar.visibility = View.GONE
        }
    }

    private fun deleteSelectedPhotos() {
        if (selectedPhotos.isEmpty()) return
        val count = selectedPhotos.size

        AlertDialog.Builder(this)
            .setTitle("Delete $count ${if (count == 1) "Photo" else "Photos"}?")
            .setMessage("Are you sure you want to permanently delete $count selected ${if (count == 1) "photo" else "photos"}?")
            .setPositiveButton("Delete") { _, _ ->
                val filesToDelete = ArrayList(selectedPhotos)
                progressBar.visibility = View.VISIBLE

                executor.execute {
                    var deletedCount = 0
                    for (file in filesToDelete) {
                        if (file.delete()) {
                            deletedCount++
                            try {
                                val scanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                                scanIntent.data = Uri.fromFile(file)
                                sendBroadcast(scanIntent)
                            } catch (_: Exception) {}
                        }
                    }

                    mainHandler.post {
                        progressBar.visibility = View.GONE
                        Toast.makeText(this@CollectionActivity, "$deletedCount photos deleted", Toast.LENGTH_SHORT).show()
                        exitSelectionMode()
                        loadPhotos()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun shareSelectedPhotos() {
        if (selectedPhotos.isEmpty()) return
        val uris = ArrayList<Uri>()
        for (file in selectedPhotos) {
            try {
                val uri = FileProvider.getUriForFile(
                    this,
                    "${applicationContext.packageName}.provider",
                    file
                )
                uris.add(uri)
            } catch (_: Exception) {}
        }

        if (uris.isEmpty()) return

        val shareIntent = Intent().apply {
            if (uris.size == 1) {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, uris[0])
            } else {
                action = Intent.ACTION_SEND_MULTIPLE
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            }
            type = "image/jpeg"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(shareIntent, "Share ${uris.size} Photos"))
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

    /**
     * Modern full-screen Gallery Viewer supporting:
     * - Horizontal swiping between photos via ViewPager2
     * - Pinch-to-zoom and double-tap zoom via ZoomableImageView
     * - Drag & pan across zoomed photos
     * - Tap photo to toggle header/footer bars
     * - Dynamic title and counter updates
     * - Share & Delete actions
     */
    private fun showGalleryViewer(initialPosition: Int) {
        if (photosList.isEmpty() || initialPosition !in photosList.indices) return

        val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_fullscreen_photo)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.BLACK))

        val vpGallery = dialog.findViewById<ViewPager2>(R.id.vp_gallery)
        val topBar = dialog.findViewById<View>(R.id.dialog_top_bar)
        val bottomBar = dialog.findViewById<View>(R.id.dialog_bottom_bar)
        val btnClose = dialog.findViewById<ImageView>(R.id.btn_close_dialog)
        val tvTitle = dialog.findViewById<TextView>(R.id.tv_photo_title)
        val tvCounter = dialog.findViewById<TextView>(R.id.tv_photo_counter)
        val btnShare = dialog.findViewById<View>(R.id.btn_share_photo)
        val btnDelete = dialog.findViewById<View>(R.id.btn_delete_photo)

        var isBarsVisible = true
        fun toggleBars() {
            isBarsVisible = !isBarsVisible
            val targetAlpha = if (isBarsVisible) 1f else 0f
            topBar.animate().alpha(targetAlpha).setDuration(200).withEndAction {
                topBar.visibility = if (isBarsVisible) View.VISIBLE else View.GONE
            }.start()
            bottomBar.animate().alpha(targetAlpha).setDuration(200).withEndAction {
                bottomBar.visibility = if (isBarsVisible) View.VISIBLE else View.GONE
            }.start()
        }

        fun updateHeader(position: Int) {
            if (position in photosList.indices) {
                val file = photosList[position]
                tvTitle.text = file.name
                tvCounter.text = "${position + 1} of ${photosList.size}"
            }
        }

        btnClose.setOnClickListener { dialog.dismiss() }

        val galleryAdapter = GalleryPagerAdapter(photosList) {
            toggleBars()
        }
        vpGallery.adapter = galleryAdapter
        vpGallery.setCurrentItem(initialPosition, false)
        updateHeader(initialPosition)

        vpGallery.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateHeader(position)
            }
        })

        btnShare.setOnClickListener {
            val currentPos = vpGallery.currentItem
            if (currentPos in photosList.indices) {
                sharePhoto(photosList[currentPos])
            }
        }

        btnDelete.setOnClickListener {
            val currentPos = vpGallery.currentItem
            if (currentPos !in photosList.indices) return@setOnClickListener

            val photoFile = photosList[currentPos]
            AlertDialog.Builder(this)
                .setTitle("Delete Photo")
                .setMessage("Are you sure you want to delete this photo?")
                .setPositiveButton("Delete") { _, _ ->
                    if (photoFile.delete()) {
                        Toast.makeText(this, "Photo deleted", Toast.LENGTH_SHORT).show()
                        photosList.removeAt(currentPos)
                        galleryAdapter.notifyItemRemoved(currentPos)
                        adapter.notifyDataSetChanged()
                        tvPhotoCount.text = "${photosList.size} photos"

                        if (photosList.isEmpty()) {
                            dialog.dismiss()
                            emptyLayout.visibility = View.VISIBLE
                        } else {
                            val newPos = vpGallery.currentItem.coerceIn(0, photosList.size - 1)
                            updateHeader(newPos)
                        }
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

    // Grid PhotoAdapter supporting Multi-Selection
    inner class PhotoAdapter(
        private val list: List<File>
    ) : RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder>() {

        inner class PhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val ivThumbnail: ImageView = itemView.findViewById(R.id.iv_thumbnail)
            val selectionOverlay: View = itemView.findViewById(R.id.selection_overlay)
            val ivCheckBadge: ImageView = itemView.findViewById(R.id.iv_check_badge)
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

            val cached = thumbnailCache.get(path)
            if (cached != null) {
                holder.ivThumbnail.setImageBitmap(cached)
            } else {
                holder.itemView.tag = path
                executor.execute {
                    val thumb = decodeSampledBitmap(path, 250, 250)
                    if (thumb != null) {
                        thumbnailCache.put(path, thumb)
                        mainHandler.post {
                            if (holder.itemView.tag == path) {
                                holder.ivThumbnail.setImageBitmap(thumb)
                            }
                        }
                    }
                }
            }

            // Selection state
            if (isSelectionMode) {
                holder.ivCheckBadge.visibility = View.VISIBLE
                if (selectedPhotos.contains(file)) {
                    holder.ivCheckBadge.setImageResource(R.drawable.ic_check_circle_checked)
                    holder.selectionOverlay.visibility = View.VISIBLE
                } else {
                    holder.ivCheckBadge.setImageResource(R.drawable.ic_check_circle_empty)
                    holder.selectionOverlay.visibility = View.GONE
                }
            } else {
                holder.ivCheckBadge.visibility = View.GONE
                holder.selectionOverlay.visibility = View.GONE
            }

            // Regular tap
            holder.itemView.setOnClickListener {
                if (isSelectionMode) {
                    togglePhotoSelection(file)
                } else {
                    val pos = holder.bindingAdapterPosition
                    if (pos != RecyclerView.NO_POSITION) {
                        showGalleryViewer(pos)
                    }
                }
            }

            // Long press to enter selection mode
            holder.itemView.setOnLongClickListener {
                if (!isSelectionMode) {
                    holder.itemView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    enterSelectionMode(file)
                    true
                } else {
                    togglePhotoSelection(file)
                    true
                }
            }
        }
    }

    // Fullscreen ViewPager2 Gallery PagerAdapter
    inner class GalleryPagerAdapter(
        private val files: List<File>,
        private val onPhotoTap: () -> Unit
    ) : RecyclerView.Adapter<GalleryPagerAdapter.GalleryViewHolder>() {

        inner class GalleryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val zoomablePhoto: ZoomableImageView = itemView.findViewById(R.id.zoomable_photo)
            val progressBar: ProgressBar = itemView.findViewById(R.id.gallery_item_progress)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GalleryViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_gallery_photo, parent, false)
            return GalleryViewHolder(view)
        }

        override fun getItemCount(): Int = files.size

        override fun onBindViewHolder(holder: GalleryViewHolder, position: Int) {
            val file = files[position]
            val path = file.absolutePath

            holder.zoomablePhoto.resetZoom()
            holder.zoomablePhoto.onPhotoTapListener = onPhotoTap

            val cached = fullImageCache.get(path)
            if (cached != null) {
                holder.progressBar.visibility = View.GONE
                holder.zoomablePhoto.setImageBitmap(cached)
            } else {
                holder.progressBar.visibility = View.VISIBLE
                holder.zoomablePhoto.setImageDrawable(null)
                holder.itemView.tag = path

                executor.execute {
                    val bitmap = decodeSampledBitmap(path, 1920, 1920)
                    if (bitmap != null) {
                        fullImageCache.put(path, bitmap)
                    }
                    mainHandler.post {
                        if (holder.itemView.tag == path) {
                            holder.progressBar.visibility = View.GONE
                            if (bitmap != null) {
                                holder.zoomablePhoto.setImageBitmap(bitmap)
                            } else {
                                Toast.makeText(this@CollectionActivity, "Unable to load image", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }

        override fun onViewRecycled(holder: GalleryViewHolder) {
            super.onViewRecycled(holder)
            holder.zoomablePhoto.resetZoom()
        }
    }
}
