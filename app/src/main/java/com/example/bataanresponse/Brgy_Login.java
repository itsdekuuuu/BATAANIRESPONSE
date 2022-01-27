package com.example.bataanresponse;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class Brgy_Login extends AppCompatActivity implements View.OnClickListener {

    EditText etemail, etpass;
    Button btnlogin, btnsignup;
    CheckBox checkBox;

    FirebaseAuth firebaseAuth;
    FirebaseFirestore firestore;

    String email, pass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_brgy__login);

        etemail = findViewById(R.id.email);
        etpass = findViewById(R.id.password);
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

    @Override
    protected void onStart() {
        super.onStart();
        if (FirebaseAuth.getInstance().getCurrentUser() != null){
            DocumentReference df = firestore.collection("Admin").document(FirebaseAuth.getInstance().getCurrentUser().getUid());
            df.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot documentSnapshot) {
                    //
                    if (documentSnapshot.getString("Admin") != null){
                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        if(user.isEmailVerified()){
                            Toast.makeText(Brgy_Login.this, "Login Successful", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(getApplicationContext(), Brgy_Home.class));
                        finish();
                        }else {
                            user.sendEmailVerification();
                            Toast.makeText(Brgy_Login.this, "Check your email to verify  for your account", Toast.LENGTH_SHORT).show();
                        }
                    }else{
                        Toast.makeText(Brgy_Login.this, "Account can't find in Admins", Toast.LENGTH_SHORT).show();
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    //
                    FirebaseAuth.getInstance().signOut();
                    startActivity(new Intent(getApplicationContext(), Brgy_Login.class));
                    finish();
                }
            });
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.login:
                Login();
                break;
            case R.id.signup:
                startActivity(new Intent(getApplicationContext(), Brgy_Signup.class));
                finish();
                break;
        }
    }

    private void Login() {
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

        //firebase method in singing in the user
        firebaseAuth.signInWithEmailAndPassword(email, pass).addOnSuccessListener(new OnSuccessListener<AuthResult>() {
            @Override
            public void onSuccess(AuthResult authResult) {
                //
                checkifadmin(authResult.getUser().getUid());
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                //
                Toast.makeText(Brgy_Login.this, "Error: "+e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void checkifadmin(String uid) {
        //
        DocumentReference df = firestore.collection("Admin").document(uid);
        //extract data from the documentreference
        df.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                //
                Log.d("TAG","onSuccess: "+ documentSnapshot.getData());

                if (documentSnapshot.getString("Admin") != null){
                    //
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user.isEmailVerified()){
                        Toast.makeText(Brgy_Login.this, "Login Successfully", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(getApplicationContext(), Brgy_Home.class));
                        finish();
                    }else{
                        user.sendEmailVerification();
                        Toast.makeText(Brgy_Login.this, "Check your email to verify your account", Toast.LENGTH_LONG).show();
                    }

                }else{
                    Toast.makeText(Brgy_Login.this, "Account can't find in Admin", Toast.LENGTH_SHORT).show();
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                //
                Toast.makeText(Brgy_Login.this, "Error: "+e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}