package com.sandhyyasofttech.attendsmart.Activities;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public class PersonalDetailsActivity extends AppCompatActivity {

    private TextInputEditText etName, etEmail, etMobile, etDOB, etReligion, etAddress, etCity,
            etState, etPinCode, etAadhaarNumber, etPanNumber, etPassportNumber, etFatherName,
            etMotherName, etSpouseName, etEmergencyContactName, etEmergencyContact, etEmergencyRelation;
    private AutoCompleteTextView spGender, spMaritalStatus, spBloodGroup, spNationality;
    private MaterialButton btnSave;
    private String dob = "";

    private DatabaseReference dbRef;
    private String companyKey, employeeMobile;

    // ✅ Validation patterns
    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z\\s.'-]{2,50}$");
    private static final Pattern MOBILE_PATTERN = Pattern.compile("^[6-9]\\d{9}$");
    private static final Pattern PIN_PATTERN = Pattern.compile("^[1-9][0-9]{5}$");
    private static final Pattern AADHAAR_PATTERN = Pattern.compile("^\\d{12}$");
    private static final Pattern PAN_PATTERN = Pattern.compile("^[A-Z]{5}[0-9]{4}[A-Z]{1}$");
    private static final Pattern PASSPORT_PATTERN = Pattern.compile("^[A-PR-WYa-pr-wy][1-9]\\d\\s?\\d{4}[1-9]$");

    private static final String[] GENDER_OPTIONS = {"Male", "Female", "Other"};
    private static final String[] MARITAL_OPTIONS = {"Single", "Married", "Divorced", "Widowed"};
    private static final String[] BLOOD_OPTIONS = {"A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-"};
    private static final String[] NATIONALITY_OPTIONS = {"Indian", "American", "British", "Canadian", "Australian", "Other"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personal_details);
        // Set status bar color
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.blue_800));
        }


        PrefManager pref = new PrefManager(this);
        companyKey = pref.getCompanyKey();
        employeeMobile = pref.getEmployeeMobile();

        if (companyKey == null) companyKey = getIntent().getStringExtra("companyKey");
        if (employeeMobile == null) employeeMobile = getIntent().getStringExtra("employeeMobile");

        if (companyKey == null || employeeMobile == null) {
            toast("⚠️ Please login first");
            finish();
            return;
        }

        initViews();
        setupToolbar();
        setupDropdowns();
        setupDobPicker();
        loadPersonalData();
        setupSaveButton();
    }

    private void initViews() {
        etName = findViewById(R.id.etName); etEmail = findViewById(R.id.etEmail); etMobile = findViewById(R.id.etMobile);
        etDOB = findViewById(R.id.etDOB); etReligion = findViewById(R.id.etReligion);
        spGender = findViewById(R.id.spGender); spMaritalStatus = findViewById(R.id.spMaritalStatus);
        spBloodGroup = findViewById(R.id.spBloodGroup); spNationality = findViewById(R.id.spNationality);
        etAddress = findViewById(R.id.etAddress); etCity = findViewById(R.id.etCity);
        etState = findViewById(R.id.etState); etPinCode = findViewById(R.id.etPinCode);
        etAadhaarNumber = findViewById(R.id.etAadhaarNumber); etPanNumber = findViewById(R.id.etPanNumber);
        etPassportNumber = findViewById(R.id.etPassportNumber); etFatherName = findViewById(R.id.etFatherName);
        etMotherName = findViewById(R.id.etMotherName); etSpouseName = findViewById(R.id.etSpouseName);
        etEmergencyContactName = findViewById(R.id.etEmergencyContactName);
        etEmergencyContact = findViewById(R.id.etEmergencyContact); etEmergencyRelation = findViewById(R.id.etEmergencyRelation);
        btnSave = findViewById(R.id.btnSave);
        dbRef = FirebaseDatabase.getInstance().getReference();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle("Personal Details");
            }
        }
    }

    private void setupDropdowns() {
        setupDropdown(spGender, GENDER_OPTIONS);
        setupDropdown(spMaritalStatus, MARITAL_OPTIONS);
        setupDropdown(spBloodGroup, BLOOD_OPTIONS);
        setupDropdown(spNationality, NATIONALITY_OPTIONS);
    }

    private void setupDropdown(AutoCompleteTextView dropdown, String[] options) {
        if (dropdown != null) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, options);
            dropdown.setAdapter(adapter);
            dropdown.setThreshold(1);
        }
    }

    private void setupDobPicker() {
        etDOB.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    this,
                    (view, year, month, dayOfMonth) -> {
                        Calendar dobCal = Calendar.getInstance();
                        dobCal.set(year, month, dayOfMonth);

                        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
                        dob = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(dobCal.getTime());
                        etDOB.setText(sdf.format(dobCal.getTime()));
                        etDOB.setError(null);
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );

            // DOB must be past date only
            datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
            datePickerDialog.show();
        });
    }

    private void loadPersonalData() {
        dbRef.child("Companies").child(companyKey).child("employees").child(employeeMobile).child("info")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        setText(etName, snapshot.child("employeeName").getValue(String.class));
                        setText(etEmail, snapshot.child("employeeEmail").getValue(String.class));
                        setText(etMobile, snapshot.child("employeeMobile").getValue(String.class));
                        setText(etDOB, snapshot.child("dob").getValue(String.class));
                        setText(etReligion, snapshot.child("religion").getValue(String.class));
                        setDropdownText(spGender, snapshot.child("gender").getValue(String.class));
                        setDropdownText(spMaritalStatus, snapshot.child("maritalStatus").getValue(String.class));
                        setDropdownText(spBloodGroup, snapshot.child("bloodGroup").getValue(String.class));
                        setDropdownText(spNationality, snapshot.child("nationality").getValue(String.class));
                        setText(etAddress, snapshot.child("address").getValue(String.class));
                        setText(etCity, snapshot.child("city").getValue(String.class));
                        setText(etState, snapshot.child("state").getValue(String.class));
                        setText(etPinCode, snapshot.child("pinCode").getValue(String.class));
                        setText(etAadhaarNumber, snapshot.child("aadhaarNumber").getValue(String.class));
                        setText(etPanNumber, snapshot.child("panNumber").getValue(String.class));
                        setText(etPassportNumber, snapshot.child("passportNumber").getValue(String.class));
                        setText(etFatherName, snapshot.child("fatherName").getValue(String.class));
                        setText(etMotherName, snapshot.child("motherName").getValue(String.class));
                        setText(etSpouseName, snapshot.child("spouseName").getValue(String.class));
                        setText(etEmergencyContactName, snapshot.child("emergencyContactName").getValue(String.class));
                        setText(etEmergencyContact, snapshot.child("emergencyContact").getValue(String.class));
                        setText(etEmergencyRelation, snapshot.child("emergencyRelation").getValue(String.class));
                    }
                    @Override public void onCancelled(DatabaseError error) {}
                });
    }

    private void setText(TextInputEditText editText, String value) {
        if (editText != null && value != null) editText.setText(value);
    }

    private void setDropdownText(AutoCompleteTextView dropdown, String value) {
        if (dropdown != null && value != null) dropdown.setText(value, false);
    }

    private void setupSaveButton() {
        if (btnSave != null) {
            btnSave.setOnClickListener(v -> {
                if (validateAllFields()) {
                    savePersonalData();
                }
            });
        }
    }

    // ✅ FIXED: UNIVERSAL getText() method - Works for ALL EditText types
    private String getText(EditText editText) {
        if (editText != null && editText.getText() != null) {
            return editText.getText().toString().trim();
        }
        return "";
    }

    private boolean isOneOf(String value, String[] options) {
        for (String option : options) {
            if (option.equalsIgnoreCase(value)) return true;
        }
        return false;
    }

    // ✅ FULL VALIDATION - checks every field, shows inline errors, focuses first invalid field
    private boolean validateAllFields() {
        boolean isValid = true;
        EditText firstInvalidField = null;

        // ----- Basic Information -----
        String name = getText(etName);
        if (name.isEmpty()) {
            etName.setError("Name is required");
            isValid = false;
            if (firstInvalidField == null) firstInvalidField = etName;
        } else if (!NAME_PATTERN.matcher(name).matches()) {
            etName.setError("Enter a valid name (letters only, 2-50 characters)");
            isValid = false;
            if (firstInvalidField == null) firstInvalidField = etName;
        }

        String email = getText(etEmail);
        if (!email.isEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Enter a valid email address");
            isValid = false;
            if (firstInvalidField == null) firstInvalidField = etEmail;
        }

        String mobile = getText(etMobile);
        if (mobile.isEmpty()) {
            etMobile.setError("Mobile number is required");
            isValid = false;
            if (firstInvalidField == null) firstInvalidField = etMobile;
        } else if (!MOBILE_PATTERN.matcher(mobile).matches()) {
            etMobile.setError("Enter a valid 10-digit mobile number");
            isValid = false;
            if (firstInvalidField == null) firstInvalidField = etMobile;
        }

        // ----- Personal Details -----
        String dobText = getText(etDOB);
        // DOB optional — no required check

        String genderText = getText(spGender);
        if (!genderText.isEmpty() && !isOneOf(genderText, GENDER_OPTIONS)) {
            spGender.setError("Select a valid gender from the list");
            isValid = false;
            if (firstInvalidField == null) firstInvalidField = spGender;
        }

        String bloodText = getText(spBloodGroup);
        if (!bloodText.isEmpty() && !isOneOf(bloodText, BLOOD_OPTIONS)) {
            spBloodGroup.setError("Select a valid blood group from the list");
            isValid = false;
            if (firstInvalidField == null) firstInvalidField = spBloodGroup;
        }

        String maritalText = getText(spMaritalStatus);
        if (!maritalText.isEmpty() && !isOneOf(maritalText, MARITAL_OPTIONS)) {
            spMaritalStatus.setError("Select a valid marital status from the list");
            isValid = false;
            if (firstInvalidField == null) firstInvalidField = spMaritalStatus;
        }

        String nationalityText = getText(spNationality);
        if (!nationalityText.isEmpty() && !isOneOf(nationalityText, NATIONALITY_OPTIONS)) {
            spNationality.setError("Select a valid nationality from the list");
            isValid = false;
            if (firstInvalidField == null) firstInvalidField = spNationality;
        }

        String religionText = getText(etReligion);
        if (!religionText.isEmpty() && !NAME_PATTERN.matcher(religionText).matches()) {
            etReligion.setError("Enter a valid religion (letters only)");
            isValid = false;
            if (firstInvalidField == null) firstInvalidField = etReligion;
        }

        // ----- Address Details -----
        String addressText = getText(etAddress);
        if (!addressText.isEmpty() && addressText.length() < 5) {
            etAddress.setError("Enter a complete address");
            isValid = false;
            if (firstInvalidField == null) firstInvalidField = etAddress;
        }

        String cityText = getText(etCity);
        if (!cityText.isEmpty() && !NAME_PATTERN.matcher(cityText).matches()) {
            etCity.setError("Enter a valid city name");
            isValid = false;
            if (firstInvalidField == null) firstInvalidField = etCity;
        }

        String stateText = getText(etState);
        if (!stateText.isEmpty() && !NAME_PATTERN.matcher(stateText).matches()) {
            etState.setError("Enter a valid state name");
            isValid = false;
            if (firstInvalidField == null) firstInvalidField = etState;
        }

        String pinText = getText(etPinCode);
        if (!pinText.isEmpty() && !PIN_PATTERN.matcher(pinText).matches()) {
            etPinCode.setError("Enter a valid 6-digit PIN code");
            isValid = false;
            if (firstInvalidField == null) firstInvalidField = etPinCode;
        }

        // ----- Government IDs (optional, validated only if filled) -----
        String aadhaarText = getText(etAadhaarNumber);
        if (!aadhaarText.isEmpty() && !AADHAAR_PATTERN.matcher(aadhaarText).matches()) {
            etAadhaarNumber.setError("Enter a valid 12-digit Aadhaar number");
            isValid = false;
            if (firstInvalidField == null) firstInvalidField = etAadhaarNumber;
        }

        String panText = getText(etPanNumber);
        if (!panText.isEmpty() && !PAN_PATTERN.matcher(panText.toUpperCase(Locale.getDefault())).matches()) {
            etPanNumber.setError("Enter a valid PAN number (e.g. ABCDE1234F)");
            isValid = false;
            if (firstInvalidField == null) firstInvalidField = etPanNumber;
        }

        String passportText = getText(etPassportNumber);
        if (!passportText.isEmpty() && !PASSPORT_PATTERN.matcher(passportText.toUpperCase(Locale.getDefault())).matches()) {
            etPassportNumber.setError("Enter a valid passport number");
            isValid = false;
            if (firstInvalidField == null) firstInvalidField = etPassportNumber;
        }

        // ----- Family & Emergency Contact -----
        String fatherText = getText(etFatherName);
        if (!fatherText.isEmpty() && !NAME_PATTERN.matcher(fatherText).matches()) {
            etFatherName.setError("Enter a valid name (letters only)");
            isValid = false;
            if (firstInvalidField == null) firstInvalidField = etFatherName;
        }

        String motherText = getText(etMotherName);
        if (!motherText.isEmpty() && !NAME_PATTERN.matcher(motherText).matches()) {
            etMotherName.setError("Enter a valid name (letters only)");
            isValid = false;
            if (firstInvalidField == null) firstInvalidField = etMotherName;
        }

        String spouseText = getText(etSpouseName);
        if (!spouseText.isEmpty() && !NAME_PATTERN.matcher(spouseText).matches()) {
            etSpouseName.setError("Enter a valid name (letters only)");
            isValid = false;
            if (firstInvalidField == null) firstInvalidField = etSpouseName;
        }
        if (!spouseText.isEmpty() && maritalText.equalsIgnoreCase("Single")) {
            etSpouseName.setError("Spouse name should be blank for marital status 'Single'");
            isValid = false;
            if (firstInvalidField == null) firstInvalidField = etSpouseName;
        }

        String emergencyNameText = getText(etEmergencyContactName);
        if (!emergencyNameText.isEmpty() && !NAME_PATTERN.matcher(emergencyNameText).matches()) {
            etEmergencyContactName.setError("Enter a valid name (letters only)");
            isValid = false;
            if (firstInvalidField == null) firstInvalidField = etEmergencyContactName;
        }

        String emergencyContactText = getText(etEmergencyContact);
        if (emergencyContactText.isEmpty()) {
            etEmergencyContact.setError("Emergency contact number is required");
            isValid = false;
            if (firstInvalidField == null) firstInvalidField = etEmergencyContact;
        } else if (!MOBILE_PATTERN.matcher(emergencyContactText).matches()) {
            etEmergencyContact.setError("Enter a valid 10-digit emergency contact number");
            isValid = false;
            if (firstInvalidField == null) firstInvalidField = etEmergencyContact;
        } else if (!mobile.isEmpty() && emergencyContactText.equals(mobile)) {
            etEmergencyContact.setError("Emergency contact cannot be the same as your own mobile number");
            isValid = false;
            if (firstInvalidField == null) firstInvalidField = etEmergencyContact;
        }

        String emergencyRelationText = getText(etEmergencyRelation);
        if (!emergencyRelationText.isEmpty() && !NAME_PATTERN.matcher(emergencyRelationText).matches()) {
            etEmergencyRelation.setError("Enter a valid relationship (letters only)");
            isValid = false;
            if (firstInvalidField == null) firstInvalidField = etEmergencyRelation;
        }

        if (!isValid) {
            if (firstInvalidField != null) {
                firstInvalidField.requestFocus();
            }
            toast("⚠️ Please fix the highlighted fields");
        }

        return isValid;
    }

    private void savePersonalData() {
        String name = getText(etName);

        Map<String, Object> updates = new HashMap<>();
        updates.put("employeeName", name);

        // ✅ SAFE: Check if text is not empty before adding to updates
        String emailText = getText(etEmail);
        if (!emailText.isEmpty()) updates.put("employeeEmail", emailText);

        String mobileText = getText(etMobile);
        if (!mobileText.isEmpty()) updates.put("employeeMobile", mobileText);

        String dobText = getText(etDOB);
        if (!dobText.isEmpty()) updates.put("dob", dobText);

        String religionText = getText(etReligion);
        if (!religionText.isEmpty()) updates.put("religion", religionText);

        String genderText = getText(spGender);
        if (!genderText.isEmpty()) updates.put("gender", genderText);

        String maritalText = getText(spMaritalStatus);
        if (!maritalText.isEmpty()) updates.put("maritalStatus", maritalText);

        String bloodText = getText(spBloodGroup);
        if (!bloodText.isEmpty()) updates.put("bloodGroup", bloodText);

        String nationalityText = getText(spNationality);
        if (!nationalityText.isEmpty()) updates.put("nationality", nationalityText);

        String addressText = getText(etAddress);
        if (!addressText.isEmpty()) updates.put("address", addressText);

        String cityText = getText(etCity);
        if (!cityText.isEmpty()) updates.put("city", cityText);

        String stateText = getText(etState);
        if (!stateText.isEmpty()) updates.put("state", stateText);

        String pinText = getText(etPinCode);
        if (!pinText.isEmpty()) updates.put("pinCode", pinText);

        String aadhaarText = getText(etAadhaarNumber);
        if (!aadhaarText.isEmpty()) updates.put("aadhaarNumber", aadhaarText);

        String panText = getText(etPanNumber);
        if (!panText.isEmpty()) updates.put("panNumber", panText.toUpperCase(Locale.getDefault()));

        String passportText = getText(etPassportNumber);
        if (!passportText.isEmpty()) updates.put("passportNumber", passportText.toUpperCase(Locale.getDefault()));

        String fatherText = getText(etFatherName);
        if (!fatherText.isEmpty()) updates.put("fatherName", fatherText);

        String motherText = getText(etMotherName);
        if (!motherText.isEmpty()) updates.put("motherName", motherText);

        String spouseText = getText(etSpouseName);
        if (!spouseText.isEmpty()) updates.put("spouseName", spouseText);

        String emergencyNameText = getText(etEmergencyContactName);
        if (!emergencyNameText.isEmpty()) updates.put("emergencyContactName", emergencyNameText);

        String emergencyContactText = getText(etEmergencyContact);
        if (!emergencyContactText.isEmpty()) updates.put("emergencyContact", emergencyContactText);

        String emergencyRelationText = getText(etEmergencyRelation);
        if (!emergencyRelationText.isEmpty()) updates.put("emergencyRelation", emergencyRelationText);

        dbRef.child("Companies").child(companyKey).child("employees").child(employeeMobile).child("info")
                .updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    toast("✅ Profile saved!");
                    finish();
                })
                .addOnFailureListener(e -> toast("❌ Save failed"));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}