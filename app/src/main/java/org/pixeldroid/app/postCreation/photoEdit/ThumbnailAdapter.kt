package org.pixeldroid.app.postCreation.photoEdit

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import org.pixeldroid.app.R
import org.pixeldroid.app.databinding.ThumbnailListItemBinding
import com.zomato.photofilters.utils.ThumbnailItem

class ThumbnailAdapter (private val context: Context,
                        private val tbItemList: List<ThumbnailItem>,
                        private val listener: FilterListFragment): RecyclerView.Adapter<ThumbnailAdapter.MyViewHolder>() {

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
            holder.filterName.setTextColor(ContextCompat.getColor(context, R.color.filterLabelSelected))
        else
            holder.filterName.setTextColor(ContextCompat.getColor(context, R.color.filterLabelNormal))
    }

    class MyViewHolder(itemBinding: ThumbnailListItemBinding): RecyclerView.ViewHolder(itemBinding.root) {
        var thumbnail: ImageView = itemBinding.thumbnail
        var filterName: TextView = itemBinding.filterName
    }
}