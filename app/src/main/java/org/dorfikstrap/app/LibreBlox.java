package org.dorfikstrap.app;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.core.content.FileProvider;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/** Finds, downloads (from GitHub Releases) and launches the LibreBlox companion app. */
final class LibreBlox {
    static final String PKG = "org.libreblox.app";
    private static final String RELEASES = "https://api.github.com/repos/dorfandiysoftware-glitch/LibreBlox/releases?per_page=1";

    interface Progress { void on(int percent); }

    private LibreBlox() {}

    static boolean installed(Context c) {
        try {
            c.getPackageManager().getPackageInfo(PKG, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    static boolean launch(Context c) {
        Intent i = c.getPackageManager().getLaunchIntentForPackage(PKG);
        if (i == null) return false;
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        c.startActivity(i);
        return true;
    }

    /** True when the system lets this app install packages; otherwise opens the settings page for it. */
    static boolean ensureInstallPermission(Context c) {
        if (Build.VERSION.SDK_INT < 26 || c.getPackageManager().canRequestPackageInstalls()) return true;
        Intent i = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + c.getPackageName()));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        c.startActivity(i);
        return false;
    }

    /** Blocking: call from a background thread. Returns the downloaded APK. */
    static File download(Context c, Progress p) throws Exception {
        String json = readString(RELEASES);
        JSONArray releases = new JSONArray(json);
        if (releases.length() == 0) throw new Exception("No LibreBlox release found");
        JSONArray assets = releases.getJSONObject(0).getJSONArray("assets");
        String url = null;
        for (int i = 0; i < assets.length(); i++) {
            JSONObject a = assets.getJSONObject(i);
            if (a.getString("name").endsWith(".apk")) { url = a.getString("browser_download_url"); break; }
        }
        if (url == null) throw new Exception("The release has no APK");

        File dir = new File(c.getCacheDir(), "updates");
        if (!dir.exists() && !dir.mkdirs()) throw new Exception("Cannot create cache dir");
        File out = new File(dir, "LibreBlox.apk");
        HttpURLConnection h = open(url);
        try {
            long total = h.getContentLengthLong();
            try (InputStream in = h.getInputStream(); OutputStream os = new FileOutputStream(out)) {
                byte[] buf = new byte[32 * 1024];
                long done = 0;
                int n, last = -1;
                while ((n = in.read(buf)) > 0) {
                    os.write(buf, 0, n);
                    done += n;
                    if (total > 0) {
                        int pc = (int) (done * 100 / total);
                        if (pc != last) { last = pc; p.on(pc); }
                    }
                }
            }
        } finally {
            h.disconnect();
        }
        return out;
    }

    static void install(Context c, File apk) {
        Uri uri = FileProvider.getUriForFile(c, c.getPackageName() + ".files", apk);
        Intent i = new Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
        c.startActivity(i);
    }

    private static HttpURLConnection open(String url) throws Exception {
        HttpURLConnection h = (HttpURLConnection) new URL(url).openConnection();
        h.setConnectTimeout(15000);
        h.setReadTimeout(30000);
        h.setInstanceFollowRedirects(true);
        h.setRequestProperty("User-Agent", "Dorfikstrap");
        h.setRequestProperty("Accept", "application/vnd.github+json");
        int code = h.getResponseCode();
        if (code / 100 != 2) throw new Exception("HTTP " + code);
        return h;
    }

    private static String readString(String url) throws Exception {
        HttpURLConnection h = open(url);
        try (InputStream in = h.getInputStream()) {
            java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
            return bo.toString("UTF-8");
        } finally {
            h.disconnect();
        }
    }
}
