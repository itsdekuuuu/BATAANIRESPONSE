package com.example.bataanresponse.Resident;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.bataanresponse.MainActivity;
import com.example.bataanresponse.R;
import com.example.bataanresponse.notification.MainApp;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Res_Signup extends AppCompatActivity implements View.OnClickListener {

    EditText etfname, etbrgy, etcity, etcnum, etemail, etpass;
    Button btnsignup, btnlogin;

    FirebaseAuth firebaseAuth;
    FirebaseFirestore firestore;

    String fname, brgy, city, cnum, email, pass;

    private ArrayList<String> municipalSpinnerArray;
    private Spinner municipalSpinner;
    private String selectedMunicipal;

    private ArrayList<String> brgySpinnerArray;
    private Spinner brgySpinner;
    private String selectedBrgy;
    MainApp app;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_res__signup);
         app = new MainApp();
        etfname = findViewById(R.id.r_fname);
        etcnum = findViewById(R.id.r_cnum);
        etemail = findViewById(R.id.r_email);
        etpass = findViewById(R.id.r_password);
        btnsignup = findViewById(R.id.r_signup);
        btnlogin = findViewById(R.id.r_login);
        brgySpinner = findViewById(R.id.spinnerBrgy);
        municipalSpinner = findViewById(R.id.spinnerMunicipal);
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        btnsignup.setOnClickListener(this);
        btnlogin.setOnClickListener(this);
        loadMunicipalitiesSpinner();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.r_signup:
                SignUp();
                break;
            case R.id.r_login:
                startActivity(new Intent(getApplicationContext(), Res_Login.class));
                finish();
                break;
        }
    }


    private void loadMunicipalitiesSpinner() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("municipalities")
                .orderBy("title")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            setupMunicipalSpinner(task);
                        } else {
                            Log.d("TAG", "Error getting documents: ", task.getException());
                        }

                    }
                });
    }

    private void setupMunicipalSpinner(Task<QuerySnapshot> task) {
        // you need to have a list of data that you want the spinner to display
        municipalSpinnerArray = new ArrayList<String>();

        for (QueryDocumentSnapshot document : task.getResult()) {
            municipalSpinnerArray.add(document.getString("title"));
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this, R.layout.custom_spinner_item, municipalSpinnerArray);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        municipalSpinner.setAdapter(adapter);
        municipalSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedMunicipal = parent.getSelectedItem().toString();
                loadBrgySpinnerData(selectedMunicipal);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void loadBrgySpinnerData(String municipal) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("baranggays")
                .orderBy("title")
                .whereEqualTo("municipality", municipal)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            setupBrgySpinner(task);
                        } else {
                            Log.d("TAG", "Error getting documents: ", task.getException());
                        }

                    }
                });
    }

    private void setupBrgySpinner(Task<QuerySnapshot> task) {
        brgySpinnerArray = new ArrayList<String>();
        for (QueryDocumentSnapshot document : task.getResult()) {
            brgySpinnerArray.add(document.getString("title"));
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this, R.layout.custom_spinner_item, brgySpinnerArray);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        brgySpinner.setAdapter(adapter);
        adapter.notifyDataSetChanged();

        brgySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedBrgy = parent.getSelectedItem().toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }


    private void SignUp() {
        fname = etfname.getText().toString().trim();
        cnum = etcnum.getText().toString().trim();
        email = etemail.getText().toString().trim();
        pass = etpass.getText().toString().trim();





        if (fname.isEmpty()){
            etfname.setError("Full name is Required");
            etfname.requestFocus();
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

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("requests")
                .document(email)
                .get()
                .addOnSuccessListener(documentSnapshot ->  {
                    if(documentSnapshot.exists()) {
                        etemail.setError("This email has already requested an account.");
                        Toast.makeText(this,"Please wait for the approval of baranggay admin",Toast.LENGTH_LONG).show();
                    }else{
                        Map<String, Object> resInfo = new HashMap<>();
                        resInfo.put("contact_number", cnum);
                        resInfo.put("baranggay", brgySpinner.getSelectedItem().toString());
                        resInfo.put("municipal", municipalSpinner.getSelectedItem().toString());
                        resInfo.put("full_name", fname);
                        resInfo.put("role", "Resident");
                        resInfo.put("password", pass);

                        db.collection("requests").document(email)
                                .set(resInfo)
                                .addOnSuccessListener(documentReference -> {
                                    app.subscribe(email.replace("@",""));
                                    Toast.makeText(this, "Your request has been sent, please wait for approval.",Toast.LENGTH_LONG).show();
                                    startActivity(new Intent(getApplicationContext(), MainActivity.class));
                                    finish();
                                });
                    }
        });

    }
}