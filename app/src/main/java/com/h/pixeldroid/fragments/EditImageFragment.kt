package com.h.pixeldroid.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import com.h.pixeldroid.R
import com.h.pixeldroid.interfaces.EditImageFragmentListener

class EditImageFragment : Fragment(),  SeekBar.OnSeekBarChangeListener {

    private var listener: EditImageFragmentListener? = null

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

        seekbarBrightness.setOnSeekBarChangeListener(this)
        seekbarContrast.setOnSeekBarChangeListener(this)
        seekbarSaturation.setOnSeekBarChangeListener(this)

        return view
    }

    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        var prog = progress

        if(listener != null) {
            when(seekBar!!.id) {
                R.id.seekbar_brightness -> listener!!.onBrightnessChange(progress - 100)
                R.id.seekbar_saturation -> {
                    prog += 10
                    val tempProgress = .10f * prog
                    listener!!.onSaturationChange(tempProgress)
                }
                R.id.seekbar_contrast -> {
                    val tempProgress = .10f * prog
                    listener!!.onContrastChange(tempProgress)
                }
            }
        }
    }

    fun resetControl() {
        seekbarBrightness.progress = BRIGHTNESS_START
        seekbarContrast.progress = CONTRAST_START
        seekbarSaturation.progress = SATURATION_START
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {
        if(listener != null)
            listener!!.onEditStarted()
    }

    override fun onStopTrackingTouch(seekBar: SeekBar?) {
        if(listener != null)
            listener!!.onEditCompleted()
    }

    fun setListener(listener: EditImageFragmentListener) {
        this.listener = listener
    }
}
