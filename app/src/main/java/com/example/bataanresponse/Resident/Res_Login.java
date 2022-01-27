package com.example.bataanresponse.Resident;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;

import com.example.bataanresponse.Applicant;
import com.example.bataanresponse.Brgy_Home;
import com.example.bataanresponse.MainActivity;
import com.example.bataanresponse.R;
import com.example.bataanresponse.TownAdmin;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class Res_Login extends AppCompatActivity implements View.OnClickListener {

    EditText etemail, etpass;
    Button btnlogin, btnsignup;
    CheckBox checkBox;

    FirebaseAuth firebaseAuth;
    FirebaseFirestore firestore;

    String email, pass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_res__login);

        etemail = findViewById(R.id.remail);
        etpass = findViewById(R.id.rpassword);
        btnlogin = findViewById(R.id.login);
        btnsignup = findViewById(R.id.signup);
        checkBox = findViewById(R.id.checkbox);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        btnlogin.setOnClickListener(this);
        btnsignup.setOnClickListener(this);

        //SHOW PASSWORD
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked)
                {
                    etpass.setTransformationMethod(null);
                }
                else{
                    etpass.setTransformationMethod(new PasswordTransformationMethod());
                }
            }
        });
    }

    public void onRequestClick(View view) {
        startActivity(new Intent(getApplicationContext(),Res_Signup.class));
        finish();
    }

    void savepRef(String username, String password){
        SharedPreferences settings = getApplicationContext().getSharedPreferences("BATAAN_RESPONSE", 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("username", username);
        editor.putString("password", password);
// Apply the edits!
        editor.apply();
    }


    @Override
    protected void onStart() {
        super.onStart();

        if (FirebaseAuth.getInstance().getCurrentUser() != null){
            DocumentReference df = firestore.collection("users").document(FirebaseAuth.getInstance().getCurrentUser().getUid());
            df.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot documentSnapshot) {
                    if (documentSnapshot.getId() != null){
                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        if(user.isEmailVerified()){
                            if(documentSnapshot.getString("role").equals("Admin")){
                                Toast.makeText(Res_Login.this, "Welcome Barangay Admin!", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(getApplicationContext(), Brgy_Home.class));
                                finish();
                            }else if(documentSnapshot.getString("role").equals("Resident")){
                                Toast.makeText(Res_Login.this, "Welcome Resident!", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(getApplicationContext(), Res_Home.class));
                                finish();
                            }else if(documentSnapshot.getString("role").equals("Tadmin")){
                                Toast.makeText(Res_Login.this, "Welcome Town Admin", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(getApplicationContext(), TownAdmin.class));
                                finish();
                            }else{
                                Toast.makeText(Res_Login.this, "Error Role is Not Known", Toast.LENGTH_SHORT).show();
                            }
                        }else {
                            user.sendEmailVerification();
                            Toast.makeText(Res_Login.this, "Check your email to verify  for your account", Toast.LENGTH_SHORT).show();
                        }
                    }else{
                        Toast.makeText(Res_Login.this, "There is a problem with your account.", Toast.LENGTH_SHORT).show();
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    FirebaseAuth.getInstance().signOut();
                    startActivity(new Intent(getApplicationContext(), Res_Login.class));
                    finish();
                }
            });
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.login:
                LogIn();
                break;
            case R.id.signup:
                startActivity(new Intent(getApplicationContext(), Res_Signup.class));
                finish();
                break;
        }
    }

    private void LogIn() {
        email = etemail.getText().toString().trim();
        pass = etpass.getText().toString().trim();

        if (email.isEmpty()){
            etemail.setError("Email is required");
            etemail.requestFocus();
            return;
        }
        if (pass.isEmpty()){
            etpass.setError("Password is required");
            etpass.requestFocus();
            return;
        }
        if (pass.length() < 6){
            etpass.setError("Minimum of 6 characters is required");
            etpass.requestFocus();
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("requests")
                .document(email)
                .get()
                .addOnSuccessListener(documentSnapshot ->  {
                    if(documentSnapshot.exists()) {
                        etemail.setError("Your account is waiting for approval.");
                        Toast.makeText(this,"Please wait for the approval of Barangay Admin",Toast.LENGTH_LONG).show();
                    }else{
                        firebaseAuth.signInWithEmailAndPassword(email, pass).addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                            @Override
                            public void onSuccess(AuthResult authResult) {
                                savepRef(email,pass);
                                checkAuth(authResult.getUser().getUid());
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                //
                                Log.e("Error", e.getMessage());
                                Toast.makeText(Res_Login.this, "Error: "+e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
    }

    private void checkAuth(String uid) {
        DocumentReference df = firestore.collection("users").document(uid);
        df.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot.getId() != null){
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user.isEmailVerified()){
                        if(documentSnapshot.getString("role").equals("Admin")){
                            Toast.makeText(Res_Login.this, "Welcome Barangay Admin!", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(getApplicationContext(), Brgy_Home.class));
                            finish();
                        }else if(documentSnapshot.getString("role").equals("Resident")){
                            Toast.makeText(Res_Login.this, "Welcome Resident!", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(getApplicationContext(), Res_Home.class));
                            finish();
                        }else if(documentSnapshot.getString("role").equals("Tadmin")){
                            Toast.makeText(Res_Login.this, "Welcome Town Admin", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(getApplicationContext(), TownAdmin.class));
                            finish();
                        }else{
                            Toast.makeText(Res_Login.this, "Error Role is Not Known", Toast.LENGTH_SHORT).show();
                        }
                    }else{
                        user.sendEmailVerification();
                        Toast.makeText(Res_Login.this, "Check your email to verify account", Toast.LENGTH_SHORT).show();
                    }
                }else{
                    Toast.makeText(Res_Login.this, "Access Forbidden", Toast.LENGTH_SHORT).show();
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(Res_Login.this, "Error: "+e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}