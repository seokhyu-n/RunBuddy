package com.example.runbuddy.ui.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.runbuddy.data.local.AppDb
import com.example.runbuddy.data.repo.UserRepository

class SignUpViewModelFactory(private val db: AppDb) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SignUpViewModel::class.java)) {
            val repo = UserRepository(db) // 내부에서 Firebase Auth/Firestore 사용
            return SignUpViewModel(repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
