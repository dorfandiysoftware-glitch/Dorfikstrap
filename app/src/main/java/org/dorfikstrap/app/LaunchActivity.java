package org.dorfikstrap.app;

import android.app.Activity;
import android.os.Bundle;

/** Target of the home-screen shortcut: apply flags, launch Roblox, vanish. */
public class LaunchActivity extends Activity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        String uri = getIntent() != null ? getIntent().getStringExtra("uri") : null;
        Roblox.applyAndLaunch(this, new FlagStore(this), true, uri, this::finish);
    }
}
