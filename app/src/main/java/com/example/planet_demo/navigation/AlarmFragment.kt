package com.example.planet_demo.navigation

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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.fragment_alarm.view.*
import kotlinx.android.synthetic.main.item_comment.view.*

class AlarmFragment : Fragment(){
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        var view=LayoutInflater.from(activity).inflate(R.layout.fragment_alarm,container,false)
        view.alarmfragment_recyclerview.adapter = AlarmRecyclerviewAdapter()//adapter와 recyclerview 연결
        view.alarmfragment_recyclerview.layoutManager = LinearLayoutManager(activity)//recyclerview 배치 방향 (세로로)

        return view
    }

    inner class AlarmRecyclerviewAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>(){
        var alarmDTOList: ArrayList<AlarmDTO> = arrayListOf()

        init {
            val uid=FirebaseAuth.getInstance().currentUser?.uid //현재 로그인한 계정의 uid

            //FirebaseFirestore에 alarms collection에 도착한 알람의 destinationUid와 uid가 같으면
            FirebaseFirestore.getInstance().collection("alarms").whereEqualTo("destinationUid",uid).addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                alarmDTOList.clear()
                if(querySnapshot==null) return@addSnapshotListener

                for (snapshot in querySnapshot.documents){ //for문을 통해 querySnapshot 순회
                    alarmDTOList.add(snapshot.toObject(AlarmDTO::class.java)!!)
                }
                notifyDataSetChanged()
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            //알림 리스트 디자인: 댓글 리스트 디자인 재활용
            var view=LayoutInflater.from(parent.context).inflate(R.layout.item_comment,parent,false)
            return CustomViewHolder(view)
        }
        inner class CustomViewHolder(view: View) : RecyclerView.ViewHolder(view)

        override fun getItemCount(): Int {
            return alarmDTOList.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            var view=holder.itemView

            //알림에 프로필사진 띄우기 (profileImages collection에 들어있는 사진 중 알림 이벤트로 전달된 uid와 같은 것 출력)
            FirebaseFirestore.getInstance().collection("profileImages").document(alarmDTOList[position].uid!!).get().addOnCompleteListener { task ->
                var url=task.result!!["image"]
                Glide.with(view.context).load(url).apply(RequestOptions().circleCrop()).into(view.commentviewitem_imageview_profile)
            }

            //알람 종류에 따라 다르게 출력
            when(alarmDTOList[position].kind){
                0->{
                    val str_0=alarmDTOList[position].userId + " " + getString(R.string.alarm_favorite)
                    view.commentviewitem_textview_profile.text=str_0
                }
                1->{
                    val str_0=alarmDTOList[position].userId + " " + getString(R.string.alarm_comment) + " of "+ alarmDTOList[position].message
                    view.commentviewitem_textview_profile.text=str_0
                }
                2->{
                    val str_0=alarmDTOList[position].userId + " " + getString(R.string.alarm_follow)
                    view.commentviewitem_textview_profile.text=str_0
                }
            }
            view.commentviewitem_textview_comment.visibility=View.INVISIBLE //garbage text 숨기기
        }

    }
}