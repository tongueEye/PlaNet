package com.example.planet_demo.navigation

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.planet_demo.R

class ConfirmDialogFragment(private val onConfirmListener: () -> Unit) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            val customView = LayoutInflater.from(context).inflate(R.layout.dialog_confirm, null)
            builder.setView(customView) // dialog_confirm.xml 레이아웃 사용

            // 커스텀한 알림창 UI의 뷰 요소 가져오기
            val confirmTextView: TextView = customView.findViewById(R.id.confirmTextView)
            val cancelButton: Button = customView.findViewById(R.id.noButton)
            val confirmButton: Button = customView.findViewById(R.id.yesButton)

            // 확인 버튼 클릭 처리
            confirmButton.setOnClickListener {
                onConfirmListener.invoke()
                dismiss()
            }

            // 취소 버튼 클릭 처리
            cancelButton.setOnClickListener {
                dismiss()
            }

            // 커스텀한 알림창 UI에 메시지 설정
            confirmTextView.text = "삭제하시겠습니까?"
            builder.create()

        } ?: throw IllegalStateException("Activity cannot be null")
    }


}