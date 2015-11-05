package hu.zhu.vga.layoutmanagertest;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.Display;
import android.view.WindowManager;

public class ScreenUtils {
    private static int screenWidth = 0;
    private static int screenHeight = 0;
    private static final int[] EMPTY_STATE = new int[]{};

    public static void clearState(Drawable drawable) {
        if(drawable != null) {
            drawable.setState(EMPTY_STATE);
        }
    }

    public static int dpToPx(double dp) {
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
    }

    public static double pxToDp(double px) {
        return ((double) (px)) / Resources.getSystem().getDisplayMetrics().density;
    }

    //int width = this.getResources().getDisplayMetrics().widthPixels;
    //int height = this.getResources().getDisplayMetrics().heightPixels;

    @SuppressWarnings("deprecation")
    public static int getScreenHeight() {
        Context context = CustomApplication.get();
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            if (screenHeight == 0) {
                WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
                Display display = wm.getDefaultDisplay();
                Point size = new Point();
                display.getSize(size);
                screenHeight = size.y;
            }
        } else {
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            Display display = wm.getDefaultDisplay();
            screenHeight = display.getHeight();
        }
        return screenHeight;
    }

    @SuppressWarnings("deprecation")
    public static int getScreenWidth() {
        Context context = CustomApplication.get();
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            if (screenHeight == 0) {
                WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
                Display display = wm.getDefaultDisplay();
                Point size = new Point();
                display.getSize(size);
                screenWidth = size.x;
            }
        } else {
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            Display display = wm.getDefaultDisplay();
            screenWidth = display.getWidth();
        }
        return screenWidth;
    }

    public static int getStatusBarHeight() {
        int result = 0;
        int resourceId = CustomApplication.get()
                .getResources()
                .getIdentifier("status_bar_height", "dimen", "android");
        if(resourceId > 0) {
            result = CustomApplication.get()
                    .getResources()
                    .getDimensionPixelSize(resourceId);
            dpToPx(result);
        }
        return result;
    }

}