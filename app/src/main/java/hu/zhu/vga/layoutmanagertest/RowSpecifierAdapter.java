package hu.zhu.vga.layoutmanagertest;

import android.support.v7.widget.RecyclerView;
import android.util.Pair;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Map;
import java.util.TreeMap;

/**
 * Created by Zhuinden on 2015.11.05..
 */
public class RowSpecifierAdapter
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
    public int[][] positionArrays = new int[][] {
            new int[]{0, 1, 2, 3, 4},
            new int[]{5, 6, 7, 8, 9, 10, 11},
            new int[]{12, 13, 14, 15, 16, 17, 18},
            new int[]{19, 20, 21, 22, 23, 24},
            new int[]{25, 26, 27, 28, 29, 30},
            new int[]{31, 32, 33, 34, 35, 36, 37},
            new int[]{38, 39, 40, 41, 42, 43, 44},
            new int[]{45, 46, 47, 48, 49, 50, 51},
            new int[]{52, 53, 54, 55, 56, 57},
            new int[]{58, 59, 60, 61, 62, 63},
            new int[]{64, 65, 66, 67, 68},
            new int[]{69, 70, 71, 72, 73, 74, 75},
            new int[]{76, 77, 78, 79, 80, 81, 82},
            new int[]{83, 84, 85, 86, 87, 88},
            new int[]{89, 90, 91, 92, 93, 94},
            new int[]{95, 96, 97, 98, 99, 100, 101},
            new int[]{102, 103, 104, 105, 106, 107, 108},
            new int[]{109, 110, 111, 112, 113, 114, 115},
            new int[]{116, 117, 118, 119, 120, 121},
            new int[]{122, 123, 124, 125, 126, 127}};

    public int[][] widthArrays = new int[][]{
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

    public int[] heightArray =
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

    public static class MetadataHolder {
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
