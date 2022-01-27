package com.example.bataanresponse.Resident;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import com.example.bataanresponse.Brgy_Home;
import com.example.bataanresponse.R;
import com.example.bataanresponse.TownAdmin;
import com.example.bataanresponse.ViewReports;
import com.example.bataanresponse.notification.MainApp;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EditProfile extends AppCompatActivity {

    EditText fullNameTxt;
    EditText emailtxt;
    EditText contactTxt;
    EditText addressTxt;
    private Spinner municipalSpinner;
    private Spinner brgySpinner;

    ProgressDialog progressDialog;
    String role, selectedMunicipal="", selectedBrgy="", userMunicipal="", userBrgy = "";
    List<String> brgySpinnerArray, municipalSpinnerArray;
    MainApp app;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);
        fullNameTxt = findViewById(R.id.fullNameTxt);
        emailtxt = findViewById(R.id.emailTxt);
        contactTxt = findViewById(R.id.contactNumber);
        addressTxt = findViewById(R.id.addressTxt);
        municipalSpinner = findViewById(R.id.municipalSpinner);
        brgySpinner = findViewById(R.id.brgySpinner);
        app = new MainApp();
        fetchUserInfo();
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
        municipalSpinner.setAdapter(adapter);
        municipalSpinner.setSelection(municipalSpinnerArray.indexOf(userMunicipal));
        municipalSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedMunicipal = parent.getSelectedItem().toString();
                loadBrgySpinnerData(selectedMunicipal);
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
        brgySpinnerArray = new ArrayList<String>();
        for (QueryDocumentSnapshot document : task.getResult()) {
            brgySpinnerArray.add(document.getString("title"));
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this, R.layout.custom_spinner_item, brgySpinnerArray);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        brgySpinner.setAdapter(adapter);
        adapter.notifyDataSetChanged();

        brgySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedBrgy = parent.getSelectedItem().toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        brgySpinner.setSelection(brgySpinnerArray.indexOf(userBrgy));
    }

    void fetchUserInfo() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        String full_name = documentSnapshot.getString("full_name");
                        String email = documentSnapshot.getString("email");
                        String contact_number = documentSnapshot.getString("contact_number");
                        role = documentSnapshot.getString("role");
                        userMunicipal = documentSnapshot.getString("municipal");
                        userBrgy = documentSnapshot.getString("baranggay");
                        fetchAddress(documentSnapshot.getString("baranggay"),
                                documentSnapshot.getString("municipal"),
                                full_name, email, contact_number);
//                        if(role.equals("Admin")){
                        addressTxt.setVisibility(View.GONE);
                        findViewById(R.id.adminAddressView).setVisibility(View.VISIBLE);
                        loadMunicipalitiesSpinner();
//                        }
                    }
                });
    }

    void fetchAddress(String brgy, String municipal, String fullName, String email, String contact) {
        Log.e("C", "TEST SPOT + " + brgy + municipal + fullName + email + contact);
        loadData(fullName, email, contact, userMunicipal + ", " + userBrgy);
    }

    void loadData(String fname, String mail, String cNumber, String location) {
        fullNameTxt.setText(fname);
        emailtxt.setText(mail);
        contactTxt.setText(cNumber);
        addressTxt.setText(location);
    }

    public void onSave(View view) {
        view.setEnabled(false);

        if (fullNameTxt.getText().toString().isEmpty()) {
            showError("Full name is required");
        } else if (emailtxt.getText().toString().isEmpty()) {
            showError("Email is required");
        } else if (contactTxt.getText().toString().isEmpty()) {
            showError("Contact number is required");
        } else if (contactTxt.getText().toString().length() < 5) {
            showError("Contact number must be a valid contact");
        } else {
            if (!selectedBrgy.equals(userBrgy) || !selectedMunicipal.equals(userMunicipal)) {
                askResidentConfirmation(view);
            } else {
                handleDialog();
            }
        }
    }

    void askResidentConfirmation(View view) {
        new AlertDialog.Builder(EditProfile.this)
                .setTitle("Delete Message")
                .setMessage("Are you sure you want to change your barangay address? You will no longer " +
                        "notified from incident reports and events on your previous barangay.")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("YES", (dialog, whichButton) -> {
                    view.setEnabled(true);
                    app.unsubscribe(userMunicipal+userBrgy);
                    handleDialog();
                })
                .setNegativeButton(android.R.string.no, (dialog,button) -> {
                    view.setEnabled(true);
                }).show();
    }

    void handleDialog() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        AlertDialog.Builder builder = new AlertDialog.Builder(EditProfile.this);
        builder.setTitle("Enter your password");

// Set up the input
        final EditText input = new EditText(this);
// Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);
        builder.setCancelable(false);
