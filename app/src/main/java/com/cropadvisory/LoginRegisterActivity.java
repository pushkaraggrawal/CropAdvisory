package com.cropadvisory;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;
import java.util.List;

public class LoginRegisterActivity extends AppCompatActivity {
    private static String TAG = "LoginActivity";
    private static int RC_SIGN_IN = 192;

    private FirebaseAuth mAuth;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        Button mLoginBtn = findViewById(R.id.btn_login);
        mLoginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                login();
            }
        });
    }

    private void login(){
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == RC_SIGN_IN && resultCode == RESULT_OK){
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            showToast("Log in Successful: " + user.getDisplayName());
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        }else{
            showToast("Log In Failed");
        }
    }

    private void showToDoToast(){
        Toast.makeText(this, "TODO: Implement", Toast.LENGTH_SHORT).show();
    }
    private void showToast(String msg){
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }


}