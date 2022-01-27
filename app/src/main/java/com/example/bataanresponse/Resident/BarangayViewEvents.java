package com.example.bataanresponse.Resident;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
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
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.example.bataanresponse.Brgy_Home;
import com.example.bataanresponse.R;
import com.example.bataanresponse.ViewReports;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
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
import com.google.type.DateTime;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class BarangayViewEvents extends AppCompatActivity {

    LayoutInflater layoutInflater;

    StorageReference storageReference;
    FirebaseFirestore db;

    ViewGroup parent;

    Button btn_viewImage,
        btn_editEvent;

    EditText txt_event_what,
        txt_event_when,
        txt_event_where,
        txt_event_description;

    TextView event_what,
            event_when,
            event_where,
            event_description;

    AlertDialog.Builder dialogBuilder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barangay_view_events);

        db = FirebaseFirestore.getInstance();

        layoutInflater = getLayoutInflater();
        parent = findViewById(R.id.barangay_events_container);

        handleEvents();
    }

    /**
     * Get the user's barangay and municipality in order to fetch proper events.
     */
    void handleEvents() {
        String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db.collection("users")
                .document(userID)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            String barangay = task.getResult().getString("baranggay");
                            String municipality = task.getResult().getString("municipal");

                            fetchMyEvents(barangay, municipality);
                        } else {
                            Log.d("TAG", "Error getting documents: Hello world");
                        }
                    }
                });
    }

    /**
     *
     * @param brgyName
     * @param mun
     */
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
                                String when = new SimpleDateFormat("MM/dd/yyyy").format(timestamp.toDate());
                                String rawWhen = new SimpleDateFormat("yyyy-MM-dd").format(timestamp.toDate());

                                insertCards(
                                        eventDocument.getString("what"),
                                        eventDocument.getString("where"),
                                        eventDocument.getString("from"),
                                        eventDocument.getString("to"),
                                        eventDocument.getString("description"),
                                        when.concat(" (" + get12HrTime(eventDocument.getString("from")) + " - " + get12HrTime(eventDocument.getString("to")) + ")"),
                                        rawWhen,
                                        eventDocument.getString("images"),
                                        eventDocument.getId()
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

    final Calendar myCalendar = Calendar.getInstance();

    private final DatePickerDialog.OnDateSetListener myDateListener = new DatePickerDialog.OnDateSetListener() {
        @Override
        public void onDateSet(DatePicker datePicker, int arg1, int arg2, int arg3) {
            myCalendar.set(datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth());
            String myFormat = "yyyy-MM-dd";
            SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.TAIWAN);
            txt_event_when.setText(sdf.format(myCalendar.getTime()));
        }
    };

    public void onWhenClick(View view) {
        new DatePickerDialog(this, myDateListener, myCalendar.get(Calendar.YEAR),
                myCalendar.get(Calendar.MONTH),
                myCalendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void handleOnEdit(String what, String where, String from, String to, String eventDescription, String when, String eventId) {
        layoutInflater = LayoutInflater.from(BarangayViewEvents.this);
        LinearLayout myLayout = (LinearLayout) layoutInflater.inflate(R.layout.update_event, null);
        dialogBuilder = new AlertDialog.Builder(BarangayViewEvents.this);
        dialogBuilder.setView(myLayout);
        EditText whatTxt = myLayout.findViewById(R.id.txt_event_what);
        EditText whereTxt = myLayout.findViewById(R.id.txt_event_where);
        EditText fromTxt = myLayout.findViewById(R.id.fromTxt);
        EditText toTxt = myLayout.findViewById(R.id.toTxt);
        EditText eventDescriptionTxt = myLayout.findViewById(R.id.txt_event_description);
        txt_event_when = myLayout.findViewById(R.id.txt_event_when);

        whatTxt.setText(what);
        whereTxt.setText(where);
        fromTxt.setText(from);
        toTxt.setText(to);
        eventDescriptionTxt.setText(eventDescription);
        txt_event_when.setText(when);

        fromTxt.setOnClickListener(this::timeFieldClick);
        toTxt.setOnClickListener(this::timeFieldClick);
        txt_event_when.setOnClickListener(this::onWhenClick);

        dialogBuilder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                updateEvent(eventId,
                        whatTxt.getText().toString(),
                        whereTxt.getText().toString(),
                        fromTxt.getText().toString(),
                        toTxt.getText().toString(),
                        eventDescriptionTxt.getText().toString(),
                        txt_event_when.getText().toString(),
                        dialog);
            }
        });

        // Cancel button pressed
        dialogBuilder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        dialogBuilder.show();
    }

    private void updateEvent(String eventId, String what, String where, String from, String to, String eventDescription, String when, DialogInterface dialog) {

        Map<String, Object> eventInfo = new HashMap<>();
        eventInfo.put("what", what);
        eventInfo.put("where", where);
        eventInfo.put("from", from);
        eventInfo.put("to", to);
        eventInfo.put("description", eventDescription);
        eventInfo.put("when", getDateFromString(when));
        Toast.makeText(BarangayViewEvents.this, when, Toast.LENGTH_LONG).show();

        db.collection("events").document(eventId)
                .update(eventInfo)
                .addOnSuccessListener(aVoid -> {
                    dialog.dismiss();
                    showMessage("Event has been updated!");
                    handleEvents();
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        e.printStackTrace();
                    }
                });
    }

    public Date getDateFromString(String datetoSaved) {
        final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        try {
            Date date = format.parse(datetoSaved + "T00:00:00Z");
            return date;
        } catch (ParseException e) {
            return null;
        }
    }

    void showMessage(String message) {
        new AlertDialog.Builder(BarangayViewEvents.this)
                .setTitle("Success")
                .setCancelable(false)
                .setIcon(android.R.drawable.ic_dialog_info)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null).show();
    }

    private void insertCards(String what, String where, String from, String to, String description, String when, String rawWhen, String images, String id) {
        View view = layoutInflater.inflate(R.layout.event_card_barangay, parent, false);

        btn_viewImage = view.findViewById(R.id.btn_viewImage);
        btn_editEvent = view.findViewById(R.id.btn_editEvent);

        /**
         * Text Views
         */
        event_what = view.findViewById(R.id.event_what);
        event_when = view.findViewById(R.id.event_when);
        event_where = view.findViewById(R.id.event_where);

        event_description = view.findViewById(R.id.event_description);
        event_description.setMovementMethod(new ScrollingMovementMethod());

        // Set values
        event_what.setText(what);
        event_when.setText(when);
        event_description.setText(description);
        event_where.setText(where);

        btn_viewImage.setVisibility(View.GONE);

        btn_editEvent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                handleOnEdit(what, where, from, to, description, rawWhen, id);
            }
        });

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

    public void timeFieldClick(View view) {
        EditText timeText = (EditText) view;
        Calendar mcurrentTime = Calendar.getInstance();
        int hour = mcurrentTime.get(Calendar.HOUR_OF_DAY);
        int minute = mcurrentTime.get(Calendar.MINUTE);
        TimePickerDialog mTimePicker;
        mTimePicker = new TimePickerDialog(this, new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                timeText.setText(String.format("%02d", selectedHour) + ":" + String.format("%02d", selectedMinute));
            }
        }, hour, minute, false);
        mTimePicker.setTitle("Select Time");
        mTimePicker.show();
    }

    public void goBack(View view) {
        startActivity(new Intent(getApplicationContext(), Brgy_Home.class));
        finish();
    }

}
