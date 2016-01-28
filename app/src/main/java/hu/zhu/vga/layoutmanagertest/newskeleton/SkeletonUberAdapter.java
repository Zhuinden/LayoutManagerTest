package hu.zhu.vga.layoutmanagertest.newskeleton;


import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.JobManager;
import com.path.android.jobqueue.Params;
import com.path.android.jobqueue.TagConstraint;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.inject.Inject;

import hu.zhu.vga.layoutmanagertest.R;
import hu.zhu.vga.layoutmanagertest.ScreenUtils;

/**
 * Created by Owner on 2016.01.28.
 */
public class SkeletonUberAdapter
        extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    @Inject
    JobManager jobManager;

    private Map<Integer, MetadataHolder> positionToMetadataMap;
    public List<List<MetadataHolder>> metadataHolderArrays;

    private int totalCount = 0;

    private boolean initialized = false;
    private boolean valid = false;

    public SkeletonUberAdapter() {
        valid = false;
        initialized = false;
        initForLoading();
    }

    private static MetadataHolder createNewMetadataHolder(int position, int width, int height, int accumulatedWidth, int accumulatedHeight, int rowCount, int columnCount, boolean isHorizontalScrollable, boolean isVerticalScrollable) {
        MetadataHolder metadataHolder = new MetadataHolder();
        metadataHolder.setPosition(position);
        metadataHolder.setWidth(width);
        metadataHolder.setHeight(height);
        metadataHolder.setAccumulatedWidth(accumulatedWidth);
        metadataHolder.setAccumulatedHeight(accumulatedHeight);
        metadataHolder.setRowCount(rowCount);
        metadataHolder.setColumnCount(columnCount);
        metadataHolder.setIsHorizontalScrollable(isHorizontalScrollable);
        metadataHolder.setIsVerticalScrollable(isVerticalScrollable);
        return metadataHolder;
    }

    private void initForLoading() {
        valid = false;

        positionToMetadataMap = new HashMap<>();
        metadataHolderArrays = new ArrayList<>();
        MetadataHolder metadataHolder = createNewMetadataHolder(0,
                ScreenUtils.dpToPx(60),
                ScreenUtils.dpToPx(60),
                0,
                0,
                0,
                0,
                false,
                false);
        positionToMetadataMap.put(0, metadataHolder);
        List<MetadataHolder> metadataHolders = new ArrayList<>(1);
        metadataHolders.add(metadataHolder);
        metadataHolderArrays.add(metadataHolders);
        totalCount = 1;
        initialized = true;
        notifyDataSetChanged();
    }

    public void setDatastructures() {
        initForLoading();
        jobManager.cancelJobs(TagConstraint.ANY, "refresh");
        jobManager.addJobInBackground(new RefreshJob());
    }

    public void onEventMainThread(IndexEvent indexEvent) {
        positionToMetadataMap = indexEvent.positionToMetadataMap;
        metadataHolderArrays = indexEvent.metadataHolderArrays;
        totalCount = indexEvent.totalCount;
        valid = true;
        notifyDataSetChanged();
    }

    enum ViewTypes {
        //...
        LOADING
    }

    @Override
    public int getItemViewType(int position) {
        return 0; // TODO
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if(viewType == ViewTypes.LOADING.ordinal()) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.viewholder_progress, parent, false);
            ProgressViewHolder progressViewHolder = new ProgressViewHolder(view);
            progressViewHolder.getRoot()
                    .setLayoutParams(new RecyclerView.LayoutParams(ScreenUtils.getScreenWidth(), ScreenUtils.dpToPx(120)));
            progressViewHolder.startProgress("", true);
            return progressViewHolder;
        } // TODO
        throw new IllegalArgumentException("Invalid view type [" + viewType + "]");
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder abstractHolder, int position) {
        MetadataHolder metadataHolder = getMetadataHolderForPosition(position);
        // TODO
    }

    @Override
    public int getItemCount() {
        if(!initialized) {
            return 0;
        }
        if(!valid) {
            return 1;
        }
//        if(!RealmChecker.isValid(tvGroups)) {
//            setDatastructures();
//        }
        return totalCount;
    }

    public MetadataHolder getMetadataHolderForPosition(int position) {
        if(position >= 0 && position < getItemCount()) {
            return positionToMetadataMap.get(position);
        } else {
            return null;
        }
    }

    public boolean isVerticalScrollable(int position) {
        return true;
    }

    public boolean isHorizontalScrollable(int position) {
        return true;
    }

    public boolean isStatic(int position) {
        return false;
    }

    public static class MetadataHolder {
        private int width;
        private int height;
        private int accumulatedHeight;
        private int accumulatedWidth;
        private int position;
        private int rowCount;
        private int columnCount;

        private boolean isVerticalScrollable;
        private boolean isHorizontalScrollable;

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

        public boolean isVerticalScrollable() {
            return isVerticalScrollable;
        }

        public void setIsVerticalScrollable(boolean isVerticalScrollable) {
            this.isVerticalScrollable = isVerticalScrollable;
        }

        public boolean isHorizontalScrollable() {
            return isHorizontalScrollable;
        }

        public void setIsHorizontalScrollable(boolean isHorizontalScrollable) {
            this.isHorizontalScrollable = isHorizontalScrollable;
        }

        public int getColumnCount() {
            return columnCount;
        }

        public void setColumnCount(int columnCount) {
            this.columnCount = columnCount;
        }
    }

    public static class RefreshJob
            extends Job {
        private Date currentDate;

        protected RefreshJob() {
            super(new Params(1).addTags("refresh").groupBy("refresh"));
        }

        @Override
        public void onAdded() {
            Log.d("RefreshJob", "Added [RefreshJob]");
        }

        private Map<Integer, MetadataHolder> positionToMetadataMap;
        public List<List<MetadataHolder>> metadataHolderArrays;

        private int totalCount = 0;

        @Override
        public void onRun()
                throws Throwable {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");

            //Realm realm = null;
            //try {
            //    realm = Realm.getInstance(CustomApplication.get().getAppConfig().getDefaultRealmConfiguration());
            metadataHolderArrays = new ArrayList<>(1000);
            positionToMetadataMap = new TreeMap<>();
            int rowCount = 0;
            int columnCount = 0;
            int accumulatedWidth = 0;
            int accumulatedHeight = 0;

            int positionCounter = 0;
            totalCount = 0;

            // START TABLET TIME TRACKER ADAPTER ELEMENTS

            MetadataHolder metadataHolder = createNewMetadataHolder(positionCounter,
                    70,
                    34,
                    accumulatedWidth,
                    accumulatedHeight,
                    rowCount,
                    columnCount,
                    true,
                    false);
            positionToMetadataMap.put(positionCounter, metadataHolder);
            List<MetadataHolder> metadataHolders = new ArrayList<>(1);
            metadataHolders.add(metadataHolder);
            columnCount++;
            accumulatedWidth += 70;
            positionCounter++;
            accumulatedHeight += 34;
            metadataHolderArrays.add(metadataHolders);

            IndexEvent indexEvent = new IndexEvent();

            indexEvent.positionToMetadataMap = positionToMetadataMap;
            indexEvent.metadataHolderArrays = metadataHolderArrays;
            indexEvent.totalCount = totalCount;
            SingletonBus.INSTANCE.post(indexEvent);
            Log.d("RefreshJob", "Posted index event!");
            //} finally {
            //    if(realm != null) {
            //        realm.close();
            //    }
            //}
            // END TABLET CHANNEL + TABLET ADAPTER
            Log.d("RefreshJob", "Finished setting datastructures [" + totalCount + "]");
        }

        @Override
        protected void onCancel() {
            Log.d("RefreshJob", "Canceled [RefreshJob]");
        }

        @Override
        protected boolean shouldReRunOnThrowable(Throwable throwable) {
            Log.e(getClass().getSimpleName(), "An error occurred during refresh", throwable);
            return false;
        }
    }

    public static class IndexEvent {
        private Map<Integer, MetadataHolder> positionToMetadataMap;
        public List<List<MetadataHolder>> metadataHolderArrays;

        private int totalCount;
    }
}
