package hu.zhu.vga.layoutmanagertest;

import android.app.Application;

/**
 * Created by Zhuinden on 2015.10.10..
 */
public class CustomApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        ApplicationHolder.INSTANCE.setApplication(this);
    }

    public static CustomApplication get() {
        return ApplicationHolder.INSTANCE.getApplication();
    }
}
