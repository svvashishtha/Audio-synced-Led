package test.so.audio_led.utils;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.TypedValue;

public final class TextUtils {
    public static float getFontSize(Context ctx, int textAppearance) {
        TypedValue typedValue = new TypedValue();
        ctx.getTheme().resolveAttribute(textAppearance, typedValue, true);
        TypedArray arr = ctx.obtainStyledAttributes(typedValue.data, new int[]{16842901});
        float fontSize = (float) arr.getDimensionPixelSize(0, -1);
        arr.recycle();
        return fontSize;
    }
}
