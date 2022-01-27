package com.example.bataanresponse;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.TextView;

import com.example.bataanresponse.AddEvent;
import com.example.bataanresponse.Applicant;
import com.example.bataanresponse.FileChooserFragment;
import com.example.bataanresponse.MainActivity;
import com.example.bataanresponse.R;
import com.example.bataanresponse.Resident.EditProfile;
import com.example.bataanresponse.ViewReports;
import com.example.bataanresponse.ViewResident;
import com.example.bataanresponse.notification.MainApp;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;


    public class TownAdmin extends AppCompatActivity {

        FrameLayout importBrgy;
        FrameLayout importMunicipal;

        FirebaseAuth firebaseAuth;
        FirebaseFirestore firestore;

        private FileChooserFragment fragment;

        String role, userBrgy, userMunicipal;

        boolean isSuper = false;


        @Override
        protected void onStart() {
            super.onStart();
            String message = getIntent().getStringExtra("message");
            if (message != null && !message.isEmpty()) {
                showSuccess(message);
            }
        }

        void subscribeToMyTopic(String myID) {
            MainApp app = new MainApp();
            app.subscribe(myID);
            app.subscribe(userMunicipal + userBrgy);
        }

        private void showSuccess(String message) {
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
            setContentView(R.layout.activity_town_admin);
            fetchUser();
            importBrgy = findViewById(R.id.importBaranggay);
            importMunicipal = findViewById(R.id.importMunicipal);
            firebaseAuth = FirebaseAuth.getInstance();
            firestore = FirebaseFirestore.getInstance();

            FragmentManager fragmentManager = this.getSupportFragmentManager();
            this.fragment = (FileChooserFragment) fragmentManager.findFragmentById(R.id.fragment_fileChooser);

        }

        private void fetchUser() {
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
                            if (documentSnapshot.getBoolean("super") != null &&
                                    documentSnapshot.getBoolean("super")) {
                                importBrgy.setVisibility(View.VISIBLE);
                                importMunicipal.setVisibility(View.VISIBLE);
                            } else {
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

        public void onClickLogOut(View view) {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(getApplicationContext(), MainActivity.class));
            finish();
        }



        public void onClickBarangayAdmins(View view) {
            startActivity(new Intent(getApplicationContext(), ViewBarangayAdmins.class));
            finish();
        }
    }