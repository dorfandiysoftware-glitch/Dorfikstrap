package org.dorfikstrap.app;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.Locale;

import rikka.shizuku.Shizuku;

public class MainActivity extends AppCompatActivity {
    private static final int REQ = 1;

    private final boolean uk = Locale.getDefault().getLanguage().equals("uk");
    private FlagStore store;
    private FrameLayout content;
    private View[] pages = new View[4];
    private TextView status, flagsOut, report;
    private LinearLayout flagList;
    private volatile boolean busy;

    private final Shizuku.OnRequestPermissionResultListener permListener =
            (code, result) -> runOnUiThread(this::refresh);
    private final Shizuku.OnBinderReceivedListener binderListener =
            () -> runOnUiThread(this::refresh);
    private final Shizuku.OnBinderDeadListener deadListener =
            () -> runOnUiThread(this::refresh);

    private String t(String en, String ukr) { return uk ? ukr : en; }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        store = new FlagStore(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        content = new FrameLayout(this);
        root.addView(content, new LinearLayout.LayoutParams(-1, 0, 1f));
        BottomNavigationView nav = new BottomNavigationView(this);
        nav.inflateMenu(R.menu.nav);
        root.addView(nav, new LinearLayout.LayoutParams(-1, -2));
        setContentView(root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            androidx.core.graphics.Insets i = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(i.left, i.top, i.right, i.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        pages[0] = buildMain();
        pages[1] = buildFlags();
        pages[2] = buildMods();
        pages[3] = buildOther();
        for (View p : pages) {
            p.setVisibility(View.GONE);
            content.addView(p, new FrameLayout.LayoutParams(-1, -1));
        }
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            int idx = id == R.id.nav_main ? 0 : id == R.id.nav_flags ? 1 : id == R.id.nav_mods ? 2 : 3;
            for (int i = 0; i < 4; i++) pages[i].setVisibility(i == idx ? View.VISIBLE : View.GONE);
            return true;
        });
        nav.setSelectedItemId(R.id.nav_main);

        Shizuku.addRequestPermissionResultListener(permListener);
        Shizuku.addBinderReceivedListenerSticky(binderListener);
        Shizuku.addBinderDeadListener(deadListener);
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

    // ---------- helpers ----------

    private LinearLayout column() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(16), dp(16), dp(16), dp(16));
        return l;
    }

    private ScrollView scroll(LinearLayout l) {
        ScrollView s = new ScrollView(this);
        s.addView(l);
        return s;
    }

    private TextView title(String s) {
        TextView v = new TextView(this);
        v.setText(s);
        v.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_HeadlineMedium);
        return v;
    }

    private TextView body(String s) {
        TextView v = new TextView(this);
        v.setText(s);
        v.setPadding(0, dp(8), 0, dp(8));
        return v;
    }

    private TextView mono() {
        TextView v = new TextView(this);
        v.setTypeface(android.graphics.Typeface.MONOSPACE);
        v.setTextSize(11);
        v.setTextIsSelectable(true);
        v.setPadding(dp(8), dp(8), dp(8), dp(8));
        return v;
    }

    private MaterialButton button(String s, View.OnClickListener l, boolean tonal) {
        MaterialButton b = new MaterialButton(this, null, tonal
                ? com.google.android.material.R.attr.materialButtonOutlinedStyle
                : com.google.android.material.R.attr.materialButtonStyle);
        b.setText(s);
        b.setOnClickListener(l);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(8);
        b.setLayoutParams(lp);
        return b;
    }

    private boolean needReady() {
        if (Sh.ready()) return true;
        refresh();
        Toast.makeText(this, t("Shizuku is not ready", "Shizuku не готовий"), Toast.LENGTH_SHORT).show();
        return false;
    }

    /** Runs the shell work off the UI thread and shows the result in the given view. */
    private void async(TextView out, java.util.function.Supplier<String> work) {
        if (busy) return;
        if (!needReady()) return;
        busy = true;
        out.setText(t("Running…", "Виконується…"));
        new Thread(() -> {
            String r;
            try { r = work.get(); } catch (Throwable e) { r = String.valueOf(e); }
            final String res = r;
            runOnUiThread(() -> { out.setText(res); busy = false; });
        }).start();
    }

    private boolean robloxInstalled() {
        try {
            getPackageManager().getPackageInfo(Sh.PKG, 0);
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    private void launchRoblox() {
        Intent i = getPackageManager().getLaunchIntentForPackage(Sh.PKG);
        if (i == null) Toast.makeText(this, t("Roblox is not installed", "Roblox не встановлено"), Toast.LENGTH_SHORT).show();
        else startActivity(i);
    }

    private void refresh() {
        String s;
        try {
            if (!Shizuku.pingBinder())
                s = t("Shizuku is not running. Start it and come back.", "Shizuku не запущено. Запусти його і повернись.");
            else if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED)
                s = t("Shizuku is running. Tap “Allow Shizuku”.", "Shizuku працює. Натисни «Дозволити Shizuku».");
            else s = t("Shizuku is ready.", "Shizuku готовий.");
        } catch (Throwable e) {
            s = t("Shizuku is not installed.", "Shizuku не встановлено.");
        }
        status.setText(s + "\nRoblox: " + (robloxInstalled() ? t("installed", "встановлено") : t("not installed", "не встановлено"))
                + "\n" + t("Flags enabled: ", "Увімкнено прапорів: ") + enabledCount());
    }

    private int enabledCount() {
        int n = 0;
        for (FlagStore.Flag f : store.flags) if (f.enabled) n++;
        return n;
    }

    // ---------- Main ----------

    private View buildMain() {
        LinearLayout l = column();
        l.addView(title("Dorfikstrap"));
        status = body("");
        l.addView(status);
        l.addView(button(t("Allow Shizuku", "Дозволити Shizuku"), v -> {
            try {
                if (Shizuku.pingBinder() && !Shizuku.isPreV11()) Shizuku.requestPermission(REQ);
            } catch (Throwable e) {
                Toast.makeText(this, String.valueOf(e), Toast.LENGTH_LONG).show();
            }
        }, true));
        l.addView(button(t("Launch Roblox", "Запустити Roblox"), v -> launchRoblox(), false));
        TextView out = mono();
        l.addView(button(t("Apply flags and restart Roblox", "Застосувати прапори і перезапустити Roblox"), v -> {
            if (!needReady()) return;
            async(out, () -> {
                String r = Sh.runLogged(store.writeCommand()) + Sh.runLogged("am force-stop " + Sh.PKG);
                runOnUiThread(this::launchRoblox);
                return r;
            });
        }, true));
        l.addView(out);
        l.addView(body(t("Dorfikstrap does not modify the Roblox client. Flags are written to a settings file that Roblox itself reads at start. Not affiliated with Roblox Corporation.",
                "Dorfikstrap не змінює клієнт Roblox. Прапори записуються у файл налаштувань, який Roblox сам читає при запуску. Не пов'язаний з Roblox Corporation.")));
        return scroll(l);
    }

    // ---------- FastFlags ----------

    private View buildFlags() {
        LinearLayout l = column();
        l.addView(title("FastFlags"));
        l.addView(body(t("Flags the client does not know are ignored. Roblox may block some flags; use at your own risk.",
                "Невідомі клієнту прапори ігноруються. Roblox може блокувати частину прапорів; ризик на тобі.")));
        flagList = new LinearLayout(this);
        flagList.setOrientation(LinearLayout.VERTICAL);
        l.addView(flagList);
        renderFlags();
        l.addView(button(t("Add flag", "Додати прапор"), v -> editFlag(null), true));
        l.addView(button(t("Presets", "Пресети"), v -> {
            String[] items = new String[FlagStore.PRESETS.length];
            for (int i = 0; i < items.length; i++)
                items[i] = FlagStore.PRESETS[i][2] + "\n" + FlagStore.PRESETS[i][0] + " = " + FlagStore.PRESETS[i][1];
            new MaterialAlertDialogBuilder(this).setTitle(t("Presets", "Пресети")).setItems(items, (d, w) -> {
                store.put(FlagStore.PRESETS[w][0], FlagStore.PRESETS[w][1]);
                renderFlags();
                refresh();
            }).show();
        }, true));
        flagsOut = mono();
        l.addView(button(t("Apply", "Застосувати"), v -> async(flagsOut, () -> Sh.runLogged(store.writeCommand())), false));
        l.addView(button(t("Apply and restart Roblox", "Застосувати і перезапустити Roblox"), v -> {
            async(flagsOut, () -> {
                String r = Sh.runLogged(store.writeCommand()) + Sh.runLogged("am force-stop " + Sh.PKG);
                runOnUiThread(this::launchRoblox);
                return r;
            });
        }, false));
        l.addView(button(t("Check in logs", "Перевірити в логах"), v -> async(flagsOut, () -> {
            StringBuilder sb = new StringBuilder();
            sb.append(Sh.runLogged("ls -la " + Sh.FLAGS_FILE));
            sb.append(Sh.runLogged("logcat -d -b all | grep -E 'readLocalSettings|ClientAppSettings' | tail -5"));
            for (FlagStore.Flag f : store.flags)
                if (f.enabled)
                    sb.append(Sh.runLogged("logcat -d -b all | grep -F '" + f.name + "' | tail -3"));
            return sb.toString();
        }), true));
        l.addView(button(t("Delete flag file (reset)", "Видалити файл прапорів (скинути)"), v ->
                async(flagsOut, () -> Sh.runLogged("rm -f " + Sh.FLAGS_FILE + "; ls -la " + Sh.FLAGS_FILE)), true));
        l.addView(flagsOut);
        return scroll(l);
    }

    private void renderFlags() {
        flagList.removeAllViews();
        if (store.flags.isEmpty()) flagList.addView(body(t("No flags yet.", "Прапорів ще немає.")));
        for (FlagStore.Flag f : store.flags) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            TextView tv = new TextView(this);
            tv.setText(f.name + "\n= " + f.value);
            row.addView(tv, new LinearLayout.LayoutParams(0, -2, 1f));
            tv.setOnClickListener(v -> editFlag(f));
            tv.setOnLongClickListener(v -> {
                new MaterialAlertDialogBuilder(this).setTitle(f.name)
                        .setMessage(t("Delete this flag?", "Видалити цей прапор?"))
                        .setPositiveButton(t("Delete", "Видалити"), (d, w) -> {
                            store.flags.remove(f);
                            store.save();
                            renderFlags();
                            refresh();
                        }).setNegativeButton(android.R.string.cancel, null).show();
                return true;
            });
            MaterialSwitch sw = new MaterialSwitch(this);
            sw.setChecked(f.enabled);
            sw.setOnCheckedChangeListener((b, c) -> { f.enabled = c; store.save(); refresh(); });
            row.addView(sw);
            row.setPadding(0, dp(6), 0, dp(6));
            flagList.addView(row);
        }
    }

    private void editFlag(FlagStore.Flag f) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(24), dp(8), dp(24), 0);
        EditText name = new EditText(this);
        name.setHint(t("Flag name, e.g. FFlagDebugDisplayFPS", "Назва прапора, напр. FFlagDebugDisplayFPS"));
        name.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        EditText value = new EditText(this);
        value.setHint(t("Value, e.g. true or 120", "Значення, напр. true або 120"));
        value.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        if (f != null) { name.setText(f.name); name.setEnabled(false); value.setText(f.value); }
        box.addView(name);
        box.addView(value);
        new MaterialAlertDialogBuilder(this).setTitle(f == null ? t("Add flag", "Додати прапор") : f.name)
                .setView(box)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    if (!store.put(name.getText().toString().trim(), value.getText().toString().trim()))
                        Toast.makeText(this, t("Invalid name or value", "Недопустима назва або значення"), Toast.LENGTH_SHORT).show();
                    renderFlags();
                    refresh();
                }).setNegativeButton(android.R.string.cancel, null).show();
    }

    // ---------- Modloader ----------

    private View buildMods() {
        LinearLayout l = column();
        l.addView(title("Modloader"));
        l.addView(body(t("Not available yet. Roblox keeps its content (textures, sounds, fonts) inside its private folder or the APK, and Shizuku (shell) cannot write there. The public folder Android/data/com.roblox.client/files exists but is empty so far.\n\nBelow you can check what the app sees there.",
                "Поки недоступний. Roblox тримає свій контент (текстури, звуки, шрифти) у приватній папці або в APK, а Shizuku (shell) туди писати не може. Публічна папка Android/data/com.roblox.client/files існує, але поки порожня.\n\nНижче можна перевірити, що додаток там бачить.")));
        TextView out = mono();
        l.addView(button(t("Inspect Roblox folders", "Переглянути папки Roblox"), v -> async(out, () ->
                Sh.runLogged("ls -laR /sdcard/Android/data/" + Sh.PKG + " | head -80")
                        + Sh.runLogged("ls -la /sdcard/Android/media/" + Sh.PKG)), true));
        l.addView(out);
        return scroll(l);
    }

    // ---------- Other ----------

    private View buildOther() {
        LinearLayout l = column();
        l.addView(title(t("Other", "Інше")));
        report = mono();
        l.addView(button(t("Roblox log (last lines)", "Лог Roblox (останні рядки)"), v -> async(report, () ->
                Sh.runLogged("logcat -d -s Roblox:I | tail -120")), true));
        l.addView(button(t("Run diagnostics", "Запустити діагностику"), v -> async(report, this::diag), true));
        l.addView(button(t("Copy", "Копіювати"), v -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("report", report.getText()));
            Toast.makeText(this, t("Copied", "Скопійовано"), Toast.LENGTH_SHORT).show();
        }, true));
        l.addView(button(t("Share", "Поділитись"), v -> startActivity(Intent.createChooser(
                new Intent(Intent.ACTION_SEND).setType("text/plain")
                        .putExtra(Intent.EXTRA_TEXT, report.getText().toString()), null)), true));
        l.addView(report);
        return scroll(l);
    }

    private String diag() {
        String P = Sh.PKG, d = "/sdcard/Android/data/" + P;
        String[] cmds = {
                "id",
                "pm path " + P,
                "dumpsys package " + P + " | grep -E 'versionName|versionCode|targetSdk|DEBUGGABLE'",
                "ls -la " + d,
                "find " + d + " -maxdepth 5 | head -100",
                "ps -A | grep -i roblox",
                "ls -la " + Sh.FLAGS_FILE,
                "logcat -d -b all | grep -iE 'readLocalSettings|ClientAppSettings' | tail -20",
        };
        StringBuilder out = new StringBuilder("Dorfikstrap diagnostics, Android " + Build.VERSION.RELEASE + " / " + Build.MODEL + "\n\n");
        for (String c : cmds) out.append(Sh.runLogged(c));
        return out.toString();
    }
}
