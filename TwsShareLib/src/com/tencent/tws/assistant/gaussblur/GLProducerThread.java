package com.tencent.tws.assistant.gaussblur;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;
import javax.microedition.khronos.opengles.GL;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.opengl.GLUtils;
import android.os.Handler;
import android.view.View;
import android.view.ViewParent;
import android.widget.ImageView;

@SuppressLint("NewApi")
public class GLProducerThread extends Thread {

	private AtomicBoolean mShouldRender;
	private SurfaceTexture mSurfaceTexture;
	public static final int CALC_FINISHED = 1;
	private GLRendererImpl mRenderer = null;
	
	private EGL10 mEgl;
	private EGLDisplay mEglDisplay = EGL10.EGL_NO_DISPLAY;
	private EGLContext mEglContext = EGL10.EGL_NO_CONTEXT;
	private EGLSurface mEglSurface = EGL10.EGL_NO_SURFACE;
	private GL mGL;
	
	private static final int EGL_CONTEXT_CLIENT_VERSION = 0x3098;
	private static final int EGL_OPENGL_ES2_BIT = 4;
	
	private boolean mQuitThread = false;
	
	public GLProducerThread(SurfaceTexture surfaceTexture, Context context, int width, int height, AtomicBoolean shouldRender) 
	{
		if(mRenderer == null)
			mRenderer = new GLRendererImpl(context);
		
		mRenderer.setViewport(width, height);
		mSurfaceTexture = surfaceTexture;
		mShouldRender = shouldRender;
	}
	
	private static int[] getConfig() 
	{
        return new int[] {
                EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
                EGL10.EGL_RED_SIZE, 8,
                EGL10.EGL_GREEN_SIZE, 8,
                EGL10.EGL_BLUE_SIZE, 8,
                EGL10.EGL_ALPHA_SIZE, 8,
                EGL10.EGL_DEPTH_SIZE, 16,
                EGL10.EGL_STENCIL_SIZE, 8,
                EGL10.EGL_NONE
        };
    }
	
	private void initGL()
	{
		mEgl = (EGL10)EGLContext.getEGL();
		
		mEglDisplay = mEgl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
		if (mEglDisplay == EGL10.EGL_NO_DISPLAY) 
		{
			throw new RuntimeException("eglGetdisplay failed : " + GLUtils.getEGLErrorString(mEgl.eglGetError()));
		}
		
		int []version = new int[2];
		if (!mEgl.eglInitialize(mEglDisplay, version))
		{
			throw new RuntimeException("eglInitialize failed : " + GLUtils.getEGLErrorString(mEgl.eglGetError()));
		}
		
		int []configAttribs = {
				EGL10.EGL_BUFFER_SIZE, 32,
				EGL10.EGL_ALPHA_SIZE, 8,
				EGL10.EGL_BLUE_SIZE, 8,
				EGL10.EGL_GREEN_SIZE, 8,
				EGL10.EGL_RED_SIZE, 8,
				EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
				EGL10.EGL_SURFACE_TYPE, EGL10.EGL_WINDOW_BIT,
				EGL10.EGL_NONE
		};
		
		int []numConfigs = new int[1];
		EGLConfig []configs = new EGLConfig[1]; 
		if (!mEgl.eglChooseConfig(mEglDisplay, getConfig(), configs, 1, numConfigs)) {
			throw new RuntimeException("eglChooseConfig failed : " + GLUtils.getEGLErrorString(mEgl.eglGetError()));
		}
		
		int []contextAttribs = {
				EGL_CONTEXT_CLIENT_VERSION, 2,
				EGL10.EGL_NONE
		};
		mEglContext = mEgl.eglCreateContext(mEglDisplay, configs[0], EGL10.EGL_NO_CONTEXT, contextAttribs);
		mEglSurface = mEgl.eglCreateWindowSurface(mEglDisplay, configs[0], mSurfaceTexture, null);
		if (mEglSurface == EGL10.EGL_NO_SURFACE || mEglContext == EGL10.EGL_NO_CONTEXT) 
		{
			int error = mEgl.eglGetError();
			if (error == EGL10.EGL_BAD_NATIVE_WINDOW) 
			{
				throw new RuntimeException("eglCreateWindowSurface returned  EGL_BAD_NATIVE_WINDOW. " );
			}
			throw new RuntimeException("eglCreateWindowSurface failed : " + GLUtils.getEGLErrorString(mEgl.eglGetError()));
		}
		
		if (!mEgl.eglMakeCurrent(mEglDisplay, mEglSurface, mEglSurface, mEglContext)) 
		{
			throw new RuntimeException("eglMakeCurrent failed : " + GLUtils.getEGLErrorString(mEgl.eglGetError()));
		}
		
		mGL = mEglContext.getGL();
	}
	
	public void  quitThread()
	{
		synchronized (mGL)
		{
			mQuitThread = true;
		}
	}
	
