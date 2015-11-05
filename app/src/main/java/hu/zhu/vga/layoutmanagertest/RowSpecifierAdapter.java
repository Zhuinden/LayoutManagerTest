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
                    100};
    //@formatter:on

    public int getRowNumber(int scrolledY) {
        int accumulatedHeight = 0;
        int row = 0;
        for(int height : heightArray) {
            if(scrolledY > accumulatedHeight && scrolledY < accumulatedHeight + height) {
                return row;
            } else {
                accumulatedHeight += height;
            }
        }
        return row;
    }

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

    public int getRowCountForPosition(int position) {
        if(!positionToRowAndAccumulatedWidthMap.containsKey(position)) {
            initializeMetadataForPosition(position);
        }
        return positionToRowAndAccumulatedWidthMap.get(position).getRowCount();
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
