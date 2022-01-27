package com.example.bataanresponse;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * A simple {@link Fragment} subclass.
 * create an instance of this fragment.
 */
public class ImportBaranggay extends Fragment {


    private static final int MY_REQUEST_CODE_PERMISSION = 1000;
    private static final int MY_RESULT_CODE_FILECHOOSER = 2000;
    Button buttonBrowse;
    private ArrayList<String> failedRegistrants;
    private  ArrayList<String> municipals = new ArrayList<String>();
    private ProgressDialog pDialog;
    Context mainContext;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_import_baranggay, container, false);
        pDialog = new ProgressDialog(this.getContext());
        mainContext = this.getContext();
        pDialog.setMessage("Uploading to Database");
        pDialog.setCancelable(false);
        buttonBrowse = rootView.findViewById(R.id.importBaranggayBtn);
        buttonBrowse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fetchMunicipals();
                askPermissionAndBrowseFile();
            }

        });
        return rootView;
    }

    void fetchMunicipals() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("municipalities")
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        municipals = new ArrayList<String>();
                        for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            municipals.add(doc.getString("title"));
                        }
                        Log.e("loaded", "here");
                    }
                });
    }


    private Timer timer = new Timer();
    private int storeCounter = 0;
    private ArrayList<List> registrants = new ArrayList<List>();

    private void intervalTimer() {
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            private final Handler updateUI = new Handler() {
                @Override
                public void dispatchMessage(Message msg) {
                    super.dispatchMessage(msg);
                    if (storeCounter == registrants.size() && registrants.size() != 0) {
                        onDone();
                    }
                }
            };

            public void run() {
                try {
                    updateUI.sendEmptyMessage(0);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, 0, 2000);
    }

    private void alertShow(String message) {
        new AlertDialog.Builder(this.getContext())
                .setTitle("Error")
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void successShow(String message) {
        new AlertDialog.Builder(this.getContext())
                .setTitle("Success")
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .setIcon(android.R.drawable.ic_dialog_info)
                .show();
    }

    private void readCSVFile(String filePath) {
        try {
            File file = new File(filePath);
            String[] data;
            if (file.exists()) {
                BufferedReader br = new BufferedReader(new FileReader(file));
                try {
                    String csvLine;
                    int counter = 0;
                    while ((csvLine = br.readLine()) != null) {
                        if (counter == 0) {
                            counter++;
                            continue;
                        }
                        data = csvLine.split(",");
                        List<String> list = new ArrayList<String>(Arrays.asList(data));
                        list.removeAll(Arrays.asList("", null));
                        if (list.size() < 2 || list.size() > 2) {
                            Log.e("Problem", "CSV File contains more than two columns.");
                            alertShow("Error: CSV File contains more than two columns.");
                            return;
                        } else if (municipals.indexOf(list.get(1)) == -1) {
                            Log.e("Problem", " File has records of unknown municipal." + municipals.indexOf(list.get(1)));
                            System.out.println(municipals);
                            alertShow("Error: File has records of unknown municipal named " + list.get(1));
                            return;
                        }
                        registrants.add(list);
                    }
                    saveData();
                } catch (IOException ex) {
                    throw new RuntimeException("Error in reading CSV file: " + ex);
                }
            } else {
                Toast.makeText(this.getContext(), "file not exists", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.d("TAG", "ERROR SA SD CARD WRITE: " + e.getMessage());
        }
    }

    boolean isFailed = false;

    private void saveData() {
        isFailed = false;
        pDialog.show();
        storeCounter = 0;
        buttonBrowse.setText(storeCounter + "/" + registrants.size() + " UPLOAD");
        buttonBrowse.setEnabled(false);
        failedRegistrants = new ArrayList<String>();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        intervalTimer();
        for (int x = 0; x < registrants.size(); x++) {
            String municipalName = registrants.get(x).get(1).toString();
            String brgyName = registrants.get(x).get(0).toString();
            db.collection("baranggays")
                    .whereEqualTo("title", brgyName)
                    .whereEqualTo("municipality", municipalName)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.getResult().size() <= 0) {
                                Map<String, Object> dataMap = new HashMap<>();
                                dataMap.put("title", brgyName);
                                dataMap.put("municipality", municipalName);
                                db.collection("baranggays").add(dataMap)
                                        .addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
                                            @Override
                                            public void onComplete(@NonNull Task<DocumentReference> task) {
                                                storeCounter++;
                                                buttonBrowse.setText(storeCounter + "/" + registrants.size() + " UPLOAD");
                                            }
                                        });
                            } else {
                                storeCounter++;
                                buttonBrowse.setText(storeCounter + "/" + registrants.size() + " UPLOAD");
                            }
                        }
                    });
        }
    }

    private void onDone() {
        if (failedRegistrants.size() > 0) {
            alertShow("Failed to store some users. Due to duplicate or invalid data.");
        } else {
            successShow("Baranggay has been added successfully.");
        }
        reset();
    }

    void reset() {
        pDialog.dismiss();
        buttonBrowse.setText("IMPORT BARANGGAY (CSV)");
        buttonBrowse.setEnabled(true);
        registrants = new ArrayList<List>();
        failedRegistrants = new ArrayList<String>();
        timer.cancel();
    }

    private void askPermissionAndBrowseFile() {
        // With Android Level >= 23, you have to ask the user
        // for permission to access External Storage.
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) { // Level 23

            // Check if we have Call permission
            int permisson = ActivityCompat.checkSelfPermission(this.getContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE);

            if (permisson != PackageManager.PERMISSION_GRANTED) {
                // If don't have permission so prompt the user.
                this.requestPermissions(
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        MY_REQUEST_CODE_PERMISSION
                );
                return;
            }
        }
        this.doBrowseFile();
    }

    private void doBrowseFile() {
        Intent chooseFileIntent = new Intent(Intent.ACTION_GET_CONTENT);
        chooseFileIntent.setType("text/*");
        // Only return URIs that can be opened with ContentResolver
        chooseFileIntent.addCategory(Intent.CATEGORY_OPENABLE);

        chooseFileIntent = Intent.createChooser(chooseFileIntent, "Choose a file");
        startActivityForResult(chooseFileIntent, MY_RESULT_CODE_FILECHOOSER);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        //
        switch (requestCode) {
            case MY_REQUEST_CODE_PERMISSION: {
                // Note: If request is cancelled, the result arrays are empty.
                // Permissions granted (CALL_PHONE).
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    Log.i("LOG_TAG", "Permission granted!");
                    Toast.makeText(this.getContext(), "Permission granted!", Toast.LENGTH_SHORT).show();

                    this.doBrowseFile();
                }
                // Cancelled or denied.
                else {
                    Log.i("LOG_TAG", "Permission denied!");
                    Toast.makeText(this.getContext(), "Permission denied!", Toast.LENGTH_SHORT).show();
                }
                break;
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case MY_RESULT_CODE_FILECHOOSER:
                if (resultCode == Activity.RESULT_OK) {
                    if (data != null) {
                        Uri fileUri = data.getData();
                        Log.i("LOG_TAG", "Uri: " + fileUri);

                        String filePath = null;
                        try {
                            filePath = FileUtils.getPath(this.getContext(), fileUri);
                        } catch (Exception e) {
                            Log.e("LOG_TAG", "Error: " + e);
                            Toast.makeText(this.getContext(), "Error: " + e, Toast.LENGTH_SHORT).show();
                        }
                        readCSVFile(filePath);
                    }
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}