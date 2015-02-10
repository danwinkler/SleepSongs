package com.danwink.sleepsongs;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;


public class ChooseSongActivity extends ActionBarActivity {

    ArrayList<MusicSet> folders = new ArrayList<MusicSet>();

    public int timeLeft;
    public MediaPlayer mp;

    File folderDir;

    SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //requestWindowFeature( Window.FEATURE_NO_TITLE );
        setContentView(R.layout.activity_choose_song);

        prefs = getPreferences( MODE_PRIVATE );


        folderDir = new File( Environment.getExternalStorageDirectory() + "/simplesongs/" );

        if( !folderDir.isDirectory() )
        {
            folderDir.mkdirs();
        }

        mp = new MediaPlayer();

        for( int i = 0; i < 4; i++ )
        {
            MusicSet ms = new MusicSet( i );
            switch( i )
            {
                case 0: ms.getButtons( this, R.id.b_a ); break;
                case 1: ms.getButtons( this, R.id.b_b ); break;
                case 2: ms.getButtons( this, R.id.b_c ); break;
                case 3: ms.getButtons( this, R.id.b_d ); break;
            }
            ms.makeDirs();
            folders.add( ms );
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        System.runFinalizersOnExit(true);
        System.exit(0);
    }

    public void setPrefInt( String name, int i )
    {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt( name, i );
        editor.commit();
    }

    public int getPrefInt( String name )
    {
        return prefs.getInt( name, 0 );
    }

    public class MusicSet implements View.OnClickListener, MediaPlayer.OnCompletionListener
    {
        boolean streamSet = false;
        Button button;
        int pos;
        String[] files;
        StopMusic stopMusic = new StopMusic();
        Timer timer;

        int onTrack = 0;

        int timesClicked = 0;
        StartMusic startMusic = new StartMusic();

        Handler handler = new Handler();

        int lastTime;

        int minutesToPlay;

        int totalTimePlayed = 0;

        public MusicSet( int pos )
        {
            this.pos = pos;
            this.streamSet = pos == 3;
        }

        public void getButtons( Context c, int id )
        {
            button = (Button)findViewById( id );
            button.setOnClickListener( this );
        }

        public void makeDirs()
        {
            File f = new File( Environment.getExternalStorageDirectory() + "/simplesongs/" + "f" + pos + "/" );
            if( !f.isDirectory() )
            {
                f.mkdirs();
            }
            files = f.list();
        }

        public void playMusic( int minutes )
        {
            minutesToPlay = minutes;

            if( pos == 3 )
            {
                try
                {
                    String url = "http://pubint.ic.llnwd.net/stream/pubint_kut";
                    if( mp != null )
                    {
                        mp.stop();
                    }
                    mp = new MediaPlayer();
                    mp.setAudioStreamType( AudioManager.STREAM_MUSIC );
                    mp.setDataSource( url );
                    mp.prepare();

                    mp.start();
                    timer = new Timer();
                    stopMusic = new StopMusic();
                    timer.schedule( stopMusic, minutes * 60 * 1000 );
                } catch( IllegalArgumentException e )
                {
                    Log.e( "SS", e.toString() );
                } catch( IllegalStateException e )
                {
                    Log.e( "SS", e.toString() );
                } catch( IOException e )
                {
                    Log.e( "SS", e.toString() );
                }
            }
            else if( files.length > 0 )
            {
                try
                {
                    if( mp != null )
                    {
                        mp.stop();
                    }

                    lastTime = getPrefInt( "set" + pos );

                    int tempLastTime = lastTime;
                    onTrack = 0;
                    mp = new MediaPlayer();
                    if( tempLastTime > 0 )
                    {
                        mp.reset();
                        mp.setDataSource( Environment.getExternalStorageDirectory() + "/simplesongs/" + "f" + pos + "/" + files[0] );
                        mp.prepare();
                        int trackDur = mp.getDuration();
                        if( tempLastTime > trackDur )
                        {
                            while( tempLastTime > trackDur )
                            {
                                if( onTrack < files.length )
                                {
                                    mp.reset();
                                    mp.setDataSource( Environment.getExternalStorageDirectory() + "/simplesongs/" + "f" + pos + "/" + files[onTrack] );
                                    mp.prepare();
                                    trackDur = mp.getDuration();
                                    tempLastTime -= mp.getDuration();
                                }
                                else
                                {
                                    onCompletion( mp );
                                    tempLastTime = 0;
                                    mp.reset();
                                    mp.setDataSource( Environment.getExternalStorageDirectory() + "/simplesongs/" + "f" + pos + "/" + files[0] );
                                    mp.prepare();
                                }
                                onTrack++;
                            }
                        }
                        mp.seekTo( tempLastTime );
                    }
                    else
                    {
                        String dataSource = Environment.getExternalStorageDirectory() + "/simplesongs/" + "f" + pos + "/" + files[0];
                        mp.reset();
                        mp.setDataSource( dataSource );
                        mp.prepare();
                    }

                    mp.start();
                    mp.setOnCompletionListener( this );
                    timer = new Timer();
                    stopMusic = new StopMusic();
                    timer.schedule( stopMusic, minutes * 60 * 1000 );
                } catch( IllegalArgumentException e )
                {
                    Log.e( "SS", e.toString() );
                } catch( IllegalStateException e )
                {
                    Log.e( "SS", e.toString() );
                } catch( IOException e )
                {
                    Log.e( "SS", e.toString() );
                }

            }
        }

        @Override
        public void onClick( View v )
        {
            timesClicked++;
            handler.removeCallbacks( startMusic );
            handler.postDelayed( startMusic, 2000 );
            button.setText( timesClicked*20 + " mins" );
            for( MusicSet set : folders )
            {
                if( set != this )
                    set.cancelEverything();
            }
        }

        private void cancelEverything()
        {
            stopMusic.run();
            if( timer != null )
                timer.cancel();
            handler.removeCallbacks( startMusic );
            timesClicked = 0;
        }

        @Override
        public void onCompletion( MediaPlayer mp )
        {
            onTrack++;
            if( onTrack < files.length )
            {
                try
                {
                    mp.reset();
                    mp.setDataSource( Environment.getExternalStorageDirectory() + "/simplesongs/" + "f" + pos + "/" + files[onTrack] );
                    //Log.e( "SS", files[onTrack] );
                    mp.prepare();
                    mp.start();
                } catch( IllegalArgumentException e )
                {
                    Log.e( "SS", e.toString() );
                } catch( IllegalStateException e )
                {
                    Log.e( "SS", e.toString() );
                } catch( IOException e )
                {
                    Log.e("SS", e.toString());
                }
            }
            else
            {
                setPrefInt( "set" + pos, 0 );
            }
        }

        public class StopMusic extends TimerTask
        {
            public void run()
            {
                setPrefInt( "set" + pos, (minutesToPlay*1000*60) + lastTime );
                mp.stop();
                if( !streamSet ) {
                    onTrack = files.length;
                }
            }
        }

        public class StartMusic implements Runnable
        {
            public void run()
            {
                playMusic( timesClicked*20 );
                button.setText( (pos+1) + "" );
                timesClicked = 0;
            }
        }
    }

    /*
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_choose_song, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    */
}
