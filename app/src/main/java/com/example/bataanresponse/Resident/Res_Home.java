package com.example.bataanresponse.Resident;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.example.bataanresponse.MainActivity;
import com.example.bataanresponse.R;
import com.example.bataanresponse.ViewReports;
import com.example.bataanresponse.Resident.ViewNotifications;
import com.example.bataanresponse.notification.MainApp;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

public class Res_Home extends AppCompatActivity implements View.OnClickListener {

    TextView btnlogout, btnviewevents, btnaddrep, btnviewrep;

    FirebaseAuth firebaseAuth;
    FirebaseFirestore firestore;
    MainApp app;

    @Override
    protected void onStart() {
        super.onStart();
        String message = getIntent().getStringExtra("message");
        if (message != null && !message.isEmpty()) {
            showSuccess(message);
        }
    }

    void showSuccess(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Success")
                .setIcon(android.R.drawable.ic_menu_info_details)
                .setCancelable(false)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null).show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_res__home);
        app = new MainApp();

        btnlogout = findViewById(R.id.rlogout);
        btnviewevents = findViewById(R.id.viewevents);
        btnaddrep = findViewById(R.id.addrep);
        btnviewrep = findViewById(R.id.viewrep);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        btnaddrep.setOnClickListener(this);
        btnviewrep.setOnClickListener(this);
        btnviewevents.setOnClickListener(this);
        btnlogout.setOnClickListener(this);

        setupHandler();
    }

    void setupHandler() {
        fetchReports();
        fetchBrgy();
    }

    void fetchReports() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("reports")
                .whereEqualTo("user_id", FirebaseAuth.getInstance().getCurrentUser().getUid())
                .get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    if (task.getResult().size() == 0) {
                        btnviewrep.setEnabled(false);
                    }
                } else {
                    Log.d("TAG", "Error getting documents: ", task.getException());
                }
            }
        });
    }

    void fetchBrgy() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db.collection("users")
                .document(userID)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            String brgyName = task.getResult().getString("baranggay");
                            String munName = task.getResult().getString("municipal");
                            app.subscribe(munName+brgyName);
                            fetchEvents(brgyName,munName);
                            subscribeToMyReports(userID);
                        } else {
                            Log.d("TAG", "Error getting documents: ", task.getException());
                        }
                    }
                });
    }

    void subscribeToMyReports(String myId){
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("reports")
                .whereEqualTo("user_id",myId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot snap : queryDocumentSnapshots) {
                        app.subscribe(snap.getId());
                    }
                });
    }

    public void onClickEditProfile(View view) {
        startActivity(new Intent(getApplicationContext(), EditProfile.class));
        finish();
    }

    void fetchEvents(String brgy,String mun) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("events")
                .whereEqualTo("baranggay", brgy)
                .whereEqualTo("municipal", mun)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            if (task.getResult().size() == 0) {
                                btnviewevents.setEnabled(false);
                            }
                        } else {
                            Log.d("TAG", "Error getting documents: ", task.getException());
                        }
                    }
                });
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.addrep:
                startActivity(new Intent(getApplicationContext(), AddReport.class));
                finish();
                break;
            case R.id.viewrep:
                String resid = firebaseAuth.getCurrentUser().getUid();
                Intent intent = new Intent(getApplicationContext(), ViewReports.class);
                intent.putExtra("resid", resid);
                startActivity(intent);
                finish();
                break;
            case R.id.viewevents:
                Log.d("here","Events");
                startActivity(new Intent(getApplicationContext(), ViewEvents.class));
                finish();
                break;
            case R.id.viewNotification:
                startActivity(new Intent(getApplicationContext(), ViewNotifications.class));
                finish();
                break;
            case R.id.rlogout:
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(getApplicationContext(), MainActivity.class));
                finish();
                break;

        }
    }
}