package org.dorfikstrap.app;

import android.content.pm.PackageManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;

import rikka.shizuku.Shizuku;

/** Runs shell commands (uid shell) through Shizuku. */
final class Sh {
    static final String PKG = "com.roblox.client";
    static final String FLAGS_FILE = "/data/local/tmp/ClientAppSettings.json";

    private Sh() {}

    static boolean ready() {
        try {
            return Shizuku.pingBinder() && !Shizuku.isPreV11()
                    && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED;
        } catch (Throwable t) {
            return false;
        }
    }

    /** Shizuku.newProcess is private in the API; reached by reflection. */
    private static Process spawn(String cmd) throws Exception {
        Method m = Shizuku.class.getDeclaredMethod("newProcess",
                String[].class, String[].class, String.class);
        m.setAccessible(true);
        return (Process) m.invoke(null, new Object[]{new String[]{"sh", "-c", cmd}, null, null});
    }

    /** Blocking. Returns stdout+stderr (max 400 lines). */
    static String run(String cmd) {
        StringBuilder sb = new StringBuilder();
        try {
            Process p = spawn(cmd + " 2>&1");
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            int n = 0;
            while ((line = r.readLine()) != null && n++ < 400) sb.append(line).append('\n');
            p.waitFor();
        } catch (Throwable t) {
            Throwable c = t.getCause() != null ? t.getCause() : t;
            sb.append("ERROR: ").append(c).append('\n');
        }
        return sb.toString();
    }

    /** Same as run() but with the command echoed and exit code appended (for reports). */
    static String runLogged(String cmd) {
        return "$ " + cmd + "\n" + run(cmd) + "\n";
    }
}
