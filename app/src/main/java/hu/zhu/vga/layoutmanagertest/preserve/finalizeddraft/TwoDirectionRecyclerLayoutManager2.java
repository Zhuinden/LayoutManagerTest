package hu.zhu.vga.layoutmanagertest.preserve.finalizeddraft;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import hu.zhu.vga.layoutmanagertest.ScreenUtils;

/**
 * Created by Zhuinden on 2015.11.05..
 */
public class TwoDirectionRecyclerLayoutManager2
        extends RecyclerView.LayoutManager {
    private RowSpecifierAdapter rowSpecifierAdapter;

    public TwoDirectionRecyclerLayoutManager2(RowSpecifierAdapter rowSpecifierAdapter) {
        this.rowSpecifierAdapter = rowSpecifierAdapter;
        this.visiblePositions = new ArrayList<>();
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
        if((ScreenUtils.dpToPx(accumulatedWidth + current.getWidth()) > anchorInfo.currentScrolledX) || (ScreenUtils.dpToPx(accumulatedWidth) >= anchorInfo.currentScrolledX && ScreenUtils
                .dpToPx(accumulatedWidth) < (anchorInfo.currentScrolledX + getWidth()))) { //is inside rectangle
            return true;
        }
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

        for(Integer i : visiblePositions) {
            View view = viewCache.get(i);
            if(view == null) {
                view = recycler.getViewForPosition(i);
                addView(view);
                measureChildWithMargins(view, 0, 0);

                RowSpecifierAdapter.MetadataHolder metadataHolder = rowSpecifierAdapter.getMetadataHolderForPosition(i);
                view.layout(ScreenUtils.dpToPx(metadataHolder.getAccumulatedWidth()) - anchorInfo.currentScrolledX,
                        ScreenUtils.dpToPx(metadataHolder.getAccumulatedHeight()) - anchorInfo.currentScrolledY,
                        ScreenUtils.dpToPx(metadataHolder.getAccumulatedWidth() + metadataHolder.getWidth()) - anchorInfo.currentScrolledX,
                        ScreenUtils.dpToPx(metadataHolder.getAccumulatedHeight() + metadataHolder.getHeight()) - anchorInfo.currentScrolledY); //assuming uniform height per row
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

    static class RowSpecifierAdapter
            extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private Map<Integer, MetadataHolder> positionToRowAndAccumulatedWidthMap;

        private int totalCount = 0;

        public RowSpecifierAdapter() {
            setDatastructures();
        }

        protected void setDatastructures() {
            positionToRowAndAccumulatedWidthMap = new TreeMap<>();
            totalCount = 0;
            for(int[] array : widthArrays) {
                for(int i : array) {
                    totalCount++;
                }
            }
            //lazy init in onBindViewHolder
        }

        //@formatter:off
    private int[][] widthArrays = new int[][]{
            new int[]{200, 100, 200, 250, 250},
            new int[]{50, 100, 150, 200, 200, 200, 100},
            new int[]{250, 100, 100, 50, 100, 200, 200},
            new int[]{150, 100, 250, 250, 100, 150},
            new int[]{100, 150, 100, 150, 350, 150},
            new int[]{100, 150, 150, 100, 100, 200, 200},
            new int[]{200, 150, 150, 150, 150, 100, 100},
            new int[]{300, 100, 100, 200, 100, 50, 150},
            new int[]{100, 100, 200, 100, 300, 200},
            new int[]{200, 100, 200, 100, 300, 100},
            new int[]{200, 100, 200, 250, 250},
            new int[]{50, 100, 150, 200, 200, 200, 100},
            new int[]{250, 100, 100, 50, 100, 200, 200},
            new int[]{150, 100, 250, 250, 100, 150},
            new int[]{100, 150, 100, 150, 350, 150},
            new int[]{100, 150, 150, 100, 100, 200, 200},
            new int[]{200, 150, 150, 150, 150, 100, 100},
            new int[]{300, 100, 100, 200, 100, 50, 150},
            new int[]{100, 100, 200, 100, 300, 200},
            new int[]{200, 100, 200, 100, 300, 100}};

    private int[] heightArray =
            new int[]{
                    100,
                    100,
                    100,
                    100,
                    100,
                    100,
                    100,
                    100,
                    100,
                    100,
                    100,
                    100,
                    100,
                    100,
                    100,
                    100,
                    100,
                    100,
                    100,
                    100};
    //@formatter:on

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new TestViewHolder(new TextView(parent.getContext()));
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            initializeMetadataForPosition(position);
            MetadataHolder metadataHolder = getMetadataHolderForPosition(position);
            String positionText = (position < 10) ? "0" + position : "" + position;
            //Log.i(getClass().getSimpleName(), "[" + position + "] Initializing as [" + positionText + "] for row [" + row + "] and height [" + height + "]");
            ((TestViewHolder) holder).getTextView().setText("[" + positionText + "] Hello World!");
            ((TestViewHolder) holder).getTextView()
                    .setLayoutParams(new RecyclerView.LayoutParams(ScreenUtils.dpToPx(metadataHolder.getWidth()),
                            ScreenUtils.dpToPx(metadataHolder.getHeight())));
        }

        @Override
        public int getItemCount() {
            return totalCount;
        }

        public MetadataHolder getMetadataHolderForPosition(int position) {
            if(position >= 0 && position < getItemCount()) {
                if(!positionToRowAndAccumulatedWidthMap.containsKey(position)) {
                    initializeMetadataForPosition(position);
                }
                return positionToRowAndAccumulatedWidthMap.get(position);
            } else {
                return null;
            }
        }

        private void initializeMetadataForPosition(int position) {
            int seek = 0;
            int width = 0;
            int row = 0;
            int accumulatedWidth = 0;
            int height = 0;
            int accumulatedHeight = 0;
            for(int i = 0; i < widthArrays.length; i++) {
                int[] array = widthArrays[i];
                if(position < seek + array.length) {
                    width = array[position - seek];
                    row = i;
                    accumulatedWidth = 0;
                    for(int j = 0; j < position - seek; j++) {
                        accumulatedWidth += array[j];
                    }
                    height = heightArray[row];
                    MetadataHolder metadataHolder = new MetadataHolder();
                    metadataHolder.setPosition(position);
                    metadataHolder.setAccumulatedWidth(accumulatedWidth);
                    metadataHolder.setWidth(width);
                    metadataHolder.setRowCount(row);
                    metadataHolder.setHeight(height);
                    metadataHolder.setAccumulatedHeight(accumulatedHeight);
                    positionToRowAndAccumulatedWidthMap.put(position, metadataHolder);
                    break;
                } else {
                    seek += array.length;
                    accumulatedHeight += heightArray[i];
                }
            }
        }

        static class MetadataHolder {
            private int width;
            private int height;
            private int accumulatedHeight;
            private int accumulatedWidth;
            private int position;
            private int rowCount;


            public int getWidth() {
                return width;
            }

            public void setWidth(int width) {
                this.width = width;
            }

            public int getAccumulatedWidth() {
                return accumulatedWidth;
            }

            public void setAccumulatedWidth(int accumulatedWidth) {
                this.accumulatedWidth = accumulatedWidth;
            }

            public int getPosition() {
                return position;
            }

            public void setPosition(int position) {
                this.position = position;
            }

            public int getRowCount() {
                return rowCount;
            }

            public void setRowCount(int rowCount) {
                this.rowCount = rowCount;
            }

            public int getHeight() {
                return height;
            }

            public void setHeight(int height) {
                this.height = height;
            }

            public int getAccumulatedHeight() {
                return accumulatedHeight;
            }

            public void setAccumulatedHeight(int accumulatedHeight) {
                this.accumulatedHeight = accumulatedHeight;
            }
        }
    }

    static class TestViewHolder extends RecyclerView.ViewHolder {

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
}
