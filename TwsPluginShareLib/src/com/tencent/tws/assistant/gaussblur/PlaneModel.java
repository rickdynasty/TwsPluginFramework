package com.tencent.tws.assistant.gaussblur;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;

public class PlaneModel {
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Shader
	private static final String mvshader = 
	"precision mediump float;\n" +
	"attribute vec3 inVertex;\n" +					
	"attribute vec3	inNormal;\n" +				
	"attribute float inAlpha;\n" +			
	"attribute vec2 inTexCoord;\n" +			
	"uniform  mat4  MVPMatrix;\n" +
	"uniform bool odd;\n" +
	"uniform float minTextureHeight;\n" +
	"uniform bool gaussianDone;\n" +
	"varying vec2 TexCoord;\n" +
	"varying float alpha;\n" +
	
					
	"void main()\n" +
	"{\n" +							
	"		gl_Position = MVPMatrix * vec4(inVertex, 1.0);\n" +
			
	"		if(odd)\n" +
	"			TexCoord = inTexCoord;\n" +
	"		else\n" +
	"		{\n" +
	"			TexCoord.x = inTexCoord.x;\n" +
	"			TexCoord.y = 1.0 - inTexCoord.y;\n" +
	"		}\n" +
	
	" 		if(gaussianDone)\n" + 
	"		{\n" +
	" 			if(TexCoord.y < minTextureHeight)\n" +
	"				TexCoord.y = minTextureHeight;\n" +
	"		}\n" +

	"		alpha = inAlpha;\n" +
	"}\n";
	
	private static final String mfshader =
	"precision mediump float;\n" +
	"uniform sampler2D  sTexture;\n" +
	"uniform bool DrawColor;\n" +
	"uniform int renderTime;\n" +
	"uniform int nRadius;\n" +
	"uniform float textureW;\n" +
	"uniform float textureH;\n" +
	"uniform float blurAlpha;\n" +
	"uniform float luminance;\n" +
	"varying vec2 TexCoord;\n" +				
	"varying float alpha;\n" +
						
	"const float PI = 3.14159265359;\n" +

	"float Gaussian(float x, float variance)\n" +
	"{\n" +
	"  	 return (1.0 / sqrt(2.0 * PI * variance)) * exp(-((x * x) / (2.0 * variance)));\n" +
	"}\n" +
			
	"void main()\n" +					
	"{\n" +
	"	if(DrawColor)\n" +
	"	{\n" +
	"		gl_FragColor = vec4(0.4, 0.8, 0.8, alpha);\n" +
	"	}\n" +
	"	else\n" +
	"	{\n" +
	"		if(renderTime != 0)\n" +
	"		{\n" +
	"			vec4 midColors = vec4(0.0);\n" +
			
	"			float halfBlurRadius = float(nRadius) / 2.0;\n" +
	"			float std = halfBlurRadius * 0.35;\n" +
	"			std *= std;\n" +

	"			for(int i = 0; i < nRadius; ++i)\n" +
	"			{\n" +
	"				float disCenter = float(i) - halfBlurRadius;\n" +

	"				if(renderTime == 1) // horizontal\n" +
	"				{\n" +
	"					vec2 midTexCoord = TexCoord + vec2(textureW * disCenter, 0.0);\n" +
	"					midColors += texture2D(sTexture, midTexCoord) * Gaussian(disCenter * luminance, std);\n" +
	"				}\n" +
	"				else if(renderTime == 2) // vertial\n" +
	"				{\n" +
	"					vec2 midTexCoord = TexCoord + vec2(0.0, textureH * disCenter);\n" +
	"					midColors += texture2D(sTexture, midTexCoord) * Gaussian(disCenter * luminance, std);\n" +
	"				}\n" +
	"			}\n" +

	"			midColors = clamp(midColors, 0.0, 1.0);\n" +
	" 			if(renderTime == 2)\n" +	
	"				midColors[3] *= blurAlpha;\n" +
	" 			else \n" +
	"				midColors[3] *= 1.0;\n" +
	"			gl_FragColor = midColors;\n" +
	"		}\n" +
	"		else\n" +
	"		{\n" +
	"			mediump vec4 midColors;\n" +
	"			midColors = texture2D(sTexture, TexCoord);\n" +
	"			//midColors[3] *= 1.0;\n" +
	"			gl_FragColor = midColors;\n" +
	"		}\n" +
	"	}\n" +
	"}\n";
	
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// define  vertex3, every 3 float
	class Vec3
	{
		private float[] value = new float[3]; 
		
