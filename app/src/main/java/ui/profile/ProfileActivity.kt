package com.example.runbuddy.ui.profile

import android.graphics.Color
import android.app.AlertDialog
import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.runbuddy.databinding.ActivityProfileBinding

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // SharedPreferences
        val prefs = getSharedPreferences("runbuddy_prefs", MODE_PRIVATE)

        // 전달된 유저 정보
        var name = intent.getStringExtra("userName") ?: "사용자"
        val email = intent.getStringExtra("userEmail") ?: "unknown"
        val uid = intent.getStringExtra("userUid") ?: "-"

        // 표시
        binding.tvUserName.text = name
        binding.tvUserEmail.text = email
        binding.tvUserUid.text = uid

        binding.btnEditName.setOnClickListener {

            val editText = EditText(this).apply {
                setText(binding.tvUserName.text.toString())
                setPadding(50, 40, 50, 40)
            }

            val dialog = AlertDialog.Builder(this)
                .setTitle("이름 수정")
                .setView(editText)
                .setNegativeButton("취소", null)
                .setPositiveButton("저장", null)
                .create()

            dialog.setOnShowListener {

                // 버튼 가져오기
                val negative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                val positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE)

                // 🔥 글씨 검정색으로 변경
                negative.setTextColor(Color.WHITE)
                positive.setTextColor(Color.WHITE)

                // 🔥 버튼 간격 띄우기
                val params = positive.layoutParams as LinearLayout.LayoutParams
                params.marginStart = 40   // ← 원하는 만큼 띄우기(40dp 느낌)
                positive.layoutParams = params

                // 🔥 저장 버튼 클릭
                positive.setOnClickListener {
                    val newName = editText.text.toString().trim()
                    if (newName.isNotEmpty()) {

                        binding.tvUserName.text = newName

                        // SharedPreferences 저장
                        val prefs = getSharedPreferences("runbuddy_prefs", MODE_PRIVATE)
                        prefs.edit().putString("userName", newName).apply()

                        dialog.dismiss()
                    }
                }
            }

            dialog.show()
        }

    }
}
