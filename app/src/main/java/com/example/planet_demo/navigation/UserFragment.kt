package com.example.planet_demo.navigation

import android.app.Dialog
import android.content.ContentValues.TAG
import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.planet_demo.LoginActivity
import com.example.planet_demo.MainActivity
import com.example.planet_demo.R
import com.example.planet_demo.SetInfoActivity
import com.example.planet_demo.navigation.DetailViewFragment.DetailViewRecyclerViewAdapter.CustomViewHolder
import com.example.planet_demo.navigation.model.AlarmDTO
import com.example.planet_demo.navigation.model.ContentDTO
import com.example.planet_demo.navigation.model.FollowDTO
import com.example.planet_demo.navigation.model.TodoDTO
import com.example.planet_demo.navigation.util.FcmPush
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_user.view.*
import kotlinx.android.synthetic.main.item_todo.view.*

class UserFragment : Fragment(){
    var fragmentView: View?=null
    var firestore: FirebaseFirestore?=null
    var uid: String?=null
    var auth: FirebaseAuth?=null
    var currentUserUid:String?=null
    var followListenerRegistration:ListenerRegistration?=null

    // 클래스 레벨 변수들
    private var todoAdapter: TodoListAdapter? = null
    private val todoList = ArrayList<TodoDTO>()

