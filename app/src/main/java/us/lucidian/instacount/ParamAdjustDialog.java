package us.lucidian.instacount;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;

public class ParamAdjustDialog extends DialogFragment {
    private static final String TAG = "InstaCount::ParamAdjustDialog";

    public static MaterialDialog md;
    public        TextView       min_distance_tv;
    public        TextView       min_radius_tv;
    public        TextView       max_radius_tv;
    public        TextView       canny_threshold_tv;
    public        TextView       accumulator_threshold_tv;
    public        SeekBar        min_distance_seek_bar;
    public        SeekBar        min_radius_seek_bar;
    public        SeekBar        max_radius_seek_bar;
    public        SeekBar        canny_threshold_seek_bar;
    public        SeekBar        accumulator_threshold_seek_bar;

    public ParamAdjustDialog() { }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        md = new MaterialDialog.Builder(getActivity()).title("Circle Detection Parameters")
                                                      .autoDismiss(false)
                                                      .customView(R.layout.dialog_param_adjust)
                                                      .negativeText("Cancel")
                                                      .neutralText("Reset to Defaults")
                                                      .positiveText("Ok")
                                                      .callback(new MaterialDialog.FullCallback() {
                                                          @Override
                                                          public void onPositive(MaterialDialog materialDialog) {
                                                              saveSharedPreferences();
                                                              Toast.makeText(getActivity(), "Settings Saved!", Toast.LENGTH_SHORT).show();
                                                              materialDialog.dismiss();
                                                          }

                                                          @Override
                                                          public void onNegative(MaterialDialog materialDialog) {
                                                              materialDialog.dismiss();
                                                          }

                                                          @Override
                                                          public void onNeutral(MaterialDialog materialDialog) { resetSharedPreferences(); }
                                                      })
                                                      .build();

        min_distance_tv = (TextView) md.getCustomView().findViewById(R.id.min_distance_tv);
        min_radius_tv = (TextView) md.getCustomView().findViewById(R.id.min_radius_tv);
        max_radius_tv = (TextView) md.getCustomView().findViewById(R.id.max_radius_tv);
        canny_threshold_tv = (TextView) md.getCustomView().findViewById(R.id.canny_threshold_tv);
        accumulator_threshold_tv = (TextView) md.getCustomView().findViewById(R.id.accumulator_threshold_tv);

        min_distance_seek_bar = (SeekBar) md.getCustomView().findViewById(R.id.min_distance_seek_bar);
        min_radius_seek_bar = (SeekBar) md.getCustomView().findViewById(R.id.min_radius_seek_bar);
        max_radius_seek_bar = (SeekBar) md.getCustomView().findViewById(R.id.max_radius_seek_bar);
        canny_threshold_seek_bar = (SeekBar) md.getCustomView().findViewById(R.id.canny_threshold_seek_bar);
        accumulator_threshold_seek_bar = (SeekBar) md.getCustomView().findViewById(R.id.accumulator_threshold_seek_bar);

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

        loadSharedPreferences();

        return md;
    }

    public void loadSharedPreferences() {
        Log.i(TAG, "loadSharedPreferences Called");

        SharedPreferences pref = getActivity().getPreferences(0);
        if (min_distance_tv != null) min_distance_tv.setText(pref.getString("min_distance", getString(R.string.default_min_distance)));
        if (min_radius_tv != null) min_radius_tv.setText(pref.getString("min_radius", getString(R.string.default_min_radius)));
        if (max_radius_tv != null) max_radius_tv.setText(pref.getString("max_radius", getString(R.string.default_max_radius)));
        if (canny_threshold_tv != null) canny_threshold_tv.setText(pref.getString("canny_threshold", getString(R.string.default_canny_threshold)));
        if (accumulator_threshold_tv != null) accumulator_threshold_tv.setText(pref.getString("accumulator_threshold", getString(R.string.default_accumulator_threshold)));

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

        if (min_distance_tv != null) edt.putString("min_distance", min_distance_tv.getText().toString());
        if (min_radius_tv != null) edt.putString("min_radius", min_radius_tv.getText().toString());
        if (max_radius_tv != null) edt.putString("max_radius", max_radius_tv.getText().toString());
        if (canny_threshold_tv != null) edt.putString("canny_threshold", canny_threshold_tv.getText().toString());
        if (accumulator_threshold_tv != null) edt.putString("accumulator_threshold", accumulator_threshold_tv.getText().toString());

        edt.commit();
    }

    public void resetSharedPreferences() {
        Log.i(TAG, "resetSharedPreferences Called");

        if (min_distance_tv != null) min_distance_tv.setText(getString(R.string.default_min_distance));
        if (min_radius_tv != null) min_radius_tv.setText(getString(R.string.default_min_radius));
        if (max_radius_tv != null) max_radius_tv.setText(getString(R.string.default_max_radius));
        if (canny_threshold_tv != null) canny_threshold_tv.setText(getString(R.string.default_canny_threshold));
        if (accumulator_threshold_tv != null) accumulator_threshold_tv.setText(getString(R.string.default_accumulator_threshold));

        if (min_distance_seek_bar != null) min_distance_seek_bar.setProgress(Integer.parseInt(getString(R.string.default_min_distance)));
        if (min_radius_seek_bar != null) min_radius_seek_bar.setProgress(Integer.parseInt(getString(R.string.default_min_radius)));
        if (max_radius_seek_bar != null) max_radius_seek_bar.setProgress(Integer.parseInt(getString(R.string.default_max_radius)));
        if (canny_threshold_seek_bar != null) canny_threshold_seek_bar.setProgress(Integer.parseInt(getString(R.string.default_canny_threshold)));
        if (accumulator_threshold_seek_bar != null) accumulator_threshold_seek_bar.setProgress(Integer.parseInt(getString(R.string.default_accumulator_threshold)));
    }
}