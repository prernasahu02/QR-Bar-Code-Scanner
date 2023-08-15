package com.example.qrbarcodescanner;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.List;

public class HomeActivity extends AppCompatActivity {

    private MaterialButton btn_camera, btn_gallery, btn_scan;
    ImageView iv_image;
    TextView tv_result;

    private String[] cameraPermission;
    private String[] storagePermission;

    private Uri imageUri = null;

    public static final int cameraRequestCode = 100;
    public static final int storageRequestCode = 101;

    private static final String TAG = "MAIN_TAG";

    private BarcodeScannerOptions barcodeScannerOptions;
    private BarcodeScanner barcodeScanner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        btn_camera = findViewById(R.id.btn_camera);
        btn_gallery = findViewById(R.id.btn_gallery);
        btn_scan = findViewById(R.id.btn_scanner);
        iv_image = findViewById(R.id.iv_image);
        tv_result = findViewById(R.id.tv_result);

        cameraPermission = new String[]{Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermission = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

        barcodeScannerOptions = new BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS).build();

        barcodeScanner = BarcodeScanning.getClient(barcodeScannerOptions);


        btn_camera.setOnClickListener(view -> {
            if(checkCameraPermission())
                cameraShot();
            else
                requestCameraPermission();
        });


        btn_gallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(checkStoragePermission())
                    gallerySelection();
                else
                    requestStoragePermission();
            }
        });

        btn_scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(imageUri == null){
                    Toast.makeText(HomeActivity.this, "Select any Image", Toast.LENGTH_SHORT).show();
                }
                else{
                    detectResultFromImage();
                }
            }
        });
    }

    private void gallerySelection(){
        Intent i = new Intent(Intent.ACTION_PICK);
        i.setType("image/*");
        gallerySelectionLauncher.launch(i);
    }

    private final ActivityResultLauncher<Intent> gallerySelectionLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            if(result.getResultCode() == Activity.RESULT_OK){
                Intent data = result.getData();
                imageUri = data.getData();
                Log.d(TAG,"onActivityResult:imageUri" +imageUri);
                iv_image.setImageURI(imageUri);
            }
            else Toast.makeText(HomeActivity.this, "Gallery Selection Cancelled", Toast.LENGTH_SHORT).show();
        }
    });

    private void cameraShot(){

        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Images.Media.TITLE,"Sample Title");
        contentValues.put(MediaStore.Images.Media.DESCRIPTION,"Sample Image Description");

        imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,contentValues);

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT,imageUri);

        cameraShotLauncher.launch(intent);

    }

    private final ActivityResultLauncher<Intent > cameraShotLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
           if (result.getResultCode()==Activity.RESULT_OK){
               Intent data = result.getData();
               Log.d(TAG,"onActivityResult : imageUri" +imageUri);
               iv_image.setImageURI(imageUri);
           }
           else Toast.makeText(HomeActivity.this,"Cancelled",Toast.LENGTH_SHORT).show();
        }
    });

    private  boolean checkStoragePermission(){
        boolean result = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)== PackageManager.PERMISSION_GRANTED;
        return result;
    }

    private void requestStoragePermission(){
        ActivityCompat.requestPermissions(this,storagePermission,storageRequestCode);
    }

    private boolean checkCameraPermission(){
        boolean resultCamera = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED;

        boolean resultStorage = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)== PackageManager.PERMISSION_GRANTED;
         return resultCamera && resultStorage;
    }

    private void requestCameraPermission(){
        ActivityCompat.requestPermissions(this,cameraPermission,cameraRequestCode);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch(requestCode){
            case cameraRequestCode:{
                if(grantResults.length>0){
                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean storageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                if (cameraAccepted&&storageAccepted){
                    Toast.makeText(this,"Camera &  Storage permissions are granted",Toast.LENGTH_SHORT).show();
                    cameraShot();
                }
                else
                    Toast.makeText(this,"Camera &  Storage permissions are required",Toast.LENGTH_SHORT).show();
                }
            }
            break;
            case storageRequestCode:{
                if(grantResults.length>0){
                    boolean storageAccepted = grantResults[0]==PackageManager.PERMISSION_GRANTED;
                    if(storageAccepted){
                        Toast.makeText(this,"Storage permissions are granted",Toast.LENGTH_SHORT).show();
                        gallerySelection();
                    }
                    else
                        Toast.makeText(this,"Storage permission is required",Toast.LENGTH_SHORT).show();
                }
            }
            break;
        }
    }


    private void detectResultFromImage() {
        try{
            InputImage inputImage = InputImage.fromFilePath(this,imageUri);
            Task<List<Barcode>> barcodeResult = barcodeScanner.process(inputImage).addOnSuccessListener(new OnSuccessListener<List<Barcode>>() {
                        @Override
                        public void onSuccess(List<Barcode> barcodes) {
                            extractBarCodeQRCodeInfo(barcodes);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(HomeActivity.this,"Failed Scanning due to"+e.getMessage(),Toast.LENGTH_SHORT).show();
                        }
                    });
        } catch (Exception e)
        {
            Toast.makeText(HomeActivity.this,"Failed due to"+e.getMessage(),Toast.LENGTH_SHORT).show();
        }
    }

    private void extractBarCodeQRCodeInfo(List<Barcode> barcodes) {
        for(Barcode barcode : barcodes){
            Rect bound = barcode.getBoundingBox();
            Point[] corners = barcode.getCornerPoints();

            String rawValue = barcode.getRawValue();
            Log.d(TAG,"extractBarCodeQRCodeInfo: rawValue"+rawValue);

            int valueType = barcode.getValueType();

            switch(valueType){
                case Barcode.TYPE_WIFI:{
                    Barcode.WiFi typeWiFi = barcode.getWifi();
                    String ssid = ""+typeWiFi.getSsid();
                    String password = ""+typeWiFi.getPassword();
                    String encryptionType = "" +typeWiFi.getEncryptionType();

                    Log.d(TAG,"extractBarCodeQRCodeInfo: TYPE_WIFI");
                    Log.d(TAG,"extractBarCodeQRCodeInfo: ssid"+ssid);
                    Log.d(TAG,"extractBarCodeQRCodeInfo: password"+password);
                    Log.d(TAG,"extractBarCodeQRCodeInfo: encryptionType"+encryptionType);

                    tv_result.setText("TYPE: TYPE_WIFI \nssid: "+ssid+"\npassword: "+password+"\nencryptionType : "+encryptionType+"\nrawValue: "+rawValue);
                }
                break;
                case Barcode.TYPE_URL:{
                    Barcode.UrlBookmark typeUrl = barcode.getUrl();

                    String title = ""+typeUrl.getTitle();
                    String url = ""+typeUrl.getUrl();

                    Log.d(TAG,"extractBarCodeQRCodeInfo: TYPE_URL");
                    Log.d(TAG,"extractBarCodeQRCodeInfo: title: "+title);
                    Log.d(TAG,"extractBarCodeQRCodeInfo: url: "+url);

                    tv_result.setText("TYPE: TYPE_URL \ntitle: "+title+"\nurl: "+url+"\nrawValue: "+rawValue);
                }
                break;
                case Barcode.TYPE_EMAIL:{
                    Barcode.Email typeEmail = barcode.getEmail();

                    String address = ""+typeEmail.getAddress();
                    String body = ""+typeEmail.getBody();
                    String subject = ""+typeEmail.getSubject();

                    Log.d(TAG,"extractBarCodeQRCodeInfo: TYPE_EMAIL");                    Log.d(TAG,"extractBarCodeQRCodeInfo: TYPE_EMAIL");
                    Log.d(TAG,"extractBarCodeQRCodeInfo: address: "+address);
                    Log.d(TAG,"extractBarCodeQRCodeInfo: body : "+body);
                    Log.d(TAG,"extractBarCodeQRCodeInfo: subject: "+subject);

                    tv_result.setText("TYPE: TYPE_EMAIL \naddress: "+address+"\nbody: "+body+"\nsubject: "+subject+"\nrawValue: "+rawValue);
                }
                break;
                case Barcode.TYPE_CONTACT_INFO:{

                    Barcode.ContactInfo typeContact = barcode.getContactInfo();
                    String title = ""+typeContact.getTitle();
                    String organizer = ""+typeContact.getOrganization();
                    String name = ""+typeContact.getName().getFirst()+" "+typeContact.getName().getLast();
                    String phones = ""+typeContact.getPhones().get(0).getNumber();

                    Log.d(TAG,"extractBarCodeQRCodeInfo: TYPE_CONTACT_INFO");
                    Log.d(TAG,"extractBarCodeQRCodeInfo: title: "+title);
                    Log.d(TAG,"extractBarCodeQRCodeInfo: organizer: "+organizer);
                    Log.d(TAG,"extractBarCodeQRCodeInfo: name: "+name);
                    Log.d(TAG,"extractBarCodeQRCodeInfo: phones: "+phones);

                    tv_result.setText("TYPE: TYPE_CONTACT_INFO \ntitle: "+title+"\norganizer: "+organizer+"\nname: "+name+"\nphones: "+phones+"\nrawValue: "+rawValue);
                }
                break;
                default:
                    tv_result.setText("rawValue: "+rawValue);
            }
        }
    }
}