    companion object{
        var PICK_PROFILE_FROM_ALBUM=10
        const val REQUEST_CODE_REMOVE_ACCOUNT = 1001
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
            fragmentView?.profile_setting_btn?.visibility = View.VISIBLE // 설정 아이콘 보이기
            fragmentView?.account_btn_follow_signout?.setOnClickListener {
                activity?.finish()
                startActivity(Intent(activity,LoginActivity::class.java))
                auth?.signOut()
            }

            // Firestore에서 "infos" 컬렉션에서 uid와 일치하는 문서를 가져옴
            firestore?.collection("infos")?.document(uid!!)
                ?.get()
                ?.addOnSuccessListener { documentSnapshot ->
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        val nickname = documentSnapshot.getString("nickname")
                        val bio = documentSnapshot.getString("bio")

                        // 닉네임과 소개글 표시
                        fragmentView?.account_tv_nickname?.text = nickname
                        fragmentView?.account_tv_intro?.text = bio
                    }
                }
            // 설정 아이콘 클릭 처리
            fragmentView?.profile_setting_btn?.setOnClickListener {
                // Create a PopupMenu
                val popupMenu = PopupMenu(requireContext(), it)
                popupMenu.inflate(R.menu.popup_menu_item3) // Use the popup_menu_item3.xml

                // Set click listener for menu items
                popupMenu.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        R.id.set_info_option -> {
                            // "프로필 설정" 메뉴 클릭 시 프로필 설정 화면으로 이동
                            startActivity(Intent(context, SetInfoActivity::class.java))
                            true
                        }
                        R.id.set_pw_option -> {
                            // "비밀번호 재설정" 메뉴 클릭 시 비밀번호 설정 화면으로 이동
                            startActivity(Intent(context, SetPwActivity::class.java))
                            true
                        }
                        R.id.remove_account_option -> {
                            //"회원 탈퇴" 메뉴 클릭 시 (비밀번호 인증/회원 탈퇴) 화면으로 이동
                            val intent = Intent(context, CheckPasswordActivity::class.java)
                            startActivityForResult(intent, REQUEST_CODE_REMOVE_ACCOUNT)
                            true
                        }
                        else -> false
                    }
                }

                // Show the popup menu
                popupMenu.show()
            }
            // 플래너 아이콘 클릭 처리
            fragmentView?.planner_btn?.setOnClickListener {
                Toast.makeText(context, "플래너 버튼 클릭", Toast.LENGTH_LONG).show()
                // 플래너 버튼 클릭시 할 일 목록 다이얼로그를 띄움
                showTodoListDialog()
            }
            // RecyclerView와 어댑터 초기화
            val todoRecyclerView = fragmentView?.findViewById<RecyclerView>(R.id.todoRecyclerView)
            todoAdapter = TodoListAdapter(todoList)
            todoRecyclerView?.layoutManager = LinearLayoutManager(requireContext())
            todoRecyclerView?.adapter = todoAdapter

            // Firestore에서 할 일 목록 데이터를 불러와서 todoList에 추가하는 코드
            firestore?.collection("todolists")?.document(currentUserUid!!)
                ?.collection("todos")
                ?.orderBy("timestamp", Query.Direction.ASCENDING) // 오름차순 정렬
                ?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                    if (querySnapshot != null) {
                        todoList.clear()
                        for (document in querySnapshot.documents) {
                            val todo = document.toObject(TodoDTO::class.java)
                            if (todo != null) {
                                todoList.add(todo)
                            }
                        }
                        todoAdapter?.notifyDataSetChanged()
                    }
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

            fragmentView?.profile_setting_btn?.visibility = View.GONE // 설정 아이콘 숨기기

            fragmentView?.account_btn_follow_signout?.setOnClickListener {
                requestFollow()
            }

            firestore?.collection("infos")?.document(uid!!)
                ?.get()
                ?.addOnSuccessListener { documentSnapshot ->
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        val nickname = documentSnapshot.getString("nickname")
                        val bio = documentSnapshot.getString("bio")

                        // 닉네임과 소개글 표시
                        fragmentView?.account_tv_nickname?.text = nickname
                        fragmentView?.account_tv_intro?.text = bio

                        // 닉네임을 툴바의 TextView에 설정
                        mainactivity?.toolbar_username?.text = nickname
                    }
                }
        }

        fragmentView?.account_recyclerview?.adapter=UserFragmentRecyclerViewAdapter()
        fragmentView?.account_recyclerview?.layoutManager=GridLayoutManager(activity,3)

        getProfileImage() //프로필 업로드
        getFollowerAndFollowing()

        // 플래너 아이콘 클릭 처리
        // 플래너 아이콘 클릭 처리
        fragmentView?.planner_btn?.setOnClickListener {
            if (uid != currentUserUid) { // 현재 로그인한 사용자의 프로필이 아니라면
                // 다른 사용자의 할 일 목록 다이얼로그 표시
                showOtherUserTodoListDialog()
            } else {
                // 현재 로그인한 사용자의 할 일 목록 다이얼로그 표시
                showTodoListDialog()
            }
        }

        return fragmentView
    }

    private fun showOtherUserTodoListDialog() {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_todo_list)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val todoRecyclerView = dialog.findViewById<RecyclerView>(R.id.todoRecyclerView)
        val otherUserTodoList = ArrayList<TodoDTO>()

        // Firestore에서 다른 사용자의 할 일 목록 데이터를 불러오는 코드
        firestore?.collection("todolists")?.document(uid!!)
            ?.collection("todos")
            ?.orderBy("timestamp", Query.Direction.ASCENDING) // 오름차순 정렬
            ?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                if (querySnapshot != null) {
                    otherUserTodoList.clear()
                    for (document in querySnapshot.documents) {
                        val todo = document.toObject(TodoDTO::class.java)
                        if (todo != null) {
                            otherUserTodoList.add(todo)
                        }
                    }
                    // 어댑터 설정
                    val otherUserTodoAdapter = OtherUserTodoListAdapter(otherUserTodoList)
                    todoRecyclerView?.layoutManager = LinearLayoutManager(requireContext())
                    todoRecyclerView?.adapter = otherUserTodoAdapter

                    todoAdapter?.notifyDataSetChanged()
                }
            }

        val closeImageView = dialog.findViewById<ImageView>(R.id.closeImageView)

        // Close 버튼 클릭 시 다이얼로그 닫기
        closeImageView.setOnClickListener {
            dialog.dismiss()
        }

        // 숨기는 부분 추가
        val todoInputText = dialog.findViewById<EditText>(R.id.todoInputText)
        val addTodoImageView = dialog.findViewById<ImageView>(R.id.addTodoImageView)
        todoInputText.visibility = View.GONE
        addTodoImageView.visibility = View.GONE

        // 다이얼로그를 표시하기 위한 코드
        dialog.show()
    }

    private inner class OtherUserTodoListAdapter(private val todos: ArrayList<TodoDTO>) :
        RecyclerView.Adapter<OtherUserTodoListAdapter.TodoViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodoViewHolder {
            val itemView = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_todo, parent, false)
            return TodoViewHolder(itemView)
        }

        override fun onBindViewHolder(holder: TodoViewHolder, position: Int) {
            val todo = todos[position]
            holder.bind(todo)

            // OtherUserPage에서는 optionsImageView를 숨깁니다.
            holder.itemView.optionsImageView.visibility = View.GONE
        }

        override fun getItemCount(): Int {
            return todos.size
        }

        inner class TodoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            fun bind(todo: TodoDTO) {
                // 데이터를 UI 요소에 바인딩하는 부분
                val todoTextView = itemView.findViewById<TextView>(R.id.todoTextView)
                val checkBoxImageView = itemView.findViewById<ImageView>(R.id.todoCheckBoxImageView)

                todoTextView.text = todo.todo
                checkBoxImageView.setImageResource(
                    if (todo.checked) R.drawable.ic_checkbox else R.drawable.ic_box
                )
            }
        }
    }

    private fun showTodoListDialog() {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_todo_list)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val todoInputText = dialog.findViewById<EditText>(R.id.todoInputText)
        val addTodoImageView = dialog.findViewById<ImageView>(R.id.addTodoImageView)
        val closeImageView = dialog.findViewById<ImageView>(R.id.closeImageView)


        // RecyclerView와 어댑터 초기화
        val todoRecyclerView = dialog.findViewById<RecyclerView>(R.id.todoRecyclerView)
        todoAdapter = TodoListAdapter(todoList)
        todoRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        todoRecyclerView.adapter = todoAdapter

        // 새로운 할 일을 추가하는 처리
        addTodoImageView.setOnClickListener {
            val todoText = todoInputText.text.toString().trim()
            if (todoText.isNotEmpty()) {
                val newTodo = TodoDTO(todo = todoText)
                addTodoToFirestore(newTodo)
                todoInputText.text.clear() // 입력 필드 초기화
            }
        }

        // Close 버튼 클릭 시 다이얼로그 닫기
        closeImageView.setOnClickListener {
            dialog.dismiss()
        }

        // 다이얼로그를 표시하기 위한 코드
        dialog.show()
    }

    private fun addTodoToFirestore(newTodo: TodoDTO) {
        val todoDocRef = firestore?.collection("todolists")?.document(currentUserUid!!)
        val todosCollectionRef = todoDocRef?.collection("todos")

        todosCollectionRef?.add(newTodo)?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // 할 일이 성공적으로 추가됨, UI 업데이트
                todoList.add(newTodo)
                todoAdapter?.notifyItemInserted(todoList.size - 1)

                // Firestore에 추가된 데이터 반영
                todosCollectionRef.document(task.result?.id ?: "").update("todoId", task.result?.id)
            } else {
                // 오류 처리
                Toast.makeText(requireContext(), "할 일을 추가할 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showPopupMenu(view: View, todo: TodoDTO) {
        val popupMenu = PopupMenu(requireContext(), view)
        popupMenu.inflate(R.menu.popup_menu_item) // 수정, 삭제 메뉴 포함한 XML 파일
        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_edit_option -> {
                    // "수정" 메뉴 클릭 시 할 일 수정 동작 수행
                    showEditTodoDialog(todo)
                    true
                }
                R.id.menu_delete_option -> {
                    // "삭제" 메뉴 클릭 시 할 일 삭제 동작 수행
                    deleteTodoFromFirestore(todo)
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    private fun showEditTodoDialog(todo: TodoDTO) {
        val editDialog = Dialog(requireContext())
        editDialog.setContentView(R.layout.item_edit_todo)

        val todoEditText = editDialog.findViewById<EditText>(R.id.todoEditText)
        val optionsImageView = editDialog.findViewById<ImageView>(R.id.optionsImageView)

        // 기존 할 일 내용을 에딧텍스트에 설정
        todoEditText.setText(todo.todo)

        optionsImageView.setOnClickListener {
            val updatedTodo = todoEditText.text.toString()
            if (updatedTodo.isNotEmpty()) {
                // 수정된 내용을 Firestore에 업데이트하고 UI 갱신
                updateTodoInFirestore(todo.todoId, updatedTodo)
                editDialog.dismiss()
            } else {
                // 내용이 비어있을 경우에 대한 처리
                // 예: 사용자에게 알림 또는 처리 로직 추가
            }
        }

        // 수정 다이얼로그를 표시하기 위한 코드
        editDialog.show()
    }

    private fun updateTodoInFirestore(todoId: String, updatedTodo: String) {
        // Firestore에서 해당 할 일 문서를 찾아 수정
        firestore?.collection("todolists")?.document(currentUserUid!!)
            ?.collection("todos")?.document(todoId)
            ?.update("todo", updatedTodo)
            ?.addOnSuccessListener {
                // 수정 성공 시 UI 업데이트 처리
                // 예: Toast 메시지 출력 또는 todoList 갱신
            }
            ?.addOnFailureListener { e ->
                // 수정 실패 시 처리
                // 예: Toast 메시지 출력 또는 오류 처리 로직
            }
    }

    private fun deleteTodoFromFirestore(todo: TodoDTO) {
        val todoDocRef = firestore?.collection("todolists")?.document(currentUserUid!!)
        val todosCollectionRef = todoDocRef?.collection("todos")

        todosCollectionRef?.document(todo.todoId)?.delete()
            ?.addOnSuccessListener {
                // 성공적으로 삭제되었을 때 UI 업데이트
                todoList.remove(todo)
                todoAdapter?.notifyDataSetChanged()
            }
            ?.addOnFailureListener { e ->
                // 삭제 실패 처리
                Toast.makeText(requireContext(), "할 일을 삭제할 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
    }

    inner class TodoListAdapter(private val todos: ArrayList<TodoDTO>) :
        RecyclerView.Adapter<TodoListAdapter.TodoViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodoViewHolder {
            val itemView = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_todo, parent, false)
            return TodoViewHolder(itemView)
        }

        override fun onBindViewHolder(holder: TodoViewHolder, position: Int) {
            val todo = todos[position]
            holder.bind(todo)

            // 옵션 버튼 클릭 시 팝업 메뉴 표시
            holder.itemView.optionsImageView.setOnClickListener {
                showPopupMenu(holder.itemView.optionsImageView, todo)
            }
        }

        override fun getItemCount(): Int {
            return todos.size
        }

        inner class TodoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            fun bind(todo: TodoDTO) {
                // 데이터를 UI 요소에 바인딩하는 부분
                val todoTextView = itemView.findViewById<TextView>(R.id.todoTextView)
                val checkBoxImageView = itemView.findViewById<ImageView>(R.id.todoCheckBoxImageView)

                todoTextView.text = todo.todo
                checkBoxImageView.setImageResource(
                    if (todo.checked) R.drawable.ic_checkbox else R.drawable.ic_box
                )

                checkBoxImageView.setOnClickListener {
                    // 체크 여부 업데이트 및 UI 업데이트
                    todo.checked = !todo.checked
                    checkBoxImageView.setImageResource(
                        if (todo.checked) R.drawable.ic_checkbox else R.drawable.ic_box
                    )

                    // Firestore에 checked 필드 업데이트
                    updateTodoChecked(todo)
                }
            }
        }

        // Firestore에 checked 필드 업데이트하는 함수 추가
        private fun updateTodoChecked(todo: TodoDTO) {
            firestore?.collection("todolists")?.document(currentUserUid!!)
                ?.collection("todos")?.document(todo.todoId)
                ?.update("checked", todo.checked)
                ?.addOnSuccessListener {
                    Toast.makeText(context,"Todo checked updated successfully",Toast.LENGTH_LONG)
                }?.addOnFailureListener { e ->
                    Toast.makeText(context,"Error updating todo checked",Toast.LENGTH_LONG)
                }
        }
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
                followDTO!!.followings[uid!!]=true

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
        var nicknameRef= currentUserUid?.let {
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
                alarmDTO.uid=auth?.currentUser?.uid
                alarmDTO.kind=2
                alarmDTO.timestamp=System.currentTimeMillis()
                FirebaseFirestore.getInstance().collection("alarms").document().set(alarmDTO)

                //팔로우 클릭시 FCM 메시지 생성
                var message=nickname + getString(R.string.alarm_follow)
                FcmPush.instance.sendMessage(destinationUid,"PlaNet",message)
            }
        }
    }

    //firestore database에서 프로필 이미지 받아놈
    fun getProfileImage(){
        // Check if the current user is viewing their own profile
        val isCurrentUserProfile = uid == currentUserUid

        // Load the profile image for the current user
        firestore?.collection("profileImages")?.document(uid!!)?.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
            if (documentSnapshot == null) return@addSnapshotListener
            if (documentSnapshot.data != null) {
                val url = documentSnapshot.data!!["image"]
                Glide.with(requireActivity()).load(url).apply(RequestOptions().circleCrop()).into(fragmentView?.account_iv_profile!!)
            }
        }

        if (isCurrentUserProfile) {
            // Allow changing profile image only for the current user
            fragmentView?.account_iv_profile?.setOnClickListener {
                var photoPickerIntent = Intent(Intent.ACTION_PICK)
                photoPickerIntent.type = "image/*"
                activity?.startActivityForResult(photoPickerIntent, PICK_PROFILE_FROM_ALBUM)
            }
        } else {
            // Disable profile image click for other users
            fragmentView?.account_iv_profile?.setOnClickListener(null)
        }
    }
}