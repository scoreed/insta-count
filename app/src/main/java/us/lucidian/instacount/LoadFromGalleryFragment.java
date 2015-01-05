package us.lucidian.instacount;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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

        InstaCountUtils.mDstWidth = getResources().getDimensionPixelSize(R.dimen.destination_width);
        InstaCountUtils.mDstHeight = getResources().getDimensionPixelSize(R.dimen.destination_height);
        
        InstaCountUtils.LoadSharedPreferences(getActivity());

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

        load_from_gallery_view.findViewById(R.id.btn_canny).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSelectedImage == null) return;
                InstaCountUtils.mRgba = new Mat(mSelectedImage.getHeight(), mSelectedImage.getWidth(), CvType.CV_8UC4);
                InstaCountUtils.mIntermediateMat = new Mat(mSelectedImage.getHeight(), mSelectedImage.getWidth(), CvType.CV_8UC4);
                InstaCountUtils.mGray = new Mat(mSelectedImage.getHeight(), mSelectedImage.getWidth(), CvType.CV_8UC1);
                
                Bitmap bmp32 = mSelectedImage.copy(Bitmap.Config.ARGB_8888, true);
                Utils.bitmapToMat(bmp32, InstaCountUtils.mRgba);
                Imgproc.cvtColor(InstaCountUtils.mRgba, InstaCountUtils.mGray, Imgproc.COLOR_BGR2GRAY, 1);
                
                if (InstaCountUtils.mGray == null) Log.e(TAG, "load_from_gallery_view mGray is null");
                if (InstaCountUtils.mIntermediateMat == null) Log.e(TAG, "load_from_gallery_view mIntermediateMat is null");
                
                try {
                    Imgproc.Canny(InstaCountUtils.mGray, InstaCountUtils.mIntermediateMat, 35, InstaCountUtils.cannyThreshold);
                    Imgproc.cvtColor(InstaCountUtils.mIntermediateMat, InstaCountUtils.mRgba, Imgproc.COLOR_GRAY2RGBA, 4);
                    bmp32 = Bitmap.createBitmap(InstaCountUtils.mRgba.cols(), InstaCountUtils.mRgba.rows(), Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(InstaCountUtils.mRgba, bmp32);
                    img.setImageBitmap(bmp32);
                }
                catch (Exception e) {
                    Log.e(TAG, "load_from_gallery_view canny: Exception = " + e.getMessage());
                }
            }
        });
        
        ((TextView)load_from_gallery_view.findViewById(R.id.tv_circle_count)).setText(InstaCountUtils.SetInfoMessage());

//        left_actions = ((FloatingActionsMenu)load_from_gallery_view.findViewById(R.id.left_actions));
//        right_actions = ((FloatingActionsMenu)load_from_gallery_view.findViewById(R.id.right_actions));
//
//        load_from_gallery_view.findViewById(R.id.left_actions).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Toast.makeText(getActivity(), "Clicked left_actions", Toast.LENGTH_SHORT).show();
//                if (left_actions.isExpanded()) { right_actions.collapse(); }
//            }
//        });
//        load_from_gallery_view.findViewById(R.id.right_actions).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Toast.makeText(getActivity(), "Clicked right_actions", Toast.LENGTH_SHORT).show();
//                if (right_actions.isExpanded()) { left_actions.collapse(); }
//            }
//        });
        
        load_from_gallery_view.findViewById(R.id.btn_blur_up).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (InstaCountUtils.blurSize <= InstaCountUtils.maxBlurSize) {
                    InstaCountUtils.blurSize += 2;
                    runCircleDetect();
                }
            }
        });
        load_from_gallery_view.findViewById(R.id.btn_blur_down).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (InstaCountUtils.blurSize >= 3) {
                    InstaCountUtils.blurSize -= 2;
                    runCircleDetect();
                }
            }
        });
        load_from_gallery_view.findViewById(R.id.btn_canny_up).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (InstaCountUtils.cannyThreshold <= InstaCountUtils.maxCannyThreshold) {
                    InstaCountUtils.cannyThreshold++;
                    runCircleDetect();
                }
            }
        });
        load_from_gallery_view.findViewById(R.id.btn_canny_down).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (InstaCountUtils.cannyThreshold > 1) {
                    InstaCountUtils.cannyThreshold--;
                    runCircleDetect();
                }
            }
        });
        load_from_gallery_view.findViewById(R.id.btn_accum_up).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (InstaCountUtils.accumulatorThreshold <= InstaCountUtils.maxAccumulatorThreshold) {
                    InstaCountUtils.accumulatorThreshold++;
                    runCircleDetect();
                }
            }
        });
        load_from_gallery_view.findViewById(R.id.btn_accum_down).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (InstaCountUtils.accumulatorThreshold > 1) {
                    InstaCountUtils.accumulatorThreshold--;
                    runCircleDetect();
                }
            }
        });
        load_from_gallery_view.findViewById(R.id.btn_min_distance_up).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (InstaCountUtils.minDistance <= InstaCountUtils.maxMinDistance) {
                    InstaCountUtils.minDistance++;
                    runCircleDetect();
                }
            }
        });
        load_from_gallery_view.findViewById(R.id.btn_min_distance_down).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (InstaCountUtils.minDistance > 1) {
                    InstaCountUtils.minDistance--;
                    runCircleDetect();
                }
            }
        });
        load_from_gallery_view.findViewById(R.id.btn_min_radius_up).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (InstaCountUtils.minRadius <= InstaCountUtils.maxMinRadius) {
                    InstaCountUtils.minRadius++;
                    runCircleDetect();
                }
            }
        });
        load_from_gallery_view.findViewById(R.id.btn_min_radius_down).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (InstaCountUtils.minRadius > 1) {
                    InstaCountUtils.minRadius--;
                    runCircleDetect();
                }
            }
        });
        load_from_gallery_view.findViewById(R.id.btn_max_radius_up).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (InstaCountUtils.maxRadius <= InstaCountUtils.maxMaxRadius) {
                    InstaCountUtils.maxRadius++;
                    runCircleDetect();
                }
            }
        });
        load_from_gallery_view.findViewById(R.id.btn_max_radius_down).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (InstaCountUtils.maxRadius > 1) {
                    InstaCountUtils.maxRadius--;
                    runCircleDetect();
                }
            }
        });
        return load_from_gallery_view;
    }

    public void runCircleDetect() {
        if (mSelectedImage == null) {
            ((TextView)load_from_gallery_view.findViewById(R.id.tv_circle_count)).setText(InstaCountUtils.SetInfoMessage());
            return;
        }
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
                    Bitmap unscaledBitmap = ScalingUtilities.decodeFile(mSelectedImagePath, InstaCountUtils.mDstWidth, InstaCountUtils.mDstHeight, ScalingUtilities.ScalingLogic.FIT);
                    if (unscaledBitmap.getHeight() > 1024 || unscaledBitmap.getWidth() > 1024) {
                        mSelectedImage = ScalingUtilities.createScaledBitmap(unscaledBitmap, InstaCountUtils.mDstWidth, InstaCountUtils.mDstHeight, ScalingUtilities.ScalingLogic.FIT);
                        unscaledBitmap.recycle();
                    } else {
                        mSelectedImage = unscaledBitmap;
                    }
                    runCircleDetect();
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
    
    @Override
    public void onStop() { super.onStop(); InstaCountUtils.SaveSharedPreferences(getActivity()); }
    
    public void onDestroy() { super.onDestroy(); InstaCountUtils.SaveSharedPreferences(getActivity()); }
}
