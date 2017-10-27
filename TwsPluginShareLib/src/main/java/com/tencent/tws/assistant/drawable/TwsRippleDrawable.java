/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.tws.assistant.drawable;


import java.io.IOException;
import java.util.Arrays;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;

import com.tencent.tws.sharelib.R;

/**
 * Drawable that shows a ripple effect in response to state changes. The
 * anchoring position of the ripple for a given state may be specified by
 * calling {@link #twsSetHotspot(float, float)} with the corresponding state
 * attribute identifier.
 * <p>
 * A touch feedback drawable may contain multiple child layers, including a
 * special mask layer that is not drawn to the screen. A single layer may be
 * set as the mask from XML by specifying its {@code android:id} value as
 * {@link android.R.id#mask}. At run time, a single layer may be set as the
 * mask using {@code setId(..., android.R.id.mask)} or an existing mask layer
 * may be replaced using {@code setDrawableByLayerId(android.R.id.mask, ...)}.
 * <pre>
 * <code>&lt!-- A red ripple masked against an opaque rectangle. --/>
 * &ltripple android:color="#ffff0000">
 *   &ltitem android:id="@android:id/mask"
 *         android:drawable="@android:color/white" />
 * &lt/ripple></code>
 * </pre>
 * <p>
 * If a mask layer is set, the ripple effect will be masked against that layer
 * before it is drawn over the composite of the remaining child layers.
 * <p>
 * If no mask layer is set, the ripple effect is masked against the composite
 * of the child layers.
 * <pre>
 * <code>&lt!-- A green ripple drawn atop a black rectangle. --/>
 * &ltripple android:color="#ff00ff00">
 *   &ltitem android:drawable="@android:color/black" />
 * &lt/ripple>
 *
 * &lt!-- A blue ripple drawn atop a drawable resource. --/>
 * &ltripple android:color="#ff0000ff">
 *   &ltitem android:drawable="@drawable/my_drawable" />
 * &lt/ripple></code>
 * </pre>
 * <p>
 * If no child layers or mask is specified and the ripple is set as a View
 * background, the ripple will be drawn atop the first available parent
 * background within the View's hierarchy. In this case, the drawing region
 * may extend outside of the Drawable bounds.
 * <pre>
 * <code>&lt!-- An unbounded red ripple. --/>
 * &ltripple android:color="#ffff0000" /></code>
 * </pre>
 *
 * @attr ref android.R.styleable#RippleDrawable_android_color
 */
public class TwsRippleDrawable extends TwsLayerDrawable {
    /**
     * Radius value that specifies the ripple radius should be computed based
     * on the size of the ripple's container.
     */
    public static final int RADIUS_AUTO = -1;

    private static final int MASK_UNKNOWN = -1;
    private static final int MASK_NONE = 0;
    private static final int MASK_CONTENT = 1;
    private static final int MASK_EXPLICIT = 2;

    /** The maximum number of ripples supported. */
    private static final int MAX_RIPPLES = 10;

    private final Rect mTempRect = new Rect();

    /** Current ripple effect bounds, used to constrain ripple effects. */
    private final Rect mHotspotBounds = new Rect();

    /** Current drawing bounds, used to compute dirty region. */
    private final Rect mDrawingBounds = new Rect();

    /** Current dirty bounds, union of current and previous drawing bounds. */
    private final Rect mDirtyBounds = new Rect();

    /** Mirrors mLayerState with some extra information. */
    private RippleState mState;

    /** The masking layer, e.g. the layer with id R.id.mask. */
    private Drawable mMask;

    /** The current background. May be actively animating or pending entry. */
    private TwsRippleBackground mBackground;

    private Bitmap mMaskBuffer;
    private BitmapShader mMaskShader;
    private Canvas mMaskCanvas;
    private Matrix mMaskMatrix;
    private PorterDuffColorFilter mMaskColorFilter;
    private boolean mHasValidMask;

