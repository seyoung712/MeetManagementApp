package com.baekseok.meet;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import java.io.FileInputStream;
import java.io.FileOutputStream;

public class MeetSchedule extends AppCompatActivity {
    public String readDay = null;
    public String str = null;
    public CalendarView calendarView;
    public Button cha_Btn, del_Btn, save_Btn;
    public TextView diaryTextView, textView2, textView3;
    public EditText contextEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.meet_schedule);

        //위젯 추가
        calendarView = findViewById(R.id.calendarView);//캘린더
        diaryTextView = findViewById(R.id.diaryTextView);//클릭한 날짜
        save_Btn = findViewById(R.id.save_Btn);//저장
        del_Btn = findViewById(R.id.del_Btn);//삭제
        cha_Btn = findViewById(R.id.cha_Btn);//수정
        textView2 = findViewById(R.id.textView2);//출력창
        contextEditText = findViewById(R.id.contextEditText); //입력창

        //1-날짜 클릭 시 (저장된 내용이 없는 날짜)
        calendarView.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(@NonNull CalendarView view, int year, int month, int dayOfMonth) {
                diaryTextView.setVisibility(View.VISIBLE);//해당 날짜 출력 활성화
                save_Btn.setVisibility(View.VISIBLE); //저장만 활성화
                cha_Btn.setVisibility(View.INVISIBLE);
                del_Btn.setVisibility(View.INVISIBLE);
                contextEditText.setVisibility(View.VISIBLE);//입력창 활성화
                textView2.setVisibility(View.INVISIBLE);//출력창 비활성화
                diaryTextView.setText(String.format("%d / %d / %d", year, month + 1, dayOfMonth));
                contextEditText.setText("");
                checkDay(year, month, dayOfMonth);
            }//onSelectedDayChange
        });//setOnDateChangeListener

        //2-저장버튼을 눌렀을 때 (이미 저장된 내용)
        save_Btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveDiary(readDay);
                str = contextEditText.getText().toString(); //EditText의 내용을 가져옴
                textView2.setText(str); //저장된 내용으로 출력
                save_Btn.setVisibility(View.INVISIBLE);
                cha_Btn.setVisibility(View.VISIBLE); //수정 활성화
                del_Btn.setVisibility(View.VISIBLE); //삭제 활성화
                contextEditText.setVisibility(View.INVISIBLE);//입력창 비활성화
                textView2.setVisibility(View.VISIBLE);//출력창 활성화

            }//onClick
        });//save_Btn.setOnClickListener
    }//onCreate



    //1. 날짜 클릭 함수
    public void checkDay(int cYear, int cMonth, int cDay) {
        readDay = "" + cYear + "-" + (cMonth + 1) + "" + "-" + cDay + ".txt";
        FileInputStream fis;

        try {
            fis = openFileInput(readDay);

            byte[] fileData = new byte[fis.available()];
            fis.read(fileData);
            fis.close();

            str = new String(fileData);

            contextEditText.setVisibility(View.INVISIBLE);//입력창 비활성화
            textView2.setVisibility(View.VISIBLE);//출력창 활성화
            textView2.setText(str);

            save_Btn.setVisibility(View.INVISIBLE);
            cha_Btn.setVisibility(View.VISIBLE);
            del_Btn.setVisibility(View.VISIBLE);

            //수정버튼
            cha_Btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    contextEditText.setVisibility(View.VISIBLE); //입력창 활성화
                    textView2.setVisibility(View.INVISIBLE); //출력창 비활성화
                    contextEditText.setText(str);

                    save_Btn.setVisibility(View.VISIBLE);//저장만 활성화
                    cha_Btn.setVisibility(View.INVISIBLE);
                    del_Btn.setVisibility(View.INVISIBLE);
                    textView2.setText(contextEditText.getText());//출력창을 수정내용으로 덮어쓰기
                }//onClick
            });//cha_Btn

            //삭제버튼
            del_Btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    textView2.setVisibility(View.INVISIBLE);
                    contextEditText.setText("");
                    contextEditText.setVisibility(View.VISIBLE);
                    save_Btn.setVisibility(View.VISIBLE);
                    cha_Btn.setVisibility(View.INVISIBLE);
                    del_Btn.setVisibility(View.INVISIBLE);
                    removeDiary(readDay);
                }//onClick
            });//del_Btn
            if (textView2.getText() == null) { //값이 존재할 때
                textView2.setVisibility(View.INVISIBLE);
                diaryTextView.setVisibility(View.VISIBLE);//캘린더 활성화
                save_Btn.setVisibility(View.VISIBLE);//저장만 활성화
                cha_Btn.setVisibility(View.INVISIBLE);
                del_Btn.setVisibility(View.INVISIBLE);
                contextEditText.setVisibility(View.VISIBLE); //입력창 활성화
            }//if

        } catch (Exception e) {
            e.printStackTrace();
        }//try-catch
    }//checkDay

    //2. 삭제버튼 함수
    @SuppressLint("WrongConstant")
    public void removeDiary(String readDay) {
        FileOutputStream fos;
        try {
            fos = openFileOutput(readDay, MODE_NO_LOCALIZED_COLLATORS);
            String content = "";
            fos.write((content).getBytes());
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }//try-catch
    }//removeDiary

    //3. 저장버튼 함수
    @SuppressLint("WrongConstant")
    public void saveDiary(String readDay) {
        FileOutputStream fos;
        try {
            fos = openFileOutput(readDay, MODE_NO_LOCALIZED_COLLATORS);
            String content = contextEditText.getText().toString();
            fos.write((content).getBytes());
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }//try-catch
    }//saveDiary


}//Class
