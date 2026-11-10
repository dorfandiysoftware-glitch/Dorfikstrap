package org.dorfikstrap.app;

import android.app.Activity;
import android.content.Intent;
import android.widget.Toast;

/** Applies the FastFlag file and starts Roblox. */
final class Roblox {
    private Roblox() {}

    static boolean installed(Activity a) {
        return a.getPackageManager().getLaunchIntentForPackage(Sh.PKG) != null;
    }

    static void start(Activity a) {
        Intent i = a.getPackageManager().getLaunchIntentForPackage(Sh.PKG);
        if (i == null) Toast.makeText(a, R.string.roblox_missing, Toast.LENGTH_SHORT).show();
        else a.startActivity(i);
    }

    /**
     * Writes the flag file when Shizuku is ready, optionally force-stops Roblox so the flags are
     * re-read, then launches it. Without Shizuku Roblox is simply launched.
     */
    static void applyAndLaunch(Activity a, FlagStore store, boolean restart, Runnable done) {
        new Thread(() -> {
            if (Sh.ready()) {
                Sh.run(store.writeCommand());
                if (restart) Sh.run("am force-stop " + Sh.PKG);
            }
            a.runOnUiThread(() -> {
                start(a);
                if (done != null) done.run();
            });
        }).start();
    }

    private static String a_not_ready() { return "Shizuku is not ready"; }

    /** Writes the flag file only. Returns the shell output. */
    static String apply(FlagStore store) {
        return Sh.ready() ? Sh.run(store.writeCommand()) : a_not_ready();
    }
}
