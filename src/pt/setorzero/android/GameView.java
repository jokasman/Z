package pt.setorzero.android;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.concurrent.ConcurrentLinkedQueue;

import pt.setorzero.core.Game;
import pt.setorzero.core.Store;

/**
 * Superfície do jogo. O jogo é desenhado numa imagem pequena (cerca de 270 píxeis de altura) que
 * depois é ampliada sem suavização, com linhas de varrimento opcionais (efeito de monitor CRT).
 */
final class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    private static final int DOWN = 0, MOVE = 1, UP = 2, CANCEL = 3, BACK = 4, RESIZE = 5, PAUSE = 6;

    private final Activity activity;
    private final Game game;
    private final AndroidGfx gfx = new AndroidGfx();
    private final ConcurrentLinkedQueue<float[]> events = new ConcurrentLinkedQueue<>();
    private final float density;
    private final Paint blit = new Paint();
    private final Paint scan = new Paint();
    private final Rect dst = new Rect();
    private Thread thread;
    private volatile boolean running;
    private volatile int px = 4;
    private int vw, vh, sw, sh;
    private Bitmap low;
    private Canvas lowCanvas;

    GameView(Activity activity, AndroidSfx sfx, Store store) {
        super(activity);
        this.activity = activity;
        this.density = getResources().getDisplayMetrics().density;
        this.game = new Game(sfx, store);
        blit.setFilterBitmap(false);
        scan.setColor(0x34000000);
        getHolder().addCallback(this);
        setFocusable(true);
        setKeepScreenOn(true);
    }

    @Override public void surfaceCreated(SurfaceHolder holder) {
        running = true;
        thread = new Thread(this, "jogo");
        thread.start();
    }

    @Override public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        px = Math.max(1, Math.round(h / 280f));
        events.add(new float[]{RESIZE, 0, w, h});
    }

    @Override public void surfaceDestroyed(SurfaceHolder holder) {
        running = false;
        if (thread != null) {
            try { thread.join(1000); } catch (InterruptedException ignored) { }
            thread = null;
        }
    }

    void postBack() { events.add(new float[]{BACK, 0, 0, 0}); }

    void postPause() { events.add(new float[]{PAUSE, 0, 0, 0}); }

    @Override public void run() {
        long last = System.nanoTime();
        while (running) {
            long now = System.nanoTime();
            float dt = (now - last) / 1e9f;
            last = now;
            float[] e;
            while ((e = events.poll()) != null) handle(e);
            game.update(dt);
            if (low != null) {
                gfx.set(lowCanvas);
                game.render(gfx);
                SurfaceHolder h = getHolder();
                Canvas c;
                try { c = h.lockHardwareCanvas(); } catch (Exception ex) { c = null; }
                if (c == null) {
                    try { c = h.lockCanvas(); } catch (Exception ex) { c = null; }
                }
                if (c != null) {
                    try {
                        int p = px;
                        dst.set(0, 0, vw * p, vh * p);
                        c.drawBitmap(low, null, dst, blit);
                        if (game.crt() && p >= 2) {
                            int t = Math.max(1, p / 3);
                            for (int y = 1; y <= vh; y++) c.drawRect(0, y * p - t, sw, y * p, scan);
                        }
                    } finally {
                        try { h.unlockCanvasAndPost(c); } catch (Exception ignored) { }
                    }
                }
            }
            long spent = (System.nanoTime() - now) / 1000000L;
            if (spent < 15) {
                try { Thread.sleep(15 - spent); } catch (InterruptedException ignored) { }
            }
        }
    }

    private void handle(float[] e) {
        switch ((int) e[0]) {
            case DOWN: game.pointerDown((int) e[1], e[2], e[3]); break;
            case MOVE: game.pointerMove((int) e[1], e[2], e[3]); break;
            case UP: game.pointerUp((int) e[1], e[2], e[3]); break;
            case CANCEL: game.pointerCancel(); break;
            case RESIZE: {
                sw = (int) e[2];
                sh = (int) e[3];
                int p = px;
                vw = (sw + p - 1) / p;
                vh = (sh + p - 1) / p;
                low = Bitmap.createBitmap(vw, vh, Bitmap.Config.ARGB_8888);
                lowCanvas = new Canvas(low);
                game.resize(vw, vh, density / p);
                break;
            }
            case PAUSE: game.pause(); break;
            case BACK:
                if (!game.back()) {
                    activity.runOnUiThread(new Runnable() {
                        @Override public void run() { activity.finish(); }
                    });
                }
                break;
        }
    }

    @Override public boolean onTouchEvent(MotionEvent ev) {
        float p = px;
        int action = ev.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN: {
                int i = ev.getActionIndex();
                events.add(new float[]{DOWN, ev.getPointerId(i), ev.getX(i) / p, ev.getY(i) / p});
                break;
            }
            case MotionEvent.ACTION_MOVE:
                for (int i = 0; i < ev.getPointerCount(); i++)
                    events.add(new float[]{MOVE, ev.getPointerId(i), ev.getX(i) / p, ev.getY(i) / p});
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP: {
                int i = ev.getActionIndex();
                events.add(new float[]{UP, ev.getPointerId(i), ev.getX(i) / p, ev.getY(i) / p});
                break;
            }
            case MotionEvent.ACTION_CANCEL:
                events.add(new float[]{CANCEL, 0, 0, 0});
                break;
        }
        return true;
    }
}
