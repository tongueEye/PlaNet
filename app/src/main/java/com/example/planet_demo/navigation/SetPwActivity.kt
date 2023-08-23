package com.example.planet_demo.navigation

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.planet_demo.LoginActivity
import com.example.planet_demo.R
import com.google.firebase.auth.FirebaseAuth

class SetPwActivity : AppCompatActivity() {

    private lateinit var newPasswordEditText: EditText
    private lateinit var newPasswordCheckEditText: EditText
    private lateinit var setPasswordButton: Button
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_set_pw)

        firebaseAuth = FirebaseAuth.getInstance()

        newPasswordEditText = findViewById(R.id.new_password_edittext)
        newPasswordCheckEditText = findViewById(R.id.new_password_check_edittext)
        setPasswordButton = findViewById(R.id.set_password_button)

        setPasswordButton.setOnClickListener {
            val newPassword = newPasswordEditText.text.toString()
            val newPasswordCheck = newPasswordCheckEditText.text.toString()

            if (newPassword.isNotEmpty() && newPassword == newPasswordCheck) {
                val user = firebaseAuth.currentUser
                if (user != null) {
                    user.updatePassword(newPassword)
                        .addOnCompleteListener { updatePasswordTask ->
                            if (updatePasswordTask.isSuccessful) {
                                // Password updated successfully
                                Toast.makeText(this, "비밀번호 변경에 성공했습니다.", Toast.LENGTH_SHORT).show()
                                Toast.makeText(this, "다시 로그인 해주세요.", Toast.LENGTH_SHORT).show()

                                // 로그아웃 처리
                                firebaseAuth.signOut()

                                // 로그인 화면으로 이동
                                val intent = Intent(this, LoginActivity::class.java)
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                                startActivity(intent)
                                finish()
                            } else {
                                // Failed to update password
                                // Handle the error
                                Toast.makeText(this, "비밀번호 변경에 실패했습니다.", Toast.LENGTH_SHORT).show()
                            }
                        }
                }
            } else {
                // Passwords do not match
                Toast.makeText(this, "비밀번호를 다시 확인해주세요.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
