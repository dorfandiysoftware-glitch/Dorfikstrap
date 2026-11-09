package org.dorfikstrap.app;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** The user's FastFlag list (persisted) and its ClientAppSettings.json form. */
final class FlagStore {
    static final class Flag {
        String name, value;
        boolean enabled;
        Flag(String n, String v, boolean e) { name = n; value = v; enabled = e; }
    }

    static final Pattern NAME = Pattern.compile("[A-Za-z0-9_.]{2,100}");
    static final Pattern VALUE = Pattern.compile("[A-Za-z0-9_.:/ -]{0,100}");

    /** {name, value, description}; experimental, not verified against Roblox's allowlist. */
    static final String[][] PRESETS = {
            {"FFlagDebugDisplayFPS", "true", "FPS counter (verified on 2.746)"},
            {"DFIntTaskSchedulerTargetFps", "120", "Target FPS 120 (experimental)"},
            {"FIntDebugForceMSAASamples", "1", "Disable MSAA (experimental)"},
            {"FIntRenderShadowIntensity", "0", "No shadows (experimental)"},
    };

    private final SharedPreferences sp;
    final List<Flag> flags = new ArrayList<>();

    FlagStore(Context c) {
        sp = c.getSharedPreferences("flags", Context.MODE_PRIVATE);
        try {
            JSONArray a = new JSONArray(sp.getString("list", "[]"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                flags.add(new Flag(o.getString("n"), o.getString("v"), o.optBoolean("e", true)));
            }
        } catch (Exception ignored) {
        }
    }

    void save() {
        JSONArray a = new JSONArray();
        try {
            for (Flag f : flags)
                a.put(new JSONObject().put("n", f.name).put("v", f.value).put("e", f.enabled));
        } catch (Exception ignored) {
        }
        sp.edit().putString("list", a.toString()).apply();
    }

    Flag find(String name) {
        for (Flag f : flags) if (f.name.equals(name)) return f;
        return null;
    }

    /** Adds or updates. Returns false if name/value contain forbidden characters. */
    boolean put(String name, String value) {
        if (!NAME.matcher(name).matches() || !VALUE.matcher(value).matches()) return false;
        Flag f = find(name);
        if (f == null) flags.add(new Flag(name, value, true));
        else { f.value = value; f.enabled = true; }
        save();
        return true;
    }

    String json() {
        JSONObject o = new JSONObject();
        try {
            for (Flag f : flags) if (f.enabled) o.put(f.name, f.value);
        } catch (Exception ignored) {
        }
        return o.toString();
    }

    /** Shell command that writes the file. Values are validated, so single quotes cannot occur. */
    String writeCommand() {
        return "printf '%s' '" + json() + "' > " + Sh.FLAGS_FILE + " && chmod 644 " + Sh.FLAGS_FILE
                + " && cat " + Sh.FLAGS_FILE;
    }
}
