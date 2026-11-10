package org.dorfikstrap.app;

import android.app.Application;
import android.content.Context;

import androidx.appcompat.app.AppCompatDelegate;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        applyNight(this);
    }

    /** AMOLED and the green theme are dark-only; Material 3 colours follow the system. */
    static void applyNight(Context c) {
        Prefs p = new Prefs(c);
        AppCompatDelegate.setDefaultNightMode(p.md3() && !p.amoled()
                ? AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM : AppCompatDelegate.MODE_NIGHT_YES);
    }
}
