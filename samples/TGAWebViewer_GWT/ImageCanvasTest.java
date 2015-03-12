package net.npe.imagecanvastest.client;

import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.canvas.dom.client.CanvasPixelArray;
import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.canvas.dom.client.ImageData;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.typedarrays.client.Uint8ArrayNative;
import com.google.gwt.typedarrays.shared.ArrayBuffer;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.xhr.client.ReadyStateChangeHandler;
import com.google.gwt.xhr.client.XMLHttpRequest;
import com.google.gwt.xhr.client.XMLHttpRequest.ResponseType;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class ImageCanvasTest implements EntryPoint {
	
	private static String [] TGA_FILES = {
		"grayscale_a_LL.tga",
		"grayscale_a_LR.tga",
		"grayscale_a_rle_LL.tga",
		"grayscale_a_rle_LR.tga",
		"grayscale_a_rle_UL.tga",
		"grayscale_a_rle_UR.tga",
		"grayscale_a_UL.tga",
		"grayscale_a_UR.tga",
		"grayscale_LL.tga",
		"grayscale_LR.tga",
		"grayscale_rle_LL.tga",
		"grayscale_rle_LR.tga",
		"grayscale_rle_UL.tga",
		"grayscale_rle_UR.tga",
		"grayscale_UL.tga",
		"grayscale_UR.tga",
		"indexed_a_LL.tga",
		"indexed_a_LR.tga",
		"indexed_a_rle_LL.tga",
		"indexed_a_rle_LR.tga",
		"indexed_a_rle_UL.tga",
		"indexed_a_rle_UR.tga",
		"indexed_a_UL.tga",
		"indexed_a_UR.tga",
		"indexed_LL.tga",
		"indexed_LR.tga",
		"indexed_rle_LL.tga",
		"indexed_rle_LR.tga",
		"indexed_rle_UL.tga",
		"indexed_rle_UR.tga",
		"indexed_UL.tga",
		"indexed_UR.tga",
		"rgb_a_LL.tga",
		"rgb_a_LR.tga",
		"rgb_a_rle_LL.tga",
		"rgb_a_rle_LR.tga",
		"rgb_a_rle_UL.tga",
		"rgb_a_rle_UR.tga",
		"rgb_a_UL.tga",
		"rgb_a_UR.tga",
		"rgb_LL.tga",
		"rgb_LR.tga",
		"rgb_rle_LL.tga",
		"rgb_rle_LR.tga",
		"rgb_rle_UL.tga",
		"rgb_rle_UR.tga",
		"rgb_UL.tga",
		"rgb_UR.tga",
	};

	public void onModuleLoad() {
		
		panel = new FlowPanel();
		panel.getElement().getStyle().setBackgroundColor("orange");

		for(int i=0; i<TGA_FILES.length; i++) {
			addTGACanvas("images/"+TGA_FILES[i]);
		}
		
		RootLayoutPanel.get().add(new ScrollPanel(panel));
		
	}
	
	private FlowPanel panel;
	
	private void addTGACanvas(String url) {
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
							int pixels [] = TGAReader.read(buffer, TGAReader.ABGR);
							int width = TGAReader.getWidth(buffer);
							int height = TGAReader.getHeight(buffer);
							
							Canvas canvas = createImageCanvas(pixels, width, height);
							panel.add(canvas);
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
	
	private Canvas createImageCanvas(int [] pixels, int width, int height) {
		
		Canvas canvas = Canvas.createIfSupported();
		canvas.setCoordinateSpaceWidth(width);
		canvas.setCoordinateSpaceHeight(height);
		
		Context2d context = canvas.getContext2d();
		ImageData data = context.createImageData(width, height);

		CanvasPixelArray array = data.getData();
		for(int i=0; i<width*height; i++) { // ABGR
			array.set(4*i+0, pixels[i] & 0xFF);
			array.set(4*i+1, (pixels[i] >> 8) & 0xFF);
			array.set(4*i+2, (pixels[i] >> 16) & 0xFF);
			array.set(4*i+3, (pixels[i] >> 24) & 0xFF);
		}
		context.putImageData(data, 0, 0);
		
		canvas.getElement().getStyle().setMargin(4, Unit.PX);
		
		return canvas;
		
	}
	
}
