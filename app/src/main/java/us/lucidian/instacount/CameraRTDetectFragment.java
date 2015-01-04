package us.lucidian.instacount;

/*
This code to go into CameraBridgeViewBase in 
order to scale the bitmap to the size of the screen
 
The file is at:
 
[your path to the sdk]\OpenCV-2.4.4-android-sdk\sdk\java\src\org\opencv\android

   protected void deliverAndDrawFrame(CvCameraViewFrame frame) {
       Mat modified;

       if (mListener != null) {
           modified = mListener.onCameraFrame(frame);
       } else {
           modified = frame.rgba();
       }

       boolean bmpValid = true;
       if (modified != null) {
           try {
               Utils.matToBitmap(modified, mCacheBitmap);
           } catch(Exception e) {
               Log.e(TAG, "Mat type: " + modified);
               Log.e(TAG, "Bitmap type: " + mCacheBitmap.getWidth() + "*" + mCacheBitmap.getHeight());
               Log.e(TAG, "Utils.matToBitmap() throws an exception: " + e.getMessage());
               bmpValid = false;
           }
       }
       if (bmpValid && mCacheBitmap != null) {
           Canvas canvas = getHolder().lockCanvas();
           if (canvas != null) {
               canvas.drawColor(0, android.graphics.PorterDuff.Mode.CLEAR);
               
               
               /////////////////////////////////////////////////////
               ////// THIS IS THE CHANGED PART /////////////////////
               int width = mCacheBitmap.getWidth();
               int height = mCacheBitmap.getHeight();
               float scaleWidth = ((float) canvas.getWidth()) / width;
               float scaleHeight = ((float) canvas.getHeight()) / height;
               float fScale = Math.min(scaleHeight,  scaleWidth);
               // CREATE A MATRIX FOR THE MANIPULATION
               Matrix matrix = new Matrix();
               // RESIZE THE BITMAP
               matrix.postScale(fScale, fScale);

               /////////////////////////////////////////////////////

               // RECREATE THE NEW BITMAP
               Bitmap resizedBitmap = Bitmap.createBitmap(mCacheBitmap, 0, 0, width, height, matrix, false);
               
               canvas.drawBitmap(resizedBitmap, (canvas.getWidth() - resizedBitmap.getWidth()) / 2, (canvas.getHeight() - resizedBitmap.getHeight()) / 2, null);
               if (mFpsMeter != null) {
                   mFpsMeter.measure();
                   mFpsMeter.draw(canvas, 20, 30);
               }
               getHolder().unlockCanvasAndPost(canvas);
           }
       }
   }
*
*
*/

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.NumberPicker;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

@SuppressWarnings({"FieldCanBeLocal", "UnusedDeclaration"})
public class CameraRTDetectFragment extends Activity implements CvCameraViewListener {
    private static final String             TAG                = "InstaCount::CameraRTDetectFragment";
    private static final String             ARG_SECTION_NUMBER = "section_number";
    public static final int     VIEW_MODE_RGBA         = 0;
    public static final int     VIEW_MODE_HOUGHCIRCLES = 1;
    public static final int     VIEW_MODE_CANNY        = 3;
    public static       int     viewMode               = VIEW_MODE_RGBA;
    private             boolean bShootNow              = false;
    private double dTextScaleFactor;
    private Point  pt, pt1, pt2;
    private int iLineThickness = 3;
    private JavaCameraView mOpenCvCameraView;
    private long lFrameCount = 0, lMilliStart = 0, lMilliNow = 0, lMilliShotTime = 0;
    private Mat mRgba, mGray, mIntermediateMat, lines;
    private Scalar colorRed, colorGreen, colorWhite;
    private Size sSize5, sSize7, sSize9, sMatSize;
    private String string, sShotText;

