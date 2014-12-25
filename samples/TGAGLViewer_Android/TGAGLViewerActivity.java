/**
 * TGAGLViewerActivity.java
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

import android.app.Activity;
import android.os.Bundle;

public class TGAGLViewerActivity extends Activity {
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		view = new TGAGLSurfaceView(this);
		setContentView(view);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		view.onResume();
	}
	
	@Override
	public void onPause() {
		super.onPause();
		view.onPause();
	}
	
	private TGAGLSurfaceView view;
	
}
