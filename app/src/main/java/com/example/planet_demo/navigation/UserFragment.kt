package com.example.planet_demo.navigation

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.planet_demo.LoginActivity
import com.example.planet_demo.MainActivity
import com.example.planet_demo.R
import com.example.planet_demo.navigation.DetailViewFragment.DetailViewRecyclerViewAdapter.CustomViewHolder
import com.example.planet_demo.navigation.model.ContentDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_user.view.*

class UserFragment : Fragment(){
    var fragmentView: View?=null
    var firestore: FirebaseFirestore?=null
    var uid: String?=null
    var auth: FirebaseAuth?=null
    var currentUserUid:String?=null

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
        }

        fragmentView?.account_recyclerview?.adapter=UserFragmentRecyclerViewAdapter()
        fragmentView?.account_recyclerview?.layoutManager=GridLayoutManager(activity,3)
        return fragmentView
    }

    inner class UserFragmentRecyclerViewAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>(){
        var contentDTOs: ArrayList<ContentDTO> = arrayListOf()
        init {
            firestore?.collection("images")?.whereEqualTo("uid",uid)?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                //Sometimes, This code return null of querySnapshot when it signout
                if(querySnapshot==null) return@addSnapshotListener

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
        }

        override fun getItemCount(): Int {
            return contentDTOs.size
        }

    }
}