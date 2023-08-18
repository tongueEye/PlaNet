package com.example.planet_demo.navigation

import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.planet_demo.LoginActivity
import com.example.planet_demo.MainActivity
import com.example.planet_demo.R
import com.example.planet_demo.navigation.DetailViewFragment.DetailViewRecyclerViewAdapter.CustomViewHolder
import com.example.planet_demo.navigation.model.AlarmDTO
import com.example.planet_demo.navigation.model.ContentDTO
import com.example.planet_demo.navigation.model.FollowDTO
import com.example.planet_demo.navigation.util.FcmPush
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_user.view.*

class UserFragment : Fragment(){
    var fragmentView: View?=null
    var firestore: FirebaseFirestore?=null
    var uid: String?=null
    var auth: FirebaseAuth?=null
    var currentUserUid:String?=null
    var followListenerRegistration:ListenerRegistration?=null

    companion object{
        var PICK_PROFILE_FROM_ALBUM=10
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        fragmentView=LayoutInflater.from(activity).inflate(R.layout.fragment_user,container,false)
        uid=arguments?.getString("destinationUid")
        firestore=FirebaseFirestore.getInstance()
        auth=FirebaseAuth.getInstance()

        currentUserUid=auth?.currentUser?.uid
        if(uid==currentUserUid){
            //MyPage - 로그아웃 버튼이 보이게, 클릭시 로그아웃하고 로그인 화면으로 이동
            fragmentView?.account_btn_follow_signout?.text=getString(R.string.signout)
            fragmentView?.account_btn_follow_signout?.setOnClickListener {
                activity?.finish()
                startActivity(Intent(activity,LoginActivity::class.java))
                auth?.signOut()
            }

        }else{
            //OtherUserPage - 팔로우 버튼이 보이게, 다른 유저의 아이디 표시, 뒤로가기 클릭시 홈화면으로 이동
            fragmentView?.account_btn_follow_signout?.text=getString(R.string.follow)
            var mainactivity=(activity as MainActivity)
            mainactivity?.toolbar_username?.text=arguments?.getString("userId")
            mainactivity?.toolbar_btn_back?.setOnClickListener{
                mainactivity.bottom_navigation.selectedItemId=R.id.action_home
            }
            mainactivity?.toolbar_title_image?.visibility=View.GONE
            mainactivity?.toolbar_username?.visibility=View.VISIBLE
            mainactivity?.toolbar_btn_back?.visibility=View.VISIBLE

            fragmentView?.account_btn_follow_signout?.setOnClickListener {
                requestFollow()
            }
        }

        fragmentView?.account_recyclerview?.adapter=UserFragmentRecyclerViewAdapter()
        fragmentView?.account_recyclerview?.layoutManager=GridLayoutManager(activity,3)

        //프로필 업로드
        fragmentView?.account_iv_profile?.setOnClickListener {
            var photoPickerIntent=Intent(Intent.ACTION_PICK)
            photoPickerIntent.type="image/*"
            activity?.startActivityForResult(photoPickerIntent,PICK_PROFILE_FROM_ALBUM)
        }
        getProfileImage()
        getFollowerAndFollowing()
        return fragmentView
    }

    inner class UserFragmentRecyclerViewAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>(){
        var contentDTOs: ArrayList<ContentDTO> = arrayListOf()
        init {
            firestore?.collection("images")?.whereEqualTo("uid",uid)?.orderBy("timestamp",Query.Direction.DESCENDING)?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                //Sometimes, This code return null of querySnapshot when it signout
                if(querySnapshot==null) return@addSnapshotListener

                // Clear existing data before adding new data
                contentDTOs.clear()

                //Get data
                for (snapshot in querySnapshot.documents){
                    contentDTOs.add(snapshot.toObject(ContentDTO::class.java)!!)
                }
                fragmentView?.account_tv_post_count?.text=contentDTOs.size.toString()
                notifyDataSetChanged()
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            var width=resources.displayMetrics.widthPixels/3
            var imageview=ImageView(parent.context)
            imageview.layoutParams=LinearLayoutCompat.LayoutParams(width,width)
            return CustomViewHolder(imageview)
        }
        inner class CustomViewHolder(var imageview: ImageView):RecyclerView.ViewHolder(imageview){

        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            var imageview=(holder as CustomViewHolder).imageview
            Glide.with(holder.itemView.context).load(contentDTOs[position].imageUrl).apply(RequestOptions().centerCrop()).into(imageview)

            holder.itemView.setOnClickListener {
                val clickedItem=contentDTOs[position]
                val content_id=clickedItem.contentId
                showDetail(content_id)
            }
        }

        // 선택한 아이템의 uid에 해당하는 데이터를 가져와서 아이템 상세화면을 표시하는 함수
        private fun showDetail(cid: String) {
            val detailFragment = ItemDetailFragment()
            val bundle = Bundle()
            bundle.putString("cid", cid)
            detailFragment.arguments = bundle

            val transaction = activity?.supportFragmentManager?.beginTransaction()
            transaction?.replace(R.id.main_content, detailFragment)
            transaction?.addToBackStack(null)
            transaction?.commit()
        }

        override fun getItemCount(): Int {
            return contentDTOs.size
        }

    }

