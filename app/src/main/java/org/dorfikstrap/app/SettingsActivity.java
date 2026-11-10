package org.dorfikstrap.app;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.Insets;
import androidx.core.graphics.drawable.IconCompat;
import androidx.core.os.LocaleListCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.navigationrail.NavigationRailView;
import com.google.android.material.tabs.TabLayout;

public class SettingsActivity extends BaseActivity {
    private FlagStore store;
    private final View[] pages = new View[4];
    private LinearLayout flagList;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        store = new FlagStore(this);

        LinearLayout root = new LinearLayout(this);
        NavigationRailView rail = new NavigationRailView(this);
        rail.setLabelVisibilityMode(NavigationRailView.LABEL_VISIBILITY_LABELED);
        Menu m = rail.getMenu();
        String[] names = {"FastFlags", getString(R.string.tab_integrations),
                getString(R.string.tab_appearance), getString(R.string.tab_shortcuts)};
        int[] icons = {android.R.drawable.ic_menu_edit, android.R.drawable.ic_menu_share,
                android.R.drawable.ic_menu_view, android.R.drawable.ic_menu_add};
        for (int i = 0; i < 4; i++) m.add(Menu.NONE, i, i, names[i]).setIcon(icons[i]);
        root.addView(rail, new LinearLayout.LayoutParams(dp(84), -1));

        LinearLayout right = new LinearLayout(this);
        right.setOrientation(LinearLayout.VERTICAL);
        FrameLayout content = new FrameLayout(this);
        right.addView(content, new LinearLayout.LayoutParams(-1, 0, 1f));
        right.addView(actionBar());
        root.addView(right, new LinearLayout.LayoutParams(0, -1, 1f));

        pages[0] = buildFlags();
        pages[1] = buildIntegrations();
        pages[2] = buildAppearance();
        pages[3] = buildShortcuts();
        for (View p : pages) {
            p.setVisibility(View.GONE);
            content.addView(p, new FrameLayout.LayoutParams(-1, -1));
        }
        rail.setOnItemSelectedListener(item -> {
            for (int i = 0; i < 4; i++) pages[i].setVisibility(i == item.getItemId() ? View.VISIBLE : View.GONE);
            return true;
        });
        int start = b == null ? 0 : b.getInt("tab", 0);
        rail.setSelectedItemId(start);
        tab = rail;

