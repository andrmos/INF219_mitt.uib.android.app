package com.mossige.finseth.follo.inf219_mitt_uib.fragments;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
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
import com.mossige.finseth.follo.inf219_mitt_uib.models.CalendarEvent;
import com.mossige.finseth.follo.inf219_mitt_uib.models.MyCalendar;
import com.mossige.finseth.follo.inf219_mitt_uib.network.JSONParser;
import com.mossige.finseth.follo.inf219_mitt_uib.network.RequestQueueHandler;
import com.mossige.finseth.follo.inf219_mitt_uib.network.UrlEndpoints;
import com.roomorama.caldroid.CaldroidFragment;
import com.roomorama.caldroid.CaldroidListener;

import org.json.JSONArray;
import org.json.JSONException;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import hirondelle.date4j.DateTime;


public class CalendarFragment extends Fragment {

    private static final String TAG = "CalendarFragment";

    private CaldroidFragment caldroidFragment;
    //Have to use java.Utils.Date, since Caldroid uses Map<Date, 'color'> to display background color for dates.
    private Date tmpDate;
    private Map<Date,ColorDrawable> backgrounds;
    private Map<Date, Drawable> dates;
    private MyCalendar calendar;
    private ArrayList<Integer> courseIds;
    private boolean firstRequest;

    OnDateClickListener mCallback;

    public interface OnDateClickListener {
        void setAgendas(ArrayList<CalendarEvent> events);
    }

