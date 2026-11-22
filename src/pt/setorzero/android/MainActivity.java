package pt.setorzero.android;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;

import pt.setorzero.core.Net;
import pt.setorzero.core.Store;

public final class MainActivity extends Activity {
    private GameView view;
    private AndroidSfx sfx;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        sfx = new AndroidSfx(this);
        final SharedPreferences prefs = getSharedPreferences("setorzero", MODE_PRIVATE);
        Store store = new Store() {
            @Override public int get(String key, int def) { return prefs.getInt(key, def); }
            @Override public void put(String key, int value) { prefs.edit().putInt(key, value).apply(); }
        };
        view = new GameView(this, sfx, store);
        // alguns telemóveis só entregam difusões UDP (procura de jogos) com esta autorização ativa
        final WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifi != null) {
            final WifiManager.MulticastLock lock = wifi.createMulticastLock("setorzero");
            lock.setReferenceCounted(false);
            Net.hook = new Net.Hook() {
                @Override public void listening(boolean on) {
                    try { if (on) lock.acquire(); else lock.release(); } catch (Exception ignored) { }
                }
            };
        }
        setContentView(view);
        immersive();
    }

    @SuppressWarnings("deprecation")
    private void immersive() {
        if (Build.VERSION.SDK_INT >= 30) {
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController c = getWindow().getInsetsController();
            if (c != null) {
                c.hide(WindowInsets.Type.systemBars());
                c.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }
    }

    @Override public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) immersive();
    }

    @Override protected void onPause() {
        super.onPause();
        view.postPause();
        sfx.setMuted(true);
    }

    @Override protected void onResume() {
        super.onResume();
        sfx.setMuted(false);
        immersive();
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        sfx.release();
    }

    @SuppressWarnings("deprecation")
    @Override public void onBackPressed() {
        view.postBack();
    }
}