	private void destoryGL() 
	{
		if(mEglContext != null && mEglContext != EGL10.EGL_NO_CONTEXT)
		{
			mEgl.eglDestroyContext(mEglDisplay, mEglContext);
			mEglContext = EGL10.EGL_NO_CONTEXT;
		}
			
		if(mEglSurface != null && mEglSurface != EGL10.EGL_NO_SURFACE)
		{
			mEgl.eglDestroySurface(mEglDisplay, mEglSurface);
			mEglSurface = EGL10.EGL_NO_SURFACE;
		}
		
		if(mRenderer != null)
		{
			mRenderer.setEnableDraw(false);
			mRenderer.Destroy();
			mRenderer = null;
		}
	}
	
	public void setViewImage(Bitmap bitmap, int positionX, int positionY, int rectWidth, int rectHeight)
	{
		mRenderer.setViewImage(bitmap, positionX, positionY, rectWidth, rectHeight);
	}
	
	public void setViewImage(Bitmap bitmap, ImageView view)
	{
		android.graphics.Matrix matrix = view.getImageMatrix();
		Rect rect = view.getDrawable().getBounds();
		float[] value = new float[9];
		matrix.getValues(value);
		
		float left = value[2];
		float top = value[5];
		float right = left + rect.width() * value[0];
		float bottom = top + rect.height() * value[0];
		
		int[] location = new  int[2];
		view.getLocationInWindow(location);
		
		int posX = location[0] + (int)left;
		int posY = (int)top;
		
		ViewParent parentView = view.getParent();
		View v = (View)parentView;
		posY = (int)(v.getHeight() - bottom); 
		
		
		mRenderer.setViewImage(bitmap, (int)posX, (int)posY, (int)(right - left), (int)(bottom - top));
	}
	
	public void enableGaussionBlurView(boolean display)
	{
		mRenderer.setEnableDraw(display);
	}
	
	public long getGaussTime()
	{
		return mRenderer.getGaussTime();
	}
	
	public void setBlurRadius(int blurRadiusSize)
	{
		mRenderer.setBlurRadius(blurRadiusSize);
	}
	
	public void setLuminance(float lu)
	{
		mRenderer.setLuminance(lu);
	}
	
	public void setBlurTrucent(float blurAlpha)
	{
		mRenderer.setBlurTrucent(blurAlpha);
	}
	
	public void setModel(int usemodel)
	{
		mRenderer.setModel(usemodel);
	}
	
	public void setCureentViewHeight(int hieght)
	{
		mRenderer.setCureentViewHeight(hieght);
	}
	
	public void getGaussBlurBitmap(Bitmap bitmap, Handler handler, boolean compressbitmap)
	{
		mRenderer.startCalc(handler);
		mRenderer.setViewImage(bitmap, 0, 0, 256, 256);
		mRenderer.setGetBlurStatus();
		mRenderer.setGaussBlurRetBitmap(compressbitmap);
		enableGaussionBlurView(true);
	}
	
	public  Bitmap getGaussBlurBitmap(Bitmap bitmap, boolean compressbitmap)
	{		
		Bitmap retBitmap = null;
		int width = bitmap.getWidth();
		int height = bitmap.getHeight();
		
		mRenderer.setViewImage(bitmap, 0, 0, 256, 256);
		mRenderer.setGetBlurStatus();
		mRenderer.setGaussBlurRetBitmap(compressbitmap);
		enableGaussionBlurView(true);
		
		while(true)
		{
			retBitmap = mRenderer.getGaussBlurBitmap();
			
			if(retBitmap != null)
			{
				break;
			}
		}
		
		retBitmap = Bitmap.createScaledBitmap(retBitmap, width, height, false);
		return retBitmap;
	}
	
	public void setViewport(int width, int height)
	{
		mRenderer.setViewport(width, height);
	}
	
	public void resize(int width, int height)
	{
		mRenderer.resize(width, height);
	}
	
	public void run() 
	{
		initGL();
		
		if (mRenderer != null)
		{
			((GLRendererImpl)mRenderer).initRenderImpl();
		}
		
		while (mShouldRender != null && mShouldRender.get() != false) 
		{
//			synchronized (mGL)
			{
				if(mQuitThread)
				{
					if(mRenderer != null)
					{
						mRenderer.setEnableDraw(false);
						mRenderer.Destroy();
						mRenderer = null;
					}
						
					break;
				}
				else
				{
					if (mRenderer != null)
					{
						mRenderer.drawFrame();
						mEgl.eglSwapBuffers(mEglDisplay, mEglSurface);
							
//						try
//						{
//							sleep(5);
//						} 
//						catch(InterruptedException e)
//						{
//								
//						}
					}
				}
			}
		}
		
		destoryGL();
	}
	
	public void destroy()
	{
		destoryGL();
	}
}
