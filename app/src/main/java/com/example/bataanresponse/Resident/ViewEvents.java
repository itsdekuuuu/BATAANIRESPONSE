package com.example.bataanresponse.Resident;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.example.bataanresponse.Brgy_Home;
import com.example.bataanresponse.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ViewEvents extends AppCompatActivity {

    LayoutInflater layoutInflater;
    StorageReference storageReference;
    FirebaseFirestore db;

    ViewGroup parent;

    Button btn_viewImage;

    TextView event_what,
            event_when,
            event_where,
            event_description;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_events);

        db = FirebaseFirestore.getInstance();

        layoutInflater = getLayoutInflater();
        parent = findViewById(R.id.resident_events_container);

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
        db.collection("events")
                .orderBy("when", Query.Direction.DESCENDING)
                .whereEqualTo("baranggay", brgyName)
                .whereEqualTo("municipal", mun)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        parent.removeAllViews();
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot eventDocument : task.getResult()) {
                                Timestamp timestamp = (Timestamp) eventDocument.getData().get("when");
                                String when = new SimpleDateFormat("yyyy-MM-dd").format(timestamp.toDate());
                                insertCards(
                                        eventDocument.getString("what"),
                                        eventDocument.getString("where"),
                                        when.concat(" (" + get12HrTime(eventDocument.getString("from")) + " - " + get12HrTime(eventDocument.getString("to")) + ")"),
                                        eventDocument.getString("description"),
                                        eventDocument.getString("images")
                                );
                            }
                        } else {
                            Log.d("TAG", "Error getting documents: ", task.getException());
                        }
                    }
                });
    }


    private String get12HrTime(String time) {
        final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        final Date dateObj;
        try {
            dateObj = sdf.parse(time);
            time = new SimpleDateFormat("hh:mm aa").format(dateObj);
            return time;
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void insertCards(String what, String where, String when, String description, String images) {
        View view = layoutInflater.inflate(R.layout.event_card_resident, parent, false);

        btn_viewImage = view.findViewById(R.id.btn_viewImage);

        event_what = view.findViewById(R.id.event_what);
        event_when = view.findViewById(R.id.event_when);
        event_where = view.findViewById(R.id.event_where);

        event_description = view.findViewById(R.id.event_description);
        event_description.setMovementMethod(new ScrollingMovementMethod());

        event_what.setText(what);
        event_when.setText(when);
        event_description.setText(description);
        event_where.setText(where);

        btn_viewImage.setVisibility(View.GONE);

        if (images != null) {
            String array_images[] = images.split(";");
            storageReference = FirebaseStorage.getInstance().getReference().child("images/" + array_images[0]);


            storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                @Override
                public void onSuccess(Uri uri) {
                    btn_viewImage.setVisibility(View.VISIBLE);

                    btn_viewImage.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            showPhoto(uri);
                        }
                    });

                    parent.addView(view);
                }
            });
        } else {
            parent.addView(view);
        }
    }

    private void showPhoto(Uri photoUri){
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(photoUri, "image/*");
        startActivity(intent);
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

    public void goBack(View view) {
        startActivity(new Intent(getApplicationContext(), Res_Home.class));
        finish();
    }



}
