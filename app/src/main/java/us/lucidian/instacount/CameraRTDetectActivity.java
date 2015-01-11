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
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.getbase.floatingactionbutton.FloatingActionsMenu;

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
import java.util.Date;

@SuppressWarnings({"FieldCanBeLocal", "UnusedDeclaration"})
public class CameraRTDetectActivity extends Activity implements CvCameraViewListener {
    public static final  int    VIEW_MODE_RGBA         = 0;
    public static        int    viewMode               = VIEW_MODE_RGBA;
    public static final  int    VIEW_MODE_HOUGHCIRCLES = 1;
    public static final  int    VIEW_MODE_CANNY        = 3;
    private static final String TAG                    = "InstaCount::CameraRTDetectActivity";
    private static final String ARG_SECTION_NUMBER     = "section_number";
    public int iCannyLowerThreshold;
    public  int     iCircleCount = 0;
    private boolean bShootNow    = false;
    private double dTextScaleFactor;
    private Point  pt, pt1, pt2;
    private int iLineThickness = 3;
    private JavaCameraView mOpenCvCameraView;
    private long lFrameCount = 0, lMilliStart = 0, lMilliNow = 0, lMilliShotTime = 0;
    private Scalar colorRed, colorGreen, colorWhite;
    private String title, sShotText;
    private TextView            info;
    private FloatingActionsMenu right_actions, left_actions;
    
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
        setContentView(R.layout.activity_camerartdetect);
        mOpenCvCameraView = (JavaCameraView) findViewById(R.id.java_surface_view);
        mOpenCvCameraView.setCvCameraViewListener(this);
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_4, this, mLoaderCallback);

        info = (TextView) findViewById(R.id.tv_info);

        findViewById(R.id.btn_reset).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewMode = VIEW_MODE_RGBA;
                lFrameCount = 0;
                lMilliStart = 0;
            }
        });
