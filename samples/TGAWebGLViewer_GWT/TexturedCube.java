package net.npe.texturedcube.client;

import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.canvas.dom.client.CanvasPixelArray;
import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.canvas.dom.client.ImageData;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.typedarrays.client.Uint8ArrayNative;
import com.google.gwt.typedarrays.shared.ArrayBuffer;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.xhr.client.ReadyStateChangeHandler;
import com.google.gwt.xhr.client.XMLHttpRequest;
import com.google.gwt.xhr.client.XMLHttpRequest.ResponseType;
import com.googlecode.gwtgl.array.Float32Array;
import com.googlecode.gwtgl.binding.WebGLBuffer;
import com.googlecode.gwtgl.binding.WebGLProgram;
import com.googlecode.gwtgl.binding.WebGLRenderingContext;
import com.googlecode.gwtgl.binding.WebGLShader;
import com.googlecode.gwtgl.binding.WebGLTexture;
import com.googlecode.gwtgl.binding.WebGLUniformLocation;

import static com.googlecode.gwtgl.binding.WebGLRenderingContext.*;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class TexturedCube implements EntryPoint {
	
	private float [] VERTICES = {
			// position.xyz, texcoord.xy
			// front
			-0.5f, +0.5f, +0.5f, 0.f, 0.f,
			-0.5f, -0.5f, +0.5f, 0.f, 1.f,
			+0.5f, +0.5f, +0.5f, 1.f, 0.f,
			+0.5f, -0.5f, +0.5f, 1.f, 1.f,
			// back
			+0.5f, +0.5f, -0.5f, 0.f, 0.f,
			+0.5f, -0.5f, -0.5f, 0.f, 1.f,
			-0.5f, +0.5f, -0.5f, 1.f, 0.f,
			-0.5f, -0.5f, -0.5f, 1.f, 1.f,
	};
	
	private String VERTEX_SHADER_SOURCE =
			"attribute vec3 position;"+
			"attribute vec2 texcoord;"+
			"varying vec2 vTexcoord;"+
			"uniform mat4 MVP;"+
			"void main() { gl_Position = MVP * vec4(position, 1.0); vTexcoord = texcoord; }";
	
	private String FRAGMENT_SHADER_SOURCE =
			"precision highp float;"+
			"varying vec2 vTexcoord;"+
			"uniform sampler2D texture;"+
			"void main() { gl_FragColor = texture2D(texture, vTexcoord); }";
	
	private float [][] MODELMATRICES = {
			{ // none
				1, 0, 0, 0,
				0, 1, 0, 0,
				0, 0, 1, 0,
				0, 0, 0, 1,
			},
			{ // rotY90
				0, 0, -1, 0,
				0, 1, 0, 0,
				1, 0, 0, 0,
				0, 0, 0, 1,
			},
			{ // rotX-90
				1, 0, 0, 0,
				0, 0, -1, 0,
				0, 1, 0, 0,
				0, 0, 0, 1,
			},
	};

	public static final String [] TEXTURE_URLS = {
		"images/rgb_rle_LL.tga",
		"images/grayscale_rle_LL.tga",
		"images/indexed_rle_LL.tga",
	};
	
	public void onModuleLoad() {
		
		Canvas canvas = Canvas.createIfSupported();
		canvas.setStyleName("MyCanvas");
		canvas.setCoordinateSpaceWidth(400);
		canvas.setCoordinateSpaceHeight(400);
		RootLayoutPanel.get().add(canvas);
		
		gl = (WebGLRenderingContext)canvas.getContext("experimental-webgl");
		gl.viewport(0, 0, 400, 400);
		
		WebGLBuffer vertexBuffer = gl.createBuffer();
		gl.bindBuffer(ARRAY_BUFFER, vertexBuffer);
		gl.bufferData(ARRAY_BUFFER, Float32Array.create(VERTICES), STATIC_DRAW);

		WebGLShader vertexShader = gl.createShader(VERTEX_SHADER);
		gl.shaderSource(vertexShader, VERTEX_SHADER_SOURCE);
		gl.compileShader(vertexShader);
		
		WebGLShader fragmentShader = gl.createShader(FRAGMENT_SHADER);
		gl.shaderSource(fragmentShader, FRAGMENT_SHADER_SOURCE);
		gl.compileShader(fragmentShader);
		
		program = gl.createProgram();
		gl.attachShader(program, vertexShader);
		gl.attachShader(program, fragmentShader);
		gl.linkProgram(program);
		
		gl.useProgram(program);
		gl.bindBuffer(ARRAY_BUFFER, vertexBuffer);
		
		WebGLUniformLocation texture = gl.getUniformLocation(program, "texture");
		gl.uniform1i(texture, 0);
		
		int posAttr = gl.getAttribLocation(program, "position");
		gl.vertexAttribPointer(posAttr, 3, FLOAT, false, 5*4, 0);
		gl.enableVertexAttribArray(posAttr);
		
		int texAttr = gl.getAttribLocation(program, "texcoord");
		gl.vertexAttribPointer(texAttr, 2, FLOAT, false, 5*4, 3*4);
		gl.enableVertexAttribArray(texAttr);
		
		for(int i=0; i<TEXTURE_URLS.length; i++) {
			loadTexture(TEXTURE_URLS[i], i);
		}
		
	}
	
	void draw() {
		
		gl.clearColor(0.5f, 0.5f, 0.5f, 1);
		gl.clear(COLOR_BUFFER_BIT);
		gl.enable(CULL_FACE);
		gl.cullFace(BACK);
		gl.enable(BLEND);
		gl.blendFunc(SRC_ALPHA, ONE_MINUS_SRC_ALPHA);
		
		float [] view = new float[16];
		lookAt(view, 2, 2, 2, 0, 0, 0, 0, 1, 0);
		
		float [] proj = new float[16];
		perspective(proj, 45, 1.f, 1.f, 10.f);
		
		float [] vp = new float[16];
		multiply(vp, view, proj);
		
		float [] mvp = new float[16];
		for(int i=0; i<3; i++) {
			multiply(mvp, MODELMATRICES[i], vp);
			draw(mvp, textures[i]);
		}
		
	}
	
	void draw(float [] mvp, WebGLTexture texture) {
		
		WebGLUniformLocation MVP = gl.getUniformLocation(program, "MVP");
		gl.uniformMatrix4fv(MVP, false, mvp);
		
		gl.bindTexture(TEXTURE_2D, texture);
		gl.activeTexture(TEXTURE0);
		
		gl.drawArrays(TRIANGLE_STRIP, 0, 4);
		gl.drawArrays(TRIANGLE_STRIP, 4, 4);
		
	}
	
	Canvas createImageCanvas(int [] pixels, int width, int height) {

	    Canvas canvas = Canvas.createIfSupported();
	    canvas.setCoordinateSpaceWidth(width);
	    canvas.setCoordinateSpaceHeight(height);

	    Context2d context = canvas.getContext2d();
	    ImageData data = context.createImageData(width, height);

	    CanvasPixelArray array = data.getData();
	    for(int i=0; i<width*height; i++) {
	        array.set(4*i+0, pixels[i] & 0xFF);
	        array.set(4*i+1, (pixels[i] >> 8) & 0xFF);
	        array.set(4*i+2, (pixels[i] >> 16) & 0xFF);
	        array.set(4*i+3, (pixels[i] >> 24) & 0xFF);
	    }
	    context.putImageData(data, 0, 0);

	    return canvas;

	}
	
	void loadTexture(String url, int index) {
		final int i = index;
	    XMLHttpRequest request = XMLHttpRequest.create();
	    request.open("GET", url);
	    request.setResponseType(ResponseType.ArrayBuffer);
	    request.setOnReadyStateChange(new ReadyStateChangeHandler() {
	        @Override
	        public void onReadyStateChange(XMLHttpRequest xhr) {
	            if(xhr.getReadyState() == XMLHttpRequest.DONE) {
	                if(xhr.getStatus() >= 400) {
	                    // error
	                    System.out.println("Error");
	                }
	                else {
	                	try {
		                	ArrayBuffer arrayBuffer = xhr.getResponseArrayBuffer();
		    				Uint8ArrayNative u8array = Uint8ArrayNative.create(arrayBuffer);
		    				byte [] buffer = new byte[u8array.length()];
		    				for(int i=0; i<buffer.length; i++) {
		    					buffer[i] = (byte)u8array.get(i);
		    				}
		    				
		    				int [] pixels = TGAReader.read(buffer, TGAReader.ABGR);
		    				int width = TGAReader.getWidth(buffer);
		    				int height = TGAReader.getHeight(buffer);
		    				
		    				Canvas canvas = createImageCanvas(pixels, width, height);
		    				
		    				WebGLTexture texture = gl.createTexture();
		    				gl.enable(TEXTURE_2D);
		    				gl.bindTexture(TEXTURE_2D, texture);
		    				
		    				gl.texImage2D(TEXTURE_2D, 0, RGBA, RGBA, UNSIGNED_BYTE, canvas.getElement());
	
		    				gl.texParameteri(TEXTURE_2D, TEXTURE_WRAP_S, CLAMP_TO_EDGE);
		    				gl.texParameteri(TEXTURE_2D, TEXTURE_WRAP_T, CLAMP_TO_EDGE);
		    				gl.texParameteri(TEXTURE_2D, TEXTURE_MAG_FILTER, LINEAR);
		    				gl.texParameteri(TEXTURE_2D, TEXTURE_MIN_FILTER, LINEAR);
		    				
		    				textures[i] = texture;
		    				draw();
	                	}
	                	catch(Exception e) {
	                		e.printStackTrace();
	                	}
	                }
	            }
	        }
	    });
	    request.send();
	}
	
	public static void multiply(float [] m0, float [] m1, float [] m2) {
		
		m0[ 0] = m1[ 0]*m2[ 0] + m1[ 1]*m2[ 4] + m1[ 2]*m2[ 8] + m1[ 3]*m2[12];
		m0[ 1] = m1[ 0]*m2[ 1] + m1[ 1]*m2[ 5] + m1[ 2]*m2[ 9] + m1[ 3]*m2[13];
		m0[ 2] = m1[ 0]*m2[ 2] + m1[ 1]*m2[ 6] + m1[ 2]*m2[10] + m1[ 3]*m2[14];
		m0[ 3] = m1[ 0]*m2[ 3] + m1[ 1]*m2[ 7] + m1[ 2]*m2[11] + m1[ 3]*m2[15];

		m0[ 4] = m1[ 4]*m2[ 0] + m1[ 5]*m2[ 4] + m1[ 6]*m2[ 8] + m1[ 7]*m2[12];
		m0[ 5] = m1[ 4]*m2[ 1] + m1[ 5]*m2[ 5] + m1[ 6]*m2[ 9] + m1[ 7]*m2[13];
		m0[ 6] = m1[ 4]*m2[ 2] + m1[ 5]*m2[ 6] + m1[ 6]*m2[10] + m1[ 7]*m2[14];
		m0[ 7] = m1[ 4]*m2[ 3] + m1[ 5]*m2[ 7] + m1[ 6]*m2[11] + m1[ 7]*m2[15];
		
		m0[ 8] = m1[ 8]*m2[ 0] + m1[ 9]*m2[ 4] + m1[10]*m2[ 8] + m1[11]*m2[12];
		m0[ 9] = m1[ 8]*m2[ 1] + m1[ 9]*m2[ 5] + m1[10]*m2[ 9] + m1[11]*m2[13];
		m0[10] = m1[ 8]*m2[ 2] + m1[ 9]*m2[ 6] + m1[10]*m2[10] + m1[11]*m2[14];
		m0[11] = m1[ 8]*m2[ 3] + m1[ 9]*m2[ 7] + m1[10]*m2[11] + m1[11]*m2[15];
		
		m0[12] = m1[12]*m2[ 0] + m1[13]*m2[ 4] + m1[14]*m2[ 8] + m1[15]*m2[12];
		m0[13] = m1[12]*m2[ 1] + m1[13]*m2[ 5] + m1[14]*m2[ 9] + m1[15]*m2[13];
		m0[14] = m1[12]*m2[ 2] + m1[13]*m2[ 6] + m1[14]*m2[10] + m1[15]*m2[14];
		m0[15] = m1[12]*m2[ 3] + m1[13]*m2[ 7] + m1[14]*m2[11] + m1[15]*m2[15];
		
	}
	
    public static void lookAt(float [] m, float ex, float ey, float ez, float cx, float cy, float cz, float ux, float uy, float uz) {

        float fx = ex - cx;
        float fy = ey - cy;
        float fz = ez - cz;
        float rlf = 1.f / (float)Math.sqrt(fx*fx + fy*fy + fz*fz);
        m[2] = rlf * fx;
        m[6] = rlf * fy;
        m[10] = rlf * fz;

        float sx = m[10] * uy - m[6] * uz;
        float sy = m[2] * uz - m[10] * ux;
        float sz = m[6] * ux - m[2] * uy;
        float rls = 1.f / (float)Math.sqrt(sx*sx + sy*sy + sz*sz);
        m[0] = rls * sx;
        m[4] = rls * sy;
        m[8] = rls * sz;

        m[1] = m[6] * m[8] - m[10] * m[4];
        m[5] = m[10] * m[0] - m[2] * m[8];
        m[9] = m[2] * m[4] - m[6] * m[0];
        
        m[12] = -(ex * m[0] + ey * m[4] + ez * m[8]);
        m[13] = -(ex * m[1] + ey * m[5] + ez * m[9]);
        m[14] = -(ex * m[2] + ey * m[6] + ez * m[10]);

        m[3] = m[7] = m[11] = 0;
        m[15] = 1;

    }

	public static void perspective(float [] m, float fov, float aspect, float near, float far) {

		float top = near * (float)Math.tan(fov * Math.PI / 360.0);
		float bottom = -top;
		float left = bottom * aspect;
		float right = top * aspect;
		
		m[0] = 2 * near / (right - left);
		m[1] = 0;
		m[2] = 0;
		m[3] = 0;
		m[4] = 0;
		m[5] = 2 * near / (top - bottom);
		m[6] = 0;
		m[7] = 0;
		m[8] = (right + left) / (right - left);
		m[9] = (top + bottom) / (top - bottom);
		m[10] = -(far + near) / (far - near);
		m[11] = -1;
		m[12] = 0;
		m[13] = 0;
		m[14] = -2 * far * near / (far - near);
		m[15] = 0;
		
	}
	
	private WebGLRenderingContext gl;
	private WebGLProgram program;
	private WebGLTexture [] textures = new WebGLTexture[3];
	
}
