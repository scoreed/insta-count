package us.lucidian.instacount;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("UnusedDeclaration")
public class InstaCountUtils {
    private static final String TAG = "InstaCount::InstaCountUtils";
    public static Mat mGray            = null;
    public static Mat mRgba            = null;
    public static Mat mIntermediateMat = null;
    public static Mat resizedGray            = null;
    public static Mat resizedRgba            = null;
    public static Mat resizedIntermediateMat = null;
    public static int mCircleCount = 0;
    public static final int PHOTO_WIDTH  = 1280;
    public static final int PHOTO_HEIGHT = 1024;
    public static int iLineThickness = 3;
    public static int minDistance;
    public static int minRadius;
    public static int maxRadius;
    public static int cannyThreshold;
    public static int accumulatorThreshold;
    public static int blurSize;
    public static int maxMinDistance;
    public static int maxMinRadius;
    public static int maxMaxRadius;
    public static int maxCannyThreshold;
    public static int maxAccumulatorThreshold;
    public static int maxBlurSize;

    /**
     * void HoughCircles(InputArray image, OutputArray circles, int method, double dp, double minDist, double param1=100, double param2=100, int minRadius=0, int maxRadius=0 )
     *
     * 	image       – 8-bit, single-channel, grayscale input image.
     * 	circles     – Output vector of found circles. Each vector is encoded as a 3-element floating-point vector (x, y, radius) .
     * 	method      – Detection method to use. Currently, the only implemented method is CV_HOUGH_GRADIENT , which is basically 21HT.
     * 	dp          – Inverse ratio of the accumulator resolution to the image resolution. For example, if dp=1 , the accumulator has the same resolution as the input image.
     *                 If dp=2 , the accumulator has half as big width and height.
     * 	minDist     – Minimum distance between the centers of the detected circles. If the parameter is too small, multiple neighbor circles may be falsely detected in addition to a true one.
    *                 If it is too large, some circles may be missed.
    * 	param1      – First method-specific parameter. In case of CV_HOUGH_GRADIENT , it is the higher threshold of the two passed to the Canny() edge detector (the lower one is twice smaller).
    * 	param2      – Second method-specific parameter. In case of CV_HOUGH_GRADIENT , it is the accumulator threshold for the circle centers at the detection stage.
    *                 The smaller it is, the more false circles may be detected. Circles, corresponding to the larger accumulator values, will be returned first.
    * 	minRadius   – Minimum circle radius.
    * 	maxRadius   – Maximum circle radius.
    */
    public static String DetectCircles(Activity activity) {
        Log.i(TAG, "InstaCountUtils.DetectCircles Called");

//        LoadSharedPreferences(activity);

//        resizedGray = new Mat();
//        resizedRgba = new Mat();

        resizedRgba = mRgba.clone();
        resizedGray = mGray.clone();
        
        Mat smoothedGray = new Mat();
        Mat circles = new Mat();

//        Imgproc.resize(mGray, resizedGray, new Size(PHOTO_WIDTH, PHOTO_HEIGHT));
//        Imgproc.resize(mRgba, resizedRgba, new Size(PHOTO_WIDTH, PHOTO_HEIGHT));
       
        Imgproc.GaussianBlur(resizedGray, smoothedGray, new Size(blurSize,blurSize), 20, 20);
        
        Imgproc.HoughCircles(smoothedGray, circles, Imgproc.CV_HOUGH_GRADIENT, 1.0, minDistance, cannyThreshold, accumulatorThreshold, minRadius, maxRadius);

        mCircleCount = circles.cols() > 50 ? 50 : circles.cols();

        for (int i = 0; i < circles.cols(); i++) {
            double[] vCircle = circles.get(0, i);
            Point center = new Point(vCircle[0], vCircle[1]);
            int radius = (int) Math.round(vCircle[2]);
            //Core.circle(resizedRgba, center, 3, new Scalar(0, 255, 0), -1, 8, 0);     // circle center
            Core.circle(resizedRgba, center, radius, new Scalar(0, 0, 255), iLineThickness, 8, 0); // circle outline
            DrawCross(resizedRgba, new Scalar(0, 255, 0), center); // circle center
        }
        return SetInfoMessage();
    }

