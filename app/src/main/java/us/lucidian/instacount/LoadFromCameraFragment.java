package us.lucidian.instacount;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

public class LoadFromCameraFragment extends Fragment implements CvCameraViewListener2 {
    private static final String             TAG                = "InstaCount::LoadFromCameraFragment";
    private              BaseLoaderCallback mLoaderCallback    = new BaseLoaderCallback(getActivity()) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };
    private static final String             ARG_SECTION_NUMBER = "section_number";
    private static final int                VIEW_MODE_RGBA     = 0;
    private static final int                VIEW_MODE_CANNY    = 2;
    private static final int                VIEW_MODE_CIRCLES  = 6;
    private int                  mViewMode;
    private CameraBridgeViewBase mOpenCvCameraView;
    private TextView             tv_circle_count;
    private ImageView            img;

    private Mat cvCameraViewFrameRgb;
    private Mat cvCameraViewFrameGrey;

    public LoadFromCameraFragment() { Log.i(TAG, "Instantiated new LoadFromCameraFragment"); }

    public static LoadFromCameraFragment newInstance(int sectionNumber) {
        LoadFromCameraFragment fragment = new LoadFromCameraFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //if (getArguments() != null) { int mSectionNumber = getArguments().getInt(ARG_SECTION_NUMBER); }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View load_from_camera_view = inflater.inflate(R.layout.fragment_load_from_camera, container, false);
        img = (ImageView) load_from_camera_view.findViewById(R.id.ImageView01);
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        tv_circle_count = (TextView) load_from_camera_view.findViewById(R.id.tv_circle_count);
        tv_circle_count.setText(InstaCountUtils.BuildInfoMessage(0, 0, 0));

        mOpenCvCameraView = (CameraBridgeViewBase) load_from_camera_view.findViewById(R.id.instacount_surface_view);
        mOpenCvCameraView.setCvCameraViewListener(this);

        load_from_camera_view.findViewById(R.id.btn_camera_view).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mOpenCvCameraView != null && img !=null) {
                        mOpenCvCameraView.enableView();
                        mOpenCvCameraView.setVisibility(View.VISIBLE);
                        img.setVisibility(View.GONE);
                    }
                }
         });

        load_from_camera_view.findViewById(R.id.btn_detect_circles).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mViewMode = VIEW_MODE_CIRCLES;
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        InstaCountUtils.mRgba = cvCameraViewFrameRgb;
                        InstaCountUtils.mGray = cvCameraViewFrameGrey;
                        tv_circle_count.setText(InstaCountUtils.DetectCircles(getActivity()));
                        try {
                            Bitmap bmp32 = Bitmap.createBitmap(InstaCountUtils.mRgba.cols(), InstaCountUtils.mRgba.rows(), Bitmap.Config.ARGB_8888);
                            Utils.matToBitmap(InstaCountUtils.mRgba, bmp32);
                            img.setImageBitmap(bmp32);
                            if (mOpenCvCameraView != null) {
                                mOpenCvCameraView.disableView();
                                mOpenCvCameraView.setVisibility(View.GONE);
                            }
                            img.setVisibility(View.VISIBLE);

                        } catch (Exception e) {
                            Log.e(TAG, "load_from_camera_view: Exception = " + e.getMessage());
                        }
                    }
                });
            }
        });
        load_from_camera_view.findViewById(R.id.btn_canny).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mViewMode = VIEW_MODE_CANNY;
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        InstaCountUtils.mRgba = cvCameraViewFrameRgb;
                        InstaCountUtils.mGray = cvCameraViewFrameGrey;
                        Imgproc.Canny(InstaCountUtils.mGray, InstaCountUtils.mIntermediateMat, 80, 100);
                        Imgproc.cvtColor(InstaCountUtils.mIntermediateMat, InstaCountUtils.mRgba, Imgproc.COLOR_GRAY2RGBA, 4);
                        try {
                            Bitmap bmp32 = Bitmap.createBitmap(InstaCountUtils.mRgba.cols(), InstaCountUtils.mRgba.rows(), Bitmap.Config.ARGB_8888);
                            Utils.matToBitmap(InstaCountUtils.mRgba, bmp32);
                            img.setImageBitmap(bmp32);
                            if (mOpenCvCameraView != null) {
                                mOpenCvCameraView.disableView();
                                mOpenCvCameraView.setVisibility(View.GONE);
                            }
                            img.setVisibility(View.VISIBLE);

                        } catch (Exception e) {
                            Log.e(TAG, "load_from_camera_view: Exception = " + e.getMessage());
                        }
                    }
                });
            }
        });
        return load_from_camera_view;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, getActivity(), mLoaderCallback);
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        InstaCountUtils.mRgba = new Mat(height, width, CvType.CV_8UC4);
        InstaCountUtils.mIntermediateMat = new Mat(height, width, CvType.CV_8UC4);
        InstaCountUtils.mGray = new Mat(height, width, CvType.CV_8UC1);
    }

    @Override
    public void onCameraViewStopped() {
        InstaCountUtils.mRgba.release();
        InstaCountUtils.mGray.release();
        InstaCountUtils.mIntermediateMat.release();
    }

    @Override
    public Mat onCameraFrame(final CvCameraViewFrame inputFrame) {
        cvCameraViewFrameRgb = inputFrame.rgba();
        cvCameraViewFrameGrey = inputFrame.gray();
        return cvCameraViewFrameRgb;
    }
}