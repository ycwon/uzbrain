/*
 * This file is part of MAME4droid.
 *
 * Copyright (C) 2013 David Valdeita (Seleuco)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 * Linking MAME4droid statically or dynamically with other modules is
 * making a combined work based on MAME4droid. Thus, the terms and
 * conditions of the GNU General Public License cover the whole
 * combination.
 *
 * In addition, as a special exception, the copyright holders of MAME4droid
 * give you permission to combine MAME4droid with free software programs
 * or libraries that are released under the GNU LGPL and with code included
 * in the standard release of MAME under the MAME License (or modified
 * versions of such code, with unchanged license). You may copy and
 * distribute such a system following the terms of the GNU GPL for MAME4droid
 * and the licenses of the other code concerned, provided that you include
 * the source code of that other code when and as the GNU GPL requires
 * distribution of source code.
 *
 * Note that people who make modified versions of MAME4idroid are not
 * obligated to grant this special exception for their modified versions; it
 * is their choice whether to do so. The GNU General Public License
 * gives permission to release a modified version without this exception;
 * this exception also makes it possible to release a modified version
 * which carries forward this exception.
 *
 * MAME4droid is dual-licensed: Alternatively, you can license MAME4droid
 * under a MAME license, as set out in http://mamedev.org/
 */

package com.uzbrain.mame4droid;

import java.io.File;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.uzbrain.mame4droid.R;
import com.uzbrain.mame4droid.helpers.DialogHelper;
import com.uzbrain.mame4droid.helpers.MainHelper;
import com.uzbrain.mame4droid.helpers.MenuHelper;
import com.uzbrain.mame4droid.helpers.PrefsHelper;
import com.uzbrain.mame4droid.input.ControlCustomizer;
import com.uzbrain.mame4droid.input.InputHandler;
import com.uzbrain.mame4droid.input.InputHandlerExt;
import com.uzbrain.mame4droid.input.InputHandlerFactory;
import com.uzbrain.mame4droid.views.FilterView;
import com.uzbrain.mame4droid.views.IEmuView;
import com.uzbrain.mame4droid.views.InputView;

import android.app.*;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.*;

final class NotificationHelper
{
        private static NotificationManager notificationManager = null;

		public static void addNotification(Context ctx, String onShow, String title, String message)
        {
                if(notificationManager == null)
                        notificationManager = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
                int icon = R.drawable.icon_sb; // TODO: don't hard-code
                long when = System.currentTimeMillis();
                Notification notification = new Notification(icon, /*onShow*/null, when);
                notification.flags |= Notification.FLAG_ONGOING_EVENT | Notification.FLAG_AUTO_CANCEL;
                CharSequence contentTitle = title;
                CharSequence contentText = message;
                Intent notificationIntent = new Intent(ctx, MAME4droid.class);
                PendingIntent contentIntent = PendingIntent.getActivity(ctx, 0, notificationIntent, 0);

                notification.setLatestEventInfo(ctx, contentTitle, contentText, contentIntent);
                notificationManager.notify(1, notification);
        }
       
        public static void removeNotification()
        {
                if(notificationManager != null)
                        notificationManager.cancel(1);
        }
}

public class MAME4droid extends Activity {

	protected View emuView = null;

	protected InputView inputView = null;
	
	protected FilterView filterView = null;
	
	protected MainHelper mainHelper = null;
	protected MenuHelper menuHelper = null;
	protected PrefsHelper prefsHelper = null;
	protected DialogHelper dialogHelper = null;
	
	protected InputHandler inputHandler = null;
	
	protected FileExplorer fileExplore = null;
	
	protected NetPlay netPlay = null;
	
	// Name of the connected device
	private String mConnectedDeviceName = null;
    private BluetoothAdapter mBluetoothAdapter = null;
    private static BluetoothSerialService mSerialService = null;
    private boolean mEnablingBT;
    private boolean mSdkEnable = true;
    private MenuItem mMenuItemConnect;
    
    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    public static final String TAG = "MotionRing";
    
    // Message types sent from the BluetoothReadService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";
    public static Context mMainMenuActivityContext;
	
