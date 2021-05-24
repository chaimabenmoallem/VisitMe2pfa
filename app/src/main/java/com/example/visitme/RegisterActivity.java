package com.example.visitme;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.example.visitme.databinding.ActivityRegisterBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import static android.Manifest.permission_group.CONTACTS;

public class RegisterActivity extends AppCompatActivity {
    //view binding
    private ActivityRegisterBinding binding;
    private FirebaseAuth firebaseAuth;
    //progress dialog
    private ProgressDialog progressDialog;
    private FirebaseDatabase database;
    private DatabaseReference mDatabase;
    private static final String USERS = "users";
    private String TAG = "RegisterActivity";
    private User user;


    private String name="", email="" , password = "", phone="", confirmpassword="";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        changeStatusBarColor();

        //initialiser firebase auth
        firebaseAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        //get database reference
        mDatabase = database.getReference(USERS);

        //configure progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("please wait");
        progressDialog.setMessage("Creating your account...");
        progressDialog.setCanceledOnTouchOutside(false);

        binding.cirRegisterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                validationData();
            }
        });


    }

    private void validationData() {
        //get data
        email = binding.editTextEmail.getText().toString().trim();
        password = binding.editTextPassword.getText().toString().trim();
        confirmpassword = binding.editTextConfirmPassword.getText().toString().trim();
        name= binding.editTextName.getText().toString().trim();
        phone=binding.editTextPhone.getText().toString().trim();
        //validate data
        if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            // email format is invalid,don't procced further
            binding.editTextEmail.setError("Invalid email format");
            return;
        }
        else if(TextUtils.isEmpty(password)){
            //no password is entered
            binding.editTextPassword.setError("Entrer le mot de passe");
            return;
        }
        else if (password.length()<6){
            //password length less than 6
            binding.editTextPassword.setError("Le mot de passe doit comporter au moins 6 caractères");
            return;
        }
        else if (phone.length()<8){
            //password length less than 8
            binding.editTextPhone.setError("le numéro de téléphone doit comporter au moins 8 chiffres");
            return;
        }
        else if (!password.equals(confirmpassword))
        {binding.editTextConfirmPassword.setError("les mot de passe doit etre compatibles");}
        else {
            //data is valid, now continue firebase signup
            user = new User(name, email, phone);
            firebaseSignUp();
        }
    }

    private void firebaseSignUp() {
        //show progress dialog
        progressDialog.show();

        firebaseAuth.createUserWithEmailAndPassword(email,password)
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        // save User
                       // FirebaseUser user = firebaseAuth.getCurrentUser();
                        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                        String uid = firebaseUser.getUid();

                        updateUI(uid);
                        finish();

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //signup failed
                        progressDialog.dismiss();
                        Toast.makeText(RegisterActivity.this,""+e.getMessage(),Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public void changeStatusBarColor()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){

            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(getResources().getColor(R.color.register_bk_color));


        }
    }

    public  void onLoginClick(View view)
    {
        startActivity(new Intent(this,LoginActivity.class));
        overridePendingTransition(R.anim.slide_in_left, android.R.anim.slide_out_right);
    }


    public void updateUI(String uid) {
        //String keyid = mDatabase.push().getKey();
        mDatabase.child(uid).setValue(user);//adding user info to database

        //signup success
        progressDialog.dismiss();
        Toast.makeText(RegisterActivity.this,"Account created\n",Toast.LENGTH_SHORT).show();

        //open login activity
        Intent loginIntent = new Intent(this, LoginActivity.class);
        startActivity(loginIntent);

    }

}