
package com.cropadvisory;

import static com.cropadvisory.SoilField.KEY_SENSOR_ID;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FieldsActivity extends AppCompatActivity {

    Boolean mListFetched;
    private static String TAG = "FieldsActivity";
    private FirebaseAuth mAuth;
    FirebaseFirestore db;
    DocumentSnapshot doc;
    List<Map<String, Object>> fieldsList;
    Context ctx = this;
    ListView fieldListView ;
    List<String> main_text = new ArrayList<>();
    List<String> sub_text = new ArrayList<>();

    private ArrayAdapter<String> listAdapter ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fields);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        fieldListView = findViewById(R.id.listFields);
        fieldListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                fieldEditOptionsDialog(position);
                //Toast.makeText(ctx, String.format("Item Clicked: %d", position), Toast.LENGTH_SHORT).show();
            }
        });

        //fetchExistingFields();//Directly Calling in OnResume
        findViewById(R.id.addField).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(fieldsList!=null && fieldsList.size()>4){
                    showToast("Only 5 fields Allowed!");
                }else {
                    AddNewField(-1);
                }
            }
        });
    }

    /**
     *
     * @param positionToEdit == -1 to just add a new field else pass position for whichever element to edit.
     */
    private void AddNewField(int positionToEdit){
        Intent intent = new Intent(ctx, AddNewField.class);
        intent.putExtra(AddNewField.EDIT_FIELD, positionToEdit);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //Note to whoever develops later:
        // Better way will be to just add s DocumentChangeSnapshotListener which will auto update whenever required
        fetchExistingFields();
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
                        mListFetched = true;
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            doc = document;//Making it globally accessible
                            //List<Map<String, Object>> fieldsList;
                            main_text.clear();
                            sub_text.clear();
                            try {
                                fieldsList = (List<Map<String, Object>>) doc.get("fields");
                                if(fieldsList != null){
                                    int n = fieldsList.size();
                                    for(int i=0; i<n; i++){
                                        GeoPoint geoPoint = (GeoPoint) fieldsList.get(i).get("location");
                                        Geocoder gcd = new Geocoder(ctx, Locale.getDefault());
                                        double lat = geoPoint.getLatitude();
                                        double lng = geoPoint.getLongitude();
                                        List<Address> addresses = gcd.getFromLocation(lat, lng, 1);
                                        String city = "";
                                        if (addresses.size() > 0) {
                                            city = addresses.get(0).getLocality() + ", " + addresses.get(0).getSubAdminArea();
                                            city = city.replace("null", "");
                                        }
                                        else {
                                            // do your stuff
                                        }
                                        String main = fieldsList.get(i).get("fieldType") + " | " + city;
                                        String sub = (String) fieldsList.get(i).get("advisory");
                                        main_text.add(main);
                                        sub_text.add(sub);
                                    }
                                }
                            }catch (Exception e){e.printStackTrace();}
                            initListView();
                        } else {
                            Log.d(TAG, "User Id document not found.");
                        }
                    }
                }
            });
        }catch (Exception e){
            findViewById(R.id.progress_loading_existing_fields).setVisibility(View.GONE);
            Log.w(TAG, e.getMessage());
        }
    }


    private void initListView() {
        if(main_text.size()==0){
            findViewById(R.id.empty_fields_view).setVisibility(View.VISIBLE);
        }else{
            findViewById(R.id.empty_fields_view).setVisibility(View.GONE);
        }

        //listAdapter = new ArrayAdapter(this, R.layout.fieldlistitem, R.id.text_temp, main_text) {
        listAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_2, android.R.id.text1, main_text) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);

                TextView text1 = (TextView) view.findViewById(android.R.id.text1);
                TextView text2 = (TextView) view.findViewById(android.R.id.text2);
//                TextView text1 = (TextView) view.findViewById(R.id.text_temp);
//                TextView text2 = (TextView) view.findViewById(R.id.text2);
//                Button weather = (Button) view.findViewById(R.id.weather);
//                weather.setOnClickListener(new View.OnClickListener() {
//                    public void onClick(View v)
//                    {
//                        Intent intent = new Intent(FieldsActivity.this, weatherActivity.class);
//                        startActivity(intent);
//                    }
//                });
                text1.setText(main_text.get(position));
                text2.setText(sub_text.get(position));
                return view;
            }
        };
        fieldListView.setAdapter(listAdapter);
    }

    private void fieldEditOptionsDialog(int position){
        AlertDialog.Builder b = new AlertDialog.Builder(this);//, R.style.MyDialogTheme);
        String[] types = {"Weather Forecast","Soil feed","Edit Field", "Delete Field"};
        b.setItems(types, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

                dialog.dismiss();
                switch(which){
                    case 0:
                        GeoPoint geoPoint = (GeoPoint) fieldsList.get(position).get("location");
                        double lat = geoPoint.getLatitude();
                        double lng = geoPoint.getLongitude();

                        Intent intent = new Intent(FieldsActivity.this, weatherActivity.class);
                        intent.putExtra(weatherActivity.KEY_LAT, String.valueOf(lat));
                        intent.putExtra(weatherActivity.KEY_LONG, String.valueOf(lng));
                        startActivity(intent);
                        break;
                    case 1:
                        if(fieldsList.get(position).get("sensor") != null) {
                            Intent intent1 = new Intent(FieldsActivity.this, SoilField.class);
                            intent1.putExtra(KEY_SENSOR_ID, fieldsList.get(position).get("sensor").toString());
                            startActivity(intent1);
                        }else{
                            showToast("No Sensor Linked to the Field!");
                        }
                        break;
                    case 2:
                        editFields(position);
                        break;
                    case 3:
                        removeFields(position);
                        break;

                    default:
                        //Nothing
                }
            }

        });

        b.show();
    }

    private void editFields(int position){
        AddNewField(position);
    }

    private void removeFields(int AddressToRemove){
        DocumentReference docRef = db.collection("users").document(mAuth.getCurrentUser().getUid());
        docRef.update("fields", FieldValue.arrayRemove(fieldsList.get(AddressToRemove))).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                fetchExistingFields();//To Refresh Data and Update ListView
                Toast.makeText(FieldsActivity.this, "Deleted Successfully!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showToDoToast(){
        Toast.makeText(this, "TODO: Implement", Toast.LENGTH_SHORT).show();
    }
    private void showToast(String msg){
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }


}