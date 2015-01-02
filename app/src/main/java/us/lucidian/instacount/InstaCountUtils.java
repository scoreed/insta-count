package us.lucidian.instacount;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

@SuppressWarnings("UnusedDeclaration")
public class InstaCountUtils {
    private static final String TAG = "InstaCount::InstaCountUtils";

    public static Mat mGray            = null;
    public static Mat mRgba            = null;
    public static Mat mIntermediateMat = null;
    public static int mCircleCount     = 0;

    public static int minDistance;
    public static int minRadius;
    public static int maxRadius;
    public static int cannyThreshold;
    public static int accumulatorThreshold;

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

        LoadSharedPreferences(activity);

        Mat smoothedGray = new Mat();
        Mat circles = new Mat();

        Imgproc.GaussianBlur(mGray, smoothedGray, new Size(5,5), 20, 20);
        Imgproc.HoughCircles(smoothedGray, circles, Imgproc.CV_HOUGH_GRADIENT, 1, minDistance, cannyThreshold, accumulatorThreshold, minRadius, maxRadius);

        mCircleCount = circles.cols() > 50 ? 50 : circles.cols();

        for (int i = 0; i < circles.cols(); i++) {
            double[] vCircle = circles.get(0, i);
            Point center = new Point(vCircle[0], vCircle[1]);
            int radius = (int) Math.round(vCircle[2]);
            Core.circle(mRgba, center, 3, new Scalar(0, 255, 0), -1, 8, 0);     // circle center
            Core.circle(mRgba, center, radius, new Scalar(0, 0, 255), 3, 8, 0); // circle outline
        }
        return BuildInfoMessage();
    }

    public static String BuildInfoMessage() {
        return BuildInfoMessage(mGray.rows(), mGray.cols(), mCircleCount);
    }

    public static String BuildInfoMessage(int imageRows, int imageCols, int circleCount) {
        String info;
        info = "Image Rows: " + String.valueOf(imageRows) + "  ";
        info += "Image Cols: " + String.valueOf(imageCols) + "  ";
        info += "Circle Count: " + String.valueOf(circleCount);
        Log.d(TAG, "BuildInfoMessage: info = " + info);
        return info;
    }

    public static void LoadSharedPreferences(Activity activity)
    {
        Log.i(TAG, "LoadSharedPreferences Called");

        SharedPreferences pref = activity.getPreferences(0);
        minDistance = Integer.parseInt(pref.getString("min_distance", activity.getString(R.string.default_min_distance)));
        minRadius = Integer.parseInt(pref.getString("min_radius", activity.getString(R.string.default_min_radius)));
        maxRadius = Integer.parseInt(pref.getString("max_radius", activity.getString(R.string.default_max_radius)));
        cannyThreshold = Integer.parseInt(pref.getString("canny_threshold", activity.getString(R.string.default_canny_threshold)));
        accumulatorThreshold = Integer.parseInt(pref.getString("accumulator_threshold", activity.getString(R.string.default_accumulator_threshold)));
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
