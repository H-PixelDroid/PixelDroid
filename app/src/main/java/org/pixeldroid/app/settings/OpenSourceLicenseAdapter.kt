package org.pixeldroid.app.settings

import android.annotation.SuppressLint
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import org.pixeldroid.app.databinding.OpenSourceItemBinding
import org.pixeldroid.app.settings.licenseObjects.OpenSourceItem

class OpenSourceLicenseAdapter(private val openSourceItems: List<OpenSourceItem>) :
    RecyclerView.Adapter<OpenSourceLicenseAdapter.OpenSourceLicenceViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OpenSourceLicenceViewHolder
        {
            val itemBinding = OpenSourceItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return OpenSourceLicenceViewHolder(itemBinding)
        }

    override fun onBindViewHolder(holder: OpenSourceLicenceViewHolder, position: Int) {
        val item = openSourceItems[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = openSourceItems.size

    class OpenSourceLicenceViewHolder(val binding: OpenSourceItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        fun bind(item: OpenSourceItem) {
            with(binding) {
                if (!item.libraryName.isNullOrEmpty()) {
                    title.isVisible = true
                    title.text = "${item.libraryName}"
                } else {
                    title.isVisible = false
                }
                val license = item.license
                if (license != null) {
                    val licenseUrl = item.licenseUrl?.let { " (${it} )" } ?: ""
                    copyright.isVisible = true
                    copyright.apply {
                        text = "$license$licenseUrl"
                        movementMethod = LinkMovementMethod.getInstance()
                    }
                } else {
                    copyright.isVisible = false
                }
                if (item.url != null || item.copyrightHolder != null) {
                    val licenseUrl = item.url?.let { " (${it} )" } ?: ""
                    url.isVisible = true
                    url.apply {
                        text = "${item.copyrightHolder ?: ""}$licenseUrl"
                        movementMethod = LinkMovementMethod.getInstance()
                    }
                } else {
                    url.isVisible = false
                }
            }
        }
    }
}