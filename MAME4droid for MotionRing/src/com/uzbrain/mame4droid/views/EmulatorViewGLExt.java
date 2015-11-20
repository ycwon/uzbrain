package com.uzbrain.mame4droid.views; 

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import com.uzbrain.mame4droid.MAME4droid;
import com.uzbrain.mame4droid.helpers.DialogHelper;
import com.uzbrain.mame4droid.helpers.PrefsHelper;
import com.uzbrain.mame4droid.input.InputHandler;
import com.uzbrain.mame4droid.views.EmulatorViewGL;

public class EmulatorViewGLExt extends EmulatorViewGL implements  android.view.View.OnSystemUiVisibilityChangeListener  {
	
	protected int mLastSystemUiVis;
	
	private boolean volumeChanges = false;
	
	public void setMAME4droid(MAME4droid mm) {
		
		if(mm==null)
		{
			setOnSystemUiVisibilityChangeListener(null);
			return;
		}
		super.setMAME4droid(mm);		
        setNavVisibility(true);        
        setOnSystemUiVisibilityChangeListener(this);	
	}

	public EmulatorViewGLExt(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
    Runnable mNavHider = new Runnable() {
        @Override public void run() {
        	volumeChanges = false;
            setNavVisibility(false);
        }
    };
    
    @Override protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);

        System.out.println("onWindowVisibilityChanged");
        
        // When we become visible, we show our navigation elements briefly
        // before hiding them.        
        if(mm.getPrefsHelper().getNavBarMode()==PrefsHelper.PREF_NAVBAR_IMMERSIVE)
        {
           setNavVisibility(false);
           //getHandler().postDelayed(mNavHider, 2000);
        }
        else
           getHandler().postDelayed(mNavHider, 3000);	
    }
    
	@Override
	public void onSystemUiVisibilityChange(int visibility) {
		// Detect when we go out of low-profile mode, to also go out
        // of full screen.  We only do this when the low profile mode
        // is changing from its last state, and turning off.
		
		System.out.println("onSystemUiVisibilityChange");
		if((visibility & SYSTEM_UI_FLAG_HIDE_NAVIGATION) == SYSTEM_UI_FLAG_HIDE_NAVIGATION)
		   System.out.println("SYSTEM_UI_FLAG_HIDE_NAVIGATION");
		else
		   System.out.println("NO SYSTEM_UI_FLAG_HIDE_NAVIGATION");
		if((visibility & SYSTEM_UI_FLAG_FULLSCREEN) == SYSTEM_UI_FLAG_FULLSCREEN)
			   System.out.println("SYSTEM_UI_FLAG_FULLSCREEN");
		else
			System.out.println("NO SYSTEM_UI_FLAG_FULLSCREEN");
		
			
        int diff = mLastSystemUiVis ^ visibility;
        mLastSystemUiVis = visibility;
                       
        if ((diff&SYSTEM_UI_FLAG_HIDE_NAVIGATION) != 0
                && (visibility&SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0) {                    
        	   setNavVisibility(true);
                        
        	if(DialogHelper.savedDialog == DialogHelper.DIALOG_NONE && mm.getPrefsHelper().getNavBarMode()!=PrefsHelper.PREF_NAVBAR_IMMERSIVE && !volumeChanges)
               mm.showDialog(DialogHelper.DIALOG_FULLSCREEN);
        }
        else  if ((diff&SYSTEM_UI_FLAG_LOW_PROFILE) != 0
                && (visibility&SYSTEM_UI_FLAG_LOW_PROFILE) == 0) {
            setNavVisibility(true);
        }
        
	}
	
	void setNavVisibility(boolean visible) {
    	if(mm==null)return;
        int newVis = 0;
        boolean full = mm.getInputHandler().getInputHandlerState() == InputHandler.STATE_SHOWING_NONE;
        
        if(full || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && mm.getPrefsHelper().getNavBarMode()==PrefsHelper.PREF_NAVBAR_IMMERSIVE))
        {
        	newVis = SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | SYSTEM_UI_FLAG_LAYOUT_STABLE;
        }
        else
        {
        	newVis = SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | SYSTEM_UI_FLAG_LAYOUT_STABLE;
        }
        
        if (!visible) 
        {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT  && mm.getPrefsHelper().getNavBarMode()==PrefsHelper.PREF_NAVBAR_IMMERSIVE)
            {
                newVis |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                       | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                       | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;	
            }        	
            else if(full)
        	{
                newVis |= SYSTEM_UI_FLAG_LOW_PROFILE | SYSTEM_UI_FLAG_FULLSCREEN
                       | SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        	}
        	else
        	{
        		newVis |= SYSTEM_UI_FLAG_LOW_PROFILE | SYSTEM_UI_FLAG_FULLSCREEN;
        	}        	
        }
        
        // If we are now visible, schedule a timer for us to go invisible.
        if (visible) {
            Handler h = getHandler();
            if (h != null) {
                h.removeCallbacks(mNavHider);
                h.postDelayed(mNavHider, mm.getPrefsHelper().getNavBarMode()==PrefsHelper.PREF_NAVBAR_IMMERSIVE ? 1000 : 3000);               
            }
        }

        // Set the new desired visibility.        
        setSystemUiVisibility(newVis);
    }

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		if(event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN ||  event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP)
		{
			volumeChanges = true;
			
			Handler h = getHandler();
            if (h != null) {
                h.removeCallbacks(mNavHider);
                h.postDelayed(mNavHider, 4000);               
            }
		}
		return super.dispatchKeyEvent(event);
	}

	@Override
	public void onWindowFocusChanged(boolean hasWindowFocus) {
		
		//System.out.println("onWindowFocusChanged:"+hasWindowFocus);
		if(hasWindowFocus)		
	        if(mm.getPrefsHelper().getNavBarMode()==PrefsHelper.PREF_NAVBAR_IMMERSIVE)
	        {
	           setNavVisibility(false);
	           //getHandler().postDelayed(mNavHider, 2000);
	        }
	        else
	           getHandler().postDelayed(mNavHider, 3000);	
				
		super.onWindowFocusChanged(hasWindowFocus);
	}	
    
  
}
