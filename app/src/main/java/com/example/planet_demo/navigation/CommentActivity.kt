package com.example.planet_demo.navigation

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.planet_demo.R
import com.example.planet_demo.navigation.model.AlarmDTO
import com.example.planet_demo.navigation.model.ContentDTO
import com.example.planet_demo.navigation.util.FcmPush
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_comment.*
import kotlinx.android.synthetic.main.item_comment.view.*

class CommentActivity : AppCompatActivity() {
    var contentUid: String?=null
    var destinationUid: String?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comment)
        contentUid=intent.getStringExtra("contentUid")
        destinationUid=intent.getStringExtra("destinationUid")

        comment_recyclerview.adapter=CommentRecyclerViewAdapter()
        comment_recyclerview.layoutManager=LinearLayoutManager(this)

        comment_btn_send?.setOnClickListener {
            var comment=ContentDTO.Comment()
            comment.userId=FirebaseAuth.getInstance().currentUser?.email
            comment.uid=FirebaseAuth.getInstance().currentUser?.uid
            comment.comment=comment_edit_message.text.toString()
            comment.timestamp=System.currentTimeMillis()

            FirebaseFirestore.getInstance()
                .collection("images")
                .document(contentUid!!)
                .collection("comments").document().set(comment)

            commentAlarm(destinationUid!!,comment_edit_message.text.toString())

            comment_edit_message.setText("") //메시지 보낸 후 edit_message 초기화
        }
    }

    fun commentAlarm(destinationUid: String, message: String){

        val docRef = contentUid?.let {
            // contentUid가 null이 아닌 경우에 코드 블록 안의 내용을 실행

            //contentUid를 이용하여 Firebase Firestore의 "images" 컬렉션에서 특정 contentUid를 가진 문서를 가져오는 작업을 수행
            FirebaseFirestore.getInstance()
                .collection("images")
                .document(it)
                .get()
                .addOnCompleteListener { task -> //문서 가져오기 작업이 완료되면 이 리스너가 호출, task는 작업 결과를 나타내는 객체
                    if (task.isSuccessful) { //task가 성공적으로 완료되었다면 해당 블록 내부의 코드를 실행
                        val content = task.result?.toObject(ContentDTO::class.java) //문서에서 가져온 데이터를 ContentDTO 클래스로 변환하여 content 변수에 저장, 이 데이터에는 게시물 작성자의 식별자인 uid가 포함되어 있음.
                        val destinationUid = content?.uid //content 객체에서 작성자의 식별자인 uid를 가져와 destinationUid 변수에 저장

                        //만약 작성자의 uid가 현재 로그인한 사용자의 uid와 다르다면, 즉 댓글을 단 사용자가 게시물 작성자가 아니라면, 아래 내용을 실행
                        if (destinationUid != FirebaseAuth.getInstance().currentUser?.uid) {
                            //댓글을 단 사용자가 게시물 작성자가 아닌 경우, 알람 데이터 alarmDTO를 생성하고 Firebase Firestore의 "alarms" 컬렉션에 추가
                            val alarmDTO = AlarmDTO()
                            alarmDTO.destinationUid = destinationUid
                            alarmDTO.userId = FirebaseAuth.getInstance().currentUser?.email
                            alarmDTO.uid = FirebaseAuth.getInstance().currentUser?.uid
                            alarmDTO.kind = 1
                            alarmDTO.timestamp = System.currentTimeMillis()
                            alarmDTO.message = message
                            FirebaseFirestore.getInstance().collection("alarms").document().set(alarmDTO)

                            // 또한 FCM 메시지를 생성하여 게시물 작성자에게 보내는 FcmPush 객체를 이용하여 전송
                            val msg = FirebaseAuth.getInstance().currentUser?.email + " " + getString(R.string.alarm_comment) + " : " + message
                            FcmPush.instance.sendMessage(destinationUid!!, "PlaNet", msg)
                        }
                    }
                }
        }
    }

    inner class CommentRecyclerViewAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>(){
        var comments: ArrayList<ContentDTO.Comment> = arrayListOf() //댓글을 담을 arraylist

        init {
            //Firebase 데이터를 시간순으로 읽어옴
            FirebaseFirestore.getInstance()
                .collection("images")
                .document(contentUid!!)
                .collection("comments")
                .orderBy("timestamp")
                .addSnapshotListener{ querySnapshot, firebaseFirestoreException ->
                comments.clear() //값이 중복으로 쌓일 수 있기 때문에 comments.clear()해줌

                //코드 안정성을 위해 querySnapshot이 null일 때 리턴해줌
                if (querySnapshot==null)
                    return@addSnapshotListener

                //for문으로 스냅샷 하나씩 읽어옴
                for (snapshot in querySnapshot.documents!!){
                    comments.add(snapshot.toObject(ContentDTO.Comment::class.java)!!) //comments안에 이 스냅샷을 ContentDTO.Comment로 캐스팅해서 넣어줌
                }

                notifyDataSetChanged() //RecyclerView를 새로고침

            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            var view=LayoutInflater.from(parent.context).inflate(R.layout.item_comment, parent, false)
            return CustomViewHolder(view)
        }
        private inner class CustomViewHolder(view: View): RecyclerView.ViewHolder(view)

        override fun getItemCount(): Int{
            return comments.size
        }

        //서버에서 넘어온 아이디나 메시지, 프로필 이미지를 매핑해 줌
        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            var view= holder.itemView
            view.commentviewitem_textview_comment.text=comments[position].comment
            view.commentviewitem_textview_profile.text=comments[position].userId

            // 댓글 작성자의 uid와 현재 사용자의 uid를 비교하여 같은지 확인
            val isCommentAuthor = comments[position].uid == FirebaseAuth.getInstance().currentUser?.uid

            // contentUid 변수에서 게시물 작성자의 uid를 가져옴
            val postAuthorUid = destinationUid

            if (isCommentAuthor || postAuthorUid == FirebaseAuth.getInstance().currentUser?.uid) {
                // 사용자가 댓글 작성자 또는 게시물 작성자인 경우, 옵션 버튼을 보이도록 설정
                view.commentviewitem_option_btn.visibility = View.VISIBLE

                // 옵션 버튼 클릭 시 팝업 메뉴가 나타나도록 설정
                view.commentviewitem_option_btn.setOnClickListener {
                    val popupMenu = PopupMenu(it.context, it)
                    if (isCommentAuthor) {
                        popupMenu.inflate(R.menu.popup_menu_item) // 댓글 작성자용 메뉴
                    } else {
                        popupMenu.inflate(R.menu.popup_menu_item2) // 글 작성자용 메뉴
                    }
                    popupMenu.setOnMenuItemClickListener { menuItem ->
                        when (menuItem.itemId) {
                            R.id.menu_edit_option -> {
                                // "수정" 메뉴 클릭 시 수행할 동작 구현
                                // 예: 댓글 수정 화면으로 이동
                                val commentId = comments[position].commentId // 수정할 댓글의 고유 ID
                                val comment = comments[position].comment // 기존 댓글 내용
                                val userId=comments[position].userId // 유저 아이디
                                val uid = comments[position].uid.toString() // 사용자의 uid 가져오기

                                val profileImageRef = FirebaseFirestore.getInstance()
                                    .collection("profileImages")
                                    .document(uid)

                                profileImageRef.get().addOnSuccessListener { documentSnapshot ->
                                    if (documentSnapshot.exists()) {
                                        val imageUrl = documentSnapshot.getString("image")
                                        // imageUrl을 사용하여 작업 수행
                                        if (comment != null) {
                                            navigateToEditComment(commentId, comment, imageUrl, userId)
                                        }
                                    } else {
                                        // 문서가 없을 경우의 처리
                                        Toast.makeText(baseContext,"문서가 존재하지 않습니다.",Toast.LENGTH_LONG)
                                    }
                                }.addOnFailureListener { exception ->
                                    // 데이터 가져오기 실패 시의 처리
                                    Toast.makeText(baseContext,"데이터를 가져오는 데 실패했습니다.",Toast.LENGTH_LONG)
                                }

                                true
                            }
                            R.id.menu_delete_option -> {
                                // "삭제" 메뉴 클릭 시 수행할 동작 구현
                                // 예: 댓글 삭제 요청
                                val commentId = comments[position].commentId // 댓글의 고유 ID
                                showDeleteConfirmationDialog(commentId) // 확인 대화상자 표시
                                true
                            }
                            else -> false
                        }
                    }
                    popupMenu.show()
                }

            } else {
                // 그 외의 경우에는 옵션 버튼을 숨김 처리
                view.commentviewitem_option_btn.visibility = View.GONE
            }

            FirebaseFirestore.getInstance()
                .collection("profileImages")
                .document(comments[position].uid!!)
                .get()
                .addOnCompleteListener { task ->
                if(task.isSuccessful){
                    var url=task.result!!["image"]
                    Glide.with(holder.itemView.context).load(url).apply(RequestOptions().circleCrop()).into(view.commentviewitem_imageview_profile)
                }
            }
        }

        // 댓글 수정 화면으로 이동하는 메소드
        private fun navigateToEditComment(commentId: String, comment: String, imageUrl: String?, userId: String?) {
            val intent = Intent(this@CommentActivity, EditCommentActivity::class.java)
            intent.putExtra("contentUid",contentUid)
            intent.putExtra("commentId", commentId) // 수정할 댓글의 고유 ID 전달
            intent.putExtra("comment", comment) // 기존 댓글 내용 전달
            intent.putExtra("profileImage", imageUrl) // 프로필 이미지 URL 전달
            intent.putExtra("userId", userId) // 사용자 ID 전달
            startActivity(intent)
        }

        // "삭제" 메뉴를 선택했을 때 띄우는 확인 대화상자 표시 메소드
        private fun showDeleteConfirmationDialog(commentId: String) {
            val confirmDialog = ConfirmDialogFragment {
                // 확인 버튼이 클릭되었을 때 수행할 동작
                deleteComment(commentId) // 댓글 삭제 요청
            }
            confirmDialog.show(supportFragmentManager, "confirm_dialog")
        }

        // 댓글 삭제 요청 및 Firestore에서 삭제하는 메소드
        private fun deleteComment(commentId: String) {
            // "images" 컬렉션에서 "contentId" 필드 값이 현재 contentUid와 같은 문서 찾기
            FirebaseFirestore.getInstance()
                .collection("images")
                .document(contentUid!!)
                .collection("comments")
                .whereEqualTo("commentId",commentId)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    for (commentDocument in querySnapshot) {
                        commentDocument.reference.delete()
                            .addOnSuccessListener {
                                Toast.makeText(baseContext, "댓글이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener {
                                Toast.makeText(baseContext, "댓글 삭제에 실패했습니다.", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
        }

    }
}
