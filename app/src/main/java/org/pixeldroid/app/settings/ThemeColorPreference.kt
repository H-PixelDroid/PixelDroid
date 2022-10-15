package org.pixeldroid.app.settings

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.TypedArray
import android.os.Build
import android.os.Bundle
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.annotation.ColorRes
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.preference.DialogPreference
import androidx.preference.PreferenceDialogFragmentCompat
import androidx.preference.PreferenceViewHolder
import org.pixeldroid.app.R
import org.pixeldroid.app.databinding.ColorDialogBinding


/** Inspired by https://github.com/andstatus/todoagenda's color chooser.
 * AndroidX version created by yvolk@yurivolkov.com
 * based on this answer: https://stackoverflow.com/a/53290775/297710
 * and on the code of https://github.com/koji-1009/ChronoDialogPreference
 */
class ColorPreferenceDialog(preference: ColorPreference) :
    PreferenceDialogFragmentCompat() {
    private val preference: ColorPreference
    private var mPicker: ColorPickerView? = null

    init {
        this.preference = preference
        val b = Bundle()
        b.putString(ARG_KEY, preference.key)
        arguments = b
    }

    override fun onCreateDialogView(context: Context): View? {
        val picker = ColorPickerView(context)
        preference.color?.let {
            picker.color = it
        }
        mPicker = picker
        return mPicker
    }

    override fun onPrepareDialogBuilder(builder: AlertDialog.Builder) {
        if (preference.selectNoneButtonText != null) {
            builder.setNeutralButton(preference.selectNoneButtonText, null)
        }
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult) {
            val color: Int = mPicker!!.color
            if (preference.callChangeListener(color)) {
                preference.color = color
            }
        }
    }
}

class ColorPreference constructor(context: Context, attrs: AttributeSet? = null) : DialogPreference(context, attrs) {
    var selectNoneButtonText: String? = null
    var defaultColor: Int? = null
    private var thumbnail: View? = null

    override fun onBindViewHolder(viewHolder: PreferenceViewHolder) {
        thumbnail = addThumbnail(viewHolder.itemView)
        showColor(persistedIntDefaultOrNull)
        // Only call after showColor sets any summary text:
        super.onBindViewHolder(viewHolder)
    }

    override fun onGetDefaultValue(a: TypedArray, index: Int): Any? {
        defaultColor = readDefaultValue(a, index)
        return defaultColor
    }

    override fun setDefaultValue(defaultValue: Any?) {
        super.setDefaultValue(defaultValue)
        defaultColor = parseDefaultValue(defaultValue)
    }

    override fun onSetInitialValue(defaultValue: Any?) {
        color = defaultValue?.let {
            parseDefaultValue(defaultValue)
        } ?: color
    }

    private fun addThumbnail(view: View): View {
        val widgetFrameView: LinearLayout = view.findViewById(android.R.id.widget_frame)
        widgetFrameView.visibility = View.VISIBLE
        widgetFrameView.removeAllViews()
        LayoutInflater.from(context).inflate(R.layout.color_preference_thumbnail, widgetFrameView)
        return widgetFrameView.findViewById(R.id.thumbnail)
    }

    private val persistedIntDefaultOrNull: Int
        get() = if (shouldPersist() && sharedPreferences?.contains(key) == true)
            Integer.valueOf(getPersistedInt(0)) else defaultColor!!

    private fun showColor(color: Int?) {
        val thumbColor = color ?: defaultColor
        thumbnail?.visibility = if (thumbColor == null) View.GONE else View.VISIBLE
        @ColorRes
        val colorCode: Int = when(thumbColor){
            -1 -> android.R.color.transparent
            1 -> R.color.seed2
            2 -> R.color.seed3
            3 -> R.color.seed4
            else -> R.color.seed
        }
        thumbnail?.findViewById<ImageView>(R.id.colorPreview)?.imageTintList = ColorStateList.valueOf(
            ContextCompat.getColor(context, colorCode)
        )
    }

    private fun removeSetting() {
        if (shouldPersist()) {
            sharedPreferences
                ?.edit()
                ?.remove(key)
                ?.apply()
        }
    }


    var color: Int?
        get() = persistedIntDefaultOrNull
        set(color) {
            if (color == null) {
                removeSetting()
            } else {
                persistInt(color)
            }
            showColor(color)
        }

    companion object {
        private fun readDefaultValue(a: TypedArray, index: Int): Int? {
            if (a.peekValue(index) != null) {
                val type = a.peekValue(index).type
                if (TypedValue.TYPE_FIRST_INT <= type && type <= TypedValue.TYPE_LAST_INT) {
                    return a.getInt(index, 0)
                }
            }
            return null
        }

        private fun parseDefaultValue(defaultValue: Any?): Int {
            return if (defaultValue == null) 0 else if (defaultValue is Int) defaultValue else 0
        }
    }
}
class ColorPickerView(context: Context?, attrs: AttributeSet? = null) : FrameLayout(context!!, attrs) {
    var binding: ColorDialogBinding

    init {
        binding = ColorDialogBinding.inflate(LayoutInflater.from(context),this,  true)
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
            binding.dynamicColorSwitch.isVisible = true
            binding.dynamicColorSwitch.setOnCheckedChangeListener{ _, isChecked ->
                binding.themeChooser.isVisible = !isChecked
                color = if(isChecked) -1 else 0
            }
        }
        binding.theme1.setOnClickListener { color = 0 }
        binding.theme2.setOnClickListener { color = 1 }
        binding.theme3.setOnClickListener { color = 2 }
        binding.theme4.setOnClickListener { color = 3 }
    }

    private fun changeConstraint(button2: View) {
        binding.chosenTheme.isVisible = true
        val params = binding.chosenTheme.layoutParams as ConstraintLayout.LayoutParams
        params.endToEnd = button2.id
        params.startToStart = button2.id
        binding.chosenTheme.layoutParams = params
        binding.chosenTheme.requestLayout()
    }
    /** Returns the color selected by the user  */
    /** Sets the original color swatch and the current color to the specified value.  */
    var color: Int = 0
        set(value) {
            field = value
            when(value) {
                0 -> binding.theme1
                1 -> binding.theme2
                2 -> binding.theme3
                3 -> binding.theme4
                else -> null
            }?.let { changeConstraint(it) }

            // Check switch if set to dynamic
            binding.dynamicColorSwitch.isChecked = value == -1
        }
}