    /** Whether we expect to draw a background when visible. */
    private boolean mBackgroundActive;

    /** The current ripple. May be actively animating or pending entry. */
    private TwsRippleForeground mRipple;

    /** Whether we expect to draw a ripple when visible. */
    private boolean mRippleActive;

    // Hotspot coordinates that are awaiting activation.
    private float mPendingX;
    private float mPendingY;
    private boolean mHasPending;

    /**
     * Lazily-created array of actively animating ripples. Inactive ripples are
     * pruned during draw(). The locations of these will not change.
     */
    private TwsRippleForeground[] mExitingRipples;
    private int mExitingRipplesCount = 0;

    /** Paint used to control appearance of ripples. */
    private Paint mRipplePaint;

    /** Target density of the display into which ripples are drawn. */
    private float mDensity = 1.0f;

    /** Whether bounds are being overridden. */
    private boolean mOverrideBounds;

    public static final int RIPPLE_STYLE_NORMAL = 1000;
    public static final int RIPPLE_STYLE_CLEAR = 1001;
    public static final int RIPPLE_STYLE_RING = 1002;
    private int mRippleStyle = RIPPLE_STYLE_NORMAL;

    private boolean mIsSupportCheckedRipple = true;
    private boolean mChecked = true;
    private boolean mHasCheckedPending = false;

    /**
     * Constructor used for drawable inflation.
     */
    TwsRippleDrawable() {
        this(new RippleState(null, null, null), null);
    }

    /**
     * Creates a new ripple drawable with the specified ripple color and
     * optional content and mask drawables.
     *
     * @param color The ripple color
     * @param content The content drawable, may be {@code null}
     * @param mask The mask drawable, may be {@code null}
     */
    public TwsRippleDrawable(@NonNull ColorStateList color, @Nullable Drawable content,
            @Nullable Drawable mask) {
        this(color, content, mask, RIPPLE_STYLE_NORMAL);
    }

    public TwsRippleDrawable(@NonNull ColorStateList color, @Nullable Drawable content,
            @Nullable Drawable mask, int rippleStyle) {
        this(new RippleState(null, null, null), null);
        
        if (color == null) {
            throw new IllegalArgumentException("RippleDrawable requires a non-null color");
        }
        
        if (content != null) {
            addLayer(content, null, 0, 0, 0, 0, 0);
        }
        
        if (mask != null) {
            addLayer(mask, null, R.id.mask, 0, 0, 0, 0);
        }
        
        setColor(color);
        ensurePadding();
        refreshPadding();
        updateLocalState();

        mRippleStyle = rippleStyle;
        if (mRippleStyle == RIPPLE_STYLE_RING) {
            mHasCheckedPending = true;
        }
    }

    @Override
    public void jumpToCurrentState() {
        super.jumpToCurrentState();

        if (mRipple != null) {
            mRipple.end();
        }

        if (mBackground != null) {
            mBackground.end();
        }

        cancelExitingRipples();
    }

    private void cancelExitingRipples() {
        final int count = mExitingRipplesCount;
        final TwsRippleForeground[] ripples = mExitingRipples;
        for (int i = 0; i < count; i++) {
            ripples[i].end();
        }

        if (ripples != null) {
            Arrays.fill(ripples, 0, count, null);
        }
        mExitingRipplesCount = 0;

        // Always draw an additional "clean" frame after canceling animations.
        invalidateSelf(false);
    }

