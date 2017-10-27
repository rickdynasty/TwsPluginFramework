package com.tencent.tws.assistant.utils;

import java.lang.ref.WeakReference;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

public class BitmapUtil {
	

	private static final String TAG = "BitmapUtil";
	// synchronization lock
	private static final byte[] mPixelsLock = new byte[0];
	private static final int MIN_VALID_ALPHA = 28;
	private static WeakReference<int[]> mRefPixels = null;
	
	public static final int DEFAULT_PAINT_FLAGS = Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG;
	
	public static final float DENSITY_H = 1.5f;
	public static final float DENSITY_XH = 2f;
	public static final float DENSITY_XXH = 3f;
	    
	public static final float WIDTH_H = 480f;
	public static final float WIDTH_XH = 720f;
	public static final float WIDTH_XXH = 1080f;
	private static Rect mRect = new Rect();
	
	public static final String THEME_ICON_MASK = "launcher_theme_icon_mask";
	public static final String THEME_ICON_SHADOW = "launcher_theme_icon_shadow";
	
	public static final int TWS_ICON_SIZE_FRAME = 192;
	public static final int TWS_ICON_SIZE_SQUARE = 156;
	public static final int TWS_ICON_SIZE_NOT_SQUARE = 168;
	
	public static IconAnalyzedResult analyzeBitmap1(Bitmap srcBmp) {
//      long beforeTime = System.currentTimeMillis();
        final IconAnalyzedResult result = new IconAnalyzedResult();

        final int width = srcBmp.getWidth();
        final int height = srcBmp.getHeight();
        int pixelColor;

        synchronized (mPixelsLock) {
            int len = width * height;
            int[] pixels;
            if (mRefPixels == null || mRefPixels.get() == null
                    || mRefPixels.get().length < len) {
                pixels = new int[len];
                mRefPixels = new WeakReference<int[]>(pixels);
            } else {
                pixels = mRefPixels.get();
            }
            srcBmp.getPixels(pixels, 0, width, 0, 0, width, height);

            // according alpha filter pixels
            boolean flag1 = false;
            boolean flag2 = false;
            // or so close to
            for (int i = 0; i < width / 2; i++) {
                for (int j = 0; j < height; j++) {
                    int alpha;
                    if (!flag1) { // left edge
                        pixelColor = pixels[j * width + i];
                        alpha = Color.alpha(pixelColor);
                        if (alpha > MIN_VALID_ALPHA) {
                            result.rect.left = i;
                            flag1 = true;
                        }
                    }

                    if (!flag2) { // right edge
                        pixelColor = pixels[j * width + width - 1 - i];
                        alpha = Color.alpha(pixelColor);
                        if (alpha > MIN_VALID_ALPHA) {
                            // -1: in pure color round picture is cut
                            result.rect.right = width - i;
                            flag2 = true;
                        }
                    }
                }

                if (flag1 && flag2) {
                    break;
                }
            }

            flag1 = false;
            flag2 = false;
            // upper and lower approximation
            for (int j = 0; j < height / 2; j++) {
                for (int i = 0; i < width; i++) {
                    int alpha;
                    if (!flag1) { // top edge
                        pixelColor = pixels[j * width + i];
                        alpha = Color.alpha(pixelColor);
                        if (alpha > MIN_VALID_ALPHA) {
                            result.rect.top = j;
                            flag1 = true;
                        }
                    }

                    if (!flag2) { // bottom edge
                        pixelColor = pixels[(height - j - 1) * width + i];
                        alpha = Color.alpha(pixelColor);
                        if (alpha > MIN_VALID_ALPHA) {
                            result.rect.bottom = height - j;
                            flag2 = true;
                        }
                    }
                }

                if (flag1 && flag2) {
                    break;
                }
            }
            int pixelNum = 0;
            int rectWidth = result.rect.width();
//          int rectHeight = result.rect.height();
//          Log.e("hlx", "rectWidth = " + rectWidth + "; rectHeight = " + rectHeight);
            for (int j = result.rect.top; j < result.rect.bottom; j++) {
                for (int i = result.rect.left; i < result.rect.right; i++) {
                    pixelColor = pixels[j * rectWidth + i];
                    if (pixelColor != 0) {
                        int alpha = Color.alpha(pixelColor);
                        pixels[j * rectWidth + i] = alpha;
                        if (pixels[j * rectWidth + i] > 178) {
                            pixelNum++;
                        }
                    }
                }
            }
            
            float scale = (float) result.rect.width() / result.rect.height();
            float availability = (float) pixelNum
                    / (result.rect.width() * result.rect.height());
            result.isSquare = (scale >= 0.8f && scale <= 1.25f)
                    && availability >= 0.93f;
        }
//      long afterTime = System.currentTimeMillis();
//      Log.e("hlx", "intervelTime two = " + (afterTime - beforeTime));
        return result;
    }

	
	public static IconAnalyzedResult analyzeBitmap2(Bitmap srcBmp) {
		final IconAnalyzedResult result = new IconAnalyzedResult();

		final int width = srcBmp.getWidth();
		final int height = srcBmp.getHeight();
		int pixelColor;

		synchronized (mPixelsLock) {
			int len = width * height;
			int[] pixels;
			if (mRefPixels == null || mRefPixels.get() == null
					|| mRefPixels.get().length < len) {
				pixels = new int[len];
				mRefPixels = new WeakReference<int[]>(pixels);
			} else {
				pixels = mRefPixels.get();
			}
			srcBmp.getPixels(pixels, 0, width, 0, 0, width, height);

			// according alpha filter pixels
			boolean flag1 = false;
			boolean flag2 = false;
			// or so close to
			for (int i = 0; i < width / 2; i++) {
				for (int j = 0; j < height; j++) {
					int alpha;
					if (!flag1) { // left edge
						pixelColor = pixels[j * width + i];
						alpha = Color.alpha(pixelColor);
						if (alpha > MIN_VALID_ALPHA) {
							result.rect.left = i;
							flag1 = true;
						}
					}

					if (!flag2) { // right edge
						pixelColor = pixels[j * width + width - 1 - i];
						alpha = Color.alpha(pixelColor);
						if (alpha > MIN_VALID_ALPHA) {
							// -1: in pure color round picture is cut
							result.rect.right = width - i;
							flag2 = true;
						}
					}
				}

				if (flag1 && flag2) {
					break;
				}
			}

			flag1 = false;
			flag2 = false;
			// upper and lower approximation
			for (int j = 0; j < height / 2; j++) {
				for (int i = 0; i < width; i++) {
					int alpha;
					if (!flag1) { // top edge
						pixelColor = pixels[j * width + i];
						alpha = Color.alpha(pixelColor);
						if (alpha > MIN_VALID_ALPHA) {
							result.rect.top = j;
							flag1 = true;
						}
					}

					if (!flag2) { // bottom edge
						pixelColor = pixels[(height - j - 1) * width + i];
						alpha = Color.alpha(pixelColor);
						if (alpha > MIN_VALID_ALPHA) {
							result.rect.bottom = height - j;
							flag2 = true;
						}
					}
				}

				if (flag1 && flag2) {
					break;
				}
			}
		}
		return result;
	}
	
