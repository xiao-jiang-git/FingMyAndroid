package com.xiaojiang.sbu.justwifi;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;


public class messageBoardActivity extends AppCompatActivity {

    private Button send;
    private EditText text;
    private String name;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.messageboard);

        send = findViewById(R.id.sender);
        text = findViewById(R.id.message);

        Intent intent = getIntent();
        name = intent.getStringExtra("name");
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = text.getText().toString();
                if(message == ""){
                    Toast.makeText(messageBoardActivity.this, "There is nothing to send!",Toast.LENGTH_SHORT).show();

                }else{
                    DatabaseReference databaseReference;
                    databaseReference = FirebaseDatabase.getInstance().getReference("Message");
                    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置日期格式
                    String date = df.format(new Date());
                    MessageHelper messageHelper = new MessageHelper(message, name, date);
                    //child（phone）把phone作为唯一标识符
                    databaseReference.child(name).setValue(messageHelper);
                    Toast.makeText(messageBoardActivity.this, "Thanks for your feedback!",Toast.LENGTH_LONG).show();
                    text.setText("");


                }
            }
        });


    }
}
