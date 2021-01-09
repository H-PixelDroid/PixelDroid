package com.h.pixeldroid.postCreation.photoEdit

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.h.pixeldroid.R
import com.h.pixeldroid.postCreation.PostCreationActivity
import com.h.pixeldroid.utils.BaseActivity
import com.yalantis.ucrop.UCrop
import com.zomato.photofilters.imageprocessors.Filter
import com.zomato.photofilters.imageprocessors.subfilters.BrightnessSubFilter
import com.zomato.photofilters.imageprocessors.subfilters.ContrastSubFilter
import com.zomato.photofilters.imageprocessors.subfilters.SaturationSubfilter
import kotlinx.android.synthetic.main.activity_photo_edit.*
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors.newSingleThreadExecutor
import java.util.concurrent.Future

// This is an arbitrary number we are using to keep track of the permission
// request. Where an app has multiple context for requesting permission,
// this can help differentiate the different contexts.
private const val REQUEST_CODE_PERMISSIONS_SEND_PHOTO = 7
private val REQUIRED_PERMISSIONS = arrayOf(
    android.Manifest.permission.READ_EXTERNAL_STORAGE,
    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
)

class PhotoEditActivity : BaseActivity() {

    private var saving: Boolean = false
    private val BITMAP_CONFIG = Bitmap.Config.ARGB_8888
    private val BRIGHTNESS_START = 0
    private val SATURATION_START = 1.0f
    private val CONTRAST_START = 1.0f

    private var originalImage: Bitmap? = null
    private var compressedImage: Bitmap? = null
    private var compressedOriginalImage: Bitmap? = null
    private lateinit var filteredImage: Bitmap

    private var actualFilter: Filter? = null

    private lateinit var filterListFragment: FilterListFragment
    private lateinit var editImageFragment: EditImageFragment

    private lateinit var viewPager: NonSwipeableViewPager
    private lateinit var tabLayout: TabLayout

    private var brightnessFinal = BRIGHTNESS_START
    private var saturationFinal = SATURATION_START
    private var contrastFinal = CONTRAST_START

    init {
        System.loadLibrary("NativeImageProcessor")
    }

    companion object{
        private var executor: ExecutorService = newSingleThreadExecutor()
        private var future: Future<*>? = null

        private var saveExecutor: ExecutorService = newSingleThreadExecutor()
        private var saveFuture: Future<*>? = null

        private var initialUri: Uri? = null
        internal var imageUri: Uri? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_edit)

