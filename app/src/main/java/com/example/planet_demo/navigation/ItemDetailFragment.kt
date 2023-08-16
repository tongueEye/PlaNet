package com.example.planet_demo.navigation

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.planet_demo.R
import com.example.planet_demo.navigation.model.ContentDTO
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.fragment_detail.view.*
import kotlinx.android.synthetic.main.item_detail.view.*

class ItemDetailFragment : Fragment() {
    private var firestore: FirebaseFirestore? = null
    var content_id: String?=null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        var view=LayoutInflater.from(activity).inflate(R.layout.item_detail,container,false)
        firestore = FirebaseFirestore.getInstance()
        content_id = arguments?.getString("cid")

        if (content_id != null) {
            firestore?.collection("images")?.whereEqualTo("contentId", content_id)?.get()?.addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val documentSnapshot = querySnapshot.documents[0] // 여기서는 첫 번째 문서를 가져옴
                    val contentDTO = documentSnapshot.toObject(ContentDTO::class.java)
                    contentDTO?.let { showDetailInfo(view, it) }
                }
            }
        }
        return view
    }


    private fun showDetailInfo(view: View, contentDTO: ContentDTO) {

        view.detailviewitem_profile_textview.text = contentDTO.userId
        view.detailviewitem_favoritecounter_textview.text="Likes "+contentDTO.favoriteCount.toString()
        view.detailviewitem_explain_textview.text = contentDTO.explain

        firestore?.collection("profileImages")?.document(contentDTO.uid.toString())?.get()?.addOnSuccessListener { documentSnapshot ->
            if (documentSnapshot != null && documentSnapshot.exists()) {
                val profileImageURL = documentSnapshot.getString("image")

                if (profileImageURL != null) {
                    // 프로필 이미지 로딩 작업 수행
                    Glide.with(requireContext())
                        .load(profileImageURL)
                        .apply(RequestOptions().circleCrop())
                        .into(view.detailviewitem_profile_image)
                }
            }
        }
        // 이미지 로딩
        Glide.with(requireContext())
            .load(contentDTO.imageUrl)
            .apply(RequestOptions().centerCrop())
            .into(view.detailviewitem_imageview_content)


        //This code is when the profile image
        view.detailviewitem_profile_image.setOnClickListener {
            var fragment=UserFragment()
            var bundle=Bundle()
            bundle.putString("destinationUid",contentDTO.uid)
            bundle.putString("userId",contentDTO.userId)
            fragment.arguments=bundle
            activity?.supportFragmentManager?.beginTransaction()?.replace(R.id.main_content,fragment)?.commit()
        }

        view.detailviewitem_comment_imageview.setOnClickListener { v ->
            val contentId = contentDTO.contentId // contentDTO의 contentId 값

            //firestore database에서 "images" collection에 문서의 contentId 필드 항목의 값이 contentDTO.contentId 값과 같은 document의 이름 값을 가져옴
            firestore?.collection("images")
                ?.whereEqualTo("contentId", contentId)
                ?.get()
                ?.addOnSuccessListener { querySnapshot ->
                    if (!querySnapshot.isEmpty) {
                        val documentSnapshot = querySnapshot.documents[0] // 첫 번째 문서를 가져옴
                        val documentId = documentSnapshot.id // 문서의 이름 값을 가져옴

                        val intent = Intent(v.context, CommentActivity::class.java)
                        intent.putExtra("contentUid", documentId) // 문서의 이름 값을 넘김
                        intent.putExtra("destinationUid", contentDTO.uid)
                        startActivity(intent)
                    }
                }
                ?.addOnFailureListener { exception ->
                    // 실패 시 처리하는 코드
                    Toast.makeText(context, "댓글 데이터를 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
        }

    }


    // CustomViewHolder 등과 같은 클래스가 필요할 수 있습니다.
    // 아래와 같이 작성하거나 적절한 방법으로 ViewHolder 클래스를 생성하세요.
    inner class CustomViewHolder(var imageview: ImageView) : RecyclerView.ViewHolder(imageview)

    companion object {
        fun newInstance(): DetailViewFragment {
            return DetailViewFragment()
        }
    }
}