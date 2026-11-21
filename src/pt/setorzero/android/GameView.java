package pt.setorzero.android;

import android.app.Activity;
import android.graphics.Canvas;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.concurrent.ConcurrentLinkedQueue;

import pt.setorzero.core.Game;

/** Superfície do jogo: um fio próprio corre a lógica e desenha; os toques chegam por uma fila. */
final class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    private static final int DOWN = 0, MOVE = 1, UP = 2, CANCEL = 3, BACK = 4, RESIZE = 5, PAUSE = 6;

    private final Activity activity;
    private final Game game;
    private final AndroidGfx gfx = new AndroidGfx();
    private final ConcurrentLinkedQueue<float[]> events = new ConcurrentLinkedQueue<>();
    private final float density;
    private Thread thread;
    private volatile boolean running;

    GameView(Activity activity, AndroidSfx sfx) {
        super(activity);
        this.activity = activity;
        this.density = getResources().getDisplayMetrics().density;
        this.game = new Game(sfx);
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
            SurfaceHolder h = getHolder();
            Canvas c = null;
            try {
                c = h.lockHardwareCanvas();
            } catch (Exception ex) {
                c = null;
            }
            if (c == null) {
                try { c = h.lockCanvas(); } catch (Exception ex) { c = null; }
            }
            if (c != null) {
                try {
                    gfx.set(c);
                    game.render(gfx);
                } finally {
                    try { h.unlockCanvasAndPost(c); } catch (Exception ignored) { }
                }
            }
            long spent = (System.nanoTime() - now) / 1000000L;
            if (spent < 8) {
                try { Thread.sleep(8 - spent); } catch (InterruptedException ignored) { }
            }
        }
    }

    private void handle(float[] e) {
        switch ((int) e[0]) {
            case DOWN: game.pointerDown((int) e[1], e[2], e[3]); break;
            case MOVE: game.pointerMove((int) e[1], e[2], e[3]); break;
            case UP: game.pointerUp((int) e[1], e[2], e[3]); break;
            case CANCEL: game.pointerCancel(); break;
            case RESIZE: game.resize(e[2], e[3], density); break;
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
        int action = ev.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN: {
                int i = ev.getActionIndex();
                events.add(new float[]{DOWN, ev.getPointerId(i), ev.getX(i), ev.getY(i)});
                break;
            }
            case MotionEvent.ACTION_MOVE:
                for (int i = 0; i < ev.getPointerCount(); i++)
                    events.add(new float[]{MOVE, ev.getPointerId(i), ev.getX(i), ev.getY(i)});
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP: {
                int i = ev.getActionIndex();
                events.add(new float[]{UP, ev.getPointerId(i), ev.getX(i), ev.getY(i)});
                break;
            }
            case MotionEvent.ACTION_CANCEL:
                events.add(new float[]{CANCEL, 0, 0, 0});
                break;
        }
        return true;
    }
}
