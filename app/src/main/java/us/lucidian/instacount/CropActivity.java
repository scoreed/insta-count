package us.lucidian.instacount;

import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import android.widget.TextView;

import com.appkilt.client.AppKilt;
import com.getbase.floatingactionbutton.FloatingActionButton;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.lang.ref.WeakReference;

import us.lucidian.instacount.gestures.MoveGestureDetector;
import us.lucidian.instacount.gestures.RotateGestureDetector;

public class CropActivity extends ActionBarActivity implements OnTouchListener {
    private static final String TAG = "InstaCount::CropActivity";
    // Member fields.
    private static ImageView      mImg;
    private        ImageView      mTemplateImg;
    private        int            mScreenWidth;
    private        int            mScreenHeight;
    private        CropHandler    mCropHandler;
    private static ProgressDialog mProgressDialog;
    private        int            mSelectedVersion;

    private Matrix mMatrix          = new Matrix();
    private float  mScaleFactor     = 0.8f;
    private float  mRotationDegrees = 0.f;
    private float  mFocusX          = 0.f;
    private float  mFocusY          = 0.f;
    private static int                   mImageHeight;
    private static int                   mImageWidth;
    private        ScaleGestureDetector  mScaleDetector;
    private        RotateGestureDetector mRotateDetector;
    private        MoveGestureDetector   mMoveDetector;

    private int mTemplateWidth;
    private int mTemplateHeight;

    private boolean showCircles = true;
    private        TextView tv_circle_count;
    private static Bitmap   mSelectedImage;

