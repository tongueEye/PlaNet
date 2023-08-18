package com.example.planet_demo

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.bumptech.glide.Glide
import com.example.planet_demo.R
import com.example.planet_demo.navigation.model.ContentDTO
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import kotlinx.android.synthetic.main.activity_add_photo.*
import java.text.SimpleDateFormat
import java.util.*

class AddPhotoActivity : AppCompatActivity() {
    var PICK_IMAGE_FROM_ALBUM=0
    var storage: FirebaseStorage?=null
    var photoUri: Uri?=null

    var auth:FirebaseAuth?=null
    var firestore: FirebaseFirestore?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_photo)

        //Initiate storages
        storage=FirebaseStorage.getInstance()
        auth= FirebaseAuth.getInstance()
        firestore=FirebaseFirestore.getInstance()

        // Check if in edit mode
        val editMode = intent.getBooleanExtra("edit_mode", false)

        //Open the album
        var photoPickerIntent=Intent(Intent.ACTION_PICK)
        photoPickerIntent.type="image/*"
        startActivityForResult(photoPickerIntent,PICK_IMAGE_FROM_ALBUM)

        //add image upload event
        addphoto_btn_upload.setOnClickListener {
            if (editMode) {
                val contentId = intent.getStringExtra("content_id")
                loadContentForEditing(contentId)

                // 수정 모드일 경우 업데이트 로직 실행
                val newExplain = addphoto_edit_explain.text.toString()
                if (contentId != null) {
                    updateContent(contentId, newExplain)
                }
            } else {
                // 새로운 글을 작성하는 모드
                contentUpload()
            }
            //contentUpload()
        }
    }

    private fun loadContentForEditing(contentId: String?) {
        firestore?.collection("images")?.whereEqualTo("contentId", contentId)
            ?.get()
            ?.addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val documentSnapshot = querySnapshot.documents[0]
                    val contentDTO = documentSnapshot.toObject(ContentDTO::class.java)

                    if (contentDTO != null) {
                        // 이미지 로딩 및 설명 업데이트
                        Glide.with(this)
                            .load(contentDTO.imageUrl)
                            .into(addphoto_image)

                        addphoto_edit_explain.setText(contentDTO.explain)
                    }
                } else {
                    // 매칭되는 "contentId"를 가진 문서가 없을 경우 처리
                    Toast.makeText(this, "해당 글을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
            }
            ?.addOnFailureListener { exception ->
                // 실패 시 처리하는 코드
                Toast.makeText(this, "글 정보를 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
    }

    // 수정 버튼 클릭 시 호출되는 함수
    private fun updateContent(contentId: String, newExplain: String) {
        val imageFileName = "IMAGE_${SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())}_.png"
        val storageRef = storage?.reference?.child("images")?.child(imageFileName)

        // 이미지 업로드
        storageRef?.putFile(photoUri!!)
            ?.continueWithTask { task: Task<UploadTask.TaskSnapshot> ->
                return@continueWithTask storageRef.downloadUrl
            }?.addOnSuccessListener { uri ->
                val newImageUrl = uri.toString()

                firestore?.collection("images")?.whereEqualTo("contentId", contentId)
                    ?.get()
                    ?.addOnSuccessListener { querySnapshot ->
                        if (!querySnapshot.isEmpty) {
                            for (documentSnapshot in querySnapshot.documents) {
                                val documentReference = documentSnapshot.reference

                                val originalImageUrl = documentSnapshot.getString("imageUrl") // Store original image URL for deletion

                                documentReference.update(
                                    "imageUrl", newImageUrl,
                                    "explain", newExplain
                                )
                                    .addOnSuccessListener {

                                        // Remove the original image
                                        if (!originalImageUrl.isNullOrEmpty()) {
                                            val originalImageRef = storage?.getReferenceFromUrl(originalImageUrl!!)
                                            originalImageRef?.delete()
                                        }

                                        Toast.makeText(this, "글이 수정되었습니다.", Toast.LENGTH_SHORT).show()
                                        setResult(Activity.RESULT_OK)
                                        finish()
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(this, "글 수정에 실패했습니다.", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        } else {
                            Toast.makeText(this, "해당 글을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                        }
                    }
                    ?.addOnFailureListener { e ->
                        Toast.makeText(this, "글 정보를 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show()
                    }
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode==PICK_IMAGE_FROM_ALBUM){
            if (resultCode==Activity.RESULT_OK){
                //This is path to the selected image
                photoUri=data?.data
                addphoto_image.setImageURI(photoUri)
            }else{
                //Exit the addPhotoActivity if you leave the album without selecting it
                finish()
            }
        }
    }
    fun contentUpload(){
        //Make filename
        var timestamp=SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        var imageFileName="IMAGE_"+timestamp+"_.png"
        var storageRef=storage?.reference?.child("images")?.child(imageFileName)

        //file Upload
        //Promise method
        storageRef?.putFile(photoUri!!)?.continueWithTask { task: Task<UploadTask.TaskSnapshot>->
            return@continueWithTask storageRef.downloadUrl
        }?.addOnSuccessListener { uri->
            var contentDTO=ContentDTO()
            //Insert downloadUrl of image
            contentDTO.imageUrl=uri.toString()
            //Insert uid of user
            contentDTO.uid=auth?.currentUser?.uid
            //Insert userId
            contentDTO.userId=auth?.currentUser?.email
            //Insert explain of content
            contentDTO.explain=addphoto_edit_explain.text.toString()
            //Insert timestamp
            contentDTO.timestamp=System.currentTimeMillis()

            firestore?.collection("images")?.document()?.set(contentDTO)

            setResult(Activity.RESULT_OK)

            finish()
        }
    }
}