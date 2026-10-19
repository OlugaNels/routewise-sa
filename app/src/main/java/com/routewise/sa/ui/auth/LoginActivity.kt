package com.routewise.sa.ui.auth

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.routewise.sa.MainActivity
import com.routewise.sa.R
import com.routewise.sa.RouteWiseApplication
import com.routewise.sa.data.SessionManager
import com.routewise.sa.data.model.UserEntity
import com.routewise.sa.util.InputValidator
import com.routewise.sa.util.LocationData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private var isRegistering = false

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    // Views
    private lateinit var tabSignIn: TextView
    private lateinit var tabCreateAccount: TextView
    private lateinit var groupFullName: LinearLayout
    private lateinit var groupPhone: LinearLayout
    private lateinit var groupLocation: LinearLayout
    private lateinit var btnSubmitAuth: Button
    private lateinit var btnGoogleSignIn: MaterialButton
    private lateinit var formScroll: NestedScrollView
    private lateinit var successSplash: LinearLayout
    private lateinit var authCard: LinearLayout

    private lateinit var etFullName: EditText
    private lateinit var etPhone: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var spinnerProvince: AutoCompleteTextView
    private lateinit var spinnerCity: AutoCompleteTextView
    private lateinit var tvWelcomeUser: TextView

    // Forgot Password Views
    private lateinit var btnForgotPasswordLink: TextView
    private lateinit var scrollResetPassword: NestedScrollView
    private lateinit var cardForgotPasswordModal: MaterialCardView
    private lateinit var btnCloseForgotModal: ImageButton
    private lateinit var layoutResetStep1: LinearLayout
    private lateinit var layoutResetStep2: LinearLayout
    private lateinit var etResetEmail: EditText
    private lateinit var btnSendResetCode: MaterialButton

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK || result.data == null) {
            setBusy(false)
            toast("Google sign-in was cancelled")
            return@registerForActivityResult
        }
        try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                .getResult(ApiException::class.java)
            val idToken = account.idToken
            if (idToken == null) {
                setBusy(false)
                toast("Google sign-in failed: no token returned")
                return@registerForActivityResult
            }
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            firebaseAuth.signInWithCredential(credential)
                .addOnSuccessListener { authResult ->
                    val user = authResult.user
                    if (user == null) {
                        setBusy(false)
                        toast("Google sign-in failed")
                    } else {
                        onAuthSuccess(user, user.displayName ?: account.displayName, "google")
                    }
                }
                .addOnFailureListener { e ->
                    setBusy(false)
                    toast(authErrorMessage(e))
                }
        } catch (e: ApiException) {
            setBusy(false)
            toast("Google sign-in failed (code ${e.statusCode}). Check your connection and try again.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionManager = SessionManager(this)
        firebaseAuth = FirebaseAuth.getInstance()

        // Only skip the login screen when there is both a saved session and a live Firebase user
        if (sessionManager.isLoggedIn() && firebaseAuth.currentUser != null) {
            launchMainActivity()
            return
        } else if (sessionManager.isLoggedIn()) {
            sessionManager.logout()
        }

        setContentView(R.layout.activity_login)
        setupGoogleClient()
        initViews()
        initForgotPasswordViews()
        setupTabs()
        setupDropdowns()
        btnGoogleSignIn.setOnClickListener { handleGoogleSignIn() }

        // Start in Register mode
        switchToRegister()

        btnSubmitAuth.setOnClickListener { handleAuth() }

        findViewById<View>(R.id.btnClose).setOnClickListener { finish() }
    }

    private fun setupGoogleClient() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .requestProfile()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun initViews() {
        tabSignIn = findViewById(R.id.tabSignIn)
        tabCreateAccount = findViewById(R.id.tabCreateAccount)
        groupFullName = findViewById(R.id.groupFullName)
        groupPhone = findViewById(R.id.groupPhone)
        groupLocation = findViewById(R.id.groupLocation)
        btnSubmitAuth = findViewById(R.id.btnSubmitAuth)
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn)
        formScroll = findViewById(R.id.formScroll)
        successSplash = findViewById(R.id.successSplash)
        authCard = findViewById(R.id.authCard)

        etFullName = findViewById(R.id.etFullName)
        etPhone = findViewById(R.id.etPhone)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        spinnerProvince = findViewById(R.id.spinnerProvince)
        spinnerCity = findViewById(R.id.spinnerCity)
        tvWelcomeUser = findViewById(R.id.tvWelcomeUser)
    }

    private fun initForgotPasswordViews() {
        btnForgotPasswordLink = findViewById(R.id.btnForgotPasswordLink)
        scrollResetPassword = findViewById(R.id.scrollResetPassword)
        cardForgotPasswordModal = findViewById(R.id.cardForgotPasswordModal)
        btnCloseForgotModal = findViewById(R.id.btnCloseForgotModal)
        layoutResetStep1 = findViewById(R.id.layoutResetStep1)
        layoutResetStep2 = findViewById(R.id.layoutResetStep2)
        etResetEmail = findViewById(R.id.etResetEmail)
        btnSendResetCode = findViewById(R.id.btnSendResetCode)

        btnForgotPasswordLink.setOnClickListener {
            val email = etEmail.text.toString().trim()
            if (email.isNotEmpty()) {
                etResetEmail.setText(email)
            }
            formScroll.visibility = View.GONE
            findViewById<View>(R.id.tabSwitcher).visibility = View.GONE
            findViewById<View>(R.id.layoutSocialDivider).visibility = View.GONE
            btnGoogleSignIn.visibility = View.GONE
            btnSubmitAuth.visibility = View.GONE

            layoutResetStep1.visibility = View.VISIBLE
            layoutResetStep2.visibility = View.GONE
            scrollResetPassword.visibility = View.VISIBLE
        }

        btnCloseForgotModal.setOnClickListener { closeResetPanel() }

        // Real Firebase password reset: Firebase emails a link, no codes are shown on screen
        btnSendResetCode.setOnClickListener {
            val email = etResetEmail.text.toString().trim()
            val error = InputValidator.emailError(email)
            if (error != null) {
                etResetEmail.error = error
                return@setOnClickListener
            }
            btnSendResetCode.isEnabled = false
            firebaseAuth.sendPasswordResetEmail(email)
                .addOnSuccessListener {
                    btnSendResetCode.isEnabled = true
                    toast("If $email has a password account, a reset link has been sent (check spam too). If you signed up with Google, use Sign in with Google instead.")
                    closeResetPanel()
                }
                .addOnFailureListener { e ->
                    btnSendResetCode.isEnabled = true
                    toast(authErrorMessage(e))
                }
        }
    }

    private fun closeResetPanel() {
        scrollResetPassword.visibility = View.GONE
        formScroll.visibility = View.VISIBLE
        findViewById<View>(R.id.tabSwitcher).visibility = View.VISIBLE
        findViewById<View>(R.id.layoutSocialDivider).visibility = View.VISIBLE
        btnGoogleSignIn.visibility = View.VISIBLE
        btnSubmitAuth.visibility = View.VISIBLE
    }

    private fun handleGoogleSignIn() {
        setBusy(true)
        // Clear any cached account so the account chooser always shows
        googleSignInClient.signOut().addOnCompleteListener {
            googleSignInLauncher.launch(googleSignInClient.signInIntent)
        }
    }

    private fun handleAuth() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString()
        val name = etFullName.text.toString().trim()
        val phone = etPhone.text.toString().trim()

        etEmail.error = null
        etPassword.error = null
        etFullName.error = null
        etPhone.error = null

        if (isRegistering && !InputValidator.isValidName(name)) {
            etFullName.error = "Enter your full name (2 to ${InputValidator.MAX_NAME_LENGTH} characters)"
            etFullName.requestFocus()
            return
        }
        InputValidator.emailError(email)?.let {
            etEmail.error = it
            etEmail.requestFocus()
            return
        }
        if (isRegistering && !InputValidator.isValidOptionalPhone(phone)) {
            etPhone.error = "Enter a valid phone number, e.g. +27 82 456 7890"
            etPhone.requestFocus()
            return
        }
        InputValidator.passwordError(password)?.let {
            etPassword.error = it
            etPassword.requestFocus()
            return
        }

        setBusy(true)
        if (isRegistering) {
            firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    val user = result.user
                    if (user == null) {
                        setBusy(false)
                        toast("Registration failed. Please try again.")
                        return@addOnSuccessListener
                    }
                    user.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(name).build())
                        .addOnCompleteListener { onAuthSuccess(user, name, "email") }
                }
                .addOnFailureListener { e ->
                    setBusy(false)
                    toast(authErrorMessage(e))
                }
        } else {
            firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    val user = result.user
                    if (user == null) {
                        setBusy(false)
                        toast("Login failed. Please try again.")
                    } else {
                        onAuthSuccess(user, user.displayName, "email")
                    }
                }
                .addOnFailureListener { e ->
                    setBusy(false)
                    toast(authErrorMessage(e))
                }
        }
    }

    /** Called once Firebase has confirmed the user. Saves the profile and opens the app. */
    private fun onAuthSuccess(user: FirebaseUser, displayName: String?, provider: String) {
        val email = user.email ?: ""
        val name = displayName?.takeIf { it.isNotBlank() } ?: email.substringBefore("@")
        val phone = etPhone.text.toString().trim()
        val province = spinnerProvince.text.toString().ifBlank { "Gauteng" }
        val city = spinnerCity.text.toString().ifBlank { "Johannesburg" }

        // Profile in Firestore (never the password: Firebase Auth stores it hashed)
        try {
            val userMap = hashMapOf(
                "uid" to user.uid,
                "email" to email,
                "fullName" to name,
                "phoneNumber" to phone,
                "province" to province,
                "city" to city,
                "authProvider" to provider,
                "createdAt" to System.currentTimeMillis()
            )
            FirebaseFirestore.getInstance().collection("users").document(user.uid)
                .set(userMap, com.google.firebase.firestore.SetOptions.merge())
        } catch (_: Exception) {
            // Profile sync is best effort; the user is already signed in
        }

        val app = application as RouteWiseApplication
        CoroutineScope(Dispatchers.IO).launch {
            app.userRepository.saveUser(
                UserEntity(
                    uid = user.uid,
                    email = email,
                    fullName = name,
                    phoneNumber = phone,
                    vehicleType = "",
                    licensePlate = "",
                    emergencyContact = "",
                    province = province,
                    city = city
                )
            )
        }

        sessionManager.saveUser(user.uid, email, name, phone, province, city)
        showSuccessSplash(name)
    }

    private fun authErrorMessage(e: Exception): String = when (e) {
        is FirebaseAuthUserCollisionException -> "An account with this email already exists (it may have been created with Google). Use Sign In or Sign in with Google instead."
        is FirebaseAuthWeakPasswordException -> "That password is too weak. Use at least ${InputValidator.MIN_PASSWORD_LENGTH} characters."
        is FirebaseAuthInvalidUserException -> "No account found for this email. Create an account first."
        is FirebaseAuthInvalidCredentialsException -> "Incorrect email or password."
        is FirebaseNetworkException -> "No internet connection. Check your network and try again."
        else -> "Something went wrong: ${e.localizedMessage ?: "unknown error"}"
    }

    private fun setBusy(busy: Boolean) {
        btnSubmitAuth.isEnabled = !busy
        btnGoogleSignIn.isEnabled = !busy
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()

    private fun setupTabs() {
        tabSignIn.setOnClickListener { switchToSignIn() }
        tabCreateAccount.setOnClickListener { switchToRegister() }
    }

    private fun switchToSignIn() {
        isRegistering = false
        tabSignIn.setBackgroundResource(R.drawable.bg_tab_item_selected)
        tabSignIn.setTextColor(Color.parseColor("#020617"))

        tabCreateAccount.background = null
        tabCreateAccount.setTextColor(Color.parseColor("#94a3b8"))

        groupFullName.visibility = View.GONE
        groupPhone.visibility = View.GONE
        groupLocation.visibility = View.GONE

        btnSubmitAuth.text = getString(R.string.sign_in_to_routewise)

        findViewById<View>(R.id.layoutSocialDivider).visibility = View.VISIBLE
        btnGoogleSignIn.visibility = View.VISIBLE
    }

    private fun switchToRegister() {
        isRegistering = true
        tabCreateAccount.setBackgroundResource(R.drawable.bg_tab_item_selected)
        tabCreateAccount.setTextColor(Color.parseColor("#020617"))

        tabSignIn.background = null
        tabSignIn.setTextColor(Color.parseColor("#94a3b8"))

        groupFullName.visibility = View.VISIBLE
        groupPhone.visibility = View.VISIBLE
        groupLocation.visibility = View.VISIBLE

        btnSubmitAuth.text = getString(R.string.complete_registration)

        findViewById<View>(R.id.layoutSocialDivider).visibility = View.VISIBLE
        btnGoogleSignIn.visibility = View.VISIBLE
    }

    private fun setupDropdowns() {
        val provinces = LocationData.SA_PROVINCES
        val provinceAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, provinces)
        spinnerProvince.setAdapter(provinceAdapter)

        spinnerProvince.setOnItemClickListener { _, _, position, _ ->
            val selectedProvince = provinces[position]
            updateCityDropdown(selectedProvince)
        }

        // Initial city dropdown
        updateCityDropdown("Gauteng")
    }

    private fun updateCityDropdown(province: String) {
        val cities = LocationData.SA_CITIES[province] ?: emptyList()
        val cityAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, cities)
        spinnerCity.setAdapter(cityAdapter)
        if (cities.isNotEmpty()) {
            spinnerCity.setText(cities[0], false)
        }
    }

    private fun showSuccessSplash(name: String) {
        // Hide form, show success
        formScroll.visibility = View.GONE
        scrollResetPassword.visibility = View.GONE
        findViewById<View>(R.id.tabSwitcher).visibility = View.GONE
        findViewById<View>(R.id.layoutSocialDivider).visibility = View.GONE
        btnGoogleSignIn.visibility = View.GONE
        btnSubmitAuth.visibility = View.GONE

        tvWelcomeUser.text = getString(R.string.auth_success_welcome, name.split(" ")[0])
        successSplash.visibility = View.VISIBLE

        Handler(Looper.getMainLooper()).postDelayed({
            launchMainActivity()
        }, 1500)
    }

    private fun launchMainActivity() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}
