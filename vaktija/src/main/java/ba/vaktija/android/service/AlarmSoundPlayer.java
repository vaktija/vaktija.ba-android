package ba.vaktija.android.service;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.CountDownTimer;
import android.util.Log;

import java.io.IOException;

import ba.vaktija.android.util.FileLog;

public class AlarmSoundPlayer implements MediaPlayer.OnPreparedListener {
    private static final String TAG = "AlarmSoundPlayer";

    private MediaPlayer mediaPlayer;
    private AudioManager audioManager;
    private int initialStreamVolume;
    private CountDownTimer volumeTimer;
    private boolean increasing;
    private Context context;
    private int volume = 0;

    public AlarmSoundPlayer(Context context) {
        this.context = context;
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        FileLog.i(TAG, "initial stream volume: "+ initialStreamVolume);
    }

    public void play(Uri soundUri, boolean increasing) throws IOException {
        this.increasing = increasing;

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
        mediaPlayer.setLooping(true);
        mediaPlayer.setDataSource(context, soundUri);
        mediaPlayer.prepareAsync();
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        Log.d(TAG, "onPrepared");
        mp.start();

        if(increasing) {
            increaseVolume();
        }
    }

    private void increaseVolume(){
        FileLog.d(TAG, "increaseVolume");

        initialStreamVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM);
        int maxStreamVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM);

        FileLog.i(TAG, "initialStreamVolume=" + initialStreamVolume+" maxStreamVolume="+maxStreamVolume);

        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, 0, 0);

        volumeTimer = new CountDownTimer(maxStreamVolume * 1000 * 2, 2000) {

            @Override
            public void onTick(long millisUntilFinished) {
                volume++;
                FileLog.i(TAG, "volume: " + volume);
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, volume, 0);
            }

            @Override
            public void onFinish() {
//                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, initialStreamVolume, 0);
            }
        };

        volumeTimer.start();
    }

    public void cancel(){
        FileLog.d(TAG, "cancel");

        if(audioManager != null) {
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, initialStreamVolume, 0);
            FileLog.i(TAG, "restored STREAM_ALARM volume to " + initialStreamVolume);
        }

        if(mediaPlayer != null) {
            mediaPlayer.release();
        }

        if(volumeTimer != null) {
            volumeTimer.cancel();
        }
    }
}
