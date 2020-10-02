package com.amanpandey.photoshooter;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Random;

public class PhotoShooter extends AppCompatActivity {

    //Variables
    public Button btntakephoto, btnsave, btnshare, btnvid,btnvidshare;
    public ImageView ivdisplayphoto;
    public SeekBar skbarChangeColor;
    private ColorMatrix colorMatrix; //this will allow us to apply an color matrix filter on an image
    private ColorMatrixColorFilter filter;
    private Paint paint; //Appling colors
    private Canvas cv; //Area for placing colors
    private File photofile; // external storage to store images
    private  int TAKENPHOTO = 0;
    Bitmap photo,canvasBitmap;
    public int progress;
    private Uri fileUri;

    public static final int MEDIA_TYPE_VIDEO = 2;
    public static final int CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE = 200;
    public static PhotoShooter ActivityContext = null;
    public static TextView output;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.photoshooter);

        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());

        //Hooks
        ActivityContext = this;
        btntakephoto = findViewById(R.id.btn_takephoto);
        btnsave = findViewById(R.id.btn_save);
        btnshare = findViewById(R.id.btn_share);
        btnvid = findViewById(R.id.btn_video);
        btnvidshare = findViewById(R.id.btn_vidshare);
        ivdisplayphoto = findViewById(R.id.iv_displayphoto);
        skbarChangeColor = findViewById(R.id.skbarChangeColor);
        skbarChangeColor.setMax(100);
        skbarChangeColor.setKeyProgressIncrement(1);
        skbarChangeColor.setProgress(50);
        skbarChangeColor.setVisibility(View.INVISIBLE);

        output = (TextView) findViewById(R.id.output);

        // Instanciating variables



        //Setting onClick listeners on Buttons
        btntakephoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //Get external Storage public directory
                File photostorage = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                photofile = new File(photostorage,(System.currentTimeMillis()) + ".jpg");


                Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE); // intentto start camera

                startActivityForResult(i,TAKENPHOTO);
            }
        });

        btnsave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ivdisplayphoto.setDrawingCacheEnabled(true);
                ivdisplayphoto.buildDrawingCache();
                Bitmap bitmap = ivdisplayphoto.getDrawingCache();
                Random gen = new Random();
                int n = 10000;
                n = gen.nextInt(n);

                String fotoname = "Photo-"+n+".jpg";
                saveImageBitmap(bitmap,fotoname);
            }

        });

        btnshare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Getting image from imageview
                BitmapDrawable bitmapDrawable = (BitmapDrawable)ivdisplayphoto.getDrawable();
                Bitmap bitmap = bitmapDrawable.getBitmap();
                File[] cache = new File[0];
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                    cache = getApplicationContext().getExternalCacheDirs();
                }
                File sharefile = new File(String.valueOf(cache),"toshare.png");

                try {
                    //Provide out put stream from outputting an image
                    FileOutputStream out = new FileOutputStream(sharefile);
                    bitmap.compress(Bitmap.CompressFormat.PNG,100,out);
                    out.flush();
                    out.close();

                }catch (Exception e){

                }

                //Need to share to it intent so that user can share this picture
                Intent share = new Intent(Intent.ACTION_SEND);
                share.setType("image/*");
                share.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + sharefile));

                try {
                    startActivity(Intent.createChooser(share, "share via"));
                }catch (Exception e){

                }
            }
        });

        btnvid.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                fileUri = getOutputMediaFileUri(MEDIA_TYPE_VIDEO);

                intent.putExtra(MediaStore.EXTRA_OUTPUT,fileUri);

                intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY,1);

                startActivityForResult(intent,CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE);


            }
        });


        btnvidshare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String type = "video/*";
                String file = "/myVideo.mp4";
                String mediaPath = Environment.getExternalStorageDirectory() + file;
                String caption  = "<< media captions >>";

                CreatePhotoShooter(type,mediaPath,caption);
            }
        });

        skbarChangeColor.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                applyColorFilter(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }

            private void applyColorFilter(int progress) {
                colorMatrix = new ColorMatrix();
                paint = new Paint();
                colorMatrix.setSaturation(progress/(float)40);
                filter = new ColorMatrixColorFilter(colorMatrix);
                paint.setColorFilter(filter);
                canvasBitmap = Bitmap.createBitmap(photo.getWidth(),photo.getHeight(),Bitmap.Config.ARGB_8888);
                cv = new Canvas(canvasBitmap);
                cv.drawBitmap(photo,0,0,paint);
                ivdisplayphoto.setImageBitmap(canvasBitmap);
            }

        });


    }

    //Trying to save the image
    public boolean isStoragePermissionGranted() {
        String TAG = "Storage Permission";
        if (Build.VERSION.SDK_INT >= 23) {
            if (this.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                Log.v(TAG, "Permission is granted");
                return true;
            } else {
                Log.v(TAG, "Permission is revoked");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        }
        else { //permission is automatically granted on sdk<23 upon installation
            Log.v(TAG,"Permission is granted");
            return true;
        }
    }

    public void saveImageBitmap(Bitmap image_bitmap, String image_name) {
        String root = Environment.getExternalStorageDirectory().toString();
        if (isStoragePermissionGranted()) { // check or ask permission
            File myDir = new File(root, "/saved_images");
            if (!myDir.exists()) {
                myDir.mkdirs();
            }
            String fname = "Image-" + image_name + ".jpg";
            File file = new File(myDir, fname);
            if (file.exists()) {
                file.delete();
            }
            try {
                file.createNewFile(); // if file already exists will do nothing
                FileOutputStream out = new FileOutputStream(file);
                image_bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
                out.flush();
                out.close();
                Toast.makeText(ActivityContext, "Photo has been saved :-)", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(ActivityContext, "Your Photo can't be saved", Toast.LENGTH_SHORT).show();
            }

            MediaScannerConnection.scanFile(this, new String[]{file.toString()}, new String[]{file.getName()}, null);
        }
    }

    private void CreatePhotoShooter(String type, String mediaPath, String caption) {

      Intent shareVid = new Intent(Intent.ACTION_SEND);
      shareVid.setType(type);
      File media = new File(mediaPath);
      Uri uri = Uri.fromFile(media);
      shareVid.putExtra(Intent.EXTRA_STREAM,uri);
      shareVid.putExtra(Intent.EXTRA_TEXT,caption);

      startActivity(Intent.createChooser(shareVid, "Share via"));
    }

    private Uri getOutputMediaFileUri(int mediaTypeVideo) {
        return Uri.fromFile(getOutputMediaFile(mediaTypeVideo));
    }

    private File getOutputMediaFile(int mediaTypeVideo) {

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),"MyCameraVideo");
        if(!mediaStorageDir.exists()){

            if(!mediaStorageDir.mkdir()){
                output.setText("Failed to create dirtecory myCameraVideo");
                Toast.makeText(ActivityContext, "Failed to create directory MyCameraVideo", Toast.LENGTH_LONG).show();
                Log.i("MyCameraVideo","Failed to create Directory MyCameraVideo");
                return null;
            }
        }

        java.util.Date date = new java.util.Date();
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(date.getTime());

        File mediaFile;
        if(mediaTypeVideo == MEDIA_TYPE_VIDEO){
            mediaFile = new File(mediaStorageDir.getPath() + File.separator + "VID_" + timeStamp + ".mp4");
        }else{
            return null;
        }

        return mediaFile;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        //To auto generate method stub
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == TAKENPHOTO){

            try {
                photo =(Bitmap) data.getExtras().get("data");
            }catch (NullPointerException e){
                photo = BitmapFactory.decodeFile(photofile.getAbsolutePath());
            }
            if(photo != null){
                ivdisplayphoto.setImageBitmap(photo);
                skbarChangeColor.setVisibility(View.VISIBLE);
            }else{
                Toast.makeText(this, "Oops Can't Get Photo", Toast.LENGTH_LONG).show();
            }
        }

        if(requestCode == CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE){

            if(resultCode == RESULT_OK){
                output.setText("Video File : " + data.getData());

                Toast.makeText(ActivityContext, "Video Saved to :\n" + data.getData(), Toast.LENGTH_LONG).show();
            }else if(requestCode == RESULT_CANCELED){
               output.setText("User cancelled the video capture");
                Toast.makeText(ActivityContext, "User cancelled the video capture", Toast.LENGTH_LONG).show();
            }
        }else{
            output.setText("Video Capture Failed");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu this adds items to the action bar if it is present.
        return super.onCreateOptionsMenu(menu);


    }
}