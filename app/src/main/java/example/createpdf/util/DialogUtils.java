package example.createpdf.util;

import android.app.Activity;

import com.afollestad.materialdialogs.MaterialDialog;

import example.createpdf.R;

public class DialogUtils {

    public static final int ROTATE_PAGES = 20;
    public static final int ADD_PASSWORD = 21;
    public static final int REMOVE_PASSWORD = 22;
    public static final int ADD_WATERMARK = 23;

    public static MaterialDialog.Builder createWarningDialog(Activity activity,
                                                             int content) {
        return new MaterialDialog.Builder(activity)
                .title(R.string.warning)
                .content(content)
                .positiveText(android.R.string.ok)
                .negativeText(android.R.string.cancel);
    }

    public static MaterialDialog.Builder createOverwriteDialog(Activity activity) {
        return new MaterialDialog.Builder(activity)
                .title(R.string.warning)
                .content(R.string.overwrite_message)
                .positiveText(android.R.string.ok)
                .negativeText(android.R.string.cancel);
    }

    public static MaterialDialog.Builder createCustomDialog(Activity activity,
                                                            int title, int content) {
        return new MaterialDialog.Builder(activity)
                .title(title)
                .content(content)
                .positiveText(android.R.string.ok)
                .negativeText(android.R.string.cancel);
    }

    public static MaterialDialog.Builder createCustomDialogWithoutContent(Activity activity,
                                                            int title) {
        return new MaterialDialog.Builder(activity)
                .title(title)
                .positiveText(android.R.string.ok)
                .negativeText(android.R.string.cancel);
    }

    public static MaterialDialog createAnimationDialog(Activity activity) {
        return new MaterialDialog.Builder(activity)
                .customView(R.layout.lottie_anim_dialog, false)
                .build();
    }

    public static void showFilesInfoDialog(Activity activity, int dialogId) {
        int stringId = R.string.viewfiles_rotatepages;
        switch (dialogId) {
            case ROTATE_PAGES:
                stringId = R.string.viewfiles_rotatepages;
                break;
            case REMOVE_PASSWORD:
                stringId = R.string.viewfiles_removepassword;
                break;
            case ADD_PASSWORD:
                stringId = R.string.viewfiles_addpassword;
                break;
            case ADD_WATERMARK:
                stringId = R.string.viewfiles_addwatermark;
                break;
        }
        new MaterialDialog.Builder(activity)
                .title(R.string.app_name)
                .content(stringId)
                .positiveText(android.R.string.ok)
                .build()
                .show();
    }
}
