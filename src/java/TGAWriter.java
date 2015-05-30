/**
 * TGAWriter.java
 * 
 * Copyright (c) 2015 Kenji Sasaki
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

package net.npe.tga;

public class TGAWriter {
	
	public enum EncodeType {
		NONE,	// No RLE encoding
		RLE,	// RLE encoding
		AUTO,	// auto
	}
	
	public static byte [] write(int [] pixels, int width, int height, TGAReader.Order order) {
		return write(pixels, width, height, order, EncodeType.AUTO);
	}

	public static byte [] write(int [] pixels, int width, int height, TGAReader.Order order, EncodeType encodeType) {
		
		int elementCount = hasAlpha(pixels, order) ? 4 : 3;
		
		int rawSize = elementCount * pixels.length;
		int rleSize = getEncodeSize(pixels, width, elementCount);
		
		boolean encoding;
		int dataSize;
		
		switch(encodeType) {
		case RLE:
			encoding = true;
			dataSize = rleSize;
			break;
		case AUTO:
			encoding = rleSize < rawSize;
			dataSize = encoding ? rleSize : rawSize;
			break;
		default:
			// raw
			encoding = false;
			dataSize = rawSize;
			break;
		}
		
		int length = 18 + FOOTER.length + dataSize;
		
		byte [] buffer = new byte[length];
		
		int index = 0;
		
		// Header
		buffer[index++] = 0; // idFieldLength
		buffer[index++] = 0; // colormapType
		buffer[index++] = (byte)(encoding ? 10 : 2); // RGB or RGB_RLE
		buffer[index++] = 0; buffer[index++] = 0; // colormapOrigin
		buffer[index++] = 0; buffer[index++] = 0; // colormapLength
		buffer[index++] = 0; // colormapDepth
		buffer[index++] = 0; buffer[index++] = 0; // originX
		buffer[index++] = 0; buffer[index++] = 0; // originY
		buffer[index++] = (byte)((width >> 0) & 0xFF); // width
		buffer[index++] = (byte)((width >> 8) & 0xFF); // width
		buffer[index++] = (byte)((height >> 0) & 0xFF); // height
		buffer[index++] = (byte)((height >> 8) & 0xFF); // height
		buffer[index++] = (byte)(8*elementCount); // depth
		buffer[index++] = 0x20; // descriptor TODO alpha channel depth
		
		if(encoding) {
			index = encodeRLE(pixels, width, elementCount, order, buffer, index);
		}
		else {
			index = writeRaw(pixels, buffer, index, elementCount, order);
		}
		
		// Copy Footer
		for(int i=0; i<FOOTER.length; i++) {
			buffer[index++] = FOOTER[i];
		}

		return buffer;
		
	}
	
	private static int writeRaw(int [] pixels, byte [] buffer, int index, int elementCount, TGAReader.Order order) {
		if(elementCount == 3) {
			// BGR
			for(int i=0; i<pixels.length; i++) {
				buffer[index++] = (byte)((pixels[i] >> order.blueShift) & 0xFF);
				buffer[index++] = (byte)((pixels[i] >> order.greenShift) & 0xFF);
				buffer[index++] = (byte)((pixels[i] >> order.redShift) & 0xFF);
			}
		}
		else {
			// BGRA
			for(int i=0; i<pixels.length; i++) {
				buffer[index++] = (byte)((pixels[i] >> order.blueShift) & 0xFF);
				buffer[index++] = (byte)((pixels[i] >> order.greenShift) & 0xFF);
				buffer[index++] = (byte)((pixels[i] >> order.redShift) & 0xFF);
				buffer[index++] = (byte)((pixels[i] >> order.alphaShift) & 0xFF);
			}
		}
		return index;
	}

	private static final int MODE_RESET = 0;
	private static final int MODE_SELECT = 1;
	private static final int MODE_SAME_COLOR = 2;
	private static final int MODE_DIFFERENT_COLOR = 3;
	
	private static int getEncodeSize(int [] pixels, int width, int elementCount) {
		
		int size = 0;
		int color = 0;
		int mode = MODE_RESET;
		int start = 0;
		
		for(int i=0; i<pixels.length; i++) {
			if(mode == MODE_RESET) {
				color = pixels[i];
				mode = MODE_SELECT;
				start = i;
			}
			else if(mode == MODE_SELECT) {
				mode = (color == pixels[i]) ? MODE_SAME_COLOR : MODE_DIFFERENT_COLOR;
				color = pixels[i];
			}
			else if(mode == MODE_SAME_COLOR) {
				if(color != pixels[i]) {
					// packet + rleData
					size += 1 + elementCount;
					mode = MODE_SELECT;
					color = pixels[i];
					start = i;
				}
				else if((i-start) >= 127) {
					size += 1 + elementCount;
					mode = MODE_RESET;
				}
			}
			else if(mode == MODE_DIFFERENT_COLOR) {
				if(color == pixels[i]) {
					// packet + rawData * count
					size += 1 + elementCount*(i-1-start);
					mode = MODE_SAME_COLOR;
					color = pixels[i];
					start = i-1;
				}
				else if((i-start) >= 127) {
					size += 1 + elementCount*128;
					mode = MODE_RESET;
				}
			}
			
			if((i+1)%width == 0 && mode != MODE_RESET) {
				if(mode == MODE_SAME_COLOR) {
					size += 1 + elementCount;
				}
				else {
					// MODE_SELECT or MODE_DIFFERENT_COLOR
					size += 1 + elementCount*(i-start+1);
				}
				mode = MODE_RESET;
			}
			
			// update color
			color = pixels[i];
		}

		if(mode != MODE_RESET) {
			System.out.println("Error!");
		}
		
		return size;
		
	}
	
	private static int encodeRLE(int [] pixels, int width, int elementCount, TGAReader.Order order, byte [] buffer, int index) {
		
		int color = 0;
		int mode = MODE_RESET;
		int start = 0;
		
		for(int i=0; i<pixels.length; i++) {
			if(mode == MODE_RESET) {
				color = pixels[i];
				mode = MODE_SELECT;
				start = i;
			}
			else if(mode == MODE_SELECT) {
				mode = (color == pixels[i]) ? MODE_SAME_COLOR : MODE_DIFFERENT_COLOR;
				color = pixels[i];
			}
			else if(mode == MODE_SAME_COLOR) {
				if(color != pixels[i]) {
					// packet + rleData
					index = encodeRLE(buffer, index, color, i-start, elementCount, order);
					mode = MODE_SELECT;
					color = pixels[i];
					start = i;
				}
				else if((i-start) >= 127) {
					index = encodeRLE(buffer, index, color, 128, elementCount, order);
					mode = MODE_RESET;
				}
			}
			else if(mode == MODE_DIFFERENT_COLOR) {
				if(color == pixels[i]) {
					// packet + rawData * count
					index = encodeRLE(buffer, index, pixels, start, i-1-start, elementCount, order);
					mode = MODE_SAME_COLOR;
					color = pixels[i];
					start = i-1;
				}
				else if((i-start) >= 127) {
					index = encodeRLE(buffer, index, pixels, start, 128, elementCount, order);
					mode = MODE_RESET;
				}
			}
			
			if((i+1)%width == 0 && mode != MODE_RESET) {
				if(mode == MODE_SAME_COLOR) {
					index = encodeRLE(buffer, index, color, i-start+1, elementCount, order);
				}
				else {
					// MODE_SELECT or MODE_DIFFERENT_COLOR
					index = encodeRLE(buffer, index, pixels, start, i-start+1, elementCount, order);
				}
				mode = MODE_RESET;
			}
			
			// update color
			color = pixels[i];
		}

		if(mode != MODE_RESET) {
			System.out.println("Error!");
		}
		
		return index;
		
	}
	
	private static int encodeRLE(byte [] buffer, int index, int color, int count, int elementCount, TGAReader.Order order) {
		buffer[index++] = (byte)(0x80 | (count-1));
		buffer[index++] = (byte)((color >> order.blueShift) & 0xFF);
		buffer[index++] = (byte)((color >> order.greenShift) & 0xFF);
		buffer[index++] = (byte)((color >> order.redShift) & 0xFF);
		if(elementCount == 4) {
			buffer[index++] = (byte)((color >> order.alphaShift) & 0xFF);
		}
		return index;
	}
	
	private static int encodeRLE(byte [] buffer, int index, int [] pixels, int start, int count, int elementCount, TGAReader.Order order) {
		buffer[index++] = (byte)(count-1);
		for(int i=0; i<count; i++) {
			int color = pixels[start+i];
			buffer[index++] = (byte)((color >> order.blueShift) & 0xFF);
			buffer[index++] = (byte)((color >> order.greenShift) & 0xFF);
			buffer[index++] = (byte)((color >> order.redShift) & 0xFF);
			if(elementCount == 4) {
				buffer[index++] = (byte)((color >> order.alphaShift) & 0xFF);
			}
		}
		return index;
	}
	
	private static boolean hasAlpha(int [] pixels, TGAReader.Order order) {
		int alphaShift = order.alphaShift;
		for(int i=0; i<pixels.length; i++) {
			int alpha = (pixels[i] >> alphaShift) & 0xFF;
			if(alpha != 0xFF) return true;
		}
		return false;
	}
	
	private static final byte [] FOOTER = {0,0,0,0,0,0,0,0,84,82,85,69,86,73,83,73,79,78,45,88,70,73,76,69,46,0}; // TRUEVISION-XFILE
	
}
