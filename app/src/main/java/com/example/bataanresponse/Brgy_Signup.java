package com.example.bataanresponse;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.bataanresponse.Resident.Res_Signup;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class Brgy_Signup extends AppCompatActivity implements View.OnClickListener {

    EditText etfname, etbrgy, etcity, etcnum, etemail, etpass;
    Button btnsignup, btnlogin;

    FirebaseAuth firebaseAuth;
    FirebaseFirestore firestore;

    String fname, brgy, city, cnum, email, pass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_brgy__signup);

        etfname = findViewById(R.id.fullname);
        etbrgy = findViewById(R.id.brgy);
        etcity = findViewById(R.id.city);
        etcnum = findViewById(R.id.cnum);
        etemail = findViewById(R.id.email);
        etpass = findViewById(R.id.password);
        btnsignup = findViewById(R.id.signup);
        btnlogin = findViewById(R.id.login);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        btnsignup.setOnClickListener(this);
        btnlogin.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.signup:
                    RegisterAdmin();
                break;
            case R.id.login:
                startActivity(new Intent(getApplicationContext(), Brgy_Login.class));
                finish();
                break;
        }
    }

    private void RegisterAdmin() {
        fname = etfname.getText().toString().trim();
        brgy = etbrgy.getText().toString().trim();
        city = etcity.getText().toString().trim();
        cnum = etcnum.getText().toString().trim();
        email = etemail.getText().toString().trim();
        pass = etpass.getText().toString().trim();

        if (fname.isEmpty()){
            etfname.setError("Full name is Required");
            etfname.requestFocus();
            return;
        }
        if (brgy.isEmpty()){
            etbrgy.setError("Barangay is Required");
            etbrgy.requestFocus();
            return;
        }
        if (city.isEmpty()){
            etcity.setError("City is Required");
            etcity.requestFocus();
            return;
        }
        if (cnum.isEmpty()){
            etcnum.setError("Contact Number is Required");
            etcnum.requestFocus();
            return;
        }
        if (email.isEmpty()){
            etemail.setError("Email is Required");
            etemail.requestFocus();
            return;
        }
        if (pass.isEmpty()){
            etpass.setError("Password is Required");
            etpass.requestFocus();
            return;
        }
        if (pass.length() < 6){
            etpass.setError("Minimum of 6 characters is required");
            etpass.requestFocus();
            return;
        }

        firebaseAuth.createUserWithEmailAndPassword(email, pass).addOnSuccessListener(new OnSuccessListener<AuthResult>() {
            @Override
            public void onSuccess(AuthResult authResult) {
                //
                FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();

                Toast.makeText(Brgy_Signup.this, "Account Created", Toast.LENGTH_SHORT).show();
                DocumentReference df = firestore.collection("Admin").document(firebaseUser.getUid());
                Map<String, Object> adminInfo = new HashMap<>();
                adminInfo.put("Fullname", fname);
                adminInfo.put("Barangay", brgy);
                adminInfo.put("City", city);
                adminInfo.put("ContactNumber", cnum);
                adminInfo.put("Email", email);
                adminInfo.put("Admin", "1");

                df.set(adminInfo).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        //
                        startActivity(new Intent(getApplicationContext(), Brgy_Login.class));
                        finish();
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //
                        Toast.makeText(Brgy_Signup.this, "Error: "+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                //
                Log.e("ERROR PO", e.getMessage());
                Toast.makeText(Brgy_Signup.this, "Failed to Register Account", Toast.LENGTH_SHORT).show();
            }
        });
    }
}