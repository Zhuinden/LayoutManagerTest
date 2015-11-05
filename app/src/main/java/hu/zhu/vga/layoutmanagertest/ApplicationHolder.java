package hu.zhu.vga.layoutmanagertest;

/**
 * Created by Zhuinden on 2015.10.10..
 */
public enum ApplicationHolder {
    INSTANCE;

    private CustomApplication customApplication;

    private ApplicationHolder() {
    }

    void setApplication(CustomApplication customApplication) {
        this.customApplication = customApplication;
    }

    public CustomApplication getApplication() {
        return customApplication;
    }
}
