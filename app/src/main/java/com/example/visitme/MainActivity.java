package com.example.visitme;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.canhub.cropper.CropImageView;
import com.example.visitme.DRVinterface.LoadMore;
import com.example.visitme.databinding.ActivityMainBinding;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.canhub.cropper.CropImage;
import com.canhub.cropper.CropImageActivity;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    FloatingActionButton fab, CameraButton, GalleryButton,QrButton;
    Animation fabOpen, fabClose, rotateForward, rotateBackward;

    RecyclerView recyclerView;
    private StaticRvAdapter staticRvAdapter;
    MainAdapter mainAdapter;

    List<DynamicRvModel> items = new ArrayList();


    ImageButton imageButton;



    ArrayList<Contact> list;
    private DatabaseReference database;

    //text view variable
    public static TextView scantext;
    private static final  int CAMERA_REQUEST_CODE = 200;
    private static final  int STORAGE_REQUEST_CODE = 400;
    private static final  int IMAGE_PICK_GALLERY_CODE = 1000;
    private static final  int IMAGE_PICK_CAMERA_CODE = 1001;

    String cameraPermission[];
    String storagePermission[];


    Uri image_uri,resultat;

    private FirebaseAuth firebaseAuth;


    boolean isOpen=false; //by default is false
    private ActivityMainBinding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //les categories:
        ArrayList<StaticRvModel>item=new ArrayList<>();
        /*item.add(new StaticRvModel(R.drawable.doctor1, "Santé"));
        item.add(new StaticRvModel(R.drawable.restaurant, "Restaurants"));
        item.add(new StaticRvModel(R.drawable.voyage, "Agence de voyage"));
        item.add(new StaticRvModel(R.drawable.hotel, "Hôtel"));
        item.add(new StaticRvModel(R.drawable.reparation, "Mécanique-maintenance"));
        item.add(new StaticRvModel(R.drawable.immobilier, "Immobilier"));
        item.add(new StaticRvModel(R.drawable.service, "Services à domicile"));
        item.add(new StaticRvModel(R.drawable.fleuriste, "Fleuristes"));
        item.add(new StaticRvModel(R.drawable.traiteur, "Traiteurs"));
        item.add(new StaticRvModel(R.drawable.Patisserie, "Pâtisserie"));
        item.add(new StaticRvModel(R.drawable.Esthetique, "Esthetique"));
        item.add(new StaticRvModel(R.drawable.Transport, "Transport-logistique"));
        item.add(new StaticRvModel(R.drawable.Publinet, "Publinet"));
        item.add(new StaticRvModel(R.drawable.Autres, "Autres"));*/
        imageButton=findViewById(R.id.search_button);
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(MainActivity.this,"search-clicked", Toast.LENGTH_SHORT).show();
            }
        });

        recyclerView = findViewById(R.id.rv_1);
        staticRvAdapter = new StaticRvAdapter(item);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL,false));
        recyclerView.setAdapter(staticRvAdapter);


        items.add(new DynamicRvModel("personne"));
        items.add(new DynamicRvModel("personne"));


        recyclerView =(RecyclerView) findViewById(R.id.rv_2);
        //dynamicRVAdapter = new DynamicRVAdapter(drv ,this,items);
        //drv.setAdapter(dynamicRVAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        firebaseAuth = FirebaseAuth.getInstance();
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        String uid = firebaseUser.getUid();
        FirebaseRecyclerOptions<Contact> options =
                new FirebaseRecyclerOptions.Builder<Contact>()
                        .setQuery(FirebaseDatabase.getInstance().getReference().child("users").child(uid).child("contacts"),Contact.class)
                        .build();

        mainAdapter=new MainAdapter(options);
        recyclerView.setAdapter(mainAdapter);



        //text view

        fab=(FloatingActionButton) findViewById(R.id.fab);
        CameraButton=(FloatingActionButton) findViewById(R.id.fab1);
        GalleryButton=(FloatingActionButton) findViewById(R.id.fab2);
        QrButton=(FloatingActionButton) findViewById(R.id.fab3);




        //animations
        fabOpen = AnimationUtils.loadAnimation(this,R.anim.fab_open);
        fabClose = AnimationUtils.loadAnimation(this,R.anim.fab_close);

        rotateForward=AnimationUtils.loadAnimation(this, R.anim.rotate_forward);
        rotateBackward=AnimationUtils.loadAnimation(this, R.anim.rotate_backward);

        //initialisation permission
        cameraPermission = new String[] {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermission = new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE};

        // set the click listener on the MAIN FAB
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                animateFab();
            }
        });
        CameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                animateFab();
                Toast.makeText(MainActivity.this,"camera clicked", Toast.LENGTH_SHORT).show();
                if (!checkCameraPermission()){
                    //camera permission not allowed, request it
                    requestCameraPermission();
                    
                }
                else{
                    //permission allowed , take picture
                    pickCamera();
                }
            }
        });

        GalleryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                animateFab();
                //Toast.makeText(MainActivity.this,"Galerie clicked", Toast.LENGTH_SHORT).show();
                if (!checkStoragePermission()){
                    //storage permission not allowed, request it
                    requestStoragePermission();
                }
                else{
                    //permission allowed , take picture
                    pickGallery();
                }
            }
        });

        QrButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                animateFab();
                Toast.makeText(MainActivity.this,"QR Code clicked", Toast.LENGTH_SHORT).show();
                startActivity (new Intent(getApplicationContext(),qr.class));
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        mainAdapter.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mainAdapter.startListening();
    }


    private void animateFab(){
        if(isOpen){
            fab.startAnimation(rotateBackward);
            CameraButton.startAnimation(fabClose);
            GalleryButton.startAnimation(fabClose);
            QrButton.startAnimation(fabClose);
            CameraButton.setClickable(false);
            GalleryButton.setClickable(false);
            QrButton.setClickable(false);
            isOpen=false;
        }
        else{
            fab.startAnimation(rotateForward);
            CameraButton.startAnimation(fabOpen);
            GalleryButton.startAnimation(fabOpen);
            QrButton.startAnimation(fabOpen);
            CameraButton.setClickable(true);
            GalleryButton.setClickable(true);
            QrButton.setClickable(true);
            isOpen=true;
        }
    }

    private void pickCamera(){
        // intent to take image from camera, it will also ba save to storage to get high quality image
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE,"NEWPIC"); //title of the picture
        values.put(MediaStore.Images.Media.DESCRIPTION,"Image To Text"); //description
        image_uri  = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT,image_uri);
        startActivityForResult(cameraIntent,IMAGE_PICK_CAMERA_CODE);
        Log.v("------","ouvrir caméra");

    }
    private void requestStoragePermission(){
        ActivityCompat.requestPermissions(this,storagePermission,STORAGE_REQUEST_CODE);

    }

    private  boolean checkStoragePermission(){
        boolean result = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);

        return  result;
    }

    private void  requestCameraPermission(){
        ActivityCompat.requestPermissions(this,cameraPermission,CAMERA_REQUEST_CODE);
    }

    private boolean checkCameraPermission()
    {boolean result = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == (PackageManager.PERMISSION_GRANTED);
        boolean result1 = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
        return result && result1;
    }
    private void pickGallery(){
        //intent to pick image from gallery
        Intent intent = new Intent(Intent.ACTION_PICK);
        //set intent type to image
        intent.setType("image/*");
        startActivityForResult(intent, IMAGE_PICK_GALLERY_CODE);

    }
    //handle permission result
    @Override
    public  void onRequestPermissionsResult (int requestCode, @NonNull String[] permissions, @NonNull int[]  grantResults){
        switch (requestCode){
            case CAMERA_REQUEST_CODE:
                if(grantResults.length > 0){
                    boolean cameraAccepted = grantResults [0] == PackageManager.PERMISSION_GRANTED;
                    boolean writeStorageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if(cameraAccepted && writeStorageAccepted){
                        pickCamera();
                    }
                    else{
                        Toast.makeText(this,"permission denied",Toast.LENGTH_SHORT).show();
                    }
                }
                break;

            case STORAGE_REQUEST_CODE:
                if(grantResults.length > 0){
                    boolean writeStorageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if(writeStorageAccepted){
                        pickGallery();
                    }
                    else{
                        Toast.makeText(this,"permission denied",Toast.LENGTH_SHORT).show();
                    }
                }
                break;
        }
    }


    @Override


    protected  void  onActivityResult (int requestCode , int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == IMAGE_PICK_GALLERY_CODE) {
                //got image from gallerie now crop it
                CropImage.activity(data.getData())
                        .setGuidelines(CropImageView.Guidelines.ON) //enable image guidelines
                        .start(this);
            }
            if (requestCode == IMAGE_PICK_CAMERA_CODE) {
                //got image from camera now crop it
                CropImage.activity(image_uri)
                        .setGuidelines(CropImageView.Guidelines.ON) //enable image guidelines
                        .start(this);
            }

        }
        //get cropped image
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);

            if (resultCode == RESULT_OK) {

                Uri resultUri = result.getUriContent(); //get image uri
                resultat = resultUri;

                InputImage image1 = null;
                try {
                    image1 = InputImage.fromFilePath(this,resultUri);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                TextRecognizer recognizer = TextRecognition.getClient();
                Task<Text> resultOCR =
                        recognizer.process(image1)
                                .addOnSuccessListener(new OnSuccessListener<Text>() {
                                    @Override
                                    public void onSuccess(Text visionText) {
                                        Intent intent = new Intent(getBaseContext(), Extraction.class);
                                        intent.putExtra("Scantext",visionText.getText());
                                        intent.putExtra("uri",resultUri);

                                        startActivity(intent);
                                        // Task completed successfully
                                        // ...

                                    }
                                });
            }
        }
    }


    public Uri getResultat() {
        return resultat;
    }
}


