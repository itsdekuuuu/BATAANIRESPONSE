package com.example.bataanresponse.Resident;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.example.bataanresponse.R;
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
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class AddReport extends AppCompatActivity implements View.OnClickListener {

    Uri imageUri;

    TableRow report_images;
    ArrayList<Uri> images = new ArrayList<>();
    StorageReference storageReference;
    ArrayList<String> image_decoded = new ArrayList<>();


    MainApp app;

    EditText etlocation, ettime, etdate, etpeople, etdes, namesOfPeopleTxt;
    Button btnsubmit, btnback, btnChoose;

    FirebaseAuth firebaseAuth;
    FirebaseFirestore firestore;
    List<String> brgySpinnerArray, municipalSpinnerArray;
    String location, time, date, people,
            in_des, resId, user_id,
            classType = "Choose Classification",
            municipalType,
            brgyType,
            classificationStr = "Attention Needed";

    LinearLayout layout1, layout2, layout3, layout4;
    String myFullName;
    private int currentPage = 1;
    private Spinner classTypeSpinner;
    private Spinner classSpinner;
    private Spinner municipalSpinner;
    private Spinner brgySpinner;

    final Calendar myCalendar = Calendar.getInstance();

    String userMunicipal, userBrgy, selectedBrgyID;
    boolean isDataLoaded = false;

    List<String> brgyIDCollections = new ArrayList<String>();

    private final DatePickerDialog.OnDateSetListener myDateListener = new DatePickerDialog.OnDateSetListener() {
        @Override
        public void onDateSet(DatePicker datePicker, int arg1, int arg2, int arg3) {
            myCalendar.set(datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth());
            String myFormat = "yyyy-MM-dd";
            SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.TAIWAN);
            etdate.setText(sdf.format(myCalendar.getTime()));
        }
    };
    private void getLocation() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference df = db.collection("users").document(FirebaseAuth.getInstance().getCurrentUser().getUid());
        df.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot.getString("role").equals("Resident")) {
                    myFullName = documentSnapshot.getString("full_name");
                    setLocationOfUser(documentSnapshot.getString("baranggay"), documentSnapshot.getString("municipal"));
                } else {
                    Toast.makeText(AddReport.this, "Error ACCESS FORBIDDEN", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        });
    }

    private void setLocationOfUser(String brgy, String municipal) {
        userBrgy = brgyType = brgy;
        userMunicipal = municipalType = municipal;
        loadMunicipalitiesSpinner();
        loadClassifications();
        loadClassTypes();
    }

    private void loadMunicipalitiesSpinner() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("municipalities")
                .orderBy("title")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            setupMunicipalSpinner(task);
                        } else {
                            Log.d("TAG", "Error getting documents: ", task.getException());
                        }

                    }
                });
    }

    private void setupMunicipalSpinner(Task<QuerySnapshot> task) {
        // you need to have a list of data that you want the spinner to display
        municipalSpinnerArray = new ArrayList<String>();

        for (QueryDocumentSnapshot document : task.getResult()) {
            municipalSpinnerArray.add(document.getString("title"));
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this, R.layout.custom_spinner_item, municipalSpinnerArray);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        municipalSpinner = findViewById(R.id.municipalitySpinner);
        municipalSpinner.setAdapter(adapter);
        Log.e("TEXT", userMunicipal + userBrgy);
        municipalSpinner.setSelection(municipalSpinnerArray.indexOf(userMunicipal));
        municipalSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                municipalType = parent.getSelectedItem().toString();
                loadBrgySpinnerData(municipalType);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void loadBrgySpinnerData(String municipal) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("baranggays")
                .orderBy("title")
                .whereEqualTo("municipality", municipal)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            setupBrgySpinner(task);
                        } else {
                            Log.d("TAG", "Error getting documents: ", task.getException());
                        }

                    }
                });
    }

    private void setupBrgySpinner(Task<QuerySnapshot> task) {
        // you need to have a list of data that you want the spinner to display
        brgyIDCollections = new ArrayList<String>();
        brgySpinnerArray = new ArrayList<String>();
        for (QueryDocumentSnapshot document : task.getResult()) {
            brgySpinnerArray.add(document.getString("title"));
            brgyIDCollections.add(document.getId());
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this, R.layout.custom_spinner_item, brgySpinnerArray);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        this.brgySpinner.setAdapter(adapter);
        adapter.notifyDataSetChanged();
        this.brgySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                brgyType = parent.getSelectedItem().toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        this.brgySpinner.setSelection(brgySpinnerArray.indexOf(userBrgy));
    }


    public void timeClick(View view) {
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

    public void onDateClick(View view) {
        new DatePickerDialog(this, myDateListener, myCalendar.get(Calendar.YEAR),
                myCalendar.get(Calendar.MONTH),
                myCalendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    public void onNextPage(View view) {
        if (currentPage < 4) currentPage++;
        togglePage();
    }

    public void onPrevPage(View view) {
        if (currentPage > 1) currentPage--;
        togglePage();
    }

    private void setCurrentPage(int page) {
        currentPage = page;
        togglePage();
    }

    private void togglePage() {
        layout1.setVisibility(View.GONE);
        layout2.setVisibility(View.GONE);
        layout3.setVisibility(View.GONE);
        layout4.setVisibility(View.GONE);

        if (currentPage == 1) {
            layout1.setVisibility(View.VISIBLE);
        }
        if (currentPage == 2) {
            layout2.setVisibility(View.VISIBLE);
        }
        if (currentPage == 3) {
            layout3.setVisibility(View.VISIBLE);
        }
        if (currentPage == 4) {
            layout4.setVisibility(View.VISIBLE);
        }
    }

    private void loadClassTypes() {
        // you need to have a list of data that you want the spinner to display
        List<String> spinnerArray = new ArrayList<String>();
        spinnerArray.add("Choose Classification");
        spinnerArray.add("Low");
        spinnerArray.add("High");
        spinnerArray.add("Severe");

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this, R.layout.custom_spinner_item, spinnerArray);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        classTypeSpinner = findViewById(R.id.classificationType);
        classTypeSpinner.setAdapter(adapter);
        classTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                classType = parent.getSelectedItem().toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void loadClassifications() {
        // you need to have a list of data that you want the spinner to display
        List<String> spinnerArray = new ArrayList<String>();
        spinnerArray.add("Fire");
        spinnerArray.add("Emergency/Rescue");
        spinnerArray.add("Property Damage");
        spinnerArray.add("Theft/Robbery");
        spinnerArray.add("Homicide");

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this, R.layout.custom_spinner_item, spinnerArray);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        classSpinner = findViewById(R.id.classification);
        classSpinner.setAdapter(adapter);
        classSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                classificationStr = parent.getSelectedItem().toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_report);
        app = new MainApp();
        brgySpinner = findViewById(R.id.brgySpinner);
        btnChoose = findViewById(R.id.choose_image_report);
        etlocation = findViewById(R.id.whatTxt);
        ettime = findViewById(R.id.toTxt);
        etdate = findViewById(R.id.fromTxt);
        etpeople = findViewById(R.id.whenTxt);
        etdes = findViewById(R.id.eventDescription);
        btnsubmit = findViewById(R.id.repsubmit);
        btnback = findViewById(R.id.repback);
        layout1 = findViewById(R.id.pageLayout1);
        layout2 = findViewById(R.id.pageLayout2);
        layout3 = findViewById(R.id.pageLayout3);
        layout4 = findViewById(R.id.pageLayout4);

        report_images = findViewById(R.id.report_images);
        namesOfPeopleTxt = findViewById(R.id.namesOfPeople);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        btnsubmit.setOnClickListener(this);
        btnback.setOnClickListener(this);
        btnChoose.setOnClickListener(this);

        fetchUser();
        getLocation();
    }

    private void fetchUser() {
        //Methods for calling firestore database
        FirebaseUser user = firebaseAuth.getCurrentUser();
        resId = user.getUid();
        //Foreign keys
        DocumentReference df = firestore.collection("users").document(resId);
        df.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                user_id = documentSnapshot.getId();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
            }
        });
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.choose_image_report:
                selectImage();
                break;
            case R.id.repsubmit:
                addReport(view);
                break;
            case R.id.repback:
                startActivity(new Intent(getApplicationContext(), Res_Home.class));
                finish();
                break;
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

    private void addReport(View view) {
        view.setEnabled(false);
        //check inputs
        location = etlocation.getText().toString();
        time = ettime.getText().toString();
        date = etdate.getText().toString();
        people = etpeople.getText().toString();
        in_des = etdes.getText().toString();

        if (people.length() == 0) {
            people = "0";
        }

        etdate.setError(null);
        ettime.setError(null);

        if (location.isEmpty()) {
            setCurrentPage(1);
            view.setEnabled(true);
            etlocation.setError("Required Field");
            return;
        }
        if (date.isEmpty()) {
            setCurrentPage(1);
            view.setEnabled(true);
            etdate.setError("Required Field");
            return;
        }
        if (time.isEmpty()) {
            setCurrentPage(1);
            view.setEnabled(true);
            ettime.setError("Required Field");
            return;
        }
        if (classType.equals("Choose Classification")) {
            setCurrentPage(3);
            view.setEnabled(true);
            TextView errorText = (TextView) classTypeSpinner.getSelectedView();
            if (errorText == null) return;
            errorText.setError("");
            errorText.setTextColor(Color.RED);//just to highlight that this is an error
            errorText.setText("This field is required");//
            return;
        }
     /*   if (classificationStr == "Incident Classification") {
            setCurrentPage(3);
            TextView errorText = (TextView) classSpinner.getSelectedView();
            if (errorText == null) return;
            errorText.setError("");
            errorText.setTextColor(Color.RED);//just to highlight that this is an error
            errorText.setText("This field is required");//
            return;
        }
*/
        if (in_des.isEmpty()) {
            setCurrentPage(3);
            view.setEnabled(true);
            etdes.setError("Required Field");
            return;
        }

        //setting up of data for saving
        Map<String, Object> reportInfo = new HashMap<>();
        reportInfo.put("time", time);
        reportInfo.put("date", getDateFromString(date));
        reportInfo.put("involve", people);
        reportInfo.put("classification", classificationStr);
        reportInfo.put("classification_type", classType);
        reportInfo.put("description", in_des);
        reportInfo.put("location_details", location);
        reportInfo.put("people", namesOfPeopleTxt.getText().toString());
        reportInfo.put("user_id", user_id);
        reportInfo.put("baranggay", brgyType);
        reportInfo.put("municipal", municipalType);

        int uploaded_images = 0;
        int total_images = images.size();

        // Upload the file to storages
        for (Uri image: images) {
            storageReference = FirebaseStorage.getInstance().getReference("images");
            String fileName = getRandomString();
            final StorageReference ref = storageReference.child(fileName);

            ref.putFile(image).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    Toast.makeText(AddReport.this, "Uploaded " + ((total_images - images.size()) + 1) + " / " + total_images, Toast.LENGTH_SHORT).show();
                    images.remove(0);

                    image_decoded.add(fileName);
                    if (images.isEmpty()) {
                        reportInfo.put("images", TextUtils.join(";", image_decoded));

                        firestore.collection("reports").add(reportInfo).addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
                            @Override
                            public void onComplete(@NonNull Task<DocumentReference> task) {
                                Toast.makeText(AddReport.this, "Report has been Added", Toast.LENGTH_SHORT).show();
                                notifyAdmins(userMunicipal,userBrgy,"Incident Reported", classificationStr +
                                        " ("+classType+") is reported from your barangay by " + myFullName + ".");
                                startActivity(new Intent(getApplicationContext(), Res_Home.class));
                                finish();
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                view.setEnabled(true);
                                Toast.makeText(AddReport.this, "Error Occured: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            });
        }
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

                        report_images.addView(imageView, 512, 512);
                        images.add(imageUri);
                    }
                } else if (data.getData() != null){
                    imageUri = data.getData();

                    ImageView imageView = new ImageView(this);
                    imageView.setImageURI(imageUri);

                    report_images.addView(imageView, 512, 512);
                    images.add(imageUri);
                }
            }
        }
    }

    void notifyAdmins(String m, String b,String title, String message){
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> reportInfo = new HashMap<>();
        reportInfo.put("title",title);
        reportInfo.put("description",message);
        reportInfo.put("barangay", b);
        reportInfo.put("municipal", m);
        reportInfo.put("type", "report");
        db.collection("notifications")
                .add(reportInfo);

        db.collection("users")
                .whereEqualTo("role","Admin")
                .whereEqualTo("municipal",m)
                .whereEqualTo("baranggay",b)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot snap : queryDocumentSnapshots){
                        app.sendNotification(snap.getId(),title,message);
                    }
                });
    }

}