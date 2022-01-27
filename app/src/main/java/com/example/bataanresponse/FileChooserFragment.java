package com.example.bataanresponse;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
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
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.bataanresponse.Resident.Res_Login;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class FileChooserFragment extends Fragment {

    private static final int MY_REQUEST_CODE_PERMISSION = 1000;
    private static final int MY_RESULT_CODE_FILECHOOSER = 2000;

    private Button buttonBrowse;

    private static final String LOG_TAG = "LOG ANDROID APP";
    private String realPath;

    private Timer timer = new Timer();
    private int storeCounter = 0;
    private ArrayList<List> registrants = new ArrayList<List>();
    private ProgressDialog pDialog;
    private  ArrayList<String> municipals = new ArrayList<String>();
    private  ArrayList<String> baranggayCollections = new ArrayList<String>();

    String baranggayCurrent;
    private String authEmail;
    private String authPassword;

    FirebaseAuth auth;
    FirebaseFirestore fireStore;
    private boolean hasFailed = false;

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

                        /* if successfull load municipals
                         * then ask permission to browse files to get csv
                         * readCSVFile after selecting csv file*/

                        askPermissionAndBrowseFile();
                    }
                });
    }

    void fetchBrgys() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("baranggays")
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        baranggayCollections = new ArrayList<String>();
                        for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            baranggayCollections.add(doc.getString("title"));
                        }
                    }
                });
    }


    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_file_chooser, container, false);
        pDialog = new ProgressDialog(this.getContext());
        pDialog.setMessage("Uploading to Database");
        pDialog.setCancelable(false);
        auth = FirebaseAuth.getInstance();
        fireStore = FirebaseFirestore.getInstance();
        FirebaseUser firebaseUser = auth.getCurrentUser();
        DocumentReference df = fireStore.collection("users").document(firebaseUser.getUid());
        df.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                DocumentSnapshot doc = task.getResult();
                if (doc.exists()) {
                    baranggayCurrent = doc.getString("baranggay");
                }
            }
        });
        this.buttonBrowse = rootView.findViewById(R.id.browseBtn);
        this.buttonBrowse.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                fetchMunicipals();
                fetchBrgys();
                Log.e("TAGET", "LOADEDD MUNICIPAL");
            }

        });
        return rootView;
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

    private void showInfo(String message, DialogInterface.OnClickListener callback) {
        new AlertDialog.Builder(this.getContext())
                .setTitle("Success")
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, callback)
                .setIcon(android.R.drawable.ic_dialog_info)
                .show();
    }


    private void registerUser(String email, String password, String name, String brgy, String city, String phoneNumber, String role) {
        CollectionReference docRef = fireStore.collection("baranggays");
        docRef.whereEqualTo("municipality", city);
        docRef.whereEqualTo("title", brgy);
        docRef.limit(1);
        docRef.get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            if (task.getResult().size() <= 0) {
                                alertShow("UPLOAD INTERRUPTED, There is no such address as " + city + " - " + brgy);
                                reset();
                            } else {
                                auth.createUserWithEmailAndPassword(email, password).addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                                    @Override
                                    public void onSuccess(AuthResult authResult) {
                                        storeCounter++;
                                        buttonBrowse.setText(storeCounter + "/" + registrants.size() + " USERS");
                                        FirebaseUser firebaseUser = auth.getCurrentUser();
                                        DocumentReference df = fireStore.collection("users").document(firebaseUser.getUid());
                                        Map<String, Object> residentInfo = new HashMap<>();
                                        residentInfo.put("email", email);
                                        residentInfo.put("full_name", name);
                                        residentInfo.put("contact_number", phoneNumber);
                                        residentInfo.put("role", role);
                                        residentInfo.put("baranggay", brgy);
                                        residentInfo.put("municipal", city);
                                        df.set(residentInfo).addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                //Email registered but failed to save credentials
                                                Log.e("ERROR FIREBASE 1", e.getMessage());
                                            }
                                        });
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        //Failed Registered
                                        alertShow("UPLOAD INTERRUPTED, Cause by user " + email + " | " + e.getMessage());
                                        reset();
                                        Log.e("ERROR FIREBASE 2", e.getMessage());
                                    }
                                });
                            }
                        } else {
                            Log.d("TAG", "Error getting documents: ", task.getException());
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                alertShow("UPLOAD INTERRUPTED, " + e.getMessage());
                Log.e("ERROR FIREBASE 3", e.getMessage());
                reset();
            }
        });
    }

    private void readCSVFile(String filePath) {
        try {
            File root = Environment.getExternalStorageDirectory();
            File file = new File(filePath);
            String[] data;
            if (file.exists()) {
                BufferedReader br = new BufferedReader(new FileReader(file));
                try {
                    String csvLine;
                    String baranggay = "";
                    int counter = 0;
                    while ((csvLine = br.readLine()) != null) {
                        if (counter == 0) {
                            counter++;
                            continue;
                        }
                        data = csvLine.split(",");
                        List<String> list = new ArrayList<String>(Arrays.asList(data));
                        list.removeAll(Arrays.asList("", null));

                        if (list.size() < 6 || list.size() > 6) {
                            Log.e("Problem", "CSV File has an invalid data");
                            alertShow("Error: CSV File has an invalid data.");
                            return;
                        } else if (!baranggayCollections.contains(list.get(1))) {
                            Log.e("Problem", "Baranggay Unknown");
                            alertShow("Upload Interrupted, unknown baranggay specified, " + list.get(1));
                            return;
                        } else if (!municipals.contains(list.get(2))) {
                            Log.e("Problem", "Municipal Unknown");
                            alertShow("Upload Interrupted, unknown municipal specified, " + list.get(2) + ". Check the spelling or proper case, it mus be capitalize.");
                            return;
                        } else if (!list.get(5).equalsIgnoreCase("Admin") &&
                                !list.get(5).equalsIgnoreCase("Resident") &&  !list.get(5).equalsIgnoreCase("Tadmin"))  {
                            Log.e("Problem", "Role Unknown");
                            alertShow("Upload Interrupted, unknown role specified, " + list.get(5));
                            return;
                        } else if (baranggayCurrent != list.get(1)) {
                            Log.e("Problem", "Barangay outside of bounds.");
                            alertShow("Upload Interrupted. Barangay is outside of boundary cause by barangay " + list.get(1)) ;
                            return;
                        }

                        registrants.add(list);
                    }
                    Log.e("SAVE 1", "SAVING HERE ON READ CSV FILE");
                    saveUsers();
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

    void timeOutHandler() {
        new android.os.Handler().postDelayed(
                new Runnable() {
                    public void run() {
                        Log.e("coutner", String.valueOf(storeCounter));
                        if (FileChooserFragment.this.storeCounter == 0) {
                            new AlertDialog.Builder(FileChooserFragment.this.getContext())
                                    .setTitle("Server Issue")
                                    .setMessage("It seems like, its taking too long, Do you want to retry uploading again?")
                                    .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                                        reset();
                                    })
                                    .setCancelable(false)
                                    .setIcon(android.R.drawable.ic_dialog_info)
                                    .show();
                        }
                    }
                },
                5000);
    }

    private void saveUsers() {
//        timeOutHandler();
        pDialog.show();
        storeCounter = 0;
        buttonBrowse.setText(storeCounter + "/" + registrants.size() + " USERS");
        buttonBrowse.setEnabled(false);
        for (int x = 0; x < registrants.size(); x++) {
            registerUser(registrants.get(x).get(4).toString(),
                    registrants.get(x).get(3).toString(),
                    registrants.get(x).get(0).toString(),
                    registrants.get(x).get(1).toString(),
                    registrants.get(x).get(2).toString(),
                    registrants.get(x).get(3).toString(),
                    registrants.get(x).get(5).toString());
        }
        intervalTimer();
    }

    void signInAuthUser() {
        FirebaseAuth.getInstance().signOut();
        auth.signInWithEmailAndPassword(authEmail, authPassword).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                showInfo("Your session has expired, please re-login.", (dialog, which) -> {
                    startActivity(new Intent(FileChooserFragment.this.getContext(), Res_Login.class));
                    getActivity().finish();
                });
            }
        });
    }

    private void onDone() {
        successShow("Users has been registered successfully.");
        reset();
    }

    void reset() {
        signInAuthUser();
        registrants = new ArrayList<>();
        hasFailed = true;
        pDialog.dismiss();
        storeCounter = 0;
        buttonBrowse.setText("IMPORT USERS (CSV)");
        buttonBrowse.setEnabled(true);
        timer.cancel();
    }


    public String getPath() {
        return this.realPath;
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
        handleDialog();
    }

    void handleDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(FileChooserFragment.this.getContext());
        builder.setTitle("Enter your password");

