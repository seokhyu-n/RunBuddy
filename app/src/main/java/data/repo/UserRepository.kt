package com.example.runbuddy.data.repo

import com.example.runbuddy.data.local.AppDb
import com.example.runbuddy.data.local.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.security.MessageDigest

/**
 * RunBuddy UserRepository
 * - Firebase Auth: 이메일/비밀번호 계정 생성 및 로그인
 * - Firestore: 사용자 프로필(name, email) 저장
 * - Room(Local): 로컬 캐시 저장
 */
class UserRepository(
    private val db: AppDb,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val store: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /** Firestore에 저장할 사용자 프로필 모델 */
    data class CloudUser(
        val name: String = "",
        val email: String = ""
    )

    /**
     * 회원가입
     * 1) Room 중복 이메일 체크
     * 2) Firebase Auth 생성
     * 3) Firestore 프로필 저장
     * 4) Room 로컬 저장
     */
    suspend fun signUp(
        name: String,
        email: String,
        password: String
    ): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val userDao = db.userDao()

            // 1) Room 중복 체크
            if (userDao.findByEmail(email) != null) {
                return@withContext Result.failure(Exception("이미 사용 중인 이메일입니다."))
            }

            // 2) Firebase Auth 계정 생성
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = authResult.user?.uid ?: throw IllegalStateException("회원가입 실패: UID 없음")

            // 3) Firestore 프로필 저장
            val cloudUser = CloudUser(name = name, email = email)
            store.collection("users").document(uid).set(cloudUser).await()

            // 4) 로컬 Room 저장
            val hashedPw = sha256(password)
            val localUser = User(
                name = name,
                email = email,
                passwordHash = hashedPw,
                targetKm = null  // ← 필드만 남아있을 수 있으니 null 저장
            )
            val rowId = userDao.insert(localUser)

            Result.success(rowId)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * 로그인
     */
    suspend fun login(email: String, password: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val res = auth.signInWithEmailAndPassword(email, password).await()
                val uid = res.user?.uid ?: throw IllegalStateException("로그인 실패: UID 없음")
                Result.success(uid)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }

    /** 현재 로그인 UID */
    fun currentUid(): String? = auth.currentUser?.uid

    /** 로그아웃 */
    fun signOut() = auth.signOut()

    /** 이메일로 로컬 사용자 조회 */
    suspend fun findUserByEmail(email: String): User? = withContext(Dispatchers.IO) {
        db.userDao().findByEmail(email)
    }

    /** SHA-256 해시 */
    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