//        findViewById(R.id.btn_canny).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                viewMode = VIEW_MODE_CANNY;
//                lFrameCount = 0;
//                lMilliStart = 0;
//            }
//        });
        findViewById(R.id.btn_circles).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewMode = VIEW_MODE_HOUGHCIRCLES;
                lFrameCount = 0;
                lMilliStart = 0;
            }
        });
        findViewById(R.id.btn_save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bShootNow = true;
            }
        });

        InstaCountUtils.LoadSharedPreferences(CameraRTDetectActivity.this);

        info.setText(InstaCountUtils.SetInfoMessage());
        left_actions = ((FloatingActionsMenu)findViewById(R.id.left_actions));
        right_actions = ((FloatingActionsMenu)findViewById(R.id.right_actions));

        findViewById(R.id.btn_blur_up).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (InstaCountUtils.blurSize <= InstaCountUtils.maxBlurSize) {
                    InstaCountUtils.blurSize += 2;
                    if (viewMode != VIEW_MODE_HOUGHCIRCLES) info.setText(InstaCountUtils.SetInfoMessage());
                }
            }
        });
        findViewById(R.id.btn_blur_down).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (InstaCountUtils.blurSize >= 3) {
                    InstaCountUtils.blurSize -= 2;
                    if (viewMode != VIEW_MODE_HOUGHCIRCLES) info.setText(InstaCountUtils.SetInfoMessage());
                }
            }
        });
        findViewById(R.id.btn_canny_up).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (InstaCountUtils.cannyThreshold <= InstaCountUtils.maxCannyThreshold) {
                    InstaCountUtils.cannyThreshold++;
                    if (viewMode != VIEW_MODE_HOUGHCIRCLES) info.setText(InstaCountUtils.SetInfoMessage());
                }
            }
        });
        findViewById(R.id.btn_canny_down).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (InstaCountUtils.cannyThreshold > 1) {
                    InstaCountUtils.cannyThreshold--;
                    if (viewMode != VIEW_MODE_HOUGHCIRCLES) info.setText(InstaCountUtils.SetInfoMessage());
                }
            }
        });
        findViewById(R.id.btn_accum_up).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (InstaCountUtils.accumulatorThreshold <= InstaCountUtils.maxAccumulatorThreshold) {
                    InstaCountUtils.accumulatorThreshold++;
                    if (viewMode != VIEW_MODE_HOUGHCIRCLES) info.setText(InstaCountUtils.SetInfoMessage());
                }
            }
        });
        findViewById(R.id.btn_accum_down).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (InstaCountUtils.accumulatorThreshold > 1) {
                    InstaCountUtils.accumulatorThreshold--;
                    if (viewMode != VIEW_MODE_HOUGHCIRCLES) info.setText(InstaCountUtils.SetInfoMessage());
                }
            }
        });
        findViewById(R.id.btn_min_distance_up).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (InstaCountUtils.minDistance <= InstaCountUtils.maxMinDistance) {
                    InstaCountUtils.minDistance++;
                    if (viewMode != VIEW_MODE_HOUGHCIRCLES) info.setText(InstaCountUtils.SetInfoMessage());
                }
            }
        });
        findViewById(R.id.btn_min_distance_down).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (InstaCountUtils.minDistance > 1) {
                    InstaCountUtils.minDistance--;
                    if (viewMode != VIEW_MODE_HOUGHCIRCLES) info.setText(InstaCountUtils.SetInfoMessage());
                }
            }
        });
        findViewById(R.id.btn_min_radius_up).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (InstaCountUtils.minRadius <= InstaCountUtils.maxMinRadius) {
                    InstaCountUtils.minRadius++;
                    if (viewMode != VIEW_MODE_HOUGHCIRCLES) info.setText(InstaCountUtils.SetInfoMessage());
                }
            }
        });
        findViewById(R.id.btn_min_radius_down).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (InstaCountUtils.minRadius > 1) {
                    InstaCountUtils.minRadius--;
                    if (viewMode != VIEW_MODE_HOUGHCIRCLES) info.setText(InstaCountUtils.SetInfoMessage());
                }
            }
        });
        findViewById(R.id.btn_max_radius_up).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (InstaCountUtils.maxRadius <= InstaCountUtils.maxMaxRadius) {
                    InstaCountUtils.maxRadius++;
                    if (viewMode != VIEW_MODE_HOUGHCIRCLES) info.setText(InstaCountUtils.SetInfoMessage());
                }
            }
        });
        findViewById(R.id.btn_max_radius_down).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (InstaCountUtils.maxRadius > 1) {
                    InstaCountUtils.maxRadius--;
                    if (viewMode != VIEW_MODE_HOUGHCIRCLES) info.setText(InstaCountUtils.SetInfoMessage());
                }
            }
        });
    }

    public void onResume() {
        super.onResume();
        viewMode = VIEW_MODE_RGBA;
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_4, this, mLoaderCallback);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null) mOpenCvCameraView.disableView();
        InstaCountUtils.SaveSharedPreferences(CameraRTDetectActivity.this);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mOpenCvCameraView != null) mOpenCvCameraView.disableView();
        InstaCountUtils.SaveSharedPreferences(CameraRTDetectActivity.this);
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null) mOpenCvCameraView.disableView();
        InstaCountUtils.SaveSharedPreferences(CameraRTDetectActivity.this);
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        colorRed = new Scalar(255, 0, 0, 255);
        colorGreen = new Scalar(0, 255, 0, 255);
        colorWhite = new Scalar(255, 255, 255, 255);
        
        pt = new Point(0, 0);
        pt1 = new Point(0, 0);
        pt2 = new Point(0, 0);
        title = "";
        
        DisplayMetrics dm = this.getResources().getDisplayMetrics();
        int densityDpi = dm.densityDpi;
        dTextScaleFactor = ((double) densityDpi / 240.0) * 0.9;

        InstaCountUtils.mGray = new Mat();
        InstaCountUtils.mRgba = new Mat(height, width, CvType.CV_8UC4);
        InstaCountUtils.mIntermediateMat = new Mat(height, width, CvType.CV_8UC4);
    }

    @Override
    public void onCameraViewStopped() { releaseMats(); }

    public void releaseMats() {
        InstaCountUtils.mRgba.release();
        InstaCountUtils.mIntermediateMat.release();
        InstaCountUtils.mGray.release();
    }

    @Override
    public Mat onCameraFrame(Mat inputFrame) {
        if (lMilliStart == 0) lMilliStart = System.currentTimeMillis();
        if ((lMilliNow - lMilliStart) > 10000) {
            lMilliStart = System.currentTimeMillis();
            lFrameCount = 0;
        }
        inputFrame.copyTo(InstaCountUtils.mRgba);
        switch (viewMode) {
            case VIEW_MODE_RGBA:
                ShowTitle("RGB", 1, colorGreen);
                break;
            case VIEW_MODE_CANNY:
                Imgproc.cvtColor(InstaCountUtils.mRgba, InstaCountUtils.mGray, Imgproc.COLOR_RGBA2GRAY);
                Imgproc.GaussianBlur(InstaCountUtils.mGray, InstaCountUtils.mGray, new Size(InstaCountUtils.blurSize, InstaCountUtils.blurSize), 2, 2);
                iCannyLowerThreshold = 35;
                Imgproc.Canny(InstaCountUtils.mGray, InstaCountUtils.mIntermediateMat, iCannyLowerThreshold, InstaCountUtils.cannyThreshold);
                Imgproc.cvtColor(InstaCountUtils.mIntermediateMat, InstaCountUtils.mRgba, Imgproc.COLOR_GRAY2BGRA, 4);
                ShowTitle("Canny Edges", 1, colorGreen);
                break;
            case VIEW_MODE_HOUGHCIRCLES:
                Imgproc.cvtColor(InstaCountUtils.mRgba, InstaCountUtils.mGray, Imgproc.COLOR_RGBA2GRAY);
                Imgproc.GaussianBlur(InstaCountUtils.mGray, InstaCountUtils.mGray, new Size(InstaCountUtils.blurSize, InstaCountUtils.blurSize), 2, 2);
                Imgproc.HoughCircles(InstaCountUtils.mGray, InstaCountUtils.mIntermediateMat, Imgproc.CV_HOUGH_GRADIENT, 1.0, InstaCountUtils.minDistance, InstaCountUtils.cannyThreshold, InstaCountUtils.accumulatorThreshold, InstaCountUtils.minRadius, InstaCountUtils.maxRadius);
                InstaCountUtils.mCircleCount = InstaCountUtils.mIntermediateMat.cols() > 50 ? 50 : InstaCountUtils.mIntermediateMat.cols();
                if (InstaCountUtils.mCircleCount > 0) {
                    for (int x = 0; x < Math.min(InstaCountUtils.mIntermediateMat.cols(), 10); x++) {
                        double vCircle[] = InstaCountUtils.mIntermediateMat.get(0, x);
                        if (vCircle == null) break;
                        pt.x = Math.round(vCircle[0]);
                        pt.y = Math.round(vCircle[1]);
                        int radius = (int) Math.round(vCircle[2]);
                        Point center = new Point(vCircle[0], vCircle[1]);
                        Core.circle(InstaCountUtils.mRgba, pt, radius, colorWhite, iLineThickness);
                        InstaCountUtils.DrawCross(InstaCountUtils.mRgba, colorRed, pt);
                    }
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            info.setText(InstaCountUtils.SetInfoMessage());
                        }
                        catch (Exception e) {
                            Log.e(TAG, "onCameraFrame info.setText(InstaCountUtils.SetInfoMessage()): Exception = " + e.getMessage());
                        }
                    }
                });

                ShowTitle("Hough Circles", 1, colorGreen);
                break;
        }
        
        lMilliNow = System.currentTimeMillis();
        lFrameCount++;
        title = String.format("FPS: %2.1f", (float) (lFrameCount * 1000) / (float) (lMilliNow - lMilliStart));
        ShowTitle(title, 2, colorGreen);
        if (bShootNow) {
            lMilliShotTime = System.currentTimeMillis();
            bShootNow = false;
            sShotText = SaveImage(InstaCountUtils.mRgba);
            Intent myIntent = new Intent(CameraRTDetectActivity.this, CropActivity.class);
            myIntent.putExtra("filepath", sShotText);
            CameraRTDetectActivity.this.startActivity(myIntent);
            finish();
        }
        if (System.currentTimeMillis() - lMilliShotTime < 1500) ShowTitle(sShotText, 3, colorRed);
        return InstaCountUtils.mRgba;
    }

    public boolean onTouchEvent(final MotionEvent event) {
        bShootNow = true;
        return false;
    }

    @SuppressLint("SimpleDateFormat")
    public String SaveImage(Mat mat) {
        Imgproc.cvtColor(mat, InstaCountUtils.mIntermediateMat, Imgproc.COLOR_RGBA2BGR, 3);
        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        String filename = "instacount_";
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        Date date = new Date(System.currentTimeMillis());
        String dateString = fmt.format(date);
        int iFileOrdinal = 0;
        filename += dateString + "-" + iFileOrdinal;
        filename += ".png";
        File file = new File(path, filename);
        filename = file.toString();
        Highgui.imwrite(filename, InstaCountUtils.mIntermediateMat);
        return filename;
    }

    private void ShowTitle(String s, int iLineNum, Scalar color) {
        Core.putText(InstaCountUtils.mRgba, s, new Point(10, (int) (dTextScaleFactor * 60 * iLineNum)), Core.FONT_HERSHEY_SIMPLEX, dTextScaleFactor, color, 2);
    }
}