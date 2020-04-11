package com.h.pixeldroid.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.h.pixeldroid.R
import com.h.pixeldroid.interfaces.FilterListFragmentListener
import com.zomato.photofilters.utils.ThumbnailItem
import kotlinx.android.synthetic.main.thumbnail_list_item.view.*

class ThumbnailAdapter (private val context: Context,
                        private val tbItemList: List<ThumbnailItem>,
                        private val listener: FilterListFragmentListener): RecyclerView.Adapter<ThumbnailAdapter.MyViewHolder>() {

    private var selectedIndex = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView = LayoutInflater.from(context).inflate(R.layout.thumbnail_list_item, parent, false)
        return MyViewHolder(itemView)
    }

    override fun getItemCount(): Int {
        return tbItemList.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val tbItem = tbItemList[position]
        holder.thumbnail.setImageBitmap(tbItem.image)
        holder.thumbnail.setOnClickListener {
            listener.onFilterSelected(tbItem.filter)
            selectedIndex = position
            notifyDataSetChanged()
        }

        holder.filterName.text = tbItem.filterName

        if(selectedIndex == position)
            holder.filterName.setTextColor(ContextCompat.getColor(context, R.color.filterLabelSelected))
        else
            holder.filterName.setTextColor(ContextCompat.getColor(context, R.color.filterLabelNormal))
    }

    class MyViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        var thumbnail: ImageView
        var filterName: TextView

        init {
            thumbnail = itemView.thumbnail
            filterName = itemView.filter_name
        }
    }
}