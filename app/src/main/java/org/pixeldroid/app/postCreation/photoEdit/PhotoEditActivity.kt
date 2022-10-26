package org.pixeldroid.app.postCreation.photoEdit

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import com.yalantis.ucrop.UCrop
import com.zomato.photofilters.imageprocessors.Filter
import com.zomato.photofilters.imageprocessors.subfilters.BrightnessSubFilter
import com.zomato.photofilters.imageprocessors.subfilters.ContrastSubFilter
import com.zomato.photofilters.imageprocessors.subfilters.SaturationSubfilter
import org.pixeldroid.app.R
import org.pixeldroid.app.databinding.ActivityPhotoEditBinding
import org.pixeldroid.app.postCreation.PostCreationActivity
import org.pixeldroid.app.utils.BaseThemedWithBarActivity
import org.pixeldroid.app.utils.bitmapFromUri
import org.pixeldroid.app.utils.getColorFromAttr
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

class PhotoEditActivity : BaseThemedWithBarActivity() {

    var saving: Boolean = false
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

    private var picturePosition: Int? = null

    private var brightnessFinal = BRIGHTNESS_START
    private var saturationFinal = SATURATION_START
    private var contrastFinal = CONTRAST_START

    init {
        System.loadLibrary("NativeImageProcessor")
    }

    companion object{
        internal const val PICTURE_URI = "picture_uri"
        internal const val PICTURE_POSITION = "picture_position"
        internal const val PICTURE_DESCRIPTION = "picture_description"
        internal const val TEMP_FILES = "temp_files"
        internal const val POST_REDRAFT = "post_redraft"

        private var executor: ExecutorService = newSingleThreadExecutor()
        private var future: Future<*>? = null

        private var saveExecutor: ExecutorService = newSingleThreadExecutor()
        private var saveFuture: Future<*>? = null

        private var initialUri: Uri? = null
        internal var imageUri: Uri? = null
    }

    private lateinit var binding: ActivityPhotoEditBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhotoEditBinding.inflate(layoutInflater)

        setContentView(binding.root)


        supportActionBar?.setTitle(R.string.toolbar_title_edit)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)

        initialUri = intent.getParcelableExtra(PICTURE_URI)
        picturePosition = intent.getIntExtra(PICTURE_POSITION, 0)
        imageUri = initialUri
        
        // Crop button on-click listener
        binding.cropImageButton.setOnClickListener {
            startCrop()
        }

        loadImage()

        setupViewPager(binding.viewPager)
    }

    private fun loadImage() {
        originalImage = bitmapFromUri(contentResolver, imageUri)

        compressedImage = resizeImage(originalImage!!)
        compressedOriginalImage = compressedImage!!.copy(BITMAP_CONFIG, true)
        filteredImage = compressedImage!!.copy(BITMAP_CONFIG, true)
        Glide.with(this).load(compressedImage).into(binding.imagePreview)
    }

    private fun resizeImage(image: Bitmap): Bitmap {
        val display = windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)

        val newY = size.y * 0.7
        val scale = newY / image.height
        return Bitmap.createScaledBitmap(image, (image.width * scale).toInt(), newY.toInt(), true)
    }

    private fun setupViewPager(viewPager: ViewPager2) {
        filterListFragment = FilterListFragment()
        filterListFragment.setListener(::onFilterSelected)

        editImageFragment = EditImageFragment()
        editImageFragment.setListener(this)

        val tabs: List<() -> Fragment> = listOf({ filterListFragment }, { editImageFragment })

        // Keep both tabs loaded at all times because values are needed there
        viewPager.offscreenPageLimit = 1

        //Disable swiping in viewpager
        viewPager.isUserInputEnabled = false

        viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun createFragment(position: Int): Fragment {
                return tabs[position]()
            }

            override fun getItemCount(): Int {
                return tabs.size
            }
        }
        TabLayoutMediator(binding.tabs, viewPager) { tab, position ->
            tab.setText(when(position) {
                0 -> R.string.tab_filters
                else -> R.string.edit
            })
        }.attach()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.edit_menu, menu)
        return true
    }

    override fun onStop() {
        super.onStop()
        saving = false
    }

    override fun onBackPressed() {
        if (noEdits()) super.onBackPressed()
        else {
            val builder = AlertDialog.Builder(this)
            builder.apply {
                setMessage(R.string.save_before_returning)
                setPositiveButton(android.R.string.ok) { _, _ ->
                    saveImageToGallery()
                }
                setNegativeButton(R.string.no_cancel_edit) { _, _ ->
                    super.onBackPressed()
                }
            }
            // Create the AlertDialog
            builder.show()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when(item.itemId) {
            android.R.id.home -> onBackPressed()
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
        binding.imagePreview.setImageBitmap(filter.processFilter(filteredImage))
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
            binding.imagePreview.post {
                binding.imagePreview.setImageBitmap(bitmap)
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
            setStatusBarColor(this@PhotoEditActivity.getColorFromAttr(R.attr.colorPrimaryDark))
            setToolbarWidgetColor(this@PhotoEditActivity.getColorFromAttr(R.attr.colorOnSurface))
            setToolbarColor(this@PhotoEditActivity.getColorFromAttr(R.attr.colorSurface))
            setActiveControlsWidgetColor(this@PhotoEditActivity.getColorFromAttr(R.attr.colorPrimary))
            setFreeStyleCropEnabled(true)
        }
        val uCrop: UCrop = UCrop.of(initialUri!!, Uri.fromFile(file)).withOptions(options)
        uCrop.start(this)
    }

    @Deprecated("Deprecated in Java")
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
            binding.imagePreview.setImageURI(resultCrop)
            val bitmap = (binding.imagePreview.drawable as BitmapDrawable).bitmap
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
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(grantResults.size > 1
            && grantResults[0] == PackageManager.PERMISSION_GRANTED
            && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
            // permission was granted
            permissionsGrantedToSave()
        } else {
            Snackbar.make(binding.root, getString(R.string.permission_denied),
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
            putExtra(PICTURE_URI, file)
            putExtra(PICTURE_POSITION, picturePosition)
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
        binding.progressBarSaveFile.visibility = VISIBLE
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
                        binding.progressBarSaveFile.visibility = GONE
                        saving = false
                    }
                }
            } catch (e: IOException) {
                this.runOnUiThread {
                    Snackbar.make(
                        binding.root, getString(R.string.save_image_failed),
                        Snackbar.LENGTH_LONG
                    ).show()
                    binding.progressBarSaveFile.visibility = GONE
                    saving = false
                }
            }
        }
    }
}
