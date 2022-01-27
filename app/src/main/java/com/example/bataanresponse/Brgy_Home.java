package com.example.bataanresponse;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridLayout;

import com.example.bataanresponse.Resident.BarangayViewEvents;
import com.example.bataanresponse.Resident.EditProfile;
import com.example.bataanresponse.Resident.ViewEvents;
import com.example.bataanresponse.notification.MainApp;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

public class Brgy_Home extends AppCompatActivity implements View.OnClickListener {

    Button btnlogout, btnpostevent, btnviewrep, btnviewevents;

    FrameLayout importBrgy;
    FrameLayout importMunicipal;

    FirebaseAuth firebaseAuth;
    FirebaseFirestore firestore;

    private FileChooserFragment fragment;

    String role, userBrgy,userMunicipal;

    boolean isSuper = false;

    private boolean pageToggle = false;

    @Override
    protected void onStart() {
        super.onStart();
        String message = getIntent().getStringExtra("message");
        if (message != null && !message.isEmpty()) {
            showSuccess(message);
        }
    }

    void subscribeToMyTopic(String myID){
        MainApp app = new MainApp();
        app.subscribe(myID);
        app.subscribe(userMunicipal+userBrgy);
    }

    public void onViewApplicants(View view){
        startActivity(new Intent(getApplicationContext(), Applicant.class));
        finish();
    }

    void showSuccess(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Success")
                .setIcon(android.R.drawable.ic_menu_info_details)
                .setCancelable(false)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null).show();
    }

    public void onNextPage(View view){
        GridLayout page1 = findViewById(R.id.firstPage);
        GridLayout page2 = findViewById(R.id.secondPage);
        page1.setVisibility(View.GONE);
        page2.setVisibility(View.GONE);
        pageToggle = !pageToggle;
        if(pageToggle){
            page1.setVisibility(View.VISIBLE);
        }else{
            page2.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_brgy__home);
        fetchUser();
        btnlogout = findViewById(R.id.logout);
        btnpostevent = findViewById(R.id.postevent);
        btnviewrep = findViewById(R.id.viewrep);
        btnviewevents = findViewById(R.id.viewevents);
        importBrgy = findViewById(R.id.importBaranggay);
        importMunicipal = findViewById(R.id.importMunicipal);
        importBrgy.setVisibility(View.GONE);
        importMunicipal.setVisibility(View.GONE);
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        btnpostevent.setOnClickListener(this);
        btnviewrep.setOnClickListener(this);
        btnviewevents.setOnClickListener(this);
        btnlogout.setOnClickListener(this);
        findViewById(R.id.viewResidentsBtn).setOnClickListener(this);

        FragmentManager fragmentManager = this.getSupportFragmentManager();
        this.fragment = (FileChooserFragment) fragmentManager.findFragmentById(R.id.fragment_fileChooser);
    }

    void fetchUser() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db.collection("users")
                .document(userID)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        role = documentSnapshot.getString("role");
                        userBrgy = documentSnapshot.getString("baranggay");
                        userMunicipal = documentSnapshot.getString("municipal");
                        if(documentSnapshot.getBoolean("super") != null &&
                        documentSnapshot.getBoolean("super")) {
                            importBrgy.setVisibility(View.VISIBLE);
                            importMunicipal.setVisibility(View.VISIBLE);
                        }else{
                            Log.e("OKAY", "HINDI SUPER");
                        }
                        subscribeToMyTopic(userID);
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
        switch (view.getId()){
            case R.id.postevent:
                startActivity(new Intent(getApplicationContext(), AddEvent.class));
                finish();
                break;
            case R.id.viewrep:
                Intent intent = new Intent(getApplicationContext(), ViewReports.class);
                intent.putExtra("baranggay", userBrgy);
                intent.putExtra("municipal", userMunicipal);
                intent.putExtra("role", role);
                startActivity(intent);
                finish();
                break;
            case R.id.viewevents:
                startActivity(new Intent(getApplicationContext(), BarangayViewEvents.class));
                finish();
                break;
            case R.id.logout:
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(getApplicationContext(), MainActivity.class));
                finish();
                break;
            case R.id.viewResidentsBtn:
                startActivity(new Intent(getApplicationContext(), ViewResident.class));
                finish();
                break;
        }
    }

}