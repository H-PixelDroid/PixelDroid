package com.h.pixeldroid.postCreation.carousel

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.Dimension
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.*
import com.h.pixeldroid.R
import me.relex.circleindicator.CircleIndicator2
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable


class ImageCarousel(
        @NotNull context: Context,
        @Nullable private var attributeSet: AttributeSet?
) : ConstraintLayout(context, attributeSet), OnItemClickListener {

    private var adapter: CarouselAdapter? = null

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

    private lateinit var carouselView: View
    private lateinit var recyclerView: RecyclerView
    private lateinit var tvCaption: TextView
    private lateinit var previousButtonContainer: FrameLayout
    private lateinit var nextButtonContainer: FrameLayout
    private var snapHelper: SnapHelper = PagerSnapHelper()

    var indicator: CircleIndicator2? = null
        set(newIndicator) {
            indicator?.apply {
                // if we remove it form the view, then the caption textView constraint won't work.
                this.visibility = View.GONE

                isBuiltInIndicator = false
            }

            field = newIndicator

            initIndicator()
        }


    private var btnPrevious: View? = null
    private var btnNext: View? = null

    private var btnGrid: View? = null
    private var btnCarousel: View? = null


    private var isBuiltInIndicator = false
    private var data: List<CarouselItem>? = null

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
    var currentPosition = -1
        get() {
            return snapHelper.getSnapPosition(recyclerView.layoutManager)
        }
        set(value) {
            val position = when {
                value >= data?.size ?: 0 -> {
                    -1
                }
                value < 0 -> {
                    -1
                }
                else -> {
                    value
                }
            }

            field = position

            if (position != -1) {
                recyclerView.smoothScrollToPosition(position)
            }
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

            previousButtonContainer.visibility =
                    if (showNavigationButtons) View.VISIBLE else View.GONE
            nextButtonContainer.visibility =
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

    @LayoutRes
    var previousButtonLayout: Int = R.layout.previous_button_layout
        set(value) {
            field = value

            btnPrevious = null

            previousButtonContainer.removeAllViews()
            LayoutInflater.from(context).apply {
                inflate(previousButtonLayout, previousButtonContainer, true)
            }
        }

    @IdRes
    var previousButtonId: Int = R.id.btn_next
        set(value) {
            field = value

            btnPrevious = carouselView.findViewById(previousButtonId)

            btnPrevious?.setOnClickListener {
                previous()
            }
        }

    @Dimension(unit = Dimension.PX)
    var previousButtonMargin: Int = 0
        set(value) {
            field = value

            val previousButtonParams = previousButtonContainer.layoutParams as LayoutParams
            previousButtonParams.setMargins(
                    previousButtonMargin,
                    0,
                    0,
                    0
            )
            previousButtonContainer.layoutParams = previousButtonParams
        }

    @LayoutRes
    var nextButtonLayout: Int = R.layout.next_button_layout
        set(value) {
            field = value

            btnNext = null

            nextButtonContainer.removeAllViews()
            LayoutInflater.from(context).apply {
                inflate(nextButtonLayout, nextButtonContainer, true)
            }
        }

    @IdRes
    var nextButtonId: Int = R.id.btn_previous
        set(value) {
            field = value

            btnNext = carouselView.findViewById(nextButtonId)

            btnNext?.setOnClickListener {
                next()
            }
        }

    @Dimension(unit = Dimension.PX)
    var nextButtonMargin: Int = 0
        set(value) {
            field = value

            val nextButtonParams = nextButtonContainer.layoutParams as LayoutParams
            nextButtonParams.setMargins(
                    0,
                    0,
                    nextButtonMargin,
                    0
            )
            nextButtonContainer.layoutParams = nextButtonParams
        }

    var showLayoutSwitchButton: Boolean = true
        set(value) {
            field = value

            btnGrid = findViewById<ImageButton>(R.id.switchToGridButton)
            btnCarousel = findViewById<ImageButton>(R.id.switchToCarouselButton)

            btnGrid?.setOnClickListener {
                layoutCarousel = false
            }
            btnCarousel?.setOnClickListener {
                layoutCarousel = true
            }

            if(value){
                if(layoutCarousel){
                    btnGrid?.visibility = VISIBLE
                    btnCarousel?.visibility = GONE
                } else {
                    btnGrid?.visibility = GONE
                    btnCarousel?.visibility = VISIBLE
                }
            } else {
                btnGrid?.visibility = GONE
                btnCarousel?.visibility = GONE
            }
        }

    var layoutCarouselCallback: ((Boolean) -> Unit)? = null

    var layoutCarousel: Boolean = true
        set(value) {
            field = value

            if(value){
                recyclerView.layoutManager = CarouselLinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

                btnNext?.visibility = VISIBLE
                btnPrevious?.visibility = VISIBLE
            } else {
                recyclerView.layoutManager = GridLayoutManager(context, 3)
                btnNext?.visibility = GONE
                btnPrevious?.visibility = GONE
            }
            showIndicator = value

            layoutCarouselCallback?.let { it(value) }

            //update layout switch button to make it take into account the change
            showLayoutSwitchButton = showLayoutSwitchButton

            initAdapter()
        }

    var addPhotoButtonCallback: (() -> Unit)? = null



    init {
        initViews()
        initAttributes()
        initAdapter()
        initListeners()
    }


    private fun initViews() {
        carouselView = LayoutInflater.from(context).inflate(R.layout.image_carousel, this)

        recyclerView = carouselView.findViewById(R.id.recyclerView)
        tvCaption = carouselView.findViewById(R.id.tv_caption)
        previousButtonContainer = carouselView.findViewById(R.id.previous_button_container)
        nextButtonContainer = carouselView.findViewById(R.id.next_button_container)

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

                previousButtonLayout = R.layout.previous_button_layout

                previousButtonId = R.id.btn_previous

                previousButtonMargin = 4.dpToPx(context)

                nextButtonLayout = R.layout.next_button_layout

                nextButtonId = R.id.btn_next

                nextButtonMargin = 4.dpToPx(context)

                showNavigationButtons = getBoolean(
                        R.styleable.ImageCarousel_showNavigationButtons,
                        true
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
            carousel = layoutCarousel
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

                if (showCaption) {
                    val position = snapHelper.getSnapPosition(recyclerView.layoutManager)

                    if (position >= 0) {
                        val dataItem = adapter?.getItem(position)

                        dataItem?.apply {
                            tvCaption.text = this.caption
                        }
                    }
                }

                onScrollListener?.onScrolled(recyclerView, dx, dy)

            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {

                onScrollListener?.apply {
                    val position = snapHelper.getSnapPosition(recyclerView.layoutManager)
                    val carouselItem = data?.get(position)

                    onScrollStateChanged(
                            recyclerView,
                            newState,
                            position,
                            carouselItem
                    )
                }

            }
        })
    }

    private fun initIndicator() {
        // If no custom indicator added, then default indicator will be shown.
        if (indicator == null) {
            indicator = carouselView.findViewById(R.id.indicator)
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

            this@ImageCarousel.data = data

            initOnScrollStateChange()
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