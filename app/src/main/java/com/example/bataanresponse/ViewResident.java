package com.example.bataanresponse;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

public class ViewResident extends AppCompatActivity {
    FirebaseFirestore db;

    TableLayout tableLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_resident);
        tableLayout = findViewById(R.id.tableResidents);
        db = FirebaseFirestore.getInstance();

        fetchUserInfo();
    }

    void fetchUserInfo() {
        db.collection("users")
                .document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        fetchResidents(documentSnapshot.getString("baranggay"), documentSnapshot.getString("municipal"));
                    }
                });
    }


    void fetchResidents(String brgy, String municipal) {
        db.collection("users")
                .whereEqualTo("role", "Resident")
                .whereEqualTo("baranggay", brgy)
                .whereEqualTo("municipal", municipal)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot eventDocument : task.getResult()) {
                                db.collection("reports")
                                        .whereEqualTo("user_id",eventDocument.getId())
                                        .get()
                                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                            @Override
                                            public void onComplete(@NonNull Task<QuerySnapshot> reportTask) {
                                                makeRow(eventDocument.getString("full_name"),
                                                        eventDocument.getString("email"),
                                                        eventDocument.getString("contact_number"),
                                                        reportTask.getResult().size()
                                                );
                                            }
                                        });
                            }
                        }
                    }
                });
    }

    /*===========BUTTON HANDLER=====================*/
    public void onReturnHandler(View view){
        startActivity(new Intent(getApplicationContext(), Brgy_Home.class));
        finish();
    }


    /*===========TABLE SETTINGS====================*/
    private void makeRow(String fullName, String email, String cNumber, int totalReports) {
        TableRow tr = new TableRow(this);
        tr.setPadding(10, 8, 10, 8);
        tr.setLayoutParams(new TableLayout.LayoutParams(TableLayout.LayoutParams.WRAP_CONTENT, TableLayout.LayoutParams.WRAP_CONTENT));
        tr.addView(columnData(fullName));
        tr.addView(columnData(email));
        tr.addView(columnData(cNumber));
        tr.addView(columnData(String.valueOf(totalReports)));
        tableLayout.addView(tr, new TableLayout.LayoutParams(TableLayout.LayoutParams.FILL_PARENT, TableLayout.LayoutParams.WRAP_CONTENT));
    }

    private TextView columnData(String value) {
        String txt = value == null || value.isEmpty() ? "Not Specified" : value;
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