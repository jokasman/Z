package pt.setorzero.core;

import java.util.Random;

/** Gera os efeitos sonoros como ficheiros WAV (16 bits, mono, 22 050 Hz). */
public final class SoundSynth {
    public static final int RATE = 22050;

    private SoundSynth() { }

    public static byte[] wav(int id) { return wav(crunch(samples(id))); }

    /** Música de fundo em WAV (repete-se em ciclo). */
    public static byte[] musicWav(int track) { return wav(track == Sfx.MUSIC_MENU ? menuTrack() : battleTrack()); }

    /** Som de 8 bits: metade da taxa de amostragem e 6 bits de resolução. */
    static float[] crunch(float[] s) {
        for (int i = 0; i < s.length; i++) {
            float v = (i & 1) == 0 ? s[i] : s[i - 1];
            s[i] = Math.round(v * 32) / 32f;
        }
        return s;
    }

    static byte[] wav(float[] s) {
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

    // ---------------------------------------------------------------- música chiptune

    private static float midi(int n) { return 440f * (float) Math.pow(2, (n - 69) / 12.0); }

    private static float square(double ph, float duty) { return (ph - Math.floor(ph)) < duty ? 1f : -1f; }

    private static float tri(double ph) {
        double f = ph - Math.floor(ph);
        return (float) (f < 0.5 ? f * 4 - 1 : 3 - f * 4);
    }

    /** Escreve uma nota quadrada/triangular no buffer. */
    private static void note(float[] buf, int start, int len, float freq, float vol, int wave, float decay) {
        double ph = 0, inc = freq / RATE;
        for (int i = 0; i < len && start + i < buf.length; i++) {
            float t = (float) i / RATE;
            float env = Math.min(1, t / 0.004f) * (float) Math.exp(-t / decay) * Math.min(1, (len - i) / 120f);
            float v = wave == 0 ? square(ph, 0.25f) : wave == 1 ? tri(ph) : square(ph, 0.5f);
            buf[start + i] += v * env * vol;
            ph += inc;
        }
    }

    private static void drum(float[] buf, int start, int kind, Random r) {
        int len = kind == 0 ? RATE / 6 : kind == 1 ? RATE / 8 : RATE / 30;
        float lp = 0;
        for (int i = 0; i < len && start + i < buf.length; i++) {
            float t = (float) i / RATE;
            float v;
            if (kind == 0) v = (float) Math.sin(6.283 * (110 - 300 * t) * t) * (float) Math.exp(-t / 0.06f) * 0.55f;
            else {
                float n = r.nextFloat() * 2 - 1;
                lp += 0.6f * (n - lp);
                v = (kind == 1 ? lp * 0.35f : (n - lp) * 0.18f) * (float) Math.exp(-t / (kind == 1 ? 0.05f : 0.012f));
            }
            buf[start + i] += v;
        }
    }

    private static float[] render(int bpm, int[] roots, boolean[] minor, int[][] melody, int bassMode, boolean drums, int repeats) {
        int step = (int) (RATE * 60f / bpm / 4); // semicolcheia
        int bars = roots.length;
        float[] buf = new float[step * 16 * bars * repeats];
        Random r = new Random(5);
        for (int rep = 0; rep < repeats; rep++) {
            for (int bar = 0; bar < bars; bar++) {
                int base = (rep * bars + bar) * 16 * step;
                int root = roots[bar];
                int[] chord = minor[bar] ? new int[]{0, 3, 7, 12, 15} : new int[]{0, 4, 7, 12, 16};
                // baixo
                for (int s = 0; s < 16; s += bassMode == 0 ? 2 : 4) {
                    int n = root - 12 + ((s / 2) % 2 == 1 && bassMode == 0 ? 12 : 0);
                    note(buf, base + s * step, step * (bassMode == 0 ? 2 : 4) - 40, midi(n), 0.32f, 1, 0.5f);
                }
                // melodia
                int[] mel = melody[(bar + rep) % melody.length];
                for (int s = 0; s < 16; s++) {
                    int k = mel[s];
                    if (k < 0) continue;
                    int len = 1;
                    while (s + len < 16 && mel[s + len] == -2) len++;
                    note(buf, base + s * step, step * len - 30, midi(root + 12 + chord[k % 5] + (k >= 5 ? 12 : 0)), 0.14f, 0, 0.12f * len + 0.05f);
                }
                // arpejo suave
                for (int s = 0; s < 16; s++) {
                    if (bassMode == 1 || s % 2 == 0) note(buf, base + s * step, step - 20, midi(root + 12 + chord[(s / (bassMode == 1 ? 1 : 2)) % 4]), bassMode == 1 ? 0.07f : 0.05f, 2, 0.06f);
                }
                if (drums) {
                    for (int s = 0; s < 16; s++) {
                        if (s % 8 == 0 || (s == 10 && bar % 2 == 1)) drum(buf, base + s * step, 0, r);
                        if (s % 8 == 4) drum(buf, base + s * step, 1, r);
                        if (s % 2 == 0) drum(buf, base + s * step, 2, r);
                    }
                }
            }
        }
        float peak = 0;
        for (float v : buf) peak = Math.max(peak, Math.abs(v));
        float g = peak > 0 ? 0.8f / peak : 1;
        for (int i = 0; i < buf.length; i++) buf[i] *= g;
        return crunch(buf);
    }

    static float[] battleTrack() {
        int[] roots = {57, 57, 53, 53, 48, 48, 55, 52};
        boolean[] minor = {true, true, false, false, false, false, false, false};
        int O = -1, H = -2;
        int[][] mel = {
            {0, H, 2, H, 1, O, 2, 3, O, 2, H, 1, 0, H, O, O},
            {3, H, 2, 1, 2, H, 0, O, 1, H, 2, H, 4, H, 3, O},
            {2, O, 2, 3, 4, H, 3, 2, 1, H, 0, O, 1, 2, H, O},
            {0, H, H, O, 1, 2, 3, H, 2, 1, 0, H, 1, H, H, O},
        };
        return render(132, roots, minor, mel, 0, true, 2);
    }

    static float[] menuTrack() {
        int[] roots = {50, 50, 46, 46, 53, 53, 48, 52};
        boolean[] minor = {true, true, false, false, false, false, false, false};
        int O = -1, H = -2;
        int[][] mel = {
            {2, H, H, H, H, H, 1, H, 0, H, H, H, O, O, O, O},
            {3, H, H, H, 2, H, H, H, 1, H, 2, H, H, H, O, O},
            {4, H, H, H, 3, H, 2, H, 3, H, H, H, H, H, O, O},
            {2, H, 1, H, 0, H, H, H, H, H, H, H, O, O, O, O},
        };
        return render(92, roots, minor, mel, 1, false, 2);
    }

    private static void put(byte[] b, int o, String s) {
        for (int i = 0; i < s.length(); i++) b[o + i] = (byte) s.charAt(i);
    }

    private static void le16(byte[] b, int o, int v) { b[o] = (byte) v; b[o + 1] = (byte) (v >> 8); }

    private static void le32(byte[] b, int o, int v) {
        b[o] = (byte) v; b[o + 1] = (byte) (v >> 8); b[o + 2] = (byte) (v >> 16); b[o + 3] = (byte) (v >> 24);
    }
}