    public CalendarFragment() {}

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            mCallback = (OnDateClickListener) context;
        } catch (ClassCastException e) {
            Log.i(TAG, "Class cast exception");
        }
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

        firstRequest = true;
        dates = new HashMap<>();
        backgrounds = new HashMap<>();
        calendar = new MyCalendar();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_calendar, container, false);
        getActivity().setTitle(R.string.calendar_title);

        tmpDate = null;

        FragmentTransaction ft = getFragmentManager().beginTransaction();
        caldroidFragment = initCalendarFragment();
        ft.replace(R.id.calendar_container, caldroidFragment);

        AgendaFragment agendaFragment = new AgendaFragment();
        agendaFragment.setArguments(getArguments());
        ft.replace(R.id.agenda_container, agendaFragment);

        ft.commit();
        return rootView;
    }

    /**
     * Initializes a CaldroidFragment and bundles it with arguments defining the calendar.
     *
     * @return the Caldroid Fragment
     */
    private CaldroidFragment initCalendarFragment() {
//        CaldroidFragment caldroidFragment = new CaldroidFragment();
        CaldroidFragment caldroidFragment = new CustomCaldroidFragment();

        // Calendar is used to get current date
        Calendar cal = Calendar.getInstance();
        // Bundle Caldroid arguments to initialize calendar
        Bundle args = new Bundle();
        args.putInt(CaldroidFragment.MONTH, cal.get(Calendar.MONTH) + 1); // January = 0
        args.putInt(CaldroidFragment.YEAR, cal.get(Calendar.YEAR));
        args.putInt(CaldroidFragment.START_DAY_OF_WEEK, CaldroidFragment.MONDAY);
        args.putBoolean(CaldroidFragment.SHOW_NAVIGATION_ARROWS, true);
        args.putBoolean(CaldroidFragment.SQUARE_TEXT_VIEW_CELL, false);
        args.putBoolean(CaldroidFragment.ENABLE_CLICK_ON_DISABLED_DATES, false);
        caldroidFragment.setArguments(args);

        // Set listeners
        caldroidFragment.setCaldroidListener(initCaldroidListener());

        return caldroidFragment;
    }

    private CaldroidListener initCaldroidListener() {

        final CaldroidListener listener = new CaldroidListener() {

            @Override
            public void onChangeMonth(int month, int year) {
                // TODO fetch events for month - 1, month and month + 1
                // needs extra check for month = 0 and month = 12

                // TODO fetch events for shown dates in calendar, instead of month?

                if (!calendar.loaded(year, month - 1)) { // Zero indexed month
                    getCalendarEvents(year, month - 1, 1);
                } else {
                    // Already loaded, do nothing
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

                // Callback to main activity to notify agenda fragment to update its calendar events
                mCallback.setAgendas(calendar.getEventsForDate(dateTime));

                // TODO Refactor?
                //Remove higlighting for selected day
                if(tmpDate != null) {
                    //If last selected day was highlighted by agendas reverse background color
                    if(backgrounds.get(tmpDate) != null){
                        caldroidFragment.setBackgroundDrawableForDate(backgrounds.get(tmpDate),tmpDate);
                    } else {
                        caldroidFragment.clearBackgroundDrawableForDate(tmpDate);
                    }
                }

                //Set color to selected date
                ColorDrawable bg = new ColorDrawable(0xFF0000);
                caldroidFragment.setBackgroundDrawableForDate(bg, date);
                caldroidFragment.refreshView();
                tmpDate = date;
            }
        };
        return listener;
    }

    /**
     * Get all calendar events for a month, and add them to the 'events' field.
     * @param year The year to get the calendar events.
     * @param month The month to get the calendar events. Zero indexed.
     * @param page_num Page number of calendar event request. Declared final since it's accessed from inner class.
     */
    private void getCalendarEvents(final int year, final int month, final int page_num) {
        // Add all course ids for use in context_codes in url
        ArrayList<String> ids = new ArrayList<>();
        for (Integer i : courseIds) {
            ids.add("course_" + i);
        }

        //What to exlude
        ArrayList<String> exclude = new ArrayList<>();
        exclude.add("child_events");
        String type = "event";

        // TODO Change to Date4j?
        //Todays date
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        final Calendar cal = Calendar.getInstance();

        cal.set(Calendar.YEAR, year);

        // Set the date to the 1st
        cal.set(Calendar.DATE, 1);
        cal.set(Calendar.MONTH, month);
        String start_date = df.format(cal.getTime());

        // Set date to last day of month
        cal.set(Calendar.DATE, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        String end_date = df.format(cal.getTime());

        //Per page set to max
        String per_page = "50";

        JsonArrayRequest calendarEventsRequest = new JsonArrayRequest(Request.Method.GET, UrlEndpoints.getCalendarEventsUrl(ids, exclude, type, start_date, end_date,per_page,page_num), (String) null, new Response.Listener<JSONArray>() {

            @Override
            public void onResponse(JSONArray response) {

                try {
                    ArrayList<CalendarEvent> events = JSONParser.parseAllCalendarEvents(response);
                    calendar.addEvents(events);

                    // If returned maximum amount of events, get events for next page
                    if(events.size() == 50) {
                        getCalendarEvents(year, month, page_num + 1);
                    }

                    calendar.setLoaded(year, month, true);

                    ColorDrawable bg = new ColorDrawable(0xFFFF6666);

                    Map<String, Object> extraData = caldroidFragment.getExtraData();

                    //Set background for dates that contains at least one agenda
                    for(CalendarEvent e : events){

                        /*
                        Date conversion explained:
                        In java.Utils.Date, months are zero indexed. So January = 0.
                        In date library Date4j, months are one indexed. So January = 1.
                        In java.Utils.Date, years are stored as year - 1900. So 2016 - 1900 = 116.
                        Caldroid uses java.Utils.Date, while our project uses Date4j.
                        So to convert Date4j to java.Utils.Date, 1900 is subtracted to year, while one is subtracted to month.
                        */
                        DateTime dt = e.getStartDate();
                        Date startDate = new Date(dt.getYear() - 1900, dt.getMonth() - 1, dt.getDay());

                        // TODO put which dates to add a circle
                        // TODO refreshView()
//                        extraData.put(e.getStartDate().getYear() + "-" + e.getStartDate().getMonth() + "-" + e.getStartDate().getDay(), true);

                        dates.put(startDate, bg);
                        backgrounds.put(startDate, bg);
                    }
                    caldroidFragment.setBackgroundDrawableForDates(dates);
                    caldroidFragment.refreshView();

                    if (firstRequest) {
                        DateTime today = DateTime.today(TimeZone.getTimeZone("Europe/Oslo"));
                        mCallback.setAgendas(calendar.getEventsForDate(today));
                        firstRequest = !firstRequest;
                    }

                } catch (JSONException e) {
                    Log.i(TAG, "exception: " + e);
                    calendar.setLoaded(year, month, false);
                }


            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.i(TAG, "onErrorResponse: " + error + " for month " + month + " (zero indexed)");
                calendar.setLoaded(year, month, false);

                // TODO Show error message
            }
        });

        calendarEventsRequest.setRetryPolicy(new DefaultRetryPolicy(5000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        RequestQueueHandler.getInstance(getContext()).addToRequestQueue(calendarEventsRequest);
    }


}