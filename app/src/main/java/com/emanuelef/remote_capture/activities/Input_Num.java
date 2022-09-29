package com.emanuelef.remote_capture.activities;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.emanuelef.remote_capture.R;

public class Input_Num extends AppCompatActivity {
    Database db;
    Button button;
    EditText editText;
    //현재 매니피스트에서 메인페이지 변경했음 내부 DB에 폰 번호 저장해서 처음 켜졌을 때, DB에 번호가 있다면 메인 기능 페이지로 인텐트
    //없다면 여기서 전화번호 입력하고 내부 DB에 저장한 후, 인텐트하는 형식으로
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.input_num);

        button = (Button) findViewById(R.id.PhoneNum_Button);
        editText = (EditText) findViewById(R.id.editPhoneNum);
        db = new Database(this);

        AddData();
        viewAll();

        if(viewAll() == true){
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(intent);
        }
    }

    //데이터베이스 추가하기
    //데이터베이스 추가하기
    public void AddData(){
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (editText.length() != 11) {
                    Toast.makeText(getApplicationContext(), "전화번호를 정확히 입력해주세요.", Toast.LENGTH_SHORT);
                } else {
                    boolean isInserted = db.insertData(editText.getText().toString());
                    if (isInserted == true) {
                        Toast.makeText(Input_Num.this, "데이터추가 성공", Toast.LENGTH_LONG).show();
                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                        startActivity(intent);
                    }
                    else
                        Toast.makeText(Input_Num.this, "데이터추가 실패", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    // 데이터베이스 읽어오기
    public boolean viewAll() {
        Cursor res = db.getAllData();
        if (res.getCount() == 0) {
        //데이터 있나 없나 확인 없으면 0을
            return false;
        }
        else{
        //있다면 1을 반환
            return true;
        }

    }
}
