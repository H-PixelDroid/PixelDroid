package org.pixeldroid.app.postCreation.photoEdit

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import org.pixeldroid.app.R
import org.pixeldroid.app.databinding.FragmentEditImageBinding

class EditImageFragment : Fragment(),  SeekBar.OnSeekBarChangeListener {

    private var listener: PhotoEditActivity? = null
    private lateinit var binding: FragmentEditImageBinding

    private var BRIGHTNESS_MAX = 200
    private var SATURATION_MAX = 20
    private var CONTRAST_MAX= 30
    private var BRIGHTNESS_START = BRIGHTNESS_MAX/2
    private var SATURATION_START = SATURATION_MAX/2
    private var CONTRAST_START = CONTRAST_MAX/2

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentEditImageBinding.inflate(inflater, container, false)

        binding.seekbarBrightness.max = BRIGHTNESS_MAX
        binding.seekbarBrightness.progress = BRIGHTNESS_START

        binding.seekbarContrast.max = CONTRAST_MAX
        binding.seekbarContrast.progress = CONTRAST_START

        binding.seekbarSaturation.max = SATURATION_MAX
        binding.seekbarSaturation.progress = SATURATION_START

        setOnSeekBarChangeListeners(this)

        return binding.root
    }

    private fun setOnSeekBarChangeListeners(listener: EditImageFragment?){
        binding.seekbarBrightness.setOnSeekBarChangeListener(listener)
        binding.seekbarContrast.setOnSeekBarChangeListener(listener)
        binding.seekbarSaturation.setOnSeekBarChangeListener(listener)
    }

    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        var prog = progress

        listener?.let {
            when(seekBar!!.id) {
                R.id.seekbar_brightness -> it.onBrightnessChange(progress - 100)
                R.id.seekbar_saturation -> {
                    prog += 10
                    it.onSaturationChange(.10f * prog)
                }
                R.id.seekbar_contrast -> {
                    it.onContrastChange(.10f * prog)
                }
            }
        }
    }

    fun resetControl() {
        // Make sure to ignore seekbar change events, since we don't want to have the reset cause
        // filter applications due to the onProgressChanged calls
        setOnSeekBarChangeListeners(null)
        binding.seekbarBrightness.progress = BRIGHTNESS_START
        binding.seekbarContrast.progress = CONTRAST_START
        binding.seekbarSaturation.progress = SATURATION_START
        setOnSeekBarChangeListeners(this)
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {
        listener?.onEditStarted()
    }

    override fun onStopTrackingTouch(seekBar: SeekBar?) {
        listener?.onEditCompleted()
    }

    fun setListener(listener: PhotoEditActivity) {
        this.listener = listener
    }
}
