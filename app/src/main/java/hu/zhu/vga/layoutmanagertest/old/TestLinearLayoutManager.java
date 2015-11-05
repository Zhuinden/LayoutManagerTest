package hu.zhu.vga.layoutmanagertest.old;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;

/**
 * Created by Zhuinden on 2015.10.11..
 */
public class TestLinearLayoutManager extends LinearLayoutManager {
    public TestLinearLayoutManager(Context context) {
        super(context);
    }

    public TestLinearLayoutManager(Context context, int orientation, boolean reverseLayout) {
        super(context, orientation, reverseLayout);
    }

    public TestLinearLayoutManager(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public boolean supportsPredictiveItemAnimations() {
        return false;
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        Log.d(getClass().getSimpleName(), "LAYOUT CHILDREN");
        super.onLayoutChildren(recycler, state);
    }

    @Override
    public void scrollToPosition(int position) {
        Log.d(getClass().getSimpleName(), "SCROLL TO POSITION");
        super.scrollToPosition(position);
    }

    @Override
    public int scrollHorizontallyBy(int dx, RecyclerView.Recycler recycler, RecyclerView.State state) {
        Log.d(getClass().getSimpleName(), "SCROLL HORIZONTALLY");
        return super.scrollHorizontallyBy(dx, recycler, state);
    }

    @Override
    public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
        Log.d(getClass().getSimpleName(), "SCROLL VERTICALLY");
        return super.scrollVerticallyBy(dy, recycler, state);
    }

    @Override
    public void offsetChildrenHorizontal(int dx) {
        Log.d(getClass().getSimpleName(), "OFFSET CHILDREN HORIZONTAL");
        super.offsetChildrenHorizontal(dx);
    }

    @Override
    public void offsetChildrenVertical(int dy) {
        Log.d(getClass().getSimpleName(), "OFFSET CHILDREN VERTICAL");
        super.offsetChildrenVertical(dy);
    }
}
