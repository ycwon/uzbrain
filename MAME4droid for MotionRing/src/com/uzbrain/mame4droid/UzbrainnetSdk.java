package com.uzbrain.mame4droid;


import java.util.LinkedList;

import android.app.Instrumentation;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;

import android.widget.Toast;

public class UzbrainnetSdk {
	
	// Debugging
    private static final String TAG = "UzbrainnetSdk";
    private static final boolean DBG = true;
    
    private static final int NEXUS7 = 0;
    private static final int G_PLATE = 1;
    private static final int buildOption = G_PLATE;	// NEXUS7, G_PLATE
    
    private static final int ADD = 1;
    private static final int POLL = 2;
    private static final int SIZE = 3;
    
    // class object
   // private static UzbrainnetParser mUzbrainnetParser = new UzbrainnetParser();//null;
    private static UzbrainnetNative mUzbrainnetNative = new UzbrainnetNative();//null;
    
    private StringBuffer mReceivedData = new StringBuffer();
    private LinkedList<String> mReceivedDataList = new LinkedList<String>();//null;
    private String mReceivedString = "";
    private String mOneLine = "sdkOneLine";
    private String mParsedLine = "sdkParsedLine";
    private String dataProcessingThreadName = "";
    
    private final Handler mHandler;
    
    
    public static Object key = new Object();
    
    private DataProcessingThread mDataProcessingThread = null;//new DataProcessingThread(mReceivedDataList);
    private KeyEventGenerateThread mKeyEventGenerateThread = null;

    
    private byte[] receiveBuffer = new byte[1024];
    
    
    public UzbrainnetSdk(Handler handler) {
        mHandler = handler;
    }
    
    
	public synchronized void start() {
    	
    	if (DBG)
    		Log.d(TAG, "!!!! UzbrainnetSdk Start !!!!");

    	if( mDataProcessingThread == null )
    	{
    		mDataProcessingThread = new DataProcessingThread();
    		mDataProcessingThread.start();
    		dataProcessingThreadName = mDataProcessingThread.getName();
    	}
    	
    	if( mKeyEventGenerateThread == null )
    	{
    		mKeyEventGenerateThread = new KeyEventGenerateThread();
    		mKeyEventGenerateThread.start();
    	}
    	
    	mUzbrainnetNative.initialize();
    	
    }
    
    public synchronized void stop() {
    	
    	if (DBG)
    		Log.d(TAG, "!!!! UzbrainnetSdk Stop !!!!");
    	
    	if( mDataProcessingThread != null )
    	{
    		mDataProcessingThread.stopThread();
    		mDataProcessingThread = null;
    	}
    	
    	if( mKeyEventGenerateThread != null )
    	{
    		mKeyEventGenerateThread.stopThread();
    		mKeyEventGenerateThread = null;
    	}
    }
    
    public synchronized void write(byte[] buffer, int length) {
    	
    		System.arraycopy(buffer, 0, receiveBuffer, 0, length);
            
        	String temp = new String(receiveBuffer, 0, length);
        	
        	int indexSOF;
        	int indexEOF;
        	
        	mReceivedData.append(temp);
            
            if( mReceivedData.length() > 79 )
        	{
        	   	while( mReceivedData.length() > 79 )
	        	{
	        		mReceivedString = mReceivedData.substring(0, 79);
	        		indexSOF = mReceivedString.indexOf("SOF");
	        		indexEOF = mReceivedString.indexOf("EOF");
	        		
	        		if( (indexSOF == 0) && (indexEOF == 74) )
	        		{
	        			mOneLine = mReceivedString;
	        			mReceivedData.delete(0, 79);
	        			accessReceivedDataList(ADD, mOneLine);
		        		mOneLine = "";
		        		//Log.d(TAG, "add oneLine");
	        		}
	        		else
	        		{
	        			if( (indexSOF == -1) || (indexSOF == 0))
	        			{
	        				//skip this phase
	        				mReceivedData.delete(0,  79);
	        				mOneLine = "";
	        			}
	        			else
	        			{
	        				mReceivedData.delete(0, indexSOF);
	        				mOneLine = "";
	        			}
	        		}
        		}
        	}
            temp = null;
    }
    
    private synchronized void oneLineParsing(String data)
    {
    	mUzbrainnetNative.parsingData(data);
    }
    