		public Vec3()
		{
			// TODO Auto-generated constructor stub
			value[0] = value[1] = value[2] = 0.0f;
		} 
		
		public Vec3(float x, float y, float z)
		{
			value[0] = x;
			value[1] = y;
			value[2] = z;
		}
		
		public float[] getValue()
		{
			return value;
		}
	}
	
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////	
	// define  vertex, every vertex contain 1 position, 1 normal, 1 alpha of vertex and texture coordinate
	class Vertex
	{
		public Vec3 position;
		private Vec3 normal;
		private float alpha = 0.0f;
		private float[] txtCoord = new float[2];
		
		public Vertex()
		{
			
		}
		
		public Vec3 getPosition()
		{
			return position;
		}
		
		public Vec3 getNormal()
		{
			return normal;
		}
		
		public float getAlpha()
		{
			return alpha;
		}
		
		public float[] getTxtCoord()
		{
			
			return txtCoord;
		}
	}
	
	public static int getVertexSize()
	{
		return ((3  + 3  + 1 + 2) * 4);
	}
	
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Plane's data and functions
	
	private Vertex[] m_Vertex = new Vertex[16];
	private short[] m_Indexs = new short[6];
	private short[] m_borderIndes = new short[48];
	private int NumCenter = 0;
	private int NumBorder = 0;
	
	private int[] mVertexbuffer = new int[1];
	
	private int[] mbuffers = new int[2];
	
	private FloatBuffer mPlaneVertexBuffer;
	private ShortBuffer mCenterBufferIndex;
	private ShortBuffer mBorderBufferIndex;
	
	private int newProgramId = 0;
	private int attributeVertexHandler = 0;
	private int attributeNormalHandler = 0;
	private int attributeAlphaHandler = 0;
	private int attributeTextureHandler = 0;
	private int UniformMVPMatrixHandler = 0;
	private int UniformOddHandler = 0;
	private int UniformMinTextureHeightHandler = 0;
	private int UniformGaussianDoneHandler = 0;
	private int UniformDrawColorHandler = 0;
	private int UniformRenderTimeHandler = 0;
	private int UniformRadiusHandler = 0;
	private int UniformTxtureWHandler = 0;
	private int UniformTxtureHHandler = 0;
	private int UniformBlurAlphaHandler = 0;
	private int UniformLuminanceHandler = 0;
	
	private float mMinTexHeight = 0.0f;
	private int mDrawTime = 0;
	private int mBlurRadius = 10;
	private float mTxtWidth = 1.0f;
	private float mTxtHeight = 1.0f;
	private float mblurAlpha = 1.0f;
	private float mLuminance = 1.0f;
	private boolean mResetting = false;
	
	private boolean initIndexVbo = false;
	private boolean loadShadered = false;
	

