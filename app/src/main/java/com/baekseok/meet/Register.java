// [!] AndroidManifest 추가 완료

package com.baekseok.meet;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class Register extends AppCompatActivity {
    private static final String ActivityName = "RegisterActivity";

    // 변수 선언
    EditText RegisterEmail, RegisterPassword, RegisterPasswordCheck;
    Button Register;
    FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register);

        // 변수 할당
        RegisterEmail = (EditText) findViewById(R.id.RegisterEmail);
        RegisterPassword = (EditText) findViewById(R.id.RegisterPassword);
        RegisterPasswordCheck = (EditText) findViewById(R.id.RegisterPasswordCheck);
        // RegisterNickname = (EditText) findViewById(R.id.RegisterNickname);
        Register = (Button) findViewById(R.id.Register);

        // 파이어베이스 접근
        firebaseAuth = FirebaseAuth.getInstance();

        Register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 유효성 확인
                if(RegisterEmail.getText().toString().length() == 0 || RegisterPassword.getText().toString().length() == 0) {
                    Toast.makeText(getApplicationContext(), "입력되지 않은 값이 있습니다.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if(!RegisterPassword.getText().toString().equals(RegisterPasswordCheck.getText().toString())) {
                    Toast.makeText(getApplicationContext(), "비밀번호와 비밀번호 확인 값이 다릅니다.", Toast.LENGTH_SHORT).show();
                    return;
                }

                String Email = RegisterEmail.getText().toString();
                String Password = RegisterPassword.getText().toString();

                firebaseAuth.createUserWithEmailAndPassword(Email, Password)
                        .addOnCompleteListener(com.baekseok.meet.Register.this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if(task.isSuccessful()) {
                                    startActivity(new Intent(getApplicationContext(), MainActivity.class));
                                    finish();
                                } else {
                                    Toast.makeText(com.baekseok.meet.Register.this, "회원가입에 오류가 발생하였습니다.", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                            }
                        });
            }
        }); // 회원가입 로직

    } // onCreate

} // RegisterActivity