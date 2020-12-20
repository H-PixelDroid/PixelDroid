package com.h.pixeldroid.fragments

import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.h.pixeldroid.PhotoEditActivity
import com.h.pixeldroid.R
import com.h.pixeldroid.adapters.ThumbnailAdapter
import com.h.pixeldroid.utils.SpaceItemDecoration
import com.zomato.photofilters.FilterPack
import com.zomato.photofilters.imageprocessors.Filter
import com.zomato.photofilters.utils.ThumbnailItem
import com.zomato.photofilters.utils.ThumbnailsManager

class FilterListFragment : Fragment() {

    internal lateinit var recyclerView: RecyclerView
    internal var listener : PhotoEditActivity? = null
    internal lateinit var adapter: ThumbnailAdapter
    internal lateinit var tbItemList: MutableList<ThumbnailItem>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view =  inflater.inflate(R.layout.fragment_filter_list, container, false)

        tbItemList = ArrayList()
        adapter = ThumbnailAdapter(requireActivity(), tbItemList, this)

        recyclerView = view.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
        recyclerView.itemAnimator = DefaultItemAnimator()

        val space = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8f, resources.displayMetrics).toInt()
        recyclerView.addItemDecoration(SpaceItemDecoration(space))
        recyclerView.adapter = adapter

        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        displayImage(null)
    }

    private fun displayImage(bitmap: Bitmap?) {
        val r = Runnable {
            val tbImage: Bitmap = (if (bitmap == null) {
                // TODO: Shouldn't use deprecated API on newer versions of Android,
                // but the proper way to do it seems to crash for OpenGL reasons
                //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                //        ImageDecoder.decodeBitmap(
                //            ImageDecoder.createSource(requireActivity().contentResolver, PhotoEditActivity.imageUri!!))
                //} else {
                    MediaStore.Images.Media.getBitmap(
                        requireActivity().contentResolver,
                        PhotoEditActivity.imageUri
                    )
               //}
            } else {
                Bitmap.createScaledBitmap(bitmap, 100, 100, false)
            })
                ?: return@Runnable

            if(activity != null) setupFilter(tbImage)

            if(context != null) tbItemList.addAll(ThumbnailsManager.processThumbs(context))
            activity?.runOnUiThread{ adapter.notifyDataSetChanged() }
        }

        Thread(r).start()
    }

    private fun setupFilter(tbImage: Bitmap?) {
        ThumbnailsManager.clearThumbs()
        tbItemList.clear()

        val tbItem = ThumbnailItem()
        tbItem.image = tbImage
        tbItem.filter.name = getString(R.string.normal_filter)
        tbItem.filterName = tbItem.filter.name
        ThumbnailsManager.addThumb(tbItem)

        val filters = FilterPack.getFilterPack(context)

        for (filter in filters) {
            val item = ThumbnailItem()
            item.image = tbImage
            item.filter = filter
            item.filterName = filter.name
            ThumbnailsManager.addThumb(item)
        }
    }

    fun onFilterSelected(filter: Filter) {
        if(listener != null ){
            listener!!.onFilterSelected(filter)
        }
    }

    fun setListener(listFragmentListener: PhotoEditActivity) {
        this.listener = listFragmentListener
    }
}
