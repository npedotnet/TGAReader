/**
 * TGASwingBufferedImage.java
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

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.IOException;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class TGASwingBufferedImage {
	
	private static final String [] TGA_PATHS = {
		"images/rgb_LL.tga",
		"images/grayscale_LL.tga",
		"images/indexed_LL.tga",
		"images/rgb_a_rle_LL.tga",
		"images/grayscale_a_rle_LL.tga",
		"images/indexed_a_rle_LL.tga",
	};
	
	public static void main(String [] args) {
		try {
			JPanel panel = new JPanel();
			panel.setBackground(Color.ORANGE);
			for(int i=0; i<TGA_PATHS.length; i++) {
				panel.add(createTGALabel(TGA_PATHS[i]));
			}
			JFrame frame = new JFrame();
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setTitle("TGA Swing BufferedImage");
			frame.setSize(420, 310);
			frame.setVisible(true);
			frame.getContentPane().add(panel);
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private static JLabel createTGALabel(String path) throws IOException {
		
		FileInputStream fis = new FileInputStream(path);
		byte [] buffer = new byte[fis.available()];
		fis.read(buffer);
		fis.close();
		
		int [] pixels = TGAReader.read(buffer, TGAReader.ARGB);
		int width = TGAReader.getWidth(buffer);
		int height = TGAReader.getHeight(buffer);
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		image.setRGB(0, 0, width, height, pixels, 0, width);
		
		ImageIcon icon = new ImageIcon(image.getScaledInstance(128, 128, BufferedImage.SCALE_SMOOTH));
		return new JLabel(icon);
	}

}
