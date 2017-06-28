package com.kara4k.moozic;


import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.util.Log;

import static android.content.Context.AUDIO_SERVICE;

public class Player implements AudioManager.OnAudioFocusChangeListener, MediaPlayer.OnCompletionListener {


    public static final int PROGRESS = 1;
    public static final int BUFFERING = 2;


    private Context mContext;
    private final AudioManager mAudioManager;
    private MediaPlayer mMediaPlayer;
    private boolean playOnInterrupt;
    private Handler mSingleFragHandler;
    private PlayerSingleCallback mPlayerSingleCallback;
    private PlayerListCallback mPlayerListCallback;
    private boolean shouldStop = false;
    private Player mPlayer;

    interface PlayerSingleCallback {
        void onPlayTrack(TrackItem trackItem);

        void onPlay();

        void onPauseTrack();

        void onStopTrack();

    }

    interface PlayerListCallback {

        void playNext();

        void playPrev();

        void repeatCurrent();
    }


    public Player(Context context) {
        mPlayer = this;
        mContext = context;
        playOnInterrupt = false;
        mAudioManager = (AudioManager) mContext.getSystemService(AUDIO_SERVICE);
        mAudioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
    }

    public void playToggle() {
        if (mMediaPlayer == null) { // TODO: 25.06.2017 db current track
//            TrackItem currentTrack = Preferences.getCurrentTrack(mContext);
//            playTrack(currentTrack);
            repeatCurrent();
        } else {
            togglePlayPause();
        }
    }

    public void playTrack(final TrackItem trackItem) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                stop();
                try {
                    if (trackItem == null) return;
                    mMediaPlayer = new MediaPlayer();
                    mMediaPlayer.setOnCompletionListener(mPlayer);
                    mMediaPlayer.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
                        @Override
                        public void onBufferingUpdate(MediaPlayer mp, int percent) {
                            if (mSingleFragHandler != null) {
                                mSingleFragHandler.obtainMessage(BUFFERING, percent, 0).sendToTarget();
                                Log.e("Player", "onBufferingUpdate: " + percent);
                            }
                        }
                    });
                    mMediaPlayer.setDataSource(trackItem.getFilePath());
                    mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                    mMediaPlayer.prepare();
                    mMediaPlayer.start();

                    if (mPlayerSingleCallback != null) {
                        mPlayerSingleCallback.onPlayTrack(trackItem);
                        mPlayerSingleCallback.onPlay();
                    }

                    startTracking();

                } catch (Exception e) {
                    stopTracking();
                    e.printStackTrace();
                }
            }
        }).start();

    }

    private void play() {
        if (mMediaPlayer == null) return;
        mMediaPlayer.start();
        if (mPlayerSingleCallback != null) {
            mPlayerSingleCallback.onPlay();
        }
        startTracking();
    }

    public void playNext() {
        if (mPlayerListCallback != null) {
            mPlayerListCallback.playNext();
        }
    }

    public void repeatCurrent() {
        if (mPlayerListCallback != null) {
            mPlayerListCallback.repeatCurrent();
        }
    }

    public void playPrev() {
        if (mPlayerListCallback != null) {
            mPlayerListCallback.playPrev();
        }
    }

    public void play(TrackItem trackItem) {
        if (mMediaPlayer == null) {
            playTrack(trackItem);

        } else {
            play();
        }
    }

    public void pause() {
        if (mMediaPlayer == null) {
            return;
        }
        if (mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();
        }
        if (mPlayerSingleCallback != null) {
            mPlayerSingleCallback.onPauseTrack();
        }
        stopTracking();
    }

    public void resume() {

        if (mMediaPlayer == null) {
            return;
        }
        if (!mMediaPlayer.isPlaying()) {
            mMediaPlayer.start();
            playOnInterrupt = true;
        }
    }

    public void togglePlayPause() {
        if (mMediaPlayer == null) {
            return;
        }
        if (mMediaPlayer.isPlaying()) {
            pause();
        } else {
            play();
        }
    }

    public void stop() {
        if (mMediaPlayer == null) {
            return;
        }
        mMediaPlayer.stop();
        mMediaPlayer.release();
        mMediaPlayer = null;
        if (mPlayerSingleCallback != null) {
            mPlayerSingleCallback.onStopTrack();
        }
    }

    public boolean isPlaying(){
        if (mMediaPlayer == null || !mMediaPlayer.isPlaying()) {
            return false;
        }
        return true;
    }

    @Override
    public void onAudioFocusChange(int i) {
        Log.e("Player", "onAudioFocusChange: " + "here");
        if (mMediaPlayer != null) {
            if (i <= 0 && i != -3) {
                if (mMediaPlayer.isPlaying()) {
                    pause();
                    playOnInterrupt = true;
                }
            } else if (i > 0 && playOnInterrupt) {
                play();
                playOnInterrupt = false;
            }
        }
    }

    public void release() {
        releaseMediaPlayer();
        mAudioManager.abandonAudioFocus(this);
    }

    private void releaseMediaPlayer() {
        if (mMediaPlayer == null) {
            return;
        }
        if (mMediaPlayer.isPlaying()) {
            mMediaPlayer.stop();
        }
        mMediaPlayer.release();
        mMediaPlayer = null;
    }

    public int getDuration() {
        if (mMediaPlayer == null) return -1;
        return mMediaPlayer.getDuration();
    }

    public int getPosition() {
        if (mMediaPlayer == null) return -1;
        return mMediaPlayer.getCurrentPosition();
    }

    public void startTracking() {
        shouldStop = false;
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (!shouldStop) {
                    try {
                        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
                            if (mSingleFragHandler == null) {
                                shouldStop = true;
                                continue;
                            }
                            mSingleFragHandler.obtainMessage(PROGRESS,
                                    mMediaPlayer.getCurrentPosition(), 0).sendToTarget();

                            Thread.sleep(500);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    public void seekTo(int position) {
        if (mMediaPlayer != null) {
            mMediaPlayer.seekTo(position);
        }
    }

    public void stopTracking() {
        shouldStop = true;
    }

    public void setSingleFragHandler(Handler singleFragHandler) {
        mSingleFragHandler = singleFragHandler;

    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        boolean repeatOne = Preferences.isRepeatOne(mContext);
        if (repeatOne) {
            repeatCurrent();
        } else {
            playNext();
        }
    }

    public void setPlayerSingleCallback(PlayerSingleCallback playerSingleCallback) {
        mPlayerSingleCallback = playerSingleCallback;
    }

    public void setPlayerListCallback(PlayerListCallback playerListCallback) {
        mPlayerListCallback = playerListCallback;
    }
}