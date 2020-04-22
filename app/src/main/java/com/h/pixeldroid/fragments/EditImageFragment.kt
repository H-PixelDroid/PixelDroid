package com.h.pixeldroid.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import com.h.pixeldroid.R
import com.h.pixeldroid.interfaces.EditImageFragmentListener
import kotlinx.android.synthetic.main.fragment_edit_image.*
import kotlinx.android.synthetic.main.fragment_feed.*

class EditImageFragment : Fragment(),  SeekBar.OnSeekBarChangeListener {

    private var listener: EditImageFragmentListener? = null

    internal lateinit var seekbar_brightness: SeekBar
    internal lateinit var seekbar_saturation: SeekBar
    internal lateinit var seekbar_contrast: SeekBar

    private var BRIGHTNESS_START = 100
    private var SATURATION_START = 10
    private var CONTRAST_START = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_edit_image, container, false)

        seekbar_brightness = view.findViewById<SeekBar>(R.id.seekbar_brightness)
        seekbar_saturation = view.findViewById<SeekBar>(R.id.seekbar_saturation)
        seekbar_contrast = view.findViewById<SeekBar>(R.id.seekbar_contrast)

        seekbar_brightness.max = 200
        seekbar_brightness.progress = BRIGHTNESS_START

        seekbar_contrast.max = 20
        seekbar_contrast.progress = CONTRAST_START

        seekbar_saturation.max = 30
        seekbar_saturation.progress = SATURATION_START

        seekbar_brightness.setOnSeekBarChangeListener(this)
        seekbar_contrast.setOnSeekBarChangeListener(this)
        seekbar_saturation.setOnSeekBarChangeListener(this)

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
                    listener!!.onSaturationChange(tempProgress)
                }
            }
        }
    }

    fun resetControl() {
        seekbar_brightness.progress = BRIGHTNESS_START
        seekbar_contrast.progress = CONTRAST_START
        seekbar_saturation.progress = SATURATION_START
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }
}
