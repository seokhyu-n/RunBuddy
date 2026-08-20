package com.example.runbuddy.login

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.runbuddy.data.local.AppDb
import com.example.runbuddy.data.repo.UserRepository
import com.example.runbuddy.databinding.ActivityLoginBinding
import com.example.runbuddy.home.HomeActivity
import com.example.runbuddy.ui.signup.SignUpActivity
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val repo by lazy { UserRepository(AppDb.get(this)) }
    private val store = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ================================
        // ⭐ SharedPreferences
        // ================================
        val prefs = getSharedPreferences("runbuddy_prefs", MODE_PRIVATE)

        val isAutoLogin = prefs.getBoolean("autoLogin", false)
        val savedUserName = prefs.getString("userName", null)
        val savedUserEmail = prefs.getString("userEmail", null)
        val savedUserUid = prefs.getString("userUid", null)

        // ⭐ 자동로그인 → 모든 정보가 있을 때만 홈으로 이동
        if (isAutoLogin &&
            !savedUserName.isNullOrBlank() &&
            !savedUserEmail.isNullOrBlank() &&
            !savedUserUid.isNullOrBlank()
        ) {
            goHome(savedUserName, savedUserEmail, savedUserUid)
            return
        }

        // ================================
        // ⭐ 로그인 버튼
        // ================================
        binding.btnLogin.setOnClickListener {
            val emailInput = binding.etEmail.text.toString().trim()
            val pw = binding.etPassword.text.toString().trim()

            if (emailInput.isEmpty()) {
                Toast.makeText(this, "이메일을 입력하세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (pw.isEmpty()) {
                Toast.makeText(this, "비밀번호를 입력하세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            hideKeyboard()
            setLoading(true)

            lifecycleScope.launch {
                val result = repo.login(emailInput, pw)
                setLoading(false)

                result.onSuccess { uid ->

                    // Firestore에서 유저 정보 가져오기
                    val doc = store.collection("users").document(uid).get().await()

                    val name = doc.getString("name") ?: "사용자"
                    val email = doc.getString("email") ?: emailInput

                    // ⭐ 자동로그인 저장
                    if (binding.cbAutoLogin.isChecked) {
                        prefs.edit()
                            .putBoolean("autoLogin", true)
                            .putString("userName", name)
                            .putString("userEmail", email)
                            .putString("userUid", uid)
                            .apply()
                    } else {
                        prefs.edit()
                            .putBoolean("autoLogin", false)
                            .remove("userName")
                            .remove("userEmail")
                            .remove("userUid")
                            .apply()
                    }

                    Toast.makeText(this@LoginActivity, "로그인 성공!", Toast.LENGTH_SHORT).show()
                    goHome(name, email, uid)

                }.onFailure { e ->
                    Snackbar.make(
                        binding.root,
                        e.message ?: "로그인 실패. 다시 시도해주세요.",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }

        // ⭐ 회원가입 이동
        binding.tvGoSignUp.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }
    }

    // ================================
    // ⭐ 홈으로 이동 (email, uid 포함!)
    // ================================
    private fun goHome(userName: String, email: String, uid: String) {
        startActivity(
            Intent(this, HomeActivity::class.java).apply {
                putExtra("userName", userName)
                putExtra("userEmail", email)
                putExtra("userUid", uid)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
        finish()
    }

    private fun setLoading(loading: Boolean) {
        binding.btnLogin.isEnabled = !loading
        binding.btnLogin.text = if (loading) "로그인 중..." else "로그인"
        binding.progress.isVisible = loading
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        currentFocus?.windowToken?.let { imm.hideSoftInputFromWindow(it, 0) }
    }
}
