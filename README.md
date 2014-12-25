TGAReader
=========

TGAReader is the TGA(Targa) image reader for Java and C.

![alt text](http://3dtech.jp/wiki/index.php?plugin=attach&refer=TGAReader&openfile=TGAReader.png "TGAReader")

## LICENSE

Released under the MIT license.

https://github.com/npedotnet/TGAReader/blob/master/LICENSE

## Getting Started

### 1. Add the source code to your project.

**Java**
- Add TGAReader.java to your project, and modify package statement.

**C**
- Add tga_reader.c and tga_reader.h to your project.

### 2. Create TGA binary data buffer.

**Java**
```java
	FileInputStream fis = new FileInputStream(new File("test.tga"));
	byte [] buffer = new byte[fis.available()];
	fis.read(buffer);
	fis.close();
```

**C**
```c
#include "tga_reader.h"

FILE *file = fopen("test.tga", "rb");
if(file) {
	int size;
	fseek(file, 0, SEEK_END);
	size = ftell(file);
	fseek(file, 0, SEEK_SET);

	unsigned char *buffer = (unsigned char *)tgaMalloc(size);
	fread(buffer, 1, size, file);
	fclose(file);
}
```

### 3. Create pixels with the RGBA byte order parameter.

ByteOrder|Java|C|Comments
---|---|---|---
ARGB|TGAReader.ARGB|TGA_READER_ARGB|for java.awt.image.BufferedImage, android.graphics.Bitmap
ABGR|TGAReader.ABGR|TGA_READER_ABGR|for OpenGL Texture(GL_RGBA), iOS UIImage

**Java**
```java
	byte [] buffer = ...;
	int [] pixels = TGAReader.read(buffer, TGAReader.ARGB);
	int width = TGAReader.getWidth(buffer);
	int height = TGAReader.getHeight(buffer);
```

**C**
```c
	unsigned char *buffer = ...;
	int *pixels = tgaRead(buffer, TGA_READER_ABGR);
	int width = tgaGetWidth(buffer);
	int height = tgaGetHeight(buffer);
```

### 4. Use created pixels in your application.

#### Java OpenGL Application
Sample code to create Java OpenGL texture.

#### Java Application
Sample code to create java.awt.image.BufferedImage.

For more details, please look at the sample project.
TODO: path

#### Android OpenGL Application
Sample code to create Android OpenGL texture.
 
For more details, please look at the sample project.
TODO: path

#### Android Application
Sample code to create android.graphics.Bitmap.

For more details, please look at the sample project.
TODO: path

#### iOS OpenGL Application
Sample code to create iOS OpenGL texture.

For more details, please look at the sample project.
TODO: path

#### iOS Application
Sample code to create iOS UIImage.

For more details, please look at the sample project.
TODO: path

### 5. Free allocated memory (C language Only)

**C**
```c
	unsigned char *buffer = ...;
	int *pixels = tgaRead(buffer, TGA_READER_ABGR);
	if(pixels) {
		tgaFree(pixels);
	}
	tgaFree(buffer);
```

## Memory Management (C language Only)

If you have your memory management system, please customize tgaMalloc() and tgaFree().

**C**
```c
	void *tgaMalloc(size_t size) {
		return malloc(size);
	}

	void tgaFree(void *memory) {
		free(memory);
	}
```

## Supported
- Colormap(Indexed) Image, RGB Color Image, Grayscale Image
- Run Length Encoding
- Colormap origin offset
- Image origin(LowerLeft, LowerRight, UpperLeft, UpperRight)

## Unsupported
- Image Type 0, 32, 33
- 16bit RGB Color image
- X/Y origin offset of image


Thank you for reading through. Enjoy your programming life!
