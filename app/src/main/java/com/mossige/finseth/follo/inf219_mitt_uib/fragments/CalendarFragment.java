package com.mossige.finseth.follo.inf219_mitt_uib.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mossige.finseth.follo.inf219_mitt_uib.R;
import com.mossige.finseth.follo.inf219_mitt_uib.listeners.MainActivityListener;
import com.mossige.finseth.follo.inf219_mitt_uib.models.CalendarEvent;
import com.mossige.finseth.follo.inf219_mitt_uib.models.MyCalendar;
import com.mossige.finseth.follo.inf219_mitt_uib.retrofit.PaginationUtils;
import com.mossige.finseth.follo.inf219_mitt_uib.retrofit.MittUibClient;
import com.mossige.finseth.follo.inf219_mitt_uib.retrofit.ServiceGenerator;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import hirondelle.date4j.DateTime;
import hugo.weaving.DebugLog;
import retrofit2.Call;
import retrofit2.Callback;


public class CalendarFragment extends Fragment implements com.prolificinteractive.materialcalendarview.OnDateSelectedListener,
        com.prolificinteractive.materialcalendarview.OnMonthChangedListener {

    private static final String TAG = "CalendarFragment";

    private MyCalendar calendar;
    private DateTime previousDateTime;
    private OnDateClickListener callBack;
    private MainActivityListener mCallback;
    private MittUibClient mittUibClient;
    private ArrayList<String> globalContextCodes;
    private String nextPage;
    private String nextPageAssignment;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            mCallback = (MainActivityListener) context;
        } catch (ClassCastException e) {
            //Do nothing
        }

        mittUibClient = ServiceGenerator.createService(MittUibClient.class, context);
    }

    public interface OnDateClickListener {
        void setAgendas(ArrayList<CalendarEvent> events);
    }

    public CalendarFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        nextPage = "";
        nextPageAssignment = "";
        calendar = new MyCalendar();
        initContextCodes();
    }

    /**
     * Creates the list of context codes with the right format.
     * The list is used by the API to know what courses to get events for.
     */
    private void initContextCodes() {
        Bundle arguments = getArguments();
        if (arguments != null) {
            if (arguments.containsKey("ids")) {
                ArrayList<Integer> courseIds = arguments.getIntegerArrayList("ids");

                // Add all course ids for use in context_codes in url
                globalContextCodes = new ArrayList<>();
                for (int i = 0; i < courseIds.size(); i++) {
                    globalContextCodes.add("course_" + courseIds.get(i));
                }
            }

            if (arguments.containsKey("user_id")) {
                globalContextCodes.add("user_" + arguments.getInt("user_id"));
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_calendar, container, false);
        getActivity().setTitle(R.string.calendar_title);

        FragmentTransaction ft = getChildFragmentManager().beginTransaction();

        MaterialCalendarView calendarView = (MaterialCalendarView) rootView.findViewById(R.id.calendarView);
        calendarView.setOnDateChangedListener(this);
        calendarView.setOnMonthChangedListener(this);


        // TODO Remove AgendaFragment and add recycler view to CalendarFragment itself
        AgendaFragment agendaFragment = new AgendaFragment();
        // Init callback to allow communication with AgendaFragment
        callBack = agendaFragment;
        agendaFragment.setArguments(getArguments());
        ft.replace(R.id.agenda_container, agendaFragment);
        ft.commit();

        this.onMonthChanged(calendarView, CalendarDay.today());

//        caldroidFragment = initCalendarFragment();
//        ft.replace(R.id.calendar_container, caldroidFragment);
//
        return rootView;
    }

    @DebugLog
    @Override
    public void onDateSelected(@NonNull MaterialCalendarView widget, @NonNull CalendarDay date, boolean selected) {
        DateTime dateSelected = DateTime.forDateOnly(date.getYear(), date.getMonth() + 1, date.getDay());
        callBack.setAgendas(calendar.getEventsForDate(dateSelected));
    }

    @DebugLog
    @Override
    public void onMonthChanged(MaterialCalendarView widget, CalendarDay date) {
        DateTime curMonth = DateTime.forDateOnly(date.getYear(), date.getMonth() + 1, 1);
        // TODO Refactor
        int requiredSplits = (int) Math.ceil((double) globalContextCodes.size() / 10);
        int j = 0;
        for (int i = 0; i < requiredSplits; i++) {
            // The context codes for a call
            ArrayList<String> contextCodeSingleCall = new ArrayList<>();
            for (; contextCodeSingleCall.size() <= 9 && j < globalContextCodes.size(); j++) {
                contextCodeSingleCall.add(globalContextCodes.get(j));
            }

            if (!calendar.loaded(curMonth.getYear(), curMonth.getMonth(), "event")) {
                getEvents(curMonth.getYear(), curMonth.getMonth(), contextCodeSingleCall);
            }

            if (!calendar.loaded(curMonth.getYear(), curMonth.getMonth(), "assignment")) {
                getAssignments(curMonth.getYear(), curMonth.getMonth(), contextCodeSingleCall);
            }

        }
    }

    /**
     * Sets background drawable for DateTime and clears previous selected date.
     *
     * @param dateTime
     */
    private void setBackground(DateTime dateTime) {
        // TODO Implement for new calendar view
//        Drawable border = ResourcesCompat.getDrawable(getResources(), R.drawable.border, null);
//        if (previousDateTime != null) {
//            // Clear background for previous selected DateTime
//            caldroidFragment.clearBackgroundDrawableForDateTime(previousDateTime);
//        }
//        // Add border drawable to selected date
//        caldroidFragment.setBackgroundDrawableForDateTime(border, dateTime);
//        caldroidFragment.refreshView();
//        previousDateTime = dateTime;
    }

    /**
     * Get all calendar events for a month, and add them to the 'events' field.
     *
     * @param year  The year to get the calendar events.
     * @param month The month to get the calendar events.
     */
    private void getEvents(final int year, final int month, final ArrayList<String> contextCodes) {
        final String type = "event";

        //What to exlude
        ArrayList<String> excludes = new ArrayList<>();
        excludes.add("child_events");

        final DateTime startDate = DateTime.forDateOnly(year, month, 1);
        final DateTime endDate = startDate.getEndOfMonth();
        String startDateString = startDate.toString();
        String endDateString = endDate.format("YYYY-MM-DD");

        int perPage = 50;

        // TODO Fix pagination: several calls are made, and will use each others nextPage field...
        // TODO Should use a arraylist?
        Call<List<CalendarEvent>> call;
        boolean firstPage = nextPage.isEmpty();
        if (firstPage) {
            call = mittUibClient.getEvents(startDateString, endDateString, contextCodes, excludes, type, perPage);
        } else {
            call = mittUibClient.getEventsPaginate(nextPage);
        }

        call.enqueue(new Callback<List<CalendarEvent>>() {

            @Override
            public void onResponse(Call<List<CalendarEvent>> call, retrofit2.Response<List<CalendarEvent>> response) {
                if (response.isSuccessful()) {

                    ArrayList<CalendarEvent> events = new ArrayList<>();
                    events.addAll(response.body());

                    calendar.addEvents(events);

                    // If returned maximum amount of events, get events for next page

                    nextPage = PaginationUtils.getNextPageUrl(response.headers());
                    if (!nextPage.isEmpty()) {
                        getEvents(year, month, contextCodes);
                    }

                    calendar.setLoaded(year, month, type, true);
//                    setBackgrounds(events);

                    // If in current month, set todays agendas
                    DateTime today = DateTime.today(TimeZone.getTimeZone("Europe/Oslo"));
                    if (today.gteq(startDate) && today.lteq(endDate)) {
                        if (callBack != null) {
                            callBack.setAgendas(calendar.getEventsForDate(today));
                        }
                    }

                } else {

                    if (isAdded()) {
                        mCallback.showSnackbar(getString(R.string.error_requesting_calendar), new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                getEvents(year, month, contextCodes);
                            }
                        });
                        calendar.setLoaded(year, month, type, false);
                    }
                }
            }

            @Override
            public void onFailure(Call<List<CalendarEvent>> call, Throwable t) {
                if (isAdded()) {
                    mCallback.showSnackbar(getString(R.string.error_requesting_calendar), new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            getEvents(year, month, contextCodes);
                        }
                    });
                    calendar.setLoaded(year, month, type, false);
                }
            }
        });
    }

    private void getAssignments(final int year, final int month, final ArrayList<String> contextCodes) {

        final String type = "assignment";

        final DateTime startDate = DateTime.forDateOnly(year, month, 1);
        final DateTime endDate = startDate.getEndOfMonth();
        String startDateString = startDate.toString();
        String endDateString = endDate.format("YYYY-MM-DD");

        int perPage = 50;

        Call<List<CalendarEvent>> call;
        boolean firstPage = nextPageAssignment.isEmpty();
        if (firstPage) {
            call = mittUibClient.getEvents(startDateString, endDateString, contextCodes, null, type, perPage);
        } else {
            call = mittUibClient.getEventsPaginate(nextPageAssignment);
        }

        call.enqueue(new Callback<List<CalendarEvent>>() {
            @Override
            public void onResponse(Call<List<CalendarEvent>> call, retrofit2.Response<List<CalendarEvent>> response) {
                if (response.isSuccessful()) {
                    ArrayList<CalendarEvent> events = new ArrayList<>();
                    events.addAll(response.body());
//                    setBackgrounds(events);

                    nextPageAssignment = PaginationUtils.getNextPageUrl(response.headers());
                    if (!nextPage.isEmpty()) {
                        getEvents(year, month, contextCodes);
                    }

                    // TODO Add set loaded
                    calendar.addEvents(events);
                    calendar.setLoaded(year, month, type, true);
                } else {
                    if (isAdded()) {
                        mCallback.showSnackbar(getString(R.string.error_requesting_assignments), new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                mCallback.showSnackbar(getString(R.string.error_requesting_calendar), new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        getAssignments(year, month, contextCodes);
                                    }
                                });
                                calendar.setLoaded(year, month, type, false);
                            }
                        });
                    }
                }

            }

            @Override
            public void onFailure(Call<List<CalendarEvent>> call, Throwable t) {
                if (isAdded()) {
                    mCallback.showSnackbar(getString(R.string.error_requesting_assignments), new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mCallback.showSnackbar(getString(R.string.error_requesting_calendar), new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    getAssignments(year, month, contextCodes);
                                }
                            });
                            calendar.setLoaded(year, month, type, false);
                        }
                    });
                }
            }
        });
    }

    /**
     * Sets backgrounds for dates.
     *
     * @param events events containing an agenda
     */
    private void setBackgrounds(ArrayList<CalendarEvent> events) {
        // TODO Implement for new calendar view
//        Map<String, Object> extraData = caldroidFragment.getExtraData();
//        for (CalendarEvent e : events) {
//            String key = e.getStartDate().getYear() + "-" + e.getStartDate().getMonth() + "-" + e.getStartDate().getDay();
//            extraData.put(key, true);
//        }
//        caldroidFragment.refreshView();
    }
}