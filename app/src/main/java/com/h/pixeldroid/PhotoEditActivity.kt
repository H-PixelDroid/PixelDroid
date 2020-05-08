package com.h.pixeldroid

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.h.pixeldroid.adapters.EditPhotoViewPagerAdapter
import com.h.pixeldroid.fragments.EditImageFragment
import com.h.pixeldroid.fragments.FilterListFragment
import com.h.pixeldroid.interfaces.EditImageFragmentListener
import com.h.pixeldroid.interfaces.FilterListFragmentListener
import com.h.pixeldroid.utils.NonSwipeableViewPager
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

// This is an arbitrary number we are using to keep track of the permission
// request. Where an app has multiple context for requesting permission,
// this can help differentiate the different contexts.
private const val REQUEST_CODE_PERMISSIONS_SAVE_PHOTO = 8
private const val REQUEST_CODE_PERMISSIONS_SEND_PHOTO = 7
private val REQUIRED_PERMISSIONS = arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE,
    android.Manifest.permission.WRITE_EXTERNAL_STORAGE)

class PhotoEditActivity : AppCompatActivity(), FilterListFragmentListener, EditImageFragmentListener {

    val BITMAP_CONFIG = Bitmap.Config.ARGB_8888

    private var originalImage: Bitmap? = null
    private lateinit var filteredImage: Bitmap
    private lateinit var finalImage: Bitmap

    private lateinit var filterListFragment: FilterListFragment
    private lateinit var editImageFragment: EditImageFragment

    private lateinit var outputDirectory: File

    lateinit var viewPager: NonSwipeableViewPager
    lateinit var tabLayout: TabLayout

    private var brightnessFinal = 0
    private var saturationFinal = 1.0f
    private var contrastFinal = 1.0f

    private var resultUri: Uri? = null

    object URI {var picture_uri: Uri? = null}

    init {
        System.loadLibrary("NativeImageProcessor")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_edit)

        URI.picture_uri = intent.getParcelableExtra("picture_uri")

        resultUri = URI.picture_uri

        setSupportActionBar(toolbar)
        supportActionBar!!.title = "Edit"
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setHomeButtonEnabled(true)

        loadImage()

        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabs)
        setupViewPager(viewPager)
        tabLayout.setupWithViewPager(viewPager)
        outputDirectory = getOutputDirectory()
    }

    /** Use external media if it is available, our app's file directory otherwise */
    private fun getOutputDirectory(): File {
        val appContext = applicationContext
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, appContext.resources.getString(R.string.app_name)).apply { mkdirs() } }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else appContext.filesDir
    }

    private fun loadImage() {
        originalImage = MediaStore.Images.Media.getBitmap(contentResolver, URI.picture_uri)

        filteredImage = originalImage!!.copy(BITMAP_CONFIG, true)
        finalImage = originalImage!!.copy(BITMAP_CONFIG, true)
        image_preview.setImageBitmap(originalImage)
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

    private fun uploadImage(file: File) {
        val intent = Intent (applicationContext, PostCreationActivity::class.java)
        intent.putExtra("picture_uri", Uri.fromFile(file))
        //file.delete()
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

    private fun File.writeBitmap(bitmap: Bitmap) {
        outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 85, out)
            out.flush()
        }
    }

    private fun permissionsGrantedToSave(save: Boolean) {
        val file = if(!save){
            //put picture in cache
            File.createTempFile("temp_img", ".png", cacheDir)
        } else{
            // Save the picture (quality is ignored for PNG)
            File(outputDirectory, SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
                    .format(System.currentTimeMillis()) + ".png")
        }
        try {
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

    override fun onFilterSelected(filter: Filter) {
        resetControls()
        filteredImage = originalImage!!.copy(BITMAP_CONFIG, true)
        image_preview.setImageBitmap(filter.processFilter(filteredImage))
        finalImage = filteredImage.copy(BITMAP_CONFIG, true)
    }

    private fun resetControls() {
        editImageFragment.resetControl()

        brightnessFinal = 0
        saturationFinal = 1.0f
        contrastFinal = 1.0f
    }

    override fun onBrightnessChange(brightness: Int) {
        brightnessFinal = brightness
        val myFilter = Filter()
        myFilter.addSubFilter(BrightnessSubFilter(brightness))
        image_preview.setImageBitmap(myFilter.processFilter(finalImage.copy(BITMAP_CONFIG, true)))
    }

    override fun onSaturationChange(saturation: Float) {
        saturationFinal = saturation
        val myFilter = Filter()
        myFilter.addSubFilter(SaturationSubfilter(saturation))
        image_preview.setImageBitmap(myFilter.processFilter(finalImage.copy(BITMAP_CONFIG, true)))
    }

    override fun onContrastChange(contrast: Float) {
        contrastFinal = contrast
        val myFilter = Filter()
        myFilter.addSubFilter(ContrastSubFilter(contrast))
        image_preview.setImageBitmap(myFilter.processFilter(finalImage.copy(BITMAP_CONFIG, true)))
    }

    override fun onEditStarted() {

    }

    override fun onEditCompleted() {
        val bitmap = filteredImage.copy(BITMAP_CONFIG, true)
        val myFilter = Filter()
        myFilter.addSubFilter(ContrastSubFilter(contrastFinal))
        myFilter.addSubFilter(SaturationSubfilter(saturationFinal))
        myFilter.addSubFilter(BrightnessSubFilter(brightnessFinal))

        finalImage = myFilter.processFilter(bitmap)
    }
}