	void Init(float width, float height)
	{
		for(int i = 0; i < 16; ++i)
			m_Vertex[i] = new Vertex();
		
		float dim = 0.5f;
		float dimLess = dim * 1.0f;
		Vec3 normal = new Vec3(0.0f, 1.0f, 0.0f);

		m_Vertex[0].position	=	new Vec3(-dim, dim, 0.0f);
		m_Vertex[1].position	=	new Vec3(-dimLess, dim, 0.0f); 
		m_Vertex[2].position	=	new Vec3(dimLess, dim, 0.0f);
		m_Vertex[3].position	=	new Vec3(dim, dim, 0.0f);
		m_Vertex[4].position	=	new Vec3(-dim, dimLess, 0.0f);
		m_Vertex[5].position	=	new Vec3(-dimLess, dimLess, 0.0f); 
		m_Vertex[6].position	=	new Vec3(dimLess, dimLess, 0.0f);
		m_Vertex[7].position	=	new Vec3(dim, -dimLess, 0.0f);
		m_Vertex[8].position	=	new Vec3(-dim, -dimLess, 0.0f);
		m_Vertex[9].position	=	new Vec3(-dimLess, -dimLess, 0.0f); 
		m_Vertex[10].position	=	new Vec3(dimLess, -dimLess, 0.0f);
		m_Vertex[11].position	=	new Vec3(dim, -dimLess, 0.0f);
		m_Vertex[12].position	=	new Vec3(-dim, -dim, 0.0f);
		m_Vertex[13].position	=	new Vec3(-dimLess, -dim, 0.0f); 
		m_Vertex[14].position	=	new Vec3(dimLess, -dim, 0.0f);
		m_Vertex[15].position	=	new Vec3(dim, -dim, 0.0f);

		for (int i = 0; i < 16; ++i)
		{
			m_Vertex[i].normal = normal;
			m_Vertex[i].alpha = 0.0f;

			m_Vertex[i].txtCoord[0] = m_Vertex[i].position.value[0] + dim;
			m_Vertex[i].txtCoord[1] = m_Vertex[i].position.value[1] + dim;

			m_Vertex[i].position.value[0] *= width;
			m_Vertex[i].position.value[1] *= height;
		}

		m_Vertex[5].alpha = 1.0f;
		m_Vertex[6].alpha = 1.0f;
		m_Vertex[9].alpha = 1.0f;
		m_Vertex[10].alpha = 1.0f;

		if(!initIndexVbo)
		{
			for(int row = 0; row < 3; ++row)
			{
				for(int col = 0; col < 3; ++col)
				{
					int start = (row*4)+col;

					if(row==1 && col == 1)
					{
						m_Indexs[NumCenter++] = (short) (start+1);
						m_Indexs[NumCenter++] = (short) start;
						m_Indexs[NumCenter++] = (short) (start+4);
						m_Indexs[NumCenter++] = (short) (start+1);
						m_Indexs[NumCenter++] = (short) (start+4);
						m_Indexs[NumCenter++] = (short) (start+5);
					}
					else
					{
						m_borderIndes[NumBorder++] = (short) (start+1);
						m_borderIndes[NumBorder++] = (short) start;
						m_borderIndes[NumBorder++] = (short) (start+4);
						m_borderIndes[NumBorder++] = (short) (start+1);
						m_borderIndes[NumBorder++] = (short) (start+4);
						m_borderIndes[NumBorder++] = (short) (start+5);
					}
				}
			}

			m_borderIndes[0] = 1;
			m_borderIndes[1] = 0;
			m_borderIndes[2] = 5;
			m_borderIndes[3] = 0;
			m_borderIndes[4] = 4;
			m_borderIndes[5] = 5;

			//bottom right
			m_borderIndes[42] = 11;
			m_borderIndes[43] = 10;
			m_borderIndes[44] = 15;
			m_borderIndes[45] = 10;
			m_borderIndes[46] = 14;
			m_borderIndes[47] = 15;
		}		
		
		createVbo();
	}
	
