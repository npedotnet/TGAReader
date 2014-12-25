/**
 * TGABitmapViewerActivity.java
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

package com.example.tgabitmapviewer;

import java.io.IOException;
import java.io.InputStream;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.os.Bundle;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

@SuppressLint("NewApi")
public class TGABitmapViewerActivity extends Activity {
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		GridLayout layout = new GridLayout(this);
		ScrollView scroll = new ScrollView(this);
		scroll.addView(layout);
		setContentView(scroll);
		
		try {
			String [] list = getAssets().list("images");
			
			// count tga images
			int count = 0;
			for(int i=0; i<list.length; i++) {
				if(list[i].endsWith(".tga")) count++;
			}
			
			layout.setColumnCount(3);
			layout.setRowCount(count/3);
			
			// create tga image view
			for(int i=0; i<list.length; i++) {
				if(list[i].endsWith(".tga")) {
					LinearLayout view = createTGAView(list[i]);
					if(view != null) layout.addView(view);
				}
			}
			
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private LinearLayout createTGAView(String name) {
		ImageView image = createTGAImageView("images/"+name);
		if(image != null) {
			LinearLayout layout = new LinearLayout(this);
			layout.setOrientation(LinearLayout.VERTICAL);
			layout.addView(image);
			TextView label = new TextView(this);
			label.setText(name);
			label.setMaxWidth(128);
			label.setPadding(8, 8, 0, 0);
			layout.addView(label);
			return layout;
		}
		return null;
	}
	
	private ImageView createTGAImageView(String path) {
		Bitmap bitmap = createTGABitmap(path);
		if(bitmap != null) {
			ImageView imageView = new ImageView(this);
			imageView.setImageBitmap(bitmap);
			imageView.setAdjustViewBounds(true);
			imageView.setMaxWidth(128);
			imageView.setMaxHeight(128);
			imageView.setPadding(8, 8, 0, 0);
			return imageView;
		}
		return null;
	}
	
	private Bitmap createTGABitmap(String path) {
		Bitmap bitmap = null;
		try {
			InputStream is = getAssets().open(path);
			byte [] buffer = new byte[is.available()];
			is.read(buffer);
			is.close();
			
			int [] pixels = TGAReader.read(buffer, TGAReader.ARGB);
			int width = TGAReader.getWidth(buffer);
			int height = TGAReader.getHeight(buffer);
			
			bitmap = Bitmap.createBitmap(pixels, 0, width, width, height, Config.ARGB_8888);
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		return bitmap;
	}

}
