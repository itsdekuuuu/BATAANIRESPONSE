package com.example.bataanresponse;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.example.bataanresponse.Resident.Res_Home;
import com.example.bataanresponse.notification.MainApp;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ViewReports extends AppCompatActivity{

    LayoutInflater layoutInflater;

    ViewGroup parent;

    StorageReference storageReference;
    FirebaseFirestore db;
    RecyclerView reportList;
    FirestoreRecyclerAdapter repadapter;

    FirebaseAuth firebaseAuth;

    Spinner action_spinner;

    Button btn_viewImage,
        btn_editReport,
        btn_notify,
        btn_call;

    TextView reports_header, // used for identifying the type of user.
            report_type,
            report_level,
            report_location,
            report_reportedBy,
            report_involve,
            report_names,
            report_when,
            report_description;

    LinearLayout reports_container;


    String resid, brgyAddress, userBrgy, userMunicipal;

    boolean isAdmin = false;

    private ArrayList<String> municipalSpinnerArray;
    private String municipalType;
    private Spinner municipalSpinner;
    private ArrayList<String> brgySpinnerArray;
    private Spinner brgySpinner;
    private String brgyType;

    AlertDialog.Builder dialogBuilder;

    MainApp notify;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_reports);

        db = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();

        reports_header = findViewById(R.id.reports_header);

        layoutInflater = getLayoutInflater();
        parent = findViewById(R.id.reports_container);

        notify = new MainApp();


        onMounted();
        handleReports();
    }


    void onMounted() {
        String role = getIntent().getStringExtra("role");
        userBrgy = getIntent().getStringExtra("baranggay");
        userMunicipal = getIntent().getStringExtra("municipal");

        if (role != null && role.equals("Admin")) {
            isAdmin = true;
            reports_header.setText("BARANGAY");
        } else {
            reports_header.setText("RESIDENT");
        }
    }

    private void cleanLinearParent(LinearLayout layout) {
        int childCount = layout.getChildCount();
        // Remove all rows except the first one
        if (childCount > 1) {
            layout.removeViews(1, childCount - 1);
        }
    }

    void handleReports() {
        if (isAdmin) {
            handleAdmin();
        } else {
            handleResident();
        }
    }

    void fetchBrgyDetails() {
        brgyAddress = userMunicipal.concat(", ") + userBrgy;
    }

    void handleAdmin() {
        fetchBrgyDetails();
        db.collection("reports")
                .orderBy("date", Query.Direction.DESCENDING)
                .whereEqualTo("baranggay", userBrgy)
                .whereEqualTo("municipal", userMunicipal)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            parent.removeAllViews();
                            for (QueryDocumentSnapshot reportDocument : task.getResult()) {
                                String when;
                                Timestamp timestamp = (Timestamp) reportDocument.getData().get("date");
                                when = new SimpleDateFormat("EEEE MMMM dd, yyyy").format(timestamp.toDate());
                                db.collection("users")
                                        .document(reportDocument.getString("user_id"))
                                        .get()
                                        .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                            @Override
                                            public void onSuccess(DocumentSnapshot documentSnapshot) {
                                                try {
                                                    String location = brgyAddress;
                                                    location += reportDocument.getString("location_details").isEmpty() ?
                                                            "" :
                                                            " - " + reportDocument.getString("location_details");

                                                    makeCardForAdmin(reportDocument.getString("classification"),
                                                            reportDocument.getString("classification_type"),
                                                            location,
                                                            reportDocument.getString("involve"),
                                                            reportDocument.getString("people"),
                                                            reportDocument.getString("description"),
                                                            when.concat(" - ").concat(get12HrTime(reportDocument.getString("time"))),
                                                            reportDocument.getId(),
                                                            documentSnapshot.getString("full_name"),
                                                            documentSnapshot.getId(),
                                                            reportDocument.getString("action"),
                                                            reportDocument.getString("images"),
                                                            documentSnapshot.getString("contact_number")
                                                    );

                                                } catch (ParseException e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                        });
                            }
                        }
                    }
                });
    }


    private void makeCardForAdmin(String type, String level, String location, String involve, String names, String description, String when, String reportID, String reportedBy,String reporterId, String myAction, String images, String phone) {
        View view = layoutInflater.inflate(R.layout.report_card_barangay, parent, false);

        // Buttons
        btn_viewImage = view.findViewById(R.id.btn_viewImage);
        btn_notify = view.findViewById(R.id.btn_notify);
        btn_call = view.findViewById(R.id.btn_call);

        // Spinner
        action_spinner = view.findViewById(R.id.action_spinner);

        report_type = view.findViewById(R.id.report_type);
        report_level = view.findViewById(R.id.report_level);
        report_location = view.findViewById(R.id.report_location);
        report_reportedBy = view.findViewById(R.id.report_reportedBy);
        report_involve = view.findViewById(R.id.report_involve);
        report_names = view.findViewById(R.id.report_names);
        report_when = view.findViewById(R.id.report_when);

        report_description = view.findViewById(R.id.report_description);
        report_description.setMovementMethod(new ScrollingMovementMethod());


        ArrayList actions  = new ArrayList<String>();
        actions.add("No Action");
        actions.add("Low - Barangay Council");
        actions.add("High - Police Investigation");
        actions.add("Severe - Investigation with Lawyer");

        if(myAction != null && !myAction.equals("No Action")) {
            action_spinner.setSelection(actions.indexOf(myAction));
        }

        action_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String action = parent.getSelectedItem().toString();
                Map<String, Object> data = new HashMap<>();

                data.put("action",action);
                db.collection("reports").document(reportID)
                        .update(data);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });


        btn_viewImage.setVisibility(View.GONE); // Hide it until we know there is an image available.

        report_type.setText(type);
        report_level.setText(level);
        report_reportedBy.setText(reportedBy);
        report_involve.setText(involve);
        report_names.setText(names);
        report_when.setText(when);
        report_description.setText(description);

        report_location.setText(location);
        report_location.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ViewReports.this, GoogleMapSearch.class);
                intent.putExtra("location", "Bataan " + location);
                startActivity(intent);
            }
        });

        btn_call.setOnClickListener(v -> {
            makeCall(phone);
        });

        btn_notify.setOnClickListener(v -> {
            new AlertDialog.Builder(ViewReports.this)
                    .setTitle("Send Notification")
                    .setMessage("Do you really want to send this notification?")
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            String message = "Hi, " + reportedBy + "! This is Barangay " + userBrgy + " of " + userMunicipal +
                                    ", We received your report on " + type + " incident" +
                                    "We'll take care of the incident, thank you for reporting.";
                            String title = "Help Incoming";
                            Map<String, Object> reportInfo = new HashMap<>();
                            reportInfo.put("title",title);
                            reportInfo.put("description",message);
                            reportInfo.put("barangay", userBrgy);
                            reportInfo.put("type", "report");
                            reportInfo.put("municipal", userMunicipal);
                            reportInfo.put("user_id", reporterId);
                            db.collection("notification_users")
                                    .add(reportInfo);

                            notify.sendNotification(reportID, title, message);
                        }
                    })
                    .setNegativeButton(android.R.string.no, null).show();

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

    void handleResident() {
        String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db.collection("reports")
                .orderBy("date", Query.Direction.DESCENDING)
                .whereEqualTo("user_id", userID)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            parent.removeAllViews();
                            for (QueryDocumentSnapshot reportDocument : task.getResult()) {
                                String location = reportDocument.getString("baranggay").concat(", ").concat(reportDocument.getString("municipal"));
                                location += reportDocument.getString("location_details").isEmpty() ?
                                        "" :
                                        " - " + reportDocument.getString("location_details");
                                String when, rawWhen;
                                try {
                                    Timestamp timestamp = (Timestamp) reportDocument.getData().get("date");
                                    when = new SimpleDateFormat("yyyy-MM-dd").format(timestamp.toDate());
                                    rawWhen = new SimpleDateFormat("yyyy-MM-dd").format(timestamp.toDate());
                                    makeCardForResident(reportDocument.getString("classification"),
                                            reportDocument.getString("classification_type"),
                                            location,
                                            reportDocument.getString("involve"),
                                            reportDocument.getString("people"),
                                            reportDocument.getString("description"),
                                            when.concat(" - ").concat(get12HrTime(reportDocument.getString("time"))),
                                            reportDocument.getId(),
                                            reportDocument.getString("municipal"),
                                            reportDocument.getString("baranggay"),
                                            reportDocument.getString("location_details"),
                                            rawWhen, reportDocument.getString("time"),
                                            reportDocument.getString("images")
                                    );
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                }
                            }
                        } else {
                            Log.d("TAG", "Error getting documents: ", task.getException());
                        }
                    }
                });
    }

    private String get12HrTime(String time) throws ParseException {
        final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        final Date dateObj = sdf.parse(time);
        time = new SimpleDateFormat("hh:mm aa").format(dateObj);
        return time;
    }


    private void makeCardForResident(String type, String level, String location, String involve, String names, String description,
                         String when, String reportID, String municipalData, String brgyData, String locationData,
                         String whenData, String timeData, String images) {

        View view = layoutInflater.inflate(R.layout.report_card_resident, parent, false);

        // Buttons
        btn_viewImage = view.findViewById(R.id.btn_viewImage);
        btn_editReport = view.findViewById(R.id.btn_editReport);
        btn_notify = view.findViewById(R.id.btn_notify);

        // Spinner
        action_spinner = view.findViewById(R.id.action_spinner);

        report_type = view.findViewById(R.id.report_type);
        report_level = view.findViewById(R.id.report_level);
        report_location = view.findViewById(R.id.report_location);
        report_involve = view.findViewById(R.id.report_involve);
        report_names = view.findViewById(R.id.report_names);
        report_when = view.findViewById(R.id.report_when);

        report_description = view.findViewById(R.id.report_description);
        report_description.setMovementMethod(new ScrollingMovementMethod());

        btn_viewImage.setVisibility(View.GONE); // Hide it until we know there is an image available.

        report_type.setText(type);
        report_level.setText(level);
        report_involve.setText(involve);
        report_names.setText(names);
        report_when.setText(when);
        report_description.setText(description);

        report_location.setText(location);
        report_location.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ViewReports.this, GoogleMapSearch.class);
                intent.putExtra("location", "Bataan " + location);
                startActivity(intent);
            }
        });

        btn_editReport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                handleOnEdit(municipalData, brgyData, locationData, whenData, timeData, involve, names, type, level, description, reportID);
            }
        });

        if (images != null) {
            String array_images[] = images.split(";");
            storageReference = FirebaseStorage.getInstance().getReference().child("images/" + array_images[0]);

            storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                @Override
                public void onSuccess(Uri uri) {
                    btn_viewImage.setVisibility(View.VISIBLE);

                    btn_viewImage.setOnClickListener(v -> {
                        showPhoto(uri);
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

    private TextView defaultSizeColumnData(String value) {
        String txt = value.isEmpty() ? "Not Specified" : value;
        TextView data = new TextView(this);
        data.setGravity(Gravity.START);
        data.setPadding(20, 20, 20, 20);
        data.setTextSize(12);
        data.setTextColor(Color.BLACK);
        data.setText(txt);
        return data;
    }


    private TextView locationColumnDataResident(String value, int size) {
        String txt = value.isEmpty() ? "Not Specified" : value;
        TextView data = new TextView(this);
        data.setMaxWidth(size);
        data.setGravity(Gravity.START);
        data.setPadding(20, 20, 20, 20);
        data.setTextSize(12);
        data.setTextColor(Color.BLACK);
        data.setText(txt);
        data.setPaintFlags(data.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        data.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ViewReports.this, GoogleMapSearch.class);
                intent.putExtra("location", "Bataan " + value);
                startActivity(intent);
            }
        });
        return data;
    }

    private TextView columnData(String value, int size) {
        String txt = value.isEmpty() ? "Not Specified" : value;
        TextView data = new TextView(this);
        data.setMaxWidth(size);
        data.setGravity(Gravity.START);
        data.setPadding(20, 20, 20, 20);
        data.setTextSize(12);
        data.setTextColor(Color.BLACK);
        data.setText(txt);
        return data;
    }


    //Resident View Report
    private void ResViewReports() {
        //Query
        Query resquery = db.collection("Reports").whereEqualTo("resid", resid);
        //Recyler Options
        FirestoreRecyclerOptions<ReportModel> resoptions = new FirestoreRecyclerOptions.Builder<ReportModel>()
                .setQuery(resquery, ReportModel.class).build();
        //View Holder
        repadapter = new FirestoreRecyclerAdapter<ReportModel, ResReportViewHolder>(resoptions) {
            @NonNull
            @Override
            public ResReportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_reports, parent, false);
                return new ResReportViewHolder(view);
            }

            @Override
            protected void onBindViewHolder(@NonNull ResReportViewHolder holder, int position, @NonNull ReportModel model) {
                //setting in the recycler view
                holder.tvtitle.setText(model.getTitle());
                holder.tvlocation.setText(model.getLocation());
                holder.tvtime.setText(model.getTime());
                holder.tvdate.setText(model.getDate());
                //Didn't show in recycler view realization: maximum of 4 data can only be displayed in recycler view
//                holder.tvpeople.setText(model.getPeople());
//                holder.tvclass.setText(model.getInclass());
//                holder.tvdes.setText(model.getIndes());
            }
        };

        reportList.setHasFixedSize(true);
        reportList.setLayoutManager(new LinearLayoutManager(this));
        reportList.setAdapter(repadapter);
        repadapter.startListening();
    }

    public void makeCall(String phone) {
        Intent callIntent = new Intent(Intent.ACTION_DIAL);
        callIntent.setData(Uri.parse("tel:" + phone));
        startActivity(callIntent);
    }

    //Barangay View Report
    private void BrgyViewReports(String resadd) {
        //Query
        Query brgyquery = db.collection("Reports").whereEqualTo("resadd", resadd);
        //Recycler Options
        FirestoreRecyclerOptions<ReportModel> brgyoptions = new FirestoreRecyclerOptions.Builder<ReportModel>()
                .setQuery(brgyquery, ReportModel.class).build();
        //View Holder
        repadapter = new FirestoreRecyclerAdapter<ReportModel, BrgyReportViewHolder>(brgyoptions) {
            @NonNull
            @Override
            public BrgyReportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_reports, parent, false);
                return new BrgyReportViewHolder(view);
            }

            @Override
            protected void onBindViewHolder(@NonNull BrgyReportViewHolder holder, int position, @NonNull ReportModel model) {
                //setting in the recycler view
                holder.tvtitle.setText(model.getTitle());
                holder.tvlocation.setText(model.getLocation());
                holder.tvtime.setText(model.getTime());
                holder.tvdate.setText(model.getDate());
                //Didn't show in recycler view realization: maximum of 4 data can only be displayed in recycler view
//                holder.tvpeople.setText(model.getPeople());
//                holder.tvclass.setText(model.getInclass());
//                holder.tvdes.setText(model.getIndes());
            }
        };
        reportList.setHasFixedSize(true);
        reportList.setLayoutManager(new LinearLayoutManager(this));
        reportList.setAdapter(repadapter);
        repadapter.startListening();

    }

    static class ResReportViewHolder extends RecyclerView.ViewHolder {
        //Initialize data
        private final TextView tvtitle;
        private final TextView tvlocation;
        private final TextView tvtime;
        private final TextView tvdate;
//        private TextView tvpeople;
//        private TextView tvclass;
//        private TextView tvdes;

        public ResReportViewHolder(@NonNull View itemView) {
            super(itemView);
            //hooks
            tvtitle = itemView.findViewById(R.id.vtitle);
            tvlocation = itemView.findViewById(R.id.vlocation);
            tvtime = itemView.findViewById(R.id.vtime);
            tvdate = itemView.findViewById(R.id.vdate);
//            tvpeople = itemView.findViewById(R.id.vpeople);
//            tvclass = itemView.findViewById(R.id.vclass);
//            tvdes = itemView.findViewById(R.id.vdes);
        }
    }


    private class BrgyReportViewHolder extends RecyclerView.ViewHolder {
        //Initialize data
        private final TextView tvtitle;
        private final TextView tvlocation;
        private final TextView tvtime;
        private final TextView tvdate;
        private TextView tvpeople;
        private TextView tvclass;
        private TextView tvdes;

        public BrgyReportViewHolder(@NonNull View itemView) {
            super(itemView);
            //hooks
            tvtitle = itemView.findViewById(R.id.vtitle);
            tvlocation = itemView.findViewById(R.id.vlocation);
            tvtime = itemView.findViewById(R.id.vtime);
            tvdate = itemView.findViewById(R.id.vdate);
//            tvpeople = itemView.findViewById(R.id.vpeople);
//            tvclass = itemView.findViewById(R.id.vclass);
//            tvdes = itemView.findViewById(R.id.vdes);
        }
    }




    /*Edit Report*/

    final Calendar myCalendar = Calendar.getInstance();
    private EditText dateTxt, timeText;
    private final DatePickerDialog.OnDateSetListener myDateListener = new DatePickerDialog.OnDateSetListener() {
        @Override
        public void onDateSet(DatePicker datePicker, int arg1, int arg2, int arg3) {
            myCalendar.set(datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth());
            String myFormat = "yyyy-MM-dd";
            SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.TAIWAN);
            dateTxt.setText(sdf.format(myCalendar.getTime()));
        }
    };

    public void onDateClick(View view) {
        new DatePickerDialog(this, myDateListener, myCalendar.get(Calendar.YEAR),
                myCalendar.get(Calendar.MONTH),
                myCalendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    public void goBack(View view) {
        if (isAdmin) {
            startActivity(new Intent(getApplicationContext(), Brgy_Home.class));
        } else {
            startActivity(new Intent(getApplicationContext(), Res_Home.class));
        }

        finish();
    }

    public void timeClick(View view) {
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

    private String classType;

    private void loadClassTypes(Spinner classTypeSpinner, String levelData) {
        // you need to have a list of data that you want the spinner to display
        List<String> spinnerArray = new ArrayList<String>();
        spinnerArray.add("Low");
        spinnerArray.add("High");
        spinnerArray.add("Severe");

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this, R.layout.custom_spinner_item, spinnerArray);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
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
        classTypeSpinner.setSelection(spinnerArray.indexOf(levelData));
    }

    private String classificationStr;

    private void loadClassifications(Spinner classSpinner, String query) {
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
        classSpinner.setSelection(spinnerArray.indexOf(query));
    }

    //Edit Report
    void handleOnEdit(String municipalData, String brgyData, String locationData, String whenData, String timeData,
                      String involveData, String namesData, String classData, String classTypeData, String descData,
                      String reportID) {
        layoutInflater = LayoutInflater.from(ViewReports.this);
        LinearLayout myLayout = (LinearLayout) layoutInflater.inflate(R.layout.update_report, null);
        dialogBuilder = new AlertDialog.Builder(ViewReports.this);
        dialogBuilder.setView(myLayout);
        municipalSpinner = myLayout.findViewById(R.id.municipalitySpinner);
        brgySpinner = myLayout.findViewById(R.id.brgySpinner);
        EditText location = myLayout.findViewById(R.id.whatTxt);
        EditText involve = myLayout.findViewById(R.id.whenTxt);
        EditText names = myLayout.findViewById(R.id.namesOfPeople);
        Spinner typeSpinner = myLayout.findViewById(R.id.classification);
        Spinner levelSpinner = myLayout.findViewById(R.id.classificationType);
        EditText descTxt = myLayout.findViewById(R.id.eventDescription);
        dateTxt = myLayout.findViewById(R.id.fromTxt);
        timeText = myLayout.findViewById(R.id.toTxt);
        loadClassTypes(levelSpinner, classTypeData);
        loadClassifications(typeSpinner, classData);
        involve.setText(involveData);
        names.setText(namesData);
//        typeTxt.setText(classData);
//        levelTxt.setText(classTypeData);
        descTxt.setText(descData);
        location.setText(locationData);
        dateTxt.setText(whenData);
        timeText.setText(timeData);
        timeText.setOnClickListener(this::timeClick);
        dateTxt.setOnClickListener(this::onDateClick);

        dialogBuilder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                updateReport(reportID, timeText.getText().toString(), dateTxt.getText().toString(),
                        involve.getText().toString(), typeSpinner.getSelectedItem().toString(),
                        levelSpinner.getSelectedItem().toString(), descTxt.getText().toString(),
                        location.getText().toString(), names.getText().toString(),
                        brgySpinner.getSelectedItem().toString(),
                        municipalSpinner.getSelectedItem().toString(), dialog);
            }
        });
        dialogBuilder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        dialogBuilder.show();
        loadMunicipalitiesSpinner(municipalData, brgyData);
    }

    void updateReport(String id, String time, String date, String people, String classData, String classType, String desc, String location, String names,
                      String brgy, String municipal, DialogInterface dialog) {
        Map<String, Object> reportInfo = new HashMap<>();
        reportInfo.put("time", time);
        reportInfo.put("date", getDateFromString(date));
        reportInfo.put("involve", people);
        reportInfo.put("classification", classData);
        reportInfo.put("classification_type", classType);
        reportInfo.put("description", desc);
        reportInfo.put("location_details", location);
        reportInfo.put("people", names);
        reportInfo.put("baranggay", brgy);
        reportInfo.put("municipal", municipal);
        db.collection("reports").document(id)
                .update(reportInfo)
                .addOnSuccessListener(aVoid -> {
                    dialog.dismiss();
                    showMessage("Report has been updated!");
                    handleResident();
                });
    }

    void showMessage(String message) {
        new AlertDialog.Builder(ViewReports.this)
                .setTitle("Success")
                .setCancelable(false)
                .setIcon(android.R.drawable.ic_dialog_info)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null).show();
    }

    private Date getDateFromString(String datetoSaved) {
        final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        try {
            Date date = format.parse(datetoSaved + "T00:00:00Z");
            return date;
        } catch (ParseException e) {
            return null;
        }
    }


    private void loadMunicipalitiesSpinner(String municipalData, String brgyData) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("municipalities")
                .orderBy("title")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            setupMunicipalSpinner(task, municipalData, brgyData);
                        } else {
                            Log.d("TAG", "Error getting documents: ", task.getException());
                        }

                    }
                });
    }


    private void setupMunicipalSpinner(Task<QuerySnapshot> task, String municipalData, String brgyData) {
        // you need to have a list of data that you want the spinner to display
        municipalSpinnerArray = new ArrayList<String>();

        for (QueryDocumentSnapshot document : task.getResult()) {
            municipalSpinnerArray.add(document.getString("title"));
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                ViewReports.this, R.layout.custom_spinner_item, municipalSpinnerArray);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        municipalSpinner.setAdapter(adapter);

        municipalSpinner.setSelection(municipalSpinnerArray.indexOf(municipalData));
        municipalSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                municipalType = parent.getSelectedItem().toString();
                loadBrgySpinnerData(municipalType, brgyData);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void loadBrgySpinnerData(String municipal, String brgyData) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("baranggays")
                .orderBy("title")
                .whereEqualTo("municipality", municipal)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            setupBrgySpinner(task, brgyData);
                        } else {
                            Log.d("TAG", "Error getting documents: ", task.getException());
                        }

                    }
                });
    }

    private void setupBrgySpinner(Task<QuerySnapshot> task, String brgyData) {
        // you need to have a list of data that you want the spinner to display
        brgySpinnerArray = new ArrayList<String>();
        for (QueryDocumentSnapshot document : task.getResult()) {
            brgySpinnerArray.add(document.getString("title"));
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

        brgySpinner.setSelection(brgySpinnerArray.indexOf(brgyData));
    }

    void setupDialogBuilder() {

    }

}