package hu.zhu.vga.layoutmanagertest.preserve;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;

import hu.zhu.vga.layoutmanagertest.ScreenUtils;

/**
 * Created by Zhuinden on 2015.11.04..
 */
public class TwoDirectionNoRecycleLayoutManager2 extends RecyclerView.LayoutManager {
    public static class LayoutState {
        public int rowCount;
        public int columnCount;
        public int currentRowAccumulatedWidth;
        public int remainingScreenWidth;
        public int remainingScreenHeight;
        public int currentRowAccumulatedHeight;

        public void reset() {
            this.rowCount = 0;
            this.columnCount = 0;
            this.currentRowAccumulatedWidth = 0;
            this.currentRowAccumulatedHeight = 0;
            this.remainingScreenWidth = ScreenUtils.getScreenWidth();
            this.remainingScreenHeight = ScreenUtils.getScreenHeight();
        }
    }

    public static class AnchorInfo implements Parcelable {
        public int currentScrolledX = 0;
        public int currentScrolledY = 0;

        public AnchorInfo() {
        }

        protected AnchorInfo(Parcel in) {
            currentScrolledX = in.readInt();
            currentScrolledY = in.readInt();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(currentScrolledX);
            dest.writeInt(currentScrolledY);
        }

        @SuppressWarnings("unused")
        public static final Parcelable.Creator<AnchorInfo> CREATOR = new Parcelable.Creator<AnchorInfo>() {
            @Override
            public AnchorInfo createFromParcel(Parcel in) {
                return new AnchorInfo(in);
            }

            @Override
            public AnchorInfo[] newArray(int size) {
                return new AnchorInfo[size];
            }
        };
    }

