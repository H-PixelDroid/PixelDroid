package org.pixeldroid.app.postCreation.photoEdit.cropper;

import android.graphics.PointF;
import android.graphics.RectF;

/**
 * Handler to update crop window edges by the move type - Horizontal, Vertical, Corner or Center.
 * <br>
 */
final class CropWindowMoveHandler {

  // region: Fields and Consts

  /** Minimum width in pixels that the crop window can get. */
  private final float mMinCropWidth;

  /** Minimum width in pixels that the crop window can get. */
  private final float mMinCropHeight;

  /** Maximum height in pixels that the crop window can get. */
  private final float mMaxCropWidth;

  /** Maximum height in pixels that the crop window can get. */
  private final float mMaxCropHeight;

  /** The type of crop window move that is handled. */
  private final Type mType;

  /**
   * Holds the x and y offset between the exact touch location and the exact handle location that is
   * activated. There may be an offset because we allow for some leeway (specified by mHandleRadius)
   * in activating a handle. However, we want to maintain these offset values while the handle is
   * being dragged so that the handle doesn't jump.
   */
  private final PointF mTouchOffset = new PointF();
  // endregion

  /**
   * @param cropWindowHandler main crop window handle to get and update the crop window edges
   * @param touchX the location of the initial touch position to measure move distance
   * @param touchY the location of the initial touch position to measure move distance
   */
  public CropWindowMoveHandler(
      Type type, CropWindowHandler cropWindowHandler, float touchX, float touchY) {
    mType = type;
    mMinCropWidth = cropWindowHandler.getMinCropWidth();
    mMinCropHeight = cropWindowHandler.getMinCropHeight();
    mMaxCropWidth = cropWindowHandler.getMaxCropWidth();
    mMaxCropHeight = cropWindowHandler.getMaxCropHeight();
    calculateTouchOffset(cropWindowHandler.getRect(), touchX, touchY);
  }

  /**
   * Updates the crop window by change in the touch location.<br>
   * Move type handled by this instance, as initialized in creation, affects how the change in toch
   * location changes the crop window position and size.<br>
   * After the crop window position/size is changed by touch move it may result in values that
   * violate constraints: outside the bounds of the shown bitmap, smaller/larger than min/max size or
   * mismatch in aspect ratio. So a series of fixes is executed on "secondary" edges to adjust it
   * by the "primary" edge movement.<br>
   * Primary is the edge directly affected by move type, secondary is the other edge.<br>
   * The crop window is changed by directly setting the Edge coordinates.
   *
   * @param x the new x-coordinate of this handle
   * @param y the new y-coordinate of this handle
   * @param bounds the bounding rectangle of the image
   * @param viewWidth The bounding image view width used to know the crop overlay is at view edges.
   * @param viewHeight The bounding image view height used to know the crop overlay is at view
   *     edges.
   * @param snapMargin the maximum distance (in pixels) at which the crop window should snap to the
   *     image
   */
  public void move(
      RectF rect,
      float x,
      float y,
      RectF bounds,
      int viewWidth,
      int viewHeight,
      float snapMargin) {

    // Adjust the coordinates for the finger position's offset (i.e. the
    // distance from the initial touch to the precise handle location).
    // We want to maintain the initial touch's distance to the pressed
    // handle so that the crop window size does not "jump".
    float adjX = x + mTouchOffset.x;
    float adjY = y + mTouchOffset.y;

    if (mType == Type.CENTER) {
      moveCenter(rect, adjX, adjY, bounds, viewWidth, viewHeight, snapMargin);
    } else {
      moveSizeWithFreeAspectRatio(rect, adjX, adjY, bounds, viewWidth, viewHeight, snapMargin);
    }
  }

  // region: Private methods

  /**
   * Calculates the offset of the touch point from the precise location of the specified handle.<br>
   * Save these values in a member variable since we want to maintain this offset as we drag the
   * handle.
   */
  private void calculateTouchOffset(RectF rect, float touchX, float touchY) {

    float touchOffsetX = 0;
    float touchOffsetY = 0;

    // Calculate the offset from the appropriate handle.
    switch (mType) {
      case TOP_LEFT:
        touchOffsetX = rect.left - touchX;
        touchOffsetY = rect.top - touchY;
        break;
      case TOP_RIGHT:
        touchOffsetX = rect.right - touchX;
        touchOffsetY = rect.top - touchY;
        break;
      case BOTTOM_LEFT:
        touchOffsetX = rect.left - touchX;
        touchOffsetY = rect.bottom - touchY;
        break;
      case BOTTOM_RIGHT:
        touchOffsetX = rect.right - touchX;
        touchOffsetY = rect.bottom - touchY;
        break;
      case LEFT:
        touchOffsetX = rect.left - touchX;
        touchOffsetY = 0;
        break;
      case TOP:
        touchOffsetX = 0;
        touchOffsetY = rect.top - touchY;
        break;
      case RIGHT:
        touchOffsetX = rect.right - touchX;
        touchOffsetY = 0;
        break;
      case BOTTOM:
        touchOffsetX = 0;
        touchOffsetY = rect.bottom - touchY;
        break;
      case CENTER:
        touchOffsetX = rect.centerX() - touchX;
        touchOffsetY = rect.centerY() - touchY;
        break;
      default:
        break;
    }

    mTouchOffset.x = touchOffsetX;
    mTouchOffset.y = touchOffsetY;
  }

