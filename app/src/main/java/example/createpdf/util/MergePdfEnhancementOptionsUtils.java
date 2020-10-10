package example.createpdf.util;

import android.content.Context;

import java.util.ArrayList;

import example.createpdf.R;
import example.createpdf.model.EnhancementOptionsEntity;

public class MergePdfEnhancementOptionsUtils {
    public static ArrayList<EnhancementOptionsEntity> getEnhancementOptions(Context context) {
        ArrayList<EnhancementOptionsEntity> options = new ArrayList<>();

        options.add(new EnhancementOptionsEntity(
                context, R.drawable.baseline_enhanced_encryption_24, R.string.set_password));
        return options;
    }
}