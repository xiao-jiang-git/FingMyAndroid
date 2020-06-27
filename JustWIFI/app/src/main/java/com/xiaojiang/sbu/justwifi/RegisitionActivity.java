package com.xiaojiang.sbu.justwifi;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.irozon.ratifier.Ratifier;

public class RegisitionActivity  extends AppCompatActivity {

    EditText regname, regemail, regpassword, regphone;
    Button submit;

    FirebaseDatabase rootNode;
    DatabaseReference reference;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.regisition);
        regname = findViewById(R.id.regname);
        regemail = findViewById(R.id.regemail);
        regpassword = findViewById(R.id.regpassword);
        regphone = findViewById(R.id.regphone);

        submit = findViewById(R.id.submit);

        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Ratifier.Valid ratify = Ratifier.getValidity(RegisitionActivity.this);
                if (ratify.isValid()) { // Form is valid
                    Toast.makeText(RegisitionActivity.this, "Sign Up Success", Toast.LENGTH_SHORT).show();
                    //到root Node
                    rootNode = FirebaseDatabase.getInstance();
                    reference = rootNode.getReference("Users");
                    String name = regname.getText().toString();
                    String email = regemail.getText().toString();
                    String password = regpassword.getText().toString();
                    String phone = regphone.getText().toString();

                    UserHelper userhelper = new UserHelper(name, email, password, phone);
                    //child（phone）把phone作为唯一标识符
                    reference.child(email).setValue(userhelper);
                    Intent it = new Intent(RegisitionActivity.this, LoginActivity.class);
                    startActivity(it);
                } else { // Form is not valid
                    Toast.makeText(RegisitionActivity.this, ratify.getErrorMsg(), Toast.LENGTH_SHORT).show();
                }

            }
        });

    }

}
