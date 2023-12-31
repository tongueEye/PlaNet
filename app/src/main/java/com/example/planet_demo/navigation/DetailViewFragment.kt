package com.example.planet_demo.navigation

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.planet_demo.R
import com.example.planet_demo.navigation.model.AlarmDTO
import com.example.planet_demo.navigation.model.ContentDTO
import com.example.planet_demo.navigation.model.InfoDTO
import com.example.planet_demo.navigation.util.FcmPush
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.android.synthetic.main.fragment_detail.view.*
import kotlinx.android.synthetic.main.item_comment.view.*
import kotlinx.android.synthetic.main.item_detail.view.*

class DetailViewFragment : Fragment(){
    var firestore: FirebaseFirestore?=null
    var uid: String?=null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        var view=LayoutInflater.from(activity).inflate(R.layout.fragment_detail,container,false)
        firestore=FirebaseFirestore.getInstance()
        uid=FirebaseAuth.getInstance().currentUser?.uid

        view.detailviewfragment_recyclerview.adapter=DetailViewRecyclerViewAdapter()
        view.detailviewfragment_recyclerview.layoutManager=LinearLayoutManager(activity)
        return view
    }

    inner class DetailViewRecyclerViewAdapter:RecyclerView.Adapter<RecyclerView.ViewHolder>(){
        var contentDTOs: ArrayList<ContentDTO> = arrayListOf()
        var contentUidList: ArrayList<String> = arrayListOf()

        init {
            firestore?.collection("images")?.orderBy("timestamp",Query.Direction.DESCENDING)?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                contentDTOs.clear()
                contentUidList.clear()

                //Sometimes,This code return null of querySnapshot when it signout
                if(querySnapshot==null) return@addSnapshotListener

                for(snapshot in querySnapshot!!.documents){
                    var item=snapshot.toObject(ContentDTO::class.java)
                    contentDTOs.add(item!!)
                    contentUidList.add(snapshot.id)
                }
                notifyDataSetChanged()
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            var view = LayoutInflater.from(parent.context).inflate(R.layout.item_detail,parent,false)
            return CustomViewHolder(view)
        }

        inner class CustomViewHolder(view: View) : RecyclerView.ViewHolder(view)

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            var viewholder=(holder as CustomViewHolder).itemView

            //UserId
            // Get nickname from "infos" collection
            FirebaseFirestore.getInstance().collection("infos").document(contentDTOs[position].uid!!).get().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val infoDTO = task.result?.toObject(InfoDTO::class.java)
                    val nickname = infoDTO?.nickname

                    // Display the nickname in the profile text view
                    viewholder.detailviewitem_profile_textview.text = nickname ?: contentDTOs!![position].userId
                }
            }

            //Image
            Glide.with(holder.itemView.context).load(contentDTOs!![position].imageUrl).into(viewholder.detailviewitem_imageview_content)

            //Explain of content
            viewholder.detailviewitem_explain_textview.text=contentDTOs!![position].explain

            //likes
            viewholder.detailviewitem_favoritecounter_textview.text="Likes"+contentDTOs!![position].favoriteCount

            //menu icon
            viewholder.detailviewitem_menu_image.visibility=View.GONE

            //ProfileImage
            FirebaseFirestore.getInstance().collection("profileImages").document(contentDTOs[position].uid!!).get().addOnCompleteListener {
                    task ->
                if(task.isSuccessful){
                    var url: Any? = task.result!!["image"]
                    Glide.with(holder.itemView.context).load(url).apply(RequestOptions().circleCrop()).into(viewholder.detailviewitem_profile_image)
                }
            }

            //This code is when the button is clicked
            viewholder.detailviewitem_favorite_imageview.setOnClickListener {
                favoriteEvent(position)
            }
            //this code is when the page is loaded
            if (contentDTOs!![position].favorites.containsKey(uid)){
                //This is like status
                viewholder.detailviewitem_favorite_imageview.setImageResource(R.drawable.ic_favorite_star)

            }else{
                //This is unlike status
                viewholder.detailviewitem_favorite_imageview.setImageResource(R.drawable.ic_favorite_border_star)
            }

            //This code is when the profile image
            viewholder.detailviewitem_profile_image.setOnClickListener {
                var fragment=UserFragment()
                var bundle=Bundle()
                bundle.putString("destinationUid",contentDTOs[position].uid)
                bundle.putString("userId",contentDTOs[position].userId)
                fragment.arguments=bundle
                activity?.supportFragmentManager?.beginTransaction()?.replace(R.id.main_content,fragment)?.commit()
            }

            viewholder.detailviewitem_comment_imageview.setOnClickListener { v ->
                var intent = Intent(v.context, CommentActivity::class.java)
                intent.putExtra("contentUid",contentUidList[position])
                intent.putExtra("destinationUid",contentDTOs[position].uid)
                startActivity(intent)
            }
        }

        fun favoriteEvent(position: Int){
            var tsDoc=firestore?.collection("images")?.document(contentUidList[position])
            firestore?.runTransaction { transaction->

                var uid=FirebaseAuth.getInstance().currentUser?.uid
                var contentDTO=transaction.get(tsDoc!!).toObject(ContentDTO::class.java)

                if (contentDTO!!.favorites.containsKey(uid)){ //좋아요가 이미 눌려있을 경우 - 좋아요 취소
                    contentDTO?.favoriteCount = contentDTO?.favoriteCount?.minus(1)!!
                    contentDTO?.favorites?.remove(uid)
                }else{ //좋아요가 눌려있지 않은 경우 - 좋아요 클릭
                    contentDTO?.favoriteCount= contentDTO?.favoriteCount?.plus(1)!!
                    contentDTO?.favorites?.set(uid!!, true)

                    // 좋아요 클릭시에만 알람을 발생시키도록 처리
                    if (contentDTO.uid != uid) { // 내 게시물에 대한 좋아요가 아닐 경우에만 알람 발생
                        favoriteAlarm(contentDTO.uid!!)
                    }
                }
                transaction.set(tsDoc,contentDTO)
            }
       }

        fun favoriteAlarm(destinationUid: String){
            var nicknameRef=
                FirebaseAuth.getInstance().currentUser?.uid?.let {
                    FirebaseFirestore.getInstance().collection("infos").document(
                        it
                    )
                }
            nicknameRef?.get()?.addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()){
                    val nickname=documentSnapshot.getString("nickname")

                    var alarmDTO=AlarmDTO()
                    alarmDTO.destinationUid=destinationUid
                    alarmDTO.userId=nickname
                    //alarmDTO.userId=FirebaseAuth.getInstance().currentUser?.email
                    alarmDTO.uid=FirebaseAuth.getInstance().currentUser?.uid
                    alarmDTO.kind=0
                    alarmDTO.timestamp=System.currentTimeMillis()
                    FirebaseFirestore.getInstance().collection("alarms").document().set(alarmDTO)

                    //좋아요 클릭시 FCM 메시지 생성
                    var message= nickname + " " +getString(R.string.alarm_favorite)
                    FcmPush.instance.sendMessage(destinationUid,"PlaNet",message)
                }
            }
        }

        override fun getItemCount(): Int {
            return contentDTOs.size
        }

    }
}