	private void createVbo()
	{ 
         ByteBuffer vertexArray = ByteBuffer.allocateDirect(m_Vertex.length * getVertexSize());
         vertexArray.order(ByteOrder.nativeOrder());
         mPlaneVertexBuffer = vertexArray.asFloatBuffer();
         
         for(int i = 0; i < m_Vertex.length; ++i)
         {
        	 Vertex temp = m_Vertex[i];
        	 
        	 mPlaneVertexBuffer.put(temp.getPosition().getValue()); // put position
        	 mPlaneVertexBuffer.put(temp.getNormal().getValue());   // put normal
        	 mPlaneVertexBuffer.put(temp.getAlpha());				  // put alpha	
        	 mPlaneVertexBuffer.put(temp.getTxtCoord());			  // put texture coordinates	
         }
         
         mPlaneVertexBuffer.position(0);
         
         GLES20.glGenBuffers(1, mVertexbuffer, 0);
         
         GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVertexbuffer[0]);
         GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, mPlaneVertexBuffer.capacity() * 4,  mPlaneVertexBuffer, GLES20.GL_STATIC_DRAW);

         if(!initIndexVbo)
         {    	
            ByteBuffer centerIndex = ByteBuffer.allocateDirect(m_Indexs.length * 2);
            centerIndex.order(ByteOrder.nativeOrder());
            mCenterBufferIndex = centerIndex.asShortBuffer();
            mCenterBufferIndex.put(m_Indexs);
            mCenterBufferIndex.position(0);
            
            ByteBuffer borderIndex = ByteBuffer.allocateDirect(m_borderIndes.length * 2);
            borderIndex.order(ByteOrder.nativeOrder());
            mBorderBufferIndex = borderIndex.asShortBuffer();
            mBorderBufferIndex.put(m_borderIndes);
            mBorderBufferIndex.position(0);
             
        	 GLES20.glGenBuffers(2, mbuffers, 0);
             
             GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, mbuffers[0]);
             GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, mCenterBufferIndex.capacity() * 2,  mCenterBufferIndex, GLES20.GL_STATIC_DRAW);

             GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, mbuffers[1]);
             GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, mBorderBufferIndex.capacity() * 2,  mBorderBufferIndex, GLES20.GL_STATIC_DRAW);
             
             initIndexVbo = true;
         }
         
         GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
         GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER,0);       
	}
	
	private static float[] mProjMatrix = new float[16];
	private static float[] mViewMatrix = new float[16];
	private static float[] mMVPMatrix = new float[16];
	
	
	public void LoadShader(Context mv)
	{
		if(!loadShadered)
		{
//			String newVertexShader = ShaderFactory.loadShaderFromAssets("myvshader.sh", mv.getResources());
//	    	String newFragmentShader = ShaderFactory.loadShaderFromAssets("myfshader.sh", mv.getResources());
//	    	newProgramId = ShaderFactory.createPrograme(newVertexShader, newFragmentShader);
	    	
	    	newProgramId = ShaderFactory.createPrograme(mvshader, mfshader);
	    	
	    	attributeVertexHandler = GLES20.glGetAttribLocation(newProgramId, "inVertex");
	    	attributeNormalHandler = GLES20.glGetAttribLocation(newProgramId, "inNormal");
	    	attributeAlphaHandler = GLES20.glGetAttribLocation(newProgramId, "inAlpha");
	    	attributeTextureHandler = GLES20.glGetAttribLocation(newProgramId, "inTexCoord");
	    	
	    	UniformMVPMatrixHandler = GLES20.glGetUniformLocation(newProgramId, "MVPMatrix");
	    	UniformOddHandler = GLES20.glGetUniformLocation(newProgramId, "odd");
	    	UniformMinTextureHeightHandler = GLES20.glGetUniformLocation(newProgramId, "minTextureHeight");
	    	UniformGaussianDoneHandler = GLES20.glGetUniformLocation(newProgramId, "gaussianDone");
	    	
	    	UniformDrawColorHandler = GLES20.glGetUniformLocation(newProgramId, "DrawColor");
	    	UniformRenderTimeHandler = GLES20.glGetUniformLocation(newProgramId, "renderTime");
	    	UniformRadiusHandler = GLES20.glGetUniformLocation(newProgramId, "nRadius");
	    	UniformTxtureWHandler = GLES20.glGetUniformLocation(newProgramId, "textureW");
	    	UniformTxtureHHandler = GLES20.glGetUniformLocation(newProgramId, "textureH");
	    	UniformBlurAlphaHandler = GLES20.glGetUniformLocation(newProgramId, "blurAlpha");
	    	UniformLuminanceHandler = GLES20.glGetUniformLocation(newProgramId, "luminance");
	    	
	    	
	    	loadShadered = true;
		}	
		
		float radio = (float)540 / 960;
		Matrix.setLookAtM(mViewMatrix, 0, 0, 0, 4, 0, 0, 0, 0, 1, 0);
		Matrix.frustumM(mProjMatrix, 0, -radio, radio, -1, 1, 1.0f, 10.0f);
		
		Matrix.multiplyMM(mMVPMatrix, 0, mProjMatrix, 0, mViewMatrix, 0);
	}
	
	public void resetting()
	{
		mResetting = false;
	}
	
	public void setMinTextureHeight(float minTV)
	{
		mMinTexHeight = minTV;
	}
	
	public void seRenderTimeValue(int time)
	{
		mDrawTime = time;
	}
	
	public void setBlurRadiusValue(int BlurRadius)
	{
		mBlurRadius = BlurRadius;
	}
	
	public void setTxtureWidth(float tw)
	{
		mTxtWidth = 1.0f / tw;
	}
	
	public void setTxtureHeight(float th)
	{
		mTxtHeight = 1.0f / th;
	}
	
	public void setBlurAlpha(float blurAlpha)
	{
		mblurAlpha = blurAlpha;
	}
	
	public void setBlurLuminance(float lu)
	{
		mLuminance = lu;
	}
	
	public void drawPlaneModel(float[] mvpMatrix, boolean hadtexure)
	{
		GLES20.glUseProgram(newProgramId);
		
		if(mResetting)
			GLES20.glUniform1i(UniformOddHandler, 1);
		else
		{
			GLES20.glUniform1i(UniformOddHandler, 0);
			mResetting = true;
		}
		
		if(mDrawTime == 0) // gaussian had done, can be change texture size
			GLES20.glUniform1i(UniformGaussianDoneHandler, 1);
		else
			GLES20.glUniform1i(UniformGaussianDoneHandler, 0);
		
		GLES20.glUniform1f(UniformMinTextureHeightHandler, mMinTexHeight);
		GLES20.glUniform1i(UniformRenderTimeHandler, mDrawTime);
		GLES20.glUniform1i(UniformRadiusHandler, mBlurRadius);
		GLES20.glUniform1f(UniformTxtureWHandler, mTxtWidth);
		GLES20.glUniform1f(UniformTxtureHHandler, mTxtHeight);
		GLES20.glUniform1f(UniformBlurAlphaHandler, mblurAlpha);
		GLES20.glUniform1f(UniformLuminanceHandler, mLuminance);
		
		final int stride = getVertexSize();
		
//		GLES20.glEnable(GLES20.GL_BLEND);
//		GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
		
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVertexbuffer[0]);
		
		GLES20.glUniformMatrix4fv(UniformMVPMatrixHandler, 1, false, mvpMatrix, 0);
		
		if(!hadtexure)
			GLES20.glUniform1i(UniformDrawColorHandler, 1);
		else
			GLES20.glUniform1i(UniformDrawColorHandler, 0);
		
		GLES20.glEnableVertexAttribArray(attributeVertexHandler);
		GLES20.glEnableVertexAttribArray(attributeNormalHandler);
		GLES20.glEnableVertexAttribArray(attributeAlphaHandler);
		GLES20.glEnableVertexAttribArray(attributeTextureHandler);
		
		GLES20.glVertexAttribPointer(attributeVertexHandler,  3, GLES20.GL_FLOAT, false, stride, 0);
		GLES20.glVertexAttribPointer(attributeNormalHandler,  3, GLES20.GL_FLOAT, false, stride, (3 * 4));
		GLES20.glVertexAttribPointer(attributeAlphaHandler,   1, GLES20.GL_FLOAT, false, stride, (6 * 4));
		GLES20.glVertexAttribPointer(attributeTextureHandler, 2, GLES20.GL_FLOAT, false, stride, (7 * 4));
		
		GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, mbuffers[0]);
		GLES20.glDrawElements(GLES20.GL_TRIANGLES, mCenterBufferIndex.capacity(), GLES20.GL_UNSIGNED_SHORT, 0);
		
//		GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, mbuffers[1]);
//		GLES20.glDrawElements(GLES20.GL_TRIANGLES, mBorderBufferIndex.capacity(), GLES20.GL_UNSIGNED_SHORT, 0);
		
		GLES20.glDisableVertexAttribArray(attributeVertexHandler);
		GLES20.glDisableVertexAttribArray(attributeNormalHandler);
		GLES20.glDisableVertexAttribArray(attributeAlphaHandler);
		GLES20.glDisableVertexAttribArray(attributeTextureHandler);
		
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER,0);   
        
        GLES20.glDisable(GLES20.GL_BLEND);
	}
	
	public static float[] getViewMatrix()
	{
		return mViewMatrix;
	}
	
	public static float[] getProjectionMatrix()
	{
		return mProjMatrix;
	}
	
	public void destroy()
	{
		NumCenter = 0;
		NumBorder = 0;
		initIndexVbo = false;
		loadShadered = false;
		GLES20.glDeleteProgram(newProgramId);
		newProgramId = 0;
	}
}