    @Override
    public int getOpacity() {
        // Worst-case scenario.
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    protected boolean onStateChange(int[] stateSet) {
        final boolean changed = super.onStateChange(stateSet);

        boolean enabled = false;
        boolean pressed = false;
        boolean focused = false;
        boolean checked = false;

        for (int state : stateSet) {
            if (state == android.R.attr.state_enabled) {
                enabled = true;
            } else if (state == android.R.attr.state_focused) {
                focused = true;
            } else if (state == android.R.attr.state_pressed) {
                pressed = true;
            } else if (state == android.R.attr.state_checked) {
                checked = true;
            }
        }

        if (mRippleStyle == RIPPLE_STYLE_NORMAL) {
            setRippleActive(enabled && pressed, false);
            setBackgroundActive(focused || (enabled && pressed), focused);
        } else if (mRippleStyle == RIPPLE_STYLE_CLEAR) {
            setRippleActive(focused || (enabled && pressed), focused);
        } else if (mRippleStyle == RIPPLE_STYLE_RING){
            if (mIsSupportCheckedRipple) {
                if (mChecked != checked) {
                    mChecked = checked;
                    startCheckedRipple(mChecked);
                }
            }
        }

        return changed;
    }

    private void setRippleActive(boolean active, boolean focused) {
        if (mRippleActive != active) {
            mRippleActive = active;
            if (active) {
                tryRippleEnter(focused);
            } else {
                tryRippleExit();
            }
        }
    }

    private void setBackgroundActive(boolean active, boolean focused) {
        if (mBackgroundActive != active) {
            mBackgroundActive = active;
            if (active) {
                tryBackgroundEnter(focused);
            } else {
                tryBackgroundExit();
            }
        }
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);

        if (!mOverrideBounds) {
            mHotspotBounds.set(bounds);
            onHotspotBoundsChanged();
        }

        if (mBackground != null) {
            mBackground.onBoundsChange();
        }

        if (mRipple != null) {
            mRipple.onBoundsChange();
        }

        invalidateSelf();
    }

    @Override
    public boolean setVisible(boolean visible, boolean restart) {
        final boolean changed = super.setVisible(visible, restart);

        if (!visible) {
            clearHotspots();
        } else if (changed) {
            // If we just became visible, ensure the background and ripple
            // visibilities are consistent with their internal states.
            if (mRippleActive) {
                tryRippleEnter(false);
            }

            if (mBackgroundActive) {
                tryBackgroundEnter(false);
            }

            // Skip animations, just show the correct final states.
            jumpToCurrentState();
        }

        return changed;
    }

    /**
     * @hide
     */
    @Override
    public boolean isProjected() {
        // If the layer is bounded, then we don't need to project.
        if (isBounded()) {
            return false;
        }

        // Otherwise, if the maximum radius is contained entirely within the
        // bounds then we don't need to project. This is sort of a hack to
        // prevent check box ripples from being projected across the edges of
        // scroll views. It does not impact rendering performance, and it can
        // be removed once we have better handling of projection in scrollable
        // views.
        final int radius = mState.mMaxRadius;
        final Rect drawableBounds = getBounds();
        final Rect hotspotBounds = mHotspotBounds;
        if (radius != RADIUS_AUTO
                && radius <= hotspotBounds.width() / 2
                && radius <= hotspotBounds.height() / 2
                && (drawableBounds.equals(hotspotBounds)
                        || drawableBounds.contains(hotspotBounds))) {
            return false;
        }

        return true;
    }

    private boolean isBounded() {
        return getNumberOfLayers() > 0;
    }

    @Override
    public boolean isStateful() {
        return true;
    }

    /**
     * Sets the ripple color.
     *
     * @param color Ripple color as a color state list.
     *
     * @attr ref android.R.styleable#RippleDrawable_android_color
     */
    public void setColor(ColorStateList color) {
        mState.mColor = color;
        invalidateSelf(false);
    }

    /**
     * Sets the radius in pixels of the fully expanded ripple.
     *
     * @param radius ripple radius in pixels, or {@link #RADIUS_AUTO} to
     *               compute the radius based on the container size
     * @attr ref android.R.styleable#RippleDrawable_radius
     */
    public void setRadius(int radius) {
        mState.mMaxRadius = radius;
        invalidateSelf(false);
    }