    public int iCannyLowerThreshold;
    public int iCircleCount = 0;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.fragment_camerartdetect);
        mOpenCvCameraView = (JavaCameraView) findViewById(R.id.java_surface_view);
        mOpenCvCameraView.setCvCameraViewListener(this);
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_4, this, mLoaderCallback);

        findViewById(R.id.btn_rbg).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewMode = VIEW_MODE_RGBA;
                lFrameCount = 0;
                lMilliStart = 0;
            }
        });
        findViewById(R.id.btn_canny).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewMode = VIEW_MODE_CANNY;
                lFrameCount = 0;
                lMilliStart = 0;
            }
        });
        findViewById(R.id.btn_circles).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewMode = VIEW_MODE_HOUGHCIRCLES;
                lFrameCount = 0;
                lMilliStart = 0;
            }
        });

        InstaCountUtils.LoadSharedPreferences(this);

        ((TextView)findViewById(R.id.tv_circle_count)).setText(InstaCountUtils.SetInfoMessage());
        final String[] stringArray = new String[InstaCountUtils.maxBlurSize / 2];
        int n = 1;
        for (int i = 0; i < InstaCountUtils.maxBlurSize / 2; i++) {
            stringArray[i] = Integer.toString(n);
            n += 2;
        }
        NumberPicker np_params_blur = (NumberPicker)findViewById(R.id.params_blur);
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
                ((TextView)findViewById(R.id.tv_circle_count)).setText(InstaCountUtils.SetInfoMessage());
            }
        });

        NumberPicker np_params_canny = (NumberPicker)findViewById(R.id.params_canny);
        np_params_canny.setMaxValue(InstaCountUtils.maxCannyThreshold);
        np_params_canny.setMinValue(0);
        np_params_canny.setWrapSelectorWheel(false);
        np_params_canny.setValue(InstaCountUtils.cannyThreshold);
        np_params_canny.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                InstaCountUtils.cannyThreshold = newVal;
                ((TextView)findViewById(R.id.tv_circle_count)).setText(InstaCountUtils.SetInfoMessage());
            }
        });

        NumberPicker np_params_accum = (NumberPicker)findViewById(R.id.params_accum);
        np_params_accum.setMaxValue(InstaCountUtils.maxAccumulatorThreshold);
        np_params_accum.setMinValue(0);
        np_params_accum.setWrapSelectorWheel(false);
        np_params_accum.setValue(InstaCountUtils.accumulatorThreshold);
        np_params_accum.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                InstaCountUtils.accumulatorThreshold = newVal;
                ((TextView)findViewById(R.id.tv_circle_count)).setText(InstaCountUtils.SetInfoMessage());
            }
        });

        NumberPicker np_params_min_distance = (NumberPicker)findViewById(R.id.params_min_distance);
        np_params_min_distance.setMaxValue(InstaCountUtils.maxMinDistance);
        np_params_min_distance.setMinValue(1);
        np_params_min_distance.setWrapSelectorWheel(false);
        np_params_min_distance.setValue(InstaCountUtils.minDistance);
        np_params_min_distance.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                InstaCountUtils.minDistance = newVal;
                ((TextView)findViewById(R.id.tv_circle_count)).setText(InstaCountUtils.SetInfoMessage());
            }
        });

        NumberPicker np_params_min_radius = (NumberPicker)findViewById(R.id.params_min_radius);
        np_params_min_radius.setMaxValue(InstaCountUtils.maxMinRadius);
        np_params_min_radius.setMinValue(1);
        np_params_min_radius.setWrapSelectorWheel(false);
        np_params_min_radius.setValue(InstaCountUtils.minRadius);
        np_params_min_radius.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                InstaCountUtils.minRadius = newVal;
                ((TextView)findViewById(R.id.tv_circle_count)).setText(InstaCountUtils.SetInfoMessage());
            }
        });

        NumberPicker np_params_max_radius = (NumberPicker)findViewById(R.id.params_max_radius);
        np_params_max_radius.setMaxValue(InstaCountUtils.maxMaxRadius);
        np_params_max_radius.setMinValue(1);
        np_params_max_radius.setWrapSelectorWheel(false);
        np_params_max_radius.setValue(InstaCountUtils.maxRadius);
        np_params_max_radius.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                InstaCountUtils.maxRadius = newVal;
                ((TextView)findViewById(R.id.tv_circle_count)).setText(InstaCountUtils.SetInfoMessage());
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null) mOpenCvCameraView.disableView();
        InstaCountUtils.SaveSharedPreferences(this);
    }

    public void onResume() {
        super.onResume();
        viewMode = VIEW_MODE_RGBA;
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_4, this, mLoaderCallback);
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null) mOpenCvCameraView.disableView();
        InstaCountUtils.SaveSharedPreferences(this);
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        colorRed = new Scalar(255, 0, 0, 255);
        colorGreen = new Scalar(0, 255, 0, 255);
        colorWhite = new Scalar(255, 255, 255);
        lines = new Mat();
        mGray = new Mat();
        mIntermediateMat = new Mat();
        mRgba = new Mat();
        pt = new Point(0, 0);
        pt1 = new Point(0, 0);
        pt2 = new Point(0, 0);
        sMatSize = new Size();
        sSize5 = new Size(5, 5);
        sSize7 = new Size(7, 7);
        sSize9 = new Size(9, 9);
        string = "";
        DisplayMetrics dm = this.getResources().getDisplayMetrics();
        int densityDpi = dm.densityDpi;
        dTextScaleFactor = ((double) densityDpi / 240.0) * 0.9;
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mIntermediateMat = new Mat(height, width, CvType.CV_8UC4);
    }

    @Override
    public void onCameraViewStopped() { releaseMats(); }

    public void releaseMats() {
        mRgba.release();
        mIntermediateMat.release();
        mGray.release();
        lines.release();
    }

    @Override
    public Mat onCameraFrame(Mat inputFrame) {
        // start the timing counter to put the framerate on screen
        // and make sure the start time is up to date, do
        // a reset every 10 seconds
        if (lMilliStart == 0) lMilliStart = System.currentTimeMillis();
        if ((lMilliNow - lMilliStart) > 10000) {
            lMilliStart = System.currentTimeMillis();
            lFrameCount = 0;
        }
        inputFrame.copyTo(mRgba);
        sMatSize.width = mRgba.width();
        sMatSize.height = mRgba.height();
        switch (viewMode) {
            case VIEW_MODE_RGBA:
                ShowTitle("RGB", 1, colorGreen);
                break;
            case VIEW_MODE_CANNY:
                Imgproc.cvtColor(mRgba, mGray, Imgproc.COLOR_RGBA2GRAY);
                Imgproc.GaussianBlur(mGray, mGray, new Size(InstaCountUtils.blurSize, InstaCountUtils.blurSize), 2, 2);
                iCannyLowerThreshold = 35;
                Imgproc.Canny(mGray, mIntermediateMat, iCannyLowerThreshold, InstaCountUtils.cannyThreshold);
                Imgproc.cvtColor(mIntermediateMat, mRgba, Imgproc.COLOR_GRAY2BGRA, 4);
                ShowTitle("Canny Edges", 1, colorGreen);
                break;
            case VIEW_MODE_HOUGHCIRCLES:
                Imgproc.cvtColor(mRgba, mGray, Imgproc.COLOR_RGBA2GRAY);
                Imgproc.GaussianBlur(mGray, mGray, new Size(InstaCountUtils.blurSize, InstaCountUtils.blurSize), 2, 2);
                Imgproc.HoughCircles(mGray, mIntermediateMat, Imgproc.CV_HOUGH_GRADIENT, 2.0, mGray.rows() / 8, InstaCountUtils.cannyThreshold, InstaCountUtils.accumulatorThreshold, InstaCountUtils.minRadius, InstaCountUtils.maxRadius);
                iCircleCount = mIntermediateMat.cols() > 50 ? 50 : mIntermediateMat.cols();
                if (mIntermediateMat.cols() > 0) for (int x = 0; x < Math.min(mIntermediateMat.cols(), 10); x++) {
                    double vCircle[] = mIntermediateMat.get(0, x);
                    if (vCircle == null) break;
                    pt.x = Math.round(vCircle[0]);
                    pt.y = Math.round(vCircle[1]);
                    int radius = (int) Math.round(vCircle[2]);
                    Point center = new Point(vCircle[0], vCircle[1]);
                    Core.circle(mRgba, pt, radius, colorWhite, iLineThickness);
                    DrawCross(mRgba, colorWhite, pt);
                }
                ShowTitle("Hough Circles", 1, colorGreen);
                break;
        }
        // get the time now in every frame
        lMilliNow = System.currentTimeMillis();
        // update the frame counter
        lFrameCount++;
        string = String.format("FPS: %2.1f", (float) (lFrameCount * 1000) / (float) (lMilliNow - lMilliStart));
        ShowTitle(string, 2, colorGreen);
        if (bShootNow) {
            // get the time of the attempt to save a screenshot
            lMilliShotTime = System.currentTimeMillis();
            bShootNow = false;
            // try it, and set the screen text accordingly.
            // this text is shown at the end of each frame until 
            // 1.5 seconds has elapsed
            if (SaveImage(mRgba)) {
                sShotText = "SCREENSHOT SAVED";
            }
            else {
                sShotText = "SCREENSHOT FAILED";
            }
        }
        if (System.currentTimeMillis() - lMilliShotTime < 1500) ShowTitle(sShotText, 3, colorRed);
        return mRgba;
    }

    public boolean onTouchEvent(final MotionEvent event) {
        bShootNow = true;
        return false;
    }

    public void DrawCross(Mat mat, Scalar color, Point pt) {
        int iCentreCrossWidth = 24;
        pt1.x = pt.x - (iCentreCrossWidth >> 1);
        pt1.y = pt.y;
        pt2.x = pt.x + (iCentreCrossWidth >> 1);
        pt2.y = pt.y;
        Core.line(mat, pt1, pt2, color, iLineThickness - 1);
        pt1.x = pt.x;
        pt1.y = pt.y + (iCentreCrossWidth >> 1);
        pt2.x = pt.x;
        pt2.y = pt.y - (iCentreCrossWidth >> 1);
        Core.line(mat, pt1, pt2, color, iLineThickness - 1);
    }

    @SuppressLint("SimpleDateFormat")
    public boolean SaveImage(Mat mat) {
        Imgproc.cvtColor(mat, mIntermediateMat, Imgproc.COLOR_RGBA2BGR, 3);
        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        String filename = "OpenCV_";
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        Date date = new Date(System.currentTimeMillis());
        String dateString = fmt.format(date);
        int iFileOrdinal = 0;
        filename += dateString + "-" + iFileOrdinal;
        filename += ".png";
        File file = new File(path, filename);
        Boolean bool;
        filename = file.toString();
        bool = Highgui.imwrite(filename, mIntermediateMat);
        return bool;
    }

    private void ShowTitle(String s, int iLineNum, Scalar color) {
        Core.putText(mRgba, s, new Point(10, (int) (dTextScaleFactor * 60 * iLineNum)), Core.FONT_HERSHEY_SIMPLEX, dTextScaleFactor, color, 2);
    }
}
