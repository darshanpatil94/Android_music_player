package com.example.musicplayer;
import android.support.v7.app.ActionBarActivity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
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
import android.net.Uri;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ContentResolver;
import android.database.Cursor;
import android.graphics.Color;
import android.os.IBinder;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;

import com.example.musicplayer.BtConnectionThread;
import com.example.musicplayer.R;
import com.example.musicplayer.Bltmain.BluetoothReceiver;
import com.example.musicplayer.MusicService.MusicBinder;

import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.MediaController.MediaPlayerControl;

public class Bltmain extends ActionBarActivity implements MediaPlayerControl,Handler.Callback 
{
	private ArrayList<Song> songList;
	private ListView songView;
	private MusicService musicSrv;
	private Intent playIntent;
	private boolean musicBound=false;
	private MusicController controller;
	private boolean paused=true, playbackPaused=true;
	private boolean shuffle=false;
	AudioManager maudioManager;
	private int vol,ch=0,ch1=0;

    private static final String TAG = "MediaRouterActivity ";
        
    
    ///////////////////////////////////
    private static final String TAG1 = "Bluetooth";
    private BluetoothAdapter bluetoothAdapter = null;
    // list of BT devices that have paired with this phone
    private Spinner foundDevicesSpinner;
    private TextView sensorValTv;
    private TextView messageTv;
    //private TextView countTv;
    private static final int REQUEST_ENABLE_BT = 666;
    private ArrayAdapter<String> foundDevices;
    // Create a BroadcastReceiver for ACTION_FOUND
    private BroadcastReceiver bluetoothReceiver;
    private HashMap<String, BluetoothDevice> btDeviceMap 
            = new HashMap<String, BluetoothDevice>();
    private HashMap<String, BtConnectionThread> btConnThreadMap 
            = new HashMap<String, BtConnectionThread>();
    private HashMap<Integer, BtConnectionThread> btConnThreadLookup 
            = new HashMap<Integer, BtConnectionThread>();
    // well known SPP UUID
    private static final UUID MY_UUID = UUID
            .fromString("00001101-0000-1000-8000-00805F9B34FB");
    private BluetoothSocket bluetoothSocket;
    private String msgStr=null;
    private String test="10";
    
    ////////////////////////////////////
	