	public NetPlay getNetPlay() {
		return netPlay;
	}

	public FileExplorer getFileExplore() {
		return fileExplore;
	}

	public MenuHelper getMenuHelper() {
		return menuHelper;
	}
    	
    public PrefsHelper getPrefsHelper() {
		return prefsHelper;
	}
    
    public MainHelper getMainHelper() {
		return mainHelper;
	}
    
    public DialogHelper getDialogHelper() {
		return dialogHelper;
	}
    
	public View getEmuView() {
		return emuView;
	}
	
	public InputView getInputView() {
		return inputView;
	}

	public FilterView getFilterView() {
		return filterView;
	}
	
    public InputHandler getInputHandler() {
		return inputHandler;
	}
    
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
		Log.d("EMULATOR", "onCreate "+this);
		
		overridePendingTransition(0, 0);
		getWindow().setWindowAnimations(0);
		
		prefsHelper = new PrefsHelper(this);

        dialogHelper  = new DialogHelper(this);
        
        mainHelper = new MainHelper(this);
                             
        fileExplore = new FileExplorer(this);
        
        netPlay = new NetPlay(this);
                
        menuHelper = new MenuHelper(this);
                
        inputHandler = InputHandlerFactory.createInputHandler(this);
        
        mainHelper.detectDevice();
        
        Emulator.setPortraitFull(getPrefsHelper().isPortraitFullscreen());
        boolean full = false;
		if(prefsHelper.isPortraitFullscreen() && mainHelper.getscrOrientation() == Configuration.ORIENTATION_PORTRAIT)
		{
			setContentView(R.layout.main_fullscreen);
			full = true;
		}
		else 
		{
            setContentView(R.layout.main);
		}        
                
        FrameLayout fl = (FrameLayout)this.findViewById(R.id.EmulatorFrame);
        
        
        //Coment to avoid BUG on 2.3.4 (reload instead)
        Emulator.setVideoRenderMode(getPrefsHelper().getVideoRenderMode());
        
        this.getLayoutInflater().inflate(R.layout.netplayview, fl);
        View v = this.findViewById(R.id.netplay_view);
        if(v!=null)
        	v.setVisibility(View.GONE);
        
        if(prefsHelper.getVideoRenderMode()==PrefsHelper.PREF_RENDER_SW)
        {
        	this.getLayoutInflater().inflate(R.layout.emuview_sw, fl);
        	emuView = this.findViewById(R.id.EmulatorViewSW);        
        }
        else 
        { 
        	if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN  && prefsHelper.getNavBarMode()!=PrefsHelper.PREF_NAVBAR_VISIBLE)
        	    this.getLayoutInflater().inflate(R.layout.emuview_gl_ext, fl);
        	else
        		this.getLayoutInflater().inflate(R.layout.emuview_gl, fl);
    		
