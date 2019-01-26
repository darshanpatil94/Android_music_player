package com.example.musicplayer;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.media.MediaControlIntent;
import android.support.v7.media.RemotePlaybackClient;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RemoteControlClient;
import android.net.Uri;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;

import com.example.musicplayer.R;
import com.example.musicplayer.MusicService.MusicBinder;

import android.widget.ListView;
import android.widget.Toast;
import android.widget.MediaController.MediaPlayerControl;

public class MainActivity extends ActionBarActivity implements MediaPlayerControl,SensorEventListener
{
	private ArrayList<Song> songList;
	private ListView songView;
	private MusicService musicSrv;
	public MediaPlayer mplayer;
	private Intent playIntent;
	private boolean musicBound=false;
	private MusicController controller;
	private boolean paused=true, playbackPaused=true;
	private boolean shuffle=false;
	AudioManager maudioManager;
	private int vol,ch=0,ch1=0;
    private MusicService mPlayer;
    private SensorManager sensorManager;
    boolean flag = false;
    private int count=0;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
    	try
    	{
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        songView = (ListView)findViewById(R.id.song_list);
        songList = new ArrayList<Song>();
        getSongList();

        Collections.sort(songList, new Comparator<Song>()      //sort the songs
        	{
        	  public int compare(Song a, Song b)
        	  {
        	    return a.getTitle().compareTo(b.getTitle());
        	  }
        	});

        SongAdapter songAdt = new SongAdapter(this, songList);
        songView.setAdapter(songAdt);
        setController();
        maudioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);

