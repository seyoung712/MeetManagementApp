package com.baekseok.meet;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {
    // 변수 선언
    Button BtnRegister, BtnLogin;
    EditText LoginEmail, LoginPassword;
    FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 변수 할당
        BtnRegister = (Button) findViewById(R.id.btnRegister);
        BtnLogin = (Button) findViewById(R.id.btnLogin);
        LoginEmail = (EditText) findViewById(R.id.LoginEmail);
        LoginPassword = (EditText) findViewById(R.id.LoginPassword);
        firebaseAuth = firebaseAuth.getInstance();

        BtnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), Register.class));
            }
        }); // 회원가입 액티비티로 이동.

        BtnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 유효성 확인
                if(LoginEmail == null || LoginPassword == null) {
                    Toast.makeText(getApplicationContext(), "입력하지 않은 값이 있습니다.", Toast.LENGTH_SHORT).show();
                    return;
                }

                String Email = LoginEmail.getText().toString();
                String Password = LoginPassword.getText().toString();

                firebaseAuth.signInWithEmailAndPassword(Email, Password)
                        .addOnCompleteListener(MainActivity.this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if(task.isSuccessful()) {
                                    startActivity(new Intent(getApplicationContext(), ConnectActivity.class));
                                } else {
                                    Toast.makeText(getApplicationContext(), "이메일 혹은 비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                            }
                        });
            }
        }); // 로그인 처리과정 프로세스 (아이디, 비밀번호 불러와 firebase 내 데이터베이스와 비교처리함)

    } // onCreate

} // MainActivity