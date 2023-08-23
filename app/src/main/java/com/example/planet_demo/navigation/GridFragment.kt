package com.example.planet_demo.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
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
    var firestore: FirebaseFirestore?=null //Firebase Firestore의 인스턴스를 저장할 변수
    var fragmentView:View?=null //프래그먼트의 뷰를 저장하는 변수
    private lateinit var adapter: UserFragmentRecyclerViewAdapter // RecyclerView 어댑터를 저장할 변수


    override fun onCreateView( //프래그먼트의 뷰를 생성하고 초기화하는 역할
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // 프래그먼트의 뷰 생성을 위한 onCreateView 메서드
        fragmentView = inflater.inflate(R.layout.fragment_grid, container, false) //fragment_grid.xml 레이아웃을 inflate하여 할당
        firestore=FirebaseFirestore.getInstance() // Firebase Firestore의 인스턴스를 가져와 할당

        // 어댑터 초기화 및 리사이클러뷰에 어댑터 연결 (userfragment 리사이클러뷰 재활용)
        adapter = UserFragmentRecyclerViewAdapter()
        fragmentView?.gridfragment_recyclerview?.adapter = adapter
        fragmentView?.gridfragment_recyclerview?.layoutManager=GridLayoutManager(activity,3) //GridLayoutManager를 사용하여 RecyclerView의 레이아웃 매니저를 그리드 형식으로 설정

        //검색 버튼을 찾아와서 클릭 리스너를 설정
        val searchButton = fragmentView?.findViewById<ImageView>(R.id.search_button)
        searchButton?.setOnClickListener {
            //검색어를 가져와서 filterContentsBySearch 함수를 호출하여 검색 결과를 처리
            val searchText = fragmentView?.search_edittext?.text.toString()
            filterContentsBySearch(searchText)
            Toast.makeText(context,"검색이 완료되었습니다.",Toast.LENGTH_LONG).show()
        }

        return fragmentView //onCreateView 메서드에서 설정한 뷰를 반환
    }

    private fun filterContentsBySearch(searchText: String) { //// Firestore에서 이미지 컬렉션을 검색어에 따라 필터링하고 결과를 처리하는 함수
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

    //user fragment의 recycler view 코드 재사용 (일부 변경) - UserFragmentRecyclerViewAdapter 클래스를 선언하고, RecyclerView 어댑터로 사용됨
    inner class UserFragmentRecyclerViewAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>(){
        var contentDTOs: ArrayList<ContentDTO> = arrayListOf() // ContentDTO 객체들을 저장하는 ArrayList
        init { //어댑터 초기화 블록으로, Firestore에서 이미지 컬렉션을 가져와서 어댑터에 데이터를 설정
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
            // 뷰홀더를 생성하여 반환하는 함수
            var width=resources.displayMetrics.widthPixels/3
            var imageview= ImageView(parent.context)
            imageview.layoutParams= LinearLayoutCompat.LayoutParams(width,width)
            return CustomViewHolder(imageview)
        }
        inner class CustomViewHolder(var imageview: ImageView): RecyclerView.ViewHolder(imageview){

        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            // 뷰홀더와 데이터를 바인딩하는 함수
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

        override fun getItemCount(): Int { //뷰홀더와 데이터를 바인딩하는 함수
            return contentDTOs.size
        }

    }
}