package pt.setorzero.android;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;

import java.io.File;
import java.io.FileOutputStream;

import pt.setorzero.core.SoundSynth;
import pt.setorzero.core.Sfx;

/** Sons e música gerados por código (SoundSynth), gravados em cache como WAV. */
final class AndroidSfx implements Sfx {
    private final SoundPool pool;
    private final AudioAttributes attrs;
    private final int[] ids = new int[COUNT];
    private final boolean[] ready = new boolean[COUNT];
    private final File[] musicFiles = new File[2];
    private final boolean[] musicReady = new boolean[2];
    private volatile boolean muted;
    private MediaPlayer mp;
    private int wanted = MUSIC_NONE;

    AndroidSfx(final Context ctx) {
        attrs = new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build();
        pool = new SoundPool.Builder().setMaxStreams(10).setAudioAttributes(attrs).build();
        pool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override public void onLoadComplete(SoundPool p, int sampleId, int status) {
                if (status != 0) return;
                synchronized (ids) {
                    for (int i = 0; i < COUNT; i++) if (ids[i] == sampleId) ready[i] = true;
                }
            }
        });
        final File dir = ctx.getCacheDir();
        new Thread(new Runnable() {
            @Override public void run() {
                for (int i = 0; i < COUNT; i++) {
                    try {
                        File f = new File(dir, "som-" + i + "-v2.wav");
                        if (!f.exists()) write(f, SoundSynth.wav(i));
                        synchronized (ids) { ids[i] = pool.load(f.getPath(), 1); }
                    } catch (Exception ignored) {
                        // sem esse som
                    }
                }
                for (int t = 0; t < 2; t++) {
                    try {
                        File f = new File(dir, "musica-" + t + "-v1.wav");
                        if (!f.exists()) write(f, SoundSynth.musicWav(t));
                        synchronized (AndroidSfx.this) {
                            musicFiles[t] = f;
                            musicReady[t] = true;
                        }
                        startIfReady();
                    } catch (Exception ignored) {
                        // sem música
                    }
                }
            }
        }, "sons").start();
    }

    private static void write(File f, byte[] data) throws Exception {
        File tmp = new File(f.getPath() + ".tmp");
        FileOutputStream out = new FileOutputStream(tmp);
        out.write(data);
        out.close();
        if (!tmp.renameTo(f)) throw new Exception("rename");
    }

    synchronized void setMuted(boolean m) {
        muted = m;
        if (m) stopPlayer();
        else startIfReady();
    }

    @Override public void play(int id, float volume) {
        if (muted || id < 0 || id >= COUNT) return;
        int sample;
        synchronized (ids) {
            if (!ready[id]) return;
            sample = ids[id];
        }
        pool.play(sample, volume, volume, 1, 0, 1f);
    }

    @Override public synchronized void music(int track) {
        if (track == wanted && mp != null) return;
        wanted = track;
        stopPlayer();
        startIfReady();
    }

    private synchronized void stopPlayer() {
        if (mp == null) return;
        try { mp.stop(); } catch (Exception ignored) { }
        mp.release();
        mp = null;
    }

    private synchronized void startIfReady() {
        if (muted || wanted < 0 || wanted > 1 || mp != null || !musicReady[wanted]) return;
        try {
            MediaPlayer p = new MediaPlayer();
            p.setAudioAttributes(attrs);
            p.setDataSource(musicFiles[wanted].getPath());
            p.setLooping(true);
            p.prepare();
            p.setVolume(0.4f, 0.4f);
            p.start();
            mp = p;
        } catch (Exception e) {
            mp = null;
        }
    }

    synchronized void release() {
        stopPlayer();
        pool.release();
    }
}
