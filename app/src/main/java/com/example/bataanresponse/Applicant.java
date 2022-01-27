package com.example.bataanresponse;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.example.bataanresponse.Resident.EditProfile;
import com.example.bataanresponse.notification.MainApp;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.common.collect.Table;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class Applicant extends AppCompatActivity {

    FirebaseFirestore db = FirebaseFirestore.getInstance();
    TableLayout tableLayout;
    MainApp notify;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_applicant);
        tableLayout = findViewById(R.id.tableResidents);
        notify = new MainApp();
        fetchUserInfo();
    }

    void fetchUserInfo() {
        db.collection("users")
                .document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        fetchApplicants(documentSnapshot.getString("baranggay"), documentSnapshot.getString("municipal"));
                    }
                });
    }


    public void onReturnView(View view) {
        startActivity(new Intent(getApplicationContext(), Brgy_Home.class));
        finish();
    }

    void fetchApplicants(String brgy, String municipal) {
        db.collection("requests")
                .whereEqualTo("baranggay", brgy)
                .whereEqualTo("municipal", municipal)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    cleanTable(tableLayout);
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        makeRow(doc.getString("full_name"), doc.getId(), doc.getString("contact_number"));
                    }
                });
    }

    /*===========TABLE SETTINGS====================*/
    private void makeRow(String fullName, String email, String cNumber) {
        TableRow tr = new TableRow(this);
        tr.setPadding(10, 8, 10, 8);
        tr.setLayoutParams(new TableLayout.LayoutParams(TableLayout.LayoutParams.WRAP_CONTENT, TableLayout.LayoutParams.WRAP_CONTENT));
        tr.addView(columnData(fullName));
        tr.addView(columnData(email));
        tr.addView(columnData(cNumber));
        Button btn = new Button(this);
        btn.setText("Approve");
        btn.setBackgroundColor(Color.parseColor("#96c896"));
        btn.setOnClickListener(v -> {
            askApprovalConfirm(email);
        });
        tr.addView(btn);
        Button btnDelete = new Button(this);
        btnDelete.setText("Delete");
        btnDelete.setBackgroundColor(Color.RED);
        btnDelete.setOnClickListener(v -> {
            askForDelete(email);
        });
        tr.addView(btnDelete);
        tableLayout.addView(tr, new TableLayout.LayoutParams(TableLayout.LayoutParams.FILL_PARENT, TableLayout.LayoutParams.WRAP_CONTENT));
    }

    FirebaseAuth auth = FirebaseAuth.getInstance();

    private void cleanTable(TableLayout tableLayout) {
        int childCount = tableLayout.getChildCount();
        // Remove all rows except the first one
        if (childCount > 1) {
            tableLayout.removeViews(1, childCount - 1);
        }
    }

    void askForDelete(String email) {
        new AlertDialog.Builder(Applicant.this)
                .setTitle("Reject Applicant")
                .setMessage("Are you sure you want to delete this applicant?")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("YES", (dialog, whichButton) -> {
                    db.collection("requests").document(email)
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                notify.sendNotification(email.replace("@",""),"Request Denied!","Your request for account has been declined.");
                                fetchUserInfo();
                            });
                })
                .setNegativeButton(android.R.string.no, null).show();
    }


    void askApprovalConfirm(String email) {
        SharedPreferences settings = getApplicationContext().getSharedPreferences("BATAAN_RESPONSE", 0);
        String myUsername = settings.getString("username","");
        String myPassword = settings.getString("password","");
        if(myPassword == null || myPassword.equals("")){
            alertShow("Please re-signin, to perform this operation.");
            return;
        }

        new AlertDialog.Builder(Applicant.this)
                .setTitle("Approval Message")
                .setMessage("Are you sure you want to approve this user?")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("YES", (dialog, whichButton) -> {
                    db.collection("requests").document(email)
                            .get()
                            .addOnSuccessListener(documentSnapshot -> {
                                db.collection("users")
                                        .whereEqualTo("email", email)
                                        .get()
                                        .addOnSuccessListener(queryDocumentSnapshots -> {
                                            if (queryDocumentSnapshots.size() > 0) {
                                                alertShow("User with this email already exists.");
                                            } else {
                                                auth.createUserWithEmailAndPassword(email, documentSnapshot.getString("password"))
                                                        .addOnSuccessListener(authResult -> {
                                                            Map<String, Object> docu = documentSnapshot.getData();
                                                            docu.remove("password");
                                                            docu.put("email",email);
                                                            db.collection("users")
                                                                    .document(authResult.getUser().getUid())
                                                                    .set(docu)
                                                                    .addOnSuccessListener(aVoid -> {
                                                                        auth.signOut();
                                                                        auth.signInWithEmailAndPassword(myUsername,myPassword)
                                                                                .addOnSuccessListener(authResult1 -> {
                                                                                    fetchUserInfo();
                                                                                });
                                                                        db.collection("requests")
                                                                                .document(email)
                                                                                .delete();
                                                                        notify.sendNotification(email.replace("@",""),"Request Approved!","Your account has been approved.");
                                                                        successShow("User has been approved successfully");
                                                                    });
                                                        }).addOnFailureListener(e -> {
                                                    alertShow(e.getMessage());
                                                });
                                            }
                                        });

                            });
                })
                .setNegativeButton(android.R.string.no, null).show();
    }

    private void successShow(String message) {
        new AlertDialog.Builder(Applicant.this)
                .setTitle("Success")
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .setIcon(android.R.drawable.ic_dialog_info)
                .show();
    }

    private void alertShow(String message) {
        new AlertDialog.Builder(Applicant.this)
                .setTitle("Error")
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
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