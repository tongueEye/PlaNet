package com.example.planet_demo.navigation

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
import com.example.planet_demo.R
import com.example.planet_demo.navigation.model.ContentDTO
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.android.synthetic.main.fragment_grid.view.* //user fragment의 recycler view 코드 재사용 - fragment_user를 fragment_grid로 수정

class GridFragment : Fragment(){
    var firestore: FirebaseFirestore?=null //user fragment의 recycler view 코드 재사용 - 변수 글로벌 변수로 선언
    var fragmentView:View?=null //user fragment의 recycler view 코드 재사용 - 변수 글로벌 변수로 선언
    private lateinit var adapter: UserFragmentRecyclerViewAdapter // 어댑터를 미리 선언


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        fragmentView = inflater.inflate(R.layout.fragment_grid, container, false)
        //fragmentView=LayoutInflater.from(activity).inflate(R.layout.fragment_grid,container,false) //user fragment의 recycler view 코드 재사용 - fragmentView 초기화
        firestore=FirebaseFirestore.getInstance() //user fragment의 recycler view 코드 재사용 - firebase 초기화

        // 어댑터 초기화 및 리사이클러뷰에 설정
        adapter = UserFragmentRecyclerViewAdapter()
        fragmentView?.gridfragment_recyclerview?.adapter = adapter
        //recyclerview 연결
        //fragmentView?.gridfragment_recyclerview?.adapter=UserFragmentRecyclerViewAdapter()
        fragmentView?.gridfragment_recyclerview?.layoutManager=GridLayoutManager(activity,3)

        val searchButton = fragmentView?.findViewById<ImageView>(R.id.search_button)
        searchButton?.setOnClickListener {
            val searchText = fragmentView?.search_edittext?.text.toString()
            filterContentsBySearch(searchText)
        }

        return fragmentView
    }

    private fun filterContentsBySearch(searchText: String) {
        firestore?.collection("images")
            ?.orderBy("timestamp", Query.Direction.DESCENDING)
            ?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                if (querySnapshot == null) return@addSnapshotListener

                val filteredContentDTOs = mutableListOf<ContentDTO>()

                for (snapshot in querySnapshot.documents) {
                    val contentDTO = snapshot.toObject(ContentDTO::class.java)
                    contentDTO?.let {
                        if (it.explain?.contains(searchText, ignoreCase = true) == true) {
                            filteredContentDTOs.add(it)
                        }
                    }
                }

                adapter.updateData(filteredContentDTOs)
            }
    }

    //user fragment의 recycler view 코드 재사용 (일부 변경)
    inner class UserFragmentRecyclerViewAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>(){
        var contentDTOs: ArrayList<ContentDTO> = arrayListOf()
        init {
            firestore?.collection("images")?.orderBy("timestamp",Query.Direction.DESCENDING)?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                //Sometimes, This code return null of querySnapshot when it signout
                if(querySnapshot==null) return@addSnapshotListener

                // Clear existing data before adding new data
                contentDTOs.clear()

                //Get data
                for (snapshot in querySnapshot.documents){
                    contentDTOs.add(snapshot.toObject(ContentDTO::class.java)!!)
                }
                notifyDataSetChanged()
            }
        }

        // 어댑터 데이터 업데이트 함수
        fun updateData(newData: List<ContentDTO>) {
            contentDTOs.clear()
            contentDTOs.addAll(newData)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            var width=resources.displayMetrics.widthPixels/3
            var imageview= ImageView(parent.context)
            imageview.layoutParams= LinearLayoutCompat.LayoutParams(width,width)
            return CustomViewHolder(imageview)
        }
        inner class CustomViewHolder(var imageview: ImageView): RecyclerView.ViewHolder(imageview){

        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            var imageview=(holder as CustomViewHolder).imageview
            Glide.with(holder.itemView.context).load(contentDTOs[position].imageUrl).apply(
                RequestOptions().centerCrop()).into(imageview)


            // 각 아이템을 클릭할 때의 동작 설정
            holder.itemView.setOnClickListener {
                val contentDTO = contentDTOs[position]
                val bundle = Bundle()
                bundle.putString("cid", contentDTO.contentId) // contentId를 인텐트로 전달

                val itemDetailFragment = ItemDetailFragment()
                itemDetailFragment.arguments = bundle

                activity?.supportFragmentManager
                    ?.beginTransaction()
                    ?.replace(R.id.main_content, itemDetailFragment)
                    ?.addToBackStack(null) // 뒤로가기 스택에 추가
                    ?.commit()
            }

        }

        override fun getItemCount(): Int {
            return contentDTOs.size
        }

    }
}