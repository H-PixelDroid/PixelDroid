package org.pixeldroid.app.postCreation.carousel

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.annotation.Dimension
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.*
import org.pixeldroid.app.R
import org.pixeldroid.app.databinding.ImageCarouselBinding
import me.relex.circleindicator.CircleIndicator2
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable


class ImageCarousel(
        @NotNull context: Context,
        @Nullable private var attributeSet: AttributeSet?
) : ConstraintLayout(context, attributeSet), OnItemClickListener {

    private var adapter: CarouselAdapter? = null

    private lateinit var binding: ImageCarouselBinding

    private val scaleTypeArray = arrayOf(
            ImageView.ScaleType.MATRIX,
            ImageView.ScaleType.FIT_XY,
            ImageView.ScaleType.FIT_START,
            ImageView.ScaleType.FIT_CENTER,
            ImageView.ScaleType.FIT_END,
            ImageView.ScaleType.CENTER,
            ImageView.ScaleType.CENTER_CROP,
            ImageView.ScaleType.CENTER_INSIDE
    )

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvCaption: TextView
    private var snapHelper: SnapHelper = PagerSnapHelper()

    var indicator: CircleIndicator2? = null
        set(newIndicator) {
            indicator?.apply {
                // if we remove it from the view, then the caption textView constraint won't work.
                this.visibility = View.GONE

                isBuiltInIndicator = false
            }

            field = newIndicator

            initIndicator()
        }
    
    private var isBuiltInIndicator = false
    private var data: MutableList<CarouselItem>? = null

    var onItemClickListener: OnItemClickListener? = this
        set(value) {
            field = value

            adapter?.listener = onItemClickListener
        }

    var onScrollListener: CarouselOnScrollListener? = null
        set(value) {
            field = value

            initOnScrollStateChange()
        }

    /**
     * Get or set current item position
     */
    var currentPosition = RecyclerView.NO_POSITION
        get() {
            return snapHelper.getSnapPosition(recyclerView.layoutManager)
        }
        set(value) {
            val position = when (value) {
                !in 0..((data?.size?.minus(1)) ?: 0) -> RecyclerView.NO_POSITION
                else -> value
            }

            if (position != RecyclerView.NO_POSITION && field != position) {
                val thisProgress = data?.getOrNull(position)?.encodeProgress
                if (thisProgress != null) {
                    binding.encodeInfoCard.visibility = VISIBLE
                    binding.encodeProgress.visibility = VISIBLE
                    binding.encodeInfoText.text =
                        context.getString(R.string.encode_progress).format(thisProgress)
                    binding.encodeProgress.progress = thisProgress
                } else {
                    binding.encodeInfoCard.visibility = GONE
                }
            } else if(position == RecyclerView.NO_POSITION) binding.encodeInfoCard.visibility = GONE

            if (position != RecyclerView.NO_POSITION && recyclerView.scrollState == RecyclerView.SCROLL_STATE_IDLE) {
                recyclerView.smoothScrollToPosition(position)
            }
            field = position
        }

    /**
     * ****************************************************************
     * Attributes
     * ****************************************************************
     */

    var showCaption = false
        set(value) {
            field = value

            tvCaption.visibility = if (showCaption) View.VISIBLE else View.GONE
        }

    @Dimension(unit = Dimension.PX)
    var captionTextSize: Int = 0
        set(value) {
            field = value

            tvCaption.setTextSize(TypedValue.COMPLEX_UNIT_PX, captionTextSize.toFloat())
        }

    var showIndicator = false
        set(value) {
            field = value

            when {
                indicator == null -> initIndicator()
                value -> indicator?.visibility = View.VISIBLE
                else -> indicator?.visibility = View.INVISIBLE
            }
        }

    var showNavigationButtons = false
        set(value) {
            field = value

            binding.btnPrevious.visibility =
                    if (showNavigationButtons) View.VISIBLE else View.GONE
            binding.btnNext.visibility =
                    if (showNavigationButtons) View.VISIBLE else View.GONE
        }

    var imageScaleType: ImageView.ScaleType = ImageView.ScaleType.CENTER_INSIDE
        set(value) {
            field = value

            initAdapter()
        }

    var carouselBackground: Drawable? = null
        set(value) {
            field = value

            recyclerView.background = carouselBackground
        }

    var imagePlaceholder: Drawable? = null
        set(value) {
            field = value

            initAdapter()
        }

    @LayoutRes
    var itemLayout: Int = R.layout.item_carousel
        set(value) {
            field = value

            initAdapter()
        }

    @IdRes
    var imageViewId: Int = R.id.img
        set(value) {
            field = value

            initAdapter()
        }

    @Dimension(unit = Dimension.PX)
    var previousButtonMargin: Int = 0
        set(value) {
            field = value

            val previousButtonParams = binding.btnPrevious.layoutParams as LayoutParams
            previousButtonParams.setMargins(
                    previousButtonMargin,
                    0,
                    0,
                    0
            )
            binding.btnPrevious.layoutParams = previousButtonParams
        }

    @Dimension(unit = Dimension.PX)
    var nextButtonMargin: Int = 0
        set(value) {
            field = value

            val nextButtonParams = binding.btnNext.layoutParams as LayoutParams
            nextButtonParams.setMargins(
                    0,
                    0,
                    nextButtonMargin,
                    0
            )
            binding.btnNext.layoutParams = nextButtonParams
        }

    var showLayoutSwitchButton: Boolean = true
        set(value) {
            field = value

            binding.switchToGridButton.setOnClickListener {
                layoutCarousel = false
            }
            binding.switchToCarouselButton.setOnClickListener {
                layoutCarousel = true
            }

            if(value){
                if(layoutCarousel){
                    binding.switchToGridButton.visibility = VISIBLE
                    binding.switchToCarouselButton.visibility = GONE
                } else {
                    binding.switchToGridButton.visibility = GONE
                    binding.switchToCarouselButton.visibility = VISIBLE
                }
            } else {
                binding.switchToGridButton.visibility = GONE
                binding.switchToCarouselButton.visibility = GONE
            }
        }

    var layoutCarouselCallback: ((Boolean) -> Unit)? = null

    var updateDescriptionCallback: ((position: Int, description: String) -> Unit)? = null


    var layoutCarousel: Boolean = true
        set(value) {
            field = value

            if(value){
                recyclerView.layoutManager = CarouselLinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

                showNavigationButtons = showNavigationButtons

                binding.editMediaDescriptionLayout.visibility = if(editingMediaDescription) VISIBLE else INVISIBLE
                tvCaption.visibility = if(editingMediaDescription) INVISIBLE else VISIBLE
            } else {
                recyclerView.layoutManager = GridLayoutManager(context, 3)
                binding.btnNext.visibility = GONE
                binding.btnPrevious.visibility = GONE

                binding.editMediaDescriptionLayout.visibility = INVISIBLE
                tvCaption.visibility = INVISIBLE
            }
            showIndicator = value

            layoutCarouselCallback?.let { it(value) }

            //update layout switch button to make it take into account the change
            showLayoutSwitchButton = showLayoutSwitchButton

            initAdapter()
        }

    var addPhotoButtonCallback: (() -> Unit)? = null

    var editingMediaDescription: Boolean = false
        set(value){

            if(layoutCarousel){
                field = value
                if(value) binding.editTextMediaDescription.setText(currentDescription)
                else {
                    val description = binding.editTextMediaDescription.text.toString()
                    currentDescription = description
                    adapter?.updateDescription(currentPosition, description)
                    updateDescriptionCallback?.invoke(currentPosition, description)
                }
                binding.editMediaDescriptionLayout.visibility = if(value) VISIBLE else INVISIBLE
                tvCaption.visibility = if(value) INVISIBLE else VISIBLE

            }

        }

    var currentDescription: String? = null
        set(value) {
            if(!value.isNullOrEmpty()) {
                field = value
                tvCaption.text = value
            } else {
                field = null
                tvCaption.text = context.getText(R.string.no_media_description)
            }

        }

    var maxEntries: Int? = null
        set(value){
            field = value
            adapter?.maxEntries = value
        }



    init {
        initViews()
        initAttributes()
        initAdapter()
        initListeners()
    }


    private fun initViews() {
        binding = ImageCarouselBinding.inflate(LayoutInflater.from(context),this,  true)

        recyclerView = binding.recyclerView
        tvCaption = binding.tvCaption

        recyclerView.setHasFixedSize(true)

        // For marquee effect
        tvCaption.isSelected = true
    }


    private fun initAttributes() {
        context.theme.obtainStyledAttributes(
                attributeSet,
                R.styleable.ImageCarousel,
                0,
                0
        ).apply {

            try {

                showCaption = getBoolean(
                        R.styleable.ImageCarousel_showCaption,
                        true
                )

                captionTextSize = getDimension(
                        R.styleable.ImageCarousel_captionTextSize,
                        14.spToPx(context).toFloat()
                ).toInt()

                showIndicator = getBoolean(
                        R.styleable.ImageCarousel_showIndicator,
                        true
                )

                imageScaleType = scaleTypeArray[
                        getInteger(
                                R.styleable.ImageCarousel_imageScaleType,
                                ImageView.ScaleType.CENTER_INSIDE.ordinal
                        )
                ]

                carouselBackground = ColorDrawable(Color.parseColor("#333333"))

                imagePlaceholder = getDrawable(
                        R.styleable.ImageCarousel_imagePlaceholder
                ) ?: ContextCompat.getDrawable(context, R.drawable.ic_picture_fallback)

                itemLayout = getResourceId(
                        R.styleable.ImageCarousel_itemLayout,
                        R.layout.item_carousel
                )

                imageViewId = getResourceId(
                        R.styleable.ImageCarousel_imageViewId,
                        R.id.img
                )

                previousButtonMargin = 4.dpToPx(context)

                nextButtonMargin = 4.dpToPx(context)

                showNavigationButtons = getBoolean(
                        R.styleable.ImageCarousel_showNavigationButtons,
                        false
                )

                layoutCarousel = getBoolean(
                    R.styleable.ImageCarousel_layoutCarousel,
                    true
                )

                showLayoutSwitchButton = getBoolean(
                    R.styleable.ImageCarousel_showLayoutSwitchButton,
                    true
                )

            } finally {
                recycle()
            }

        }
    }


    private fun initAdapter() {
        adapter = CarouselAdapter(
                itemLayout = itemLayout,
                imageViewId = imageViewId,
                listener = onItemClickListener,
                imageScaleType = imageScaleType,
                imagePlaceholder = imagePlaceholder,
                carousel = layoutCarousel,
                maxEntries = maxEntries
        )
        recyclerView.adapter = adapter

        data?.apply {
            adapter?.addAll(this)
        }

        indicator?.apply {
            try {
                adapter?.registerAdapterDataObserver(this.adapterDataObserver)
            } catch (e: IllegalStateException) {
                e.printStackTrace()
            }
        }
    }


    private fun initListeners() {
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val position = currentPosition

                if (showCaption) {
                    if (position >= 0) {
                        val dataItem = adapter?.getItem(position)

                        dataItem?.apply {
                            caption.apply {
                                if(layoutCarousel){
                                    binding.editMediaDescriptionLayout.visibility = INVISIBLE
                                    tvCaption.visibility = VISIBLE
                                }
                                currentDescription = this
                            }
                        }
                    }
                }

                if(dx !=0 || dy != 0) currentPosition = position

                onScrollListener?.onScrolled(recyclerView, dx, dy)

            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {

                onScrollListener?.apply {
                    val position = snapHelper.getSnapPosition(recyclerView.layoutManager)
                    val carouselItem = data?.getOrNull(position)

                    onScrollStateChanged(
                            recyclerView,
                            newState,
                            position,
                            carouselItem
                    )
                }

            }
        })

        tvCaption.setOnClickListener {
            editingMediaDescription = true
        }

        binding.btnNext.setOnClickListener {
            next()
        }

        binding.btnPrevious.setOnClickListener {
            previous()
        }
        binding.imageDescriptionButton.setOnClickListener {
            editingMediaDescription = false
        }
    }

    private fun initIndicator() {
        // If no custom indicator added, then default indicator will be shown.
        if (indicator == null) {
            indicator = binding.indicator
            isBuiltInIndicator = true
        }

        snapHelper.apply {
            try {
                attachToRecyclerView(recyclerView)
            } catch (e: IllegalStateException) {
                e.printStackTrace()
            }
        }

        indicator?.apply {
            if (isBuiltInIndicator) {
                // Indicator visibility
                this.visibility = if (showIndicator) View.VISIBLE else View.INVISIBLE
            }

            // Attach to recyclerview
            attachToRecyclerView(recyclerView, snapHelper)

            // Observe the adapter
            adapter?.let { carouselAdapter ->
                try {
                    carouselAdapter.registerAdapterDataObserver(this.adapterDataObserver)
                } catch (e: IllegalStateException) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun initOnScrollStateChange() {
        data?.apply {
            if (isNotEmpty()) {
                onScrollListener?.onScrollStateChanged(
                        recyclerView,
                        RecyclerView.SCROLL_STATE_IDLE,
                        0,
                        this[0]
                )
            }
        }
    }

    /**
     * Add data to the carousel.
     */
    fun addData(data: List<CarouselItem>) {
        adapter?.apply {
            addAll(data)

            this@ImageCarousel.data = data.toMutableList()

            initOnScrollStateChange()
        }
        showNavigationButtons = data.size != 1
    }

    fun updateProgress(progress: Int?, position: Int, error: Boolean){
        data?.getOrNull(position)?.encodeProgress = progress
        if(currentPosition == position) {
            if (progress == null) {
                binding.encodeProgress.visibility = GONE
                if(error){
                    binding.encodeInfoText.setText(R.string.encode_error)
                    binding.encodeInfoText.setCompoundDrawablesWithIntrinsicBounds(ContextCompat.getDrawable(context, R.drawable.error),
                        null, null, null)

                } else {
                    binding.encodeInfoText.setText(R.string.encode_success)
                    binding.encodeInfoText.setCompoundDrawablesWithIntrinsicBounds(ContextCompat.getDrawable(context, R.drawable.check_circle_24),
                    null, null, null)
                }
            } else {
                binding.encodeProgress.visibility = VISIBLE
                binding.encodeInfoCard.visibility = VISIBLE
                binding.encodeProgress.progress = progress
                binding.encodeInfoText.text = context.getString(R.string.encode_progress).format(progress)
            }
        }
    }

    /**
     * Goto previous item.
     */
    fun previous() {
        currentPosition--
    }

    /**
     * Goto next item.
     */
    fun next() {
        currentPosition++
    }

    override fun onClick(position: Int) {
        if(position == (data?.size ?: 0) ){
            addPhotoButtonCallback?.invoke()
        } else {
            if (!layoutCarousel) layoutCarousel = true
            currentPosition = position
        }
    }

    override fun onLongClick(position: Int) {
        //if(!layoutCarousel && position != (data?.size ?: 0) ) {
            //TODO Highlight selected, show toolbar?
            // Enable "long click mode?"
        //}
    }
}

interface OnItemClickListener {

    fun onClick(position: Int)

    fun onLongClick(position: Int)

}

interface CarouselOnScrollListener {

    fun onScrollStateChanged(
            recyclerView: RecyclerView,
            newState: Int,
            position: Int,
            carouselItem: CarouselItem?
    ) {}

    fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {}

}