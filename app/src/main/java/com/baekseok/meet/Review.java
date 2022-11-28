package com.baekseok.meet;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class Review extends Activity {
    String FILENAME="review.txt";
    RatingBar rating;
    int star=0;
    TextView text;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.review);
        rating =(RatingBar) findViewById(R.id.rating);
        text=(TextView) findViewById(R.id.text);

    }//onCreate

    //등록
    public void btn1Clicked(View view){
        try{
            FileOutputStream fos = openFileOutput(FILENAME,MODE_APPEND); //쓰기
/*
            FileInputStream fis = openFileInput(FILENAME); //읽기
*/

            star=(int)rating.getRating();

            //쓰기-파일에 저장
            String str = star + "점 | " + text.getText().toString() + ";";
            fos.write(str.getBytes());

            Toast.makeText(getApplicationContext(),"평가가 등록되었습니다. 감사합니다^^",Toast.LENGTH_SHORT).show();

/*
            //읽기
            byte[] buffer = new byte[fis.available()];
            fis.read(buffer);

            text.setText("");
            String[] count = (new String(buffer)).split(";");

            for(int i=0; i<count.length; i++) {
                text.setText(text.getText().toString() + "\n" + count[i]);
            }
*/

            fos.close();
/*
            fis.close();
*/

        } catch (IOException e){
            e.printStackTrace();
        }
    }//btn1Clicked

    //삭제
    public void btn2Clicked(View view) {
        try{
            FileOutputStream fos = openFileOutput(FILENAME,MODE_PRIVATE);
            FileInputStream fis = openFileInput(FILENAME);

            fos.write("".getBytes());

            byte[] buffer = new byte[fis.available()];
            fis.read(buffer);
            text.setText(new String(buffer));

            fos.close();
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}//Review
