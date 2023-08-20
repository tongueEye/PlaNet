package com.example.planet_demo

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_set_info.*

class SetInfoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_set_info)

        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        set_profile_button.setOnClickListener {
            val nickname = nickname_edittext.text.toString().trim()
            val bio = bio_edittext.text.toString().trim()

            if (nickname.isEmpty() || bio.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
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
        }
    }
}