package com.example.runbuddy.ui.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runbuddy.data.repo.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// ✅ UI 상태
data class SignUpUiState(
    val loading: Boolean = false,
    val success: Boolean = false,
    val error: String? = null
)

class SignUpViewModel(private val repo: UserRepository) : ViewModel() {

    private val _state = MutableStateFlow(SignUpUiState())
    val state: StateFlow<SignUpUiState> = _state

    fun signUp(
        name: String,
        email: String,
        password: String,
        confirm: String
    ) {
        // ✅ 입력 검증
        when {
            name.isBlank() -> return fail("이름을 입력해주세요.")
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() ->
                return fail("올바른 이메일을 입력해주세요.")
            !isStrong(password) ->
                return fail("비밀번호는 8자 이상이며 숫자를 포함해야 합니다.")
            password != confirm ->
                return fail("비밀번호가 일치하지 않습니다.")
        }

        // 로딩 시작
        _state.value = SignUpUiState(loading = true)

        viewModelScope.launch {
            // ⭐ targetKm 제거 → repo.signUp(name, email, password) 만 호출
            val result = repo.signUp(name, email, password)

            _state.value = result.fold(
                onSuccess = { SignUpUiState(success = true) },
                onFailure = { SignUpUiState(error = it.message ?: "회원가입 실패") }
            )
        }
    }

    private fun isStrong(pw: String): Boolean {
        val hasDigit = pw.any { it.isDigit() }
        return pw.length >= 8 && hasDigit
    }

    private fun fail(msg: String) {
        _state.value = SignUpUiState(error = msg)
    }
}
