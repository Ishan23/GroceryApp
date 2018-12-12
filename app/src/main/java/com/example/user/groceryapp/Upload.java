package com.example.user.groceryapp;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobileconnectors.s3.transferutility.*;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.util.IOUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Upload extends AppCompatActivity {

    private Boolean Camera_CHECK = false;

    private int PICK_IMAGE_REQUEST = 1;
    private int CAMERA_IMAGE_REQUEST = 20;
    private ImageView imageView;
    private Bitmap bitmap;

    private String mCurrentPhotoPath;
    private Bitmap mImageBitmap;


    private Uri fileUri = null;

    private SharedPreferences pref;
    private SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        pref = getSharedPreferences("preferences", MODE_PRIVATE);
        editor = pref.edit();

        //New code
        setContentView(R.layout.activity_upload_image);
        imageView = (ImageView) findViewById(R.id.imageView3);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //Creating the instance of PopupMenu
                PopupMenu popup = new PopupMenu(Upload.this, imageView);
                //Inflating the Popup using xml file
                popup.getMenuInflater().inflate(R.menu.upload_menu, popup.getMenu());

                //registering popup with OnMenuItemClickListener
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    public boolean onMenuItemClick(MenuItem item) {

                        if (item.getItemId()==R.id.one)
                        {
                            getImage_byGallery();
                        }
                        else if (item.getItemId()==R.id.two)
                        {
                            getImage_byCamera();
                        }

                        return true;
                    }
                });

                popup.show();//showing popup menu

            }
        });


        AWSMobileClient.getInstance().initialize(this).execute();

        Button button_upload = (Button)findViewById(R.id.button_upload);
        button_upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Log.d("exec", "onClick: ");
                //Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                //startActivityForResult(cameraIntent, CAMERA_PIC_REQUEST);

                if (isStoragePermissionGranted()) {
                    uploadWithTransferUtility(fileUri);
                }
                else {
                    Toast.makeText(getApplicationContext(),"You need to grant storage permission",Toast.LENGTH_SHORT).show();
                }

            }
        });
    }

    public void getImage_byGallery()
    {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);

        if (isStoragePermissionGranted()) {
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
        }
        else {
            Toast.makeText(getApplicationContext(),"You need to grant storage permission",Toast.LENGTH_SHORT).show();
        }
    }
    public void getImage_byCamera()
    {
        if (isCameraPermissionGranted()) {


            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

            if (cameraIntent.resolveActivity(getPackageManager()) != null) {

                // Create the File where the photo should go
                File photoFile = null;
                try {
                    photoFile = createImageFile();
                } catch (IOException ex) {
                    // Error occurred while creating the File
                    Log.i("Error", "IOException");
                }
                // Continue only if the File was successfully created
                if (photoFile != null) {
                    cameraIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    Uri photoURI = FileProvider.getUriForFile(getApplicationContext(), getApplicationContext().getPackageName() + ".my.package.name.provider", photoFile);
                    cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                    startActivityForResult(cameraIntent, CAMERA_IMAGE_REQUEST);
                }
            }



        }
        else {
            Toast.makeText(getApplicationContext(),"You need to grant camera permission",Toast.LENGTH_SHORT).show();
        }

    }



    public  boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.v("External Storage","Permission is granted");
                return true;
            } else {

                Log.v("External Storage","Permission is revoked");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                return false;
            }
        }
        else { //permission is automatically granted on sdk<23 upon installation
            Log.v("External Storage","Permission is granted");
            return true;
        }
    }

    public  boolean isCameraPermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.v("Camera","Permission is granted");
                return true;
            } else {

                Log.v("Camera","Permission is revoked");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
                return false;
            }
        }
        else { //permission is automatically granted on sdk<23 upon installation
            Log.v("Camera","Permission is granted");
            return true;
        }
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        float height = imageView.getHeight();
        float width = imageView.getWidth();
        imageView.setMaxHeight((int) (2*height));
        imageView.setMaxWidth((int) (2*width));


        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {

            Camera_CHECK = false;

            try {
                fileUri = data.getData();
                //Getting the Bitmap from Gallery
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), fileUri);
                imageView.setImageBitmap(Bitmap.createScaledBitmap(bitmap, ((int) width), ((int) height), false));

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if(CAMERA_IMAGE_REQUEST == requestCode && resultCode == RESULT_OK){

            Log.d("CAMERA CHECK","7");

            Camera_CHECK = true;

            try {
                mImageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), Uri.parse(mCurrentPhotoPath));
                imageView.setImageBitmap(mImageBitmap);
                Log.d("CAMERA CHECK","8");
            } catch (IOException e) {
                e.printStackTrace();
            }


        }

    }

    private void storeCameraPhotoInSDCard(Bitmap bitmap, String currentDate){
        File outputFile = new File(Environment.getExternalStorageDirectory(), "photo_" + currentDate + ".jpg");
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (FileNotFoundException e) {
            Log.e("ERROR","-------------------------------------------------------------");
            e.printStackTrace();
        } catch (IOException e) {
            Log.e("ERROR","-------------------------------------------------------------");
            e.printStackTrace();
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "Grocery_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  // prefix
                ".jpg",         // suffix
                storageDir      // directory
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = "file:" + image.getAbsolutePath();
        return image;
    }

    public String getImagePath(Uri uri){
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        cursor.moveToFirst();
        String document_id = cursor.getString(0);
        document_id = document_id.substring(document_id.lastIndexOf(":")+1);
        cursor.close();
        cursor = getContentResolver().query(
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                null, MediaStore.Images.Media._ID + " = ? ", new String[]{document_id}, null);
        cursor.moveToFirst();
        String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
        cursor.close();

        return path;
    }

    public void uploadWithTransferUtility(Uri uri) {


        if(Camera_CHECK==true)
        {
            uri = Uri.parse(mCurrentPhotoPath);
        }

        // KEY and SECRET are gotten when we create an IAM user above
        String KEY="";
        String SECRET="";

        BasicAWSCredentials credentials = new BasicAWSCredentials(KEY, SECRET);
        AmazonS3Client s3Client = new AmazonS3Client(credentials);

        TransferUtility transferUtility =
                TransferUtility.builder()
                        .context(getApplicationContext())
                        .awsConfiguration(AWSMobileClient.getInstance().getConfiguration())
                        .s3Client(s3Client)
                        .build();

        String path = "";
        if(Camera_CHECK==true)
        {
            path = uri.toString().substring(5);
        }
        else{
            path = getImagePath(uri);
        }

        String fileName = pref.getString("user",null);

        TransferObserver uploadObserver = transferUtility.upload("billsupload",
                fileName,
                        new File(path));
                        //new File(uri.getPath()));

        uploadObserver.setTransferListener(new TransferListener() {

            @Override
            public void onStateChanged(int id, TransferState state) {
                if (TransferState.COMPLETED == state) {
                    // Handle a completed download.
                }
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                float percentDonef = ((float)bytesCurrent/(float)bytesTotal) * 100;
                int percentDone = (int)percentDonef;
            }

            @Override
            public void onError(int id, Exception ex) {
                // Handle errors
            }

        });

        // If your upload does not trigger the onStateChanged method inside your
        // TransferListener, you can directly check the transfer state as shown here.
        if (TransferState.COMPLETED == uploadObserver.getState()) {
            // Handle a completed upload.
            Log.d("DONE","COMPLETED");

        }
        Toast.makeText(this,"Uploaded succesfully",Toast.LENGTH_SHORT).show();

    }


   }
