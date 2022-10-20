package org.pixeldroid.app.postCreation.photoEdit.cropper;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;

import org.pixeldroid.app.postCreation.photoEdit.VideoEditActivity;

/** A custom View representing the crop window and the shaded background outside the crop window. */
public class CropOverlayView extends View {

  // region: Fields and Consts

  /** Handler from crop window stuff, moving and knowing position. */
  private final CropWindowHandler mCropWindowHandler = new CropWindowHandler();

  /** The Paint used to draw the white rectangle around the crop area. */
  private Paint mBorderPaint;

  /** The Paint used to draw the corners of the Border */
  private Paint mBorderCornerPaint;

  /** The Paint used to draw the guidelines within the crop area when pressed. */
  private Paint mGuidelinePaint;

  /** The bounding box around the Bitmap that we are cropping. */
  private final RectF mCalcBounds = new RectF();

  /** The bounding image view width used to know the crop overlay is at view edges. */
  private int mViewWidth;

  /** The bounding image view height used to know the crop overlay is at view edges. */
  private int mViewHeight;

  /** The Handle that is currently pressed; null if no Handle is pressed. */
  private CropWindowMoveHandler mMoveHandler;

  /** the initial crop window rectangle to set */
  private final Rect mInitialCropWindowRect = new Rect();

  /** Whether the Crop View has been initialized for the first time */
  private boolean initializedCropWindow;

  // endregion

  public CropOverlayView(Context context) {
    this(context, null);
  }

  public CropOverlayView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  /** Get the left/top/right/bottom coordinates of the crop window. */
  public RectF getCropWindowRect() {
    return mCropWindowHandler.getRect();
  }

  /** Set the left/top/right/bottom coordinates of the crop window. */
  public void setCropWindowRect(RectF rect) {
    mCropWindowHandler.setRect(rect);
  }

  /** Fix the current crop window rectangle if it is outside of cropping image or view bounds. */
  public void fixCurrentCropWindowRect() {
    RectF rect = getCropWindowRect();
    fixCropWindowRectByRules(rect);
    mCropWindowHandler.setRect(rect);
  }

  /**
   * Informs the CropOverlayView of the image's position relative to the ImageView. This is
   * necessary to call in order to draw the crop window.
   *
   * @param viewWidth The bounding image view width.
   * @param viewHeight The bounding image view height.
   */
  public void setBounds(int viewWidth, int viewHeight) {
      mViewWidth = viewWidth;
      mViewHeight = viewHeight;
      RectF cropRect = mCropWindowHandler.getRect();
      if (cropRect.width() == 0 || cropRect.height() == 0) {
        initCropWindow();
      }
    }


  /** Resets the crop overlay view. */
  public void resetCropOverlayView() {
    if (initializedCropWindow) {
      setCropWindowRect(new RectF());
      initCropWindow();
      invalidate();
    }
  }

  /**
   * Set the max width/height and scale factor of the shown image to original image to scale the
   * limits appropriately.
   */
  public void setCropWindowLimits(float maxWidth, float maxHeight) {
    mCropWindowHandler.setCropWindowLimits(maxWidth, maxHeight);
  }

  /** Get crop window initial rectangle. */
  public Rect getInitialCropWindowRect() {
    return mInitialCropWindowRect;
  }

  public void setRecordedCropWindowRect(@NonNull VideoEditActivity.RelativeCropPosition relativeCropPosition) {
    RectF rect = new RectF(
            mInitialCropWindowRect.left + relativeCropPosition.getRelativeX() * mInitialCropWindowRect.width(),
            mInitialCropWindowRect.top + relativeCropPosition.getRelativeY() * mInitialCropWindowRect.height(),
            relativeCropPosition.getRelativeWidth() * mInitialCropWindowRect.width()
                    + mInitialCropWindowRect.left + relativeCropPosition.getRelativeX() * mInitialCropWindowRect.width(),
            relativeCropPosition.getRelativeHeight() * mInitialCropWindowRect.height()
                    + mInitialCropWindowRect.top + relativeCropPosition.getRelativeY() * mInitialCropWindowRect.height()
    );

    mCropWindowHandler.setRect(rect);
  }

