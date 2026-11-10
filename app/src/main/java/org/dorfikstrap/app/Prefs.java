package org.dorfikstrap.app;

import android.content.Context;
import android.content.SharedPreferences;

final class Prefs {
    private final SharedPreferences sp;

    Prefs(Context c) { sp = c.getSharedPreferences("app", Context.MODE_PRIVATE); }

    boolean md3() { return sp.getBoolean("md3", true); }
    void setMd3(boolean v) { sp.edit().putBoolean("md3", v).apply(); }

    boolean amoled() { return sp.getBoolean("amoled", false); }
    void setAmoled(boolean v) { sp.edit().putBoolean("amoled", v).apply(); }

    /** 0 = default, 1, 2 = alternative launcher icons. */
    int icon() { return sp.getInt("icon", 0); }
    void setIcon(int v) { sp.edit().putInt("icon", v).apply(); }

    boolean discord() { return sp.getBoolean("discord", false); }
    void setDiscord(boolean v) { sp.edit().putBoolean("discord", v).apply(); }

    String discordId() { return sp.getString("discord_id", ""); }
    void setDiscordId(String v) { sp.edit().putString("discord_id", v).apply(); }

    String shortcutName() { return sp.getString("shortcut_name", "Roblox"); }
    void setShortcutName(String v) { sp.edit().putString("shortcut_name", v).apply(); }
}
