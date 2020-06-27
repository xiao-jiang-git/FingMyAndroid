package com.xiaojiang.sbu.justwifi;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Pair;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class LoginActivity extends AppCompatActivity {
    Button callSignUp, login_btn;
    ImageView image;
    TextView logoText, sloganText;
    TextInputLayout email, password;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); //This Line will hide the status bar from the screen
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.login);

        if (Build.VERSION.SDK_INT >= 23) {//6.0才用动态权限
            initPermission();
        }

        //Hooks
        callSignUp = findViewById(R.id.signup);
        image = findViewById(R.id.logo_image);
        logoText = findViewById(R.id.logo_name);
        sloganText = findViewById(R.id.slogan_name);
        email = findViewById(R.id.emailinput);
        password = findViewById(R.id.password);
        login_btn = findViewById(R.id.login_btn);
        callSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LoginActivity.this, RegisitionActivity.class);
                startActivity(intent);
            }
        });

        login_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginUser(v);
            }
        });
    }


    String[] permissions = new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.READ_PHONE_STATE};
    List<String> mPermissionList = new ArrayList<>();

    private final int mRequestCode = 100;

    private void initPermission() {

        mPermissionList.clear();

        for (int i = 0; i < permissions.length; i++) {
            if (ContextCompat.checkSelfPermission(this, permissions[i]) != PackageManager.PERMISSION_GRANTED) {
                mPermissionList.add(permissions[i]);
            }
        }

        //申请权限
        if (mPermissionList.size() > 0) {
            ActivityCompat.requestPermissions(this, permissions, mRequestCode);
        }else{
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean hasPermissionDismiss = false;//有权限没有通过
        if (mRequestCode == requestCode) {
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] == -1) {
                    hasPermissionDismiss = true;
                }
            }
            //如果有权限没有被允许
            if (hasPermissionDismiss) {
                showPermissionDialog();
            }else{

            }
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     * 不再提示权限时的展示对话框
     */
    AlertDialog mPermissionDialog;
    String mPackName = "com.example.administrator.alarmclock";

    private void showPermissionDialog() {
        if (mPermissionDialog == null) {
            mPermissionDialog = new AlertDialog.Builder(this)
                    .setMessage("Permission disabled, are you sure to grant permission")
                    .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            cancelPermissionDialog();

                            Uri packageURI = Uri.parse("package:" + mPackName);
                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageURI);
                            startActivity(intent);

                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //关闭页面或者做其他操作
                            cancelPermissionDialog();

                        }
                    })
                    .create();
        }
        mPermissionDialog.show();
    }

    //关闭对话框
    private void cancelPermissionDialog() {
        mPermissionDialog.cancel();
    }



    public void loginUser(View view){
        if(!validateUsername() | !validatePassword()){
            return;
        }
        else{
            isUser();
        }
    }

    private void isUser() {
        String userEnteredEmail = email.getEditText().getText().toString().trim();
        String userEnteredPassword = password.getEditText().getText().toString().trim();

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");

        Query checkUser = reference.orderByChild("email").equalTo(userEnteredEmail);

        checkUser.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){

                    email.setError(null);
                    //输入username下的password
                    String passwordFromDB = dataSnapshot.child(userEnteredEmail).child("password").getValue(String.class);

                    if(passwordFromDB.equals(userEnteredPassword)){
                        String nameFromDB = dataSnapshot.child(userEnteredEmail).child("name").getValue(String.class);
                        String emailFromDB = dataSnapshot.child(userEnteredEmail).child("email").getValue(String.class);
                        String phoneFromDB = dataSnapshot.child(userEnteredEmail).child("phone").getValue(String.class);

                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                        intent.putExtra("name",nameFromDB);
                        intent.putExtra("email",emailFromDB);
                        intent.putExtra("phone",phoneFromDB);

                        startActivity(intent);
                        TimerTask task = new TimerTask() {
                            @Override
                            public void run() {
                                LoginActivity.this.finish();
                            }
                        };
                        Timer timer = new Timer();
                        timer.schedule(task, 3000);

                    }
                    else{
                        password.setError("Wrong Password");
                        password.requestFocus();
                    }
                }else{
                    email.setError("No such user exist! ");
                    password.requestFocus();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private boolean validatePassword() {
        String val = password.getEditText().getText().toString();
        if(val.isEmpty()){
            password.setError("Field cannot be empty!");
            return false;
        }else{
            password.setError(null);
            return true;
        }
    }

    private Boolean validateUsername(){
        String val = email.getEditText().getText().toString();
        if(val.isEmpty()){
            email.setError("Field cannot be empty!");
            return false;
        }else{
            email.setError(null);
            return true;
        }
    }
}