package org.pixeldroid.app.settings

import android.app.Dialog
import android.content.res.XmlResourceParser
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.pixeldroid.app.R

class LanguageSettingFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val list: MutableList<String> = mutableListOf()
        // IDE doesn't find it, but compiling works apparently?
        resources.getXml(R.xml._generated_res_locale_config).use {
            var eventType = it.eventType
            while (eventType != XmlResourceParser.END_DOCUMENT) {
                when (eventType) {
                    XmlResourceParser.START_TAG -> {
                        if (it.name == "locale") {
                            list.add(it.getAttributeValue(0))
                        }
                    }
                }
                eventType = it.next()
            }
        }
        val locales = AppCompatDelegate.getApplicationLocales()
        val checkedItem: Int =
            if(locales.isEmpty) 0
            else {
                // For some reason we get a bit inconsistent language tags. This normalises it for
                // the currently used languages, but it might break in the future if we add some
                val index = list.indexOf(locales.get(0)?.toLanguageTag()?.lowercase()?.replace('_', '-'))
                // If found, we want to compensate for the first in the list being the default
                if(index == -1) -1
                else index + 1
            }

        return MaterialAlertDialogBuilder(requireContext()).apply {
            setIcon(R.drawable.translate_black_24dp)
            setTitle(R.string.language)
            setSingleChoiceItems((mutableListOf(getString(R.string.default_system)) + list.map {
                val appLocale = LocaleListCompat.forLanguageTags(it)
                appLocale.get(0)!!.getDisplayName(appLocale.get(0)!!)
            }).toTypedArray(), checkedItem) { dialog, which ->
                val languageTag = if(which in 1..list.size) list[which - 1] else null
                dialog.dismiss()
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageTag))
            }
            setNegativeButton(android.R.string.ok) { _, _ -> }
        }.create()
    }
}