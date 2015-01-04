package us.lucidian.instacount;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

@SuppressWarnings({"FieldCanBeLocal", "UnusedDeclaration"})
public class SettingsFragment extends Fragment {
    private static final String ARG_SECTION_NUMBER = "section_number";
    private static final String TAG = "InstaCount::SettingsFragment";
    private int      mSectionNumber;

    public View settingsView;

    public  TextView blur_size_tv;
    public  TextView min_distance_tv;
    public  TextView min_radius_tv;
    public  TextView max_radius_tv;
    public  TextView canny_threshold_tv;
    public  TextView accumulator_threshold_tv;
    public  SeekBar  blur_size_seek_bar;
    public  SeekBar  min_distance_seek_bar;
    public  SeekBar  min_radius_seek_bar;
    public  SeekBar  max_radius_seek_bar;
    public  SeekBar  canny_threshold_seek_bar;
    public  SeekBar  accumulator_threshold_seek_bar;

    public SettingsFragment() { }

    public static SettingsFragment newInstance(int sectionNumber) {
        SettingsFragment fragment = new SettingsFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mSectionNumber = getArguments().getInt(ARG_SECTION_NUMBER);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        settingsView = inflater.inflate(R.layout.fragment_settings, container, false);

        blur_size_tv = (TextView) settingsView.findViewById(R.id.blur_size_tv);
        min_distance_tv = (TextView) settingsView.findViewById(R.id.min_distance_tv);
        min_radius_tv = (TextView) settingsView.findViewById(R.id.min_radius_tv);
        max_radius_tv = (TextView) settingsView.findViewById(R.id.max_radius_tv);
        canny_threshold_tv = (TextView) settingsView.findViewById(R.id.canny_threshold_tv);
        accumulator_threshold_tv = (TextView) settingsView.findViewById(R.id.accumulator_threshold_tv);

        blur_size_seek_bar = (SeekBar) settingsView.findViewById(R.id.blur_size_seek_bar);
        min_distance_seek_bar = (SeekBar) settingsView.findViewById(R.id.min_distance_seek_bar);
        min_radius_seek_bar = (SeekBar) settingsView.findViewById(R.id.min_radius_seek_bar);
        max_radius_seek_bar = (SeekBar) settingsView.findViewById(R.id.max_radius_seek_bar);
        canny_threshold_seek_bar = (SeekBar) settingsView.findViewById(R.id.canny_threshold_seek_bar);
        accumulator_threshold_seek_bar = (SeekBar) settingsView.findViewById(R.id.accumulator_threshold_seek_bar);

        blur_size_seek_bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progressValue, boolean fromUser) {
                if (blur_size_tv != null) {
                    int progress = (Math.round(progressValue/2 ))*2;
                    blur_size_tv.setText(Integer.toString(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });
        min_distance_seek_bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progressValue, boolean fromUser) {
                if (min_distance_tv != null) min_distance_tv.setText(Integer.toString(progressValue));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });
        min_radius_seek_bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progressValue, boolean fromUser) {
                if (min_radius_tv != null) min_radius_tv.setText(Integer.toString(progressValue));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });
        max_radius_seek_bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progressValue, boolean fromUser) {
                if (max_radius_tv != null) max_radius_tv.setText(Integer.toString(progressValue));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });
        canny_threshold_seek_bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progressValue, boolean fromUser) {
                if (canny_threshold_tv != null) canny_threshold_tv.setText(Integer.toString(progressValue));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });
        accumulator_threshold_seek_bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progressValue, boolean fromUser) {
                if (accumulator_threshold_tv != null) accumulator_threshold_tv.setText(Integer.toString(progressValue));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        settingsView.findViewById(R.id.btn_settings_save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveSharedPreferences();
                Toast.makeText(getActivity(), "Settings Saved!", Toast.LENGTH_SHORT).show();
                loadSharedPreferences();
            }
        });

        settingsView.findViewById(R.id.btn_settings_reset).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetSharedPreferences();
            }
        });

        loadSharedPreferences();

        return settingsView;
    }

    public void loadSharedPreferences() {
        Log.i(TAG, "loadSharedPreferences Called");

        SharedPreferences pref = getActivity().getPreferences(0);
        if (blur_size_tv != null) blur_size_tv.setText(pref.getString("blur_size", getString(R.string.default_blur_size)));
        if (min_distance_tv != null) min_distance_tv.setText(pref.getString("min_distance", getString(R.string.default_min_distance)));
        if (min_radius_tv != null) min_radius_tv.setText(pref.getString("min_radius", getString(R.string.default_min_radius)));
        if (max_radius_tv != null) max_radius_tv.setText(pref.getString("max_radius", getString(R.string.default_max_radius)));
        if (canny_threshold_tv != null) canny_threshold_tv.setText(pref.getString("canny_threshold", getString(R.string.default_canny_threshold)));
        if (accumulator_threshold_tv != null) accumulator_threshold_tv.setText(pref.getString("accumulator_threshold", getString(R.string.default_accumulator_threshold)));

        if (blur_size_seek_bar != null) blur_size_seek_bar.setProgress(Integer.parseInt(pref.getString("blur_size", getString(R.string.default_blur_size))));
        if (min_distance_seek_bar != null) min_distance_seek_bar.setProgress(Integer.parseInt(pref.getString("min_distance", getString(R.string.default_min_distance))));
        if (min_radius_seek_bar != null) min_radius_seek_bar.setProgress(Integer.parseInt(pref.getString("min_radius", getString(R.string.default_min_radius))));
        if (max_radius_seek_bar != null) max_radius_seek_bar.setProgress(Integer.parseInt(pref.getString("max_radius", getString(R.string.default_max_radius))));
        if (canny_threshold_seek_bar != null) canny_threshold_seek_bar.setProgress(Integer.parseInt(pref.getString("canny_threshold", getString(R.string.default_canny_threshold))));
        if (accumulator_threshold_seek_bar != null) accumulator_threshold_seek_bar.setProgress(Integer.parseInt(pref.getString("accumulator_threshold", getString(R.string.default_accumulator_threshold))));
    }

    public void saveSharedPreferences() {
        Log.i(TAG, "saveSharedPreferences Called");

        SharedPreferences pref = getActivity().getPreferences(0);
        SharedPreferences.Editor edt = pref.edit();

        if (blur_size_tv != null) edt.putString("blur_size", blur_size_tv.getText().toString());
        if (min_distance_tv != null) edt.putString("min_distance", min_distance_tv.getText().toString());
        if (min_radius_tv != null) edt.putString("min_radius", min_radius_tv.getText().toString());
        if (max_radius_tv != null) edt.putString("max_radius", max_radius_tv.getText().toString());
        if (canny_threshold_tv != null) edt.putString("canny_threshold", canny_threshold_tv.getText().toString());
        if (accumulator_threshold_tv != null) edt.putString("accumulator_threshold", accumulator_threshold_tv.getText().toString());

        edt.commit();
    }

    public void resetSharedPreferences() {
        Log.i(TAG, "resetSharedPreferences Called");

        if (blur_size_tv != null) blur_size_tv.setText(getString(R.string.default_blur_size));
        if (min_distance_tv != null) min_distance_tv.setText(getString(R.string.default_min_distance));
        if (min_radius_tv != null) min_radius_tv.setText(getString(R.string.default_min_radius));
        if (max_radius_tv != null) max_radius_tv.setText(getString(R.string.default_max_radius));
        if (canny_threshold_tv != null) canny_threshold_tv.setText(getString(R.string.default_canny_threshold));
        if (accumulator_threshold_tv != null) accumulator_threshold_tv.setText(getString(R.string.default_accumulator_threshold));

        if (blur_size_seek_bar != null) blur_size_seek_bar.setProgress(Integer.parseInt(getString(R.string.default_blur_size)));
        if (min_distance_seek_bar != null) min_distance_seek_bar.setProgress(Integer.parseInt(getString(R.string.default_min_distance)));
        if (min_radius_seek_bar != null) min_radius_seek_bar.setProgress(Integer.parseInt(getString(R.string.default_min_radius)));
        if (max_radius_seek_bar != null) max_radius_seek_bar.setProgress(Integer.parseInt(getString(R.string.default_max_radius)));
        if (canny_threshold_seek_bar != null) canny_threshold_seek_bar.setProgress(Integer.parseInt(getString(R.string.default_canny_threshold)));
        if (accumulator_threshold_seek_bar != null) accumulator_threshold_seek_bar.setProgress(Integer.parseInt(getString(R.string.default_accumulator_threshold)));
    }
}
