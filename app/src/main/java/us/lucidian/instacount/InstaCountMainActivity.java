package us.lucidian.instacount;

import android.content.Intent;
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
    private SettingsFragment        settingsFragment        = SettingsFragment.newInstance(1);

//    public static final String CROP_VERSION_SELECTED_KEY = "crop";

    public static final int VERSION_1 = 1;
//    public static final int VERSION_2 = 2;

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
                Intent myIntent = new Intent(InstaCountMainActivity.this, CameraRTDetectFragment.class);
                InstaCountMainActivity.this.startActivity(myIntent);
                break;
            case 3:
                mTitle = getString(R.string.title_section4);
                fragmentManager.beginTransaction().replace(R.id.container, settingsFragment).commit();
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
        return super.onOptionsItemSelected(item);
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