    /**
     * @return the radius in pixels of the fully expanded ripple if an explicit
     *         radius has been set, or {@link #RADIUS_AUTO} if the radius is
     *         computed based on the container size
     * @attr ref android.R.styleable#RippleDrawable_radius
     */
    public int getRadius() {
        return mState.mMaxRadius;
    }

    public void setMaxRadius(int radius) {
        mState.mMaxRadius = radius;
        invalidateSelf(false);
    }

    @Override
    public void inflate(Resources r, XmlPullParser parser, AttributeSet attrs)
            throws XmlPullParserException, IOException {
        final TypedArray a = r.obtainAttributes(attrs, R.styleable.RippleDrawable);
        updateStateFromTypedArray(a);
        a.recycle();

        // Force padding default to STACK before inflating.
        setPaddingMode(PADDING_MODE_STACK);

        super.inflate(r, parser, attrs);

        setTargetDensity(r.getDisplayMetrics());

        updateLocalState();
    }

    @Override
    public boolean setDrawableByLayerId(int id, Drawable drawable) {
        if (super.setDrawableByLayerId(id, drawable)) {
            if (id == R.id.mask) {
                mMask = drawable;
                mHasValidMask = false;
            }

            return true;
        }

        return false;
    }

    /**
     * Specifies how layer padding should affect the bounds of subsequent
     * layers. The default and recommended value for RippleDrawable is
     * {@link #PADDING_MODE_STACK}.
     *
     * @param mode padding mode, one of:
     *            <ul>
     *            <li>{@link #PADDING_MODE_NEST} to nest each layer inside the
     *            padding of the previous layer
     *            <li>{@link #PADDING_MODE_STACK} to stack each layer directly
     *            atop the previous layer
     *            </ul>
     * @see #getPaddingMode()
     */
    @Override
    public void setPaddingMode(int mode) {
        super.setPaddingMode(mode);
    }

    /**
     * Initializes the constant state from the values in the typed array.
     */
    private void updateStateFromTypedArray(TypedArray a) throws XmlPullParserException {
        final RippleState state = mState;

        // Account for any configuration changes.
//        state.mChangingConfigurations |= a.getChangingConfigurations();

        // Extract the theme attributes, if any.
//        state.mTouchThemeAttrs = a.extractThemeAttrs();

        final ColorStateList color = a.getColorStateList(R.styleable.RippleDrawable_android_color);
        if (color != null) {
            mState.mColor = color;
        }

        mState.mMaxRadius = a.getDimensionPixelSize(
                R.styleable.RippleDrawable_android_radius, mState.mMaxRadius);

        verifyRequiredAttributes(a);
    }

    private void verifyRequiredAttributes(TypedArray a) throws XmlPullParserException {
        if (mState.mColor == null && (mState.mTouchThemeAttrs == null
                || mState.mTouchThemeAttrs[R.styleable.RippleDrawable_android_color] == 0)) {
            throw new XmlPullParserException(a.getPositionDescription() +
                    ": <ripple> requires a valid color attribute");
        }
    }

    /**
     * Set the density at which this drawable will be rendered.
     *
     * @param metrics The display metrics for this drawable.
     */
    private void setTargetDensity(DisplayMetrics metrics) {
        if (mDensity != metrics.density) {
            mDensity = metrics.density;
            invalidateSelf(false);
        }
    }

    public void twsSetHotspot(float x, float y) {
        if (mHasCheckedPending) {
            return;
        }

        if (mRipple == null || mBackground == null) {
            mPendingX = x;
            mPendingY = y;
            mHasPending = true;
        }

        if (mRipple != null) {
            mRipple.move(x, y);
        }
    }

    /**
     * Creates an active hotspot at the specified location.
     */
    private void tryBackgroundEnter(boolean focused) {
        if (mBackground == null) {
            mBackground = new TwsRippleBackground(this, mHotspotBounds);
        }

        mBackground.setup(mState.mMaxRadius, mDensity);
        mBackground.enter(focused);
    }

