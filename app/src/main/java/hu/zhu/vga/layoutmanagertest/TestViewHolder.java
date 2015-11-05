package hu.zhu.vga.layoutmanagertest;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

/**
 * Created by Zhuinden on 2015.11.05..
 */
public class TestViewHolder extends RecyclerView.ViewHolder {

    private TextView textView;

    public TestViewHolder(View itemView) {
        super(itemView);
        this.setTextView((TextView)itemView);
    }

    public TextView getTextView() {
        return textView;
    }

    public void setTextView(TextView textView) {
        this.textView = textView;
    }
}