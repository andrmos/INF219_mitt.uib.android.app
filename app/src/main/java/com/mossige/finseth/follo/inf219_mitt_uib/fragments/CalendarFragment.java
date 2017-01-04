package com.mossige.finseth.follo.inf219_mitt_uib.fragments;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.mossige.finseth.follo.inf219_mitt_uib.R;
import com.mossige.finseth.follo.inf219_mitt_uib.listeners.MainActivityListener;
import com.mossige.finseth.follo.inf219_mitt_uib.models.CalendarEvent;
import com.mossige.finseth.follo.inf219_mitt_uib.models.MyCalendar;
import com.mossige.finseth.follo.inf219_mitt_uib.network.HeaderLinksHelper;
import com.mossige.finseth.follo.inf219_mitt_uib.network.JSONParser;
import com.mossige.finseth.follo.inf219_mitt_uib.network.RequestQueueHandler;
import com.mossige.finseth.follo.inf219_mitt_uib.network.UrlEndpoints;
import com.mossige.finseth.follo.inf219_mitt_uib.network.retrofit.MittUibClient;
import com.mossige.finseth.follo.inf219_mitt_uib.network.retrofit.ServiceGenerator;
import com.roomorama.caldroid.CaldroidFragment;
import com.roomorama.caldroid.CaldroidListener;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import hirondelle.date4j.DateTime;
import retrofit2.Call;
import retrofit2.Callback;


public class CalendarFragment extends Fragment {

    private static final String TAG = "CalendarFragment";

