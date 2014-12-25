/**
 * TGAGLSurfaceView.java
 * 
 * Copyright (c) 2014 Kenji Sasaki
 * Released under the MIT license.
 * https://github.com/npedotnet/TGAReader/blob/master/LICENSE
 * 
 * English document
 * https://github.com/npedotnet/TGAReader/blob/master/README.md
 * 
 * Japanese document
 * http://3dtech.jp/wiki/index.php?TGAReader
 * 
 */

package com.example.tgaglviewer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import static javax.microedition.khronos.opengles.GL10.*;

public class TGAGLSurfaceView extends GLSurfaceView implements GLSurfaceView.Renderer {
	
	public TGAGLSurfaceView(Context context) {
		super(context);
		setRenderer(this);
		
		float POSITIONS[] = {
			// front
			-0.5f, +0.5f, +0.5f,
			-0.5f, -0.5f, +0.5f,
			+0.5f, +0.5f, +0.5f,
			+0.5f, -0.5f, +0.5f,
			// back
			+0.5f, +0.5f, -0.5f,
			+0.5f, -0.5f, -0.5f,
			-0.5f, +0.5f, -0.5f,
			-0.5f, -0.5f, -0.5f,
		};
		float TEXCOORDS[] = {
			// front
			0.f, 0.f,
			0.f, 1.f,
			1.f, 0.f,
			1.f, 1.f,
			// back
			0.f, 0.f,
			0.f, 1.f,
			1.f, 0.f,
			1.f, 1.f,
		};
		positionBuffer = createFloatBuffer(POSITIONS);
		texcoordBuffer = createFloatBuffer(TEXCOORDS);
	}
	
	private FloatBuffer createFloatBuffer(float [] buffer) {
		ByteBuffer byteBuffer = ByteBuffer.allocateDirect(buffer.length*4);
		byteBuffer.order(ByteOrder.nativeOrder());
		FloatBuffer floatBuffer = byteBuffer.asFloatBuffer();
		floatBuffer.put(buffer);
		floatBuffer.position(0);
		return floatBuffer;
	}
	
	public int createTGATexture(GL10 gl, String path) {
		int texture = 0;
		try {
			
			InputStream is = getContext().getAssets().open(path);
			byte [] buffer = new byte[is.available()];
			is.read(buffer);
			is.close();
			
			int [] pixels = TGAReader.read(buffer, TGAReader.ABGR);
			int width = TGAReader.getWidth(buffer);
			int height = TGAReader.getHeight(buffer);
			
			int [] textures = new int[1];
			gl.glGenTextures(1, textures, 0);
			
			gl.glEnable(GL_TEXTURE_2D);
			gl.glBindTexture(GL_TEXTURE_2D, textures[0]);
			gl.glPixelStorei(GL_UNPACK_ALIGNMENT, 4);

			IntBuffer texBuffer = IntBuffer.wrap(pixels);
			gl.glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, texBuffer);
			
			gl.glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
			gl.glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
			gl.glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
			gl.glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
			
			texture = textures[0];
			
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		
		return texture;
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		tgaTextures[0] = createTGATexture(gl, "images/rgb_rle_LL.tga");
		tgaTextures[1] = createTGATexture(gl, "images/grayscale_rle_LL.tga");
		tgaTextures[2] = createTGATexture(gl, "images/indexed_rle_LL.tga");
	}

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		gl.glViewport(0, 0, width, height);
		
		gl.glMatrixMode(GL_PROJECTION);
		gl.glLoadIdentity();
		GLU.gluPerspective(gl, 45, (float)width/height, 1, 10);
	}

	@Override
	public void onDrawFrame(GL10 gl) {
		gl.glClearColor(0.5f, 0.5f, 0.5f, 1);
		gl.glClear(GL_DEPTH_BUFFER_BIT|GL_COLOR_BUFFER_BIT);
		
		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, positionBuffer);

		gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
		gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, texcoordBuffer);
		
		gl.glEnable(GL_CULL_FACE);
		gl.glCullFace(GL_BACK);
		
		gl.glMatrixMode(GL10.GL_MODELVIEW);
		gl.glLoadIdentity();
		GLU.gluLookAt(gl, 3, 3, 3, 0, 0, 0, 0, 1, 0);
		
		// draw front and back
		draw(gl, tgaTextures[0]);
		
		// draw left and right
		gl.glPushMatrix();
		gl.glRotatef(90, 0, 1, 0);
		draw(gl, tgaTextures[1]);
		gl.glPopMatrix();
		
		// draw top and bottom
		gl.glPushMatrix();
		gl.glRotatef(-90, 1, 0, 0);
		draw(gl, tgaTextures[2]);
		gl.glPopMatrix();
	}
	
	private void draw(GL10 gl, int texture) {
		
		gl.glEnable(GL_TEXTURE_2D);
		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_2D, texture);
		
		// front
		gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, 4);
		// back
		gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 4, 4);
		
		gl.glDisable(GL_TEXTURE_2D);
	}
	
	private FloatBuffer positionBuffer;
	private FloatBuffer texcoordBuffer;
	private int [] tgaTextures = new int[3];

}