    private synchronized void accessReceivedDataList(int method, String addData)
    {
    	//Log.d(TAG, "dataProcessingThreadName: " + dataProcessingThreadName + "currentThreadName: " + Thread.currentThread().getName());
    	String pollData = "";
    	try {
    		if( dataProcessingThreadName == Thread.currentThread().getName())
    		{
    			
    			wait();
    		}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	switch(method)
    	{
    		case ADD:
    			
    			mReceivedDataList.add(addData);
    			//Log.d(TAG, "add list" + addData);
    			
    			break;
    			
    		case POLL:
    			
    			/*pollData = mReceivedDataList.poll();
    			if( null == pollData )
    			{
    				//Log.d(TAG, "poll data null: " + pollData +"size: " + mReceivedDataList.size());
    			}
    			else
    			{
    				//while( 2 < mReceivedDataList.size() )
    				//{
    					oneLineParsing(pollData);
        				Log.d(TAG, "poll data null: " + pollData + "size: " + mReceivedDataList.size());
        				//Log.d(TAG, "parsed data: " + mParsedLine);
    				//}
    				
    			}*/
    			
    			while( 1 < mReceivedDataList.size())
    			{
    				pollData = mReceivedDataList.poll();
    				oneLineParsing(pollData);
    				//Log.d(TAG, "parsed data: " + mParsedLine);
    			}
    			
    			break;
    	}
    	notifyAll();
    }
    
    private class DataProcessingThread extends Thread {
    	
    	private boolean threadRunning = true;
    	
		public void run() {
			// TODO Auto-generated method stub
			super.run();
			
				//Log.d(TAG, "!!!! DataProcessingThread !!!!");
				
				while( threadRunning )
				{
					try {
							//Log.d(TAG, "!!!! DataProcessingThread !!!!");
							accessReceivedDataList(POLL, null);
							
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						break;
					}
				}
		}
		
		public void stopThread() {
			
			threadRunning = false;
		}
    }
    
    private class KeyEventGenerateThread extends Thread {
    	
    	private boolean threadRunning = true;
    	Instrumentation mInst = new Instrumentation();

    	private int axValue = 0;
    	private int ayValue = 0;

    	private boolean moveLeftFlag = false;
    	private boolean moveRightFlag = false;
    	private boolean moveUpFlag = false;
    	private boolean moveDownFlag = false;
    	
    	private boolean ButtonBFlag = false;
    	private boolean ButtonAFlag = false;
    	private boolean ButtonSelectFlag = false;
    	private boolean ButtonStartFlag = false;
    	private boolean ButtonPowerFlag = false;
    	private boolean activateMotionRingFlag = true;

    	KeyEvent eventDownBack = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK);
    	KeyEvent eventUpBack = new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK);
    	