	public static Bitmap processOrdinaryIcon(Context context, Bitmap src, Bitmap dst,
			boolean preprocess, IconAnalyzedResult analyzedResult,
			boolean needIconLayer, BitmapFactory.Options options) {
		if (src == null || src.isRecycled()) {
			return null;
		}
		// check input dst bitmap whether we can reuse
		Bitmap res = null;
		// reuse the bitmap must be app_icon_content_size
		// 204px
		int outerSize = TWS_ICON_SIZE_FRAME;
		int iconSize = outerSize;
		if (options != null && options.inSampleSize > 1) {
			iconSize = outerSize / options.inSampleSize;
		}
		if (dst != null && dst.isMutable() && !dst.isRecycled() && dst.getWidth() == outerSize) {
			dst.eraseColor(Color.TRANSPARENT);
			res = dst;
		} else {
			if (iconSize != 0) {
				res = createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888);
				
//				return res;
			}
		}

		if (outerSize != 0 && res != null) {
			int shadowHeight = (int) (0 * iconSize / (float) outerSize);
			// analysis of srcBitmap(background,shape)
			Canvas canvas = new Canvas(res);
			Paint paint = new Paint(DEFAULT_PAINT_FLAGS);
			int innerSize = TWS_ICON_SIZE_SQUARE;
			if (analyzedResult.isSquare && preprocess) {
				float a = getScreenDensity(context);
//				innerSize = innerSize + (int) (2 * getScreenDensity());
				innerSize = (int) (innerSize * iconSize / (float) outerSize);
				RectF desRect = scaleInnerIcon(innerSize, analyzedResult);
				float width = desRect.width();
				float height = desRect.height();
				desRect.offset((iconSize - desRect.width()) / 2, 
						       (iconSize - desRect.height() - shadowHeight) / 2);
				canvas.drawBitmap(src, analyzedResult.rect, desRect, paint);
			} else {
				// old drawable is not a rectangle need add mask
				// first to draw the base of color(on the basis of BitmapAnalyzedResult)
				// this num order to random get background drawable
				int num = 6;
				mRect.set(0, 0, iconSize, iconSize);
				// bruce modify if (num > 0) to false
				if (num > 0) {
					// get icon the background
//					Bitmap bg = getIconBackground(key, num);
//					if (bg != null) {
//						canvas.drawBitmap(bg, null, mRect, paint);
//					}
				}

				// picture drawing draw the inner box,auto adapt the innersize
				int scale = 100;
				int dy = 0;
				innerSize = TWS_ICON_SIZE_NOT_SQUARE;
				innerSize = (int) (innerSize * ((float) scale) / 100 * iconSize / outerSize);
				RectF desRect = scaleInnerIcon(innerSize, analyzedResult);
				float width1 = desRect.width();
				float height1 = desRect.height();
				desRect.offset((iconSize - desRect.width()) / 2, 
							   (iconSize - desRect.height() - shadowHeight) / 2 - dy);
				canvas.drawBitmap(src, analyzedResult.rect, desRect, paint);
			}
		}
		return res;
	}
	
	public static Bitmap createBitmap(int width, int height, Config config) {
		if (width <= 0 || height <= 0) {
			return null;
		}

		try {
			if (config != null) {
				return Bitmap.createBitmap(width, height, config);
			} else {
				return Bitmap.createBitmap(width, height, Config.ARGB_8888);
			}
		} catch (OutOfMemoryError ex) {
			return null;
		}
	}
	
	public static float getScreenDensity(Context context) {
    	int widthPixels = 0;
		android.view.WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		if (manager != null) {
			DisplayMetrics dm = new DisplayMetrics();
			Display display = manager.getDefaultDisplay();
			if (display != null) {
				manager.getDefaultDisplay().getMetrics(dm);
				widthPixels = dm.widthPixels;
				float originDensity = dm.density;
		    	float adaptDensity = 0;
		    	if(widthPixels >= WIDTH_XXH) {
		    		adaptDensity = DENSITY_XXH;
		    	} else if(widthPixels >= WIDTH_XH) {
		    		adaptDensity =  DENSITY_XH;
		    	} else if(widthPixels >= WIDTH_H) {
		    		adaptDensity = DENSITY_H;
		    	}
		    	return originDensity > adaptDensity ? originDensity : adaptDensity;
			}
		}
		return 0;
    }
	
	public static RectF scaleInnerIcon(int size, IconAnalyzedResult result) {
		RectF rect;
		// calculate
		float scale = (float) result.rect.width() / result.rect.height();
		scale = scale > 1 ? 1 / scale : scale;
		if (result.isSquare) {
			// create rect
			if (result.rect.width() > result.rect.height()) {
				rect = new RectF(0, 0, size / scale, size);
			} else {
				rect = new RectF(0, 0, size, size / scale);
			}
		} else {
			// create rect
			if (result.rect.width() > result.rect.height()) {
				rect = new RectF(0, 0, size, size*scale);
			} else {
				rect = new RectF(0, 0, size * scale, size);
			}
		}
		return rect;
	}
	
	public static void blendIconLayer(Canvas canvas, Rect desRect, Paint paint,
			Bitmap mask, Bitmap shadow) {
		if (mask != null) {
			paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
			canvas.drawBitmap(mask, null, desRect, paint);
			paint.setXfermode(null);
		}

		if (shadow != null) {
			canvas.drawBitmap(shadow, null, desRect, paint);
		}
	}
	
	public static class IconAnalyzedResult {
		public Rect rect = new Rect(0, 0, 0, 0);
		public boolean isSquare = false;
	}


}
