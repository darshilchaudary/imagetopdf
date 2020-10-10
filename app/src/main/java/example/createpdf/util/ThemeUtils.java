package example.createpdf.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import example.createpdf.R;

import static example.createpdf.util.Constants.THEME_BLACK;
import static example.createpdf.util.Constants.THEME_DARK;
import static example.createpdf.util.Constants.THEME_WHITE;

public class ThemeUtils {

    public static void setThemeApp(Context context) {
        SharedPreferences mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String themeName = mSharedPreferences.getString(Constants.DEFAULT_THEME_TEXT,
                Constants.DEFAULT_THEME);
        switch (themeName) {
            case THEME_WHITE:
                context.setTheme(R.style.AppThemeWhite);
                break;
            case THEME_BLACK:
                context.setTheme(R.style.AppThemeBlack);
                break;
            case THEME_DARK:
                context.setTheme(R.style.ActivityThemeDark);
                break;
        }
    }

    public static int getSelectedThemePosition(Context context) {
        SharedPreferences mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String themeName = mSharedPreferences.getString(Constants.DEFAULT_THEME_TEXT,
                Constants.DEFAULT_THEME);
        switch (themeName) {
            case THEME_BLACK:
                return  0;
            case THEME_DARK:
                return  1;
            case THEME_WHITE:
                return  2;
        }
        return 0;
    }

    public static void saveTheme(Context context, String themeName) {
        SharedPreferences mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putString(Constants.DEFAULT_THEME_TEXT, themeName);
        editor.apply();
    }
}
