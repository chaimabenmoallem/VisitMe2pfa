package com.example.visitme;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.nfc.Tag;
import android.os.Bundle;

import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;


import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Extraction extends AppCompatActivity {
private ProgressDialog progressDialog;
    FloatingActionButton save;
    EditText nom , entreprise , titre , adresse , mobile_1 , mobile_2 , fix_1, fix_2 , email , site_web , note ;
    ImageView mPreviewIv ;
    String categorieSelected="";
    Uri imageFile;
    String scanResult;
    String QRresult;
    Contact contact;
    FirebaseStorage storage;
    StorageReference storageRef;
    private String TAG = "ExtractionActivity";
    private FirebaseDatabase database;
    private DatabaseReference mDatabase;
    private static final String CONTACTS = "users";
    private User user;
    private FirebaseAuth firebaseAuth;
    //TAG
    private static final String TAG1 = "CONTACT_TAG";
    //write contact permission request constant
    private static final int WRITE_CONTACT_PERMISSION_CODE = 100;

    //array of permission to request for write contact
    private String[] contactPermission;

    //image uri
    private Uri image_uri;
    //
    int test=0;
    //
    ContactQR contactQR;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_extraction);
        //init storafe firebase
        // Create a storage reference from our app
        storage = FirebaseStorage.getInstance();
        database = FirebaseDatabase.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
        //get database reference
        mDatabase = database.getReference(CONTACTS);
        storageRef = storage.getReference();
            // lier les variables a leurs id
            save = findViewById(R.id.save);
            nom = findViewById(R.id.nom_prénom);
            entreprise = findViewById(R.id.entreprise);
            titre = findViewById(R.id.titre);
            adresse = findViewById(R.id.adresse);
            mobile_1 = findViewById(R.id.mobile_1);
            mobile_2 = findViewById(R.id.mobile_2);
            fix_1 = findViewById(R.id.fix_1);
            fix_2=findViewById(R.id.fix_2);
            email = findViewById(R.id.email);
            site_web = findViewById(R.id.site_web);
            note = findViewById(R.id.note);
            mPreviewIv = findViewById(R.id.mPreviewIv);

        //init permission array , need only write contact permission
        contactPermission = new String[]{Manifest.permission.WRITE_CONTACTS};

        imageFile = getIntent().getParcelableExtra("uri");
        mPreviewIv.setImageURI(imageFile);
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("veuillez patienter");
        progressDialog.setMessage("Sauvegarde en cours...");
        progressDialog.setCanceledOnTouchOutside(false);
        //scan QR code from camera
        QRresult=getIntent().getExtras().getString("ScanQR","defaultKey");
        if(!QRresult.equals("defaultKey")){extractdata(QRresult);test=1;}
        //scan QR code from photo

        scanResult=getIntent().getExtras().getString("Scantext","defaultKey");
        if(!scanResult.equals("defaultKey")){ extractdata(scanResult);test=2;}

            
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isWriteContactPermissionEnabled()){
                    //saveCard dans firebase();
                    //open Alert dialog for categories
                    showCategories();
                    //permission already enabled, save contact dans repertoires
                    saveContact();
                }
                else{
                    // permission not enabled , request
                    requestWriteContactPermission();
                }
            }
        });

    }
    private void saveContact() {
        // input data
        String firstName = nom.getText().toString().trim();
        String Entreprise = entreprise.getText().toString().trim();
        String Profession = titre.getText().toString().trim();
        String phoneMobile = mobile_1.getText().toString().trim();
        String phoneMobile2 = mobile_2.getText().toString().trim();
        String phoneHome = fix_1.getText().toString().trim();
        String phoneHome2 = fix_2.getText().toString().trim();
        String Email = email.getText().toString().trim();
        String address = adresse.getText().toString().trim();
        String SiteWeb = site_web.getText().toString().trim();
        String Note = note.getText().toString().trim();

        ArrayList<ContentProviderOperation> cpo = new ArrayList<>();
        //contact id
        int rawContactId = cpo.size();

        cpo.add(ContentProviderOperation.newInsert(
                ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE,null)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME,null)
                .build());

        //Add First Name ,Last name
        cpo.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.RawContacts.Data.RAW_CONTACT_ID,rawContactId)
                .withValue(ContactsContract.RawContacts.Data.MIMETYPE,ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME,firstName)
                .build());

        // add company name
        cpo.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.RawContacts.Data.RAW_CONTACT_ID,rawContactId)
                .withValue(ContactsContract.RawContacts.Data.MIMETYPE,ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Organization.COMPANY,Entreprise)
                .withValue(ContactsContract.CommonDataKinds.Organization.TYPE,ContactsContract.CommonDataKinds.Organization.TYPE_WORK)
                .withValue(ContactsContract.CommonDataKinds.Organization.TITLE,Profession)
                .build());

        // add web site
        cpo.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.RawContacts.Data.RAW_CONTACT_ID,rawContactId)
                .withValue(ContactsContract.RawContacts.Data.MIMETYPE,ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Website.URL,SiteWeb)
                .withValue(ContactsContract.CommonDataKinds.Website.TYPE,ContactsContract.CommonDataKinds.Website.TYPE_HOMEPAGE)
                .build());

        // add note
        cpo.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.RawContacts.Data.RAW_CONTACT_ID,rawContactId)
                .withValue(ContactsContract.RawContacts.Data.MIMETYPE,ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Note.NOTE,Note)
                //.withValue(ContactsContract.CommonDataKinds.Note.TYPE,ContactsContract.CommonDataKinds.Website.TYPE_HOMEPAGE)
                .build());


        //Add Phone number (mobile)
        cpo.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.RawContacts.Data.RAW_CONTACT_ID,rawContactId)
                .withValue(ContactsContract.RawContacts.Data.MIMETYPE,ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER,phoneMobile)
                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE,ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                .build());
        //Add Phone number2 (mobile)
        cpo.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.RawContacts.Data.RAW_CONTACT_ID,rawContactId)
                .withValue(ContactsContract.RawContacts.Data.MIMETYPE,ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER,phoneMobile2)
                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE,ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                .build());

        //Add Phone number (Home)
        cpo.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.RawContacts.Data.RAW_CONTACT_ID,rawContactId)
                .withValue(ContactsContract.RawContacts.Data.MIMETYPE,ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER,phoneHome)
                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE,ContactsContract.CommonDataKinds.Phone.TYPE_HOME)
                .build());
        //Add Phone number2 (Home)
        cpo.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.RawContacts.Data.RAW_CONTACT_ID,rawContactId)
                .withValue(ContactsContract.RawContacts.Data.MIMETYPE,ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER,phoneHome2)
                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE,ContactsContract.CommonDataKinds.Phone.TYPE_HOME)
                .build());

        //Add email
        cpo.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.RawContacts.Data.RAW_CONTACT_ID,rawContactId)
                .withValue(ContactsContract.RawContacts.Data.MIMETYPE,ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Email.DATA,Email)
                .withValue(ContactsContract.CommonDataKinds.Email.TYPE,ContactsContract.CommonDataKinds.Email.TYPE_WORK) //add/change any type
                .build());

        //add Address
        cpo.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.RawContacts.Data.RAW_CONTACT_ID,rawContactId)
                .withValue(ContactsContract.RawContacts.Data.MIMETYPE,ContactsContract.CommonDataKinds.SipAddress.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.SipAddress.DATA,address)
                .withValue(ContactsContract.CommonDataKinds.SipAddress.TYPE,ContactsContract.CommonDataKinds.Email.TYPE_WORK) //add/change any type
                .build());

        //get image, convert to bytes to store in contact
        byte[] imageBytes = imageUriToBytes();
        if(imageBytes != null){
            //contact with image
            //add image
            cpo.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.RawContacts.Data.RAW_CONTACT_ID,rawContactId)
                    .withValue(ContactsContract.RawContacts.Data.MIMETYPE,ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Photo.PHOTO,imageBytes)
                    .build());
        }
        //else {contact without image}

        // save contact
        try {
            ContentProviderResult[] results = getContentResolver().applyBatch(ContactsContract.AUTHORITY , cpo);
            Log.d(TAG1,"saveContact : Saved...");
            Toast.makeText(this,"Enregistré dans le répertoire de contact...",Toast.LENGTH_SHORT).show();
        }  catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG1, "saveContact"+e.getMessage());
            Toast.makeText(this,""+e.getMessage(),Toast.LENGTH_SHORT).show();
        }


    }
    private byte[] imageUriToBytes() {
        Bitmap bitmap;
        ByteArrayOutputStream baos = null;
        try {
           Bitmap bm =((BitmapDrawable)mPreviewIv.getDrawable()).getBitmap();
           // bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(),image_uri);
            baos = new ByteArrayOutputStream();
            bm.compress(Bitmap.CompressFormat.JPEG,50,baos);
            return baos.toByteArray();

        } catch (Exception e) {
            Log.d(TAG1,"imageUriToBytes:"+e.getMessage());
            return null;
        }
    }
    private boolean isWriteContactPermissionEnabled(){
        boolean result = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CONTACTS) == (PackageManager.PERMISSION_GRANTED);
        return  result;
    }

    private void requestWriteContactPermission(){
        ActivityCompat.requestPermissions(this,contactPermission,WRITE_CONTACT_PERMISSION_CODE);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        //handle permission result

        if (grantResults.length>0){
            if(requestCode == WRITE_CONTACT_PERMISSION_CODE){
                boolean haveWriteContactPermission = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                if (haveWriteContactPermission){
                    //permission garanted , save contact
                    saveContact();
                    //saveCard dans firebase();
                    //open Alert dialog for categories
                    showCategories();
                }
                else{
                    // permission denied , can't save contact
                    Toast.makeText(this,"Permission denied ...",Toast.LENGTH_SHORT).show();

                }
            }
        }
    }



    private void showCategories() {
        String[] categories= {"categorie 1","categorie 2","categorie 3","categorie 4","Autre"};
        AlertDialog.Builder builder = new AlertDialog.Builder(Extraction.this);
        builder.setTitle("Choisir catégorie");

        builder.setSingleChoiceItems(categories, -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                categorieSelected= categories[which];
                Log.v(TAG, categories[which]);
            }
        });
    builder.setPositiveButton("Valider", new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {

            saveCard();
            dialog.dismiss();
        }
    });
        builder.setNegativeButton("Annuler", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
    builder.show();
    }

    public void extractdata(String s) {
        //scantext.setText(visionText.getText());
        //String s=(visionText.getText());
        nom.setText(s);
        /////////////////////////////////////////////////extraction d'email////////////////////////////////////////////////
         String emails="";

         Matcher m = Pattern.compile("[a-zA-Z][a-zA-Z0-9_.+-]+\\s?@\\s?[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+").matcher(s);
         while (m.find()) {
             emails=emails+m.group()+"\n";
         }
         email.setText(emails);

        ////////////////////////////////////////////////fin extraction email //////////////////////////////////////////



        /////////////////////////////////////////////////extraction mobile ////////////////////////////////////////////////
        String mobiles = ",";


        Matcher n = Pattern.compile("\\(?((\\+|00)?216)?\\)?\\s?\\.?(((40|41|42|44|46|56|50|51|52|53|54|55|58)\\s?\\.?[0-9]{3}\\s?\\.?[0-9]{3})|((9|2)[0-9]\\s?\\.?[0-9]{3}\\s?\\.?[0-9]{3})|((40|41|42|44|46|56|50|51|52|53|54|55|58)\\s?\\.?[0-9]{2}\\s?\\.?[0-9]{2}\\s?\\.?[0-9]{2})|((9|2)[0-9]\\s?\\.?[0-9]{2}\\s?\\.?[0-9]{2}\\s?\\.?[0-9]{2}))").matcher(s);
        while (n.find()) {
            mobiles = mobiles+ n.group()+",";

        }

       //mobile_1.setText(mobiles);

        Pattern p = Pattern.compile(",") ;
        String [] tableau = p.split(mobiles) ;
        if(tableau.length>1)
        mobile_1.setText(tableau[1]);
        if(tableau.length>2)
        mobile_2.setText(tableau[2]);


              ////////////////////////////////////////////////fin extraction mobile //////////////////////////////////////////

        /////////////////////////////////////////////////extraction fix ////////////////////////////////////////////////
        String fix = ",";
        Matcher k = Pattern.compile("\\(?((\\+|00)216)?\\)?\\s?\\.?((30|31|32|36|39|80|82)\\s?\\.?[0-9]{2}\\s?\\.?[0-9]{2}\\s?\\.?[0-9]{2}|((7)[0-9]\\s?\\.?[0-9]{2}\\s?\\.?[0-9]{2}\\s?\\.?[0-9]{2})|(30|31|32|36|39|80|82)\\s?\\.?[0-9]{3}\\s?\\.?[0-9]{3}|((7)[0-9]\\s?\\.?[0-9]{3}\\s?\\.?[0-9]{3}))").matcher(s);
        while (k.find()) {
            fix = fix+ k.group()+",";
        }

        //Pattern x = Pattern.compile(",");
        String [] tableaufix = p.split(fix) ;
        if(tableaufix.length>1)
        fix_1.setText(tableaufix[1]);
        if(tableaufix.length>2)
        fix_2.setText(tableaufix[2]);

        ////////////////////////////////////////////////fin extraction fix //////////////////////////////////////////

        /////////////////////////////////////////////////extraction site web ////////////////////////////////////////////////
        String bb = "";
        Matcher v = Pattern.compile("((www\\.)|(WWW\\.))?[a-zA-Z0-9-_.]+\\.([a-zA-Z]{2,8})(\\.[a-zA-Z]{2,8})?").matcher(s);
        while (v.find()) {
            bb = bb +"\n" + v.group();

        }
        Matcher u = Pattern.compile("((www\\.)|(WWW\\.))[a-zA-Z0-9-_.]+\\.([a-zA-Z]{2,8})(\\.[a-zA-Z]{2,8})?").matcher(bb);
        if(u.find()) {
            site_web.setText(u.group());
        } else site_web.setText(bb);
        ////////////////////////////////////////////////fin extraction site web //////////////////////////////////////////

        /////////////////////////////////////////////////extraction profession ////////////////////////////////////////////////
        /*1er methode
        String sentrée="\n"+s;

        Pattern j = Pattern.compile("\n") ;
        String [] tableausep = j.split(sentrée) ;
        String titres= "";
        String profes = tableausep[2]+tableausep[3];
        Matcher c= Pattern.compile("([a-zA-Z-éè]{4,14}(ienne|IENNE|ien|IEN)(\\s[a-zA-Z'èé]{2,17})?(\\s[a-zA-Z'éè]{1,15})?(\\s[a-zA-Zéè]{4,8})?)|([a-zA-Zé-]{3,15}(ier|IER)((-|\\s)[a-zA-Z']{2,13})?(\\s[a-zA-Z'éè]{4,11})?(\\s[a-zA-Z]{6,7})?)|([a-zA-Z]{5,8}(ière|iere|IERE)(\\s[a-zA-Z]{2})?(\\s[a-zA-Zé]{10})?)|([a-zA-Z-]{1,15}(eronne|ERONNE|eron|ERON))|([a-zA-Zéè-]{3,18}(euse|EUSE|eur|EUR)(\\s[a-zA-Z-éè]{2,19})?(\\s[a-zA-Zéè-]{2,15})?(\\s[a-zA-Z-]{2,12})?(\\s[a-zA-Z]{5,8})?)|([a-zA-Zé]{2,8}(trice|TRICE))|([a-zA-Z]{2,6}(and|AND)(\\s[a-zA-Z]{2,8})?(\\s[a-zA-Z]{8})?)|([a-zA-Z-é]{3,23}(iste|ISTE)((-|\\s)[a-zA-Z]{2,12})?(\\s[a-zA-Zé]{2,13})?(\\s[a-zA-Zé]{8,12})?)|([a-zA-Zé']{1,10}(aire|AIRE)(\\s[a-zA-Z'é]{2,15})?(\\s[a-zA-Zéè']{3,12})?(\\s[a-zA-Z']{7})?)|([a-zA-Zé-]{4,12}(er|ER)((-|\\s)[a-zA-Z']{2,13})?(\\s[a-zA-Z'éè]{5,13})?(\\s[a-zA-Z]{3,8})?(\\s[a-zA-Z'éè]{2})?)|([a-zA-Z]{3}(oint|OINT)(\\s[a-zA-Z]{2,13})?(\\s[a-zA-Zé]{8,9})?)|([a-zA-Zé]{3,8}(in|IN)(\\s[a-zA-Zé]{2,8})?(\\s[a-zA-Z]{2,5})?(\\s[a-zA-Z]{6})?(\\s[a-zA-Z]{9})?)|([a-zA-Z]{5,6}(ate|ATE))|([a-zA-Zé]{2,6}(ent|ENT)((-|\\s)[a-zA-Z']{2,11})?(\\s[a-zA-Zé]{2,13})?(\\s[a-zA-Z]{5,10})?)|([a-zA-Z]{1,6}(ome|OME)(\\s[a-zA-Z]{2,6})?(\\s[a-zA-Z]{8})?)|([a-zA-Z]{0}(aide|Aide|AIDE)((-|\\s)[a-zA-Zé-]{8,20})?(\\s[a-zA-Z]{2})?(\\s[a-zA-Zé]{12})?)|([a-zA-Z]{4,9}(yste|YSTE)(\\s[a-zA-Zé]{3,11})?(\\s[a-zA-Z]{2})?(\\s[a-zA-Z]{10})?)|([a-zA-Zé]{4,12}(ogue|OGUE)(\\s[a-zA-Z]{2,9})?(\\s[a-zA-Zé]{7})?)|([a-zA-Z-]{6}(ecte|ECTE)(\\s[a-zA-Z'\\p{javaLowerCase}]+)?(\\s[a-zA-Z'\\p{javaLowerCase}]+)?(\\s[a-zA-Z'\\p{javaLowerCase}]+)?)|([a-zA-Z]{3,4}(san|SAN)(\\s[a-zA-Z]+)?(\\s[a-zA-Z]+)?(\\s[a-zA-Z]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{3,11}(ant|ANT)-?(\\s?[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}]{3,11}(ante|ANTE)(\\s[a-zA-Z-'\\p{javaLowerCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}]{4,9}(aute|AUTE)(\\s[a-zA-Z-'\\p{javaLowerCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}]{4,8}(ète|ETE)(\\s[a-zA-Z-'\\p{javaLowerCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{3}(cat|CAT)(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{3,11}(aphe|APHE)(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{2,7}(ron|RON)(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{2,10}(man|MAN)(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{2,10}(aine|AINE)(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{2,3}(ef|EF)(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{2}(ach|ACH)(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{7}(ial|IAL)(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{3,8}(able|ABLE)(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{5}(erge|ERGE)(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{3}(ane|ANE)(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{3}(ert|ERT)-?(\\s ?[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{3}(esse|ESSE)-?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{8,14}(eute|EUTE)-?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{1}(uge|UGE)-?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{4}(ère|ERE)-?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{3,8}(ote|OTE)-?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{3,12}(tre|TRE)-?(\\s?[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)([a-zA-Z-éè]{4,14}(ienne|IENNE|ien|IEN)(\\s[a-zA-Z'èé]{2,17})?(\\s[a-zA-Z'éè]{1,15})?(\\s[a-zA-Zéè]{4,8})?)|([a-zA-Zé-]{3,15}(ier|IER)((-|\\s)[a-zA-Z']{2,13})?(\\s[a-zA-Z'éè]{4,11})?(\\s[a-zA-Z]{6,7})?)|([a-zA-Z]{5,8}(ière|iere|IERE)(\\s[a-zA-Z]{2})?(\\s[a-zA-Zé]{10})?)|([a-zA-Z-]{1,15}(eronne|ERONNE|eron|ERON))|([a-zA-Zéè-]{3,18}(euse|EUSE|eur|EUR)(\\s[a-zA-Z-éè]{2,19})?(\\s[a-zA-Zéè-]{2,15})?(\\s[a-zA-Z-]{2,12})?(\\s[a-zA-Z]{5,8})?)|([a-zA-Zé]{2,8}(trice|TRICE))|([a-zA-Z]{2,6}(and|AND)(\\s[a-zA-Z]{2,8})?(\\s[a-zA-Z]{8})?)|([a-zA-Z-é]{3,23}(iste|ISTE)((-|\\s)[a-zA-Z]{2,12})?(\\s[a-zA-Zé]{2,13})?(\\s[a-zA-Zé]{8,12})?)|([a-zA-Zé']{1,10}(aire|AIRE)(\\s[a-zA-Z'é]{2,15})?(\\s[a-zA-Zéè']{3,12})?(\\s[a-zA-Z']{7})?)|([a-zA-Zé-]{4,12}(er|ER)((-|\\s)[a-zA-Z']{2,13})?(\\s[a-zA-Z'éè]{5,13})?(\\s[a-zA-Z]{3,8})?(\\s[a-zA-Z'éè]{2})?)|([a-zA-Z]{3}(oint|OINT)(\\s[a-zA-Z]{2,13})?(\\s[a-zA-Zé]{8,9})?)|([a-zA-Zé]{3,8}(in|IN)(\\s[a-zA-Zé]{2,7})?(\\s[a-zA-Z]{2,5})?(\\s[a-zA-Z]{6})?(\\s[a-zA-Z]{9})?)|([a-zA-Z]{5,6}(ate|ATE))|([a-zA-Zé]{2,6}(ent|ENT)((-|\\s)[a-zA-Z']{2,11})?(\\s[a-zA-Zé]{2,13})?(\\s[a-zA-Z]{5,10})?)|([a-zA-Z]{1,6}(ome|OME)(\\s[a-zA-Z]{2,6})?(\\s[a-zA-Z]{8})?)|([a-zA-Z]{0}(aide|Aide|AIDE)((-|\\s)[a-zA-Zé-]{8,20})?(\\s[a-zA-Z]{2})?(\\s[a-zA-Zé]{12})?)|([a-zA-Z]{4,9}(yste|YSTE)(\\s[a-zA-Zé]{3,11})?(\\s[a-zA-Z]{2})?(\\s[a-zA-Z]{10})?)|([a-zA-Zé]{4,12}(ogue|OGUE)(\\s[a-zA-Z]{2,9})?(\\s[a-zA-Zé]{7})?)|([a-zA-Z-]{6}(ecte|ECTE)(\\s[a-zA-Z'\\p{javaLowerCase}]+)?(\\s[a-zA-Z'\\p{javaLowerCase}]+)?(\\s[a-zA-Z'\\p{javaLowerCase}]+)?)|([a-zA-Z]{3,4}(san|SAN)(\\s[a-zA-Z]+)?(\\s[a-zA-Z]+)?(\\s[a-zA-Z]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{3,11}(ant|ANT)-?(\\s?[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}]{3,11}(ante|ANTE)(\\s[a-zA-Z-'\\p{javaLowerCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}]{4,9}(aute|AUTE)(\\s[a-zA-Z-'\\p{javaLowerCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}]{4,8}(ète|ETE)(\\s[a-zA-Z-'\\p{javaLowerCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{3}(cat|CAT)(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{3,11}(aphe|APHE)(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{2,7}(ron|RON)(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{2,10}(man|MAN)(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{2,10}(aine|AINE)(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{2,3}(ef|EF)(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{2}(ach|ACH)(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{7}(ial|IAL)(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{3,8}(able|ABLE)(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{5}(erge|ERGE)(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{3}(ane|ANE)(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{3}(ert|ERT)-?(\\s ?[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{3}(esse|ESSE)-?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{8,14}(eute|EUTE)-?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{1}(uge|UGE)-?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{4}(ère|ERE)-?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{3,8}(ote|OTE)-?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{3,12}(tre|TRE)-?(\\s?[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)([a-zA-Z-éè]{4,14}(ienne|IENNE|ien|IEN)(\\s[a-zA-Z'èé]{2,17})?(\\s[a-zA-Z'éè]{1,15})?(\\s[a-zA-Zéè]{4,8})?)|([a-zA-Zé-]{3,15}(ier|IER)((-|\\s)[a-zA-Z']{2,13})?(\\s[a-zA-Z'éè]{4,11})?(\\s[a-zA-Z]{6,7})?)|([a-zA-Z]{5,8}(ière|iere|IERE)(\\s[a-zA-Z]{2})?(\\s[a-zA-Zé]{10})?)|([a-zA-Z-]{1,15}(eronne|ERONNE|eron|ERON))|([a-zA-Zéè-]{3,18}(euse|EUSE|eur|EUR)(\\s[a-zA-Z-éè]{2,19})?(\\s[a-zA-Zéè-]{2,15})?(\\s[a-zA-Z-]{2,12})?(\\s[a-zA-Z]{5,8})?)|([a-zA-Zé]{2,8}(trice|TRICE))|([a-zA-Z]{2,6}(and|AND)(\\s[a-zA-Z]{2,8})?(\\s[a-zA-Z]{8})?)|([a-zA-Z-é]{3,23}(iste|ISTE)((-|\\s)[a-zA-Z]{2,12})?(\\s[a-zA-Zé]{2,13})?(\\s[a-zA-Zé]{8,12})?)|([a-zA-Zé']{1,10}(aire|AIRE)(\\s[a-zA-Z'é]{2,15})?(\\s[a-zA-Zéè']{3,12})?(\\s[a-zA-Z']{7})?)|([a-zA-Zé-]{4,12}(er|ER)((-|\\s)[a-zA-Z']{2,13})?(\\s[a-zA-Z'éè]{5,13})?(\\s[a-zA-Z]{3,8})?(\\s[a-zA-Z'éè]{2})?)|([a-zA-Z]{3}(oint|OINT)(\\s[a-zA-Z]{2,13})?(\\s[a-zA-Zé]{8,9})?)|([a-zA-Zé]{3,8}(in|IN)(\\s[a-zA-Zé]{2,7})?(\\s[a-zA-Z]{2,5})?(\\s[a-zA-Z]{6})?(\\s[a-zA-Z]{9})?)|([a-zA-Z]{5,6}(ate|ATE))|([a-zA-Zé]{2,6}(ent|ENT)((-|\\s)[a-zA-Z']{2,11})?(\\s[a-zA-Zé]{2,13})?(\\s[a-zA-Z]{5,10})?)|([a-zA-Z]{1,6}(ome|OME)(\\s[a-zA-Z]{2,6})?(\\s[a-zA-Z]{8})?)|([a-zA-Z]{0}(aide|Aide|AIDE)((-|\\s)[a-zA-Zé-]{8,20})?(\\s[a-zA-Z]{2})?(\\s[a-zA-Zé]{12})?)|([a-zA-Z]{4,9}(yste|YSTE)(\\s[a-zA-Zé]{3,11})?(\\s[a-zA-Z]{2})?(\\s[a-zA-Z]{10})?)|([a-zA-Zé]{4,12}(ogue|OGUE)(\\s[a-zA-Z]{2,9})?(\\s[a-zA-Zé]{7})?)|([a-zA-Z-]{6}(ecte|ECTE)(\\s[a-zA-Z'\\p{javaLowerCase}]+)?(\\s[a-zA-Z'\\p{javaLowerCase}]+)?(\\s[a-zA-Z'\\p{javaLowerCase}]+)?)|([a-zA-Z]{3,4}(san|SAN)(\\s[a-zA-Z]+)?(\\s[a-zA-Z]+)?(\\s[a-zA-Z]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{3,11}(ant|ANT)-?(\\s?[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}]{3,11}(ante|ANTE)(\\s[a-zA-Z-'\\p{javaLowerCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}]{4,9}(aute|AUTE)(\\s[a-zA-Z-'\\p{javaLowerCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}]{4,8}(ète|ETE)(\\s[a-zA-Z-'\\p{javaLowerCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{3}(cat|CAT)(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{3,11}(aphe|APHE)(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{2,7}(ron|RON)(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{2,10}(man|MAN)(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{2,10}(aine|AINE)(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{2,3}(ef|EF)(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{2}(ach|ACH)(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{7}(ial|IAL)(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{3,8}(able|ABLE)(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{5}(erge|ERGE)(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{3}(ane|ANE)(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{3}(ert|ERT)-?(\\s ?[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{3}(esse|ESSE)-?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{8,14}(eute|EUTE)-?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{1}(uge|UGE)-?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{4}(ère|ERE)-?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{3,8}(ote|OTE)-?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{3,12}(tre|TRE)-?(\\s?[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?) ").matcher(profes);

        while (c.find()) {
            titres = titres +"\n" + c.group();
        }
        titre.setText(titres);*/

        //2eme methode

        String titres = "\n";
        Matcher w = Pattern.compile("\\D+").matcher(s);
        if (w.find()) {
            titres = titres+ w.group()+"\n";
        }
        //titre.setText(titres);

        Pattern j = Pattern.compile("\n") ;
        String [] tableauprof = j.split(titres) ;

        String resultat=tableauprof[tableauprof.length-2]+"\n"+tableauprof[tableauprof.length-1];
        //titre.setText(resultat);

        Matcher c= Pattern.compile("([a-zA-Z-éè]{4,14}(ienne|IENNE|ien|IEN)(\\s[a-zA-Z'èé]{2,17})?(\\s[a-zA-Z'éè]{1,15})?(\\s[a-zA-Zéè]{4,8})?)|([a-zA-Zé-]{3,15}(ier|IER)((-|\\s)[a-zA-Z']{2,13})?(\\s[a-zA-Z'éè]{4,11})?(\\s[a-zA-Z]{6,7})?)|([a-zA-Z]{5,8}(ière|iere|IERE)(\\s[a-zA-Z]{2})?(\\s[a-zA-Zé]{10})?)|([a-zA-Z-]{1,15}(eronne|ERONNE|eron|ERON))|([a-zA-Zéè-]{3,18}(euse|EUSE|eur|EUR)(\\s[a-zA-Z-éè]{2,19})?(\\s[a-zA-Zéè-]{2,15})?(\\s[a-zA-Z-]{2,12})?(\\s[a-zA-Z]{5,8})?)|([a-zA-Zé]{2,8}(trice|TRICE))|([a-zA-Z]{2,6}(and|AND)(\\s[a-zA-Z]{2,8})?(\\s[a-zA-Z]{8})?)|([a-zA-Z-é]{3,23}(iste|ISTE)((-|\\s)[a-zA-Z]{2,12})?(\\s[a-zA-Zé]{2,13})?(\\s[a-zA-Zé]{8,12})?)|([a-zA-Zé']{1,10}(aire|AIRE)(\\s[a-zA-Z'é]{2,15})?(\\s[a-zA-Zéè']{3,12})?(\\s[a-zA-Z']{7})?)|([a-zA-Zé-]{4,12}(er|ER)((-|\\s)[a-zA-Z']{2,13})?(\\s[a-zA-Z'éè]{5,13})?(\\s[a-zA-Z]{3,8})?(\\s[a-zA-Z'éè]{2})?)|([a-zA-Z]{3}(oint|OINT)(\\s[a-zA-Z]{2,13})?(\\s[a-zA-Zé]{8,9})?)|([a-zA-Zé]{3,8}(in|IN)(\\s[a-zA-Zé]{2,8})?(\\s[a-zA-Z]{2,5})?(\\s[a-zA-Z]{6})?(\\s[a-zA-Z]{9})?)|([a-zA-Z]{5,6}(ate|ATE))|([a-zA-Zé]{2,6}(ent|ENT)((-|\\s)[a-zA-Z']{2,11})?(\\s[a-zA-Zé]{2,13})?(\\s[a-zA-Z]{5,10})?)|([a-zA-Z]{1,6}(ome|OME)(\\s[a-zA-Z]{2,6})?(\\s[a-zA-Z]{8})?)|([a-zA-Z]{0}(aide|Aide|AIDE)((-|\\s)[a-zA-Zé-]{8,20})?(\\s[a-zA-Z]{2})?(\\s[a-zA-Zé]{12})?)|([a-zA-Z]{4,9}(yste|YSTE)(\\s[a-zA-Zé]{3,11})?(\\s[a-zA-Z]{2})?(\\s[a-zA-Z]{10})?)|([a-zA-Zé]{4,12}(ogue|OGUE)(\\s[a-zA-Z]{2,9})?(\\s[a-zA-Zé]{7})?)|([a-zA-Z-]{6}(ecte|ECTE)(\\s[a-zA-Z'\\p{javaLowerCase}]+)?(\\s[a-zA-Z'\\p{javaLowerCase}]+)?(\\s[a-zA-Z'\\p{javaLowerCase}]+)?)|([a-zA-Z]{3,4}(san|SAN)(\\s[a-zA-Z]+)?(\\s[a-zA-Z]+)?(\\s[a-zA-Z]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{3,11}(ant|ANT)-?(\\s?[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}]{3,11}(ante|ANTE)(\\s[a-zA-Z-'\\p{javaLowerCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}]{4,9}(aute|AUTE)(\\s[a-zA-Z-'\\p{javaLowerCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}]{4,8}(ète|ETE)(\\s[a-zA-Z-'\\p{javaLowerCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{3}(cat|CAT)(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{3,11}(aphe|APHE)(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{2,7}(ron|RON)(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{2,10}(man|MAN)(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{2,10}(aine|AINE)(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{2,3}(ef|EF)(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{2}(ach|ACH)(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{7}(ial|IAL)(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{3,8}(able|ABLE)(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{5}(erge|ERGE)(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{3}(ane|ANE)(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{3}(ert|ERT)-?(\\s ?[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{3}(esse|ESSE)-?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{8,14}(eute|EUTE)-?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{1}(uge|UGE)-?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{4}(ère|ERE)-?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{3,8}(ote|OTE)-?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{3,12}(tre|TRE)-?(\\s?[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)([a-zA-Z-éè]{4,14}(ienne|IENNE|ien|IEN)(\\s[a-zA-Z'èé]{2,17})?(\\s[a-zA-Z'éè]{1,15})?(\\s[a-zA-Zéè]{4,8})?)|([a-zA-Zé-]{3,15}(ier|IER)((-|\\s)[a-zA-Z']{2,13})?(\\s[a-zA-Z'éè]{4,11})?(\\s[a-zA-Z]{6,7})?)|([a-zA-Z]{5,8}(ière|iere|IERE)(\\s[a-zA-Z]{2})?(\\s[a-zA-Zé]{10})?)|([a-zA-Z-]{1,15}(eronne|ERONNE|eron|ERON))|([a-zA-Zéè-]{3,18}(euse|EUSE|eur|EUR)(\\s[a-zA-Z-éè]{2,19})?(\\s[a-zA-Zéè-]{2,15})?(\\s[a-zA-Z-]{2,12})?(\\s[a-zA-Z]{5,8})?)|([a-zA-Zé]{2,8}(trice|TRICE))|([a-zA-Z]{2,6}(and|AND)(\\s[a-zA-Z]{2,8})?(\\s[a-zA-Z]{8})?)|([a-zA-Z-é]{3,23}(iste|ISTE)((-|\\s)[a-zA-Z]{2,12})?(\\s[a-zA-Zé]{2,13})?(\\s[a-zA-Zé]{8,12})?)|([a-zA-Zé']{1,10}(aire|AIRE)(\\s[a-zA-Z'é]{2,15})?(\\s[a-zA-Zéè']{3,12})?(\\s[a-zA-Z']{7})?)|([a-zA-Zé-]{4,12}(er|ER)((-|\\s)[a-zA-Z']{2,13})?(\\s[a-zA-Z'éè]{5,13})?(\\s[a-zA-Z]{3,8})?(\\s[a-zA-Z'éè]{2})?)|([a-zA-Z]{3}(oint|OINT)(\\s[a-zA-Z]{2,13})?(\\s[a-zA-Zé]{8,9})?)|([a-zA-Zé]{3,8}(in|IN)(\\s[a-zA-Zé]{2,7})?(\\s[a-zA-Z]{2,5})?(\\s[a-zA-Z]{6})?(\\s[a-zA-Z]{9})?)|([a-zA-Z]{5,6}(ate|ATE))|([a-zA-Zé]{2,6}(ent|ENT)((-|\\s)[a-zA-Z']{2,11})?(\\s[a-zA-Zé]{2,13})?(\\s[a-zA-Z]{5,10})?)|([a-zA-Z]{1,6}(ome|OME)(\\s[a-zA-Z]{2,6})?(\\s[a-zA-Z]{8})?)|([a-zA-Z]{0}(aide|Aide|AIDE)((-|\\s)[a-zA-Zé-]{8,20})?(\\s[a-zA-Z]{2})?(\\s[a-zA-Zé]{12})?)|([a-zA-Z]{4,9}(yste|YSTE)(\\s[a-zA-Zé]{3,11})?(\\s[a-zA-Z]{2})?(\\s[a-zA-Z]{10})?)|([a-zA-Zé]{4,12}(ogue|OGUE)(\\s[a-zA-Z]{2,9})?(\\s[a-zA-Zé]{7})?)|([a-zA-Z-]{6}(ecte|ECTE)(\\s[a-zA-Z'\\p{javaLowerCase}]+)?(\\s[a-zA-Z'\\p{javaLowerCase}]+)?(\\s[a-zA-Z'\\p{javaLowerCase}]+)?)|([a-zA-Z]{3,4}(san|SAN)(\\s[a-zA-Z]+)?(\\s[a-zA-Z]+)?(\\s[a-zA-Z]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{3,11}(ant|ANT)-?(\\s?[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}]{3,11}(ante|ANTE)(\\s[a-zA-Z-'\\p{javaLowerCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}]{4,9}(aute|AUTE)(\\s[a-zA-Z-'\\p{javaLowerCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}]{4,8}(ète|ETE)(\\s[a-zA-Z-'\\p{javaLowerCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{3}(cat|CAT)(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{3,11}(aphe|APHE)(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{2,7}(ron|RON)(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{2,10}(man|MAN)(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{2,10}(aine|AINE)(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{2,3}(ef|EF)(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{2}(ach|ACH)(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{7}(ial|IAL)(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{3,8}(able|ABLE)(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{5}(erge|ERGE)(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{3}(ane|ANE)(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{3}(ert|ERT)-?(\\s ?[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{3}(esse|ESSE)-?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{8,14}(eute|EUTE)-?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{1}(uge|UGE)-?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{4}(ère|ERE)-?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{3,8}(ote|OTE)-?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{3,12}(tre|TRE)-?(\\s?[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)([a-zA-Z-éè]{4,14}(ienne|IENNE|ien|IEN)(\\s[a-zA-Z'èé]{2,17})?(\\s[a-zA-Z'éè]{1,15})?(\\s[a-zA-Zéè]{4,8})?)|([a-zA-Zé-]{3,15}(ier|IER)((-|\\s)[a-zA-Z']{2,13})?(\\s[a-zA-Z'éè]{4,11})?(\\s[a-zA-Z]{6,7})?)|([a-zA-Z]{5,8}(ière|iere|IERE)(\\s[a-zA-Z]{2})?(\\s[a-zA-Zé]{10})?)|([a-zA-Z-]{1,15}(eronne|ERONNE|eron|ERON))|([a-zA-Zéè-]{3,18}(euse|EUSE|eur|EUR)(\\s[a-zA-Z-éè]{2,19})?(\\s[a-zA-Zéè-]{2,15})?(\\s[a-zA-Z-]{2,12})?(\\s[a-zA-Z]{5,8})?)|([a-zA-Zé]{2,8}(trice|TRICE))|([a-zA-Z]{2,6}(and|AND)(\\s[a-zA-Z]{2,8})?(\\s[a-zA-Z]{8})?)|([a-zA-Z-é]{3,23}(iste|ISTE)((-|\\s)[a-zA-Z]{2,12})?(\\s[a-zA-Zé]{2,13})?(\\s[a-zA-Zé]{8,12})?)|([a-zA-Zé']{1,10}(aire|AIRE)(\\s[a-zA-Z'é]{2,15})?(\\s[a-zA-Zéè']{3,12})?(\\s[a-zA-Z']{7})?)|([a-zA-Zé-]{4,12}(er|ER)((-|\\s)[a-zA-Z']{2,13})?(\\s[a-zA-Z'éè]{5,13})?(\\s[a-zA-Z]{3,8})?(\\s[a-zA-Z'éè]{2})?)|([a-zA-Z]{3}(oint|OINT)(\\s[a-zA-Z]{2,13})?(\\s[a-zA-Zé]{8,9})?)|([a-zA-Zé]{3,8}(in|IN)(\\s[a-zA-Zé]{2,7})?(\\s[a-zA-Z]{2,5})?(\\s[a-zA-Z]{6})?(\\s[a-zA-Z]{9})?)|([a-zA-Z]{5,6}(ate|ATE))|([a-zA-Zé]{2,6}(ent|ENT)((-|\\s)[a-zA-Z']{2,11})?(\\s[a-zA-Zé]{2,13})?(\\s[a-zA-Z]{5,10})?)|([a-zA-Z]{1,6}(ome|OME)(\\s[a-zA-Z]{2,6})?(\\s[a-zA-Z]{8})?)|([a-zA-Z]{0}(aide|Aide|AIDE)((-|\\s)[a-zA-Zé-]{8,20})?(\\s[a-zA-Z]{2})?(\\s[a-zA-Zé]{12})?)|([a-zA-Z]{4,9}(yste|YSTE)(\\s[a-zA-Zé]{3,11})?(\\s[a-zA-Z]{2})?(\\s[a-zA-Z]{10})?)|([a-zA-Zé]{4,12}(ogue|OGUE)(\\s[a-zA-Z]{2,9})?(\\s[a-zA-Zé]{7})?)|([a-zA-Z-]{6}(ecte|ECTE)(\\s[a-zA-Z'\\p{javaLowerCase}]+)?(\\s[a-zA-Z'\\p{javaLowerCase}]+)?(\\s[a-zA-Z'\\p{javaLowerCase}]+)?)|([a-zA-Z]{3,4}(san|SAN)(\\s[a-zA-Z]+)?(\\s[a-zA-Z]+)?(\\s[a-zA-Z]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{3,11}(ant|ANT)-?(\\s?[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}]{3,11}(ante|ANTE)(\\s[a-zA-Z-'\\p{javaLowerCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}]{4,9}(aute|AUTE)(\\s[a-zA-Z-'\\p{javaLowerCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}]{4,8}(ète|ETE)(\\s[a-zA-Z-'\\p{javaLowerCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{3}(cat|CAT)(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{3,11}(aphe|APHE)(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{2,7}(ron|RON)(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{2,10}(man|MAN)(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{2,10}(aine|AINE)(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{2,3}(ef|EF)(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{2}(ach|ACH)(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{7}(ial|IAL)(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{3,8}(able|ABLE)(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{5}(erge|ERGE)(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{3}(ane|ANE)(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{3}(ert|ERT)-?(\\s ?[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{3}(esse|ESSE)-?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{8,14}(eute|EUTE)-?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{1}(uge|UGE)-?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{4}(ère|ERE)-?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{3,8}(ote|OTE)-?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?)|([a-zA-Z-\\p{javaLowerCase}\\p{javaUpperCase}]{3,12}(tre|TRE)-?(\\s?[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?(\\s[a-zA-Z-'\\p{javaLowerCase}\\p{javaUpperCase}]+)?) ").matcher(resultat);
        String extracttitre="";
        while (c.find()) {
            extracttitre = extracttitre + c.group()+"\n";
        }
        titre.setText(extracttitre);
        ////////////////////////////////////////////////fin extraction profession //////////////////////////////////////////
        ///////////////////////////////////////////////début extraction entreprise//////////////////////////////////////////

           }

