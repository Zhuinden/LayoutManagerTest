package hu.zhu.vga.layoutmanagertest.newskeleton;


import android.os.Parcel;
import android.os.Parcelable;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import hu.zhu.vga.layoutmanagertest.ScreenUtils;

/**
 * Created by Owner on 2016.01.28.
 */
public class SkeletonUberLayoutManager
        extends RecyclerView.LayoutManager {
    private SkeletonUberAdapter rowSpecifierAdapter;

    public SkeletonUberLayoutManager(SkeletonUberAdapter rowSpecifierAdapter) {
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
                SkeletonUberAdapter.MetadataHolder metadataHolder = rowSpecifierAdapter.getMetadataHolderForPosition(i);
                layoutParams.width = ScreenUtils.dpToPx(metadataHolder.getWidth());
                layoutParams.height = ScreenUtils.dpToPx(metadataHolder.getHeight());
                view.setLayoutParams(layoutParams);
                addView(view);
                measureChildWithMargins(view, 0, 0);

                view.layout(ScreenUtils.dpToPx(metadataHolder.getAccumulatedWidth()) - (metadataHolder.isHorizontalScrollable() ? anchorInfo.currentScrolledX : 0),
                        ScreenUtils.dpToPx(metadataHolder.getAccumulatedHeight()) - (metadataHolder.isVerticalScrollable() ? anchorInfo.currentScrolledY : 0),
                        ScreenUtils.dpToPx(metadataHolder.getAccumulatedWidth() + metadataHolder.getWidth()) - (metadataHolder.isHorizontalScrollable() ? anchorInfo.currentScrolledX : 0),
                        ScreenUtils.dpToPx(metadataHolder.getAccumulatedHeight() + metadataHolder.getHeight()) - (metadataHolder.isVerticalScrollable() ? anchorInfo.currentScrolledY : 0)); //assuming uniform height per row
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
        anchorInfo.currentScrolledX = ScreenUtils.dpToPx(rowSpecifierAdapter.getMetadataHolderForPosition(position).getAccumulatedWidth());
        anchorInfo.currentScrolledY = ScreenUtils.dpToPx(rowSpecifierAdapter.getMetadataHolderForPosition(position).getAccumulatedHeight());
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
        return true;
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

        int totalCount = state.getItemCount();
        SkeletonUberAdapter.MetadataHolder lastMetadata = rowSpecifierAdapter.getMetadataHolderForPosition(totalCount - 1);
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
        if(state.getItemCount() > 0) {
            int delta = scrollHorizontallyInternal(dx, state);
            offsetChildrenHorizontal(-delta);
            fill(recycler, state);
            return delta;
        } else {
            return 0;
        }
    }

    private int scrollHorizontallyInternal(int dx, RecyclerView.State state) {
        int childCount = getChildCount();
        if(childCount == 0) {
            return 0;
        }

        int totalCount = state.getItemCount();
        SkeletonUberAdapter.MetadataHolder lastMetadata = rowSpecifierAdapter.getMetadataHolderForPosition(totalCount - 1);
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
            //int lastWidthDp = lastMetadata.getAccumulatedWidth() + lastMetadata.getWidth();
            int lastWidth = ScreenUtils.dpToPx(9720);
            if(lastWidth - getWidth() <= currentScrollX + dx) {
                anchorInfo.currentScrolledX = lastWidth - getWidth();
                return (lastWidth - getWidth()) - currentScrollX;
            } else {
                anchorInfo.currentScrolledX += dx;
                return dx;
            }
        }
    }

    // VISIBLE POSITIONS
    private boolean isVisible(int position) {
        ensureAnchorInfo();
        SkeletonUberAdapter.MetadataHolder current = rowSpecifierAdapter.getMetadataHolderForPosition(position);
        if(current != null) {
            int accumulatedWidth = current.getAccumulatedWidth();
            if((ScreenUtils.dpToPx(accumulatedWidth + current.getWidth()) > anchorInfo.currentScrolledX) || (ScreenUtils.dpToPx(
                    accumulatedWidth) >= anchorInfo.currentScrolledX && ScreenUtils.dpToPx(accumulatedWidth) < (anchorInfo.currentScrolledX + getWidth()))) { //is inside rectangle
                return true;
            }
        }
        return false;
    }

    public void initializeVisiblePositions(RecyclerView.State state) {
        List<Integer> staticPositions = new ArrayList<>();
        visiblePositions.clear();
        ensureAnchorInfo();
        int currentScrollY = anchorInfo.currentScrolledY;

        //START: FIND WINDOW IN WHICH VIEWS ARE VISIBLE
        int accumulatedHeight = 0;
        int startRow = 0;

        // START http://stackoverflow.com/a/33264687/2413303
        int n = rowSpecifierAdapter.metadataHolderArrays.size();
        int first = 0;
        int last = n - 1;
        int middle = (first + last) / 2;
        int search = currentScrollY;
        while(first <= last) {
            if(ScreenUtils.dpToPx(rowSpecifierAdapter.metadataHolderArrays.get(middle).get(0).getAccumulatedHeight()) < search) {
                first = middle + 1;
            } else if(ScreenUtils.dpToPx(rowSpecifierAdapter.metadataHolderArrays.get(middle).get(0).getAccumulatedHeight()) == search) {
                break;
            } else {
                last = middle - 1;
            }
            middle = (first + last) / 2;
        }
        // END http://stackoverflow.com/a/33264687/2413303
        startRow = middle;
        accumulatedHeight = rowSpecifierAdapter.metadataHolderArrays.get(middle).get(0).getAccumulatedHeight();

        int endRow = startRow;
        for(int i = startRow; i < rowSpecifierAdapter.metadataHolderArrays.size(); i++) {
            int currentHeight = rowSpecifierAdapter.metadataHolderArrays.get(i).get(0).getHeight();
            if(ScreenUtils.dpToPx(accumulatedHeight + currentHeight) > (currentScrollY + getHeight()) || i == rowSpecifierAdapter.metadataHolderArrays
                    .size() - 1) {
                endRow = i;
                if(endRow != rowSpecifierAdapter.metadataHolderArrays.size() - 1) {
                    endRow++;
                }
                break;
            } else {
                accumulatedHeight += currentHeight;
            }
        }

        if(startRow > 0) {
            startRow--; //show previous row
        }

        for(int i = startRow; i <= endRow; i++) {
            int currentScrollX = anchorInfo.currentScrolledX;
            List<SkeletonUberAdapter.MetadataHolder> widthArray = rowSpecifierAdapter.metadataHolderArrays.get(i);
            int accumulatedWidth = 0;
            int startColumn = 0;

            // START http://stackoverflow.com/a/33264687/2413303
            n = widthArray.size();
            first = 0;
            last = n - 1;
            middle = (first + last) / 2;
            search = currentScrollX;
            while(first <= last) {
                if(ScreenUtils.dpToPx(widthArray.get(middle).getAccumulatedWidth()) < search) {
                    first = middle + 1;
                } else if(ScreenUtils.dpToPx(widthArray.get(middle).getAccumulatedWidth()) == search) {
                    break;
                } else {
                    last = middle - 1;
                }
                middle = (first + last) / 2;
            }
            // END http://stackoverflow.com/a/33264687/2413303
            startColumn = middle;
            accumulatedWidth = widthArray.get(middle).getAccumulatedWidth();

            int endColumn = startColumn;
            for(int k = startColumn; k < widthArray.size(); k++) {
                int currentWidth = widthArray.get(k).getWidth();
                if(ScreenUtils.dpToPx(accumulatedWidth + currentWidth) > currentScrollX + getWidth() || k == widthArray.size() - 1) {
                    endColumn = k;
                    if(endColumn != widthArray.size() - 1) {
                        endColumn++;
                    }
                    break;
                } else {
                    accumulatedWidth += currentWidth;
                }
            }
            if(startColumn > 0) {
                startColumn--; //show previous column too
            }

            for(int column = startColumn; column <= endColumn; column++) {
                int position = rowSpecifierAdapter.metadataHolderArrays.get(i).get(column).getPosition();
                if(isVisible(position)) {
                    visiblePositions.add(position);
                }
            }
        }
        //END: FIND WINDOWS IN WHICH VIEWS ARE VISIBLE

        if(!(rowSpecifierAdapter.metadataHolderArrays.get(0)
                .get(0)
                .isHorizontalScrollable() && rowSpecifierAdapter.metadataHolderArrays.get(0).get(0).isVerticalScrollable())) {
            staticPositions.add(rowSpecifierAdapter.metadataHolderArrays.get(0).get(0).getPosition()); // loading indicator
        }

        // START http://stackoverflow.com/a/33264687/2413303
        n = rowSpecifierAdapter.metadataHolderArrays.get(0).size();
        first = 0;
        last = n - 1;
        middle = (first + last) / 2;
        search = anchorInfo.currentScrolledX;
        while(first <= last) {
            if(ScreenUtils.dpToPx(rowSpecifierAdapter.metadataHolderArrays.get(0).get(middle).getAccumulatedWidth()) < search) {
                first = middle + 1;
            } else if(ScreenUtils.dpToPx(rowSpecifierAdapter.metadataHolderArrays.get(0).get(middle).getAccumulatedWidth()) == search) {
                break;
            } else {
                last = middle - 1;
            }
            middle = (first + last) / 2;
        }
        // END http://stackoverflow.com/a/33264687/2413303
        int startColumn = middle;
        int accumulatedWidth = rowSpecifierAdapter.metadataHolderArrays.get(0).get(middle).getAccumulatedWidth();

        int endColumn = startColumn;
        for(int i = startColumn; i < rowSpecifierAdapter.metadataHolderArrays.get(0).size(); i++) {
            int currentWidth = rowSpecifierAdapter.metadataHolderArrays.get(0).get(i).getWidth();
            if(ScreenUtils.dpToPx(accumulatedWidth + currentWidth) > (anchorInfo.currentScrolledX + getWidth()) || i == rowSpecifierAdapter.metadataHolderArrays
                    .get(0)
                    .size() - 1) {
                endColumn = i;
                if(endColumn != rowSpecifierAdapter.metadataHolderArrays.get(0).size() - 1) {
                    endColumn++;
                }
                break;
            } else {
                accumulatedWidth += currentWidth;
            }
        }

        if(startColumn > 0) {
            startColumn--; //show previous row
        }

        for(int i = startColumn; i <= endColumn; i++) {
            staticPositions.add(rowSpecifierAdapter.metadataHolderArrays.get(0).get(i).getPosition());
        }

        // START http://stackoverflow.com/a/33264687/2413303
        n = rowSpecifierAdapter.metadataHolderArrays.size();
        first = 0;
        last = n - 1;
        middle = (first + last) / 2;
        search = anchorInfo.currentScrolledY;
        while(first <= last) {
            if(ScreenUtils.dpToPx(rowSpecifierAdapter.metadataHolderArrays.get(middle).get(0).getAccumulatedHeight()) < search) {
                first = middle + 1;
            } else if(ScreenUtils.dpToPx(rowSpecifierAdapter.metadataHolderArrays.get(middle).get(0).getAccumulatedHeight()) == search) {
                break;
            } else {
                last = middle - 1;
            }
            middle = (first + last) / 2;
        }
        // END http://stackoverflow.com/a/33264687/2413303
        startRow = middle;
        accumulatedHeight = rowSpecifierAdapter.metadataHolderArrays.get(middle).get(0).getAccumulatedHeight();

        endRow = startRow;
        for(int i = startRow; i < rowSpecifierAdapter.metadataHolderArrays.size(); i++) {
            int currentHeight = rowSpecifierAdapter.metadataHolderArrays.get(i).get(0).getHeight();
            if(ScreenUtils.dpToPx(accumulatedHeight + currentHeight) >= (anchorInfo.currentScrolledY + getHeight()) || i == rowSpecifierAdapter.metadataHolderArrays
                    .size() - 1) {
                endRow = i;
                if(endRow != rowSpecifierAdapter.metadataHolderArrays.size() - 1) {
                    endRow++;
                }
                break;
            } else {
                accumulatedHeight += currentHeight;
            }
        }

        if(startRow > 0) {
            startRow--; //show previous row
        }

        for(int i = startRow; i <= endRow; i++) {
            staticPositions.add(rowSpecifierAdapter.metadataHolderArrays.get(i).get(0).getPosition());
        }

        for(Integer i : staticPositions) {
            visiblePositions.add(i);
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