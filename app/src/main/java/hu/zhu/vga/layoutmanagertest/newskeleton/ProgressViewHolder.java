package hu.zhu.vga.layoutmanagertest.newskeleton;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.LinearLayout;

import butterknife.Bind;
import butterknife.ButterKnife;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.LinearLayout;

import com.pnikosis.materialishprogress.ProgressWheel;

import butterknife.Bind;
import butterknife.ButterKnife;
import hu.zhu.vga.layoutmanagertest.R;

/**
 * Created by Owner on 2015.08.26..
 */
public class ProgressViewHolder
        extends RecyclerView.ViewHolder {

    @Bind(R.id.viewholder_progress_root)
    LinearLayout root;

    @Bind(R.id.viewholder_progress_circular_progress)
    ProgressWheel progressView;

    @Bind(R.id.viewholder_progress_text)
    TypefaceTextView text;

    private boolean isInProgress = false;

    public ProgressViewHolder(View itemView) {
        super(itemView);
        ButterKnife.bind(this, itemView);
        progressView.setVisibility(View.GONE);
    }

    public void startProgress(final String newText, final boolean updateNewText) {
        isInProgress = true;
        if(updateNewText) {
            text.setText(newText);
        }
        progressView.setVisibility(View.VISIBLE);
    }

    public void stopProgress(final String newText, final boolean updateNewText) {
        isInProgress = false;
        if(updateNewText) {
            text.setText(newText);
        }
        progressView.setVisibility(View.GONE);
    }

    public boolean isInProgress() {
        return isInProgress;
    }

    public TypefaceTextView getText() {
        return text;
    }

    public void setText(TypefaceTextView text) {
        this.text = text;
    }

    public LinearLayout getRoot() {
        return root;
    }

    public void setRoot(LinearLayout root) {
        this.root = root;
    }
}
