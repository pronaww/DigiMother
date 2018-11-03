package com.baurasia.pranav.digimother;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.google.zxing.FormatException;
import com.google.zxing.LuminanceSource;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.ResultPoint;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.DetectorResult;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.detector.Detector;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Vector;

import static java.lang.Math.abs;

public class TableActivity extends AppCompatActivity {

    ImageView imageView;

    static {
        System.loadLibrary("native-lib");
        System.loadLibrary("native-opencv-lib");
    }

    private static String TAG = "TableActivity";
    Mat mRgba, mGray;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_table);

        Intent intent = getIntent();
        Uri uri = intent.getExtras().getParcelable("scanned_image");

        //            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
        Bitmap bitmap = ratioChecker(uri);
        imageHeight = bitmap.getHeight();
        imageWidth = bitmap.getWidth();

        bitmap = Bitmap.createScaledBitmap(bitmap,1276, 2976, true);
        mRgba = new Mat();
        Utils.bitmapToMat(bitmap, mRgba);
        imageHeight = mRgba.height();

        bitmap = Bitmap.createScaledBitmap(bitmap,bitmap.getWidth()/2, bitmap.getHeight()/2, true);
        ImageView imageView = findViewById(R.id.image1);
        imageView.setImageBitmap(bitmap);

    }

    public static Bitmap RotateBitmap(Bitmap source, float angle)
    {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    public Bitmap ratioChecker (Uri uri) {
        Bitmap bitmap = null;
        try {
            bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
        } catch (IOException e) {
            e.printStackTrace();
        }

        float h = bitmap.getHeight();
        float w = bitmap.getWidth();

        if(w>h){
            bitmap = RotateBitmap(bitmap, 90);
        }

        return bitmap;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(OpenCVLoader.initDebug()) {
            Log.i(TAG, "opencv Loaded successfully");
//            mLoaderCallBack.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
        else {
            Log.i(TAG, "opencv not loaded");
//            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, mLoaderCallBack);
        }
    }


    public void detectChildID(View view) {
//        Button button = findViewById(R.id.btn);
//        button.setText(stringFromJNI());
        Vector<qrCodeWithLocation> n = qrFinder(true);

        if(n.size()!=0)
            Toast.makeText(this, n.get(0).barcode.displayValue, Toast.LENGTH_SHORT);
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();

    private Mat readImageFromResources(int resourceId) {
        Mat img = null;
        try {
            img = Utils.loadResource(this, resourceId);
            Imgproc.cvtColor(img, img, Imgproc.COLOR_RGB2BGRA);
        } catch (IOException e) {
            Log.e(TAG,Log.getStackTraceString(e));
        }
        return img;
    }

    public void detectTable(View view) {

        Log.d("height", String.valueOf(mRgba.height()));
        Log.d("width", String.valueOf(mRgba.width()));
        mGray = new Mat(mRgba.height(), mRgba.width(), CvType.CV_8UC1);

        OpencvNativeClass.findSquares(mRgba.getNativeObjAddr(), mGray.getNativeObjAddr());
//        OpencvNativeClass.convertGray(mRgba.getNativeObjAddr(), mGray.getNativeObjAddr());

        Bitmap bitmap = Bitmap.createBitmap(mGray.cols(), mGray.rows(),Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mGray, bitmap);
        bitmap = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth()/2, bitmap.getHeight()/2, true);
        imageView = findViewById(R.id.image1);
        imageView.setImageBitmap(bitmap);
    }

    //classifier
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private static final int INPUT_SIZE = 224;
    private static final int IMAGE_MEAN = 128;
    private static final float IMAGE_STD = 128.0f;
    private static final String INPUT_NAME = "input";
    private static final String OUTPUT_NAME = "final_result";

    private static final String MODEL_FILE = "file:///android_asset/graph.pb";
    private static final String LABEL_FILE = "file:///android_asset/labels.txt";

    private static final boolean SAVE_PREVIEW_BITMAP = false;

    private static final boolean MAINTAIN_ASPECT = true;

    private static final Size DESIRED_PREVIEW_SIZE = new Size(640, 480);

    private Classifier classifier;

    private Integer sensorOrientation;

    private int previewWidth = 0;
    private int previewHeight = 0;
    private byte[][] yuvBytes;
    private int[] rgbBytes = null;
    private Bitmap rgbFrameBitmap = null;
    private Bitmap croppedBitmap = null;

    private Bitmap cropCopyBitmap;
    ////////////////////////////////////////////////////////////////////////////////////////////////


    private Bitmap rotationChecker(Bitmap bitmap) {
        Log.d("ROTATION CHECKER", "STARTED");
        classifier =
                TensorFlowImageClassifier.create(
                        getAssets(),
                        MODEL_FILE,
                        LABEL_FILE,
                        INPUT_SIZE,
                        IMAGE_MEAN,
                        IMAGE_STD,
                        INPUT_NAME,
                        OUTPUT_NAME);

        Bitmap cropBitmap = Bitmap.createBitmap(bitmap,0,0, bitmap.getWidth(), bitmap.getHeight()/2);
        cropBitmap = Bitmap.createScaledBitmap(cropBitmap, INPUT_SIZE, INPUT_SIZE, false);

        final List<Classifier.Recognition> results = classifier.recognizeImage(cropBitmap);

        for(int i=0; i<results.size(); i++) {
            Log.d("Title is ", results.get(i).getTitle());
            Log.d("Id is ", results.get(i).getId());
            Log.d("Confidence is ", results.get(i).getId());
        }

        if(results.get(0).getTitle().equals("lower"))
            Core.rotate(mRgba, mRgba, Core.ROTATE_180);

        return bitmap;

    }

    public void rotationChecker(View view) {
        Bitmap bitmap = Bitmap.createBitmap(mRgba.cols(), mRgba.rows(),Bitmap.Config.ARGB_8888);;
        Utils.matToBitmap(mRgba, bitmap);
        bitmap = rotationChecker(bitmap);
        Utils.matToBitmap(mRgba, bitmap);
        bitmap = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth()/2, bitmap.getHeight()/2, false);
        imageView = findViewById(R.id.image1);
        imageView.setImageBitmap(bitmap);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    int imageHeight, imageWidth;
    public Vector<qrCodeWithLocation> qrFinder(boolean isChildID) {
        Bitmap bitmap = Bitmap.createBitmap(mRgba.cols(), mRgba.rows(),Bitmap.Config.ARGB_8888);;
        Utils.matToBitmap(mRgba, bitmap);

        if(isChildID==true)
            bitmap = Bitmap.createBitmap(bitmap,0,0, mRgba.cols(), mRgba.rows()/4);

        BarcodeDetector barcodeDetector = new BarcodeDetector.Builder(this).setBarcodeFormats(Barcode.QR_CODE).build();

//        Log.d("width", String.valueOf(imageWidth));
//        Log.d("height", String.valueOf(imageHeight));

        //increment i by 10% of width on each iteration
        //increment j by 10% of width on each iteration
        int addend = bitmap.getWidth()/10;

        //window size will be of length = 10% of height
        int windowLength = bitmap.getHeight()/10;

        Vector<qrCodeWithLocation> qrCodes = new Vector<>();

        Vector<Barcode> v = new Vector<>();
        Frame myFrame;
        SparseArray<Barcode> barcodes;

        long startTime = System.currentTimeMillis();

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        for(int i=0; i<width-windowLength; i+=addend){
            for(int j=0; j<height-windowLength; j+=addend){
                Bitmap croppedBitmap = Bitmap.createBitmap(bitmap, i, j, windowLength, windowLength);

                vector2 qrLocation = loc(croppedBitmap,i,j);

                myFrame = new Frame.Builder().setBitmap(croppedBitmap).build();
                barcodes = barcodeDetector.detect(myFrame);

                if(qrLocation!=null && barcodes!=null && barcodes.size()>0){
                    qrCodes.add(new qrCodeWithLocation(barcodes.valueAt(0), qrLocation));
                }
                for(int l=0; l<barcodes.size(); l++){
                    v.add(barcodes.valueAt(l));
                }

            }
        }
        long endTime   = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        Log.d("execution time", String.valueOf(totalTime));


        int len = v.size();
        Log.d("size", String.valueOf(len));

        if(len!=0) {
            Log.d("QR Code data", v.get(0).displayValue);

            for (int k = 1; k < v.size(); k++) {
                if (v.get(k - 1).displayValue.equals(v.get(k).displayValue)) continue;
                Log.d("QR Code data", v.get(k).displayValue);
            }
        }

        qrCodes = removeDuplicates(qrCodes);

        return qrCodes;
    }

    LuminanceSource bils;
    HybridBinarizer hb;
    BitMatrix bm;
    //image sent to this method should be of proper size otherwise getBlackMatrix will not work because of large size
    public vector2 loc(Bitmap image, int a, int b){

        int[] intArray = new int[(image.getHeight())*(image.getHeight())];
        image.getPixels(intArray, 0, image.getWidth(), 0, 0, image.getHeight(), imageHeight/10);//        byte[] array = BitmapToArray(image);
//        LuminanceSource bils = new PlanarYUVLuminanceSource(array, image.getWidth(), image.getHeight(), 0, 0, image.getWidth(), image.getHeight(), false);
        bils = new RGBLuminanceSource(imageHeight/10, imageHeight/10, intArray);
        hb = new HybridBinarizer(bils);//bils is BufferedImageLuminanceSource object
        bm = null;
        try {
            bm = hb.getBlackMatrix();
        } catch (NotFoundException e) {
            e.printStackTrace();
            Log.d("first not found", String.valueOf(0));
            return null;
        }

        Detector detector = new Detector( bm );

        DetectorResult dresult = null;
        try {
            dresult = detector.detect();
        } catch (NotFoundException e) {
            e.printStackTrace();
            Log.d("second not found", String.valueOf(0));
            return null;
        } catch (FormatException e) {
            e.printStackTrace();
        }

        vector2 point = new vector2(-100, -100);
        if(dresult!=null) {
            ResultPoint[] resultPoints = dresult.getPoints();

//        Log.d("resultPoints:", String.valueOf(0));
//        for (ResultPoint resultPoint :resultPoints) {
//
//            Log.d(" x = ", String.valueOf(resultPoint.getX() + a));
//            Log.d(" y = ", String.valueOf(resultPoint.getY() + b));
////            if (resultPoint instanceof FinderPattern)
////                Log.d("estimatedModuleSize = ", String.valueOf(((FinderPattern) resultPoint).getEstimatedModuleSize()));
//        }

            if (resultPoints.length >= 3) {
                float midPointX = (resultPoints[0].getX() + resultPoints[2].getX()) / 2;
                float midPointY = (resultPoints[0].getY() + resultPoints[2].getY()) / 2;
                point = new vector2(midPointX + a, midPointY + b);
                Log.d("QRLOCATION IS: ", String.valueOf(midPointX + a) + " " + String.valueOf(midPointY + b));
            }
        }

        return point;
    }

    public Vector<qrCodeWithLocation> removeDuplicates (Vector<qrCodeWithLocation> v) {
        Vector<qrCodeWithLocation> newVector = new Vector<>();
        Log.d("size of vector is ", String.valueOf(v.size()));
        for(int i=0; i<v.size(); i++){
            int j;
            for(j=i+1; j<v.size(); j++){
                if(equals(v.get(i),v.get(j))) {
                    Log.d("two locations ", "equal");
                    break;
                }
            }
            if(j==v.size()){
                newVector.add(v.get(i));
            }
        }

        for(qrCodeWithLocation qr: newVector){
            Log.d("QRCODE",qr.barcode.displayValue);
        }
        return newVector;
    }

    private boolean equals(qrCodeWithLocation qrCodeWithLocation, qrCodeWithLocation qrCodeWithLocation1) {

        float c1 = qrCodeWithLocation.coordinates.x - qrCodeWithLocation1.coordinates.x;
        float c2 = qrCodeWithLocation.coordinates.y - qrCodeWithLocation1.coordinates.y;

        if (abs(c1)<imageHeight/100 && abs(c2)<imageHeight/100) {
            return true;
        }

        return false;
    }

    Bitmap tableBitmap;
    public void cropTableOnClick(View view) {

        tableBitmap = cropTable();

    }

    public void oPenVaccineActivty(View view) {
        Uri uri = getImageUri(this, tableBitmap);
        Intent intent1 = new Intent(this, VaccineActivity.class);
        intent1.putExtra("uri",uri.toString());
        startActivity(intent1);

    }

    public Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }

    private Bitmap cropTable() {
        Mat mTable = new Mat(mRgba.height(), mRgba.width(), CvType.CV_8UC1);
        OpencvNativeClass.cropTable(mRgba.getNativeObjAddr(), mTable.getNativeObjAddr());
        Bitmap bitmap = Bitmap.createBitmap(mTable.cols(), mTable.rows(),Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mTable, bitmap);
        ImageView imageView = findViewById(R.id.image1);
        imageView.setImageBitmap(bitmap);
        return bitmap;
    }
}
