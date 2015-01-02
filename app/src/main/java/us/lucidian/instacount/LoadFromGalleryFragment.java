package us.lucidian.instacount;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import com.kbeanie.imagechooser.api.ChooserType;
import com.kbeanie.imagechooser.api.ChosenImage;
import com.kbeanie.imagechooser.api.ImageChooserListener;
import com.kbeanie.imagechooser.api.ImageChooserManager;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.util.Arrays;

@SuppressWarnings({"UnusedDeclaration", "FieldCanBeLocal"})
public class LoadFromGalleryFragment extends Fragment implements ImageChooserListener {
    private static final String             TAG                   = "InstaCount::LoadFromGalleryFragment";
    private              BaseLoaderCallback mLoaderCallback       = new BaseLoaderCallback(getActivity()) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };
    private static final String             ARG_SECTION_NUMBER    = "section_number";
    private static final int                SELECT_PICTURE        = 1;
    private static final int                SELECT_PICTURE_KITKAT = 2;
    private View                load_from_gallery_view;
    private ImageView           img;
    private TextView            tv_circle_count;
    private Bitmap              mSelectedImage;
    private ImageChooserManager imageChooserManager;
    private String              filePath;

    public LoadFromGalleryFragment() {
        Log.i(TAG, "Instantiated new LoadFromGalleryFragment");
    }

    public static LoadFromGalleryFragment newInstance(int sectionNumber) {
        LoadFromGalleryFragment fragment = new LoadFromGalleryFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) { super.onCreate(savedInstanceState); }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        load_from_gallery_view = inflater.inflate(R.layout.fragment_load_from_gallery, container, false);
        tv_circle_count = (TextView) load_from_gallery_view.findViewById(R.id.tv_circle_count);
        img = (ImageView) load_from_gallery_view.findViewById(R.id.ImageView01);

        load_from_gallery_view.findViewById(R.id.btn_browse_gallery).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { chooseImage(); }
        });

        load_from_gallery_view.findViewById(R.id.btn_detect_circles).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { runCircleDetect(); }
        });

        load_from_gallery_view.findViewById(R.id.btn_reset_image).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSelectedImage == null) return;
                tv_circle_count.setText(InstaCountUtils.SetInfoMessage());
                Bitmap bmp32 = mSelectedImage.copy(Bitmap.Config.ARGB_8888, true);
                img.setImageBitmap(bmp32);
            }
        });

        InstaCountUtils.LoadSharedPreferences(getActivity());
        ((TextView)load_from_gallery_view.findViewById(R.id.tv_circle_count)).setText(InstaCountUtils.SetInfoMessage());
        final String[] stringArray = new String[20];
        int n = 1;
        for (int i = 0; i < 20; i++) {
            stringArray[i] = Integer.toString(n);
            n += 2;
        }
        NumberPicker np_params_blur = (NumberPicker)load_from_gallery_view.findViewById(R.id.params_blur);
        np_params_blur.setMaxValue(stringArray.length - 1);
        np_params_blur.setMinValue(0);
        np_params_blur.setDisplayedValues(stringArray);
        np_params_blur.setWrapSelectorWheel(false);
        int i = Arrays.asList(stringArray).indexOf(Integer.toString(InstaCountUtils.blurSize));
        np_params_blur.setValue(i);
        np_params_blur.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                InstaCountUtils.blurSize = Integer.parseInt(stringArray[newVal]);
                runCircleDetect();
                ((TextView)load_from_gallery_view.findViewById(R.id.tv_circle_count)).setText(InstaCountUtils.SetInfoMessage());
            }
        });

        NumberPicker np_params_canny = (NumberPicker)load_from_gallery_view.findViewById(R.id.params_canny);
        np_params_canny.setMaxValue(200);
        np_params_canny.setMinValue(0);
        np_params_canny.setWrapSelectorWheel(false);
        np_params_canny.setValue(InstaCountUtils.cannyThreshold);
        np_params_canny.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                InstaCountUtils.cannyThreshold = newVal;
                runCircleDetect();
                ((TextView)load_from_gallery_view.findViewById(R.id.tv_circle_count)).setText(InstaCountUtils.SetInfoMessage());
            }
        });

        NumberPicker np_params_accum = (NumberPicker)load_from_gallery_view.findViewById(R.id.params_accum);
        np_params_accum.setMaxValue(200);
        np_params_accum.setMinValue(0);
        np_params_accum.setWrapSelectorWheel(false);
        np_params_accum.setValue(InstaCountUtils.accumulatorThreshold);
        np_params_accum.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                InstaCountUtils.accumulatorThreshold = newVal;
                runCircleDetect();
                ((TextView)load_from_gallery_view.findViewById(R.id.tv_circle_count)).setText(InstaCountUtils.SetInfoMessage());
            }
        });

        NumberPicker np_params_min_distance = (NumberPicker)load_from_gallery_view.findViewById(R.id.params_min_distance);
        np_params_min_distance.setMaxValue(100);
        np_params_min_distance.setMinValue(1);
        np_params_min_distance.setWrapSelectorWheel(false);
        np_params_min_distance.setValue(InstaCountUtils.minDistance);
        np_params_min_distance.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                InstaCountUtils.minDistance = newVal;
                runCircleDetect();
                ((TextView)load_from_gallery_view.findViewById(R.id.tv_circle_count)).setText(InstaCountUtils.SetInfoMessage());
            }
        });

        NumberPicker np_params_min_radius = (NumberPicker)load_from_gallery_view.findViewById(R.id.params_min_radius);
        np_params_min_radius.setMaxValue(100);
        np_params_min_radius.setMinValue(1);
        np_params_min_radius.setWrapSelectorWheel(false);
        np_params_min_radius.setValue(InstaCountUtils.minRadius);
        np_params_min_radius.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                InstaCountUtils.minRadius = newVal;
                runCircleDetect();
                ((TextView)load_from_gallery_view.findViewById(R.id.tv_circle_count)).setText(InstaCountUtils.SetInfoMessage());
            }
        });

        NumberPicker np_params_max_radius = (NumberPicker)load_from_gallery_view.findViewById(R.id.params_max_radius);
        np_params_max_radius.setMaxValue(400);
        np_params_max_radius.setMinValue(1);
        np_params_max_radius.setWrapSelectorWheel(false);
        np_params_max_radius.setValue(InstaCountUtils.maxRadius);
        np_params_max_radius.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                InstaCountUtils.maxRadius = newVal;
                runCircleDetect();
                ((TextView)load_from_gallery_view.findViewById(R.id.tv_circle_count)).setText(InstaCountUtils.SetInfoMessage());
            }
        });
        
        return load_from_gallery_view;
    }

    public void runCircleDetect() {
        if (mSelectedImage == null) return;
        InstaCountUtils.mRgba = new Mat(mSelectedImage.getHeight(), mSelectedImage.getWidth(), CvType.CV_8UC4);
        InstaCountUtils.mGray = new Mat(mSelectedImage.getHeight(), mSelectedImage.getWidth(), CvType.CV_8UC1);
        Bitmap bmp32 = mSelectedImage.copy(Bitmap.Config.ARGB_8888, true);
        Utils.bitmapToMat(bmp32, InstaCountUtils.mRgba);
        Imgproc.cvtColor(InstaCountUtils.mRgba, InstaCountUtils.mGray, Imgproc.COLOR_BGR2GRAY, 1);
        tv_circle_count.setText(InstaCountUtils.DetectCircles(getActivity()));
        bmp32 = Bitmap.createBitmap(InstaCountUtils.resizedRgba.cols(), InstaCountUtils.resizedRgba.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(InstaCountUtils.resizedRgba, bmp32);
        img.setImageBitmap(bmp32);
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult: requestCode = " + requestCode);
        if (resultCode == Activity.RESULT_OK && requestCode == ChooserType.REQUEST_PICK_PICTURE) {
            if (imageChooserManager == null) {
                Log.d(TAG, "onActivityResult: imageChooserManager = null");
                imageChooserManager = new ImageChooserManager(this, ChooserType.REQUEST_PICK_PICTURE, getString(R.string.app_name), true);
                imageChooserManager.setImageChooserListener(this);
            }
            imageChooserManager.submit(requestCode, data);
        }
    }

    private void chooseImage() {
        imageChooserManager = new ImageChooserManager(this, ChooserType.REQUEST_PICK_PICTURE, getString(R.string.app_name), true);
        imageChooserManager.setImageChooserListener(this);
        try {
            filePath = imageChooserManager.choose();
            Log.d(TAG, "chooseImage: filePath = " + filePath);
        }
        catch (Exception e) {
            Log.e(TAG, "chooseImage: Exception = " + e.getMessage());
        }
    }

    @Override
    public void onImageChosen(final ChosenImage chosenImage) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (chosenImage != null) {
                    String mSelectedImagePath = chosenImage.getFilePathOriginal();
                    mSelectedImage = BitmapFactory.decodeFile(mSelectedImagePath);
                    runCircleDetect();
                    
//                    Bitmap bmp32;
//                    String mSelectedImagePath = chosenImage.getFilePathOriginal();
//                    Log.d(TAG, "onImageChosen: mSelectedImagePath = " + mSelectedImagePath);
//                    mSelectedImage = BitmapFactory.decodeFile(mSelectedImagePath);
//                    if (mSelectedImage == null) return;
//                    InstaCountUtils.mRgba = new Mat(mSelectedImage.getHeight(), mSelectedImage.getWidth(), CvType.CV_8UC4);
//                    InstaCountUtils.mGray = new Mat(mSelectedImage.getHeight(), mSelectedImage.getWidth(), CvType.CV_8UC1);
//                    bmp32 = mSelectedImage.copy(Bitmap.Config.ARGB_8888, true);
//                    Utils.bitmapToMat(bmp32, InstaCountUtils.mRgba);
//                    Imgproc.cvtColor(InstaCountUtils.mRgba, InstaCountUtils.mGray, Imgproc.COLOR_BGR2GRAY, 1);
//                    tv_circle_count.setText(InstaCountUtils.DetectCircles(getActivity()));
//                    try {
//                        bmp32 = Bitmap.createBitmap(InstaCountUtils.resizedRgba.cols(), InstaCountUtils.resizedRgba.rows(), Bitmap.Config.ARGB_8888);
//                        Utils.matToBitmap(InstaCountUtils.resizedRgba, bmp32);
//                        img.setImageBitmap(bmp32);
//                    } catch (Exception e) {
//                        Log.e(TAG, "onImageChosen: Exception = " + e.getMessage());
//                    }
                }
            }
        });
    }

    @Override
    public void onError(final String reason) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getActivity(), reason, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void reinitializeImageChooser() {
        Log.d(TAG, "reinitializeImageChooser: filePath = " + filePath);
        imageChooserManager = new ImageChooserManager(this, ChooserType.REQUEST_PICK_PICTURE, getString(R.string.app_name), true);
        imageChooserManager.setImageChooserListener(this);
        imageChooserManager.reinitialize(filePath);
    }

    @Override
    public void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, getActivity(), mLoaderCallback);
    }

    @Override
    public void onPause() { super.onPause(); InstaCountUtils.SaveSharedPreferences(getActivity()); }

    public void onDestroy() { super.onDestroy(); InstaCountUtils.SaveSharedPreferences(getActivity()); }
}
