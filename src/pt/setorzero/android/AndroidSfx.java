package pt.setorzero.android;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;

import java.io.File;
import java.io.FileOutputStream;

import pt.setorzero.core.SoundSynth;
import pt.setorzero.core.Sfx;

/** Sons gerados por código (SoundSynth), gravados em cache como WAV e tocados com SoundPool. */
final class AndroidSfx implements Sfx {
    private final SoundPool pool;
    private final int[] ids = new int[COUNT];
    private final boolean[] ready = new boolean[COUNT];
    private volatile boolean muted;

    AndroidSfx(final Context ctx) {
        AudioAttributes attrs = new AudioAttributes.Builder()
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
                        File f = new File(dir, "som-" + i + "-v1.wav");
                        if (!f.exists()) {
                            FileOutputStream out = new FileOutputStream(f);
                            out.write(SoundSynth.wav(i));
                            out.close();
                        }
                        synchronized (ids) { ids[i] = pool.load(f.getPath(), 1); }
                    } catch (Exception ignored) {
                        // sem esse som
                    }
                }
            }
        }, "sons").start();
    }

    void setMuted(boolean m) { muted = m; }

    @Override public void play(int id, float volume) {
        if (muted || id < 0 || id >= COUNT) return;
        int sample;
        synchronized (ids) {
            if (!ready[id]) return;
            sample = ids[id];
        }
        pool.play(sample, volume, volume, 1, 0, 1f);
    }

    void release() { pool.release(); }
}
