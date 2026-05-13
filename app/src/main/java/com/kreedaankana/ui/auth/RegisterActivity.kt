package com.kreedaankana.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.kreedaankana.R
import com.kreedaankana.databinding.ActivityRegisterBinding
import com.kreedaankana.ui.MainActivity
import com.kreedaankana.utils.Extensions.gone
import com.kreedaankana.utils.Extensions.showToast
import com.kreedaankana.utils.Extensions.visible

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val auth = FirebaseAuth.getInstance()
    private lateinit var googleSignInClient: GoogleSignInClient

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)!!
            firebaseAuthWithGoogle(account.idToken!!)
        } catch (e: ApiException) {
            val statusCode = e.statusCode
            val explanation = when (statusCode) {
                10 -> "Developer Error (Code 10). This usually means your SHA-1 fingerprint is not registered in the Firebase Console, or doesn't match your keystore signature."
                12500 -> "Sign-In Failed (Code 12500). Please check your Google Play Services configuration, or confirm the Web Client ID matches Firebase."
                12501 -> "Google Sign-In cancelled."
                7 -> "Network Error (Code 7). Please check your internet connection."
                else -> "Error code $statusCode: ${e.message ?: "Unknown error"}"
            }
            showToast("Google Sign-In failed: $explanation")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupGoogleSignIn()
        setupListeners()
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun setupListeners() {
        binding.btnRegister.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val confirmPassword = binding.etConfirmPassword.text.toString().trim()

            if (name.isEmpty()) { binding.tilName.error = "Required"; return@setOnClickListener }
            if (email.isEmpty()) { binding.tilEmail.error = "Required"; return@setOnClickListener }
            if (password.isEmpty()) { binding.tilPassword.error = "Required"; return@setOnClickListener }
            if (password != confirmPassword) {
                binding.tilConfirmPassword.error = "Passwords do not match"
                return@setOnClickListener
            }

            binding.tilName.error = null
            binding.tilEmail.error = null
            binding.tilPassword.error = null
            binding.tilConfirmPassword.error = null

            binding.progressBar.visible()
            binding.btnRegister.isEnabled = false
            binding.btnGoogleSignUp.isEnabled = false

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(name)
                            .build()

                        val db = FirebaseFirestore.getInstance()
                        val userData = hashMapOf(
                            "uid" to user?.uid,
                            "name" to name,
                            "email" to email,
                            "teamName" to "",
                            "role" to "user",
                            "createdAt" to FieldValue.serverTimestamp()
                        )

                        user?.updateProfile(profileUpdates)?.addOnCompleteListener {
                            db.collection("users").document(user.uid).set(userData)
                                .addOnCompleteListener {
                                    binding.progressBar.gone()
                                    binding.btnRegister.isEnabled = true
                                    binding.btnGoogleSignUp.isEnabled = true
                                    showToast("Registration Successful!")
                                    startActivity(Intent(this, MainActivity::class.java).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    })
                                    finish()
                                }
                        }
                    } else {
                        binding.progressBar.gone()
                        binding.btnRegister.isEnabled = true
                        binding.btnGoogleSignUp.isEnabled = true
                        showToast("Registration failed: ${task.exception?.message}")
                    }
                }
        }

        binding.btnGoogleSignUp.setOnClickListener {
            // Sign out to ensure account selection dialog always pops up
            googleSignInClient.signOut().addOnCompleteListener {
                val signInIntent = googleSignInClient.signInIntent
                googleSignInLauncher.launch(signInIntent)
            }
        }

        binding.tvLogin.setOnClickListener {
            finish()
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        binding.progressBar.visible()
        binding.btnRegister.isEnabled = false
        binding.btnGoogleSignUp.isEnabled = false

        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val db = FirebaseFirestore.getInstance()
                    db.collection("users").document(user!!.uid).get()
                        .addOnCompleteListener { dbTask ->
                            binding.progressBar.gone()
                            binding.btnRegister.isEnabled = true
                            binding.btnGoogleSignUp.isEnabled = true

                            if (dbTask.isSuccessful && !dbTask.result.exists()) {
                                // Save new user info to Firestore
                                val userData = hashMapOf(
                                    "uid" to user.uid,
                                    "name" to (user.displayName ?: "Google User"),
                                    "email" to user.email,
                                    "teamName" to "",
                                    "role" to "user",
                                    "createdAt" to FieldValue.serverTimestamp()
                                )
                                db.collection("users").document(user.uid).set(userData)
                                    .addOnCompleteListener {
                                        showToast("Registration Successful!")
                                        startActivity(Intent(this, MainActivity::class.java).apply {
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                        })
                                        finish()
                                    }
                            } else {
                                showToast("Login Successful!")
                                startActivity(Intent(this, MainActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                })
                                finish()
                            }
                        }
                } else {
                    binding.progressBar.gone()
                    binding.btnRegister.isEnabled = true
                    binding.btnGoogleSignUp.isEnabled = true
                    showToast("Google authentication failed: ${task.exception?.message}")
                }
            }
    }
}