    private void tryBackgroundExit() {
        if (mBackground != null) {
            // Don't null out the background, we need it to draw!
            mBackground.exit();
        }
    }

    /**
     * Attempts to start an enter animation for the active hotspot. Fails if
     * there are too many animating ripples.
     */
    private void tryRippleEnter(boolean focused) {
        if (mExitingRipplesCount >= MAX_RIPPLES) {
            // This should never happen unless the user is tapping like a maniac
            // or there is a bug that's preventing ripples from being removed.
            return;
        }

        if (mRipple == null) {
            final float x;
            final float y;
            if (mHasPending && !mHasCheckedPending) {
                mHasPending = false;
                x = mPendingX;
                y = mPendingY;
            } else {
                x = mHotspotBounds.exactCenterX();
                y = mHotspotBounds.exactCenterY();
            }

//            final boolean isBounded = isBounded();
            final boolean isBounded = false;
            if (mRippleStyle == RIPPLE_STYLE_NORMAL) {
                mRipple = new TwsRippleForeground(this, mHotspotBounds, x, y, isBounded);
            } else if (mRippleStyle == RIPPLE_STYLE_CLEAR) {
                mRipple = new TwsRippleForegroundClear(this, mHotspotBounds, x, y, isBounded);
            } else if (mRippleStyle == RIPPLE_STYLE_RING) {
                mRipple = new TwsRippleForegroundRing(this, mHotspotBounds, x, y, isBounded);
            }
        }

        mRipple.setup(mState.mMaxRadius, mDensity);
        mRipple.enter(focused);
    }

    /**
     * Attempts to start an exit animation for the active hotspot. Fails if
     * there is no active hotspot.
     */
    private void tryRippleExit() {
        if (mRipple != null) {
            if (mExitingRipples == null) {
                mExitingRipples = new TwsRippleForeground[MAX_RIPPLES];
            }
            mExitingRipples[mExitingRipplesCount++] = mRipple;
            mRipple.exit();
            mRipple = null;
        }
    }

    /**
     * Cancels and removes the active ripple, all exiting ripples, and the
     * background. Nothing will be drawn after this method is called.
     */
    private void clearHotspots() {
        if (mRipple != null) {
            mRipple.end();
            mRipple = null;
            mRippleActive = false;
        }

        if (mBackground != null) {
            mBackground.end();
            mBackground = null;
            mBackgroundActive = false;
        }

        cancelExitingRipples();
    }

    public void twsSetHotspotBounds(int left, int top, int right, int bottom) {
        mOverrideBounds = true;
        mHotspotBounds.set(left, top, right, bottom);

        onHotspotBoundsChanged();
    }

    public void twsGetHotspotBounds(Rect outRect) {
        outRect.set(mHotspotBounds);
    }

    /**
     * Notifies all the animating ripples that the hotspot bounds have changed.
     */
    private void onHotspotBoundsChanged() {
        final int count = mExitingRipplesCount;
        final TwsRippleForeground[] ripples = mExitingRipples;
        for (int i = 0; i < count; i++) {
            ripples[i].onHotspotBoundsChanged();
        }

        if (mRipple != null) {
            mRipple.onHotspotBoundsChanged();
        }

        if (mBackground != null) {
            mBackground.onHotspotBoundsChanged();
        }
    }

    /**
     * Populates <code>outline</code> with the first available layer outline,
     * excluding the mask layer.
     *
     * @param outline Outline in which to place the first available layer outline
     */
    /*@Override
    public void getOutline(@NonNull Outline outline) {
        final LayerState state = mLayerState;
        final ChildDrawable[] children = state.mChildren;
        final int N = state.mNum;
        for (int i = 0; i < N; i++) {
            if (children[i].mId != R.id.mask) {
                children[i].mDrawable.getOutline(outline);
                if (!outline.isEmpty()) return;
            }
        }
    }*/