        supportActionBar?.setTitle(R.string.toolbar_title_edit)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)

        val cropButton: FloatingActionButton = findViewById(R.id.cropImageButton)

        initialUri = intent.getParcelableExtra("picture_uri")
        imageUri = initialUri
        
        // Crop button on-click listener
        cropButton.setOnClickListener {
            startCrop()
        }

        loadImage()

        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabs)
        setupViewPager(viewPager)
        tabLayout.setupWithViewPager(viewPager)
    }


    private fun loadImage() {
        originalImage = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
        compressedImage = resizeImage(originalImage!!)
        compressedOriginalImage = compressedImage!!.copy(BITMAP_CONFIG, true)
        filteredImage = compressedImage!!.copy(BITMAP_CONFIG, true)
        Glide.with(this).load(compressedImage).into(image_preview)
    }

    private fun resizeImage(image: Bitmap): Bitmap {
        val display = windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)

        val newY = size.y * 0.7
        val scale = newY / image.height
        return Bitmap.createScaledBitmap(image, (image.width * scale).toInt(), newY.toInt(), true)
    }

    private fun setupViewPager(viewPager: NonSwipeableViewPager?) {
        val adapter = EditPhotoViewPagerAdapter(supportFragmentManager)

        filterListFragment = FilterListFragment()
        filterListFragment.setListener(this)

        editImageFragment = EditImageFragment()
        editImageFragment.setListener(this)
        adapter.addFragment(filterListFragment, "FILTERS")
        adapter.addFragment(editImageFragment, "EDIT")

        viewPager!!.adapter = adapter
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.edit_photo_menu, menu)
        return true
    }

    override fun onStop() {
        super.onStop()
        saving = false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when(item.itemId) {
            android.R.id.home -> {
                if (noEdits()) onBackPressed()
                else {
                    val builder = AlertDialog.Builder(this)
                    builder.apply {
                        setMessage(R.string.save_before_returning)
                        setPositiveButton(android.R.string.ok) { _, _ ->
                            saveImageToGallery()
                        }
                        setNegativeButton(R.string.no_cancel_edit) { _, _ ->
                            onBackPressed()
                        }
                    }
                    // Create the AlertDialog
                    builder.show()
                }
            }
            R.id.action_save -> {
                saveImageToGallery()
            }
            R.id.action_reset -> {
                resetControls()
                actualFilter = null
                imageUri = initialUri
                loadImage()
                filterListFragment.resetSelectedFilter()
            }
        }

    return super.onOptionsItemSelected(item)
}

    fun onFilterSelected(filter: Filter) {
        filteredImage = compressedOriginalImage!!.copy(BITMAP_CONFIG, true)
        image_preview.setImageBitmap(filter.processFilter(filteredImage))
        compressedImage = filteredImage.copy(BITMAP_CONFIG, true)
        actualFilter = filter
        resetControls()
    }

    private fun resetControls() {
        brightnessFinal = BRIGHTNESS_START
        saturationFinal = SATURATION_START
        contrastFinal = CONTRAST_START

        editImageFragment.resetControl()
    }


    private fun applyFilterAndShowImage(filter: Filter, image: Bitmap?) {
        future?.cancel(true)
        future = executor.submit {
            val bitmap = filter.processFilter(image!!.copy(BITMAP_CONFIG, true))
            image_preview.post {
                image_preview.setImageBitmap(bitmap)
            }
        }
    }

    fun onBrightnessChange(brightness: Int) {
        brightnessFinal = brightness
        val myFilter = Filter()
        myFilter.addEditFilters(brightness, saturationFinal, contrastFinal)
        applyFilterAndShowImage(myFilter, filteredImage)
    }

    fun onSaturationChange(saturation: Float) {
        saturationFinal = saturation
        val myFilter = Filter()
        myFilter.addEditFilters(brightnessFinal, saturation, contrastFinal)
        applyFilterAndShowImage(myFilter, filteredImage)
    }

    fun onContrastChange(contrast: Float) {
        contrastFinal = contrast
        val myFilter = Filter()
        myFilter.addEditFilters(brightnessFinal, saturationFinal, contrast)
        applyFilterAndShowImage(myFilter, filteredImage)
    }

    private fun Filter.addEditFilters(br: Int, sa: Float, co: Float): Filter {
        addSubFilter(BrightnessSubFilter(br))
        addSubFilter(ContrastSubFilter(co))
        addSubFilter(SaturationSubfilter(sa))
        return this
    }

    fun onEditStarted() {
    }

    fun onEditCompleted() {
        val myFilter = Filter()
        myFilter.addEditFilters(brightnessFinal, saturationFinal, contrastFinal)
        val bitmap = filteredImage.copy(BITMAP_CONFIG, true)

        compressedImage = myFilter.processFilter(bitmap)
    }


    private fun startCrop() {
        val file = File.createTempFile("temp_crop_img", ".png", cacheDir)

        val options: UCrop.Options = UCrop.Options().apply {
            setStatusBarColor(resources.getColor(R.color.colorPrimaryDark, theme))
            setActiveControlsWidgetColor(resources.getColor(R.color.colorButtonBg, theme))
        }
        val uCrop: UCrop = UCrop.of(initialUri!!, Uri.fromFile(file)).withOptions(options)
        uCrop.start(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == Activity.RESULT_OK) {
             if (requestCode == UCrop.RESULT_ERROR) {
                handleCropError(data)
            } else {
                handleCropResult(data)
            }
        }
    }

    private fun resetFilteredImage(){
        val newBr = if(brightnessFinal != 0) BRIGHTNESS_START/brightnessFinal else 0
        val newSa = if(saturationFinal != 0.0f) SATURATION_START/saturationFinal else 0.0f
        val newCo = if(contrastFinal != 0.0f) CONTRAST_START/contrastFinal else 0.0f
        val myFilter = Filter().addEditFilters(newBr, newSa, newCo)

        filteredImage = myFilter.processFilter(filteredImage)
    }

    private fun handleCropResult(data: Intent?) {
        val resultCrop: Uri? = UCrop.getOutput(data!!)
        if(resultCrop != null) {
            imageUri = resultCrop
            image_preview.setImageURI(resultCrop)
            val bitmap = (image_preview.drawable as BitmapDrawable).bitmap
            originalImage = bitmap.copy(Bitmap.Config.ARGB_8888, true)
            compressedImage = resizeImage(originalImage!!.copy(BITMAP_CONFIG, true))
            compressedOriginalImage = compressedImage!!.copy(BITMAP_CONFIG, true)
            filteredImage = compressedImage!!.copy(BITMAP_CONFIG, true)
            resetFilteredImage()
        } else {
            Toast.makeText(this, R.string.crop_result_error, Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleCropError(data: Intent?) {
        val resultError = UCrop.getError(data!!)
        if(resultError != null) {
            Toast.makeText(this, "" + resultError, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, R.string.crop_result_error, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if(grantResults.size > 1
            && grantResults[0] == PackageManager.PERMISSION_GRANTED
            && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
            // permission was granted
            permissionsGrantedToSave()
        } else {
            Snackbar.make(coordinator_edit, getString(R.string.permission_denied),
                Snackbar.LENGTH_LONG).show()
        }
    }

    private fun applyFinalFilters(image: Bitmap?): Bitmap {
        val editFilter = Filter().addEditFilters(brightnessFinal, saturationFinal, contrastFinal)

        var finalImage = editFilter.processFilter(image!!.copy(BITMAP_CONFIG, true))
        if (actualFilter!=null) finalImage = actualFilter!!.processFilter(finalImage)
        return finalImage
    }

    private fun sendBackImage(file: String) {
        val intent = Intent(this, PostCreationActivity::class.java)
        .apply {
            putExtra("result", file)
            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        }

        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    private fun saveImageToGallery() {
        // runtime permission and process
        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(
                this,
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS_SEND_PHOTO
            )
        } else {
            permissionsGrantedToSave()
        }
    }

    /**
     * Check if all permission specified in the manifest have been granted
     */
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            applicationContext, it) == PackageManager.PERMISSION_GRANTED
    }


    private fun OutputStream.writeBitmap(bitmap: Bitmap) {
        use { out ->
            //(quality is ignored for PNG)
            bitmap.compress(Bitmap.CompressFormat.PNG, 85, out)
            out.flush()
        }
    }

    private fun noEdits(): Boolean =
            brightnessFinal == BRIGHTNESS_START
                    && contrastFinal == CONTRAST_START
                    && saturationFinal == SATURATION_START
                    && actualFilter?.let { it.name == getString(R.string.normal_filter)} ?: true

    private fun permissionsGrantedToSave() {
        if (saving) {
            val builder = AlertDialog.Builder(this)
            builder.apply {
                setMessage(R.string.busy_dialog_text)
                setNegativeButton(R.string.busy_dialog_ok_button) { _, _ -> }
            }
            // Create the AlertDialog
            builder.show()
            return
        }
        saving = true
        progressBarSaveFile.visibility = VISIBLE
        saveFuture = saveExecutor.submit {
            try {
                val path: String
                if(!noEdits()) {
                    // Save modified image in cache
                    val tempFile = File.createTempFile("temp_edit_img", ".png", cacheDir)
                    path = Uri.fromFile(tempFile).toString()
                    tempFile.outputStream().writeBitmap(applyFinalFilters(originalImage))
                }
                else {
                    path = imageUri.toString()
                }

                if(saving) {
                    this.runOnUiThread {
                        sendBackImage(path)
                        progressBarSaveFile.visibility = GONE
                        saving = false
                    }
                }
            } catch (e: IOException) {
                this.runOnUiThread {
                    Snackbar.make(
                        coordinator_edit, getString(R.string.save_image_failed),
                        Snackbar.LENGTH_LONG
                    ).show()
                    progressBarSaveFile.visibility = GONE
                    saving = false
                }
            }
        }
    }
}
