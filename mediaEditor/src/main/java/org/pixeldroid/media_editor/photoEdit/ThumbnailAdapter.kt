package org.pixeldroid.media_editor.photoEdit

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.zomato.photofilters.utils.ThumbnailItem
import org.pixeldroid.media_editor.R
import org.pixeldroid.media_editor.databinding.ThumbnailListItemBinding

class ThumbnailAdapter (private val context: Context,
                        private val tbItemList: List<ThumbnailItem>,
                        private val listener: FilterListFragment
): RecyclerView.Adapter<ThumbnailAdapter.MyViewHolder>() {

    private var selectedIndex = 0

    fun resetSelected(){
        selectedIndex = 0
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemBinding = ThumbnailListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(itemBinding)
    }

    override fun getItemCount(): Int {
        return tbItemList.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val tbItem = tbItemList[position]
        holder.thumbnail.setImageBitmap(tbItem.image)
        holder.thumbnail.setOnClickListener {
            listener.onFilterSelected(tbItem.filter)
            selectedIndex = holder.bindingAdapterPosition
            notifyDataSetChanged()
        }

        holder.filterName.text = tbItem.filterName

        if(selectedIndex == position)
            holder.filterName.setTextColor(context.getColorFromAttr(R.attr.colorPrimary))
        else
            holder.filterName.setTextColor(context.getColorFromAttr(R.attr.colorOnBackground))
    }

    class MyViewHolder(itemBinding: ThumbnailListItemBinding): RecyclerView.ViewHolder(itemBinding.root) {
        var thumbnail: ImageView = itemBinding.thumbnail
        var filterName: TextView = itemBinding.filterName
    }
}