        setContentView(root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets i = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(i.left, i.top, i.right, i.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    private NavigationRailView tab;

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putInt("tab", tab.getSelectedItemId());
    }

    // ---------- helpers ----------

    private LinearLayout column() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(16), dp(16), dp(16), dp(16));
        return l;
    }

    private View scroll(LinearLayout l) {
        ScrollView s = new ScrollView(this);
        s.addView(l);
        return s;
    }

    private TextView title(String s) {
        TextView v = new TextView(this);
        v.setText(s);
        v.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_HeadlineSmall);
        v.setPadding(0, 0, 0, dp(8));
        return v;
    }

    private TextView body(String s) {
        TextView v = new TextView(this);
        v.setText(s);
        v.setPadding(0, dp(6), 0, dp(6));
        return v;
    }

    private MaterialButton button(String s, boolean filled, View.OnClickListener l) {
        MaterialButton bt = new MaterialButton(this, null, filled
                ? com.google.android.material.R.attr.materialButtonStyle
                : com.google.android.material.R.attr.materialButtonOutlinedStyle);
        bt.setText(s);
        bt.setOnClickListener(l);
        return bt;
    }

    private LinearLayout.LayoutParams top(int dpTop) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(dpTop);
        return lp;
    }

    private MaterialSwitch row(LinearLayout parent, String text, boolean checked, boolean enabled,
                               android.widget.CompoundButton.OnCheckedChangeListener l) {
        MaterialSwitch sw = new MaterialSwitch(this);
        sw.setText(text);
        sw.setChecked(checked);
        sw.setEnabled(enabled);
        if (l != null) sw.setOnCheckedChangeListener(l);
        sw.setPadding(0, dp(6), 0, dp(6));
        parent.addView(sw, top(0));
        return sw;
    }

    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }

    // ---------- bottom-right action buttons ----------

    private View actionBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(8), dp(4), dp(8), dp(8));
        MaterialButton apply = button(getString(R.string.apply), false, v -> {
            if (!Sh.ready()) { toast(getString(R.string.shizuku_not_ready)); return; }
            new Thread(() -> {
                Roblox.apply(store);
                runOnUiThread(() -> toast(getString(R.string.applied)));
            }).start();
        });
        MaterialButton launch = button(getString(R.string.apply_launch), true, v ->
                Roblox.applyAndLaunch(this, store, true, null));
        MaterialButton close = button(getString(R.string.close), false, v -> finish());
        for (MaterialButton bt : new MaterialButton[]{apply, launch, close}) {
            bt.setTextSize(12);
            bt.setInsetTop(0);
            bt.setInsetBottom(0);
            bt.setMinimumWidth(0);
            bt.setPadding(dp(10), 0, dp(10), 0);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(48), 1f);
            lp.leftMargin = dp(4);
            bar.addView(bt, lp);
        }
        return bar;
    }

    // ---------- FastFlags ----------

    private View buildFlags() {
        LinearLayout l = column();
        l.addView(title("FastFlags"));
        row(l, getString(R.string.use_flags), store.master(), true, (v, c) -> store.setMaster(c));
        MaterialButton add = button(getString(R.string.add_flag), true, v -> addDialog());
        add.setTextSize(16);
        LinearLayout.LayoutParams lp = top(8);
        lp.height = dp(60);
        l.addView(add, lp);
        l.addView(button(getString(R.string.delete_all), false, v ->
                new MaterialAlertDialogBuilder(this)
                        .setTitle(getString(R.string.delete_all_q))
                        .setMessage(getString(R.string.delete_all_msg))
                        .setPositiveButton(getString(R.string.delete), (d, w) -> {
                            store.flags.clear();
                            store.save();
                            renderFlags();
                            if (Sh.ready()) new Thread(() -> Sh.run("rm -f " + Sh.FLAGS_FILE)).start();
                        }).setNegativeButton(android.R.string.cancel, null).show()), top(8));
        l.addView(body(getString(R.string.flags_hint)));
        flagList = new LinearLayout(this);
        flagList.setOrientation(LinearLayout.VERTICAL);
        l.addView(flagList);
        renderFlags();
        return scroll(l);
    }

    private void renderFlags() {
        flagList.removeAllViews();
        if (store.flags.isEmpty()) flagList.addView(body(getString(R.string.no_flags)));
        for (FlagStore.Flag f : store.flags) {
            LinearLayout r = new LinearLayout(this);
            r.setGravity(Gravity.CENTER_VERTICAL);
            r.setPadding(0, dp(6), 0, dp(6));
            TextView tv = new TextView(this);
            tv.setText(f.name + "\n= " + f.value);
            r.addView(tv, new LinearLayout.LayoutParams(0, -2, 1f));
            tv.setOnClickListener(v -> editDialog(f));
            tv.setOnLongClickListener(v -> {
                new MaterialAlertDialogBuilder(this).setTitle(f.name)
                        .setMessage(getString(R.string.delete_flag_q))
                        .setPositiveButton(getString(R.string.delete), (d, w) -> {
                            store.flags.remove(f);
                            store.save();
                            renderFlags();
                        }).setNegativeButton(android.R.string.cancel, null).show();
                return true;
            });
            MaterialSwitch sw = new MaterialSwitch(this);
            sw.setChecked(f.enabled);
            sw.setOnCheckedChangeListener((bt, c) -> { f.enabled = c; store.save(); });
            r.addView(sw);
            flagList.addView(r);
        }
    }

    private EditText edit(String hint, boolean multiline) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                | (multiline ? InputType.TYPE_TEXT_FLAG_MULTI_LINE : 0));
        if (multiline) { e.setMinLines(6); e.setGravity(Gravity.TOP); }
        return e;
    }

    private void editDialog(FlagStore.Flag f) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(24), dp(8), dp(24), 0);
        EditText name = edit("", false);
        name.setText(f.name);
        name.setEnabled(false);
        EditText value = edit(getString(R.string.value), false);
        value.setText(f.value);
        box.addView(name);
        box.addView(value);
        new MaterialAlertDialogBuilder(this).setTitle(getString(R.string.edit_flag)).setView(box)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    if (!store.put(f.name, value.getText().toString().trim()))
                        toast(getString(R.string.invalid_value));
                    renderFlags();
                }).setNegativeButton(android.R.string.cancel, null).show();
    }

    private void addDialog() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        TabLayout tabs = new TabLayout(this);
        tabs.addTab(tabs.newTab().setText(getString(R.string.name_value)));
        tabs.addTab(tabs.newTab().setText("JSON"));
        box.addView(tabs);
        FrameLayout panes = new FrameLayout(this);
        panes.setPadding(dp(24), dp(12), dp(24), 0);
        LinearLayout p1 = new LinearLayout(this);
        p1.setOrientation(LinearLayout.VERTICAL);
        EditText name = edit(getString(R.string.hint_name), false);
        EditText value = edit(getString(R.string.hint_value), false);
        p1.addView(name);
        p1.addView(value);
        EditText json = edit("{\"FFlagDebugDisplayFPS\": \"true\"}", true);
        json.setVisibility(View.GONE);
        panes.addView(p1);
        panes.addView(json);
        box.addView(panes);
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tb) {
                p1.setVisibility(tb.getPosition() == 0 ? View.VISIBLE : View.GONE);
                json.setVisibility(tb.getPosition() == 1 ? View.VISIBLE : View.GONE);
            }
            @Override public void onTabUnselected(TabLayout.Tab tb) {}
            @Override public void onTabReselected(TabLayout.Tab tb) {}
        });
        new MaterialAlertDialogBuilder(this).setTitle(getString(R.string.add_flag)).setView(box)
                .setPositiveButton(getString(R.string.add), (d, w) -> {
                    if (tabs.getSelectedTabPosition() == 0) {
                        if (!store.put(name.getText().toString().trim(), value.getText().toString().trim()))
                            toast(getString(R.string.invalid_name_value));
                    } else {
                        int[] r = store.importJson(json.getText().toString());
                        if (r == null) toast(getString(R.string.invalid_json));
                        else toast(r[1] > 0 ? getString(R.string.added_skipped, r[0], r[1]) : getString(R.string.added, r[0]));
                    }
                    renderFlags();
                }).setNegativeButton(android.R.string.cancel, null).show();
    }

    // ---------- Integrations ----------

    private View buildIntegrations() {
        LinearLayout l = column();
        l.addView(title(getString(R.string.tab_integrations)));
        l.addView(body("Discord"));
        row(l, getString(R.string.discord_rpc), prefs.discord(), true, (v, c) -> prefs.setDiscord(c));
        EditText id = edit(getString(R.string.discord_id), false);
        id.setInputType(InputType.TYPE_CLASS_NUMBER);
        id.setText(prefs.discordId());
        id.setOnFocusChangeListener((v, focus) -> { if (!focus) prefs.setDiscordId(id.getText().toString().trim()); });
        l.addView(id, top(4));
        l.addView(body(getString(R.string.discord_note)));
        return scroll(l);
    }

    // ---------- Appearance ----------

    private View buildAppearance() {
        LinearLayout l = column();
        l.addView(title(getString(R.string.tab_appearance)));

        l.addView(body(getString(R.string.language)));
        RadioGroup g = new RadioGroup(this);
        String[][] langs = {{"", getString(R.string.system)}, {"en", "English"}, {"uk", "Українська"},
                {"ru", "Русский"}, {"pl", "Polski"}, {"de", "Deutsch"}, {"es", "Español"},
                {"pt-BR", "Português (Brasil)"}, {"tr", "Türkçe"}};
        String cur = AppCompatDelegate.getApplicationLocales().toLanguageTags();
        for (int i = 0; i < langs.length; i++) {
            RadioButton rb = new RadioButton(this);
            rb.setId(100 + i);
            rb.setText(langs[i][1]);
            g.addView(rb);
            if (langs[i][0].equals(cur) || (i == 0 && cur.isEmpty())) rb.setChecked(true);
        }
        g.setOnCheckedChangeListener((grp, id) -> AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(langs[id - 100][0])));
        l.addView(g);

        MaterialSwitch amoled = row(l, "AMOLED", prefs.amoled() || !prefs.md3(), prefs.md3(), (v, c) -> {
            prefs.setAmoled(c);
            restyle();
        });
        row(l, getString(R.string.md3_colours), prefs.md3(), true, (v, c) -> {
            prefs.setMd3(c);
            amoled.setEnabled(c);
            restyle();
        });
        l.addView(body(getString(R.string.md3_note)));

        l.addView(body(getString(R.string.app_icon)));
        LinearLayout icons = new LinearLayout(this);
        int[] res = {R.drawable.ic_fg0, R.drawable.ic_fg1, R.drawable.ic_fg2};
        ImageView[] views = new ImageView[3];
        for (int i = 0; i < 3; i++) {
            final int idx = i;
            ImageView iv = new ImageView(this);
            iv.setImageResource(res[i]);
            iv.setBackgroundColor(android.graphics.Color.BLACK);
            iv.setAlpha(prefs.icon() == i ? 1f : 0.35f);
            iv.setOnClickListener(v -> {
                setIcon(idx);
                for (int k = 0; k < 3; k++) views[k].setAlpha(k == idx ? 1f : 0.35f);
            });
            views[i] = iv;
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(64), dp(64));
            lp.rightMargin = dp(12);
            icons.addView(iv, lp);
        }
        l.addView(icons);
        l.addView(body(getString(R.string.icon_note)));
        return scroll(l);
    }

    private void restyle() {
        App.applyNight(this);
        recreate();
    }

    private void setIcon(int idx) {
        prefs.setIcon(idx);
        PackageManager pm = getPackageManager();
        for (int pass = 0; pass < 2; pass++)
            for (int i = 0; i < 3; i++) {
                boolean want = i == idx;
                if (want != (pass == 0)) continue; // enable the new one first, then disable the others
                pm.setComponentEnabledSetting(new ComponentName(this, "org.dorfikstrap.app.Alias" + i),
                        want ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP);
            }
    }

    // ---------- Shortcuts ----------

    private View buildShortcuts() {
        LinearLayout l = column();
        l.addView(title(getString(R.string.tab_shortcuts)));
        l.addView(body(getString(R.string.shortcut_info)));
        EditText name = edit(getString(R.string.shortcut_name), false);
        name.setText(prefs.shortcutName());
        l.addView(name, top(4));
        l.addView(button(getString(R.string.create_shortcut), true, v -> {
            String n = name.getText().toString().trim();
            if (n.isEmpty()) n = "Roblox";
            prefs.setShortcutName(n);
            if (!ShortcutManagerCompat.isRequestPinShortcutSupported(this)) {
                toast(getString(R.string.no_pin_support));
                return;
            }
            ShortcutInfoCompat info = new ShortcutInfoCompat.Builder(this, "launch_roblox")
                    .setShortLabel(n)
                    .setIcon(IconCompat.createWithResource(this, R.mipmap.ic_launcher))
                    .setIntent(new Intent(this, LaunchActivity.class).setAction(Intent.ACTION_VIEW))
                    .build();
            ShortcutManagerCompat.requestPinShortcut(this, info, null);
        }), top(8));
        return scroll(l);
    }
}