  /** Set crop window initial rectangle to be used instead of default. */
  public void setInitialCropWindowRect(Rect rect) {
    mInitialCropWindowRect.set(rect != null ? rect : new Rect());
    if (initializedCropWindow) {
      initCropWindow();
      invalidate();
    }
  }

  /** Reset crop window to initial rectangle. */
  public void resetCropWindowRect() {
    if (initializedCropWindow) {
      initCropWindow();
      invalidate();
    }
  }

  /**
   * Sets all initial values, but does not call initCropWindow to reset the views.<br>
   * Used once at the very start to initialize the attributes.
   */
  public void setInitialAttributeValues() {
    DisplayMetrics dm = Resources.getSystem().getDisplayMetrics();

    mBorderPaint =
            getNewPaintOfThickness(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3, dm), Color.argb(170, 255, 255, 255));

    mBorderCornerPaint =
        getNewPaintOfThickness(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, dm), Color.WHITE);

    mGuidelinePaint =
            getNewPaintOfThickness(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, dm), Color.argb(170, 255, 255, 255));
  }

  // region: Private methods

  /**
   * Set the initial crop window size and position. This is dependent on the size and position of
   * the image being cropped.
   */
  private void initCropWindow() {

    RectF rect = new RectF();

    // Tells the attribute functions the crop window has already been initialized
    initializedCropWindow = true;

    if (mInitialCropWindowRect.width() > 0 && mInitialCropWindowRect.height() > 0) {
      // Get crop window position relative to the displayed image.
      rect.left = mInitialCropWindowRect.left;
      rect.top = mInitialCropWindowRect.top;
      rect.right = rect.left + mInitialCropWindowRect.width();
      rect.bottom = rect.top + mInitialCropWindowRect.height();
    }

    fixCropWindowRectByRules(rect);

    mCropWindowHandler.setRect(rect);
  }

  /** Fix the given rect to fit into bitmap rect and follow min, max and aspect ratio rules. */
  private void fixCropWindowRectByRules(RectF rect) {
    if (rect.width() < mCropWindowHandler.getMinCropWidth()) {
      float adj = (mCropWindowHandler.getMinCropWidth() - rect.width()) / 2;
      rect.left -= adj;
      rect.right += adj;
    }
    if (rect.height() < mCropWindowHandler.getMinCropHeight()) {
      float adj = (mCropWindowHandler.getMinCropHeight() - rect.height()) / 2;
      rect.top -= adj;
      rect.bottom += adj;
    }
    if (rect.width() > mCropWindowHandler.getMaxCropWidth()) {
      float adj = (rect.width() - mCropWindowHandler.getMaxCropWidth()) / 2;
      rect.left += adj;
      rect.right -= adj;
    }
    if (rect.height() > mCropWindowHandler.getMaxCropHeight()) {
      float adj = (rect.height() - mCropWindowHandler.getMaxCropHeight()) / 2;
      rect.top += adj;
      rect.bottom -= adj;
    }

    calculateBounds(rect);
    if (mCalcBounds.width() > 0 && mCalcBounds.height() > 0) {
      float leftLimit = Math.max(mCalcBounds.left, 0);
      float topLimit = Math.max(mCalcBounds.top, 0);
      float rightLimit = Math.min(mCalcBounds.right, getWidth());
      float bottomLimit = Math.min(mCalcBounds.bottom, getHeight());
      if (rect.left < leftLimit) {
        rect.left = leftLimit;
      }
      if (rect.top < topLimit) {
        rect.top = topLimit;
      }
      if (rect.right > rightLimit) {
        rect.right = rightLimit;
      }
      if (rect.bottom > bottomLimit) {
        rect.bottom = bottomLimit;
      }
    }
  }

  /**
   * Draw crop overview by drawing background over image not in the cropping area, then borders and
   * guidelines.
   */
  @Override
  protected void onDraw(Canvas canvas) {

    super.onDraw(canvas);

    // Draw translucent background for the notCropped area.
    drawBackground(canvas);

    if (mCropWindowHandler.showGuidelines()) {
      // Determines whether guidelines should be drawn or not
      if (mMoveHandler != null) {
        // Draw only when resizing
        drawGuidelines(canvas);
      }
    }

    drawBorders(canvas);

    drawCorners(canvas);
  }

  /** Draw shadow background over the image not including the crop area. */
  private void drawBackground(Canvas canvas) {

    RectF rect = mCropWindowHandler.getRect();

    canvas.drawRect(rect.left, rect.top, rect.right, rect.bottom, getNewPaint(Color.argb(119, 0, 0, 0)));
  }

  /**
   * Draw 2 vertical and 2 horizontal guidelines inside the cropping area to split it into 9 equal
   * parts.
   */
  private void drawGuidelines(Canvas canvas) {
    if (mGuidelinePaint != null) {
      float sw = mBorderPaint != null ? mBorderPaint.getStrokeWidth() : 0;
      RectF rect = mCropWindowHandler.getRect();
      rect.inset(sw, sw);

      float oneThirdCropWidth = rect.width() / 3;
      float oneThirdCropHeight = rect.height() / 3;

        // Draw vertical guidelines.
        float x1 = rect.left + oneThirdCropWidth;
        float x2 = rect.right - oneThirdCropWidth;
        canvas.drawLine(x1, rect.top, x1, rect.bottom, mGuidelinePaint);
        canvas.drawLine(x2, rect.top, x2, rect.bottom, mGuidelinePaint);

        // Draw horizontal guidelines.
        float y1 = rect.top + oneThirdCropHeight;
        float y2 = rect.bottom - oneThirdCropHeight;
        canvas.drawLine(rect.left, y1, rect.right, y1, mGuidelinePaint);
        canvas.drawLine(rect.left, y2, rect.right, y2, mGuidelinePaint);

    }
  }

  /** Draw borders of the crop area. */
  private void drawBorders(Canvas canvas) {
    if (mBorderPaint != null) {
      float w = mBorderPaint.getStrokeWidth();
      RectF rect = mCropWindowHandler.getRect();
      // Make the rectangle a bit smaller to accommodate for the border
      rect.inset(w / 2, w / 2);

      // Draw rectangle crop window border.
      canvas.drawRect(rect, mBorderPaint);
    }
  }

  /** Draw the corner of crop overlay. */
  private void drawCorners(Canvas canvas) {
    DisplayMetrics dm = Resources.getSystem().getDisplayMetrics();

    if (mBorderCornerPaint != null) {

      float lineWidth = mBorderPaint != null ? mBorderPaint.getStrokeWidth() : 0;
      float cornerWidth = mBorderCornerPaint.getStrokeWidth();

      // The corners should be a bit offset from the borders
      float w = (cornerWidth / 2)
                      + TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5, dm);

      RectF rect = mCropWindowHandler.getRect();
      rect.inset(w, w);

      float cornerOffset = (cornerWidth - lineWidth) / 2;
      float cornerExtension = cornerWidth / 2 + cornerOffset;

      /* the length of the border corner to draw */
      float mBorderCornerLength = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 14, dm);

      // Top left
      canvas.drawLine(
          rect.left - cornerOffset,
          rect.top - cornerExtension,
          rect.left - cornerOffset,
          rect.top + mBorderCornerLength,
          mBorderCornerPaint);
      canvas.drawLine(
          rect.left - cornerExtension,
          rect.top - cornerOffset,
          rect.left + mBorderCornerLength,
          rect.top - cornerOffset,
          mBorderCornerPaint);

      // Top right
      canvas.drawLine(
          rect.right + cornerOffset,
          rect.top - cornerExtension,
          rect.right + cornerOffset,
          rect.top + mBorderCornerLength,
          mBorderCornerPaint);
      canvas.drawLine(
          rect.right + cornerExtension,
          rect.top - cornerOffset,
          rect.right - mBorderCornerLength,
          rect.top - cornerOffset,
          mBorderCornerPaint);

      // Bottom left
      canvas.drawLine(
          rect.left - cornerOffset,
          rect.bottom + cornerExtension,
          rect.left - cornerOffset,
          rect.bottom - mBorderCornerLength,
          mBorderCornerPaint);
      canvas.drawLine(
          rect.left - cornerExtension,
          rect.bottom + cornerOffset,
          rect.left + mBorderCornerLength,
          rect.bottom + cornerOffset,
          mBorderCornerPaint);

      // Bottom left
      canvas.drawLine(
          rect.right + cornerOffset,
          rect.bottom + cornerExtension,
          rect.right + cornerOffset,
          rect.bottom - mBorderCornerLength,
          mBorderCornerPaint);
      canvas.drawLine(
          rect.right + cornerExtension,
          rect.bottom + cornerOffset,
          rect.right - mBorderCornerLength,
          rect.bottom + cornerOffset,
          mBorderCornerPaint);
    }
  }

  /** Creates the Paint object for drawing. */
  private static Paint getNewPaint(int color) {
    Paint paint = new Paint();
    paint.setColor(color);
    return paint;
  }

  /** Creates the Paint object for given thickness and color */
  private static Paint getNewPaintOfThickness(float thickness, int color) {
    Paint borderPaint = new Paint();
    borderPaint.setColor(color);
    borderPaint.setStrokeWidth(thickness);
    borderPaint.setStyle(Paint.Style.STROKE);
    borderPaint.setAntiAlias(true);
    return borderPaint;
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    // If this View is not enabled, don't allow for touch interactions.
    if (isEnabled()) {
      /* Boolean to see if multi touch is enabled for the crop rectangle */
      switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN:
          onActionDown(event.getX(), event.getY());
          return true;
        case MotionEvent.ACTION_UP:
        case MotionEvent.ACTION_CANCEL:
          getParent().requestDisallowInterceptTouchEvent(false);
          onActionUp();
          return true;
        case MotionEvent.ACTION_MOVE:
          onActionMove(event.getX(), event.getY());
          getParent().requestDisallowInterceptTouchEvent(true);
          return true;
        default:
          return false;
      }
    } else {
      return false;
    }
  }

  /**
   * On press down start crop window movement depending on the location of the press.<br>
   * if press is far from crop window then no move handler is returned (null).
   */
  private void onActionDown(float x, float y) {
    DisplayMetrics dm = Resources.getSystem().getDisplayMetrics();
    mMoveHandler = mCropWindowHandler.getMoveHandler(x, y, TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, dm));
    if (mMoveHandler != null) {
      invalidate();
    }
  }

  /** Clear move handler starting in {@link #onActionDown(float, float)} if exists. */
  private void onActionUp() {
    if (mMoveHandler != null) {
      mMoveHandler = null;
      invalidate();
    }
  }

  /**
   * Handle move of crop window using the move handler created in {@link #onActionDown(float,
   * float)}.<br>
   * The move handler will do the proper move/resize of the crop window.
   */
  private void onActionMove(float x, float y) {
    if (mMoveHandler != null) {
      RectF rect = mCropWindowHandler.getRect();

      calculateBounds(rect);
      DisplayMetrics dm = Resources.getSystem().getDisplayMetrics();
      float snapRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3, dm);

      mMoveHandler.move(
          rect,
          x,
          y,
          mCalcBounds,
          mViewWidth,
          mViewHeight,
          snapRadius
      );
      mCropWindowHandler.setRect(rect);
      invalidate();
    }
  }

  /**
   * Calculate the bounding rectangle for current crop window
   * The bounds rectangle is the bitmap rectangle
   */
  private void calculateBounds(RectF rect) {
    mCalcBounds.set(mInitialCropWindowRect);
  }
  // endregion
}
