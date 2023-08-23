package com.example.planet_demo.navigation

import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.planet_demo.R
import com.example.planet_demo.navigation.model.AlarmDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.android.synthetic.main.fragment_alarm.view.*
import kotlinx.android.synthetic.main.item_alarm.view.*

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
            val uid=FirebaseAuth.getInstance().currentUser?.uid //현재 로그인한 사용자의 UID를 가져옴

            FirebaseFirestore.getInstance().collection("alarms") //Firestore의 "alarms" 컬렉션을 참조
                .whereEqualTo("destinationUid",uid) //"alarms" 컬렉션에서 "destinationUid" 필드 값이 현재 로그인한 사용자의 UID와 일치하는 문서들을 찾음
                .orderBy("timestamp",Query.Direction.DESCENDING) // 찾은 문서들을 "timestamp" 필드 값을 기준으로 내림차순으로 정렬 - 최근 알림 우선 표시
                .addSnapshotListener { querySnapshot, firebaseFirestoreException -> //위에서 설정한 조건으로 실시간 업데이트를 수신하면서 알림 데이터를 가져옵니다.

                if(querySnapshot==null) return@addSnapshotListener //만약 querySnapshot이 null인 경우, 데이터가 없거나 로드에 실패한 것으로 간주하고 함수 실행을 중단
                alarmDTOList.clear() //알림 데이터를 다시 로드하기 전에 기존의 알림 데이터 리스트를 비움

                for (snapshot in querySnapshot.documents){
                    //querySnapshot에서 가져온 문서들을 순회하며 각 문서의 데이터를 AlarmDTO 객체로 변환하여 alarmDTOList 리스트에 추가
                    alarmDTOList.add(snapshot.toObject(AlarmDTO::class.java)!!)
                }
                notifyDataSetChanged() //데이터 변경을 알려주어 리사이클러뷰가 UI를 업데이트 하도록 함

            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            //알림 리스트 디자인: 댓글 리스트 디자인 재활용
            var view=LayoutInflater.from(parent.context).inflate(R.layout.item_alarm,parent,false)
            return CustomViewHolder(view)
        }
        inner class CustomViewHolder(view: View) : RecyclerView.ViewHolder(view)

        override fun getItemCount(): Int {
            return alarmDTOList.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            var view=holder.itemView
            var alarmDTO=alarmDTOList[position]  //새로추가

            //알림에 프로필사진 띄우기 (profileImages collection에 들어있는 사진 중 알림 이벤트로 전달된 uid와 같은 것 출력)
            FirebaseFirestore.getInstance().collection("profileImages").document(alarmDTOList[position].uid!!).get().addOnCompleteListener { task ->
                var url=task.result!!["image"]
                Glide.with(view.context).load(url).apply(RequestOptions().circleCrop()).into(view.alarmviewitem_imageview_profile)
            }

            //알람 종류에 따라 다르게 출력
            when(alarmDTOList[position].kind){
                0->{
                    val str_0=alarmDTOList[position].userId + " " + getString(R.string.alarm_favorite)
                    view.alarmviewitem_textview_profile.text=str_0
                }
                1->{
                    val str_0=alarmDTOList[position].userId + " " + getString(R.string.alarm_comment)
                    var str_1="\n: "+ alarmDTOList[position].message

                    //메시지 길이에 따라 출력 - 길이가 너무 길면 적당히 잘라서 이후 부분이 ... 로 보이도록 구현
                    if (str_1.length!! > 30){
                        str_1="\n: "+ alarmDTOList[position].message?.substring(0,30)+"..."
                    }else{
                        str_1="\n: "+ alarmDTOList[position].message
                    }

                    view.alarmviewitem_textview_profile.text=str_0+str_1
                }
                2->{
                    val str_0=alarmDTOList[position].userId + " " + getString(R.string.alarm_follow)
                    view.alarmviewitem_textview_profile.text=str_0
                }
            }
            view.alarmviewitem_textview_message.visibility=View.INVISIBLE //garbage text 숨기기

            view.alarmviewitem_delete_btn.setOnClickListener {
                val confirmDialog = ConfirmDialogFragment{
                    deleteAlarm(position, alarmDTO.alarmId)
                }
                confirmDialog.show(childFragmentManager,"confirm_dialog")
            }
        }

        private fun deleteAlarm(position: Int, alarm_Id: String){
            var db=FirebaseFirestore.getInstance()
            var collectionReference=db.collection("alarms")

            //collectionReference에 whereEqualTo("alarmId", alarm_Id)를 사용하여 해당 alarmId와 일치하는 Firestore 문서를 찾음
            collectionReference.whereEqualTo("alarmId",alarm_Id)
                .get()
                .addOnSuccessListener { querySnapshot -> //querySnapshot은 해당 조건을 만족하는 문서들의 스냅샷을 가지고 있음
                    for (documentSnapshot in querySnapshot.documents){ //querySnapshot.documents는 이 스냅샷에 포함된 각 문서에 대한 목록을 제공
                        documentSnapshot.reference.delete() //for 루프를 사용하여 각 문서를 순회하면서, 해당 문서를 삭제
                            .addOnSuccessListener {
                                //삭제 성공 시 처리
                                //삭제가 성공하면 해당 알림 아이템을 alarmDTOList에서 찾아서 제거하고 화면에서도 해당 아이템을 제거
                                val rmposition = alarmDTOList.indexOfFirst { it.alarmId == alarm_Id }
                                if (rmposition != -1) {
                                    alarmDTOList.removeAt(position)
                                    notifyItemRemoved(position)
                                }
                                notifyDataSetChanged() // 리사이클러뷰의 데이터 변경을 알려주어 화면이 업데이트된다.
                                Toast.makeText(context, "알림을 삭제했습니다.", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { exception ->
                                //삭제 실패 시 처리
                                //삭제 성공 또는 실패에 따라 해당 메시지를 토스트 메시지로 표시
                                Toast.makeText(context, "알림 삭제 실패!", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
                .addOnFailureListener { exception ->
                    // 조회 실패 시 처리
                    Toast.makeText(context, "조회 실패했습니다!", Toast.LENGTH_SHORT).show()
                }
        }

    }
}