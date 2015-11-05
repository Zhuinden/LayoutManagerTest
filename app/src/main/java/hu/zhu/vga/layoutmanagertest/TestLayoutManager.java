package hu.zhu.vga.layoutmanagertest;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Zhuinden on 2015.11.04..
 */
public class TestLayoutManager
        extends RecyclerView.LayoutManager {
    private RowSpecifierAdapter rowSpecifierAdapter;

    public TestLayoutManager(RowSpecifierAdapter rowSpecifierAdapter) {
        this.rowSpecifierAdapter = rowSpecifierAdapter;
        this.visiblePositions = new ArrayList<>();
    }

    protected class LayoutState {
        public int rowCount;
        public int columnCount;
        public int currentRowAccumulatedWidth;
        public int remainingScreenWidth;
        public int remainingScreenHeight;
        public int currentRowAccumulatedHeight;

        public void reset() {
            this.rowCount = 0;
            this.currentRowAccumulatedWidth = 0;
            this.currentRowAccumulatedHeight = 0;
            this.remainingScreenWidth = getWidth();
            this.remainingScreenHeight = getHeight();
        }
    }

    protected class AnchorInfo {
        public int currentScrolledX = 0;
        public int currentScrolledY = 0;
    }

    protected AnchorInfo anchorInfo = new AnchorInfo();
    protected HashMap<Integer, View> viewCache = new HashMap<>();

    protected List<Integer> visiblePositions;

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        if(state.getItemCount() == 0) {
            removeAndRecycleAllViews(recycler);
            return;
        }
        detachAndScrapAttachedViews(recycler);
        ensureAnchorInfo();

        fill(recycler, state);
    }

    public void initializeVisiblePositions(RecyclerView.State state) {
        visiblePositions.clear();
        for(int i = 0; i < state.getItemCount(); i++) {
            if(isVisible(i)) {
                visiblePositions.add(i);
            }
        }
    }

    private boolean isVisible(int position) {
        ensureAnchorInfo();
        RowSpecifierAdapter.MetadataHolder current = rowSpecifierAdapter.getMetadataHolderForPosition(position);
        int accumulatedWidth = current.getAccumulatedWidth();
        if(ScreenUtils.dpToPx(accumulatedWidth) >= anchorInfo.currentScrolledX && ScreenUtils.dpToPx(accumulatedWidth) < (anchorInfo.currentScrolledX + getWidth())) { //is inside rectangle
            return true;
        }
//        if(ScreenUtils.dpToPx(accumulatedWidth + current.getWidth()) > anchorInfo.currentScrolledX || ScreenUtils.dpToPx(accumulatedWidth + current
//                .getWidth()) + getWidth() < (anchorInfo.currentScrolledX + getWidth())) { //is partly visible horizontally
//            return true;
//        }
//        if(ScreenUtils.dpToPx(current.getAccumulatedHeight() + current.getHeight()) > anchorInfo.currentScrolledY || ScreenUtils.dpToPx(
//                current.getAccumulatedHeight() + current.getHeight()) + getHeight() < (anchorInfo.currentScrolledY + getHeight())) { //is partly visible vertically
//            return true;
//        }
        return false;
    }

    private void fill(RecyclerView.Recycler recycler, RecyclerView.State state) {
        ensureAnchorInfo();
        viewCache.clear();
        for(int i = 0; i < getChildCount(); i++) {
            View view = getChildAt(i);
            int position = getPosition(view);
            viewCache.put(position, view);
        }
        for(Integer i : viewCache.keySet()) {
            detachView(viewCache.get(i));
        }

        initializeVisiblePositions(state);
        //for(int i = 0; i < state.getItemCount(); i++) {
        for(Integer i : visiblePositions) {
            View view = viewCache.get(i);
            if(view == null) {
                view = recycler.getViewForPosition(i);
                addView(view);
                measureChildWithMargins(view, 0, 0);

                RowSpecifierAdapter.MetadataHolder metadataHolder = rowSpecifierAdapter.getMetadataHolderForPosition(i);
                view.layout(
                        ScreenUtils.dpToPx(metadataHolder.getAccumulatedWidth()) - anchorInfo.currentScrolledX,
                        ScreenUtils.dpToPx(metadataHolder.getAccumulatedHeight()) - anchorInfo.currentScrolledY,
                        ScreenUtils.dpToPx(metadataHolder.getAccumulatedWidth()+metadataHolder.getWidth()) - anchorInfo.currentScrolledX,
                        ScreenUtils.dpToPx(metadataHolder.getAccumulatedHeight()+metadataHolder.getHeight()) - anchorInfo.currentScrolledY); //assuming uniform height per row
                //layoutState.columnCount++;
            } else {
                attachView(view);
                viewCache.remove(i);
            }
        }
        for(Integer i : viewCache.keySet()) {
            recycler.recycleView(viewCache.get(i));
        }
    }

    private void ensureAnchorInfo() {
        if(anchorInfo == null) {
            anchorInfo = new AnchorInfo();
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
        int delta = scrollVerticallyInternal(dy, state);
        offsetChildrenVertical(-delta);
        fill(recycler, state);
        return delta;
    }

    private int scrollVerticallyInternal(int dy, RecyclerView.State state) {
        int childCount = getChildCount();
        if(childCount == 0) {
            return 0;
        }

        int totalCount = state.getItemCount();
        RowSpecifierAdapter.MetadataHolder lastMetadata = rowSpecifierAdapter.getMetadataHolderForPosition(totalCount - 1);
        ensureAnchorInfo();
        int currentScrollY = anchorInfo.currentScrolledY;

        if(dy < 0) {
            if(currentScrollY + dy < 0) {
                anchorInfo.currentScrolledY = 0;
                return -currentScrollY;
            } else {
                anchorInfo.currentScrolledY += dy;
                return dy;
            }
        } else {
            int lastHeightDp = lastMetadata.getAccumulatedHeight() + lastMetadata.getHeight();
            int lastHeight = ScreenUtils.dpToPx(lastHeightDp);
            if(lastHeight - getHeight() <= currentScrollY + dy) {
                anchorInfo.currentScrolledY = lastHeight - getHeight();
                return (lastHeight - getHeight()) - currentScrollY;
            } else {
                anchorInfo.currentScrolledY += dy;
                return dy;
            }
        }
    }

    @Override
    public int scrollHorizontallyBy(int dx, RecyclerView.Recycler recycler, RecyclerView.State state) {
        int delta = scrollHorizontallyInternal(dx, state);
        offsetChildrenHorizontal(-delta);
        fill(recycler, state);
        return delta;
    }

    private int scrollHorizontallyInternal(int dx, RecyclerView.State state) {
        int childCount = getChildCount();
        if(childCount == 0) {
            return 0;
        }

        int totalCount = state.getItemCount();
        RowSpecifierAdapter.MetadataHolder lastMetadata = rowSpecifierAdapter.getMetadataHolderForPosition(totalCount - 1);
        ensureAnchorInfo();
        int currentScrollX = anchorInfo.currentScrolledX;

        if(dx < 0) {
            if(currentScrollX + dx < 0) {
                anchorInfo.currentScrolledX = 0;
                return -currentScrollX;
            } else {
                anchorInfo.currentScrolledX += dx;
                return dx;
            }
        } else {
            int lastWidthDp = lastMetadata.getAccumulatedWidth() + lastMetadata.getWidth();
            int lastWidth = ScreenUtils.dpToPx(lastWidthDp);
            if(lastWidth - getWidth() <= currentScrollX + dx) {
                anchorInfo.currentScrolledX = lastWidth - getWidth();
                return (lastWidth - getWidth()) - currentScrollX;
            } else {
                anchorInfo.currentScrolledX += dx;
                return dx;
            }
        }
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