      //create instance of sensor manager and get system service to interact with Sensor
      		sensorManager= (SensorManager)getSystemService(Context.SENSOR_SERVICE);
      		Sensor proximitySensor= sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
      		if (proximitySensor == null){
      			Toast.makeText(MainActivity.this,"No Proximity Sensor Found! ",Toast.LENGTH_LONG).show();
      		}
    	}catch(Exception e)
    	{
    		Log.e("","Error: ", e);
    	}
    }

    private void setController()    //set the controller up
    {
    	controller = new MusicController(this);   
    	controller.setPrevNextListeners(new View.OnClickListener() 
    	{
    		  @Override
    		  public void onClick(View v) 
    		  {
    		    playNext();
    		  }
    		}, new View.OnClickListener() 
    		{
    		  @Override
    		  public void onClick(View v) 
    		  {
    		    playPrev();
    		  }
    		});
    	controller.setMediaPlayer(this);
    	controller.setAnchorView(findViewById(R.id.song_list));
    	controller.setEnabled(true);
    	
    }
    
    @Override
    protected void onStart() 
    {
    	try
    	{
         
	  super.onStart();
	  
      if(playIntent==null)
      {
        playIntent = new Intent(this, MusicService.class);
        bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
        startService(playIntent);
       
      } 
    	}catch(Exception e)
    	{
    		Log.e("","Error: ", e);
    	}					
    }

    @Override
    protected void onPause()
    {
      super.onPause();
      sensorManager.unregisterListener(this);
      paused=true;
    }
    
    @Override
    protected void onResume()
    {
      super.onResume();
   // register this class as a listener for the Proximity Sensor
   		sensorManager.registerListener(this,
   				sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY),
   				SensorManager.SENSOR_DELAY_NORMAL);
   		
      //setVolume();
      if(paused)
      {
        setController();
        paused=false; 
      }
    }
    
    @Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	// called when sensor value have changed
	@Override
	public void onSensorChanged(SensorEvent event) {
		// The Proximity sensor returns a single value either 0 or 5(also 1 depends on Sensor manufacturer).
		// 0 for near and 5 for far 
		int i;
		if(event.sensor.getType()==Sensor.TYPE_PROXIMITY)
		{		
			if(event.values[0]==0)
			{
				bltmusic("3");
			}
			else
			{
				//tVProximity.setText("You are Far: "+String.valueOf(event.values[0]));
			}
			
		}
		}
    
    @Override
    protected void onStop() 
    {
       //mMediaRouter.removeCallback(mMediaRouterCallback);
       controller.hide();
       super.onStop();
       
    } 
    
  @Override
	public void onBackPressed() 
    {
	  super.onBackPressed();
	  //controller.hide();
	}

	//connect to the service
    private ServiceConnection musicConnection = new ServiceConnection()
    { 
      @Override
      public void onServiceConnected(ComponentName name, IBinder service)
      {
        MusicBinder binder = (MusicBinder)service;
        //get service
        musicSrv = binder.getService();
        //pass list
        musicSrv.setList(songList);
        musicBound = true;
      } 
      @Override
      public void onServiceDisconnected(ComponentName name) 
      {
        musicBound = false;
      }
    };

    public void getSongList()
    {
    	  //retrieve song info
    	ContentResolver musicResolver = getContentResolver();
    	Uri musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
    	Cursor musicCursor = musicResolver.query(musicUri, null, null, null, null);
    	if(musicCursor!=null && musicCursor.moveToFirst())
    	{
    		  //get columns
    		  int titleColumn = musicCursor.getColumnIndex
    		    (android.provider.MediaStore.Audio.Media.TITLE);
    		  int idColumn = musicCursor.getColumnIndex
    		    (android.provider.MediaStore.Audio.Media._ID);
    		  int artistColumn = musicCursor.getColumnIndex
    		    (android.provider.MediaStore.Audio.Media.ARTIST);
    		  //add songs to list
    		  do 
    		  {
    		    long thisId = musicCursor.getLong(idColumn);
    		    String thisTitle = musicCursor.getString(titleColumn);
    		    String thisArtist = musicCursor.getString(artistColumn);
    		    songList.add(new Song(thisId, thisTitle, thisArtist));
    		  }while (musicCursor.moveToNext());
    	}
    	else
    	{
    	}
    }
 
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {       
        // Inflate the menu; this adds items to the action bar if it is present.
    	super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) 
    {
    	switch (item.getItemId()) 
    	{
    	case R.id.action_shuffle:
    		  musicSrv.setShuffle();
    		  ch=0;
    		  invalidateOptionsMenu();
    		  break;

    	case R.id.volume_up:
    		maudioManager.adjustVolume(AudioManager.ADJUST_RAISE, vol);
    		break;
    		
    	case R.id.volume_down:
    		maudioManager.adjustVolume(AudioManager.ADJUST_LOWER, vol);
    		break;
    		
    	case R.id.show_controller:
    		controller.show(0);
    		break;
    		
    	case R.id.action_bluetooth:
    		stopService(playIntent);
            musicSrv=null;
    		Intent intent2 = new Intent(this, Bltmain.class);
            startActivity(intent2);           
    	      break;
    	      	    	      
    	case R.id.action_about:	
    		Intent intent1 = new Intent(this, About.class);
            startActivity(intent1);
    		break;
    	      
    	case R.id.action_end:
    	    stopService(playIntent);
    	    musicSrv=null;      
    	   finish(); 
    	  break;
    	}
    	
    	return super.onOptionsItemSelected(item);   //menu item selected
    }
   
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) 
    { 
    	MenuItem mi = menu.findItem(R.id.action_shuffle);
    	if(ch==0)
    	{
        if(shuffle)
        {   
            mi.setIcon(R.drawable.shuffleon);
            ch=1;
            shuffle = false;
        }
        else
        {
        	 mi.setIcon(R.drawable.shuffleoff);
        	 ch=1;
            shuffle = true;
        }
    	}
        return super.onPrepareOptionsMenu(menu);
    }
    
    
    @Override
    protected void onDestroy()
    {
      stopService(playIntent);
      musicSrv=null;
      super.onDestroy();
    }
 
    public void songPicked(View view)
    {
    	  musicSrv.setSong(Integer.parseInt(view.getTag().toString()));
    	  musicSrv.playSong();
    	  if(playbackPaused)
    	  {
    	    setController();
    	    playbackPaused=false;
    	  }
    	  else
    	  controller.show(0);
    	}
    
    public void bltmusic(String msg)
    {	 
    		ch1= Integer.parseInt(msg);
    		switch(ch1)
    		{
    		case 1: 
    			start();
    			break;
    		case 2:
    			pause();
    			break;
    		case 3:
    			playNext();
    			break;
    		case 4:
    			playPrev();
    			break;
    		case 5:
    			//Voume up
    			maudioManager.adjustVolume(AudioManager.ADJUST_RAISE, vol);
    			break;
    		case 6:
    			//Volume down
    			maudioManager.adjustVolume(AudioManager.ADJUST_LOWER, vol);
    			break;
    			
    		default: break;
    		}
    	}
    
  //play next
    public void playNext()
    {
    	  musicSrv.playNext();
    	  if(playbackPaused)
    	  {
    	    setController();
    	    playbackPaused=false;
    	  }
    	  controller.show(0);
    	}
     
    //play previous
    public void playPrev()
    {
    	  musicSrv.playPrev();
    	  if(playbackPaused)
    	  {
    	    setController();
    	    playbackPaused=false;
    	  }
    	  controller.show(0);
    	}
    
    @Override
    public boolean canPause()
    {
      return true;
    }

    @Override
    public boolean canSeekBackward() 
    {
      return true;
    }
     
    @Override
    public boolean canSeekForward() 
    {
      return true;
    }

	@Override
	public int getAudioSessionId() 
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getBufferPercentage() 
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getCurrentPosition() 
	{
	  if(musicSrv!=null && musicBound && musicSrv.isPng())
	    return musicSrv.getPosn();
	  else 
		  return 0;
	}

	@Override
	public int getDuration() 
	{
	  if(musicSrv!=null && musicBound && musicSrv.isPng())
	    return musicSrv.getDur();
	  else 
		  return 0;
	}

	@Override
	public boolean isPlaying() 
	{
	  if(musicSrv!=null && musicBound)
	    return musicSrv.isPng();
	  return false;
	}

	@Override
	public void pause()
	{
	  playbackPaused=true;
	  musicSrv.pausePlayer();
	}
	 
	@Override
	public void seekTo(int pos) 
	{
	  musicSrv.seek(pos);
	}
	 
	@Override
	public void start() 
	{
	  musicSrv.go();
	}

/////////////////////////////////////////////////////////
		
}