  /** Center move only changes the position of the crop window without changing the size. */
  private void moveCenter(
      RectF rect, float x, float y, RectF bounds, int viewWidth, int viewHeight, float snapRadius) {
    float dx = x - rect.centerX();
    float dy = y - rect.centerY();
    if (rect.left + dx < 0
        || rect.right + dx > viewWidth
        || rect.left + dx < bounds.left
        || rect.right + dx > bounds.right) {
      dx /= 1.05f;
      mTouchOffset.x -= dx / 2;
    }
    if (rect.top + dy < 0
        || rect.bottom + dy > viewHeight
        || rect.top + dy < bounds.top
        || rect.bottom + dy > bounds.bottom) {
      dy /= 1.05f;
      mTouchOffset.y -= dy / 2;
    }
    rect.offset(dx, dy);
    snapEdgesToBounds(rect, bounds, snapRadius);
  }

  /**
   * Change the size of the crop window on the required edge (or edges for corner size move) without
   * affecting "secondary" edges.<br>
   * Only the primary edge(s) are fixed to stay within limits.
   */
  private void moveSizeWithFreeAspectRatio(
      RectF rect, float x, float y, RectF bounds, int viewWidth, int viewHeight, float snapMargin) {
    switch (mType) {
      case TOP_LEFT:
        adjustTop(rect, y, bounds, snapMargin);
        adjustLeft(rect, x, bounds, snapMargin);
        break;
      case TOP_RIGHT:
        adjustTop(rect, y, bounds, snapMargin);
        adjustRight(rect, x, bounds, viewWidth, snapMargin);
        break;
      case BOTTOM_LEFT:
        adjustBottom(rect, y, bounds, viewHeight, snapMargin);
        adjustLeft(rect, x, bounds, snapMargin);
        break;
      case BOTTOM_RIGHT:
        adjustBottom(rect, y, bounds, viewHeight, snapMargin);
        adjustRight(rect, x, bounds, viewWidth, snapMargin);
        break;
      case LEFT:
        adjustLeft(rect, x, bounds, snapMargin);
        break;
      case TOP:
        adjustTop(rect, y, bounds, snapMargin);
        break;
      case RIGHT:
        adjustRight(rect, x, bounds, viewWidth, snapMargin);
        break;
      case BOTTOM:
        adjustBottom(rect, y, bounds, viewHeight, snapMargin);
        break;
      default:
        break;
    }
  }

  /** Check if edges have gone out of bounds (including snap margin), and fix if needed. */
  private void snapEdgesToBounds(RectF edges, RectF bounds, float margin) {
    if (edges.left < bounds.left + margin) {
      edges.offset(bounds.left - edges.left, 0);
    }
    if (edges.top < bounds.top + margin) {
      edges.offset(0, bounds.top - edges.top);
    }
    if (edges.right > bounds.right - margin) {
      edges.offset(bounds.right - edges.right, 0);
    }
    if (edges.bottom > bounds.bottom - margin) {
      edges.offset(0, bounds.bottom - edges.bottom);
    }
  }

  /**
   * Get the resulting x-position of the left edge of the crop window given the handle's position
   * and the image's bounding box and snap radius.
   *
   * @param left the position that the left edge is dragged to
   * @param bounds the bounding box of the image that is being cropped
   * @param snapMargin the snap distance to the image edge (in pixels)
   */
  private void adjustLeft(
      RectF rect,
      float left,
      RectF bounds,
      float snapMargin) {

    float newLeft = left;

    if (newLeft < 0) {
      newLeft /= 1.05f;
      mTouchOffset.x -= newLeft / 1.1f;
    }

    if (newLeft < bounds.left) {
      mTouchOffset.x -= (newLeft - bounds.left) / 2f;
    }

    if (newLeft - bounds.left < snapMargin) {
      newLeft = bounds.left;
    }

    // Checks if the window is too small horizontally
    if (rect.right - newLeft < mMinCropWidth) {
      newLeft = rect.right - mMinCropWidth;
    }

    // Checks if the window is too large horizontally
    if (rect.right - newLeft > mMaxCropWidth) {
      newLeft = rect.right - mMaxCropWidth;
    }

    if (newLeft - bounds.left < snapMargin) {
      newLeft = bounds.left;
    }

    rect.left = newLeft;
  }