    /**
     * Optimized for drawing ripples with a mask layer and optional content.
     */
    @Override
    public void draw(@NonNull Canvas canvas) {
        pruneRipples();

        // Clip to the dirty bounds, which will be the drawable bounds if we
        // have a mask or content and the ripple bounds if we're projecting.
        final Rect bounds = twsGetDirtyBounds();
        final int saveCount = canvas.save(Canvas.CLIP_SAVE_FLAG);
        canvas.clipRect(bounds);

        drawContent(canvas);
        drawBackgroundAndRipples(canvas, bounds);

        canvas.restoreToCount(saveCount);
    }

    @Override
    public void invalidateSelf() {
        invalidateSelf(true);
    }

    void invalidateSelf(boolean invalidateMask) {
        super.invalidateSelf();

        if (invalidateMask) {
            // Force the mask to update on the next draw().
            mHasValidMask = false;
        }

    }

    private void pruneRipples() {
        int remaining = 0;

        // Move remaining entries into pruned spaces.
        final TwsRippleForeground[] ripples = mExitingRipples;
        final int count = mExitingRipplesCount;
        for (int i = 0; i < count; i++) {
            if (!ripples[i].hasFinishedExit()) {
                ripples[remaining++] = ripples[i];
            }
        }

        // Null out the remaining entries.
        for (int i = remaining; i < count; i++) {
            ripples[i] = null;
        }

        mExitingRipplesCount = remaining;
    }

