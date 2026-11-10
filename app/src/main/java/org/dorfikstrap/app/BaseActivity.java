package org.dorfikstrap.app;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.color.DynamicColors;
import com.google.android.material.color.MaterialColors;

abstract class BaseActivity extends AppCompatActivity {
    Prefs prefs;

    @Override
    protected void onCreate(Bundle b) {
        prefs = new Prefs(this);
        boolean md3 = prefs.md3();
        if (md3) DynamicColors.applyToActivityIfAvailable(this);
        else getTheme().applyStyle(R.style.Overlay_Green, true);
        if (!md3 || prefs.amoled()) getTheme().applyStyle(R.style.Overlay_Amoled, true);
        super.onCreate(b);
        getWindow().setBackgroundDrawable(new ColorDrawable(
                MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurface, Color.BLACK)));
    }

    int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
