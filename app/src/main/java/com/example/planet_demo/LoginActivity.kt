package com.example.planet_demo

import android.accounts.Account
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity() {
    var auth: FirebaseAuth?=null // Firebase Authentication 인스턴스를 나타내는 변수
    var googleSignInClient: GoogleSignInClient?=null //Google 로그인 클라이언트를 나타내는 변수
    var GOOGLE_LOGIN_CODE=9001 //Google 로그인 인증 요청 코드

    override fun onCreate(savedInstanceState: Bundle?) { //액티비티가 생성될 때 호출되는 onCreate 메서드
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login) //activity_login.xml 레이아웃을 설정하고,
        auth=FirebaseAuth.getInstance() //auth에 Firebase 인증 인스턴스를 할당

        //이메일 로그인 버튼과 구글 로그인 버튼의 클릭 이벤트를 설정
        email_login_button.setOnClickListener {
            signinAndSignup()
        }
        google_sign_in_button.setOnClickListener {
            //First Step
            googleLogin()
        }

        var gso=GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("1061544614415-8dbsrqsocamm783a9o2hp35phoiibkt9.apps.googleusercontent.com")
            .requestEmail()
            .build()
        googleSignInClient=GoogleSignIn.getClient(this,gso) //googleSignInClient를 초기화
    }

    override fun onStart() { //액티비티가 시작될 때 호출되는 onStart 메서드
        super.onStart()

        //현재 로그인된 사용자가 있을 경우, 해당 사용자의 정보 설정 여부를 확인
        val currentUser = auth?.currentUser
        if (currentUser != null) {
            // If the user is already logged in, check if user info is set
            checkUserInfo(currentUser)
        }
    }

    fun checkUserInfo(user: FirebaseUser) { //사용자 정보를 확인하는 함수

        //사용자의 UID를 기반으로 Firestore에서 해당 사용자의 정보(닉네임, 소개글) 설정 여부를 확인
        val uid = user.uid
        val userDocRef = FirebaseFirestore.getInstance().collection("infos").document(uid)

        //정보 설정 여부에 따라 메인 페이지로 이동하거나 프로필 설정 페이지로 이동
        userDocRef.get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    // User info is already set, move to main page
                    moveMainPage(user)
                } else {
                    // User info is not set, move to info setting page
                    startSetInfoActivity()
                }
            }
            .addOnFailureListener { e ->
                // Show the error message
                Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
            }
    }

    fun startSetInfoActivity() { //프로필 설정 페이지를 시작하는 함수
        val intent = Intent(this, SetInfoActivity::class.java)
        startActivity(intent) //SetInfoActivity 클래스로 이동
    }

    fun googleLogin(){ //구글 로그인을 시도하는 함수
        var signInIntent=googleSignInClient?.signInIntent
        signInIntent?.let { startActivityForResult(it,GOOGLE_LOGIN_CODE) } //구글 로그인 클라이언트를 사용하여 로그인 인증 요청을 보냄
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) { //로그인 결과를 처리하는 함수
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode==GOOGLE_LOGIN_CODE){ //구글 로그인 결과를 받아서 처리
            var result= data?.let { Auth.GoogleSignInApi.getSignInResultFromIntent(it) }
            if (result != null) {
                if(result.isSuccess){
                    var account= result?.signInAccount
                    //Second Step
                    firebaseAuthWithGoogle(account)
                }
            }
        }
    }
    fun firebaseAuthWithGoogle(account: GoogleSignInAccount?){ //구글 로그인 결과로 받은 인증 정보를 사용하여 Firebase에 인증을 시도하는 함수
        var credential=GoogleAuthProvider.getCredential(account?.idToken,null)
        auth?.signInWithCredential(credential)
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) { //로그인이 성공할 경우 사용자 정보를 확인
                    //Login
                    val user = task.result?.user
                    if (user != null) {
                        checkUserInfo(user) // Check user info after successful login
                    }
                } else {
                    //Show the error message
                    Toast.makeText(this,task.exception?.message,Toast.LENGTH_LONG).show()
                }
            }
    }
    fun signinAndSignup(){ //이메일과 비밀번호를 사용하여 회원 가입 및 로그인을 시도하는 함수
        auth?.createUserWithEmailAndPassword(email_edittext.text.toString(),password_edittext.text.toString())
            ?.addOnCompleteListener {
             task->
                if (task.isSuccessful){ //회원 가입 또는 로그인이 성공할 경우 사용자 정보를 확인
                    val user = task.result?.user
                    if (user != null) {
                        checkUserInfo(user)
                    }
                }else if (task.exception?.message.isNullOrEmpty()){ //실패할 경우 오류 메시지를 보여줌
                    //Show the error message
                    Toast.makeText(this,task.exception?.message,Toast.LENGTH_LONG).show()
                }else{
                    //Login if you have account
                    signinEmail()
                }
            }
    }

    fun signinEmail(){ //이메일과 비밀번호를 사용하여 로그인을 시도하는 함수
        auth?.signInWithEmailAndPassword(email_edittext.text.toString(),password_edittext.text.toString())
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Login
                    val user = task.result?.user
                    if (user != null) {
                        checkUserInfo(user)
                    }
                } else {
                    val errorMessage = task.exception?.message
                    //로그인이 실패하고 오류 메시지에 "There is no user record corresponding"이 포함되어 있는 경우, signupEmail 함수를 호출하여 회원 가입을 시도
                    if (errorMessage != null && errorMessage.contains("There is no user record corresponding")) {
                        // If there's no user record, attempt to sign up
                        signupEmail()
                    } else {
                        // Show the error message
                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                    }
                }
            }
    }

    fun signupEmail() { //이메일과 비밀번호를 사용하여 회원 가입을 시도하는 signupEmail 함수
        auth?.createUserWithEmailAndPassword(email_edittext.text.toString(), password_edittext.text.toString())
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) { //회원 가입이 성공할 경우 사용자 정보를 확인
                    // User successfully signed up, proceed to checkUserInfo
                    val user = task.result?.user
                    if (user != null) {
                        checkUserInfo(user)
                    }
                } else { //실패할 경우 오류 메시지
                    // Show the error message
                    Toast.makeText(this, task.exception?.message, Toast.LENGTH_LONG).show()
                }
            }
    }

    fun moveMainPage(user:FirebaseUser?){ //메인 페이지로 이동하는 함수
        if (user!=null){ //사용자가 로그인되어 있다면 메인 액티비티로 이동
            startActivity(Intent(this,MainActivity::class.java))
            finish()
        }
    }
}