package com.example.bataanresponse.Resident;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.bataanresponse.R;
import com.example.bataanresponse.ViewReports;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ViewNotifications extends AppCompatActivity {

    FirebaseFirestore db;

    Button btnback;

    LinearLayout layout;
    FirebaseAuth firebaseAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_notifications);

        db = FirebaseFirestore.getInstance();
        btnback = findViewById(R.id.repback);
        firebaseAuth = FirebaseAuth.getInstance();
        layout = findViewById(R.id.notificationContainer);

        btnback.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ViewNotifications.this, Res_Home.class);
                startActivity(intent);
            }
        });
        handleEvents();
    }

    void handleEvents() {
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
                            fetchMyEvents(brgyName,munName);
                        } else {
                            Log.d("TAG", "Error getting documents: ", task.getException());
                        }
                    }
                });
    }

    void fetchMyEvents(String brgyName, String mun){
        String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db.collection("notifications")
                .whereEqualTo("barangay", brgyName)
                .whereEqualTo("municipal", mun)
                .whereEqualTo("type", "event") //!UPDATE
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot eventDocument : task.getResult()) {
                                makeRow(
                                        eventDocument.getString("title"),
                                        eventDocument.getString("description"),
                                        eventDocument.getString("type")
                                );
                            }
                        } else {
                            Log.d("TAG", "Error getting documents: ", task.getException());
                        }
                    }
                });

        db.collection("notification_users")
                .whereEqualTo("user_id",userID)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            Log.e("TAGError", String.valueOf(task.getResult().size()));
                            Log.e("TAGError", userID);
                            for (QueryDocumentSnapshot eventDocument : task.getResult()) {
                                makeRow(
                                        eventDocument.getString("title"),
                                        eventDocument.getString("description"),
                                        eventDocument.getString("type")
                                );
                            }
                        } else {
                            Log.d("TAG", "Error getting documents: ", task.getException());
                        }
                    }
                });
    }


    private void makeRow(String title, String desc,String type) {
        View view = getLayoutInflater().inflate(R.layout.card,null);

        TextView titleView = view.findViewById(R.id.name);
        titleView.setText(title);
        TextView descView = view.findViewById(R.id.description);
        descView.setText(desc);

        layout.addView(view);
    }

    private TextView columnData(String value) {
        String txt = value.isEmpty() ? "Not Specified" : value;
        TextView data = new TextView(this);
        data.setMaxWidth(400);
        data.setGravity(Gravity.START);
        data.setPadding(20, 20, 20, 20);
        data.setTextSize(12);
        data.setTextColor(Color.BLACK);
        data.setText(txt);
        return data;
    }



}