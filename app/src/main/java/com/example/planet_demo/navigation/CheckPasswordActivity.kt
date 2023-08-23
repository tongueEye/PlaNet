package com.example.planet_demo.navigation

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.planet_demo.LoginActivity
import com.example.planet_demo.R
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_check_pw.*

class CheckPasswordActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_check_pw)

        remove_account_button.setOnClickListener {
            val currentPassword = check_password_edittext.text.toString()

            // Firebase 인증 객체 가져오기
            val auth = FirebaseAuth.getInstance()

            // 현재 로그인된 사용자 가져오기
            val currentUser = auth.currentUser

            // 현재 로그인된 사용자가 있다면
            currentUser?.let { user ->
                val credential = EmailAuthProvider.getCredential(user.email!!, currentPassword)

                // 비밀번호 재인증을 시도
                user.reauthenticate(credential)
                    .addOnSuccessListener {
                        // 비밀번호 재인증 성공 시 계정 삭제
                        user.delete()
                            .addOnSuccessListener {
                                // 계정 삭제 성공 시 처리
                                auth?.signOut()
                                val intent = Intent(this, LoginActivity::class.java)
                                startActivity(intent)
                                finish()
                            }
                            .addOnFailureListener { exception ->
                                // 계정 삭제 실패 시 처리
                                val errorMessage = "회원 탈퇴 실패: ${exception.message}"
                                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                            }
                    }
                    .addOnFailureListener { exception ->
                        // 비밀번호 재인증 실패 시 처리
                        val errorMessage = "비밀번호 재인증 실패: ${exception.message}"
                        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }
}