    @Override
    protected void onCreate(Bundle savedInstanceState) 
    {
    	try
    	{
        super.onCreate(savedInstanceState);
        setContentView(R.layout.blt); 
        /////////////
        foundDevicesSpinner = (Spinner) findViewById(R.id.foundSpin);
        sensorValTv = (TextView) findViewById(R.id.sensorVal);
        messageTv = (TextView) findViewById(R.id.msgLabel);
        //countTv = (TextView) findViewById(R.id.connectionsVal);
        ///////
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
       // volControl = (SeekBar)findViewById(R.id.volbar);
        
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
   
   /*
    public void setVolume()
    {
    	 maudioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
         maxVolume = maudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
         curVolume = maudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);    
        volControl.setMax(maxVolume);
         volControl.setProgress(curVolume);
         volControl.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
     @Override
     public void onStopTrackingTouch(SeekBar arg0) {
      // TODO Auto-generated method stub   }
     @Override
     public void onStartTrackingTouch(SeekBar arg0) {
      // TODO Auto-generated method stub  }
     @Override
     public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
      // TODO Auto-generated method stub
      maudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, arg1, 0);
     }  });  } */
    
    @Override
    protected void onPause()
    {
      super.onPause();
      paused=true;
    }
    
    @Override
    protected void onResume()
    {
      super.onResume();
      //setVolume();
      if(paused)
      {
        setController();
        paused=false; 
      }
    }
    
    @Override
    protected void onStop() 
    {
       //mMediaRouter.removeCallback(mMediaRouterCallback);
       controller.hide();
       super.onStop();
       //bltcntrl.stop();
    } 
    
  @Override
	public void onBackPressed() 
    {
	  super.onBackPressed();
	  finish();
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
    		  
    	case R.id.media_route_menu_item:
    		/*mediaRouteActionProvider.setRouteSelector(mSelector);
    		 mSelector = new MediaRouteSelector.Builder()
             .addControlCategory(MediaControlIntent.CATEGORY_LIVE_AUDIO)
             .addControlCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK)
             .build();
             mMediaRouter = MediaRouter.getInstance(this);
    		mMediaRouter.addCallback(mSelector, mMediaRouterCallback,
                    MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY); */
    		//BtConnectionThread btConnThread = bltConnThread.getSelectedConnThread();
    		playNext();
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
    		onDestroy();
    	      break;
    	      	    	      
    	case R.id.action_about:	
    		Intent intent1 = new Intent(this, About.class);
            startActivity(intent1);
    		break;
    	      
    	case R.id.action_end:
    	    stopService(playIntent);
    	    musicSrv=null;
           //if(bluetoothAdapter.isEnabled())
           //bluetoothAdapter.disable();        
    	   System.exit(0);
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
	
///////////////////////////////////////////////
	private void configureBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            Log.d(TAG, "configureBluetooth 1: bluetoothAdapter is null");
            return;
        }
        //Log.d(TAG, "configureBluetooth 2: GOT BT ADAPTER!");
        else if (!bluetoothAdapter.isEnabled()) 
        {
            Log.d(TAG, "configureBluetooth 3: NOT enabled - enabling");
            /*Intent enableBtIntent = new Intent(
                    BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);*/
            bluetoothAdapter.enable();
        } 
        else {
          Log.d(TAG, "configureBluetooth 4: bluetooth already enabled ");}

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter
                .getBondedDevices();
        foundDevices = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item);
        if (pairedDevices.size() > 0) {
            String key;
            for (BluetoothDevice device : pairedDevices) {
                key = device.getName() + "  " + device.getAddress();
                btDeviceMap.put(key, device);
                foundDevices.add(key);
            }
            foundDevicesSpinner.setAdapter(foundDevices);
        } 
        else 
        {
            Log.d(TAG, "configureBluetooth 5: no paired devices to show");
        }
        // Register the BroadcastReceiver
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        bluetoothReceiver = new BluetoothReceiver();
        registerReceiver(bluetoothReceiver, filter);
    }
	
	public void connect(View v) 
	{
        // discovery is a heavyweight process so
        // disable while making a connection
        bluetoothAdapter.cancelDiscovery();
        String addy = (String) foundDevicesSpinner.getSelectedItem();
        Log.d(TAG, "connect: address to connect:" + addy);
        BluetoothDevice btDevice = btDeviceMap.get(addy);
        if (btDevice == null) {
            Log.w(TAG, "connect: no bt device to connect to");
            return;
        }
        try {
            bluetoothSocket = btDevice
                    .createRfcommSocketToServiceRecord(MY_UUID);
            Log.d(TAG, "connect 0: socket:" + bluetoothSocket);
        } catch (IOException ioe) {
            String err = "Problem creating BT socket for " + addy;
            showMessage(Log.ERROR, err);
            Log.e(TAG, err);
            ioe.printStackTrace();
            return;
        }
        try {
            bluetoothSocket.connect();
            showMessage(Log.INFO, "Successfully connected to " + addy);
        } catch (IOException ioe) {
            String err = "Problem connecting to " + addy;
            showMessage(Log.ERROR, err);
            Log.e(TAG, err);
            ioe.printStackTrace();
            return;
        }
        int threadIndex = btConnThreadLookup.size();
        BtConnectionThread btConnThread = new BtConnectionThread(
                bluetoothSocket, new Handler(this), threadIndex);
        btConnThread.start();
        btConnThreadMap.put(addy, btConnThread);
        btConnThreadLookup.put(threadIndex, btConnThread);
        //countTv.setText(String.valueOf(btConnThreadMap.size()));
        Log.d(TAG, "number of connection threads:" + btConnThreadMap.size());
    }

    public void disconnect(View v) {
        BtConnectionThread btConnThread = getSelectedConnThread();
        if (btConnThread != null) {
            showMessage(Log.INFO,
                    "Disconnecting from " + btConnThread.getDeviceDisplayVal());
            btConnThread.disconnect();
            removeSelectedConnThread();
        }
    }