// Set up the buttons
        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (input.getText() == null || input.getText().toString().isEmpty()) {
                    showError("Invalid Password!");
                    Log.e("ERROR", "PASSWORD IS REQUIRED");
                } else {
                    updateEmail(input.getText().toString());
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                findViewById(R.id.saveBtn).setEnabled(true);
                dialog.cancel();
            }
        });

        builder.show();
    }

    public void onBackClick(View view) {
        if (role.equals("Admin")) {
            Intent intent = new Intent(getBaseContext(), Brgy_Home.class);
            startActivity(intent);
            finish();
        } else if (role.equals("Resident")) {
            Intent intent = new Intent(getBaseContext(), Res_Home.class);
            startActivity(intent);
            finish();
        } else if (role.equals("Tadmin")) {
            Intent intent = new Intent(getBaseContext(), TownAdmin.class);
            startActivity(intent);
            finish();

        } else {
            Toast.makeText(this, "ACCESS FORBIDDEN", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    void updateUserInfo() {
        progressDialog = ProgressDialog.show(EditProfile.this, "",
                "Loading. Please wait...", true);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> data = new HashMap<>();
        data.put("full_name", fullNameTxt.getText().toString());
        data.put("contact_number", contactTxt.getText().toString());
        data.put("email", emailtxt.getText().toString());
//        if(role.equals("Admin")){
        data.put("baranggay", selectedBrgy);
        data.put("municipal", selectedMunicipal);
        app.subscribe(selectedMunicipal+selectedBrgy);

//        }
        db.collection("users").document(user.getUid())
                .update(data)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d("SUCCESS", "SUCCESS UPDATE");
                        EditText passwordTxt = findViewById(R.id.passwordTxt);
                        EditText passConfirmTxt = findViewById(R.id.confirmPasswordTxt);
                        Intent intent;
                        if (role.equals("Admin")) {
                            intent = new Intent(getBaseContext(), Brgy_Home.class);
                        }else if(role.equals("Tadmin")){
                            intent = new Intent(getBaseContext(), TownAdmin.class);
                        } else {
                            intent = new Intent(getBaseContext(), Res_Home.class);
                        }
                        MainApp app = new MainApp();
                        app.subscribe(selectedMunicipal+selectedBrgy);
                        intent.putExtra("message", "UPDATE PROFILE SUCCESSFUL");
                        if ((!passwordTxt.getText().toString().isEmpty() && !passwordTxt.getText().toString().isEmpty()) &&
                                (passwordTxt.getText().toString().equals(passwordTxt.getText().toString())) &&
                                (passwordTxt.getText().toString().length() >= 6)) {
                            user.updatePassword(passwordTxt.getText().toString())
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            progressDialog.dismiss();
                                            startActivity(intent);
                                            finish();
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            progressDialog.dismiss();
                                            Log.e("ERROR ", e.getMessage());
                                            showError(e.getMessage());
                                        }
                                    });
                        } else if (!passwordTxt.getText().toString().equals(passConfirmTxt.getText().toString())) {
                            progressDialog.dismiss();
                            showError("Password do not match.");
                        } else if (passConfirmTxt.getText().toString().isEmpty() && passwordTxt.getText().toString().isEmpty()) {
                            progressDialog.dismiss();
                            startActivity(intent);
                            finish();
                        } else if (passwordTxt.getText().toString().length() < 6) {
                            progressDialog.dismiss();
                            showError("Password must be at least 6 characters.");
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss();
                        Log.e("TAG", "Error updating document", e);
                    }
                });
    }


    void showError(String message) {
        findViewById(R.id.saveBtn).setEnabled(true);
        new AlertDialog.Builder(EditProfile.this)
                .setTitle("Failed")
                .setCancelable(false)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null).show();
    }

    void updateEmail(String password) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        // Get auth credentials from the user for re-authentication
        if (!user.getEmail().equals(emailtxt.getText().toString())) {
            AuthCredential credential = EmailAuthProvider
                    .getCredential(user.getEmail(), password); // Current Login Credentials \\
            // Prompt the user to re-provide their sign-in credentials
            user.reauthenticate(credential)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            Log.d("TAG", "User re-authenticated.");
                            //Now change your email address \\
                            //----------------Code for Changing Email Address----------\\
                            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                            user.updateEmail(emailtxt.getText().toString())
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                updateUserInfo();
                                            }
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Log.e("ERROR TAG", e.getMessage());
                                            showError(e.getMessage());
                                        }
                                    });
                        }
                    });
        } else {
            Log.d("UPDATE", "PROFILE ONLY NO EMAIL");
            AuthCredential credential = EmailAuthProvider
                    .getCredential(user.getEmail(), password); // Current Login Credentials \\
            // Prompt the user to re-provide their sign-in credentials
            user.reauthenticate(credential)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            updateUserInfo();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e("ERROR TAG", e.getMessage());
                            showError("Password invalid!");
                        }
                    });
        }

    }
}