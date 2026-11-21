package pt.setorzero.core;

import java.util.Random;

/** Gera os efeitos sonoros como ficheiros WAV (16 bits, mono, 22 050 Hz). */
public final class SoundSynth {
    public static final int RATE = 22050;

    private SoundSynth() { }

    public static byte[] wav(int id) {
        float[] s = samples(id);
        int n = s.length;
        byte[] out = new byte[44 + n * 2];
        put(out, 0, "RIFF");
        le32(out, 4, 36 + n * 2);
        put(out, 8, "WAVEfmt ");
        le32(out, 16, 16);
        le16(out, 20, 1);
        le16(out, 22, 1);
        le32(out, 24, RATE);
        le32(out, 28, RATE * 2);
        le16(out, 32, 2);
        le16(out, 34, 16);
        put(out, 36, "data");
        le32(out, 40, n * 2);
        for (int i = 0; i < n; i++) {
            int v = (int) (Math.max(-1, Math.min(1, s[i])) * 32000);
            out[44 + i * 2] = (byte) v;
            out[45 + i * 2] = (byte) (v >> 8);
        }
        return out;
    }

    static float[] samples(int id) {
        Random r = new Random(100 + id);
        switch (id) {
            case Sfx.SHOT: return noiseHit(r, 0.09f, 0.35f, 2600, 140, 0.55f);
            case Sfx.MG: return noiseHit(r, 0.05f, 0.5f, 3200, 180, 0.4f);
            case Sfx.SNIPE: return noiseHit(r, 0.22f, 0.25f, 5200, 220, 0.6f);
            case Sfx.ROCKET: return whoosh(r, 0.35f);
            case Sfx.CANNON: return boom(r, 0.45f, 70, 0.8f, 900);
            case Sfx.EXPLODE: return boom(r, 0.8f, 45, 0.9f, 650);
            case Sfx.CAPTURE: return notes(new float[]{523, 659, 784, 1047}, 0.1f, 0.5f);
            case Sfx.READY: return notes(new float[]{880, 1175}, 0.07f, 0.35f);
            case Sfx.CLICK: return notes(new float[]{1400}, 0.025f, 0.25f);
            case Sfx.WIN: return notes(new float[]{523, 659, 784, 659, 784, 1047}, 0.16f, 0.5f);
            case Sfx.LOSE: return notes(new float[]{392, 349, 311, 262}, 0.28f, 0.5f);
            case Sfx.ALERT: return siren(0.6f);
            case Sfx.BOARD: return notes(new float[]{330, 440, 660}, 0.06f, 0.35f);
            default: return new float[100];
        }
    }

    private static float[] noiseHit(Random r, float dur, float decay, float cutoff, float thump, float vol) {
        int n = (int) (dur * RATE);
        float[] s = new float[n];
        float a = cutoff / RATE * 6.283f;
        a = a / (1 + a);
        float lp = 0;
        for (int i = 0; i < n; i++) {
            float t = (float) i / RATE;
            float env = (float) Math.exp(-t / (dur * decay));
            lp += a * ((r.nextFloat() * 2 - 1) - lp);
            float body = (float) Math.sin(6.283f * thump * t) * (float) Math.exp(-t / 0.03f);
            s[i] = (lp * 0.9f + body * 0.6f) * env * vol;
        }
        return s;
    }

    private static float[] whoosh(Random r, float dur) {
        int n = (int) (dur * RATE);
        float[] s = new float[n];
        float lp = 0;
        for (int i = 0; i < n; i++) {
            float t = (float) i / RATE, p = t / dur;
            float cut = 400 + 2600 * p;
            float a = cut / RATE * 6.283f;
            a = a / (1 + a);
            lp += a * ((r.nextFloat() * 2 - 1) - lp);
            float env = (float) Math.min(1, t / 0.03f) * (1 - p);
            s[i] = lp * env * 0.7f;
        }
        return s;
    }

    private static float[] boom(Random r, float dur, float freq, float vol, float cutoff) {
        int n = (int) (dur * RATE);
        float[] s = new float[n];
        float lp = 0, lp2 = 0;
        float a = cutoff / RATE * 6.283f;
        a = a / (1 + a);
        for (int i = 0; i < n; i++) {
            float t = (float) i / RATE;
            float env = (float) Math.exp(-t / (dur * 0.28f));
            lp += a * ((r.nextFloat() * 2 - 1) - lp);
            lp2 += a * (lp - lp2);
            float f = freq * (1 - 0.4f * t / dur);
            float sub = (float) Math.sin(6.283f * f * t) * (float) Math.exp(-t / (dur * 0.35f));
            float crack = r.nextFloat() < 0.002f * (1 - t / dur) ? (r.nextFloat() - 0.5f) * 0.6f : 0;
            s[i] = (lp2 * 2.2f + sub * 0.8f + crack) * env * vol;
        }
        return s;
    }

    private static float[] notes(float[] freqs, float each, float vol) {
        int per = (int) (each * RATE);
        float[] s = new float[per * freqs.length + RATE / 20];
        for (int k = 0; k < freqs.length; k++) {
            for (int i = 0; i < per + RATE / 20 && k * per + i < s.length; i++) {
                float t = (float) i / RATE;
                float env = Math.min(1, t / 0.005f) * (float) Math.exp(-t / (each * 0.9f));
                float ph = 6.283f * freqs[k] * t;
                float v = (float) (Math.sin(ph) * 0.7 + Math.sin(ph * 2) * 0.2 + Math.sin(ph * 3) * 0.1);
                s[k * per + i] += v * env * vol;
            }
        }
        return s;
    }

    private static float[] siren(float dur) {
        int n = (int) (dur * RATE);
        float[] s = new float[n];
        float ph = 0;
        for (int i = 0; i < n; i++) {
            float t = (float) i / RATE;
            float f = ((int) (t / 0.15f) % 2 == 0) ? 660 : 880;
            ph += 6.283f * f / RATE;
            float env = Math.min(1, t / 0.01f) * Math.min(1, (dur - t) / 0.05f);
            s[i] = (float) (Math.sin(ph) > 0 ? 0.25 : -0.25) * env;
        }
        return s;
    }

    private static void put(byte[] b, int o, String s) {
        for (int i = 0; i < s.length(); i++) b[o + i] = (byte) s.charAt(i);
    }

    private static void le16(byte[] b, int o, int v) { b[o] = (byte) v; b[o + 1] = (byte) (v >> 8); }

    private static void le32(byte[] b, int o, int v) {
        b[o] = (byte) v; b[o + 1] = (byte) (v >> 8); b[o + 2] = (byte) (v >> 16); b[o + 3] = (byte) (v >> 24);
    }
}
