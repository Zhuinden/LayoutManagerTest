package hu.zhu.vga.layoutmanagertest.preserve;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.v7.widget.RecyclerView;
import android.util.Pair;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Map;
import java.util.TreeMap;

import hu.zhu.vga.layoutmanagertest.ScreenUtils;

/**
 * Created by Zhuinden on 2015.11.05..
 */
public class TwoDirectionNoRecyclerLayoutManager3 extends RecyclerView.LayoutManager {
    class TestViewHolder extends RecyclerView.ViewHolder {

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

    class RowSpecifierAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private Map<Integer, Pair<Integer, Integer>> positionToRowAndWidthMap;

        private int totalCount = 0;

        public RowSpecifierAdapter() {
            setDatastructures();
        }

        protected void setDatastructures() {
            positionToRowAndWidthMap = new TreeMap<>();
            totalCount = 0;
            for(int[] array : widthArrays) {
                for(int i : array) {
                    totalCount++;
                }
            }
            //lazy init in onBindViewHolder
        }

        private int[][] widthArrays = new int[][] {
                new int[] {200, 100, 200, 250, 250},
                new int[] {50, 100, 150, 200, 200, 200, 100},
                new int[] {250, 100, 100, 50, 100, 200, 200},
                new int[] {150, 100, 250, 250, 100, 150},
                new int[] {100, 150, 100, 150, 350, 150},
                new int[] {100, 150, 150, 100, 100, 200, 200},
                new int[] {200, 150, 150, 150, 150, 100, 100},
                new int[] {300, 100, 100, 200, 100, 50, 150},
                new int[] {100, 100, 200, 100, 300, 200},
                new int[] {200, 100, 200, 100, 300, 100}
        };

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new TestViewHolder(new TextView(parent.getContext()));
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            int seek = 0;
            int width = 0;
            for(int i = 0; i < widthArrays.length; i++) {
                int[] array = widthArrays[i];
                if(position < seek+array.length) {
                    width = array[position-seek];
                    getPositionToRowAndWidthMap().put(position, Pair.create(i, width));
                    break;
                } else {
                    seek += array.length;
                }
            }
            String positionText = (position < 10) ? "0" + position : "" + position;
            ((TestViewHolder) holder).getTextView().setText("[" + positionText + "] Hello World!");
            ((TestViewHolder) holder).getTextView()
                    .setLayoutParams(new RecyclerView.LayoutParams(ScreenUtils.dpToPx(width), ScreenUtils.dpToPx(100.0)));
        }

        @Override
        public int getItemCount() {
            return totalCount;
        }

        public Map<Integer, Pair<Integer, Integer>> getPositionToRowAndWidthMap() {
            return positionToRowAndWidthMap;
        }
    }


    private RowSpecifierAdapter rowSpecifierAdapter;

    public TwoDirectionNoRecyclerLayoutManager3(RowSpecifierAdapter rowSpecifierAdapter) {
        this.rowSpecifierAdapter = rowSpecifierAdapter;
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
            this.columnCount = 0;
            this.currentRowAccumulatedWidth = 0;
            this.currentRowAccumulatedHeight = 0;
            this.remainingScreenWidth = ScreenUtils.getScreenWidth();
            this.remainingScreenHeight = ScreenUtils.getScreenHeight();
        }
    }

    protected class AnchorInfo {
        public int currentScrolledX = 0;
        public int currentScrolledY = 0;
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
        for(int i = 0; i < state.getItemCount(); i++) {
            View view = recycler.getViewForPosition(i);
            addView(view);
            measureChildWithMargins(view, 0, 0);

            int decoratedMeasuredWidth = getDecoratedMeasuredWidth(view);
            int decoratedMeasuredHeight = getDecoratedMeasuredHeight(view);

            int previousRow = layoutState.rowCount;
            if(rowSpecifierAdapter.getPositionToRowAndWidthMap().get(i).first > previousRow) {
                layoutState.columnCount = 0;
                layoutState.rowCount = rowSpecifierAdapter.getPositionToRowAndWidthMap().get(i).first;
                layoutState.currentRowAccumulatedWidth = 0;
                layoutState.currentRowAccumulatedHeight += decoratedMeasuredHeight;
                layoutState.remainingScreenWidth = ScreenUtils.getScreenWidth();
                layoutState.remainingScreenHeight -= decoratedMeasuredHeight;
            }
            layoutDecorated(view,
                    layoutState.currentRowAccumulatedWidth,
                    layoutState.currentRowAccumulatedHeight,
                    layoutState.currentRowAccumulatedWidth + decoratedMeasuredWidth,
                    layoutState.currentRowAccumulatedHeight + decoratedMeasuredHeight); //assuming uniform height per row
            layoutState.currentRowAccumulatedWidth += decoratedMeasuredWidth;
            layoutState.columnCount++;
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