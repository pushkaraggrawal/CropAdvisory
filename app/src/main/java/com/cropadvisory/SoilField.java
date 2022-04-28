package com.cropadvisory;

import android.content.Context;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class SoilField extends AppCompatActivity {
    public static final String KEY_SENSOR_ID = "sensor_id";
    String sensorId;
    TextView mSensorName, mSensorActive, mHumidity, mMoisture, mNitrogen, mTemperature;
    private Context ctx = this;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_soil_field);

        mSensorName = findViewById(R.id.textView11);
        mSensorActive = findViewById(R.id.textView);
        mHumidity = findViewById(R.id.textView2);
        mMoisture = findViewById(R.id.textView3);
        mNitrogen = findViewById(R.id.textView4);
        mTemperature = findViewById(R.id.textView5);

        sensorId = getIntent().getExtras().getString(KEY_SENSOR_ID);
        if(sensorId == null){
            Toast.makeText(ctx, "No Sensor ID Found!", Toast.LENGTH_SHORT).show();
        }else {
            mSensorName.setText(sensorId);
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child(sensorId);
            ref.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        HashMap<String, Number> sensorData = new HashMap<>();
                        sensorData = (HashMap<String, Number>) snapshot.getValue();
                        //Log.w(TAG, snapshot);
                        //Log.w(TAG, sensors);
                        if (sensorData != null) {
                            try {
                                mSensorActive.setText(String.valueOf(sensorData.get("Active")));
                                mHumidity.setText(String.valueOf(sensorData.get("Humidity")));
                                mMoisture.setText(String.valueOf(sensorData.get("Moisture")));
                                mNitrogen.setText(String.valueOf(sensorData.get("Nitrogen")));
                                mTemperature.setText(String.valueOf(sensorData.get("Temperature")));
                            }catch (Exception e){
                                e.printStackTrace();
                                Toast.makeText(ctx, "A measured sensor value is not a number/string", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(ctx, "No Sensor Data Found for: " + sensorId, Toast.LENGTH_SHORT).show();
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }
    }
}