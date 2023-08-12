package com.example.planet_demo.navigation

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

            FirebaseFirestore.getInstance().collection("images").document(contentUid!!).collection("comments").document().set(comment)

            commentAlarm(destinationUid!!,comment_edit_message.text.toString())

            comment_edit_message.setText("") //메시지 보낸 후 edit_message 초기화
        }
    }

    fun commentAlarm(destinationUid: String, message: String){
        var alarmDTO=AlarmDTO()
        alarmDTO.destinationUid=destinationUid
        alarmDTO.userId=FirebaseAuth.getInstance().currentUser?.email
        alarmDTO.uid=FirebaseAuth.getInstance().currentUser?.uid
        alarmDTO.kind=1
        alarmDTO.timestamp=System.currentTimeMillis()
        alarmDTO.message=message
        FirebaseFirestore.getInstance().collection("alarms").document().set(alarmDTO)

        //댓글 작성 시 FCM 메시지 생성
        var msg=FirebaseAuth.getInstance().currentUser?.email + " " + getString(R.string.alarm_comment) + " of " + message
        FcmPush.instance.sendMessage(destinationUid,"PlaNet",msg)
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
    }
}
