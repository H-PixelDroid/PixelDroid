package com.h.pixeldroid.postCreation.photoEdit

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import com.h.pixeldroid.R

class EditImageFragment : Fragment(),  SeekBar.OnSeekBarChangeListener {

    private var listener: PhotoEditActivity? = null

    private lateinit var seekbarBrightness: SeekBar
    private lateinit var seekbarSaturation: SeekBar
    private lateinit var seekbarContrast: SeekBar

    private var BRIGHTNESS_MAX = 200
    private var SATURATION_MAX = 20
    private var CONTRAST_MAX= 30
    private var BRIGHTNESS_START = BRIGHTNESS_MAX/2
    private var SATURATION_START = SATURATION_MAX/2
    private var CONTRAST_START = CONTRAST_MAX/2

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_edit_image, container, false)

        seekbarBrightness = view.findViewById(R.id.seekbar_brightness)
        seekbarSaturation = view.findViewById(R.id.seekbar_saturation)
        seekbarContrast = view.findViewById(R.id.seekbar_contrast)

        seekbarBrightness.max = BRIGHTNESS_MAX
        seekbarBrightness.progress = BRIGHTNESS_START

        seekbarContrast.max = CONTRAST_MAX
        seekbarContrast.progress = CONTRAST_START

        seekbarSaturation.max = SATURATION_MAX
        seekbarSaturation.progress = SATURATION_START

        setOnSeekBarChangeListeners(this)

        return view
    }

    private fun setOnSeekBarChangeListeners(listener: EditImageFragment?){
        seekbarBrightness.setOnSeekBarChangeListener(listener)
        seekbarContrast.setOnSeekBarChangeListener(listener)
        seekbarSaturation.setOnSeekBarChangeListener(listener)
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
        seekbarBrightness.progress = BRIGHTNESS_START
        seekbarContrast.progress = CONTRAST_START
        seekbarSaturation.progress = SATURATION_START
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
