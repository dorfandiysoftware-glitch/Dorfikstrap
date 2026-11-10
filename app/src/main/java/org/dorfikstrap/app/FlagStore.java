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

    boolean master() { return sp.getBoolean("master", true); }

    void setMaster(boolean on) { sp.edit().putBoolean("master", on).apply(); }

    int enabledCount() {
        int n = 0;
        for (Flag f : flags) if (f.enabled) n++;
        return n;
    }

    /** Imports a JSON object of flags. Returns {added, skipped}, or null if the text is not a JSON object. */
    int[] importJson(String text) {
        try {
            String clean = text.replace("\uFEFF", "").trim();
            if (clean.startsWith("```")) {
                clean = clean.replaceFirst("^```[a-zA-Z]*", "");
                if (clean.endsWith("```")) clean = clean.substring(0, clean.length() - 3);
            }
            clean = clean.trim().replaceAll(",\\s*([}\\]])", "$1"); // trailing commas
            JSONObject o = new JSONObject(clean);
            int ok = 0, bad = 0;
            java.util.Iterator<String> it = o.keys();
            while (it.hasNext()) {
                String k = it.next();
                Object v = o.get(k);
                if (v instanceof JSONObject || v instanceof JSONArray || v == JSONObject.NULL) { bad++; continue; }
                if (put(k, String.valueOf(v))) ok++; else bad++;
            }
            return new int[]{ok, bad};
        } catch (Exception e) {
            return null;
        }
    }

    /** Shell command that writes the file. Values are validated, so single quotes cannot occur. */
    String writeCommand() {
        if (!master() || enabledCount() == 0)
            return "rm -f " + Sh.FLAGS_FILE + "; echo 'no flags: file removed'";
        return "printf '%s' '" + json() + "' > " + Sh.FLAGS_FILE + " && chmod 644 " + Sh.FLAGS_FILE
                + " && cat " + Sh.FLAGS_FILE;
    }
}
