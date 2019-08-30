package com.anteour.shivchalisa;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.AsyncTask;
interface onPlayStateListener {
    void onStateChanged(Boolean play);
}

public class MediaWorker {
    private static int AUDIO_ID = -1;
    private static int PLAYER_POS = 0;
    private static int DURATION = 0;

    private static MediaWorker mediaWorker = null;

    private static MediaPlayer mediaPlayer = null;
    private static MediaPlayer.OnCompletionListener onCompletionListener = null;

    private ExplicitOnCompletionListener c;

    private MediaWorker() {
    }

    protected static MediaWorker getInstance(MediaPlayer.OnCompletionListener onCompletionListener) {
        if (mediaWorker == null) {
            MediaWorker.mediaWorker = new MediaWorker();
            MediaWorker.onCompletionListener = onCompletionListener;
        }
        return mediaWorker;
    }

    protected void play(Context context) {
        if (AUDIO_ID == -1) {
            getAudioIDNullException().printStackTrace();
            return;
        }
        mediaPlayer = MediaPlayer.create(context, MediaWorker.AUDIO_ID);
        MediaWorker.DURATION = mediaPlayer.getDuration();
        mediaPlayer.seekTo(MediaWorker.PLAYER_POS);
        mediaPlayer.setOnCompletionListener(MediaWorker.onCompletionListener);
        mediaPlayer.start();
        c = new ExplicitOnCompletionListener();
        c.execute();
        onPlayStateListener onPlayStateListener = (onPlayStateListener) context;
        onPlayStateListener.onStateChanged(true);

    }

    protected void pause(Context context) {
        if (mediaPlayer == null) {
            getNoMediaPlayerException().printStackTrace();
            return;
        }
        MediaWorker.PLAYER_POS = mediaPlayer.getCurrentPosition();
        stopPlayer(context);
        onPlayStateListener onPlayStateListener = (onPlayStateListener) context;
        onPlayStateListener.onStateChanged(false);
    }

    protected void stop(Context context) {
        MediaWorker.PLAYER_POS = 0;
        stopPlayer(context);
    }

    protected void jumpTo(int percent) {
        if (mediaPlayer == null) {
            getNoMediaPlayerException().printStackTrace();
            return;
        }
        mediaPlayer.seekTo((percent * mediaPlayer.getDuration()) / 100);
    }

    protected void resetWorker(Context context, int audioID) {
        MediaWorker.PLAYER_POS = 0;
        MediaWorker.AUDIO_ID = audioID;
        stopPlayer(context);
    }

    protected void destroy(Context context) {
        MediaWorker.onCompletionListener = null;
        stopPlayer(context);
    }

    private void stopPlayer(Context context) {
        if (mediaPlayer == null) {
            getNoMediaPlayerException().printStackTrace();
            return;
        }
        c.cancel(true);
        c = null;
        mediaPlayer.stop();
        mediaPlayer.release();
        mediaPlayer = null;
        onPlayStateListener onPlayStateListener = (onPlayStateListener) context;
        onPlayStateListener.onStateChanged(false);
    }

    private Exception getAudioIDNullException() {
        return new Exception("MediaWorker.AUDIO_ID=null (Audio Resource not found!)");
    }

    private Exception getNoMediaPlayerException() {
        return new Exception("MediaWorker.NullPointerException (No MediaPlayer found!)");
    }

    public int getProgress() throws IllegalStateException {
        if (mediaPlayer == null) {
            getNoMediaPlayerException().printStackTrace();
            return (MediaWorker.PLAYER_POS * 100) / MediaWorker.DURATION;
        }
        return (mediaPlayer.getCurrentPosition() * 100) / MediaWorker.DURATION;
    }


    private static class ExplicitOnCompletionListener extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... voids) {
            while (mediaPlayer != null) {
                if (mediaPlayer != null && (mediaPlayer.getCurrentPosition() * 100) / MediaWorker.DURATION >= 99) {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (mediaPlayer != null && (mediaPlayer.getCurrentPosition() * 100) / MediaWorker.DURATION >= 99)
                        return true;
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean isPlaybackCompleted) {
            if (isPlaybackCompleted) {
                if (MediaWorker.onCompletionListener != null)
                    MediaWorker.onCompletionListener.onCompletion(mediaPlayer);
            }
            super.onPostExecute(isPlaybackCompleted);
        }
    }
}