package com.h.pixeldroid

import android.app.Activity
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
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.h.pixeldroid.adapters.EditPhotoViewPagerAdapter
import com.h.pixeldroid.fragments.EditImageFragment
import com.h.pixeldroid.fragments.FilterListFragment
import com.h.pixeldroid.interfaces.EditImageFragmentListener
import com.h.pixeldroid.interfaces.FilterListFragmentListener
import com.h.pixeldroid.utils.NonSwipeableViewPager
import com.yalantis.ucrop.UCrop
import com.zomato.photofilters.imageprocessors.Filter
import com.zomato.photofilters.imageprocessors.subfilters.BrightnessSubFilter
import com.zomato.photofilters.imageprocessors.subfilters.ContrastSubFilter
import com.zomato.photofilters.imageprocessors.subfilters.SaturationSubfilter
import kotlinx.android.synthetic.main.activity_photo_edit.*
import kotlinx.android.synthetic.main.content_photo_edit.*
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors.newSingleThreadExecutor
import java.util.concurrent.Future

// This is an arbitrary number we are using to keep track of the permission
// request. Where an app has multiple context for requesting permission,
// this can help differentiate the different contexts.
private const val REQUEST_CODE_PERMISSIONS_SAVE_PHOTO = 8
private const val REQUEST_CODE_PERMISSIONS_SEND_PHOTO = 7
private val REQUIRED_PERMISSIONS = arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE,
    android.Manifest.permission.WRITE_EXTERNAL_STORAGE)

class PhotoEditActivity : AppCompatActivity(), FilterListFragmentListener, EditImageFragmentListener {

    private val BITMAP_CONFIG = Bitmap.Config.ARGB_8888
    private val BRIGHTNESS_START = 0
    private val SATURATION_START = 1.0f
    private val CONTRAST_START = 1.0f

    private var originalImage: Bitmap? = null
    private var compressedImage: Bitmap? = null
    private var compressedOriginalImage: Bitmap? = null
    private lateinit var filteredImage: Bitmap
    private lateinit var finalImage: Bitmap

    private var actualFilter: Filter? = null

    private lateinit var filterListFragment: FilterListFragment
    private lateinit var editImageFragment: EditImageFragment

    private lateinit var outputDirectory: File

    lateinit var viewPager: NonSwipeableViewPager
    lateinit var tabLayout: TabLayout

    private var brightnessFinal = BRIGHTNESS_START
    private var saturationFinal = SATURATION_START
    private var contrastFinal = CONTRAST_START

    private var imageUri: Uri? = null
    private var cropUri: Uri? = null

    object URI {var picture_uri: Uri? = null}

    init {
        System.loadLibrary("NativeImageProcessor")
    }


    companion object{
        private var executor: ExecutorService = newSingleThreadExecutor()
        private var future: Future<*>? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_edit)

        //TODO move to xml:
        setSupportActionBar(toolbar)
        supportActionBar!!.title = "Edit"
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setHomeButtonEnabled(true)
        cropUri = intent.getParcelableExtra("picture_uri")

        val cropButton: FloatingActionButton = findViewById(R.id.cropImageButton)
        cropButton.alpha = 0.5f
        // set on-click listener
        cropButton.setOnClickListener {
            startCrop()
        }