    protected LayoutState layoutState = new LayoutState();
    protected AnchorInfo anchorInfo = new AnchorInfo();
    protected SparseArray<View> viewCache = new SparseArray<>();

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (state.getItemCount() == 0) {
            removeAndRecycleAllViews(recycler);
            return;
        }
        detachAndScrapAttachedViews(recycler);
        ensureLayoutState();
        ensureAnchorInfo();
        layoutState.reset();
        fill(recycler, state);
    }

    private void fill(RecyclerView.Recycler recycler, RecyclerView.State state) {
        ensureLayoutState();
        for(int i = 0; i < getChildCount(); i++) {
            View view = getChildAt(i);
            int position = getPosition(view);
            viewCache.put(position, view);
        }

        for (int i = 0; i < viewCache.size(); i++) {
            detachView(viewCache.valueAt(i));
        }

        for(int i = 0; i < state.getItemCount(); i++) {
            View view = viewCache.get(i);
            if(view == null) {
                view = recycler.getViewForPosition(i);
                addView(view);
                measureChildWithMargins(view, 0, 0);
                int decoratedMeasuredWidth = getDecoratedMeasuredWidth(view);
                int decoratedMeasuredHeight = getDecoratedMeasuredHeight(view);
                layoutDecorated(view,
                        layoutState.currentRowAccumulatedWidth,
                        layoutState.currentRowAccumulatedHeight,
                        layoutState.currentRowAccumulatedWidth + decoratedMeasuredWidth,
                        layoutState.currentRowAccumulatedHeight + decoratedMeasuredHeight); //assuming uniform height per row
                layoutState.currentRowAccumulatedWidth += decoratedMeasuredWidth;
                layoutState.columnCount++;
                if(layoutState.columnCount >= 10) {
                    layoutState.columnCount = 0;
                    layoutState.rowCount++;
                    layoutState.currentRowAccumulatedWidth = 0;
                    layoutState.currentRowAccumulatedHeight += decoratedMeasuredHeight;
                    layoutState.remainingScreenWidth = ScreenUtils.getScreenWidth();
                    layoutState.remainingScreenHeight -= decoratedMeasuredHeight;
                }
            } else {
                attachView(view);
                viewCache.remove(i);
            }
        }
        for(int i = 0; i < viewCache.size(); i++) {
            recycler.recycleView(viewCache.valueAt(i));
        }
    }

    private void ensureAnchorInfo() {
        if(anchorInfo == null) {
            anchorInfo = new AnchorInfo();
        }
    }

    protected void ensureLayoutState() {
        if(layoutState == null) {
            layoutState = new LayoutState();
        }
    }

    @Override
    public void scrollToPosition(int position) {
        requestLayout();
    }

    @Override
    public void onAdapterChanged(RecyclerView.Adapter oldAdapter, RecyclerView.Adapter newAdapter) {
        //Completely scrap the existing layout
        removeAllViews();
    }

    @Override
    public View findViewByPosition(int position) {
        return super.findViewByPosition(position);
    }

    @Override
    public boolean canScrollVertically() {
        return true;
    }

    @Override
    public boolean canScrollHorizontally() {
        return true;
    }

    @Override
    public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
        int delta = scrollVerticallyInternal(dy);
        offsetChildrenVertical(-delta);
        return delta;
    }

    private int scrollVerticallyInternal(int dy) {
        int childCount = getChildCount();
        int itemCount = getItemCount();
        if (childCount == 0){
            return 0;
        }

        final View topView = getChildAt(0);
        final View bottomView = getChildAt(childCount - 1);

        //Случай, когда все вьюшки поместились на экране
        int viewSpan = getDecoratedBottom(bottomView) - getDecoratedTop(topView);
        if (viewSpan <= getHeight()) {
            return 0;
        }

        int delta = 0;
        //если контент уезжает вниз
        if (dy < 0){
            View firstView = getChildAt(0);
            int firstViewAdapterPos = getPosition(firstView);
            if (firstViewAdapterPos > 0){ //если верхняя вюшка не самая первая в адаптере
                delta = dy;
            } else { //если верхняя вьюшка самая первая в адаптере и выше вьюшек больше быть не может
                int viewTop = getDecoratedTop(firstView);
                delta = Math.max(viewTop, dy);
            }
        } else if (dy > 0){ //если контент уезжает вверх
            View lastView = getChildAt(childCount - 1);
            int lastViewAdapterPos = getPosition(lastView);
            if (lastViewAdapterPos < itemCount - 1){ //если нижняя вюшка не самая последняя в адаптере
                delta = dy;
            } else { //если нижняя вьюшка самая последняя в адаптере и ниже вьюшек больше быть не может
                int viewBottom = getDecoratedBottom(lastView);
                int parentBottom = getHeight();
                delta = Math.min(viewBottom - parentBottom, dy);
            }
        }
        ensureAnchorInfo();
        anchorInfo.currentScrolledY += delta;
        return delta;
    }

    @Override
    public int scrollHorizontallyBy(int dx, RecyclerView.Recycler recycler, RecyclerView.State state) {
        int delta = scrollHorizontallyInternal(dx);
        offsetChildrenHorizontal(-delta);
        return delta;
    }

    private int scrollHorizontallyInternal(int dx) {
        int childCount = getChildCount();
        int itemCount = getItemCount();
        if (childCount == 0){
            return 0;
        }

        final View leftView = getChildAt(0);
        final View rightView = getChildAt(childCount - 1);

        //Случай, когда все вьюшки поместились на экране
        int viewSpan = getDecoratedRight(rightView) - getDecoratedLeft(leftView);
        if (viewSpan <= getWidth()) {
            return 0;
        }

        int delta = 0;
        //если контент уезжает вниз
        if (dx < 0){
            View firstView = getChildAt(0);
            int firstViewAdapterPos = getPosition(firstView);
            if (firstViewAdapterPos > 0){ //если верхняя вюшка не самая первая в адаптере
                delta = dx;
            } else { //если верхняя вьюшка самая первая в адаптере и выше вьюшек больше быть не может
                int viewTop = getDecoratedLeft(firstView);
                delta = Math.max(viewTop, dx);
            }
        } else if (dx > 0){ //если контент уезжает вверх
            View lastView = getChildAt(childCount - 1);
            int lastViewAdapterPos = getPosition(lastView);
            if (lastViewAdapterPos < itemCount - 1){ //если нижняя вюшка не самая последняя в адаптере
                delta = dx;
            } else { //если нижняя вьюшка самая последняя в адаптере и ниже вьюшек больше быть не может
                int viewBottom = getDecoratedRight(lastView);
                int parentBottom = getWidth();
                delta = Math.min(viewBottom - parentBottom, dx);
            }
        }
        ensureAnchorInfo();
        anchorInfo.currentScrolledX += delta;
        return delta;
    }


    // STATE SAVING
    @Override
    public Parcelable onSaveInstanceState() {
        //begin boilerplate code that allows parent classes to save state
        Parcelable superState = super.onSaveInstanceState();

        SavedState ss = new SavedState(superState);
        //end
        ensureAnchorInfo();
        ss.currentScrollX = anchorInfo.currentScrolledX;
        ss.currentScrollY = anchorInfo.currentScrolledY;
        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        //begin boilerplate code so parent classes can restore state
        if(!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        //end

        ensureAnchorInfo();
        anchorInfo.currentScrolledX = ss.currentScrollX;
        anchorInfo.currentScrolledY = ss.currentScrollY;
    }

    static class SavedState
            implements Parcelable {
        public static final SavedState EMPTY_STATE = new SavedState() {
        };

        // This keeps the parent(RecyclerView)'s state
        Parcelable superState;

        private int currentScrollX;
        private int currentScrollY;

        SavedState() {
            superState = null;
        }

        SavedState(Parcelable superState) {
            this.superState = superState != EMPTY_STATE ? superState : null;
        }

        private SavedState(Parcel in) {
            // Parcel 'in' has its parent(RecyclerView)'s saved state.
            // To restore it, class loader that loaded RecyclerView is required.
            Parcelable state = in.readParcelable(RecyclerView.class.getClassLoader());
            this.superState = state != null ? state : EMPTY_STATE;
            currentScrollX = in.readInt();
            currentScrollY = in.readInt();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            out.writeParcelable(superState, flags);
            out.writeInt(currentScrollX);
            out.writeInt(currentScrollY);
        }

        public Parcelable getSuperState() {
            return superState;
        }

        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
}