  /**
   * Get the resulting x-position of the right edge of the crop window given the handle's position
   * and the image's bounding box and snap radius.
   *
   * @param right the position that the right edge is dragged to
   * @param bounds the bounding box of the image that is being cropped
   * @param viewWidth
   * @param snapMargin the snap distance to the image edge (in pixels)
   */
  private void adjustRight(
      RectF rect,
      float right,
      RectF bounds,
      int viewWidth,
      float snapMargin) {

    float newRight = right;

    if (newRight > viewWidth) {
      newRight = viewWidth + (newRight - viewWidth) / 1.05f;
      mTouchOffset.x -= (newRight - viewWidth) / 1.1f;
    }

    if (newRight > bounds.right) {
      mTouchOffset.x -= (newRight - bounds.right) / 2f;
    }

    // If close to the edge
    if (bounds.right - newRight < snapMargin) {
      newRight = bounds.right;
    }

    // Checks if the window is too small horizontally
    if (newRight - rect.left < mMinCropWidth) {
      newRight = rect.left + mMinCropWidth;
    }

    // Checks if the window is too large horizontally
    if (newRight - rect.left > mMaxCropWidth) {
      newRight = rect.left + mMaxCropWidth;
    }

    // If close to the edge
    if (bounds.right - newRight < snapMargin) {
      newRight = bounds.right;
    }

    rect.right = newRight;
  }

  /**
   * Get the resulting y-position of the top edge of the crop window given the handle's position and
   * the image's bounding box and snap radius.
   *
   * @param top the x-position that the top edge is dragged to
   * @param bounds the bounding box of the image that is being cropped
   * @param snapMargin the snap distance to the image edge (in pixels)
   */
  private void adjustTop(
      RectF rect,
      float top,
      RectF bounds,
      float snapMargin) {

    float newTop = top;

    if (newTop < 0) {
      newTop /= 1.05f;
      mTouchOffset.y -= newTop / 1.1f;
    }

    if (newTop < bounds.top) {
      mTouchOffset.y -= (newTop - bounds.top) / 2f;
    }

    if (newTop - bounds.top < snapMargin) {
      newTop = bounds.top;
    }

    // Checks if the window is too small vertically
    if (rect.bottom - newTop < mMinCropHeight) {
      newTop = rect.bottom - mMinCropHeight;
    }

    // Checks if the window is too large vertically
    if (rect.bottom - newTop > mMaxCropHeight) {
      newTop = rect.bottom - mMaxCropHeight;
    }

    if (newTop - bounds.top < snapMargin) {
      newTop = bounds.top;
    }

    rect.top = newTop;
  }

  /**
   * Get the resulting y-position of the bottom edge of the crop window given the handle's position
   * and the image's bounding box and snap radius.
   *
   * @param bottom     the position that the bottom edge is dragged to
   * @param bounds     the bounding box of the image that is being cropped
   * @param viewHeight
   * @param snapMargin the snap distance to the image edge (in pixels)
   */
  private void adjustBottom(
      RectF rect,
      float bottom,
      RectF bounds,
      int viewHeight,
      float snapMargin) {

    float newBottom = bottom;

    if (newBottom > viewHeight) {
      newBottom = viewHeight + (newBottom - viewHeight) / 1.05f;
      mTouchOffset.y -= (newBottom - viewHeight) / 1.1f;
    }

    if (newBottom > bounds.bottom) {
      mTouchOffset.y -= (newBottom - bounds.bottom) / 2f;
    }

    if (bounds.bottom - newBottom < snapMargin) {
      newBottom = bounds.bottom;
    }

    // Checks if the window is too small vertically
    if (newBottom - rect.top < mMinCropHeight) {
      newBottom = rect.top + mMinCropHeight;
    }

    // Checks if the window is too small vertically
    if (newBottom - rect.top > mMaxCropHeight) {
      newBottom = rect.top + mMaxCropHeight;
    }

    if (bounds.bottom - newBottom < snapMargin) {
      newBottom = bounds.bottom;
    }

    rect.bottom = newBottom;
  }


  // endregion

  // region: Inner class: Type

  /** The type of crop window move that is handled. */
  public enum Type {
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
    LEFT,
    TOP,
    RIGHT,
    BOTTOM,
    CENTER
  }
  // endregion
}