    private void disconnectAll() {
        for (BtConnectionThread btConnThread : btConnThreadMap.values()) {
            btConnThread.disconnect();
        }
        showMessage(Log.INFO, "Disconnecting from all devices");
       //countTv.setText("0");
    }

    public void turnOn(View v) {
   try{
        BtConnectionThread btConnThread = getSelectedConnThread();
        showMessage(Log.INFO,
                "Turning on " + btConnThread.getDeviceDisplayVal());
       btConnThread.write("*".getBytes());      
     }catch(Exception e)
      {
	     return;
      }
    }

    public void showMessage(int msgType, String message) {
        if (msgType == Log.ERROR) {
            messageTv.setTextColor(Color.rgb(200, 0, 0));
        } else if (msgType == Log.WARN) {
            messageTv.setTextColor(Color.rgb(255, 255, 0));
        } else {
            messageTv.setTextColor(Color.parseColor("#00B700"));
        }
        messageTv.setText(message);
    }

    /**
     * Remove the selected thread from the map
     */
    private void removeSelectedConnThread() {
        String addy = (String) foundDevicesSpinner.getSelectedItem();
        BtConnectionThread btct = btConnThreadMap.remove(addy);
        if (btct != null) {
        }
    }

    /**
     * Get the connection thread for the currently-selected device
     * 
     * @return relevant thread
     */
    private BtConnectionThread getSelectedConnThread() {
        String addy = (String) foundDevicesSpinner.getSelectedItem();
        return btConnThreadMap.get(addy);
    }

    public void findDevices(View v) {
    	configureBluetooth();
    	if(!bluetoothAdapter.isEnabled())
    	{
    		bluetoothAdapter.enable();
    	}
        foundDevices.clear();
        sensorValTv.setText("00");
        bluetoothAdapter.startDiscovery();
       Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        foundDevices = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item);
        if (pairedDevices.size() > 0) 
        {
            String key;
            for (BluetoothDevice device : pairedDevices) 
            {
                key = device.getName() + "  " + device.getAddress();
                btDeviceMap.put(key, device);
                foundDevices.add(key);
            }
        } 
        else 
        {
            Log.d(TAG, "configureBluetooth 5: no paired devices to show");
        }
        foundDevicesSpinner.setAdapter(foundDevices);
        showMessage(Log.INFO, "");
    }

    public void closeblt(View v) {
    	for (BtConnectionThread btConnThread : btConnThreadMap.values()) 
    	{
            btConnThread.disconnect();
        }
        showMessage(Log.INFO, "Disconnecting from all devices");
        //countTv.setText("0");
        bluetoothAdapter.disable();       
    }
    
    class BluetoothReceiver extends BroadcastReceiver 
    {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onreceive 0");
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                Log.d(TAG, "onreceive 1: found BT device");
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent
                        .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String key = device.getName() + "  " + device.getAddress();
                btDeviceMap.put(key, device);
                foundDevices.add(key);
                showMessage(Log.INFO, "Found Device:" + key);
            } else {
                Log.d(TAG, "onreceive 2: different bt action received");
            }
        }
    }

/*    @Override
    public boolean handleMessage(Message msg) {
        Log.d(TAG, "handleMessage: got message obj:" + msg.obj);
        byte[] byteArr = (byte[]) msg.obj;
        //String msgStr = new String(byteArr);
        String msgStr = byteArr.toString();
        if (msgStr != null && btConnThreadLookup.get(msg.what) != null) {

            showMessage(Log.INFO, "Received message from "
                    + btConnThreadLookup.get(msg.what).getDeviceDisplayVal());
        }
               
        if(!msgStr.equals(test))
        {
      	test=msgStr;
  	    sensorValTv.setText(test);
  	    bltmusic("3");
        }
        return true;
    } */
    
    @Override
    public boolean handleMessage(Message msg) 
    {
    	byte[] writeBuf = (byte[]) msg.obj;
    	int begin = (int)msg.arg1;
    	int end = (int)msg.arg2;
    	
    	switch(msg.what) 
    	{
    	case 1:
    	String writeMessage = new String(writeBuf);
    	writeMessage = writeMessage.substring(begin, end);
    	sensorValTv.setText(writeMessage);
    	bltmusic(sensorValTv.getText().toString());
    	break;
    	}
    	return true;
    }
    
}
