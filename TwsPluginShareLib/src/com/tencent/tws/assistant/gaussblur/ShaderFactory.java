package com.tencent.tws.assistant.gaussblur;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.content.res.AssetManager;
import android.content.res.Resources;
import android.opengl.GLES20;
import android.util.Log;

public class ShaderFactory {
	
	private static int loadShader(int shadeType, String source)
	{
		int shader = 0;
		
		shader = GLES20.glCreateShader(shadeType);
		if(shader != 0)
		{
			GLES20.glShaderSource(shader, source);
			GLES20.glCompileShader(shader);
			int[] compiled = new int[1];
			GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
			if(compiled[0] == 0)
			{
//				Log.e("GLES20_ERR", "Compile Log:: " + GLES20.glGetShaderInfoLog(shader));
				GLES20.glDeleteShader(shader);
				shader = 0;
			}
		}
		
		return shader;
	}
	
	public static int createPrograme(String vertexSource, String fragmentSource)
	{
		int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
		if(vertexShader == 0)
		{
			return 0;
		}
		int pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
		if(pixelShader == 0)
		{
			return 0;
		}
		
		int program = 0;
		program = GLES20.glCreateProgram();
		
		if(program != 0)
		{
			GLES20.glAttachShader(program, vertexShader);
			GLES20.glAttachShader(program, pixelShader);
			
			GLES20.glLinkProgram(program);
			int[] linked = new int[1];
			GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linked, 0);
			if(linked[0] == 0)
			{
//				Log.e("GLES20_ERR", "Compile Log:: " + GLES20.glGetProgramInfoLog(program));
				GLES20.glDeleteProgram(program);
				program = 0;
			}
		}
		
		return program;
	}
	
	public static String loadShaderFromAssets(String s, Resources r)
	{
		String res = null;
		AssetManager am = r.getAssets();
		if(am != null)
		{
			try 
			{
				InputStream is = am.open(s);
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				int c = -1;
				while((c = is.read()) != -1)
				{
					bos.write(c);
				}
				byte[] byteArr = bos.toByteArray();
				bos.close();
				is.close();
				res = new String(byteArr, "UTF-8");
				
				res.replaceAll("\\r\\n", "\n");
			} 
			catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		return res;
	}
}
