import android.os.Parcel;
import android.os.Parcelable;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Zhuinden on 2016.06.16..
 */
public class CalendarWeekDayLayoutManager
        extends RecyclerView.LayoutManager {
    private CalendarWeekDayAdapter rowSpecifierAdapter;

    public CalendarWeekDayLayoutManager(CalendarWeekDayAdapter rowSpecifierAdapter) {
        this.rowSpecifierAdapter = rowSpecifierAdapter;
        this.visiblePositions = new ArrayList<>();
        ensureAnchorInfo();
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

        for(Integer i : visiblePositions) {
            View view = viewCache.get(i);
            if(view == null) {
                view = recycler.getViewForPosition(i);

                RecyclerView.LayoutParams layoutParams = (RecyclerView.LayoutParams) view.getLayoutParams();
                CalendarWeekDayAdapter.MetadataHolder metadataHolder = rowSpecifierAdapter.getMetadataHolderForPosition(i);
                layoutParams.width = metadataHolder.getWidth();
                layoutParams.height = metadataHolder.getHeight();
                view.setLayoutParams(layoutParams);
                addView(view);
                measureChildWithMargins(view, 0, 0);

                view.layout(metadataHolder.getAccumulatedWidth() - (metadataHolder.isHorizontalScrollable() ? anchorInfo.currentScrolledX : 0),
                        metadataHolder.getAccumulatedHeight() - (metadataHolder.isVerticalScrollable() ? anchorInfo.currentScrolledY : 0),
                        metadataHolder.getAccumulatedWidth() + metadataHolder.getWidth() - (metadataHolder.isHorizontalScrollable() ? anchorInfo.currentScrolledX : 0),
                        metadataHolder.getAccumulatedHeight() + metadataHolder.getHeight() - (metadataHolder.isVerticalScrollable() ? anchorInfo.currentScrolledY : 0));
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
        ensureAnchorInfo();
        anchorInfo.currentScrolledX = rowSpecifierAdapter.getMetadataHolderForPosition(position).getAccumulatedWidth();
        anchorInfo.currentScrolledY = rowSpecifierAdapter.getMetadataHolderForPosition(position).getAccumulatedHeight();
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

    // SCROLLING


    @Override
    public boolean canScrollVertically() {
        return true;
    }

    @Override
    public boolean canScrollHorizontally() {
        return false;
    }

    @Override
    public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
        if(state.getItemCount() > 0) {
            int delta = scrollVerticallyInternal(dy, state);
            offsetChildrenVertical(-delta);
            fill(recycler, state);
            return delta;
        } else {
            return 0;
        }
    }


    private int scrollVerticallyInternal(int dy, RecyclerView.State state) {
        int childCount = getChildCount();
        if(childCount == 0) {
            return 0;
        }

        CalendarWeekDayAdapter.MetadataHolder lastMetadata = rowSpecifierAdapter.getMetadataHolderForPosition(23); // 23:00
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
            int lastHeightPx = lastMetadata.getAccumulatedHeight() + lastMetadata.getHeight();
            int lastHeight = lastHeightPx;
            if(lastHeight - getHeight() <= currentScrollY + dy) {
                anchorInfo.currentScrolledY = lastHeight - getHeight();
                if(anchorInfo.currentScrolledY < 0) { //ugly hack fix if total height is smaller than the recycler height
                    anchorInfo.currentScrolledY = 0;
                    return 0;
                } else {
                    return (lastHeight - getHeight()) - currentScrollY;
                }
            } else {
                anchorInfo.currentScrolledY += dy;
                if(anchorInfo.currentScrolledY < 0) { //ugly hack fix if total height is smaller than the recycler height
                    anchorInfo.currentScrolledY = 0;
                    return 0;
                } else {
                    return dy;
                }
            }
        }
    }

    @Override
    public int scrollHorizontallyBy(int dx, RecyclerView.Recycler recycler, RecyclerView.State state) {
        return 0;
    }

    // VISIBLE POSITIONS
    private boolean isVisible(int position) {
        ensureAnchorInfo();
        CalendarWeekDayAdapter.MetadataHolder current = rowSpecifierAdapter.getMetadataHolderForPosition(position);
        if(current != null) {
            int currentScrollY = anchorInfo.currentScrolledY;
            int width = current.getWidth();
            int height = current.getHeight();
            int accWidth = current.getAccumulatedWidth();
            int accHeight = current.getAccumulatedHeight();
            boolean isInFromTop = accHeight <= currentScrollY && (accHeight + height) >= currentScrollY;
            boolean isInFromBottom = accHeight <= (currentScrollY + getHeight()) && (accHeight + height) >= (currentScrollY + getHeight());
            boolean isInMiddle = accHeight >= currentScrollY && (accHeight + height) <= (currentScrollY + getHeight());
            boolean isOverlapping = accHeight <= currentScrollY && (height + accHeight) >= (currentScrollY + getHeight());
            return isInFromTop || isInFromBottom || isInMiddle || isOverlapping;
        }
        return false;
    }

    public void initializeVisiblePositions(RecyclerView.State state) {
        ensureAnchorInfo();
        visiblePositions.clear();
        for(int i = 0; i < state.getItemCount(); i++) {
            if(isVisible(i)) {
                visiblePositions.add(i);
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