    /**
     * @return whether we need to use a mask
     */
    private void updateMaskShaderIfNeeded() {
        if (mHasValidMask) {
            return;
        }

        final int maskType = getMaskType();
        if (maskType == MASK_UNKNOWN) {
            return;
        }

        mHasValidMask = true;

        final Rect bounds = getBounds();
        if (maskType == MASK_NONE || bounds.isEmpty()) {
            if (mMaskBuffer != null) {
                mMaskBuffer.recycle();
                mMaskBuffer = null;
                mMaskShader = null;
                mMaskCanvas = null;
            }
            mMaskMatrix = null;
            mMaskColorFilter = null;
            return;
        }

        // Ensure we have a correctly-sized buffer.
        if (mMaskBuffer == null
                || mMaskBuffer.getWidth() != bounds.width()
                || mMaskBuffer.getHeight() != bounds.height()) {
            if (mMaskBuffer != null) {
                mMaskBuffer.recycle();
            }

            mMaskBuffer = Bitmap.createBitmap(
                    bounds.width(), bounds.height(), Bitmap.Config.ALPHA_8);
            mMaskShader = new BitmapShader(mMaskBuffer,
                    Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
            mMaskCanvas = new Canvas(mMaskBuffer);
        } else {
            mMaskBuffer.eraseColor(Color.TRANSPARENT);
        }

        if (mMaskMatrix == null) {
            mMaskMatrix = new Matrix();
        } else {
            mMaskMatrix.reset();
        }

        if (mMaskColorFilter == null) {
            final int color = mState.mColor.getColorForState(getState(), Color.BLACK);
            final int fullAlphaColor = color | (0xFF << 24);
            mMaskColorFilter = new PorterDuffColorFilter(fullAlphaColor, PorterDuff.Mode.SRC_IN);
        }

        // Draw the appropriate mask anchored to (0,0).
        final int left = bounds.left;
        final int top = bounds.top;
        mMaskCanvas.translate(-left, -top);
        if (maskType == MASK_EXPLICIT) {
            drawMask(mMaskCanvas);
        } else if (maskType == MASK_CONTENT) {
            drawContent(mMaskCanvas);
        }
        mMaskCanvas.translate(left, top);
    }

    private int getMaskType() {
        if (mRipple == null && mExitingRipplesCount <= 0
                && (mBackground == null || !mBackground.isVisible())) {
            // We might need a mask later.
            return MASK_UNKNOWN;
        }

        if (mMask != null) {
            if (mMask.getOpacity() == PixelFormat.OPAQUE) {
                // Clipping handles opaque explicit masks.
                return MASK_NONE;
            } else {
                return MASK_EXPLICIT;
            }
        }

        // Check for non-opaque, non-mask content.
        final ChildDrawable[] array = mLayerState.mChildren;
        final int count = mLayerState.mNum;
        for (int i = 0; i < count; i++) {
            if (array[i].mDrawable.getOpacity() != PixelFormat.OPAQUE) {
                return MASK_CONTENT;
            }
        }

        // Clipping handles opaque content.
        return MASK_NONE;
    }

    private void drawContent(Canvas canvas) {
        // Draw everything except the mask.
        final ChildDrawable[] array = mLayerState.mChildren;
        final int count = mLayerState.mNum;
        for (int i = 0; i < count; i++) {
            if (array[i].mId != R.id.mask) {
                array[i].mDrawable.draw(canvas);
            }
        }
    }

    private void drawBackgroundAndRipples(Canvas canvas, Rect rippleBounds) {
        final TwsRippleForeground active = mRipple;
        final TwsRippleBackground background = mBackground;
        final int count = mExitingRipplesCount;
        if (active == null && count <= 0 && (background == null || !background.isVisible())) {
            // Move along, nothing to draw here.
            return;
        }

        final float x = mHotspotBounds.exactCenterX();
        final float y = mHotspotBounds.exactCenterY();

        if (mRippleStyle == RIPPLE_STYLE_CLEAR) {
            canvas.saveLayer(rippleBounds.left, rippleBounds.top, rippleBounds.right, rippleBounds.bottom, new Paint(), Canvas.ALL_SAVE_FLAG);
        }

        canvas.translate(x, y);

        updateMaskShaderIfNeeded();

        // Position the shader to account for canvas translation.
        if (mMaskShader != null) {
            final Rect bounds = getBounds();
            mMaskMatrix.setTranslate(bounds.left - x, bounds.top - y);
            mMaskShader.setLocalMatrix(mMaskMatrix);
        }

        // Grab the color for the current state and cut the alpha channel in
        // half so that the ripple and background together yield full alpha.
        final int color = mState.mColor.getColorForState(getState(), Color.BLACK);
        final int halfAlpha = (Color.alpha(color) / 2) << 24;
        final Paint p = getRipplePaint();

        if (mMaskColorFilter != null) {
            // The ripple timing depends on the paint's alpha value, so we need
            // to push just the alpha channel into the paint and let the filter
            // handle the full-alpha color.
//            final int fullAlphaColor = color | (0xFF << 24);
//            mMaskColorFilter.setColor(fullAlphaColor);

            p.setColor(halfAlpha);
            p.setColorFilter(mMaskColorFilter);
            p.setShader(mMaskShader);
        } else {
            final int halfAlphaColor = (color & 0xFFFFFF) | halfAlpha;
            p.setColor(halfAlphaColor);
            p.setColorFilter(null);
            p.setShader(null);
        }

        if (background != null && background.isVisible()) {
            background.draw(canvas, p);
        }

        if (count > 0) {
            final TwsRippleForeground[] ripples = mExitingRipples;
            for (int i = 0; i < count; i++) {
                ripples[i].draw(canvas, p);
            }
        }

        if (active != null) {
            active.draw(canvas, p);
        }

        canvas.translate(-x, -y);
    }

    private void drawMask(Canvas canvas) {
        mMask.draw(canvas);
    }

    private Paint getRipplePaint() {
        if (mRipplePaint == null) {
            mRipplePaint = new Paint();
            mRipplePaint.setAntiAlias(true);
            mRipplePaint.setStyle(Paint.Style.FILL);
        }
        return mRipplePaint;
    }

    public Rect twsGetDirtyBounds() {
        if (!isBounded()) {
            final Rect drawingBounds = mDrawingBounds;
            final Rect dirtyBounds = mDirtyBounds;
            dirtyBounds.set(drawingBounds);
            drawingBounds.setEmpty();

            final int cX = (int) mHotspotBounds.exactCenterX();
            final int cY = (int) mHotspotBounds.exactCenterY();
            final Rect rippleBounds = mTempRect;

            final TwsRippleForeground[] activeRipples = mExitingRipples;
            final int N = mExitingRipplesCount;
            for (int i = 0; i < N; i++) {
                activeRipples[i].getBounds(rippleBounds);
                rippleBounds.offset(cX, cY);
                drawingBounds.union(rippleBounds);
            }

            if (mRippleStyle == RIPPLE_STYLE_CLEAR || mRippleStyle == RIPPLE_STYLE_RING) {
                final TwsRippleForeground ripple = mRipple;
                if (ripple != null) {
                    ripple.getBounds(rippleBounds);
                    rippleBounds.offset(cX, cY);
                    drawingBounds.union(rippleBounds);
                }
            }

            final TwsRippleBackground background = mBackground;
            if (background != null) {
                background.getBounds(rippleBounds);
                rippleBounds.offset(cX, cY);
                drawingBounds.union(rippleBounds);
            }

            dirtyBounds.union(drawingBounds);
            dirtyBounds.union(super.getBounds());
            return dirtyBounds;
        } else {
            return getBounds();
        }
    }

    @Override
    public ConstantState getConstantState() {
        return mState;
    }

    @Override
    public Drawable mutate() {
        super.mutate();

        // LayerDrawable creates a new state using createConstantState, so
        // this should always be a safe cast.
        mState = (RippleState) mLayerState;

        // The locally cached drawable may have changed.
        mMask = findDrawableByLayerId(R.id.mask);

        return this;
    }

    @Override
    RippleState createConstantState(LayerState state, Resources res) {
        return new RippleState(state, this, res);
    }

    static class RippleState extends LayerState {
        int[] mTouchThemeAttrs;
        ColorStateList mColor = ColorStateList.valueOf(Color.MAGENTA);
        int mMaxRadius = RADIUS_AUTO;

        public RippleState(LayerState orig, TwsRippleDrawable owner, Resources res) {
            super(orig, owner, res);

            if (orig != null && orig instanceof RippleState) {
                final RippleState origs = (RippleState) orig;
                mTouchThemeAttrs = origs.mTouchThemeAttrs;
                mColor = origs.mColor;
                mMaxRadius = origs.mMaxRadius;
            }
        }

        @Override
        public Drawable newDrawable() {
            return new TwsRippleDrawable(this, null);
        }

        @Override
        public Drawable newDrawable(Resources res) {
            return new TwsRippleDrawable(this, res);
        }

        @Override
        public int getChangingConfigurations() {
//            return super.getChangingConfigurations()
//                    | (mColor != null ? mColor.getChangingConfigurations() : 0);
            return super.getChangingConfigurations();
        }
    }

    private TwsRippleDrawable(RippleState state, Resources res) {
        mState = new RippleState(state, this, res);
        mLayerState = mState;

        if (mState.mNum > 0) {
            ensurePadding();
            refreshPadding();
        }

        if (res != null) {
            mDensity = res.getDisplayMetrics().density;
        }

        updateLocalState();
    }

    private void updateLocalState() {
        // Initialize from constant state.
        mMask = findDrawableByLayerId(R.id.mask);
    }

    void removeRipple(TwsRippleForeground ripple) {
        if (mRipple != null && ripple != null && ripple == mRipple) {
            mRipple = null;
            invalidateSelf();
        }
    }

    public void setSupportCheckedRipple(boolean checkedRipple) {
        mIsSupportCheckedRipple = checkedRipple;
    }

    public void startCheckedRipple(boolean isChecked) {
        if (mRippleActive != isChecked) {
            mRippleActive = isChecked;
            if (isChecked) {
                tryRippleEnter(false);
            } else {
//                tryRippleExit();
            }
        }
    }
}