        loadImage()
        val file = File.createTempFile("temp_compressed_img", ".png", cacheDir)
        file.writeBitmap(compressedImage!!)
        URI.picture_uri = Uri.fromFile(file)

        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabs)
        setupViewPager(viewPager)
        tabLayout.setupWithViewPager(viewPager)
        outputDirectory = getOutputDirectory()
    }


    //<editor-fold desc="ON LAUNCH">
    private fun loadImage() {
        originalImage = MediaStore.Images.Media.getBitmap(contentResolver, cropUri)
        compressedImage = resizeImage(originalImage!!.copy(BITMAP_CONFIG, true))
        compressedOriginalImage = compressedImage!!.copy(BITMAP_CONFIG, true)
        filteredImage = compressedImage!!.copy(BITMAP_CONFIG, true)
        image_preview.setImageBitmap(compressedImage)
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when(item.itemId) {
            android.R.id.home -> {
                super.onBackPressed()
            }
            R.id.action_upload -> {
                saveImageToGallery(false)
            }
            R.id.action_save -> {
                saveImageToGallery(true)
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    //</editor-fold>
    //<editor-fold desc="FILTERS">

    override fun onFilterSelected(filter: Filter) {
        resetControls()
        filteredImage = compressedOriginalImage!!.copy(BITMAP_CONFIG, true)
        image_preview.setImageBitmap(filter.processFilter(filteredImage))
        compressedImage = filteredImage.copy(BITMAP_CONFIG, true)
        actualFilter = filter
    }

    private fun resetControls() {
        editImageFragment.resetControl()

        brightnessFinal = BRIGHTNESS_START
        saturationFinal = SATURATION_START
        contrastFinal = CONTRAST_START
    }


    //</editor-fold>
    //<editor-fold desc="EDITS">

    private fun applyFilterAndShowImage(filter: Filter, image: Bitmap?) {
        future?.cancel(true)
        future = executor.submit {
            val bitmap = filter.processFilter(image!!.copy(BITMAP_CONFIG, true))
            image_preview.post {
                image_preview.setImageBitmap(bitmap)
            }
        }
    }

    override fun onBrightnessChange(brightness: Int) {
        brightnessFinal = brightness
        val myFilter = Filter()
        myFilter.addEditFilters(brightness, saturationFinal, contrastFinal)
        applyFilterAndShowImage(myFilter, filteredImage)
    }

    override fun onSaturationChange(saturation: Float) {
        saturationFinal = saturation
        val myFilter = Filter()
        myFilter.addEditFilters(brightnessFinal, saturation, contrastFinal)
        applyFilterAndShowImage(myFilter, filteredImage)
    }

    override fun onContrastChange(contrast: Float) {
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

    override fun onEditStarted() {
    }

    override fun onEditCompleted() {
        val myFilter = Filter()
        myFilter.addEditFilters(brightnessFinal, saturationFinal, contrastFinal)
        val bitmap = filteredImage.copy(BITMAP_CONFIG, true)

        compressedImage = myFilter.processFilter(bitmap)
    }

    //</editor-fold>
    //<editor-fold desc="CROPPING">

    private fun startCrop() {
        applyFinalFilters(MediaStore.Images.Media.getBitmap(contentResolver, cropUri))
        val file = File.createTempFile("temp_crop_img", ".png", cacheDir)
        file.writeBitmap(finalImage)

        val uCrop: UCrop = UCrop.of(Uri.fromFile(file), URI.picture_uri!!)
        uCrop.start(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == Activity.RESULT_OK) {
            imageUri = data!!.data

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
            image_preview.setImageURI(resultCrop)

            val bitmap = (image_preview.drawable as BitmapDrawable).bitmap
            originalImage = bitmap.copy(Bitmap.Config.ARGB_8888, true)
            compressedImage = resizeImage(originalImage!!.copy(BITMAP_CONFIG, true))
            compressedOriginalImage = compressedImage!!.copy(BITMAP_CONFIG, true)
            filteredImage = compressedImage!!.copy(BITMAP_CONFIG, true)
            resetFilteredImage()
        } else {
            Toast.makeText(this, "Cannot retrieve image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleCropError(data: Intent?) {
        val resultError = UCrop.getError(data!!)
        if(resultError != null) {
            Toast.makeText(this, "" + resultError, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Unexpected Error", Toast.LENGTH_SHORT).show()
        }
    }

    //</editor-fold>
    //<editor-fold desc="FLOW">
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if(grantResults.size > 1
            && grantResults[0] == PackageManager.PERMISSION_GRANTED
            && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
            // permission was granted
            when (requestCode) {
                REQUEST_CODE_PERMISSIONS_SAVE_PHOTO -> permissionsGrantedToSave(true)
                REQUEST_CODE_PERMISSIONS_SEND_PHOTO -> permissionsGrantedToSave(false)
            }
        } else {
            Snackbar.make(coordinator_edit, "Permission denied", Snackbar.LENGTH_LONG).show()
        }
    }

    private fun applyFinalFilters(image: Bitmap?) {
        val editFilter = Filter().addEditFilters(brightnessFinal, saturationFinal, contrastFinal)

        finalImage = editFilter.processFilter(image!!.copy(BITMAP_CONFIG, true))
        if (actualFilter!=null) finalImage = actualFilter!!.processFilter(finalImage)
    }

    private fun uploadImage(file: File) {
        val intent = Intent (applicationContext, PostCreationActivity::class.java)
        intent.putExtra("picture_uri", Uri.fromFile(file))
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        applicationContext!!.startActivity(intent)
    }

    private fun saveImageToGallery(save: Boolean) {
        // runtime permission and process
        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(
                this,
                REQUIRED_PERMISSIONS,
                if(save) REQUEST_CODE_PERMISSIONS_SAVE_PHOTO else REQUEST_CODE_PERMISSIONS_SEND_PHOTO
            )
        } else {
            permissionsGrantedToSave(save)
        }
    }

    /**
     * Check if all permission specified in the manifest have been granted
     */
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            applicationContext, it) == PackageManager.PERMISSION_GRANTED
    }

    /** Use external media if it is available, our app's file directory otherwise */
    private fun getOutputDirectory(): File {
        val appContext = applicationContext
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, appContext.resources.getString(R.string.app_name)).apply { mkdirs() } }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else appContext.filesDir
    }

    private fun File.writeBitmap(bitmap: Bitmap) {
        outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 85, out)
            out.flush()
        }
    }

    private fun permissionsGrantedToSave(save: Boolean) {
        val file =
            if(!save){
                //put picture in cache
                File.createTempFile("temp_edit_img", ".png", cacheDir)
            } else{
                // Save the picture (quality is ignored for PNG)
                File(outputDirectory, SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
                    .format(System.currentTimeMillis()) + ".png")
            }
        try {
            applyFinalFilters(originalImage)
            file.writeBitmap(finalImage)
        } catch (e: IOException) {
            Snackbar.make(coordinator_edit, "Unable to save image", Snackbar.LENGTH_LONG).show()
        }

        if (!save) {
            uploadImage(file)
        } else {
            Snackbar.make(coordinator_edit, "Image succesfully saved", Snackbar.LENGTH_LONG).show()
        }
    }
    //</editor-fold>
}
