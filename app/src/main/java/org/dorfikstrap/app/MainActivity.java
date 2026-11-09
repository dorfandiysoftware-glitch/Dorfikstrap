package org.dorfikstrap.app;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;

import rikka.shizuku.Shizuku;

public class MainActivity extends AppCompatActivity {
    private static final int REQ = 1;
    private static final String PKG = "com.roblox.client";

    private TextView status, report;
    private volatile boolean busy;

    private final Shizuku.OnRequestPermissionResultListener permListener =
            (code, result) -> runOnUiThread(this::refresh);
    private final Shizuku.OnBinderReceivedListener binderListener =
            () -> runOnUiThread(this::refresh);
    private final Shizuku.OnBinderDeadListener deadListener =
            () -> runOnUiThread(this::refresh);

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);
        status = findViewById(R.id.status);
        report = findViewById(R.id.report);
        Shizuku.addRequestPermissionResultListener(permListener);
        Shizuku.addBinderReceivedListenerSticky(binderListener);
        Shizuku.addBinderDeadListener(deadListener);

        findViewById(R.id.grant).setOnClickListener(v -> {
            try {
                if (Shizuku.pingBinder() && !Shizuku.isPreV11()) Shizuku.requestPermission(REQ);
            } catch (Throwable t) {
                report.setText(String.valueOf(t));
            }
        });
        findViewById(R.id.run).setOnClickListener(v -> runDiag());
        findViewById(R.id.copy).setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("report", report.getText()));
            Toast.makeText(this, R.string.copied, Toast.LENGTH_SHORT).show();
        });
        findViewById(R.id.share).setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_SEND).setType("text/plain")
                    .putExtra(Intent.EXTRA_TEXT, report.getText().toString());
            startActivity(Intent.createChooser(i, null));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    protected void onDestroy() {
        Shizuku.removeRequestPermissionResultListener(permListener);
        Shizuku.removeBinderReceivedListener(binderListener);
        Shizuku.removeBinderDeadListener(deadListener);
        super.onDestroy();
    }

    private boolean robloxInstalled() {
        try {
            getPackageManager().getPackageInfo(PKG, 0);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private boolean ready() {
        try {
            return Shizuku.pingBinder() && !Shizuku.isPreV11()
                    && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED;
        } catch (Throwable t) {
            return false;
        }
    }

    private void refresh() {
        int s;
        try {
            if (!Shizuku.pingBinder()) s = R.string.st_not_running;
            else if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED)
                s = R.string.st_need_permission;
            else s = R.string.st_ready;
        } catch (Throwable t) {
            s = R.string.st_not_installed;
        }
        status.setText(getString(s) + "\n" + getString(R.string.st_roblox,
                getString(robloxInstalled() ? R.string.installed : R.string.not_installed)));
    }

    /** Shizuku.newProcess is private in the API; reached by reflection. */
    private static Process shell(String cmd) throws Exception {
        Method m = Shizuku.class.getDeclaredMethod("newProcess",
                String[].class, String[].class, String.class);
        m.setAccessible(true);
        return (Process) m.invoke(null, new Object[]{new String[]{"sh", "-c", cmd}, null, null});
    }

    private static String exec(String cmd) {
        StringBuilder sb = new StringBuilder();
        sb.append("$ ").append(cmd).append('\n');
        try {
            Process p = shell(cmd + " 2>&1");
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            int n = 0;
            while ((line = r.readLine()) != null && n++ < 400) sb.append(line).append('\n');
            sb.append("[exit ").append(p.waitFor()).append("]\n");
        } catch (Throwable t) {
            Throwable c = t.getCause() != null ? t.getCause() : t;
            sb.append("ERROR: ").append(c).append('\n');
        }
        return sb.append('\n').toString();
    }

    private void runDiag() {
        if (busy) return;
        if (!ready()) {
            refresh();
            return;
        }
        busy = true;
        report.setText(R.string.running);
        final String d = "/sdcard/Android/data/" + PKG;
        final String[] cmds = {
                "id",
                "getprop ro.build.version.release; getprop ro.build.version.sdk; getprop ro.product.model",
                "pm path " + PKG,
                "dumpsys package " + PKG + " | grep -E 'versionName|versionCode|flags=|dataDir|targetSdk|DEBUGGABLE|userId'",
                "ls -la " + d,
                "find " + d + " -maxdepth 5 | head -150",
                "find " + d + " \\( -iname '*setting*' -o -iname '*flag*' -o -iname '*.json' -o -iname '*.xml' \\) | head -60",
                "ls -la /sdcard/Android/obb/" + PKG,
                "run-as " + PKG + " id",
                "ls -la /data/data/" + PKG,
                "ls -la /data/user/0/" + PKG,
                "ls -la /data/user/0/" + PKG + "/files",
                "find /data/user/0/" + PKG + " -maxdepth 3 \\( -iname '*setting*' -o -iname '*flag*' -o -iname '*.json' \\) | head -60",
        };
        new Thread(() -> {
            StringBuilder out = new StringBuilder();
            out.append("Dorfikstrap diagnostics, Android ").append(Build.VERSION.RELEASE)
                    .append(" / ").append(Build.MODEL).append("\n\n");
            for (String c : cmds) out.append(exec(c));
            final String res = out.toString();
            runOnUiThread(() -> {
                report.setText(res);
                busy = false;
            });
        }).start();
    }
}
