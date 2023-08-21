package com.example.planet_demo

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.example.planet_demo.MainActivity
import com.example.planet_demo.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_set_info.*

class SetInfoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_set_info)

        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        // Firestore에서 "infos" 컬렉션에서 uid와 일치하는 문서를 가져옴
        currentUser?.uid?.let { uid ->
            val userDocRef = FirebaseFirestore.getInstance().collection("infos").document(uid)
            userDocRef.get()
                .addOnSuccessListener { documentSnapshot ->
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        val nickname = documentSnapshot.getString("nickname")
                        val bio = documentSnapshot.getString("bio")

                        // 가져온 데이터를 입력 필드에 설정
                        nickname_edittext.setText(nickname)
                        bio_edittext.setText(bio)
                    }
                }
                .addOnFailureListener { e ->
                    // Show the error message
                    Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
                }
        }

        set_profile_button.setOnClickListener {
            val nickname = nickname_edittext.text.toString().trim()
            val bio = bio_edittext.text.toString().trim()

            if (nickname.isEmpty() || bio.isEmpty()) {
                Toast.makeText(this, "모든 항목을 입력해주세요!", Toast.LENGTH_SHORT).show()
            } else if (nickname.length > 10) {
                Toast.makeText(this, "닉네임을 10자 이하로 입력해주세요!", Toast.LENGTH_SHORT).show()
            } else if (bio.length > 50) {
                Toast.makeText(this, "소개글을 50자 이하로 입력해주세요!", Toast.LENGTH_SHORT).show()
            } else {
                val uid = currentUser?.uid
                if (uid != null) {
                    val userDocRef = FirebaseFirestore.getInstance().collection("infos").document(uid)

                    val userInfo = HashMap<String, Any>()
                    userInfo["nickname"] = nickname
                    userInfo["bio"] = bio

                    userDocRef.set(userInfo)
                        .addOnSuccessListener {
                            // User info is set, move to main page
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        }
                        .addOnFailureListener { e ->
                            // Show the error message
                            Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
                        }
                }
            }

            // 데이터를 Firebase Firestore의 "infos" 컬렉션에 업데이트
            currentUser?.uid?.let { uid ->
                val userDocRef = FirebaseFirestore.getInstance().collection("infos").document(uid)

                val userInfo = HashMap<String, Any>()
                userInfo["nickname"] = nickname
                userInfo["bio"] = bio

                userDocRef.set(userInfo)
                    .addOnSuccessListener {
                        // User info is set, move to main page
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
                    .addOnFailureListener { e ->
                        // Show the error message
                        Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
                    }
            }
        }



    }
}