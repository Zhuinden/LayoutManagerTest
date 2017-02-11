/**
 * Created by Zhuinden on 2016.06.16..
 */
public class CalendarWeekDayAdapter
        extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    Map<Integer, MetadataHolder> positionToMetadataMap;
    List<MetadataHolder> metadataHolderArray;
    int totalCount = 0;

    RealmResults<CalendarEvent> calendarEvents;

    RealmHolder realmHolder;
    CalendarEventRepository calendarEventRepository;

    Date date;

    public CalendarWeekDayAdapter(Date date) {
        this.date = date;
        metadataHolderArray = new ArrayList<>(50);
        positionToMetadataMap = new TreeMap<>();

        realmHolder = Injector.INSTANCE.getApplicationComponent().realmHolder();
        calendarEventRepository = Injector.INSTANCE.getApplicationComponent().calendarEventRepository();
        setDatastructures();
    }

    public void setDate(Date date) {
        this.date = date;
        setDatastructures();
        notifyDataSetChanged();
    }

    private static MetadataHolder createNewMetadataHolder(int position, int width, int height, int accumulatedWidth, int accumulatedHeight, ViewTypes viewType) {
        MetadataHolder metadataHolder = new MetadataHolder();
        metadataHolder.setPosition(position);
        metadataHolder.setWidth(width);
        metadataHolder.setHeight(height);
        metadataHolder.setAccumulatedWidth(accumulatedWidth);
        metadataHolder.setAccumulatedHeight(accumulatedHeight);
        metadataHolder.setViewType(viewType);
        return metadataHolder;
    }

    enum ViewTypes {
        HOUR,
        EVENT
    }

    @Override
    public int getItemViewType(int position) {
        return positionToMetadataMap.get(position).viewType.ordinal();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if(viewType == ViewTypes.HOUR.ordinal()) {
            return new HourViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.viewgroup_calendarweek_hour, parent, false));
        } else if(viewType == ViewTypes.EVENT.ordinal()) {
            return new EventViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.viewgroup_calendarweek_event, parent, false));
        }
        throw new IllegalArgumentException("Invalid view type [" + viewType + "]");
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder abstractHolder, int position) {
        final MetadataHolder metadataHolder = getMetadataHolderForPosition(position);
        if(abstractHolder instanceof HourViewHolder) {
            HourViewHolder holder = (HourViewHolder) abstractHolder;
            holder.time.setText(metadataHolder.hour);
        } else if(abstractHolder instanceof EventViewHolder) {
            final long calendarEventId = metadataHolder.calendarEventId;
            CalendarEvent calendarEvent = calendarEventRepository.findOne(realmHolder.getRealm(), metadataHolder.calendarEventId);
            EventViewHolder eventViewHolder = (EventViewHolder) abstractHolder;
            if(calendarEvent != null) {
                eventViewHolder.eventText.setText(calendarEvent.getTitle());
                int eventColor = calendarEventRepository.findSelectedColorForEvent(realmHolder.getRealm(), calendarEvent);
                eventViewHolder.container.setBackgroundColor(eventColor);
            }

            eventViewHolder.container.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Flow.get(v).set(CalendarEventKey.create(calendarEventId, false));
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        if(!RealmChecker.isValid(calendarEvents)) {
            setDatastructures();
        }
        return totalCount;
    }

    public MetadataHolder getMetadataHolderForPosition(int position) {
        if(position >= 0 && position < getItemCount()) {
            return positionToMetadataMap.get(position);
        } else {
            return null;
        }
    }

    public static class MetadataHolder {
        private int width;
        private int height;
        private int accumulatedHeight;
        private int accumulatedWidth;
        private int position;
        private ViewTypes viewType;
        private long calendarEventId;

        private String hour;

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

        public ViewTypes getViewType() {
            return viewType;
        }

        public void setViewType(ViewTypes viewType) {
            this.viewType = viewType;
        }

        public boolean isHorizontalScrollable() {
            return false;
        }

        public boolean isVerticalScrollable() {
            return true;
        }

        public long getCalendarEventId() {
            return calendarEventId;
        }

        public void setCalendarEventId(long calendarEventId) {
            this.calendarEventId = calendarEventId;
        }
    }

    public void setDatastructures() {
        metadataHolderArray.clear();
        positionToMetadataMap.clear();

        float hourHeightDouble = ResourceUtils.getDimension(R.dimen.calendar_week_hour_block_height);
        int hourHeight = ((Float) (hourHeightDouble)).intValue();
        int topMargin = ((Float) ResourceUtils.getDimension(R.dimen.calendar_week_item_first_top_margin)).intValue();
        int eventLeftMargin = ((Float) ResourceUtils.getDimension(R.dimen.calendar_week_event_leftmargin)).intValue();

        for(int i = 0; i < 24; i++) {
            String time = ((i < 10) ? "0" : "") + i + ":00";
            MetadataHolder metadataHolder = createNewMetadataHolder(i,
                    ScreenUtils.getScreenWidth(),
                    hourHeight,
                    0,
                    topMargin + i * hourHeight,
                    ViewTypes.HOUR);
            metadataHolder.hour = time;
            positionToMetadataMap.put(i, metadataHolder);
            metadataHolderArray.add(metadataHolder);
        }
        Date startOfDay = MiDateUtils.setToMidnight(date);
        Date endOfDay = MiDateUtils.setToEndOfDay(date);
        calendarEvents = calendarEventRepository.findAllForDay(realmHolder.getRealm(), date);
        for(int i = 24; i < 24 + calendarEvents.size(); i++) {
            CalendarEvent calendarEvent = calendarEvents.get(i - 24);
            Date _startDate = calendarEvent.getStartDate();
            Date _endDate = calendarEvent.getEndDate();
            Date startDate;
            Date endDate;
            if(_startDate.getTime() < startOfDay.getTime()) { // fix overlapping event
                startDate = startOfDay;
            } else {
                startDate = _startDate;
            }
            if(_endDate.getTime() > endOfDay.getTime()) { // fix overlapping event
                endDate = endOfDay;
            } else {
                endDate = _endDate;
            }
            long minutesSinceStartOfDay = (startDate.getTime() - startOfDay.getTime()) / 1000 / 60;
            long minutesDuration = (endDate.getTime() - startDate.getTime()) / 1000 / 60;
            MetadataHolder metadataHolder = createNewMetadataHolder(i,
                    ScreenUtils.getScreenWidth() - eventLeftMargin,
                    ((Float) (minutesDuration * (hourHeightDouble / 60))).intValue(),
                    eventLeftMargin,
                    ((Float) ((topMargin + (minutesSinceStartOfDay * (hourHeightDouble / 60))))).intValue(),
                    ViewTypes.EVENT);
            metadataHolder.calendarEventId = calendarEvent.getId();
            positionToMetadataMap.put(i, metadataHolder);
            metadataHolderArray.add(metadataHolder);
        }
        this.totalCount = 24 + calendarEvents.size();
    }

    public static class HourViewHolder
            extends RecyclerView.ViewHolder {
        private String hour;

        @BindView(R.id.calendar_week_time_text)
        TextView time;

        public HourViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    public static class EventViewHolder
            extends RecyclerView.ViewHolder {
        @BindView(R.id.calendar_week_event_container)
        ViewGroup container;

        @BindView(R.id.calendar_weeK_event_text)
        TextView eventText;

        public EventViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
