package com.uzbrain.mame4droid;

import android.util.Log;

public class UzbrainnetNative {
	
	private static final String TAG = "UzbrainnetNative";
	
	static {
		
		System.loadLibrary("UzbrainnetSDK_MAME");
		Log.d(TAG,"UzbrainnetSDK_MAME library loaded");
	}


	public UzbrainnetNative() {
		// TODO Auto-generated constructor stub
		super();
		// TODO Auto-generated constructor stub
		Log.d(TAG,"UzbrainnetNativeCall created");
	}

	
	public native void initialize();
	public native void parsingData(String oneLine);
	
	// get Value
	public native int getAxValue();
	public native int getAyValue();
	public native int getAzValue();
	public native int getGxValue();
	public native int getGyValue();
	public native int getGzValue();
	public native int getLeftButton();
	public native int getRightButton();
	public native int getModeButton();
	public native int getPowerButton();
	public native int getBatteryValue();
	public native int getTimeStamp();


}



