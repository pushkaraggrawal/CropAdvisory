package com.cropadvisory;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static String TAG = "MainActivity";
    private static int RC_SIGN_IN = 192;

    Button mLogin;
    private FirebaseAuth mAuth;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        mLogin = findViewById(R.id.login);
        //openLoginPage();
        if(mAuth.getCurrentUser() != null){
            fetchInfo();
            mLogin.setText("Log Out");
        }else{
            openLoginPage();
        }

        findViewById(R.id.gotoFields).setOnClickListener(this);
        findViewById(R.id.submit).setOnClickListener(this);
        mLogin.setOnClickListener(this);
    }

    private void fetchInfo(){

        DocumentReference docRef = db.collection("users").document(mAuth.getCurrentUser().getUid());
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if(task.isSuccessful()){
                    DocumentSnapshot doc = task.getResult();
                    if(doc.exists()){
                        //Doc Exists
                        EditText name = findViewById(R.id.name);
                        EditText phone = findViewById(R.id.phone);
                        EditText aadhar  = findViewById(R.id.aadhar);
                        name.setText(doc.getString("name"));
                        aadhar.setText(doc.getString("aadhar"));
                        phone.setText(doc.getString("phone"));

                    }else{
                        //Doc doesn't Exist
                    }
                }else{
                    Log.d(TAG, "get failed with" + task.getException());
                }
            }
        });
    }

    private void openLoginPage(){
        Intent intent = new Intent(this, LoginRegisterActivity.class);
        startActivity(intent);
    }
    private void showToDoToast(){
        Toast.makeText(this, "TODO: Implement", Toast.LENGTH_SHORT).show();
    }
    private void showToast(String msg){
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if(id==R.id.login){
            login();
        }
        else if(id == R.id.submit){
            submitForm();
        }
        else if(id == R.id.gotoFields){
            if(mAuth.getCurrentUser()==null){
                showToast("Please Login First!");
                login();
                return;
            }
            Intent intent = new Intent(this, FieldsActivity.class);
            startActivity(intent);
        }
    }

    private void login(){
        if(mAuth.getCurrentUser()!=null){
            mAuth.signOut();
            mLogin.setText(getString(R.string.log_in));
            openLoginPage();
            return;
        }

        List<AuthUI.IdpConfig> provider = Arrays.asList(
                new AuthUI.IdpConfig.PhoneBuilder().build()
        );

        Intent intent = AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(provider)
                .setLogo(R.drawable.common_google_signin_btn_icon_dark)
                .build();

        startActivityForResult(intent, RC_SIGN_IN);
    }

    private void submitForm(){
        if(mAuth.getCurrentUser()==null){
            showToast("Please Login First!");
            login();
            return;
        }
        EditText name = findViewById(R.id.name);
        EditText phone = findViewById(R.id.phone);
        EditText aadhar  = findViewById(R.id.aadhar);

        if(TextUtils.isEmpty(name.getText())){
            name.setError(getString(R.string.field_empty));
            return;
        }else if(TextUtils.isEmpty(phone.getText())){
            phone.setError(getString(R.string.field_empty));
            return;
        }else if(TextUtils.isEmpty(aadhar.getText())){
            aadhar.setError(getString(R.string.field_empty));
            return;
        }

        Map<String, String> userInfo = new HashMap<>();
        userInfo.put("name", name.getText().toString());
        userInfo.put("phone", phone.getText().toString());
        userInfo.put("aadhar", aadhar.getText().toString());

        DocumentReference docRef = db.collection("users").document(mAuth.getCurrentUser().getUid());
        docRef.set(userInfo, SetOptions.merge()).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                //Success
                showToast("Data Saved!");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.w(TAG, "Failed with: " + e.getMessage());
                e.printStackTrace();
                //Failed, Check Internet
                showToast("Failed, Try Again!");
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == RC_SIGN_IN && resultCode == RESULT_OK){
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            showToast("Sign in Successfully: " + user.getDisplayName());
            mLogin.setText("Log Out");
        }else{
            showToast("Log In Failed");
        }
    }
}