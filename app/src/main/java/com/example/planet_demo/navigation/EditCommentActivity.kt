package com.example.planet_demo.navigation

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.*
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.planet_demo.R
import com.google.firebase.firestore.FirebaseFirestore

class EditCommentActivity : AppCompatActivity() {

    private lateinit var contentUid: String
    private lateinit var commentId: String
    private lateinit var originalComment: String
    private lateinit var profileImage: String
    private lateinit var userId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.item_edit_comment)

        contentUid = intent.getStringExtra("contentUid") ?: ""
        commentId = intent.getStringExtra("commentId") ?: ""
        originalComment = intent.getStringExtra("comment") ?: ""
        profileImage = intent.getStringExtra("profileImage") ?: ""
        userId = intent.getStringExtra("userId") ?: ""

        val editCommentEditText: EditText = findViewById(R.id.commentviewitem_edittext_comment)
        editCommentEditText.setText(originalComment)

        val profileImageView: ImageView = findViewById(R.id.commentviewitem_imageview_profile)
        val userIdTextView: TextView = findViewById(R.id.commentviewitem_textview_profile)

        // 프로필 이미지와 userId 설정
        Glide.with(this).load(profileImage).apply(RequestOptions().circleCrop()).into(profileImageView)
        userIdTextView.text = userId

        val saveButton: ImageView = findViewById(R.id.commentviewitem_option_btn)
        saveButton.setOnClickListener {
            val updatedComment = editCommentEditText.text.toString()
            updateCommentInFirestore(updatedComment)
        }
    }

    private fun updateCommentInFirestore(updatedComment: String) {

        // 확인을 위해 contentUid와 commentId 값을 출력해보자
        val commentsCollectionRef = FirebaseFirestore.getInstance()
            .collection("images")
            .document(contentUid)
            .collection("comments")

        // "commentId" 필드가 commentId 변수 값과 일치하는 문서 가져오기
        val query = commentsCollectionRef.whereEqualTo("commentId", commentId)

        query.get().addOnSuccessListener { querySnapshot ->
            if (!querySnapshot.isEmpty) {
                val commentDocument = querySnapshot.documents[0]
                commentDocument.reference.update("comment", updatedComment)
                    .addOnSuccessListener {
                        Toast.makeText(this, "댓글이 수정되었습니다.", Toast.LENGTH_SHORT).show()
                        finish() // 작업 완료 후 액티비티 종료
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "댓글 수정에 실패하였습니다.", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "댓글을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "댓글을 불러오는 데 실패하였습니다.", Toast.LENGTH_SHORT).show()
        }
    }
}