        	emuView = this.findViewById(R.id.EmulatorViewGL);
        	
        }
        
        if(full && prefsHelper.isPortraitTouchController())
        {
        	FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams )emuView.getLayoutParams();
        	lp.gravity =  Gravity.TOP | Gravity.CENTER;
        }
                       
        inputView = (InputView) this.findViewById(R.id.InputView);
                
        ((IEmuView)emuView).setMAME4droid(this);

        inputView.setMAME4droid(this);
        
        Emulator.setMAME4droid(this);        
            
        View frame = this.findViewById(R.id.EmulatorFrame);
	    frame.setOnTouchListener(inputHandler);        	
        	    
        if((prefsHelper.getPortraitOverlayFilterValue()!=PrefsHelper.PREF_OVERLAY_NONE && mainHelper.getscrOrientation() == Configuration.ORIENTATION_PORTRAIT)
        		||
           (prefsHelper.getLandscapeOverlayFilterValue()!=PrefsHelper.PREF_OVERLAY_NONE && mainHelper.getscrOrientation() == Configuration.ORIENTATION_LANDSCAPE))
        {	
        	String value;
            
            if(mainHelper.getscrOrientation() == Configuration.ORIENTATION_PORTRAIT)
            	value = prefsHelper.getPortraitOverlayFilterValue();
            else
            	value = prefsHelper.getLandscapeOverlayFilterValue();
           
            if(!value.equals(PrefsHelper.PREF_OVERLAY_NONE))
            {
	        	getLayoutInflater().inflate(R.layout.filterview, fl);
	            filterView = (FilterView)this.findViewById(R.id.FilterView);
	            
	            String fileName = getPrefsHelper().getROMsDIR()+File.separator+"overlays"+File.separator+value;
	            
	            Bitmap bmp = BitmapFactory.decodeFile(fileName);
	            BitmapDrawable bitmapDrawable = new BitmapDrawable(bmp);
	            bitmapDrawable.setTileModeXY(Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
	            
	            int alpha = 0;	            
		   		switch(getPrefsHelper().getEffectOverlayIntensity())
			    {
			       case 1: alpha = 25;break;
			       case 2: alpha = 50;break;
			       case 3: alpha = 55;break;
			       case 4: alpha = 60;break;
			       case 5: alpha = 65;break;
			       case 6: alpha = 70;break;
			       case 7: alpha = 75;break;
			       case 8: alpha = 80;break;
			       case 9: alpha = 100;break;
			       case 10: alpha = 125;break;			       
                }
            
	            bitmapDrawable.setAlpha(alpha);
	            filterView.setBackgroundDrawable(bitmapDrawable);
	            
	            //this.getEmuView().setAlpha(250);
		            
	            if(full && prefsHelper.isPortraitTouchController())
	            {
	            	FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams )filterView.getLayoutParams();
	            	lp.gravity =  Gravity.TOP | Gravity.CENTER;
	            }
	            
	            filterView.setMAME4droid(this);
            }
      
        }
                
        inputHandler.setInputListeners();
        
        mainHelper.updateMAME4droid();
               
        if(!Emulator.isEmulating())
        {
			if(prefsHelper.getROMsDIR()==null)
			{	            
				if(DialogHelper.savedDialog==DialogHelper.DIALOG_NONE)
				   showDialog(DialogHelper.DIALOG_ROMs_DIR);                      
			}
			else
			{
				getMainHelper().ensureROMsDir(prefsHelper.getROMsDIR());
				runMAME4droid();	
			}
        }
        
        Log.d("MotionRing", "onCreate");
        mMainMenuActivityContext = this;
        if( null == mBluetoothAdapter )
        {
        	mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        }
        
		if (mBluetoothAdapter == null) {
            finishDialogNoBluetooth();
            Log.d("MotionRing", "bluetoothAdapter == null");
			return;
		}
		else
		{
			Log.d("MotionRing", "bluetoothAdapter != null");
			if( null == mSerialService )
			{
				mSerialService = new BluetoothSerialService(this, mHandlerBT );
			}
		}
    }
        
    public void runMAME4droid(){  	
	    getMainHelper().copyFiles();
	    getMainHelper().removeFiles();
    	Emulator.emulate(mainHelper.getLibDir(),prefsHelper.getROMsDIR());	
    }
     
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		overridePendingTransition(0 , 0);
		this.getMainHelper().updateMAME4droid();
		//this.getMainHelper().reload();
		overridePendingTransition(0 , 0);
	}
	
	public int getConnectionState() {
		return mSerialService.getState();
	}
	
	public void connectMotionRing() {
		Log.d("EMULATOR", "connectMotionRing ");
		if( null == mSerialService)
		{
			Log.d("EMULATOR", "mSerialService is null ");
		} else {
			Log.d("EMULATOR", "mSerialService is not null ");
			if (getConnectionState() == BluetoothSerialService.STATE_NONE) {
	    		// Launch the DeviceListActivity to see devices and do scan
	    		Intent serverIntent = new Intent(this, DeviceListActivity.class);
	    		startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
	    	}
	    	else if (getConnectionState() == BluetoothSerialService.STATE_CONNECTED) {
	    		mSerialService.stop();
	    		mSerialService.start();
	        }
		}
	}

	//MENU STUFF
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {		
		
		if(menuHelper!=null)
		{
		   if(menuHelper.createOptionsMenu(menu))return true;
		}  
		
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if(menuHelper!=null)
		{	
		   if(menuHelper.prepareOptionsMenu(menu)) return true;
		}   
		return super.onPrepareOptionsMenu(menu); 
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if(menuHelper!=null)
		{
		   if(menuHelper.optionsItemSelected(item))
			   return true;
		}
		return super.onOptionsItemSelected(item);
	}

	//ACTIVITY
    @Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		//super.onActivityResult(requestCode, resultCode, data);
    	
    	switch (requestCode) {
        
        case REQUEST_CONNECT_DEVICE:

            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                // Get the device MAC address
                String address = data.getExtras()
                                     .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                // Get the BLuetoothDevice object
                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                // Attempt to connect to the device
                mSerialService.connect(device);                
            }
            break;

        case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
                //Log.d(LOG_TAG, "BT not enabled");
            	Log.d(TAG, "BT enabled");
                
                //finishDialogNoBluetooth();                
            }
        }
    	
		if(mainHelper!=null)
		   mainHelper.activityResult(requestCode, resultCode, data);
	}
	
	//LIVE CYCLE
	@Override
	protected void onResume() {
		Log.d("EMULATOR", "onResume");				
		super.onResume();
		if(prefsHelper!=null)
		   prefsHelper.resume();
				
		if(DialogHelper.savedDialog!=-1)
			showDialog(DialogHelper.savedDialog);
		else if(!ControlCustomizer.isEnabled())
		  Emulator.resume();
		
		if(inputHandler!= null)
		{
			if(inputHandler.getTiltSensor()!=null)
			   inputHandler.getTiltSensor().enable();
		}
		
		NotificationHelper.removeNotification();
		//System.out.println("OnResume");
		
		if (!mEnablingBT) { // If we are turning on the BT we cannot check if it's enable
		    if ( (mBluetoothAdapter != null)  && (!mBluetoothAdapter.isEnabled()) ) {
			
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(R.string.alert_dialog_turn_on_bt)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(R.string.alert_dialog_warning_title)
                    .setCancelable( false )
                    .setPositiveButton(R.string.alert_dialog_yes, new DialogInterface.OnClickListener() {
                    	public void onClick(DialogInterface dialog, int id) {
                    		mEnablingBT = true;
                    		Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    		startActivityForResult(enableIntent, REQUEST_ENABLE_BT);			
                    	}
                    })
                    .setNegativeButton(R.string.alert_dialog_no, new DialogInterface.OnClickListener() {
                    	public void onClick(DialogInterface dialog, int id) {
                    		finishDialogNoBluetooth();            	
                    	}
                    });
                AlertDialog alert = builder.create();
                alert.show();
		    }		
		
		    if (mSerialService != null) {
		    	// Only if the state is STATE_NONE, do we know that we haven't started already
		    	if (mSerialService.getState() == BluetoothSerialService.STATE_NONE) {
		    		// Start the Bluetooth chat services
		    		//mSerialService.start();
		    		Log.d(TAG, "!!!! mSerialService Start !!!!");
		    	}
		    }

		    if (mBluetoothAdapter != null) {
		    	mSerialService.sdkEnable(mSdkEnable);
		    	Log.d(TAG, "!!!! SDK ENABLE !!!!");
		    }
		}
	}
	