    	KeyEvent eventDownMoveLeft = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT);
    	KeyEvent eventUpMoveLeft = new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_LEFT);
    	KeyEvent eventDownMoveRight = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT);
    	KeyEvent eventUpMoveRight = new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_RIGHT);
    	KeyEvent eventDownMoveUp = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_UP);
    	KeyEvent eventUpMoveUp = new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_UP);
    	KeyEvent eventDownMoveDown = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_DOWN);
    	KeyEvent eventUpMoveDown = new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_DOWN);
    	
    	KeyEvent eventDownButtonB = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_B);
    	KeyEvent eventUpButtonB = new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BUTTON_B);
    	KeyEvent eventDownButtonA = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_A);
    	KeyEvent eventUpButtonA = new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BUTTON_A);
    	KeyEvent eventDownStart = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_START);
    	KeyEvent eventUpStart = new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BUTTON_START);
    	KeyEvent eventDownCoin = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_SELECT);
    	KeyEvent eventUpCoin = new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BUTTON_SELECT);
    	
		public void run() {
			// TODO Auto-generated method stub
			super.run();
			
				//Log.d(TAG, "!!!! DataProcessingThread !!!!");
				
				while( threadRunning )
				{
					try {
							/*Log.d(TAG, mUzbrainnetNative.getAxValue() + "	" + mUzbrainnetParser.getAyValue() + "	" + mUzbrainnetParser.getAzValue() +"	"
									+ mUzbrainnetParser.getGxValue() + "	" + mUzbrainnetParser.getGyValue() + "	" + mUzbrainnetParser.getGzValue());*/
						
							axValue = mUzbrainnetNative.getAxValue();
							ayValue = mUzbrainnetNative.getAyValue();
							
							if( 1 == mUzbrainnetNative.getPowerButton() )
							{
								if( false == ButtonPowerFlag )
								{
									mInst.sendKeySync( eventDownBack );
									
									ButtonPowerFlag = true;
								}
							}
							else
							{
								if( true == ButtonPowerFlag )
								{
									mInst.sendKeySync( eventUpBack );
									ButtonPowerFlag = false;
								}
							}
							
							if( (0 == mUzbrainnetNative.getModeButton()) && (true == activateMotionRingFlag) )
							{
							
								if( -3 > axValue) //기존 -2
					        	{
					        		if( false == moveLeftFlag)
					        		{
					        			mInst.sendKeySync( eventDownMoveLeft );
					        			moveLeftFlag = true;
					        			Log.d(TAG, "!!!! eventDownMoveLeft !!!!");
					        		}
					        		if( true == moveRightFlag)
					        		{
					        			mInst.sendKeySync( eventUpMoveRight );
					        			moveRightFlag = false;
					        			Log.d(TAG, "!!!! eventUpMoveRight !!!!");
					        		}
					        	}
					        	else if( 3 < axValue)//기존 2
					        	{
					        		if( false == moveRightFlag )
					        		{
					        			mInst.sendKeySync( eventDownMoveRight );
					        			moveRightFlag = true;
					        			Log.d(TAG, "!!!! eventDownMoveRight !!!!");
					        		}
					        		if( true == moveLeftFlag)
					        		{
					        			mInst.sendKeySync( eventUpMoveLeft );
					        			moveLeftFlag = false;
					        			Log.d(TAG, "!!!! eventUpMoveLeft !!!!");
					        		}
					        	}
					        	else if( (-3 <= axValue) && (3 >= axValue) )
					        	{
					        		if( true == moveLeftFlag)
					        		{
					        			mInst.sendKeySync( eventUpMoveLeft );
					        			moveLeftFlag = false;
					        			Log.d(TAG, "!!!! eventUpMoveLeft !!!!");
					        		}
					        		if( true == moveRightFlag)
					        		{
					        			mInst.sendKeySync( eventUpMoveRight );
					        			moveRightFlag = false;
					        			Log.d(TAG, "!!!! eventUpMoveRight !!!!");
					        		}
					        	}
								
								if( 3 < ayValue)// 기존 -2
					        	{
					        		if( false == moveDownFlag)
					        		{
					        			mInst.sendKeySync( eventDownMoveDown );
					        			moveDownFlag = true;
					        								        		}
					        		if( true == moveUpFlag)
					        		{
					        			mInst.sendKeySync( eventUpMoveUp );
					        			moveUpFlag = false;
					        		}
					        	}
					        	else if( -3 > ayValue)
					        	{
					        		if( false == moveUpFlag )
					        		{
					        			mInst.sendKeySync( eventDownMoveUp );
					        			moveUpFlag = true;
					        		}
					        		if( true == moveDownFlag)
					        		{
					        			mInst.sendKeySync( eventUpMoveDown );
					        			moveDownFlag = false;
					        		}
					        	}
					        	else if( (-3 <= ayValue) && (3 >= ayValue) )
					        	{
					        		if( true == moveUpFlag)
					        		{
					        			mInst.sendKeySync( eventUpMoveUp );
					        			moveUpFlag = false;
					        		}
					        		if( true == moveDownFlag)
					        		{
					        			mInst.sendKeySync( eventUpMoveDown );
					        			moveDownFlag = false;
					        		}
					        	}
								
								if( 1 == mUzbrainnetNative.getLeftButton() )
						        {
									if( false == ButtonBFlag )
					        		{
					        			mInst.sendKeySync( eventDownButtonB );
					        			ButtonBFlag = true;
					        			Log.d(TAG, "!!!! eventDownButtonB !!!!");
					        		}
						        }
					        	if( 0 == mUzbrainnetNative.getLeftButton() )
						        {
					        		if( true == ButtonBFlag )
					        		{
					        			mInst.sendKeySync( eventUpButtonB );
					        			ButtonBFlag = false;
					        			Log.d(TAG, "!!!! eventUpButtonB !!!!");
					        		}
						        }
					        	
					        	if( 1 == mUzbrainnetNative.getRightButton() )
						        {
					        		if( false == ButtonAFlag )
					        		{
					        			//mInst.sendKeySync( eventDownButtonA );
					        			mInst.sendKeySync( eventDownButtonA );
					        			ButtonAFlag = true;
					        			Log.d(TAG, "!!!! eventDownButtonA !!!!");
					        		}
						        }
					        	if( 0 == mUzbrainnetNative.getRightButton() )
						        {
					        		if( true == ButtonAFlag )
					        		{
					        			//mInst.sendKeySync( eventUpButtonA );
					        			mInst.sendKeySync( eventUpButtonA );
					        			ButtonAFlag = false;
					        			Log.d(TAG, "!!!! eventUpButtonA !!!!");
					        		}
						        }
							}
							else if( (1 == mUzbrainnetNative.getModeButton()) && (true == activateMotionRingFlag) )
							{
								if( 1 == mUzbrainnetNative.getLeftButton() )
						        {
									if( false == ButtonSelectFlag )
					        		{
					        			mInst.sendKeySync( eventDownCoin );
					        			ButtonSelectFlag = true;
					        			Log.d(TAG, "!!!! eventDownCoin !!!!");
					        		}
						        }
					        	if( 0 == mUzbrainnetNative.getLeftButton() )
						        {
					        		if( true == ButtonSelectFlag )
					        		{
					        			mInst.sendKeySync( eventUpCoin );
					        			ButtonSelectFlag = false;
					        			Log.d(TAG, "!!!! eventUpCoin !!!!");
					        		}
						        }
					        	
					        	if( 1 == mUzbrainnetNative.getRightButton() )
						        {
					        		if( false == ButtonStartFlag )
					        		{
					        			mInst.sendKeySync( eventDownStart );
					        			ButtonStartFlag = true;
					        			Log.d(TAG, "!!!! eventDownStart !!!!");
					        		}
						        }
					        	if( 0 == mUzbrainnetNative.getRightButton() )
						        {
					        		if( true == ButtonStartFlag )
					        		{
					        			mInst.sendKeySync( eventUpStart );
					        			ButtonStartFlag = false;
					        			Log.d(TAG, "!!!! eventUpStart !!!!");
					        		}
						        }
							}

			        		sleep(4);
							
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						break;
					}
				}
		}
		
public void stopThread() {
			
			threadRunning = false;
		}
    }
}
