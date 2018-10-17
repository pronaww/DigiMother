package com.baurasia.pranav.digimother;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.util.SparseArray;
import android.view.View;
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
import com.scanlibrary.ScanActivity;
import com.scanlibrary.ScanConstants;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import static java.lang.Math.abs;

public class MainActivity extends AppCompatActivity {

    int imageHeight, imageWidth;
    String string = "No Child ID detected";

    AlertDialog.Builder builder;
    ImageView img;
    private final int CAMERA_REQUEST_CODE = 2;
    private final int READ_REQUEST_CODE = 3;
    private final int WRITE_REQUEST_CODE = 1;
    private final int INTERNET_REQUEST_CODE = 4;
    private final int ALL_REQUEST_CODE = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String permissions[] = new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.INTERNET};

        //For permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, permissions, ALL_REQUEST_CODE);
        }
        else if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[] {permissions[1], permissions[2], permissions[3]}, ALL_REQUEST_CODE);
        }
        else if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[] {permissions[2], permissions[3]}, ALL_REQUEST_CODE);
        }
//        else if(ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED){
//            ActivityCompat.requestPermissions(this, new String[] {permissions[3]}, ALL_REQUEST_CODE);
//        }


        img = findViewById(R.id.imageView);
    }


    private void askPermission(String permission, int requestCode) {
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED){
            //We don't have permission
            ActivityCompat.requestPermissions(this, new String[] {permission}, requestCode);
        } else {
            //We have permission already granted
//            Toast.makeText(this, "Permission already granted", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case CAMERA_REQUEST_CODE:
                if (grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Camera Permission Granted", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Camera Permission Denied", Toast.LENGTH_SHORT).show();
                }
            case READ_REQUEST_CODE:
                if (grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Read Storage Permission Granted", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Read Storage Permission Denied", Toast.LENGTH_SHORT).show();
                }
            case WRITE_REQUEST_CODE:
                if (grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Write Storage Permission Granted", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Write Storage Permission Denied", Toast.LENGTH_SHORT).show();
                }
            case INTERNET_REQUEST_CODE:
                if (grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Internet Permission Granted", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Internet Permission Denied", Toast.LENGTH_SHORT).show();
                }

        }
    }

    public void openCamera(View v){
        int REQUEST_CODE = 99;
        int preference = ScanConstants.OPEN_CAMERA;
        Intent intent = new Intent(this, ScanActivity.class);
        intent.putExtra(ScanConstants.OPEN_INTENT_PREFERENCE, preference);
        startActivityForResult(intent, REQUEST_CODE);
    }

    public void openGallery(View v){
        Log.d("TAG", "openGallery started");
        int REQUEST_CODE = 99;
        int preference = ScanConstants.OPEN_MEDIA;
        Intent intent = new Intent(this, ScanActivity.class);
        intent.putExtra(ScanConstants.OPEN_INTENT_PREFERENCE, preference);
        startActivityForResult(intent, REQUEST_CODE);
        Log.d("TAG", "openGallery finished");
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 99 && resultCode == Activity.RESULT_OK) {
            Uri uri = data.getExtras().getParcelable(ScanConstants.SCANNED_RESULT);
            if(ratioChecker(uri)){
                PhotoScanInBack photoScanInBack = new PhotoScanInBack();
                photoScanInBack.execute(uri);
            }
        }
    }

    public boolean ratioChecker (Uri uri) {
        Bitmap bitmap = null;
        try {
            bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
        } catch (IOException e) {
            e.printStackTrace();
        }

        float h = bitmap.getHeight();
        float w = bitmap.getWidth();
        float ratio = h/w;
        Log.d("ratio is ", String.valueOf(ratio));
        builder = new AlertDialog.Builder(this);
//        if(ratio<2.1 || ratio >2.7) {
//        if(ratio<2.4 || ratio >3.5) {
//            builder.setMessage("Image is not scanned properly")
//                    .setCancelable(false)
//                    .setPositiveButton("Try Again", new DialogInterface.OnClickListener() {
//                        public void onClick(DialogInterface dialog, int id) {
////                                    finish();
//                            Toast.makeText(getApplicationContext(),"Crop the image properly and in proper orientation",
//                                    Toast.LENGTH_SHORT).show();
//                        }
//                    });
//            //Creating dialog box
//            AlertDialog alert = builder.create();
//            //Setting the title manually
//            alert.setTitle("Alert");
//            alert.show();
//            return false;
//        }
//        else {
            img.setImageBitmap(bitmap);
            return true;
//        }

    }

    class PhotoScanInBack extends AsyncTask<Uri, Void, String> {

        ProgressDialog progressDialog;
        @Override
        protected void onPreExecute() {
            progressDialog = new ProgressDialog(MainActivity.this);
            progressDialog.setTitle("Dectection in Progress....");
            progressDialog.setMessage("Loading... \nPlease Wait");
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        @Override
        protected String doInBackground(Uri... params) {
            Uri uri = params[0];
            Bitmap bitmap = null;
            try {
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
            } catch (IOException e) {
                e.printStackTrace();
            }

            //to check the right rotation of image or rotation is required
//            bitmap = rotationChecker(bitmap);

            imageHeight = bitmap.getHeight();
            imageWidth = bitmap.getWidth();

            Vector<qrCodeWithLocation> qrCodes = qrFinder(bitmap);

            //Image classifier will be here
            template1(qrCodes);

            getContentResolver().delete(uri, null, null);

            return string;
        }

        @Override
        protected void onProgressUpdate(Void... aVoid) {
            super.onProgressUpdate(aVoid);
        }

        @Override
        protected void onPostExecute(String result) {
            progressDialog.hide();
            //starts qrActivity
            Intent intent = new Intent(MainActivity.this, qrActivity.class);
            intent.putExtra("text",string);
            startActivity(intent);

        }
    }

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

        Bitmap cropBitmap = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth(), bitmap.getHeight()/2, false);

        final List<Classifier.Recognition> results = classifier.recognizeImage(cropBitmap);

        for(int i=0; i<results.size(); i++) {
            Log.d("Title is ", results.get(i).getTitle());
            Log.d("Id is ", results.get(i).getId());
            Log.d("Confidence is ", results.get(i).getId());
        }
        return bitmap;

    }

    public Vector<qrCodeWithLocation> qrFinder(Bitmap bitmap) {

        BarcodeDetector barcodeDetector = new BarcodeDetector.Builder(this).setBarcodeFormats(Barcode.QR_CODE).build();

//        Log.d("width", String.valueOf(imageWidth));
//        Log.d("height", String.valueOf(imageHeight));

        //increment i by 10% of width on each iteration
        //increment j by 10% of width on each iteration
        int addend = imageWidth/10;

        //window size will be of length = 10% of height
        int windowLength = imageHeight/10;

        Vector<qrCodeWithLocation> qrCodes = new Vector<>();

        Vector<Barcode> v = new Vector<>();
        Frame myFrame;
        SparseArray<Barcode> barcodes;

        long startTime = System.currentTimeMillis();

        int width = imageWidth;
        int height = imageHeight;
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

   private void template1 (Vector<qrCodeWithLocation> qrCodes) {

        Vector<varQRCode> varQRCodes = if_matched_with_vaccine_page_template();

       //For checking if childID found then only check vaccines
       String childID = childIDFinder(qrCodes, new vector2(0,0), new vector2(imageWidth, imageHeight/3));
       if (varQRCodes !=null) {

           Log.d("Awesome is", " awesome");
           child kid = new child(childID);

           for (qrCodeWithLocation qr: qrCodes) {
                for(varQRCode vqrc: varQRCodes) {
                    checkVaccine(qr, vqrc.name, vqrc.startingCoordinates, vqrc.endingCoordinates);
                }

//               checkVaccine(qr, "BCG", new vector2(imageWidth / 14, (int) (imageHeight / 2.7)));
//               checkVaccine(qr, "OPV-0", new vector2((int) (imageWidth / 3.4), (int) (imageHeight / 2.7)));
//               checkVaccine(qr, "Hepatitis B-0", new vector2((int) (imageWidth / 1.94), (int) (imageHeight / 2.7)));
//
//               checkVaccine(qr, "OPV-1", new vector2(imageWidth / 14, (int) (imageHeight / 2.15)));
//               checkVaccine(qr, "OPV-2", new vector2((int) (imageWidth / 3.4), (int) (imageHeight / 2.15)));
//               checkVaccine(qr, "OPV-3", new vector2((int) (imageWidth / 1.94), (int) (imageHeight / 2.15)));
//
//               checkVaccine(qr, "DPT-1", new vector2(imageWidth / 14, (int) (imageHeight / 1.78)));
//               checkVaccine(qr, "DPT-2", new vector2((int) (imageWidth / 3.4), (int) (imageHeight / 1.78)));
//               checkVaccine(qr, "DPT-3", new vector2((int) (imageWidth / 1.94), (int) (imageHeight / 1.78)));
//               checkVaccine(qr, "Measles", new vector2((int) (imageWidth / 1.36), (int) (imageHeight / 1.78)));
//
//               checkVaccine(qr, "Hepatitis B-1", new vector2(imageWidth / 14, (int) (imageHeight / 1.536)));
//               checkVaccine(qr, "Hepatitis B-2", new vector2((int) (imageWidth / 3.4), (int) (imageHeight / 1.536)));
//               checkVaccine(qr, "Hepatitis B-3", new vector2((int) (imageWidth / 1.94), (int) (imageHeight / 1.536)));
//               checkVaccine(qr, "Vitamin A", new vector2((int) (imageWidth / 1.36), (int) (imageHeight / 1.536)));
//
//               checkVaccine(qr, "DPT Booster", new vector2((int) (imageWidth / 22.3), (int) (imageHeight / 1.3)));
//               checkVaccine(qr, "Polio Booster", new vector2(imageWidth / 4, (int) (imageHeight / 1.3)));
//               checkVaccine(qr, "Vitamin A 16 months", new vector2((int) (imageWidth / 1.94), (int) (imageHeight / 1.3)));
//               checkVaccine(qr, "Vitamin A 24 months", new vector2((int) (imageWidth / 1.38), (int) (imageHeight / 1.3)));
//
//               checkVaccine(qr, "Vitamin A 30 months", new vector2((int) (imageWidth / 22.3), (int) (imageHeight / 1.15)));
//               checkVaccine(qr, "Vitamin A 36 months", new vector2(imageWidth / 4, (int) (imageHeight / 1.15)));
           }
       }

   }

    public Document parseXML(InputSource source) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(false);
            dbf.setValidating(false);
            DocumentBuilder db = dbf.newDocumentBuilder();
            return db.parse(source);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

   private Vector<varQRCode> if_matched_with_vaccine_page_template () {
        Log.d("here", "here");
       Vector<varQRCode> varQRCodes = new Vector<>();
       DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
       try {
           DocumentBuilder builder = factory.newDocumentBuilder();
//           FileInputStream fis = new FileInputStream("//android_asset/vaccinePageTemplate.xml");
//           File file = new File("");
           InputStream XmlFileInputStream = getResources().openRawResource(R.raw.vaccine_page_template);
//           InputStream inputStream= new FileInputStream(file);
           Reader reader = new InputStreamReader(XmlFileInputStream,"UTF-8");
           InputSource is = new InputSource(reader);
           Document doc = builder.parse(is);

           Element root = doc.getDocumentElement();
           System.out.println(root.getNodeName());

           NodeList nodeL = doc.getElementsByTagName("imageHeight");
           Node n = nodeL.item(0);
           Element e = (Element) n;
           String h = e.getTextContent();
           float height_in_pixels = Float.parseFloat(h);
           nodeL = doc.getElementsByTagName("imageWidth");
           n = nodeL.item(0);
           e = (Element) n;
           String w = e.getTextContent();
           float width_in_pixels = Float.parseFloat(w);


           NodeList nodeList = doc.getElementsByTagName("varQRcode");
           Log.d("Lengthofxml", String.valueOf(nodeList.getLength()));
           for(int i=0; i<nodeList.getLength(); i++) {
               Node node = nodeList.item(i);

               if(node.getNodeType()==Node.ELEMENT_NODE) {

                   Element element = (Element) node;
                   String name = element.getAttribute("varname");
                   Log.d("Varname", name);

                   Log.d("start","");
                   Element start = (Element) element.getElementsByTagName("start").item(0);
                   Log.d("", start.getElementsByTagName("x").item(0).getTextContent());
                   Log.d("", start.getElementsByTagName("y").item(0).getTextContent());

                   String x = start.getElementsByTagName("x").item(0).getTextContent();
                   String y = start.getElementsByTagName("y").item(0).getTextContent();
                   float x_in_pixels = Float.parseFloat(x);
                   float y_in_pixels = Float.parseFloat(y);
                   Log.d("xfloatstart", String.valueOf(imageWidth*x_in_pixels/width_in_pixels));
                   Log.d("yfloatstart", String.valueOf(imageHeight*y_in_pixels/height_in_pixels));
                   vector2 startCoord = new vector2(imageWidth*x_in_pixels/width_in_pixels, imageHeight*y_in_pixels/height_in_pixels);

                   Log.d("","end");
                   Element end = (Element) element.getElementsByTagName("end").item(0);
                   Log.d("", end.getElementsByTagName("x").item(0).getTextContent());
                   Log.d("", end.getElementsByTagName("y").item(0).getTextContent());

                   x = end.getElementsByTagName("x").item(0).getTextContent();
                   y = end.getElementsByTagName("y").item(0).getTextContent();
                   x_in_pixels = Float.parseFloat(x);
                   y_in_pixels = Float.parseFloat(y);
                   Log.d("xfloatend", String.valueOf(imageWidth*x_in_pixels/width_in_pixels));
                   Log.d("yfloatend", String.valueOf(imageHeight*y_in_pixels/height_in_pixels));
                   vector2 endCoord = new vector2(imageWidth*x_in_pixels/width_in_pixels, imageHeight*y_in_pixels/height_in_pixels);
                   Log.d("qrcodeName",name);
                   varQRCodes.add(new varQRCode(name, startCoord, endCoord));

               }
           }


       } catch (ParserConfigurationException e) {
           // TODO Auto-generated catch block
           e.printStackTrace();
           return null;
       } catch (SAXException e) {
           // TODO Auto-generated catch block
           e.printStackTrace();
           return null;
       } catch (IOException e) {
           // TODO Auto-generated catch block
           e.printStackTrace();
           return null;
       }

       return varQRCodes;

   }

    //TODO
    LuminanceSource bils;
    HybridBinarizer hb;
    BitMatrix bm;
    //image sent to this method should be of proper size otherwise getBlackMatrix will not work because of large size
    public vector2 loc(Bitmap image, int a, int b){

        int[] intArray = new int[imageHeight/10*imageHeight/10];
        image.getPixels(intArray, 0, image.getWidth(), 0, 0, imageHeight/10, imageHeight/10);//        byte[] array = BitmapToArray(image);
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

    //square window of length = imgHeight/7

    public String childIDFinder(Vector<qrCodeWithLocation> qrCodes, vector2 startingCoordinates, vector2 endingCoordinates) {
        String childID = "";
        float x = startingCoordinates.x;
        float xPlusDelta = endingCoordinates.x;

        float y = startingCoordinates.x;
        float yPlusDelta = endingCoordinates.y;
        for(int i=0; i<qrCodes.size(); i++) {
            qrCodeWithLocation qr = qrCodes.get(i);
            if(x < qr.coordinates.x && qr.coordinates.x < xPlusDelta){
                Log.d("child code", "x correct");
                if(y < qr.coordinates.y && qr.coordinates.y < yPlusDelta){
                    Log.d("child code", "y correct");
                    string = "Child " + qr.barcode.displayValue +"\n";
                    childID = qr.barcode.displayValue;
                    return childID;
                }
            }
        }

        return childID;
    }

    public void checkVaccine(qrCodeWithLocation qr, String vaccineName, vector2 startingCoordinates, vector2 endingCoordinates) {
        float x = startingCoordinates.x;
        float y = startingCoordinates.y;
        float xPlusDelta = endingCoordinates.x;//x + imageHeight/15;
        float yPlusDelta = endingCoordinates.y;//y + imageHeight/15;
        if(x < qr.coordinates.x && qr.coordinates.x < xPlusDelta){
            Log.d(vaccineName + " code", "x correct");
            if(y < qr.coordinates.y && qr.coordinates.y < yPlusDelta){
                Log.d(vaccineName + " code", "y correct");
                Log.d(vaccineName, String.valueOf(qr.coordinates.x) + " " + String.valueOf(qr.coordinates.y));
                Log.d(vaccineName, qr.barcode.displayValue);
                string = string + "- "  + vaccineName + " vaccine\n";
            }
        }
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


}
