package com.example.musicplayer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class Startup extends Activity
{
	@Override
    protected void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.startup);
       Thread logoTimer=new Thread(){
        	public void run(){
        		try{
        		    int Timer=0;
        			while(Timer<3200)
        			{
        				sleep(100);
        				Timer=Timer+100;
        			}
        			startActivity(new Intent("com.example.musicplayer.MAIN"));
        		}catch(InterruptedException e)
        		{
        			return;
        		}
        		finally
        		{
        			finish();
        		}
        	}
        };
        logoTimer.start();
    }

	@Override
	protected void onDestroy() 
	{
		super.onDestroy();
	}
}
