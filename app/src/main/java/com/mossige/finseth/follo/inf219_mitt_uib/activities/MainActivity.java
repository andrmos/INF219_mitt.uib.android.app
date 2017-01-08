package com.mossige.finseth.follo.inf219_mitt_uib.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.design.widget.NavigationView;
import android.support.design.widget.NavigationView.OnNavigationItemSelectedListener;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.mossige.finseth.follo.inf219_mitt_uib.R;
import com.mossige.finseth.follo.inf219_mitt_uib.fragments.AboutFragment;
import com.mossige.finseth.follo.inf219_mitt_uib.fragments.CalendarFragment;
import com.mossige.finseth.follo.inf219_mitt_uib.fragments.SettingFragment;
import com.mossige.finseth.follo.inf219_mitt_uib.fragments.ConversationFragment;
import com.mossige.finseth.follo.inf219_mitt_uib.listeners.MainActivityListener;
import com.mossige.finseth.follo.inf219_mitt_uib.models.Course;
import com.mossige.finseth.follo.inf219_mitt_uib.models.User;
import com.mossige.finseth.follo.inf219_mitt_uib.network.retrofit.MittUibClient;

import com.mossige.finseth.follo.inf219_mitt_uib.fragments.CourseListFragment;
import com.mossige.finseth.follo.inf219_mitt_uib.network.retrofit.ServiceGenerator;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;

public class MainActivity extends AppCompatActivity implements OnNavigationItemSelectedListener, MainActivityListener {

    private static final String TAG = "MainActivity";

    private User profile;
    private ArrayList<Course> courses;
    private NavigationView navigationView;
    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

        courses = new ArrayList<>();

        requestProfile();
        requestUnreadCount();
        requestCourses(); // Need events in calendar

        initFragment(new CourseListFragment(), getSupportFragmentManager().beginTransaction());

        // Setup toolbar and navigation drawer
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        initNavigationDrawer(toolbar);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        // Setup fragment transaction for replacing fragment
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        // Reset back stack when navigating to a new fragment from the nav bar
        getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

        boolean fragmentTransaction = false;

        switch(id){

            case R.id.nav_course:
                initFragment(new CourseListFragment(), transaction);
                fragmentTransaction = true;
                break;

            case R.id.nav_calendar:
                initCalendarFragment();
                fragmentTransaction = true;
                break;

            case R.id.nav_about:
                initFragment(new AboutFragment(),transaction);
                fragmentTransaction = true;
                break;

            case R.id.nav_settings:
                initFragment(new SettingFragment(),transaction);
                fragmentTransaction = true;
                break;

            case R.id.nav_inbox:
                transaction.addToBackStack("inbox");
                initFragment(new ConversationFragment(), transaction);
                fragmentTransaction = true;
                break;

        }

        if(fragmentTransaction) {
            drawerLayout.closeDrawer(navigationView);
        }

        return fragmentTransaction;
    }

    private void initNavigationDrawer(Toolbar toolbar) {
        // Setup navigation drawer
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle drawerToggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.setDrawerListener(drawerToggle);
        drawerToggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }

    public void initCalendarFragment() {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        CalendarFragment calendarFragment = new CalendarFragment();

        // Bundle course ids
        Bundle bundle = new Bundle();
        ArrayList<Integer> ids = new ArrayList<>();
        for (Course c : courses) {
            ids.add(c.getId());
        }
        bundle.putInt("user_id", profile.getId());
        bundle.putIntegerArrayList("ids", ids);
        calendarFragment.setArguments(bundle);

        transaction.replace(R.id.content_frame, calendarFragment);
        transaction.commit();
    }

    // TODO Move method to Calendar Fragment
    private void requestCourses() {

        MittUibClient client = ServiceGenerator.createService(MittUibClient.class, getApplicationContext());
        Call<List<Course>> call = client.getCourses();
        call.enqueue(new Callback<List<Course>>() {
            @Override
            public void onResponse(Call<List<Course>> call, retrofit2.Response<List<Course>> response) {
                if (response.isSuccessful()) {

                    courses.clear();
                    courses.addAll(response.body());

                } else {
                    showSnackbar();
                }
            }

            @Override
            public void onFailure(Call<List<Course>> call, Throwable t) {
                showSnackbar();
            }
        });
    }

    private void showSnackbar() {
        showSnackbar(getString(R.string.request_courses_error), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestCourses();
            }
        });
    }

    private void requestProfile() {
        MittUibClient client = ServiceGenerator.createService(MittUibClient.class, getApplicationContext());
        Call<User> call = client.getProfile();
        call.enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, retrofit2.Response<User> response) {
                if (response.isSuccessful()) {

                    profile = response.body();

                    //Set name on navigation header
                    TextView nameTV = (TextView) findViewById(R.id.name);
                    nameTV.setText(profile.getName());

                    //Set email on navigation header
                    TextView emailTV = (TextView) findViewById(R.id.primary_email);
                    emailTV.setText(profile.getPrimary_email());
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                showSnackbar(getString(R.string.error_profile), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        requestProfile();
                    }
                });
            }
        });
        
    }

    public void setMenuCounter(@IdRes int itemId, int count) {
        TextView view = (TextView) navigationView.getMenu().findItem(itemId).getActionView();
        view.setText(count > 0 ? String.valueOf(count) : null);
    }

    @Override
    public void showSnackbar(String toastMessage, View.OnClickListener listener) {
        Snackbar snackbar =  Snackbar.make(findViewById(R.id.content_frame), toastMessage, Snackbar.LENGTH_INDEFINITE);
        int duration = 4000;
        snackbar = snackbar.setDuration(duration); // Gives false syntax error...

        if(listener != null) {
            snackbar.setActionTextColor(getResources().getColor(R.color.colorAccent));
            snackbar.setAction(getString(R.string.snackback_action_text), listener);
        }

        if (!snackbar.isShown()) {
            snackbar.show();
        }

    }

    @Override
    public void initCalendar() {
        initCalendarFragment();
    }

    @Override
    public void requestUnreadCount() {
        // TODO Implement
    }

    private void initFragment(Fragment fragment, FragmentTransaction transaction){
        transaction.replace(R.id.content_frame, fragment);
        transaction.commit();
    }
}