    // Constants
    public static final  int MEDIA_GALLERY      = 1;
    public static final  int TEMPLATE_SELECTION = 2;
    public static final  int DISPLAY_IMAGE      = 3;
    private final static int IMG_MAX_SIZE       = 1000;
    private final static int IMG_MAX_SIZE_MDPI  = 400;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crop);

        mSelectedVersion = InstaCountMainActivity.VERSION_1;

        mImg = (ImageView) findViewById(R.id.cp_img);
        mTemplateImg = (ImageView) findViewById(R.id.cp_face_template);
        mImg.setOnTouchListener(this);

        String filePath;
        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if (extras == null) {
                filePath = null;
            }
            else {
                filePath = extras.getString("filepath");
            }
        }
        else {
            filePath = (String) savedInstanceState.getSerializable("filepath");
        }

        // Get screen size in pixels.
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mScreenHeight = metrics.heightPixels;
        mScreenWidth = metrics.widthPixels;

        Bitmap faceTemplate = BitmapFactory.decodeResource(getResources(), R.drawable.crop_square);
        mTemplateWidth = faceTemplate.getWidth();
        mTemplateHeight = faceTemplate.getHeight();

        // Set template image accordingly to device screen size.
        if (mScreenWidth == 320 && mScreenHeight == 480) {
            mTemplateWidth = 218;
            mTemplateHeight = 300;
            faceTemplate = Bitmap.createScaledBitmap(faceTemplate, mTemplateWidth, mTemplateHeight, true);
            mTemplateImg.setImageBitmap(faceTemplate);
        }

        if (filePath != null) {
            setSelectedImage(filePath);
        }

        // View is scaled by matrix, so scale initially
        mMatrix.postScale(mScaleFactor, mScaleFactor);
        mImg.setImageMatrix(mMatrix);
        
        // Setup Gesture Detectors
        mScaleDetector = new ScaleGestureDetector(getApplicationContext(), new ScaleListener());
        mRotateDetector = new RotateGestureDetector(getApplicationContext(), new RotateListener());
        mMoveDetector = new MoveGestureDetector(getApplicationContext(), new MoveListener());
        
        // Instantiate Thread Handler.
        mCropHandler = new CropHandler(this);

        InstaCountUtils.mDstWidth = getResources().getDimensionPixelSize(R.dimen.destination_width);
        InstaCountUtils.mDstHeight = getResources().getDimensionPixelSize(R.dimen.destination_height);

        InstaCountUtils.LoadSharedPreferences(CropActivity.this);

        tv_circle_count = (TextView)findViewById(R.id.tv_circle_count);

        FloatingActionButton btn_blur_up = (FloatingActionButton) findViewById(R.id.btn_blur_up);
        FloatingActionButton btn_blur_down = (FloatingActionButton) findViewById(R.id.btn_blur_down);
        FloatingActionButton btn_canny_up = (FloatingActionButton) findViewById(R.id.btn_canny_up);
        FloatingActionButton btn_canny_down = (FloatingActionButton) findViewById(R.id.btn_canny_down);
        FloatingActionButton btn_accum_up = (FloatingActionButton) findViewById(R.id.btn_accum_up);
        FloatingActionButton btn_accum_down = (FloatingActionButton) findViewById(R.id.btn_accum_down);
        FloatingActionButton btn_min_distance_up = (FloatingActionButton) findViewById(R.id.btn_min_distance_up);
        FloatingActionButton btn_min_distance_down = (FloatingActionButton) findViewById(R.id.btn_min_distance_down);
        FloatingActionButton btn_min_radius_up = (FloatingActionButton) findViewById(R.id.btn_min_radius_up);
        FloatingActionButton btn_min_radius_down = (FloatingActionButton) findViewById(R.id.btn_min_radius_down);
        FloatingActionButton btn_max_radius_up = (FloatingActionButton) findViewById(R.id.btn_max_radius_up);
        FloatingActionButton btn_max_radius_down = (FloatingActionButton) findViewById(R.id.btn_max_radius_down);

        btn_blur_up.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (InstaCountUtils.blurSize <= InstaCountUtils.maxBlurSize) {
                    InstaCountUtils.blurSize += 2;
                    runCircleDetect();
                }
            }
        });
        btn_blur_down.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (InstaCountUtils.blurSize >= 3) {
                    InstaCountUtils.blurSize -= 2;
                    runCircleDetect();
                }
            }
        });
        btn_canny_up.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (InstaCountUtils.cannyThreshold <= InstaCountUtils.maxCannyThreshold) {
                    InstaCountUtils.cannyThreshold++;
                    runCircleDetect();
                }
            }
        });
        btn_canny_down.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (InstaCountUtils.cannyThreshold > 1) {
                    InstaCountUtils.cannyThreshold--;
                    runCircleDetect();
                }
            }
        });
        btn_accum_up.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (InstaCountUtils.accumulatorThreshold <= InstaCountUtils.maxAccumulatorThreshold) {
                    InstaCountUtils.accumulatorThreshold++;
                    runCircleDetect();
                }
            }
        });
        btn_accum_down.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (InstaCountUtils.accumulatorThreshold > 1) {
                    InstaCountUtils.accumulatorThreshold--;
                    runCircleDetect();
                }
            }
        });
        btn_min_distance_up.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (InstaCountUtils.minDistance <= InstaCountUtils.maxMinDistance) {
                    InstaCountUtils.minDistance++;
                    runCircleDetect();
                }
            }
        });
        btn_min_distance_down.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (InstaCountUtils.minDistance > 1) {
                    InstaCountUtils.minDistance--;
                    runCircleDetect();
                }
            }
        });
        btn_min_radius_up.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (InstaCountUtils.minRadius <= InstaCountUtils.maxMinRadius) {
                    InstaCountUtils.minRadius++;
                    runCircleDetect();
                }
            }
        });
        btn_min_radius_down.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (InstaCountUtils.minRadius > 1) {
                    InstaCountUtils.minRadius--;
                    runCircleDetect();
                }
            }
        });
        btn_max_radius_up.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (InstaCountUtils.maxRadius <= InstaCountUtils.maxMaxRadius) {
                    InstaCountUtils.maxRadius++;
                    runCircleDetect();
                }
            }
        });
        btn_max_radius_down.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (InstaCountUtils.maxRadius > 1) {
                    InstaCountUtils.maxRadius--;
                    runCircleDetect();
                }
            }
        });
        ((TextView)findViewById(R.id.tv_circle_count)).setText(InstaCountUtils.SetInfoMessage());
    }

    public void runCircleDetect() {
        if (mSelectedImage == null) {
            ((TextView)findViewById(R.id.tv_circle_count)).setText(InstaCountUtils.SetInfoMessage());
            return;
        }

        if (showCircles) {
            InstaCountUtils.mRgba = new Mat(mSelectedImage.getHeight(), mSelectedImage.getWidth(), CvType.CV_8UC4);
            InstaCountUtils.mGray = new Mat(mSelectedImage.getHeight(), mSelectedImage.getWidth(), CvType.CV_8UC1);
            Bitmap bmp32 = mSelectedImage.copy(Bitmap.Config.ARGB_8888, true);
            Utils.bitmapToMat(bmp32, InstaCountUtils.mRgba);
            Imgproc.cvtColor(InstaCountUtils.mRgba, InstaCountUtils.mGray, Imgproc.COLOR_BGR2GRAY, 1);
            tv_circle_count.setText(InstaCountUtils.DetectCircles(CropActivity.this));
            bmp32 = Bitmap.createBitmap(InstaCountUtils.resizedRgba.cols(), InstaCountUtils.resizedRgba.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(InstaCountUtils.resizedRgba, bmp32);
            mImg.setImageBitmap(bmp32);
        } else if (InstaCountUtils.mRgba != null && InstaCountUtils.mRgba.cols() > 0 && InstaCountUtils.mRgba.rows() > 0) {
            Bitmap bmp32 = mSelectedImage.copy(Bitmap.Config.ARGB_8888, true);
            mImg.setImageBitmap(bmp32);
        }
    }

    public void onCropImageButton(View v) {
        if (mSelectedImage == null) {
            ((TextView)findViewById(R.id.tv_circle_count)).setText(InstaCountUtils.SetInfoMessage());
            return;
        }
        // Create progress dialog and display it.
        mProgressDialog = new ProgressDialog(v.getContext());
        mProgressDialog.setCancelable(false);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setMessage("Cropping Image\nPlease Wait.....");
        mProgressDialog.show();

        // Setting values so that we can retrive the image from 
        // ImageView multiple times.
        mImg.buildDrawingCache(true);
        mImg.setDrawingCacheEnabled(true);
        mTemplateImg.buildDrawingCache(true);
        mTemplateImg.setDrawingCacheEnabled(true);
        
        // Create new thread to crop.
        new Thread(new Runnable() {
            @Override
            public void run() {
                // Crop image using the correct template size.
                Bitmap croppedImg;
                if (mScreenWidth == 320 && mScreenHeight == 480) {
                    if (mSelectedVersion == InstaCountMainActivity.VERSION_1) {
                        croppedImg = ImageProcess.cropImage(mImg.getDrawingCache(true), mTemplateImg.getDrawingCache(true), mTemplateWidth, mTemplateHeight);
                    } else {
                        croppedImg = ImageProcess.cropImageVer2(mImg.getDrawingCache(true), mTemplateImg.getDrawingCache(true), mTemplateWidth, mTemplateHeight);
                    }
                } else {
                    if (mSelectedVersion == InstaCountMainActivity.VERSION_1) {
                        croppedImg = ImageProcess.cropImage(mImg.getDrawingCache(true), mTemplateImg.getDrawingCache(true), mTemplateWidth, mTemplateHeight);
                    } else {
                        croppedImg = ImageProcess.cropImageVer2(mImg.getDrawingCache(true), mTemplateImg.getDrawingCache(true), mTemplateWidth, mTemplateHeight);
                    }
                }
                mImg.setDrawingCacheEnabled(false);
                mTemplateImg.setDrawingCacheEnabled(false);
                
                // Send a message to the Handler indicating the Thread has finished.
                mCropHandler.obtainMessage(DISPLAY_IMAGE, -1, -1, croppedImg).sendToTarget();
            }
        }).start();
    }
    
    public void onChangeTemplateButton(View v) {
        Intent intent = new Intent(this, TemplateSelectDialog.class);
        startActivityForResult(intent, TEMPLATE_SELECTION);
    }
    
    public void onChangeImageButton(View v) {
        // Start Gallery App.
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, MEDIA_GALLERY);
    }

    public void onCameraCaptureButton(View view) throws IOException {
        Intent myIntent = new Intent(CropActivity.this, CameraRTDetectFragment.class);
        CropActivity.this.startActivity(myIntent);
    }

    /*
     * Adjust the size of bitmap before loading it to memory.
     * This will help the phone by not taking up a lot memory.
     */
    private void setSelectedImage(String path) {

        Log.i(TAG, path);

        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        if (mScreenWidth == 320 && mScreenHeight == 480) {
            options.inSampleSize = calculateImageSize(options, IMG_MAX_SIZE_MDPI);
        } else {
            options.inSampleSize = calculateImageSize(options, IMG_MAX_SIZE);
        }
        
        options.inJustDecodeBounds = false;
        mSelectedImage = BitmapFactory.decodeFile(path, options);
        mImageHeight = mSelectedImage.getHeight();
        mImageWidth = mSelectedImage.getWidth();
        mImg.setImageBitmap(mSelectedImage);
    }

    /*
     * Retrieves the path to the selected image from the Gallery app.
     */
    private String getGalleryImagePath(Intent data) {
        Uri imgUri = data.getData();
        String filePath = "";
        if (data.getType() == null) {
            // For getting images from gallery.
            String[] filePathColumn = { MediaStore.Images.Media.DATA };
            Cursor cursor = getContentResolver().query(imgUri, filePathColumn, null, null, null);
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            filePath = cursor.getString(columnIndex);
            cursor.close();
        } 
        return filePath;
    }
    
    /*
     * Calculation used to determine by what factor images need to be reduced by.
     * Images with its longest side below the threshold will not be resized.
     */
    private int calculateImageSize(BitmapFactory.Options opts, int threshold) {
        int scaleFactor;
        final int height = opts.outHeight;
        final int width = opts.outWidth;

        if (width >= height) {
            scaleFactor = Math.round((float) width / threshold);
        } else {
            scaleFactor = Math.round((float) height / threshold);
        }
        return scaleFactor;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == MEDIA_GALLERY) {
                String path = getGalleryImagePath(data);
                setSelectedImage(path);
            } else if (requestCode == TEMPLATE_SELECTION) {
                int pos = data.getExtras().getInt(TemplateSelectDialog.POSITION);
                Bitmap templateImg = null;

                // Change template according to what the user has selected.
                switch (pos) {
                    case 0:
                        templateImg = BitmapFactory.decodeResource(getResources(), R.drawable.crop_square);
                        break;
                    case 1:
                        templateImg = BitmapFactory.decodeResource(getResources(), R.drawable.crop_rectangle);
                        break;
                    case 2:
                        templateImg = BitmapFactory.decodeResource(getResources(), R.drawable.crop_circle);
                        break;
                }

                if (templateImg != null) {
                    mTemplateWidth = templateImg.getWidth();
                    mTemplateHeight = templateImg.getHeight();
                    // Resize template if necessary.
                    if (mScreenWidth == 320 && mScreenHeight == 480) {
                        mTemplateWidth = 218;
                        mTemplateHeight = 300;
                        templateImg = Bitmap.createScaledBitmap(templateImg, mTemplateWidth, mTemplateHeight, true);
                    }
                    mTemplateImg.setImageBitmap(templateImg);
                }
            }
        }
    }

    public void onDetectCirclesButton(View view) {
        runCircleDetect();
        showCircles = !showCircles;
    }

    public void onClearImageButton(View view) {
        mSelectedImage = null;
        mImg.setImageDrawable(null);
    }

    private static class CropHandler extends Handler {
        WeakReference<CropActivity> mThisCA;

        CropHandler(CropActivity ca) {
            mThisCA = new WeakReference<>(ca);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            if (msg.what == DISPLAY_IMAGE) {
                mProgressDialog.dismiss();

                Bitmap cropImg = (Bitmap) msg.obj;

                if (cropImg.getHeight() > 1024 || cropImg.getWidth() > 1024) {
                    mSelectedImage = ScalingUtilities.createScaledBitmap(cropImg, InstaCountUtils.mDstWidth, InstaCountUtils.mDstHeight, ScalingUtilities.ScalingLogic.FIT);
                    cropImg.recycle();
                } else {
                    mSelectedImage = cropImg;
                }

//                mSelectedImage = cropImg.copy(Bitmap.Config.ARGB_8888, true);
                mImageHeight = mSelectedImage.getHeight();
                mImageWidth = mSelectedImage.getWidth();
                mImg.setImageBitmap(mSelectedImage);
            }
        }
    }

    public boolean onTouch(View v, MotionEvent event) {
        mScaleDetector.onTouchEvent(event);
        mRotateDetector.onTouchEvent(event);
        mMoveDetector.onTouchEvent(event);

        float scaledImageCenterX = (mImageWidth * mScaleFactor) / 2;
        float scaledImageCenterY = (mImageHeight * mScaleFactor) / 2;

        mMatrix.reset();
        mMatrix.postScale(mScaleFactor, mScaleFactor);
        mMatrix.postRotate(mRotationDegrees, scaledImageCenterX, scaledImageCenterY);
        mMatrix.postTranslate(mFocusX - scaledImageCenterX, mFocusY - scaledImageCenterY);

        ImageView view = (ImageView) v;
        view.setImageMatrix(mMatrix);
        return true;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            mScaleFactor *= detector.getScaleFactor();
            mScaleFactor = Math.max(0.1f, Math.min(mScaleFactor, 10.0f));

            return true;
        }
    }

    private class RotateListener extends RotateGestureDetector.SimpleOnRotateGestureListener {
        @Override
        public boolean onRotate(RotateGestureDetector detector) {
            mRotationDegrees -= detector.getRotationDegreesDelta();
            return true;
        }
    }

    private class MoveListener extends MoveGestureDetector.SimpleOnMoveGestureListener {
        @Override
        public boolean onMove(MoveGestureDetector detector) {
            PointF d = detector.getFocusDelta();
            mFocusX += d.x;
            mFocusY += d.y;

            return true;
        }
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.crop_activity_menu, menu);
        restoreActionBar();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_logout) {
            finish();
        } else if (id == R.id.action_reset_settings) {
            InstaCountUtils.ResetSharedPreferences(CropActivity.this);
            ((TextView)findViewById(R.id.tv_circle_count)).setText(InstaCountUtils.SetInfoMessage());
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, CropActivity.this, mLoaderCallback);
        AppKilt.onUpdateableActivityResume(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        InstaCountUtils.SaveSharedPreferences(CropActivity.this);
        AppKilt.onUpdateableActivityPause();
    }

    @Override
    public void onStop() { super.onStop(); InstaCountUtils.SaveSharedPreferences(CropActivity.this); }

    public void onDestroy() { super.onDestroy(); InstaCountUtils.SaveSharedPreferences(CropActivity.this); }
}
