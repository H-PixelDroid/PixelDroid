package com.h.pixeldroid.fragments

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.h.pixeldroid.PhotoEditActivity
import com.h.pixeldroid.R
import com.h.pixeldroid.adapters.ThumbnailAdapter
import com.h.pixeldroid.interfaces.FilterListFragmentListener
import com.h.pixeldroid.utils.BitmapUtils
import com.h.pixeldroid.utils.SpaceItemDecoration
import com.zomato.photofilters.FilterPack
import com.zomato.photofilters.imageprocessors.Filter
import com.zomato.photofilters.utils.ThumbnailItem
import com.zomato.photofilters.utils.ThumbnailsManager
import kotlinx.android.synthetic.main.fragment_filter_list.*

class FilterListFragment : Fragment(), FilterListFragmentListener {

    internal lateinit var recycler_view: RecyclerView
    internal var listener : FilterListFragmentListener? = null
    internal lateinit var adapter: ThumbnailAdapter
    internal lateinit var tbItemList: MutableList<ThumbnailItem>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view =  inflater.inflate(R.layout.fragment_filter_list, container, false)

        tbItemList = ArrayList()
        adapter = ThumbnailAdapter(activity!!, tbItemList, this)

        recycler_view = view.findViewById<RecyclerView>(R.id.recycler_view)
        recycler_view.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
        recycler_view.itemAnimator = DefaultItemAnimator()

        val space = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8f, resources.displayMetrics).toInt()
        recycler_view.addItemDecoration(SpaceItemDecoration(space))
        recycler_view.adapter = adapter

        displayImage(null)

        return view
    }

    fun displayImage(bitmap: Bitmap?) {
        val r = Runnable {
            val tbImage: Bitmap?
            if (bitmap == null) {
                Log.i("testImage", PhotoEditActivity.NAME.IMAGE_NAME)
                tbImage = BitmapUtils.getBitmapFromAssets(activity!!, PhotoEditActivity.NAME.IMAGE_NAME, 100, 100)
            } else {
                tbImage = Bitmap.createScaledBitmap(bitmap, 100, 100, false)
            }

            if (tbImage == null)
                return@Runnable

            ThumbnailsManager.clearThumbs()
            tbItemList.clear()

            val tbItem = ThumbnailItem()
            tbItem.image = tbImage
            tbItem.filterName = "Normal"
            ThumbnailsManager.addThumb(tbItem)

            val filters = FilterPack.getFilterPack(activity!!)

            for (filter in filters) {
                val item = ThumbnailItem()
                item.image = tbImage
                item.filter = filter
                item.filterName = filter.name
                ThumbnailsManager.addThumb(item)
            }

            tbItemList.addAll(ThumbnailsManager.processThumbs(activity))
            activity!!.runOnUiThread{ adapter.notifyDataSetChanged() }
        }

        Thread(r).start()
    }

    override fun onFilterSelected(filter: Filter) {
        if(listener != null ){
            listener!!.onFilterSelected(filter)
        }
    }

    fun setListener(listFragmentListener: FilterListFragmentListener) {
        this.listener = listFragmentListener
    }
}
