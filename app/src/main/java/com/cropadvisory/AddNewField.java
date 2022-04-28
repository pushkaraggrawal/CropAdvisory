package com.cropadvisory;

import android.app.DatePickerDialog;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AddNewField extends AppCompatActivity  implements View.OnClickListener, AdapterView.OnItemSelectedListener {
    private static String TAG = "AddNewFieldActivity";
    public static final String EDIT_FIELD = "position_to_edit_field";
    private int POS_EDIT_FIELD = -1;

    final Calendar myCalendar= Calendar.getInstance();

    List<Map<String, Object>> fieldsList;
    EditText fieldType;
    EditText pincode;
    EditText dateSown;
    Spinner spinner, sensorSpinner;

    List<String> sensors = new ArrayList<>();
    ArrayAdapter<String> sensorsSpinnerAdapter;

    private FirebaseAuth mAuth;
    private Context ctx = this;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_new_field);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        fieldType = findViewById(R.id.fieldType);
        pincode = findViewById(R.id.pincode);
        dateSown = findViewById(R.id.dateSown);
        spinner = (Spinner) findViewById(R.id.fieldTypeSpinner);

        findViewById(R.id.submit).setOnClickListener(this);

        inflateSensorSpinner();

        DatePickerDialog.OnDateSetListener date =new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int day) {
                myCalendar.set(Calendar.YEAR, year);
                myCalendar.set(Calendar.MONTH,month);
                myCalendar.set(Calendar.DAY_OF_MONTH,day);
                updateLabel();
            }
        };
        dateSown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new DatePickerDialog(ctx,date,myCalendar.get(Calendar.YEAR),myCalendar.get(Calendar.MONTH),myCalendar.get(Calendar.DAY_OF_MONTH)).show();
            }
        });

        try {
            if (getIntent().getExtras().get(EDIT_FIELD) != null) {
                POS_EDIT_FIELD = (int) getIntent().getExtras().get(EDIT_FIELD);
                Log.w(TAG, "Editing at: " + POS_EDIT_FIELD);
                if (POS_EDIT_FIELD >= 0) {
                    Log.w(TAG, "Editing!");
                    fetchExistingFields();
                }
            }
        }catch (Exception e){e.printStackTrace();}
    }

    private void updateLabel(){
        String myFormat="dd/MM/yy";
        SimpleDateFormat dateFormat=new SimpleDateFormat(myFormat, Locale.ENGLISH);
        dateSown.setText(dateFormat.format(myCalendar.getTime()));
    }

    private void initSpinner() {
        spinner = (Spinner) findViewById(R.id.fieldTypeSpinner);
        ArrayAdapter<CharSequence> adapter;
        adapter = ArrayAdapter.createFromResource(this,
                R.array.fieldType, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinner.setAdapter(adapter);
        //spinner.setOnItemSelectedListener(this);
    }
    private void inflateSensorSpinner(){
        sensors.clear();
        sensors.add("NA");
        sensorSpinner = (Spinner) findViewById(R.id.sensorIdSpinner);
        sensorsSpinnerAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, sensors);
        sensorsSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_expandable_list_item_1);
        sensorSpinner.setAdapter(sensorsSpinnerAdapter);
        sensorSpinner.setOnItemSelectedListener(this);
        //Set

        //Call this to Load address from database and update in the spinner
        initSensorSpinner();
    }

    private void initSensorSpinner(){
        try {
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("sensors");
            ref.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if(snapshot.exists()){
                        HashMap<String, Double> sensors = new HashMap<>();
                        sensors = (HashMap<String, Double>) snapshot.getValue();
                        //Log.w(TAG, snapshot.toString());
                        //Log.w(TAG, sensors.toString());
                        if(sensors != null){
                            AddNewField.this.sensors.clear();
                            for (Map.Entry<String, Double> pair : sensors.entrySet()) {
                                Integer val = Integer.parseInt(String.valueOf(pair.getValue()));
                                if(val == 1)
                                    AddNewField.this.sensors.add(pair.getKey());
                            }
                            //Log.w(TAG, addresses.toString());
                            sensorsSpinnerAdapter.notifyDataSetChanged();
                            if(POS_EDIT_FIELD>=0)
                                setSensorSelection();
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }catch (Exception e){
            e.printStackTrace();
            Log.w(TAG, "Sensor Spinner exception in AddNewField: "+e.getMessage());
        }
    }

    private void fetchExistingFields() {
        findViewById(R.id.progress_loading_existing_fields).setVisibility(View.VISIBLE);
        try{
            DocumentReference docRef = db.collection("users").document(mAuth.getCurrentUser().getUid());

            docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    findViewById(R.id.progress_loading_existing_fields).setVisibility(View.GONE);
                    if(task.isSuccessful()) {
                        Log.w(TAG, "Success!");
                        //mListFetched = true;
                        DocumentSnapshot document = task.getResult();
                        /**
                         * No requirement, Firebase Latency Compensation will take care even if client offline
                         */
                        //if(document.getMetadata().isFromCache()){
                        //    finish();
                        //    startActivity(getIntent());
                        //}
                        if (document.exists()) {
                            Log.w(TAG, "Doc Exists!");
                            DocumentSnapshot doc = document;//Making it globally accessible
                            //List<Map<String, Object>> fieldsList;
                            try {
                                fieldsList = (List<Map<String, Object>>) doc.get("fields");
                                Log.w(TAG, "Fields: " + fieldsList.toString());
                                if(fieldsList != null){
                                    int n = fieldsList.size();
                                    Log.w(TAG, "Field Cnt: " + n);
                                    Log.w(TAG, "Field POS_EDIT_FIELD: " + POS_EDIT_FIELD);
                                    if(n>=POS_EDIT_FIELD){
                                        try {
                                            fieldType.setText(fieldsList.get(POS_EDIT_FIELD).get("fieldType").toString());
                                            pincode.setText(fieldsList.get(POS_EDIT_FIELD).get("pincode").toString());
                                            if(fieldsList.get(POS_EDIT_FIELD).get("dateSown")!=null)
                                                dateSown.setText(fieldsList.get(POS_EDIT_FIELD).get("dateSown").toString());
                                            setSensorSelection();
                                        }catch (Exception e){e.printStackTrace();}
                                    }
                                }
                            }catch (Exception e){
                                e.printStackTrace();
                                showToast("Error: " + e.getMessage());
                                onBackPressed();
                            }
                        } else {
                            Log.d(TAG, "User Id document not found.");
                            showToast("Error: Check Network Connection!");
                            onBackPressed();
                        }
                    }
                }
            });
        }catch (Exception e){
            findViewById(R.id.progress_loading_existing_fields).setVisibility(View.GONE);
            Log.w(TAG, e.getMessage());
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if(id == R.id.submit){
            submitField();
        }
    }
    public List<Address>  findLocationName(String pin)
    {
        try {
            Geocoder gcd = new Geocoder(this, Locale.getDefault());

            List<Address> addresses;
            addresses = gcd.getFromLocationName(pin,5);

//            for(int i=0;i<addresses.size();i++){
//                Log.e("","in loop");
//                Log.e("","lat :,,,,,,,,,,,,,"+addresses.get(i).getLatitude()+"  long............"+addresses.get(i).getLongitude());
//                Toast.makeText(this, "lat : "+addresses.get(i).getLatitude(),1).show();
//                Toast.makeText(this, "long : "+addresses.get(i).getLongitude(),1).show();
//            }
            return addresses;

        }
        catch (IOException e) {e.printStackTrace();}
        return null;
    }
    private void setSensorSelection(){
        if(fieldsList == null)
            return;
        String sensorID = fieldsList.get(POS_EDIT_FIELD).get("sensor").toString();
        int i = -1;
        for(String e: sensors){
            i++;
            if(e.equals(sensorID))
                sensorSpinner.setSelection(i);
        }
    }
    private void submitField(){
        findViewById(R.id.progress_loading_existing_fields).setVisibility(View.VISIBLE);
//        if(TextUtils.isEmpty(fieldType.getText())){
//            fieldType.setError(getString(R.string.field_empty));
//            return;
//        }
        if(TextUtils.isEmpty(pincode.getText())){
            pincode.setError(getString(R.string.field_empty));
            findViewById(R.id.progress_loading_existing_fields).setVisibility(View.GONE);
            return;
        }
        String pin= pincode.getText().toString();
        if(pin.length()<6){
            pincode.setError(getString(R.string.field_empty));
            findViewById(R.id.progress_loading_existing_fields).setVisibility(View.GONE);
            return;
        }
        if(TextUtils.isEmpty(dateSown.getText())){
            dateSown.setError(getString(R.string.field_empty));
            findViewById(R.id.progress_loading_existing_fields).setVisibility(View.GONE);
            return;
        }
        if(sensorSpinner.getSelectedItem().equals("NA")){
            sensorSpinner.performClick();
            showToast("Please select a sensor once its loaded!");
            findViewById(R.id.progress_loading_existing_fields).setVisibility(View.GONE);
            return;
        }

        Map<String, Object> fieldInfo = new HashMap<>();
        //fieldInfo.put("fieldType", fieldType.getText().toString());
        fieldInfo.put("fieldType", spinner.getSelectedItem().toString());
        fieldInfo.put("pincode", pin);
        fieldInfo.put("dateSown", dateSown.getText().toString());
        fieldInfo.put("sensor", sensorSpinner.getSelectedItem().toString());

        List<Address> addresses= findLocationName(pin);
        double loc_latitude = 28.34;
        double loc_longitude = 77.22;
        for(int i=0;i<addresses.size();i++){
                //Log.e(TAG,"in loop");
                //Log.e(TAG,"lat :,,,,,,,,,,,,,"+addresses.get(i).getLatitude()+"  long............"+addresses.get(i).getLongitude());
                //Toast.makeText(this, "lat : "+addresses.get(i).getLatitude(),Toast.LENGTH_SHORT).show();
                //Toast.makeText(this, "long : "+addresses.get(i).getLongitude(),Toast.LENGTH_SHORT).show();
                if((Double)addresses.get(i).getLatitude()!=null && (Double)addresses.get(i).getLongitude()!=null)
                {
                    loc_latitude=addresses.get(i).getLatitude();
                    loc_longitude=addresses.get(i).getLongitude();
                }
            }


        GeoPoint location = new GeoPoint(loc_latitude, loc_longitude);
        fieldInfo.put("location", location);
        fieldInfo.put("advisory", "Not Available");

        //List<Map<String, Object>> fieldsArray = new ArrayList<>();
        //fieldsArray.add(fieldInfo);

        if(POS_EDIT_FIELD>=0
                && fieldInfo.get("fieldType").equals(fieldsList.get(POS_EDIT_FIELD).get("fieldType"))
                && fieldInfo.get("pincode").equals(fieldsList.get(POS_EDIT_FIELD).get("pincode"))
                && fieldInfo.get("advisory").equals(fieldsList.get(POS_EDIT_FIELD).get("advisory"))
                && fieldInfo.get("dateSown").equals(fieldsList.get(POS_EDIT_FIELD).get("dateSown"))
                && fieldInfo.get("sensor").equals(fieldsList.get(POS_EDIT_FIELD).get("sensor"))
        ){
            showToast("No Changes Detected!");
            onBackPressed();
        }else {
            Log.e(TAG, "Final Map: " + fieldInfo.toString());
            DocumentReference docRef = db.collection("users").document(mAuth.getCurrentUser().getUid());
            docRef.update("fields", FieldValue.arrayUnion(fieldInfo)).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void unused) {
                    //Success
                    if (POS_EDIT_FIELD >= 0) {
                        removeFields(POS_EDIT_FIELD);
                        POS_EDIT_FIELD = -1;
                    } else {
                        onBackPressed();
                        findViewById(R.id.progress_loading_existing_fields).setVisibility(View.GONE);
                        showToast("Data Saved!");
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    findViewById(R.id.progress_loading_existing_fields).setVisibility(View.GONE);
                    Log.w(TAG, "Failed with: " + e.getMessage());
                    e.printStackTrace();
                    //Failed, Check Internet
                    showToast("Failed, Try Again!");
                }
            });
        }
    }

    private void removeFields(int AddressToRemove){
        DocumentReference docRef = db.collection("users").document(mAuth.getCurrentUser().getUid());
        docRef.update("fields", FieldValue.arrayRemove(fieldsList.get(AddressToRemove))).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                findViewById(R.id.progress_loading_existing_fields).setVisibility(View.GONE);
                Toast.makeText(AddNewField.this, "Updated Successfully!", Toast.LENGTH_SHORT).show();
                onBackPressed();
            }
        });
    }


    //    private void updateWeather(Location location){
//        new Thread(){
//            public void run() {
//                //get Weather start:
//                if(null != location){
//                    String request = "http://www.google.com/ig/api?weather=,,,"+location.getLatitude()
//                            +","+location.getLongitude();
//                    XmlParserUtil parser = new XmlParserUtil(mHandler);
//                    try {
//                        parser.parse(HttpUtil.getWeather(request));
//                    } catch (ClientProtocolException e) {
//                        e.printStackTrace();
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                }
//                //end
//            };
//        }.start();
//    }
    private void showToDoToast(){
        Toast.makeText(this, "TODO: Implement", Toast.LENGTH_SHORT).show();
    }
    private void showToast(String msg){
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}