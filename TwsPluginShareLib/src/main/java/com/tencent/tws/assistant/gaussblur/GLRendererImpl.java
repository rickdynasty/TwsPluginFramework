package com.tencent.tws.assistant.gaussblur;

import java.nio.IntBuffer;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class GLRendererImpl{
	
	private PlaneModel plane = new PlaneModel();
	private Context ct;

	private float[] mProjMatrix = new float[16];
	private float[] mViewMatrix = new float[16];
	private float[] mMVPMatrix  = new float[16];
	
	private static final float modelW = 100.0f, modelH = 100.0f;
	
	private int[] textureId = new int[1];
	private int mRectWidth = (int)modelW, mRectHeight = (int)modelH;
	private int PosX = 0, PosY = 0;
	private Bitmap bitmap = null;
	private boolean draw = false;
	private boolean initializeed = false;
	private long gaussTime = 0;
	private boolean getGaussBlurBitmap = false;
	private long getGaussBlurBitmapstart = 0;
	private Bitmap retGaussBlurBitmap = null;
	private int[] Orignalframebuffers = new int[1];
	private int[] framebuffers = new int[1];
	private int[] depthbuffers = new int[1];
	private int[] FBOtextureId = new int[1];
	private Handler handler = null;
	public static final int CALC_FINISHED = 1;
	private boolean subTexture = false;
	private int fboTextureW = 256;
	private int fboTextureH = 256;
	
	private int[] gaussFboFrameBuffers = new int[2];
	private int[] gaussFboRenderBuffers = new int[2];
	private int[] gaussFboTexttures = new int[2];
	private int fboTextureWidth = 0;
	private int fboTextureHeight = 0;
	private int drawTime = 0;
	private int BlurRadius = 10;
	private float[] mFboMVPMatrix  = new float[16];
	
	private float mBlurTrucent = 1.0f; 
	private float mLuminance = 1.0f;
	
	private float mMintextureV = 0.0f;
	private float mGLSurfaceViewHeight = modelH;
	
	private boolean mReloeadTexture = true;
	
	public final int GENERALMODEL = 0;
	public final int DRAGMODEL = 1;
	
	private int model = 0;
	
	private boolean mCompressedResult = false;

	private int mWidth;
	private int mHeight;
	
	public GLRendererImpl(Context context) 
	{
		ct = context;
		framebuffers[0] = 0;
		depthbuffers[0] = 0;
		model = GENERALMODEL;
	}
	
	public GLRendererImpl(Context context, Bitmap bitmapImg) 
	{
		ct = context;
		framebuffers[0] = 0;
		depthbuffers[0] = 0;
		model = GENERALMODEL;
		
		bitmap = bitmapImg;
	}
	
	public void setViewImage(Bitmap bitmapImg, int positionX, int positionY, int rectWidth, int rectHeight)
	{
		synchronized(framebuffers)
		{
			if(bitmapImg != null)
			{
				if(bitmap == null)
					subTexture = false;
				else
				{
					int resFormat = bitmap.getConfig() == Config.ARGB_8888 ? GLES20.GL_RGBA : GLES20.GL_RGB;
					int nowFormat = bitmapImg.getConfig() == Config.ARGB_8888 ? GLES20.GL_RGBA : GLES20.GL_RGB;
					int resWidth = bitmap.getWidth(), resHeight = bitmap.getHeight();
					int nowWidth = bitmapImg.getWidth(), nowHeight = bitmapImg.getHeight();
					
					if(resFormat == nowFormat && resWidth == nowWidth && resHeight == nowHeight)
						subTexture = true;
					else 
						subTexture = false;
				}
				
				bitmap = bitmapImg;
				
				PosX = positionX;
				PosY = positionY;
				mRectWidth = rectWidth;
				mRectHeight = rectHeight;
				
				mReloeadTexture = true;
				initializeed = false;
			}
			else
			{
				if(bitmap != null)
				{
					PosX = positionX;
					PosY = positionY;
					mRectWidth = rectWidth;
					mRectHeight = rectHeight;
					
					mReloeadTexture = false;
					subTexture = false;
					initializeed = false;
				}
			}
		}
	}
	
	public void setBlurRadius(int blurRadiusSize)
	{
		synchronized(framebuffers)
		{
			BlurRadius = blurRadiusSize;
			if(BlurRadius < 0)
				BlurRadius = 1;
		}
	}
	
	public void setLuminance(float lu)
	{
		synchronized(framebuffers)
		{
			mLuminance = 1.0f - lu;
		}
	}
	
	public void setBlurTrucent(float bluralpha)
	{
		synchronized(framebuffers)
		{
			mBlurTrucent = bluralpha;
			if(mBlurTrucent < 0.0f)
				mBlurTrucent = 0.0f;
			
			if(mBlurTrucent > 1.0f)
				mBlurTrucent = 1.0f;
		}
	}
	
	public void setModel(int usemodel)
	{
		model = usemodel;
	}
	
	public void setCureentViewHeight(int height)
	{
		synchronized(framebuffers)
		{
			if(drawTime != 0) // mean gaussian blur had done
				return;
			
			if(model == DRAGMODEL)
				draw = true;
						
			mMintextureV = 1.0f - (float)(height) / mGLSurfaceViewHeight;
			
			Matrix.setIdentityM(mViewMatrix, 0);
			Matrix.translateM(mViewMatrix, 0, 0.0f, (mGLSurfaceViewHeight - height)/2.0f, -5.0f);
			Matrix.scaleM(mViewMatrix, 0, (float)(mRectWidth/modelW), (float)(height/modelH), 1.0f);
			Matrix.setIdentityM(mMVPMatrix, 0);
			Matrix.multiplyMM(mMVPMatrix, 0, mProjMatrix, 0, mViewMatrix, 0);

		}
	}
	
	private void reset()
	{
		drawTime = 1;
		
		if(bitmap != null)
		{
			int tW = bitmap.getWidth();
			int tH = bitmap.getHeight();
			
			int mimValue = tW > tH ? tH : tW;
			if(BlurRadius > mimValue)
				BlurRadius = mimValue;
			
			plane.setTxtureWidth(tW);
			plane.setTxtureHeight(tH);
		}
		
		plane.resetting();
		plane.setBlurRadiusValue(BlurRadius);
		plane.setBlurLuminance(mLuminance);
	}
	
	private void init()
	{
		synchronized(framebuffers)
		{
			if(initializeed)
				return;
			
			Matrix.setIdentityM(mProjMatrix, 0);
			Matrix.orthoM(mProjMatrix, 0, -(float)mRectWidth/2, (float)mRectWidth/2, -(float)mRectHeight/2, (float)mRectHeight/2, 1.0f, 100.0f);
			
			Matrix.setIdentityM(mViewMatrix, 0);
			Matrix.translateM(mViewMatrix, 0, 0.0f, 0.0f, -5.0f);
			Matrix.scaleM(mViewMatrix, 0, (float)(mRectWidth/modelW), (float)(mRectHeight/modelH), 1.0f);
			Matrix.setIdentityM(mMVPMatrix, 0);
			Matrix.multiplyMM(mMVPMatrix, 0, mProjMatrix, 0, mViewMatrix, 0);
			
			if(bitmap != null && mReloeadTexture)
			{
				if (textureId[0] != 0 && subTexture) 
				{		
					GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId[0]);
					int bitmapFormat = bitmap.getConfig() == Config.ARGB_8888 ? GLES20.GL_RGBA : GLES20.GL_RGB;
					GLUtils.texSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, bitmap, bitmapFormat, GLES20.GL_UNSIGNED_BYTE);
				}
				else
				{
					if(textureId[0] != 0)
						GLES20.glDeleteTextures(1, textureId, 0);
					
					GLES20.glGenTextures(1, textureId, 0);

					GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId[0]);
					GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
					GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
					GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
					GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
					GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);	
				}
				
				createGaussFbo(bitmap);
			}
			
			reset();
			
			createFBO();
			
			initializeed = true;
		}		
	}
	
	public void setEnableDraw(boolean render)
	{
		synchronized(framebuffers)
		{
			draw = render;
		}
	}
	
	public long getGaussTime()
	{
		synchronized(framebuffers)
		{
			return gaussTime;
		}
	}
	
	private void doSaveGaussBlur(int w, int h)
	{ 
	    int b[]=new int[w*h];
	    int bt[]=new int[w*h];
	    IntBuffer ib = IntBuffer.wrap(b);
	    ib.position(0);
	    GLES20.glReadPixels(0, 0, w, h, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, ib);

	    for(int i=0, k=0; i<h; i++, k++)
	    { //remember, that OpenGL bitmap is incompatible with Android bitmap
	      //and so, some correction need.        
	       for(int j=0; j<w; j++)
	       {
	          int pix=b[i*w+j];
	          int pb=(pix>>16)&0xff;
	          int pr=(pix<<16)&0x00ff0000;
	          int pix1=(pix&0xff00ff00) | pr | pb;
	          bt[(h-k-1)*w+j]=pix1;
	       }
	    }

	    if(retGaussBlurBitmap != null)
	    	retGaussBlurBitmap.recycle();
	    
	    retGaussBlurBitmap = Bitmap.createBitmap(bt, w, h, Bitmap.Config.ARGB_8888);
	 }
	
	public void setGaussBlurRetBitmap(boolean compressbitmap)
	{
		synchronized(framebuffers)
		{
			mCompressedResult = compressbitmap;
		}	
	}
	
	public Bitmap getGaussBlurBitmap()
	{
		synchronized(framebuffers)
		{
			return retGaussBlurBitmap;
		}	
	}
	
	public void setGetBlurStatus()
	{
		synchronized(framebuffers)
		{
			retGaussBlurBitmap = null;
			getGaussBlurBitmap = true;
		}
	}
	
	public void startCalc(Handler handler)
	{
		synchronized(framebuffers)
		{
			this.handler = handler;
		}
    }
	
	private void createFBO()
	{
		if(!getGaussBlurBitmap)
			return;
		
		if (!subTexture && framebuffers[0] != 0 && depthbuffers[0] != 0) 
		{
			GLES20.glDeleteTextures(1, FBOtextureId, 0);
			GLES20.glDeleteBuffers(1, framebuffers, 0);
			GLES20.glDeleteBuffers(1, depthbuffers, 0);
				
			FBOtextureId[0] = 0;
			framebuffers[0] = 0;
			depthbuffers[0] = 0;
		}
			
		if(framebuffers[0] == 0 && depthbuffers[0] == 0)
		{
			subTexture = true;
			
			if(mCompressedResult)
			{
				int minValue = 	bitmap.getWidth()  > bitmap.getHeight() ? bitmap.getHeight() : bitmap.getWidth();
				fboTextureW = 1;
				
				while(fboTextureW < minValue)
					fboTextureW = fboTextureW << 1;
				
				float radio = (float)(fboTextureW) / minValue;
				int scale = (int) Math.rint(radio);
				
				fboTextureW = fboTextureW >> scale;
				
				fboTextureH = fboTextureW;
			}
			else
			{
				fboTextureW = fboTextureWidth;
				fboTextureH = fboTextureHeight;
			}
			
			mRectWidth = mRectHeight = fboTextureW;
			
			int Format = bitmap.getConfig() == Config.ARGB_8888 ? GLES20.GL_RGBA : GLES20.GL_RGB;
			
			GLES20.glGenTextures(1, FBOtextureId, 0);
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, FBOtextureId[0]);
			GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, Format, fboTextureW, fboTextureH, 0, Format, GLES20.GL_UNSIGNED_BYTE, null);
			
			GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
			GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
			GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
			GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
						
			GLES20.glGenRenderbuffers(1,  depthbuffers, 0);
			GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, depthbuffers[0]);
			GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_COMPONENT16, fboTextureW, fboTextureH);
			
			GLES20.glGenFramebuffers(1, framebuffers, 0);
			GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, framebuffers[0]);
			
			// Attach the texture that the frame buffer will render to
			GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, FBOtextureId[0], 0);
		
			GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT, GLES20.GL_RENDERBUFFER, depthbuffers[0]);

			if(GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER) != GLES20.GL_FRAMEBUFFER_COMPLETE)
			{
//				Log.v("OpenGLRenderer", "ERROR: Frame buffer did not set up correctly\n");

				GLES20.glDeleteTextures(1, FBOtextureId, 0);
				GLES20.glDeleteBuffers(1, framebuffers, 0);
				GLES20.glDeleteBuffers(1, depthbuffers, 0);
				
				framebuffers[0] = 0;
				depthbuffers[0] = 0;
				getGaussBlurBitmap = false;
			}
			
			GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, Orignalframebuffers[0]);
			GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, 0);
		}	
	}
	
	private void createGaussFbo(Bitmap setbitmap)
	{	
		// Create middle render fbo result 
		if((gaussFboTexttures[0] != 0 && gaussFboTexttures[1] != 0) ||
			(gaussFboRenderBuffers[0] != 0 && gaussFboRenderBuffers[1] != 0) ||
			(gaussFboFrameBuffers[0] != 0 && gaussFboFrameBuffers[1] != 0))
		{
			if(setbitmap != null && (fboTextureWidth == setbitmap.getWidth() && fboTextureHeight == setbitmap.getHeight()))
				return;
			
			GLES20.glDeleteTextures(2, gaussFboTexttures, 0);
			GLES20.glDeleteRenderbuffers(2, gaussFboRenderBuffers, 0);
			GLES20.glDeleteFramebuffers(2, gaussFboFrameBuffers, 0);
		}
			
		fboTextureWidth = bitmap.getWidth();
		fboTextureHeight = bitmap.getHeight();
		
		int Format = bitmap.getConfig() == Config.ARGB_8888 ? GLES20.GL_RGBA : GLES20.GL_RGB;
		
		GLES20.glGenTextures(2, gaussFboTexttures, 0);
		for(int i = 0; i < 2; ++i)
		{
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, gaussFboTexttures[i]);
			GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, Format, fboTextureWidth, fboTextureHeight, 0, Format, GLES20.GL_UNSIGNED_BYTE, null);
			GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
			GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
			GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
			GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
		}
			
		GLES20.glGenRenderbuffers(2, gaussFboRenderBuffers, 0);
		for(int j = 0; j < 2; ++j)
		{
			GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, gaussFboRenderBuffers[j]);
			GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_COMPONENT16, fboTextureWidth, fboTextureHeight);
		}
			
		GLES20.glGenFramebuffers(2, gaussFboFrameBuffers, 0);
		for(int k = 0; k < 2; ++k)
		{
			GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, gaussFboFrameBuffers[k]);
			GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, gaussFboTexttures[k], 0);
			GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT, GLES20.GL_RENDERBUFFER, gaussFboRenderBuffers[k]);
		}
	
		if(GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER) != GLES20.GL_FRAMEBUFFER_COMPLETE)
		{
			GLES20.glDeleteTextures(2, gaussFboTexttures, 0);
			GLES20.glDeleteRenderbuffers(2, gaussFboRenderBuffers, 0);
			GLES20.glDeleteFramebuffers(2, gaussFboFrameBuffers, 0);
		}

		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
		GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, 0);
		
		Matrix.setIdentityM(mProjMatrix, 0);
		Matrix.orthoM(mProjMatrix, 0, -(float)fboTextureWidth/2, (float)fboTextureWidth/2, -(float)fboTextureHeight/2, (float)fboTextureHeight/2, 1.0f, 100.0f);
		
		Matrix.setIdentityM(mViewMatrix, 0);
		Matrix.translateM(mViewMatrix, 0, 0.0f, 0.0f, -5.0f);
		Matrix.scaleM(mViewMatrix, 0, (float)(fboTextureWidth/modelW), (float)(fboTextureHeight/modelH), 1.0f);
		Matrix.setIdentityM(mFboMVPMatrix, 0);
		Matrix.multiplyMM(mFboMVPMatrix, 0, mProjMatrix, 0, mViewMatrix, 0);
	}
	
	public void setViewport(int width, int height) 
	{
		mWidth = width;
		mHeight = height;
	}
	
	public void initRenderImpl()
	{
		Matrix.setIdentityM(mViewMatrix, 0);
		Matrix.orthoM(mProjMatrix, 0, -(float)mWidth/2, (float)mWidth/2, -(float)mHeight/2, (float)mHeight/2, 1.0f, 100.0f);
		
		Matrix.setIdentityM(mViewMatrix, 0);
		Matrix.translateM(mViewMatrix, 0, 0.0f, 0.0f, -5.0f);
		Matrix.multiplyMM(mMVPMatrix, 0, mProjMatrix, 0, mViewMatrix, 0);
		
		textureId[0] = 0;
		plane.Init(modelW, modelH);
		plane.LoadShader(ct);

		gaussFboTexttures[0] = 0;
		gaussFboFrameBuffers[0] = 0;
		gaussFboRenderBuffers[0] = 0;
			
		gaussFboTexttures[0] = gaussFboTexttures[1] = 0;
		gaussFboRenderBuffers[0] = gaussFboRenderBuffers[1] = 0;
		gaussFboFrameBuffers[0] = gaussFboFrameBuffers[1] = 0;
		
		GLES20.glGetIntegerv(GLES20.GL_FRAMEBUFFER_BINDING, Orignalframebuffers, 0);
			
		PosX = 0;
		PosY = 0;
		mRectWidth = mWidth;
		mRectHeight = mHeight;
		mGLSurfaceViewHeight = mRectHeight;
		init();
	}
	
	public void resize(int width, int height) 
	{
		mWidth = width;
		mHeight = height;
	}

	public void drawFrame() {
		// TODO Auto-generated method stub
		if(draw)
		{	
			init();
			
			long start = System.currentTimeMillis();	
			
			if(getGaussBlurBitmap && drawTime == 1)
			{
				getGaussBlurBitmapstart = start;
			}
			
			createFBO();
			
			if(drawTime == 1 && gaussFboFrameBuffers[0] != 0)
			{
				GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, gaussFboFrameBuffers[0]);
				GLES20.glViewport(0, 0, fboTextureWidth, fboTextureHeight);
			}
			else if(drawTime == 2 && gaussFboFrameBuffers[1] != 0)
			{
				GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, gaussFboFrameBuffers[1]);
				GLES20.glViewport(0, 0, fboTextureWidth, fboTextureHeight);
			}
			else
			{
				if(getGaussBlurBitmap && framebuffers[0] != 0)
				{	
					GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, framebuffers[0]);
					GLES20.glViewport(0, 0, fboTextureW, fboTextureH);
				}
				else
				{
					GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, Orignalframebuffers[0]);
					GLES20.glViewport(PosX, PosY, mRectWidth, mRectHeight);	
				}
			}

			GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
	    	GLES20.glClearColor(1.0f,  1.0f,  1.0f,  0.0f); 	
	    	
	    	boolean hadTexture = false;
	    	
	    	if(textureId[0] != 0 || gaussFboTexttures[0] != 0 || gaussFboTexttures[1] != 0)
	    	{
	    		GLES20.glEnable(GLES20.GL_TEXTURE_2D);
	    		
	    		if(drawTime == 1)
		    		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId[0]);
		    	else if(drawTime == 2 && gaussFboTexttures[0] != 0)
		    		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, gaussFboTexttures[0]);
		    	else
		    		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, gaussFboTexttures[1]);
	    		
	    		hadTexture = true;
	    	}
	    	
	    	plane.setMinTextureHeight(mMintextureV);
	    	plane.seRenderTimeValue(drawTime);
	    	plane.setBlurAlpha(mBlurTrucent);
	    
		    if(drawTime == 1 || drawTime == 2)
		    	plane.drawPlaneModel(mFboMVPMatrix, hadTexture);
		    else
		    	plane.drawPlaneModel(mMVPMatrix, hadTexture);
			
			if(getGaussBlurBitmap && drawTime == 0)
			{
				synchronized(framebuffers)
				{
					doSaveGaussBlur(fboTextureW, fboTextureH);
				}
					
				if(retGaussBlurBitmap != null)
				{
					gaussTime = System.currentTimeMillis() - getGaussBlurBitmapstart;
						
					getGaussBlurBitmap = false;
					draw = false;
					GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, Orignalframebuffers[0]);
				}
						
				if(handler!=null)
				{
			        // do calculation using GL handle
					Bundle b = new Bundle();
					b.putLong("time", gaussTime);
			        int flag = GLRendererImpl.CALC_FINISHED;
			        Message msg =  handler.obtainMessage();
			        msg.what = flag;
			        msg.setData(b);
			        msg.obj = retGaussBlurBitmap;
			        handler.sendMessage(msg);
			        // adds a message to the UI thread's message queue
			        handler = null;
			    }
			}
			
			gaussTime = System.currentTimeMillis() - start;
			if(draw && (drawTime == 1 || drawTime == 2))
//			Log.v("bsw", "drawTime ========= " + drawTime + "  time ========== " + gaussTime);
			
			if(drawTime == 1)
				drawTime = 2;
			else if(drawTime == 2)
			{
				drawTime = 0;
				if(model == DRAGMODEL)
					draw = false;
			}
			else
				drawTime = 0;
		}	
	}
	
    public void Destroy()
    {
    	plane.destroy();
    
    	mReloeadTexture = false;
    	
    	if(textureId[0] != 0)
    		GLES20.glDeleteTextures(1, textureId, 0);
    	
    	if(FBOtextureId[0] != 0)
    		GLES20.glDeleteTextures(1, FBOtextureId, 0);
    	if(framebuffers[0] != 0)
    		GLES20.glDeleteBuffers(1, framebuffers, 0);
    	if(depthbuffers[0] != 0)
    		GLES20.glDeleteBuffers(1, depthbuffers, 0);
    	
		GLES20.glDeleteTextures(2, gaussFboTexttures, 0);
		GLES20.glDeleteRenderbuffers(2, gaussFboRenderBuffers, 0);
		GLES20.glDeleteFramebuffers(2, gaussFboFrameBuffers, 0);
    }

}
