import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import net.npe.tga.TGAReader;
import net.npe.tga.TGAWriter;


public class TGAConverter_BufferedImage {
	
	public static void main(String [] args) {
		
		String path = "images/Mandrill.bmp";
		
		try {
			BufferedImage image = ImageIO.read(new File(path));
			int width = image.getWidth();
			int height = image.getHeight();
			int [] pixels = image.getRGB(0, 0, width, height, null, 0, width);
			
			byte [] buffer = TGAWriter.write(pixels, width, height, TGAReader.ARGB);
			FileOutputStream fos = new FileOutputStream(path.replace(".bmp", ".tga"));
			fos.write(buffer);
			fos.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		
	}

}
