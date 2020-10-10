package example.createpdf.util;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.View;

import com.afollestad.materialdialogs.MaterialDialog;

import java.util.HashMap;

import example.createpdf.R;

import static example.createpdf.util.Constants.DEFAULT_PAGE_SIZE;
import static example.createpdf.util.Constants.DEFAULT_PAGE_SIZE_TEXT;
import static example.createpdf.util.DialogUtils.createCustomDialogWithoutContent;

public class PageSizeUtils {

    private final Context mActivity;
    private final SharedPreferences mSharedPreferences;
    public static String mPageSize;
    private final String mDefaultPageSize;
    private final HashMap<Integer, Integer> mPageSizeToString;

    public PageSizeUtils(Context mActivity) {
        this.mActivity = mActivity;
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mActivity);
        mDefaultPageSize = mSharedPreferences.getString(Constants.DEFAULT_PAGE_SIZE_TEXT,
                DEFAULT_PAGE_SIZE);
        mPageSize = mSharedPreferences.getString(DEFAULT_PAGE_SIZE_TEXT, DEFAULT_PAGE_SIZE);
        mPageSizeToString = new HashMap<>();
    }

    private String getPageSize(int selectionId, String spinnerAValue, String spinnerBValue) {
        String stringPageSize;
        switch (selectionId) {

        }
        return mPageSize;
    }

    public MaterialDialog showPageSizeDialog(boolean saveValue) {
        MaterialDialog materialDialog = getPageSizeDialog(saveValue);

        View view = materialDialog.getCustomView();
        materialDialog.show();
        return materialDialog;
    }

    private MaterialDialog getPageSizeDialog(boolean saveValue) {
        MaterialDialog.Builder builder = createCustomDialogWithoutContent((Activity) mActivity,
                R.string.set_page_size_text);
        return null;
    }

    private Integer getKey(HashMap<Integer, Integer> map, String value) {
        for (HashMap.Entry<Integer, Integer> entry : map.entrySet()) {
            if (value.equals(mActivity.getString(entry.getValue()))) {
                return entry.getKey();
            }
        }
        return null;
    }
}