    private CaldroidFragment caldroidFragment;
    private MyCalendar calendar;
    private ArrayList<Integer> courseIds;
    private DateTime previousDateTime;
    private OnDateClickListener callBack;
    private MainActivityListener mCallback;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try{
            mCallback = (MainActivityListener) context;
        }catch(ClassCastException e){
            //Do nothing
        }
    }

    public interface OnDateClickListener {
        void setAgendas(ArrayList<CalendarEvent> events);
    }

    public CalendarFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle arguments = getArguments();
        if (arguments != null) {
            courseIds = arguments.getIntegerArrayList("ids");
        } else {
            courseIds = new ArrayList<>(); // Empty ids
        }

        calendar = new MyCalendar();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_calendar, container, false);
        getActivity().setTitle(R.string.calendar_title);

        FragmentTransaction ft = getChildFragmentManager().beginTransaction();

        AgendaFragment agendaFragment = new AgendaFragment();
        // Init callback to allow communication with AgendaFragment
        callBack = agendaFragment;
        agendaFragment.setArguments(getArguments());
        ft.replace(R.id.agenda_container, agendaFragment);

        caldroidFragment = initCalendarFragment();
        ft.replace(R.id.calendar_container, caldroidFragment);

        ft.commit();
        return rootView;
    }

    /**
     * Initializes a CaldroidFragment and bundles it with arguments defining the calendar.
     *
     * @return the Caldroid Fragment
     */
    private CaldroidFragment initCalendarFragment() {
        CaldroidFragment caldroidFragment = new CustomCaldroidFragment();
        DateTime today = DateTime.today(TimeZone.getTimeZone("Europe/Oslo"));

        // Bundle Caldroid arguments to initialize calendar
        Bundle args = new Bundle();
        args.putInt(CaldroidFragment.MONTH, today.getMonth());
        args.putInt(CaldroidFragment.YEAR, today.getYear());
        args.putInt(CaldroidFragment.START_DAY_OF_WEEK, CaldroidFragment.MONDAY);
        caldroidFragment.setArguments(args);

        // Set listeners
        caldroidFragment.setCaldroidListener(initCaldroidListener());

        return caldroidFragment;
    }

    private CaldroidListener initCaldroidListener() {

        final CaldroidListener listener = new CaldroidListener() {

            @Override
            public void onChangeMonth(int month, int year) {
                DateTime curMonth = new DateTime(year, month, 1, 0, 0, 0, 0); // Only need year and month
                DateTime prevMonth = curMonth.minus(0, 1, 0, 0, 0, 0, 0, DateTime.DayOverflow.FirstDay);
                DateTime nextMonth = curMonth.plus(0, 1, 0, 0, 0, 0, 0, DateTime.DayOverflow.FirstDay);

                if (!calendar.loaded(curMonth.getYear(), curMonth.getMonth() - 1)) { // Zero indexed month
                    getCalendarEvents(curMonth.getYear(), curMonth.getMonth() - 1, 1);

                }

                if (!calendar.loaded(nextMonth.getYear(), nextMonth.getMonth() - 1)) { // Zero indexed month
                    getCalendarEvents(nextMonth.getYear(), nextMonth.getMonth() - 1, 1);
                }

                if (!calendar.loaded(prevMonth.getYear(), prevMonth.getMonth() - 1)) { // Zero indexed month
                    getCalendarEvents(prevMonth.getYear(), prevMonth.getMonth() - 1, 1);
                }
            }

            @Override
            public void onSelectDate(Date date, View view) {
                /*
                Date conversion explained:
                In java.Utils.Date, months are zero indexed. So January = 0.
                In date library Date4j, months are one indexed. So January = 1.
                In java.Utils.Date, years are stored as year - 1900. So 2016 - 1900 = 116.
                Caldroid uses java.Utils.Date, while our project uses Date4j.
                So to convert java.Utils.Date to Date4j, 1900 is added to year, while one is added to month.

                Hours, minutes, seconds and nano seconds are ignored, since they are irrelevant when clicking on a date.
                 */
                DateTime dateTime = new DateTime(date.getYear() + 1900, date.getMonth() + 1, date.getDate(), 0, 0, 0, 0);

                // Callback to agenda fragment to update its calendar events
                callBack.setAgendas(calendar.getEventsForDate(dateTime));

                setBackground(dateTime);
            }
        };
        return listener;
    }

    /**
     * Sets background drawable for DateTime and clears previous selected date.
     *
     * @param dateTime
     */
    private void setBackground(DateTime dateTime) {
        Drawable border = getResources().getDrawable(R.drawable.border);
        if (previousDateTime != null) {
            // Clear background for previous selected DateTime
            caldroidFragment.clearBackgroundDrawableForDateTime(previousDateTime);
        }
        // Add border drawable to selected date
        caldroidFragment.setBackgroundDrawableForDateTime(border, dateTime);
        caldroidFragment.refreshView();
        previousDateTime = dateTime;
    }

    /**
     * Get all calendar events for a month, and add them to the 'events' field.
     *
     * @param year     The year to get the calendar events.
     * @param month    The month to get the calendar events. Zero indexed.
     * @param pageNum Page number of calendar event request. Declared final since it's accessed from inner class.
     */
    private void getCalendarEvents(final int year, final int month, final int pageNum) {
        MittUibClient client = ServiceGenerator.createService(MittUibClient.class, getContext());

        ArrayList<String> contextCodes = contextCodes();

        //What to exlude
        ArrayList<String> excludes = new ArrayList<>();
        excludes.add("child_events");
        String type = "event";

        // TODO fix zero indexed months
        final DateTime startDate = DateTime.forDateOnly(year, month + 1, 1);
        final DateTime endDate = startDate.getEndOfMonth();
        String startDateString = startDate.toString();
        String endDateString = endDate.format("YYYY-MM-DD");

        Call<List<CalendarEvent>> call = client.getEvents(startDateString, endDateString, contextCodes, excludes, type, pageNum);

        call.enqueue(new Callback<List<CalendarEvent>>() {
            @Override
            public void onResponse(Call<List<CalendarEvent>> call, retrofit2.Response<List<CalendarEvent>> response) {
                if (response.isSuccessful()) {

                    ArrayList<CalendarEvent> events = new ArrayList<>();
                    events.addAll(response.body());

                    calendar.addEvents(events);

                    // If returned maximum amount of events, get events for next page

                    String nextPage = HeaderLinksHelper.getNextPageUrl(response.headers().get("Link"));
                    if (!nextPage.isEmpty()) {
                        // TODO change to getEventsPagination(nextPage)
                        getCalendarEvents(year, month, pageNum + 1);
                    }

                    requestAssignments(year, month);
                    calendar.setLoaded(year, month, true);
                    setBackgrounds(events);

                    // If in current month, set todays agendas
                    DateTime today = DateTime.today(TimeZone.getTimeZone("Europe/Oslo"));
                    if (today.gteq(startDate) && today.lteq(endDate)) {
                        if (callBack != null) {
                            callBack.setAgendas(calendar.getEventsForDate(today));
                        }
                    }

                    //TODO requestAssignments()

                } else {
                    mCallback.showSnackbar(getString(R.string.error_requesting_calendar), new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            getCalendarEvents(year,month,pageNum);
                        }
                    });
                    calendar.setLoaded(year, month, false);
                }
            }

            @Override
            public void onFailure(Call<List<CalendarEvent>> call, Throwable t) {
                mCallback.showSnackbar(getString(R.string.error_requesting_calendar), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        getCalendarEvents(year,month,pageNum);
                    }
                });
                calendar.setLoaded(year, month, false);
            }
        });
    }

    private ArrayList<String> contextCodes() {
        //        Add all course ids for use in context_codes in url
        ArrayList<String> contextCodes = new ArrayList<>();
        for (Integer i : courseIds) {
            // TODO Max size of context codes is 10, rest is ignored...
            contextCodes.add("course_" + i);
        }

        if(getArguments() != null){
            if(getArguments().containsKey("user_id")){
                contextCodes.add("user_" + getArguments().getInt("user_id"));
            }
        }
        return contextCodes;
    }

    private void requestAssignments(final int year, final int month) {
        //Course ids for context_codes in url
        ArrayList<String> ids = new ArrayList<>();
        for (Integer i : courseIds) {
            ids.add("course_" + i);
        }

        //What to exclude
        ArrayList<String> exclude = new ArrayList<>();

        //Type - event/assignment
        String type = "assignment";

        DateTime startDate = DateTime.forDateOnly(year, month + 1, 1);
        DateTime endDate = startDate.getEndOfMonth();

        String startDateString = startDate.toString();
        String endDateString = endDate.format("YYYY-MM-DD");

        //Per page set to max
        String per_page = "50";

        JsonArrayRequest calendarEventsRequest = new JsonArrayRequest(Request.Method.GET, UrlEndpoints.getCalendarEventsUrl(ids, exclude, type, startDateString, endDateString, per_page, 1,getContext()), (String) null, new Response.Listener<JSONArray>() {

            @Override
            public void onResponse(JSONArray response) {

                ArrayList<CalendarEvent> events = JSONParser.parseAllAssignments(response);
                calendar.addEvents(events);

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                mCallback.showSnackbar(getString(R.string.error_requesting_assignments), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        requestAssignments(year,month);
                    }
                });
            }
        });

        RequestQueueHandler.getInstance(getContext()).addToRequestQueue(calendarEventsRequest);
    }

    /**
     * Sets backgrounds for dates.
     *
     * @param events events containing an agenda
     */
    private void setBackgrounds(ArrayList<CalendarEvent> events) {
        Map<String, Object> extraData = caldroidFragment.getExtraData();
        for (CalendarEvent e : events) {
            String key = e.getStartDate().getYear() + "-" + e.getStartDate().getMonth() + "-" + e.getStartDate().getDay();
            extraData.put(key, true);
        }
        caldroidFragment.refreshView();
    }
}