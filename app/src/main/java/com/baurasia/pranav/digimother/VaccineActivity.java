package com.baurasia.pranav.digimother;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.widget.TextView;

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

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import static java.lang.Math.abs;

public class VaccineActivity extends AppCompatActivity {

    int imageHeight, imageWidth;
    String string = "No Child ID detected";

    public Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vaccine);

//        Bitmap bitmap = this.getIntent().getParcelableExtra("tableBitmap");
//        Uri uri = getImageUri(this, bitmap);
        String uriString = this.getIntent().getStringExtra("uri");
        Uri uri = Uri.parse(uriString);

        PhotoScanInBack photoScanInBack = new PhotoScanInBack();
        photoScanInBack.execute(uri);

    }

    class PhotoScanInBack extends AsyncTask<Uri, Void, String> {

        ProgressDialog progressDialog;
        @Override
        protected void onPreExecute() {
            progressDialog = new ProgressDialog(VaccineActivity.this);
            progressDialog.setTitle("Dectection in Progress....");
            progressDialog.setMessage("Loading... \nPlease Wait");
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        @Override
        protected String doInBackground(Uri... params) {
            Log.d("Background", "operation started");
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

            TextView textView = findViewById(R.id.vaccines);
            textView.setText(string);

        }


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

        Vector<varQRCode> varQRCodesXML = xmlReader();

        //For checking if childID found then only check vaccines
        String childID = childIDFinder(qrCodes, new vector2(0,0), new vector2(imageWidth, imageHeight/3));
        if (varQRCodesXML !=null) {

            child kid = new child(childID);

            for (qrCodeWithLocation qr: qrCodes) {
                for(varQRCode vqrc: varQRCodesXML) {
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

    private Vector<varQRCode> xmlReader () {
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