private final Handler mHandlerBT = new Handler() {
    	
        @Override
        public void handleMessage(Message msg) {        	
            switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                switch (msg.arg1) {
                case BluetoothSerialService.STATE_CONNECTED:
                	if (mMenuItemConnect != null) {
                		mMenuItemConnect.setIcon(android.R.drawable.ic_menu_close_clear_cancel);
                		mMenuItemConnect.setTitle(R.string.disconnect);
                	}
                	
                	//mInputManager.showSoftInput(mEmulatorView, InputMethodManager.SHOW_IMPLICIT);
                	
                    //mTitle.setText(R.string.title_connected_to);
                    //mTitle.append(mConnectedDeviceName);
                    break;
                    
                case BluetoothSerialService.STATE_CONNECTING:
                    //mTitle.setText(R.string.title_connecting);
                    break;
                    
                case BluetoothSerialService.STATE_LISTEN:
                case BluetoothSerialService.STATE_NONE:
                	if (mMenuItemConnect != null) {
                		mMenuItemConnect.setIcon(android.R.drawable.ic_menu_search);
                		mMenuItemConnect.setTitle(R.string.connect);
                	}

            		//mInputManager.hideSoftInputFromWindow(mEmulatorView.getWindowToken(), 0);
                	
                    //mTitle.setText(R.string.title_not_connected);

                    break;
                }
                break;
            case MESSAGE_WRITE:
            	/*if (mLocalEcho) {
            		byte[] writeBuf = (byte[]) msg.obj;
            		mEmulatorView.write(writeBuf, msg.arg1);
            	}*/
                
                break;
/*                
            case MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;              
                mEmulatorView.write(readBuf, msg.arg1);
                
                break;
*/                
            case MESSAGE_DEVICE_NAME:
                // save the connected device's name
                mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                Toast.makeText(getApplicationContext(), "Connected to "
                               + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                break;
            case MESSAGE_TOAST:
                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                               Toast.LENGTH_SHORT).show();
                break;
            }
        }
    };
		
	public void finishDialogNoBluetooth() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.alert_dialog_no_bt)
        .setIcon(android.R.drawable.ic_dialog_info)
        .setTitle(R.string.app_name)
        .setCancelable( false )
        .setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                       finish();            	
                	   }
               });
        AlertDialog alert = builder.create();
        alert.show(); 
    }
	
	@Override
	protected void onPause() {
		Log.d("EMULATOR", "onPause");
		super.onPause();
		if(prefsHelper!=null)
		   prefsHelper.pause();
		if(!ControlCustomizer.isEnabled())		
		   Emulator.pause();
		if(inputHandler!= null)
		{
			if(inputHandler.getTiltSensor()!=null)
			   inputHandler.getTiltSensor().disable();
		}	
		
		if(dialogHelper!=null)
		{
			dialogHelper.removeDialogs();
		}
		
		if(prefsHelper.isNotifyWhenSuspend()) 
		  NotificationHelper.addNotification(getApplicationContext(), "MAME4droid was suspended!", "MAME4droid was suspended", "Press to return to MAME4droid");
		
		//System.out.println("OnPause");
	}
	
	@Override
	protected void onStart() {
		Log.d("EMULATOR", "onStart");		
		super.onStart();
		mEnablingBT = false;
		try{InputHandlerExt.resetAutodetected();}catch(Error e){};		
		//System.out.println("OnStart");
	}

	@Override
	protected void onStop() {
		Log.d("EMULATOR", "onStop");
		super.onStop();
		//System.out.println("OnStop");
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		Log.d("EMULATOR", "onNewIntent");
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.d("EMULATOR", "onDestroy "+this);
				
        View frame = this.findViewById(R.id.EmulatorFrame);
	    if(frame!=null)
           frame.setOnTouchListener(null); 
	    
		if(inputHandler!= null)
		{
		   inputHandler.unsetInputListeners();
		   
			if(inputHandler.getTiltSensor()!=null)
				   inputHandler.getTiltSensor().disable();
		}
			
        if(emuView!=null)
		   ((IEmuView)emuView).setMAME4droid(null);
        
        /*if (mSerialService != null)
        	mSerialService.stop();*/

        /*
        if(inputView!=null)
           inputView.setMAME4droid(null);
        
        if(filterView!=null)
           filterView.setMAME4droid(null);
                       
        prefsHelper = null;
        
        dialogHelper = null;
        
        mainHelper = null;
        
        fileExplore = null;
        
        menuHelper = null;
        
        inputHandler = null;
        
        inputView = null;
        
        emuView = null;
        
        filterView = null; */     	    
	}	
		

	//Dialog Stuff
	@Override
	protected Dialog onCreateDialog(int id) {

		if(dialogHelper!=null)
		{	
			Dialog d = dialogHelper.createDialog(id);
			if(d!=null)return d;
		}
		return super.onCreateDialog(id);		
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		if(dialogHelper!=null)
		   dialogHelper.prepareDialog(id, dialog);
	}
        
}