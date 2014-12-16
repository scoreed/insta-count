package us.lucidian.instacount;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.appkilt.client.AppKilt;

public class InstaCountMainActivity extends ActionBarActivity implements NavigationDrawerFragment.NavigationDrawerCallbacks {

    private NavigationDrawerFragment mNavigationDrawerFragment;
    private CharSequence             mTitle;

    private LoadFromGalleryFragment loadFromGalleryFragment = LoadFromGalleryFragment.newInstance(1);
    private LoadFromCameraFragment  loadFromCameraFragment  = LoadFromCameraFragment.newInstance(1);
    private PreferencesFragment     preferencesFragment     = PreferencesFragment.newInstance(1);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_instacount_main);
        mNavigationDrawerFragment = (us.lucidian.instacount.NavigationDrawerFragment) getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();
        DrawerLayout mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mNavigationDrawerFragment.setUp(R.id.navigation_drawer, mDrawerLayout);
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        switch (position) {
            case 0:
                mTitle = getString(R.string.title_section1);
                fragmentManager.beginTransaction().replace(R.id.container, loadFromGalleryFragment).commit();
                break;
            case 1:
                mTitle = getString(R.string.title_section2);
                fragmentManager.beginTransaction().replace(R.id.container, loadFromCameraFragment).commit();
                break;
            case 2:
                mTitle = getString(R.string.title_section3);
                fragmentManager.beginTransaction().replace(R.id.container, preferencesFragment).commit();
                break;
        }
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            getMenuInflater().inflate(R.menu.instacount_main_activity, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_logout) {
            finish();
        }
        else if (id == R.id.action_settings) {
            showParamAdjustDialog();
        }
        return super.onOptionsItemSelected(item);
    }

    public void showParamAdjustDialog() {
        new ParamAdjustDialog().show(getSupportFragmentManager(), "Settings");
    }

    @Override
    protected void onPause() {
        super.onPause();
        AppKilt.onUpdateableActivityPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        AppKilt.onUpdateableActivityResume(this);
    }
}