// Set up the input
        final EditText input = new EditText(this.getContext());
// Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);
        builder.setCancelable(false);
// Set up the buttons
        builder.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                authPassword = input.getText().toString();
                authenticate(input.getText().toString());
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    void authenticate(String password) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        authEmail = user.getEmail();
        AuthCredential credential = EmailAuthProvider
                .getCredential(authEmail, password); // Current Login Credentials \\
        // Prompt the user to re-provide their sign-in credentials
        user.reauthenticate(credential)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.e("FILECHOOSERFRAGMENT", "User re-authenticated.");
                            doBrowseFile();
                        } else {
                            Log.e("FILECHOOSERFRAGMENT", "Wrong credentials ");
                            alertShow(task.getException().getMessage());
                        }
                    }
                });
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

                    Log.i(LOG_TAG, "Permission granted!");
                    Toast.makeText(this.getContext(), "Permission granted!", Toast.LENGTH_SHORT).show();
                    handleDialog();
                }
                // Cancelled or denied.
                else {
                    Log.i(LOG_TAG, "Permission denied!");
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
                        Log.i(LOG_TAG, "Uri: " + fileUri);

                        String filePath = null;
                        try {
                            filePath = FileUtils.getPath(this.getContext(), fileUri);
                        } catch (Exception e) {
                            Log.e(LOG_TAG, "Error: " + e);
                            Toast.makeText(this.getContext(), "Error: " + e, Toast.LENGTH_SHORT).show();
                        }
//                        this.editTextPath.setText(filePath);
                        this.realPath = filePath;
                        readCSVFile(filePath);
                    }
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}