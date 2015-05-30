package com.example.tgaconverter_android;

import java.io.FileOutputStream;
import java.io.InputStream;

import android.support.v7.app.ActionBarActivity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends ActionBarActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		String inputPath = "images/Mandrill.bmp";
		String outputPath = "Mandrill.tga";
		
		try {
			InputStream is = getAssets().open(inputPath);
			Bitmap bitmap = BitmapFactory.decodeStream(is);
			is.close();
			
			int width = bitmap.getWidth();
			int height = bitmap.getHeight();
			int [] pixels = new int[width*height];
			bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
			
			byte [] buffer = TGAWriter.write(pixels, width, height, TGAReader.ARGB);

			FileOutputStream fos = this.openFileOutput(outputPath, MODE_PRIVATE);
			fos.write(buffer);
			fos.close();
		}
		catch(Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
