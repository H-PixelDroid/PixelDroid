package org.pixeldroid.app.postCreation.photoEdit

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
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
import org.pixeldroid.app.R
import com.zomato.photofilters.FilterPack
import com.zomato.photofilters.imageprocessors.Filter
import com.zomato.photofilters.utils.ThumbnailItem
import com.zomato.photofilters.utils.ThumbnailsManager

class FilterListFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private var listener : PhotoEditActivity? = null
    internal lateinit var adapter: ThumbnailAdapter
    private lateinit var tbItemList: MutableList<ThumbnailItem>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view =  inflater.inflate(R.layout.fragment_filter_list, container, false)

        tbItemList = ArrayList()

        recyclerView = view.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
        recyclerView.itemAnimator = DefaultItemAnimator()

        val space = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8f, resources.displayMetrics).toInt()
        recyclerView.addItemDecoration(SpaceItemDecoration(space))

        adapter = ThumbnailAdapter(requireActivity(), tbItemList, this)
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
                // TODO: Check that there is no crash for OpenGL reasons on newer versions of Android
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    // Honor EXIF orientation if API >= 28
                    ImageDecoder.decodeBitmap(ImageDecoder
                            .createSource(requireActivity().contentResolver, PhotoEditActivity.imageUri!!))
                            .copy(Bitmap.Config.ARGB_8888,true)
                } else {
                    // Ignore EXIF orientation otherwise
                    MediaStore.Images.Media.getBitmap(
                        requireActivity().contentResolver,
                        PhotoEditActivity.imageUri
                    )
                }
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

    fun resetSelectedFilter(){
        adapter.resetSelected()
        displayImage(null)
    }

    fun onFilterSelected(filter: Filter) {
        listener?.onFilterSelected(filter)
    }

    fun setListener(listFragmentListener: PhotoEditActivity) {
        this.listener = listFragmentListener
    }
}
