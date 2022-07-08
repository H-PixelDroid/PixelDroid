package org.pixeldroid.app.settings

import android.content.Context
import android.content.DialogInterface
import android.content.res.TypedArray
import android.graphics.Color
import android.os.Bundle
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.preference.DialogPreference
import androidx.preference.PreferenceDialogFragmentCompat
import androidx.preference.PreferenceViewHolder
import org.pixeldroid.app.R
import org.pixeldroid.app.databinding.ColorDialogBinding
import org.pixeldroid.app.databinding.ImageCarouselBinding


/** AndroidX version created by yvolk@yurivolkov.com
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
        mPicker = picker
        return mPicker
    }

    override fun onStart() {
        super.onStart()
        val dialog: AlertDialog? = dialog as AlertDialog?
        if (preference.selectNoneButtonText != null && preference.defaultColor != null && mPicker != null && dialog != null) {
            // In order to prevent dialog from closing we setup its onLickListener this late
            dialog.getButton(DialogInterface.BUTTON_POSITIVE)?.setOnClickListener{
                        mPicker?.setCurrentColor(preference.defaultColor!!)
            }
        }
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

class ColorPreference constructor(context: Context, attrs: AttributeSet? = null) :
    DialogPreference(context, attrs) {
    var selectNoneButtonText: String? = null
    var defaultColor: Int? = null
    private var noneSelectedSummaryText: String? = null
    private val summaryText: CharSequence = super.getSummary()!!
    private var thumbnail: View? = null
    private val mPicker: ColorPickerView? = null

    init {

    }

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
            Integer.valueOf(getPersistedInt(Color.GRAY)) else defaultColor!!

    private fun showColor(color: Int?) {
        val thumbColor = color ?: defaultColor
        thumbnail?.visibility = if (thumbColor == null) View.GONE else View.VISIBLE
        thumbnail?.findViewById<ImageView>(R.id.colorPreview)?.setBackgroundColor(thumbColor ?: 0)
        if (noneSelectedSummaryText != null) {
            summary = if (thumbColor == null) noneSelectedSummaryText else summaryText
        }
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
                if (type == TypedValue.TYPE_STRING) {
                    return when(a.getString(index)){
                        "default" -> 0
                        "second" -> 1
                        "third" -> 2
                        else -> null
                    }
                }
            }
            return null
        }

        private fun parseDefaultValue(defaultValue: Any?): Int {
            return if (defaultValue == null) 0 else if (defaultValue is Int) defaultValue else 0
        }
    }
}
class ColorPickerView @JvmOverloads constructor(context: Context?, attrs: AttributeSet? = null) : FrameLayout(context!!, attrs) {
    var binding: ColorDialogBinding

    fun setCurrentColor(defaultColor: Int) {
        Toast.makeText(context, "test $defaultColor", Toast.LENGTH_LONG).show()
    }

    init {
        binding = ColorDialogBinding.inflate(LayoutInflater.from(context),this,  true)
    }
    /** Returns the color selected by the user  */
    /** Sets the original color swatch and the current color to the specified value.  */
    var color: Int = 1
}
