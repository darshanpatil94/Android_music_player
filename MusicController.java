package com.example.musicplayer;

import android.content.Context;
import android.widget.MediaController;

public class MusicController extends MediaController 
{
	public MusicController(Context c)
	{
	    super(c);
	 }

	@Override
	public void setMediaPlayer(MediaPlayerControl player) {
		// TODO Auto-generated method stub
		super.setMediaPlayer(player);
	}
	
}