public void saveCard(){
    progressDialog.show();
    if(test == 2) {
        StorageReference riversRef = storageRef.child("Image-Carte-Visite/" + imageFile.getLastPathSegment());
        UploadTask uploadTask = riversRef.putFile(imageFile);

        Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
            @Override
            public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) {
                return riversRef.getDownloadUrl();
            }
        }).addOnCompleteListener(new OnCompleteListener<Uri>() {
            @Override
            public void onComplete(@NonNull Task<Uri> task) {
                if (task.isSuccessful()) {
                    String downloadUri = task.getResult().toString();
                    //String downloadUri =task.getResult().toString();
                    Log.e(TAG, "Success ==========");

                    contact = new Contact(nom.getText().toString(), entreprise.getText().toString(),
                            email.getText().toString(), site_web.getText().toString(), titre.getText().toString(),
                            mobile_1.getText().toString(), mobile_2.getText().toString(),
                            adresse.getText().toString(), fix_1.getText().toString(),
                            fix_2.getText().toString(), downloadUri, categorieSelected);

                    // save contact object in realtime database "contacts"
                    FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                    String uid = firebaseUser.getUid();
                    String keyid = mDatabase.push().getKey();
                    mDatabase.child(uid).child("contacts").child(keyid).setValue(contact);
                    progressDialog.dismiss();
                    Intent intent = new Intent(Extraction.this, MainActivity.class);
                    startActivity(intent);

                } else {
                    Log.e(TAG, "ERROR ==========");
                }
            }
        });
    }
    else if(test==1)
    {
        contactQR = new ContactQR(nom.getText().toString(), entreprise.getText().toString(),
                email.getText().toString(), site_web.getText().toString(), titre.getText().toString(),
                mobile_1.getText().toString(), mobile_2.getText().toString(),
                adresse.getText().toString(), fix_1.getText().toString(),
                fix_2.getText().toString(), categorieSelected);

        // save contact object in realtime database "contacts"
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        String uid = firebaseUser.getUid();
        String keyid = mDatabase.push().getKey();
        mDatabase.child(uid).child("contacts").child(keyid).setValue(contactQR);
        progressDialog.dismiss();
        Intent intent = new Intent(Extraction.this, MainActivity.class);
        startActivity(intent);
    }
}
}

