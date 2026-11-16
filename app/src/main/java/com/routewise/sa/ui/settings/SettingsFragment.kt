package com.routewise.sa.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.routewise.sa.R
import com.routewise.sa.data.SettingsManager
import com.routewise.sa.util.InputValidator

/**
 * The Settings screen. Every switch is saved on the phone by [SettingsManager] and read by the map,
 * the alerts feed and the notification code, so a change takes effect straight away.
 * Version 1.2.0 added the hazard alert switches and the Account & Security section
 * (edit profile shortcut and change password), in response to the Task 2 feedback.
 */
class SettingsFragment : Fragment() {

    private var btnUnitKm: TextView? = null
    private var btnUnitMph: TextView? = null
    private lateinit var settings: SettingsManager

    /** Android 13+ asks the user before an app may post notifications. */
    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            Log.i(TAG, "POST_NOTIFICATIONS permission granted = $granted")
            if (!granted) {
                Toast.makeText(
                    requireContext(),
                    "Notifications are blocked in Android settings, so alerts cannot be shown",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        settings = SettingsManager(requireContext())

        btnUnitKm = view.findViewById(R.id.btnUnitKm)
        btnUnitMph = view.findViewById(R.id.btnUnitMph)

        setupToggles(view)
        updateUnitSelection(settings.useKm)

        btnUnitKm?.setOnClickListener { chooseUnit(isKm = true) }
        btnUnitMph?.setOnClickListener { chooseUnit(isKm = false) }

        setupAccountSection(view)
        Log.d(TAG, "Settings screen opened")
    }

    private fun setupToggles(root: View) {
        data class Row(val id: Int, val key: String, val title: String, val desc: String)

        val rows = listOf(
            Row(R.id.settingLiveTraffic, SettingsManager.KEY_LIVE_TRAFFIC, "Live Hazard Layer", "Show hazards reported by other drivers on the map"),
            Row(R.id.settingNightTheme, SettingsManager.KEY_NIGHT_THEME, "Night Map Theme", "Use a dark map for night driving"),
            Row(R.id.settingTurnByTurn, SettingsManager.KEY_TURN_BY_TURN, "Turn-by-Turn Voice", "Speak directions while navigating"),
            Row(R.id.settingHazardNotifications, SettingsManager.KEY_HAZARD_NOTIFICATIONS, "Hazard Notifications", "Notify me when a driver reports a new hazard"),
            Row(R.id.settingAccidentWarnings, SettingsManager.KEY_ACCIDENT_WARNINGS, "Accident Alerts", "Show and notify me about accidents"),
            Row(R.id.settingPoliceAlerts, SettingsManager.KEY_POLICE_ALERTS, "Police Alerts", "Show and notify me about police traps"),
            Row(R.id.settingRoadworkAlerts, SettingsManager.KEY_ROADWORK_ALERTS, "Roadwork Alerts", "Show and notify me about roadworks and closures")
        )

        for (row in rows) {
            val item = root.findViewById<View>(row.id) ?: continue
            item.findViewById<TextView>(R.id.tvSettingTitle).text = row.title
            item.findViewById<TextView>(R.id.tvSettingDesc).text = row.desc
            val toggle = item.findViewById<MaterialSwitch>(R.id.switchSetting)
            toggle.setOnCheckedChangeListener(null)
            toggle.isChecked = settings.getToggle(row.key)
            toggle.setOnCheckedChangeListener { _, checked ->
                settings.setToggle(row.key, checked)
                Log.i(TAG, "Setting '${row.key}' changed to $checked")
                Toast.makeText(requireContext(), "${row.title} ${if (checked) "on" else "off"}", Toast.LENGTH_SHORT).show()
                if (checked && row.key == SettingsManager.KEY_HAZARD_NOTIFICATIONS) askForNotificationPermission()
            }
        }
    }

    private fun askForNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun chooseUnit(isKm: Boolean) {
        settings.useKm = isKm
        updateUnitSelection(isKm)
        Log.i(TAG, "Distance unit changed to ${if (isKm) "km" else "mi"}")
        Toast.makeText(requireContext(), if (isKm) "Units: km/h" else "Units: mph", Toast.LENGTH_SHORT).show()
    }

    private fun updateUnitSelection(isKm: Boolean) {
        btnUnitKm?.apply {
            setBackgroundResource(if (isKm) R.drawable.bg_card_selected else 0)
            setTextColor(if (isKm) 0xFF34D399.toInt() else 0xFF94A3B8.toInt())
        }

        btnUnitMph?.apply {
            setBackgroundResource(if (!isKm) R.drawable.bg_card_selected else 0)
            setTextColor(if (!isKm) 0xFF34D399.toInt() else 0xFF94A3B8.toInt())
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Account & Security (1.2.0)
    // ---------------------------------------------------------------------------------------------

    private fun setupAccountSection(root: View) {
        root.findViewById<Button>(R.id.btnEditProfile).setOnClickListener {
            findNavController().navigate(R.id.navigation_profile)
        }

        val user = FirebaseAuth.getInstance().currentUser
        // Accounts created with "Sign in with Google" have no password to change
        val hasPassword = user?.providerData?.any { it.providerId == EmailAuthProvider.PROVIDER_ID } == true
        root.findViewById<View>(R.id.layoutChangePassword).visibility = if (hasPassword) View.VISIBLE else View.GONE
        root.findViewById<View>(R.id.tvPasswordGoogleNote).visibility = if (hasPassword) View.GONE else View.VISIBLE

        root.findViewById<Button>(R.id.btnChangePassword).setOnClickListener { changePassword(root) }
    }

    /**
     * Changes the password in Firebase Authentication. Firebase requires a recent sign-in for this,
     * so the user's current password is checked first (re-authentication), then the new one is saved.
     * Reference: Google (n.d.) Manage users in Firebase. https://firebase.google.com/docs/auth/android/manage-users
     */
    private fun changePassword(root: View) {
        val etCurrent = root.findViewById<EditText>(R.id.etCurrentPassword)
        val etNew = root.findViewById<EditText>(R.id.etNewPassword)
        val etConfirm = root.findViewById<EditText>(R.id.etConfirmPassword)
        val button = root.findViewById<Button>(R.id.btnChangePassword)

        val current = etCurrent.text.toString()
        val new = etNew.text.toString()
        val confirm = etConfirm.text.toString()

        etCurrent.error = null
        etNew.error = null
        etConfirm.error = null

        val problem = InputValidator.changePasswordError(current, new, confirm)
        if (problem != null) {
            // Show the message under the field it belongs to
            val target = when {
                current.isEmpty() -> etCurrent
                confirm != new && InputValidator.passwordError(new) == null && new != current -> etConfirm
                else -> etNew
            }
            target.error = problem
            target.requestFocus()
            Log.w(TAG, "Change password rejected by validation")
            return
        }

        val user = FirebaseAuth.getInstance().currentUser
        val email = user?.email
        if (user == null || email.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Please sign in again to change your password", Toast.LENGTH_LONG).show()
            return
        }

        button.isEnabled = false
        Log.d(TAG, "Re-authenticating before password change")
        user.reauthenticate(EmailAuthProvider.getCredential(email, current))
            .addOnSuccessListener {
                user.updatePassword(new)
                    .addOnSuccessListener {
                        Log.i(TAG, "Password changed successfully")
                        if (!isAdded) return@addOnSuccessListener
                        button.isEnabled = true
                        etCurrent.text.clear()
                        etNew.text.clear()
                        etConfirm.text.clear()
                        Toast.makeText(requireContext(), "Password changed", Toast.LENGTH_LONG).show()
                    }
                    .addOnFailureListener { e -> onPasswordChangeFailed(e, button, etCurrent) }
            }
            .addOnFailureListener { e -> onPasswordChangeFailed(e, button, etCurrent) }
    }

    private fun onPasswordChangeFailed(e: Exception, button: Button, etCurrent: EditText) {
        Log.e(TAG, "Password change failed: ${e.javaClass.simpleName}")
        if (!isAdded) return
        button.isEnabled = true
        when (e) {
            is FirebaseAuthInvalidCredentialsException, is FirebaseAuthInvalidUserException -> {
                etCurrent.error = "Current password is incorrect"
                etCurrent.requestFocus()
            }
            is FirebaseNetworkException ->
                Toast.makeText(requireContext(), "No internet connection. Check your network and try again.", Toast.LENGTH_LONG).show()
            else ->
                Toast.makeText(requireContext(), "Could not change the password. Please try again.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        btnUnitKm = null
        btnUnitMph = null
    }

    private companion object {
        const val TAG = "SettingsFragment"
    }
}