    public static void DrawCross(Mat mat, Scalar color, Point pt) {
        Point pt1 = new Point(0, 0);
        Point pt2 = new Point(0, 0);
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
    
    public static String SetInfoMessage() {
        String info;
        info = "";

        info += "Rows: ";
        info = info + ((mRgba != null) ? String.valueOf(mRgba.rows()) : "0") + " ";
        info += "Cols: ";
        info = info + ((mRgba != null) ? String.valueOf(mRgba.cols()) : "0") + " ";
        
        info += "Count: " + String.valueOf(mCircleCount) + "  ";
        info += "Blur: " + String.valueOf(blurSize) + "  ";
        info += "Canny: " + String.valueOf(cannyThreshold) + "  ";
        info += "Accum: " + String.valueOf(accumulatorThreshold) + "  ";
        info += "Min Dist: " + String.valueOf(minDistance) + "  ";
        info += "Min Rad: " + String.valueOf(minRadius) + "  ";
        info += "Max Rad: " + String.valueOf(maxRadius);
        return info;
    }
    
    public static void LoadSharedPreferences(Activity activity) {
        Log.i(TAG, "LoadSharedPreferences Called");
        SharedPreferences pref = activity.getPreferences(0);
        
        blurSize = Integer.parseInt(pref.getString("blur_size", activity.getString(R.string.default_blur_size)));
        minDistance = Integer.parseInt(pref.getString("min_distance", activity.getString(R.string.default_min_distance)));
        minRadius = Integer.parseInt(pref.getString("min_radius", activity.getString(R.string.default_min_radius)));
        maxRadius = Integer.parseInt(pref.getString("max_radius", activity.getString(R.string.default_max_radius)));
        cannyThreshold = Integer.parseInt(pref.getString("canny_threshold", activity.getString(R.string.default_canny_threshold)));
        accumulatorThreshold = Integer.parseInt(pref.getString("accumulator_threshold", activity.getString(R.string.default_accumulator_threshold)));

        maxBlurSize = Integer.parseInt(activity.getString(R.string.max_blur_size));
        maxMinDistance = Integer.parseInt(activity.getString(R.string.max_min_distance));
        maxMinRadius = Integer.parseInt(activity.getString(R.string.max_min_radius));
        maxMaxRadius = Integer.parseInt(activity.getString(R.string.max_max_radius));
        maxCannyThreshold = Integer.parseInt(activity.getString(R.string.max_canny_threshold));
        maxAccumulatorThreshold = Integer.parseInt(activity.getString(R.string.max_accumulator_threshold));
    }

    public static void SaveSharedPreferences(Activity activity) {
        Log.i(TAG, "SaveSharedPreferences Called");
        SharedPreferences pref = activity.getPreferences(0);
        SharedPreferences.Editor edt = pref.edit();
        edt.putString("blur_size", Integer.toString(blurSize));
        edt.putString("min_distance", Integer.toString(minDistance));
        edt.putString("min_radius", Integer.toString(minRadius));
        edt.putString("max_radius", Integer.toString(maxRadius));
        edt.putString("canny_threshold", Integer.toString(cannyThreshold));
        edt.putString("accumulator_threshold", Integer.toString(accumulatorThreshold));
        edt.commit();
    }
    
    public static void DrawRectangle() {
        resizedRgba = mRgba.clone();
        Core.rectangle(resizedRgba, new Point(10,10), new Point(mRgba.cols()/2.0,mRgba.rows()/2.0), new Scalar(0,0,255),0,8,0);
    }

    public static void FindContours() {
        // clone the image
        Mat original = mGray.clone();
        
        // thresholding the image to make a binary image
        Imgproc.threshold(mGray, mGray, 100, 128, Imgproc.THRESH_BINARY_INV);
        
        // find the center of the image
        double[] centers = {(double)mGray.width()/2, (double)mGray.height()/2};
        Point image_center = new Point(centers);

        // finding the contours
        ArrayList<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(mGray, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        // finding best bounding rectangle for a contour whose distance is closer to the image center that other ones
        double d_min = Double.MAX_VALUE;
        Rect rect_min = new Rect();
        for (MatOfPoint contour : contours) {
            Rect rec = Imgproc.boundingRect(contour);
            // find the best candidates
            if (rec.height > mGray.height()/2 & rec.width > mGray.width()/2)
                continue;
            Point pt1 = new Point((double)rec.x, (double)rec.y);
            Point center = new Point(rec.x+(double)(rec.width)/2, rec.y + (double)(rec.height)/2);
            double d = Math.sqrt(Math.pow(pt1.x-image_center.x,2) + Math.pow(pt1.y -image_center.y, 2));
            if (d < d_min)
            {
                d_min = d;
                rect_min = rec;
            }
        }
        // slicing the image for result region
        int pad = 5;
        rect_min.x = rect_min.x - pad;
        rect_min.y = rect_min.y - pad;
        rect_min.width = rect_min.width + 2*pad;
        rect_min.height = rect_min.height + 2*pad;
        
        resizedGray = original.submat(rect_min);
    }
    
    public static String DetectEllipses(Activity activity) {
        Log.i(TAG, "InstaCountUtils.DetectEllipses Called");

        LoadSharedPreferences(activity);

        resizedRgba = mRgba.clone();
        resizedGray = mGray.clone();

        Mat smoothedGray = new Mat();
        Mat result = new Mat();

        Mat threshold_output = new Mat();
        List<MatOfPoint> contours = new ArrayList<>();

        int thresh = 100;

        Imgproc.threshold(resizedGray, threshold_output, thresh, 255, Imgproc.THRESH_BINARY );
        Imgproc.findContours(threshold_output, contours, new Mat(), Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE, new Point(0, 0) );

        MatOfPoint mMOP2f1 = new MatOfPoint();
        MatOfPoint2f mMOP2f2 = new MatOfPoint2f();
        List<RotatedRect> minEllipse = new ArrayList<>(contours.size());

        for(int i=0;i<contours.size();i++){
            if( contours.get(i).toArray().length > 5 ) {
                contours.get(i).convertTo(mMOP2f1, CvType.CV_32FC2);
                minEllipse.set(i, Imgproc.fitEllipse(mMOP2f2));
            }
        }

        for (int i = 0; i < contours.size(); i++) {
            Scalar color = new Scalar(0, 0, 255);
            Imgproc.drawContours(resizedRgba, contours, i, color, 1, 8, new Mat(), 0, new Point());
            Core.ellipse(resizedRgba, minEllipse.get(i), color, 2, 8);
        }

        mCircleCount = contours.size();

        return SetInfoMessage();
    }

    public Bitmap resizeImageForImageView(Bitmap bitmap) {
        Bitmap resizedBitmap = null;
        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();
        int newWidth = -1;
        int newHeight = -1;
        float multFactor;
        if(originalHeight > originalWidth) {
            newHeight = PHOTO_WIDTH;
            multFactor = (float) originalWidth/(float) originalHeight;
            newWidth = (int) (newHeight*multFactor);
        } else if(originalWidth > originalHeight) {
            newWidth = PHOTO_WIDTH;
            multFactor = (float) originalHeight/ (float)originalWidth;
            newHeight = (int) (newWidth*multFactor);
        } else if(originalHeight == originalWidth) {
            newHeight = PHOTO_WIDTH;
            newWidth = PHOTO_WIDTH;
        }
        resizedBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, false);
        return resizedBitmap;
    }
    
    /**
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.
     */
    public static String getPath(final Context context, final Uri uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // DocumentProvider
            if (DocumentsContract.isDocumentUri(context, uri)) {
                // ExternalStorageProvider
                if (isExternalStorageDocument(uri)) {
                    final String docId = DocumentsContract.getDocumentId(uri);
                    final String[] split = docId.split(":");
                    final String type = split[0];

                    if ("primary".equalsIgnoreCase(type)) {
                        return Environment.getExternalStorageDirectory() + "/" + split[1];
                    }
                }
                // DownloadsProvider
                else if (isDownloadsDocument(uri)) {
                    final String id = DocumentsContract.getDocumentId(uri);
                    final Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
                    return getDataColumn(context, contentUri, null, null);
                }
                // MediaProvider
                else if (isMediaDocument(uri)) {
                    final String docId = DocumentsContract.getDocumentId(uri);
                    final String[] split = docId.split(":");
                    final String type = split[0];

                    Uri contentUri = null;
                    switch (type) {
                        case "image":
                            contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                            break;
                        case "video":
                            contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                            break;
                        case "audio":
                            contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                            break;
                    }

                    final String selection = "_id=?";
                    final String[] selectionArgs = new String[]{split[1]};

                    return getDataColumn(context, contentUri, selection, selectionArgs);
                }
            }
            // MediaStore (and general)
            else if ("content".equalsIgnoreCase(uri.getScheme())) {
                return getDataColumn(context, uri, null, null);
            }
            // File
            else if ("file".equalsIgnoreCase(uri.getScheme())) {
                return uri.getPath();
            }
        }
        else {
            String[] projection = {MediaStore.Images.Media.DATA};
            Cursor cursor = ((Activity) context).managedQuery(uri, projection, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        }

        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }
}
