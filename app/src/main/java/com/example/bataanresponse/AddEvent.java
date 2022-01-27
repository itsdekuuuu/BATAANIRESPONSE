package com.example.bataanresponse;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.example.bataanresponse.databinding.ActivityAddEventBinding;
import com.example.bataanresponse.databinding.ActivityMainBinding;
import com.example.bataanresponse.notification.MainApp;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.w3c.dom.Text;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class AddEvent extends AppCompatActivity implements View.OnClickListener {

    ActivityAddEventBinding binding;

    TableLayout table;
    TableRow event_images;

    int image_id = 0;
    TextView tvadname;
    EditText etwhat, etwhen, etwhere, etdes, fromText, toText;
    Button btnsub, btnback, btn_selectImage;

    Uri imageUri;
    FirebaseAuth firebaseAuth;
    FirebaseFirestore firestore;
    StorageReference storageReference;

    HashMap<Integer, Uri> images = new HashMap<>();
    ArrayList<String> image_decoded = new ArrayList<>();

    String what, when, where, des, user_id, userBrgy, userMunicipal;
    MainApp app;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_event);

        tvadname = findViewById(R.id.adminname);
        etwhat = findViewById(R.id.what);
        etwhen = findViewById(R.id.when);
        etwhere = findViewById(R.id.where);
        etdes = findViewById(R.id.des);
        btnsub = findViewById(R.id.submit);
        btnback = findViewById(R.id.back);
        btn_selectImage = findViewById(R.id.choose_image);
        fromText = findViewById(R.id.fromTimeTxt);
        toText = findViewById(R.id.toTimeTxt);
        table = findViewById(R.id.event_images_container);
        event_images = findViewById(R.id.event_images);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        btnsub.setOnClickListener(this);
        btnback.setOnClickListener(this);
        btn_selectImage.setOnClickListener(this);
        app = new MainApp();
        setupHandler();
    }

    private void setupHandler() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference df = db.collection("users").document(FirebaseAuth.getInstance().getCurrentUser().getUid());
        df.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot.getString("role").equals("Admin")) {
                    userBrgy = documentSnapshot.getString("baranggay");
                    userMunicipal = documentSnapshot.getString("municipal");
                    user_id = documentSnapshot.getId();
                    app.subscribe(userMunicipal+userBrgy);
                } else {
                    Toast.makeText(AddEvent.this, "Error ACCESS FORBIDDEN", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        });
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.choose_image:
                selectImage();
                break;
            case R.id.submit:
                addEvent(view);
                break;
            case R.id.back:
                startActivity(new Intent(getApplicationContext(), Brgy_Home.class));
                finish();
                break;
        }
    }

    private void selectImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(intent, 100);
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

    private void addEvent(View view) {
        //check if all field has been filled
        what = etwhat.getText().toString().trim();
        when = etwhen.getText().toString().trim();
        where = etwhere.getText().toString().trim();
        des = etdes.getText().toString();
        String fromTime = fromText.getText().toString();
        String toTime = toText.getText().toString();
        view.setEnabled(false);
        if (what.isEmpty()) {
            view.setEnabled(true);
            etwhat.setError("Required Field");
            etwhat.requestFocus();
            return;
        }
        if (where.isEmpty()) {
            view.setEnabled(true);
            etwhere.setError("Required Field");
            etwhere.requestFocus();
            return;
        }
        if (when.isEmpty()) {
            view.setEnabled(true);
            etwhen.setError("Required Field");
            etwhen.requestFocus();
            return;
        }
        if (des.isEmpty()) {
            view.setEnabled(true);
            etdes.setError("Required Field");
            etdes.requestFocus();
            return;
        }
        if (fromTime.isEmpty()) {
            view.setEnabled(true);
            fromText.setError("Required Field");
            fromText.requestFocus();
            return;
        }
        if (toTime.isEmpty()) {
            view.setEnabled(true);
            toText.setError("Required Field");
            toText.requestFocus();
            return;
        }

        Map<String, Object> eventInfo = new HashMap<>();
        eventInfo.put("what", what);
        eventInfo.put("when", getDateFromString(when));
        eventInfo.put("where", where);
        eventInfo.put("from", fromTime);
        eventInfo.put("to", toTime);
        eventInfo.put("description", des);
        eventInfo.put("baranggay", userBrgy);
        eventInfo.put("municipal", userMunicipal);
        eventInfo.put("user_id", user_id);

        int uploaded_images = 0;
        int total_images = images.size();

        if (total_images > 0) {
            // Upload the file to storages
            for (int k : images.keySet()) {
                Uri image = images.get(k);

                storageReference = FirebaseStorage.getInstance().getReference("images");
                String fileName = getRandomString();
                final StorageReference ref = storageReference.child(fileName);

                ref.putFile(image).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        Toast.makeText(AddEvent.this, "Uploaded " + ((total_images - images.size()) + 1) + " / " + total_images, Toast.LENGTH_SHORT).show();
                        images.remove(k);

                        image_decoded.add(fileName);
                        if (images.isEmpty()) {
                            eventInfo.put("images", TextUtils.join(";", image_decoded));

                            firestore.collection("events").add(eventInfo).addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
                                @Override
                                public void onComplete(@NonNull Task<DocumentReference> task) {
                                    Toast.makeText(AddEvent.this, "Event has been Added", Toast.LENGTH_SHORT).show();

                                    app.sendNotification(userMunicipal + userBrgy, what, des);
                                    Map<String, Object> notificationInfo = new HashMap<>();
                                    notificationInfo.put("municipal", userMunicipal);
                                    notificationInfo.put("barangay", userBrgy);
                                    notificationInfo.put("title", what);
                                    notificationInfo.put("description", des);
                                    firestore.collection("notifications").add(notificationInfo)
                                            .addOnCompleteListener(command -> {
                                                Toast.makeText(AddEvent.this, "Event has been Added", Toast.LENGTH_SHORT).show();
                                                startActivity(new Intent(getApplicationContext(), Brgy_Home.class));
                                                finish();
                                            });
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    view.setEnabled(true);
                                    Toast.makeText(AddEvent.this, "Error Occurred: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
                });
            }
        } else {
            firestore.collection("events").add(eventInfo).addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
                @Override
                public void onComplete(@NonNull Task<DocumentReference> task) {
                    Toast.makeText(AddEvent.this, "Event has been Added", Toast.LENGTH_SHORT).show();

                    app.sendNotification(userMunicipal + userBrgy, what, des);
                    Map<String, Object> notificationInfo = new HashMap<>();
                    notificationInfo.put("municipal", userMunicipal);
                    notificationInfo.put("barangay", userBrgy);
                    notificationInfo.put("title", what);
                    notificationInfo.put("description", des);
                    firestore.collection("notifications").add(notificationInfo)
                            .addOnCompleteListener(command -> {
                                Toast.makeText(AddEvent.this, "Event has been Added", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(getApplicationContext(), Brgy_Home.class));
                                finish();
                            });
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    view.setEnabled(true);
                    Toast.makeText(AddEvent.this, "Error Occurred: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    protected String getRandomString() {
        String SALTCHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        StringBuilder salt = new StringBuilder();
        Random rnd = new Random();
        while (salt.length() < 8) { // length of the random string.
            int index = (int) (rnd.nextFloat() * SALTCHARS.length());
            salt.append(SALTCHARS.charAt(index));
        }
        String saltStr = salt.toString();
        return saltStr;

    }

    public void fromTimeClick(View view) {
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

    private final DatePickerDialog.OnDateSetListener myDateListener = new DatePickerDialog.OnDateSetListener() {
        @Override
        public void onDateSet(DatePicker datePicker, int arg1, int arg2, int arg3) {
            myCalendar.set(datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth());
            String myFormat = "yyyy-MM-dd";
            SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.TAIWAN);
            etwhen.setText(sdf.format(myCalendar.getTime()));
        }
    };

    final Calendar myCalendar = Calendar.getInstance();

    public void onWhenClick(View view) {
        new DatePickerDialog(this, myDateListener, myCalendar.get(Calendar.YEAR),
                myCalendar.get(Calendar.MONTH),
                myCalendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == 100) {
            if(resultCode == RESULT_OK) {
                if(data.getClipData() != null) {
                    int count = data.getClipData().getItemCount(); //evaluate the count before the for loop --- otherwise, the count is evaluated every loop.
                    for(int i = 0; i < count; i++) {
                        Uri imageUri = data.getClipData().getItemAt(i).getUri();

                        ImageView imageView = new ImageView(this);
                        imageView.setImageURI(imageUri);

                        imageView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                new AlertDialog.Builder(AddEvent.this)
                                        .setTitle("Delete Image")
                                        .setMessage("Do you really want to delete this image?")
                                        .setIcon(android.R.drawable.ic_dialog_alert)
                                        .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int whichButton) {
                                                event_images.removeView(view);
                                                Toast.makeText(AddEvent.this, "Deleted Image!", Toast.LENGTH_SHORT).show();
                                                images.remove(image_id);
                                            }
                                        })
                                        .setNegativeButton(android.R.string.no, null).show();
                            }
                        });

                        event_images.addView(imageView, 256, 256);
                        images.put(image_id, imageUri);
                        image_id++;
                    }
                } else if (data.getData() != null){
                    imageUri = data.getData();

                    ImageView imageView = new ImageView(this);
                    imageView.setImageURI(imageUri);

                    imageView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            new AlertDialog.Builder(AddEvent.this)
                                    .setTitle("Delete Image")
                                    .setMessage("Do you really want to delete this image?")
                                    .setIcon(android.R.drawable.ic_dialog_alert)
                                    .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            event_images.removeView(view);
                                            Toast.makeText(AddEvent.this, "Deleted Image!", Toast.LENGTH_SHORT).show();
                                            images.remove(image_id);
                                        }
                                    })
                                    .setNegativeButton(android.R.string.no, null).show();
                        }
                    });

                    event_images.addView(imageView, 256, 256);
                    images.put(image_id, imageUri);
                    image_id++;
                }
            }
        }
    }
}