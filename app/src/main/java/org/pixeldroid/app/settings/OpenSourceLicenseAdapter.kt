package org.pixeldroid.app.settings

import android.annotation.SuppressLint
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.entity.Library
import org.pixeldroid.app.databinding.OpenSourceItemBinding

class OpenSourceLicenseAdapter(private val openSourceItems: Libs) :
    RecyclerView.Adapter<OpenSourceLicenseAdapter.OpenSourceLicenceViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OpenSourceLicenceViewHolder
        {
            val itemBinding = OpenSourceItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return OpenSourceLicenceViewHolder(itemBinding)
        }

    override fun onBindViewHolder(holder: OpenSourceLicenceViewHolder, position: Int) {
        val item = openSourceItems.libraries[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = openSourceItems.libraries.size

    class OpenSourceLicenceViewHolder(val binding: OpenSourceItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        fun bind(item: Library) {
            with(binding) {
                if (item.name.isNotEmpty()) {
                    title.isVisible = true
                    title.text = item.name
                } else {
                    title.isVisible = false
                }
                val license = item.licenses.firstOrNull()
                val licenseName = license?.name ?: ""
                val licenseUrl = license?.url?.let { " (${it} )" } ?: ""
                copyright.isVisible = true
                copyright.apply {
                    text = "$licenseName$licenseUrl"
                    movementMethod = LinkMovementMethod.getInstance()
                }
                url.isVisible = true
                url.apply {
                    text = "${item.developers.firstOrNull()?.name ?: ""} ${item.website}"
                    movementMethod = LinkMovementMethod.getInstance()
                }
            }
        }
    }
}