package org.dorfikstrap.app;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Outline;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import rikka.shizuku.Shizuku;

public class LauncherActivity extends BaseActivity {
    private TextView status;
    private MaterialButton allow;
    private FlagStore store;

    private final Shizuku.OnRequestPermissionResultListener permListener =
            (code, result) -> runOnUiThread(this::refresh);
    private final Shizuku.OnBinderReceivedListener binderListener =
            () -> runOnUiThread(this::refresh);
    private final Shizuku.OnBinderDeadListener deadListener =
            () -> runOnUiThread(this::refresh);

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        store = new FlagStore(this);

        LinearLayout root = new LinearLayout(this);
        root.setGravity(Gravity.CENTER);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        col.setPadding(dp(24), dp(24), dp(24), dp(24));
        root.addView(col, new LinearLayout.LayoutParams(Math.min(dp(420),
                getResources().getDisplayMetrics().widthPixels), -2));

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.ic_fg0);
        logo.setClipToOutline(true);
        logo.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View v, Outline o) { o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), dp(28)); }
        });
        col.addView(logo, new LinearLayout.LayoutParams(dp(96), dp(96)));

        TextView title = new TextView(this);
        title.setText(R.string.app_name);
        title.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_HeadlineMedium);
        title.setPadding(0, dp(12), 0, dp(20));
        col.addView(title);

        col.addView(big(t("Launch Roblox", "Запуск Roblox"), false, v -> {
            if (!Roblox.installed(this)) { toast(t("Roblox is not installed", "Roblox не встановлено")); return; }
            if (!Sh.ready()) toast(t("Shizuku is not ready: launching without flags", "Shizuku не готовий: запуск без прапорів"));
            Roblox.applyAndLaunch(this, store, true, null);
        }));
        col.addView(big(t("Launch Roblox Studio", "Запуск Roblox Studio"), true, v ->
                new MaterialAlertDialogBuilder(this)
                        .setTitle("Roblox Studio")
                        .setMessage(t("Roblox Studio exists only for Windows and macOS; there is no Android version. A port would need a Windows compatibility layer, which is a big research project. Not available yet.",
                                "Roblox Studio існує лише для Windows і macOS, версії для Android немає. Порт потребував би шару сумісності з Windows, це окремий великий дослідницький проєкт. Поки недоступно."))
                        .setPositiveButton(android.R.string.ok, null).show()));
        col.addView(big(t("Settings", "Налаштування"), true, v ->
                startActivity(new Intent(this, SettingsActivity.class))));

        status = new TextView(this);
        status.setPadding(0, dp(16), 0, 0);
        status.setGravity(Gravity.CENTER);
        col.addView(status);
        allow = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        allow.setText(t("Allow Shizuku", "Дозволити Shizuku"));
        allow.setOnClickListener(v -> {
            try {
                if (Shizuku.pingBinder() && !Shizuku.isPreV11()) Shizuku.requestPermission(1);
            } catch (Throwable e) {
                toast(String.valueOf(e));
            }
        });
        col.addView(allow);

        setContentView(root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets i = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(i.left, i.top, i.right, i.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        Shizuku.addRequestPermissionResultListener(permListener);
        Shizuku.addBinderReceivedListenerSticky(binderListener);
        Shizuku.addBinderDeadListener(deadListener);
    }

    private MaterialButton big(String text, boolean tonal, View.OnClickListener l) {
        MaterialButton bt = new MaterialButton(this, null, tonal
                ? com.google.android.material.R.attr.materialButtonOutlinedStyle
                : com.google.android.material.R.attr.materialButtonStyle);
        bt.setText(text);
        bt.setTextSize(18);
        bt.setOnClickListener(l);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(64));
        lp.topMargin = dp(10);
        bt.setLayoutParams(lp);
        return bt;
    }

    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }

    @Override
    protected void onResume() {
        super.onResume();
        store = new FlagStore(this);
        refresh();
    }

    @Override
    protected void onDestroy() {
        Shizuku.removeRequestPermissionResultListener(permListener);
        Shizuku.removeBinderReceivedListener(binderListener);
        Shizuku.removeBinderDeadListener(deadListener);
        super.onDestroy();
    }

    private void refresh() {
        String s;
        boolean needAllow = false;
        try {
            if (!Shizuku.pingBinder())
                s = t("Shizuku is not running. Start it to apply FastFlags.", "Shizuku не запущено. Запусти його, щоб застосовувати FastFlags.");
            else if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                s = t("Shizuku needs permission.", "Shizuku потребує дозволу.");
                needAllow = true;
            } else s = t("Shizuku is ready. Flags on: ", "Shizuku готовий. Прапорів увімкнено: ") + (store.master() ? store.enabledCount() : 0);
        } catch (Throwable e) {
            s = t("Shizuku is not installed.", "Shizuku не встановлено.");
        }
        status.setText(s);
        allow.setVisibility(needAllow ? View.VISIBLE : View.GONE);
    }
}
