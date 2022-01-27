package com.example.bataanresponse;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.bataanresponse.Resident.Res_Login;
import com.example.bataanresponse.Resident.Res_Signup;
import com.example.bataanresponse.notification.MainApp;
import com.example.bataanresponse.notification.MainKotlinApp;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    Button btn_user, btn_admin;
    private static final String SUPER_EMAIL = "pangilinanangel20@gmail.com";
    private static final String SUPER_PASSWORD = "123456";
    private static final String SUPER_NAME = "Bataan Admin";
    private static final String SUPER_BRGY = "Imelda";
    private static final String SUPER_MUNICIPAL = "Samal";
    private static final String SUPER_CONTACT_NUMBER = "09123456789";
    FirebaseFirestore db;
    String brgyID;
//    MainApp app;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btn_user = findViewById(R.id.btnuser);
        btn_admin = findViewById(R.id.btnadmin);
        btn_user.setOnClickListener(this);
        btn_admin.setOnClickListener(this);
        db = FirebaseFirestore.getInstance();
        setup();


    }

    void checkMainUser() {
        db.collection("users")
                .whereEqualTo("email", SUPER_EMAIL)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        if (queryDocumentSnapshots.size() == 0) {
                            createSuperUser();
                        }
                    }
                });
    }

    void initAddress() {
        Map<String, Object> municipalMap = new HashMap<>();
        municipalMap.put("title",SUPER_MUNICIPAL);
        db.collection("municipalities")
                .add(municipalMap)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Map<String, Object> data = new HashMap<>();
                        data.put("title", SUPER_BRGY);
                        data.put("municipality", SUPER_MUNICIPAL);
                        db.collection("baranggays").add(data)
                                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                    @Override
                                    public void onSuccess(DocumentReference documentReference) {
                                        checkMainUser();
                                    }
                                });
                    }
                });
    }

    void setup() {
        db.collection("baranggays")
                .whereEqualTo("municipality", SUPER_MUNICIPAL)
                .whereEqualTo("title", SUPER_BRGY)
                .limit(1)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        if (queryDocumentSnapshots.size() == 0) {
                            initAddress();
//                            Log.e("NOTICE", "There is no such baranggay in municipality");
//                            Toast.makeText(getBaseContext(), "NOTICE: SPECIFIED BRGY ADMIN IS NOT DEFINE, FAILED TO CREATE SUPER USER.", Toast.LENGTH_SHORT).show();
                        }else{
                            checkMainUser();
                        }
                    }
                });
    }

    /*Create later*/
    void createSuperUser() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        auth.createUserWithEmailAndPassword(SUPER_EMAIL, SUPER_PASSWORD).addOnSuccessListener(new OnSuccessListener<AuthResult>() {
            @Override
            public void onSuccess(AuthResult authResult) {
                FirebaseUser user = auth.getCurrentUser();
                //Saving data into the firestore database
                DocumentReference document = db.collection("users").document(user.getUid());
                Map<String, Object> superUser = new HashMap<>();
                superUser.put("contact_number", SUPER_CONTACT_NUMBER);
                superUser.put("email", SUPER_EMAIL);
                superUser.put("full_name", SUPER_NAME);
                superUser.put("role", "Admin");
                superUser.put("municipal", SUPER_MUNICIPAL);
                superUser.put("baranggay", SUPER_BRGY);
                superUser.put("super", true);
                document.set(superUser).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(MainActivity.this,"Super User Initiated", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(getApplicationContext(), Res_Login.class));
                        finish();
                    }
                });
            }
        });
    }

    @Override
    public void onClick(View view) {
        startActivity(new Intent(getApplicationContext(), Res_Login.class));
        finish();
    }

    public void onRequestClick(View view) {
        startActivity(new Intent(getApplicationContext(), Res_Signup.class));
        finish();
    }
}