    //팔로워 수와 팔로잉 데이터베이스에서 실시간으로 가져와서 ui 화면에 출력
    fun getFollowerAndFollowing(){
        followListenerRegistration=firestore?.collection("users")?.document(uid!!)?.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
            if(documentSnapshot==null) return@addSnapshotListener
            var followDTO=documentSnapshot.toObject(FollowDTO::class.java)
            if (followDTO?.followingCount!=null){
                fragmentView?.account_tv_following_count?.text=followDTO?.followingCount?.toString()
            }
            if (followDTO?.followerCount!=null){
                fragmentView?.account_tv_follower_count?.text=followDTO?.followerCount?.toString()
                if(followDTO?.followers?.containsKey(currentUserUid!!) == true){
                    //팔로워 목록에 내가 있을 경우, 팔로우 버튼 누르면 팔로우 취소
                    //fragmentView?.account_btn_follow_signout?.text="FOLLOW CANCLE"
                    fragmentView?.account_btn_follow_signout?.text=getString(R.string.follow_cancel)
                    fragmentView?.account_btn_follow_signout?.background?.setColorFilter(ContextCompat.getColor(requireActivity(),R.color.colorLightGray), PorterDuff.Mode.MULTIPLY)
                }else{
                    if(uid!=currentUserUid){
                        //팔로워 목록에 없을 경우, 팔로우 버튼 누르면 팔로우
                        //fragmentView?.account_btn_follow_signout?.text="FOLLOW"
                        fragmentView?.account_btn_follow_signout?.text=getString(R.string.follow)
                        //상대방 user fragment 일 때, background color 변경
                        fragmentView?.account_btn_follow_signout?.background?.colorFilter=null
                    }

                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        followListenerRegistration?.remove()
    }

    fun requestFollow(){
        //Save data to my account
        var tsDocFollowing=firestore?.collection("users")?.document(currentUserUid!!)
        firestore?.runTransaction { transaction->
            var followDTO=transaction.get(tsDocFollowing!!).toObject(FollowDTO::class.java)
            if(followDTO==null){
                followDTO= FollowDTO()
                followDTO!!.followingCount=1
                followDTO!!.followers[uid!!]=true

                transaction.set(tsDocFollowing,followDTO)
                return@runTransaction
            }

            if (followDTO?.followings?.containsKey(uid)!!){
                //It remove following third person when a third person follow me
                followDTO?.followingCount= followDTO?.followingCount?.minus(1)!!
                followDTO?.followings?.remove(uid)
            }else{
                //It add following third person when a third person do not follow me
                followDTO?.followingCount= followDTO?.followingCount?.plus(1)!!
                followDTO?.followings!![uid!!]=true
            }
            transaction.set(tsDocFollowing,followDTO)
            return@runTransaction
        }
        //Save data to third Person
        var tsDocFollower=firestore!!.collection("users")?.document(uid!!)
        firestore?.runTransaction { transaction->
            var followDTO=transaction.get(tsDocFollower!!).toObject(FollowDTO::class.java)
            if(followDTO==null){
                followDTO= FollowDTO()
                followDTO!!.followerCount=1
                followDTO!!.followers[currentUserUid!!]=true
                followerAlarm(uid!!)

                transaction.set(tsDocFollower,followDTO!!)
                return@runTransaction
            }

            if(followDTO!!.followers.containsKey(currentUserUid)){
                //It cancel my follower when i follow a third person (팔로우 했을 경우 팔로우 취소)
                followDTO!!.followerCount=followDTO!!.followerCount.minus(1)
                followDTO!!.followers.remove(currentUserUid!!)
            }else{
                //It add my follower when i don't follow a third person (팔로우 안했을 경우 팔로우 추가)
                followDTO!!.followerCount=followDTO!!.followerCount.plus(1)
                followDTO!!.followers[currentUserUid!!]=true
                followerAlarm(uid!!)
            }
            transaction.set(tsDocFollower,followDTO!!)
            return@runTransaction
        }
    }

    fun followerAlarm(destinationUid: String){
        var alarmDTO=AlarmDTO()
        alarmDTO.destinationUid=destinationUid
        alarmDTO.userId=auth?.currentUser?.email
        alarmDTO.uid=auth?.currentUser?.uid
        alarmDTO.kind=2
        alarmDTO.timestamp=System.currentTimeMillis()
        FirebaseFirestore.getInstance().collection("alarms").document().set(alarmDTO)

        //팔로우 클릭시 FCM 메시지 생성
        var message=auth?.currentUser?.email + getString(R.string.alarm_follow)
        FcmPush.instance.sendMessage(destinationUid,"PlaNet",message)
    }

    //firestore database에서 프로필 이미지 받아놈
    fun getProfileImage(){
        firestore?.collection("profileImages")?.document(uid!!)?.addSnapshotListener{documentSnapshot, firebaseFirestoreException ->
            if(documentSnapshot==null) return@addSnapshotListener
            if(documentSnapshot.data!=null){
                var url=documentSnapshot?.data!!["image"]
                Glide.with(requireActivity()).load(url).apply(RequestOptions().circleCrop()).into(fragmentView?.account_iv_profile!!)
            }
        }
    }


}