package com.routewise.sa.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.routewise.sa.data.SessionManager
import com.routewise.sa.databinding.FragmentProfileBinding
import com.routewise.sa.ui.auth.LoginActivity
import com.routewise.sa.util.InputValidator
import java.util.Locale

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val profileBinding get() = _binding!!
    private lateinit var sessionManager: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return profileBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = SessionManager(requireContext())

        // The demo rank and statistics were placeholders, so they are not shown
        profileBinding.layoutRankRow.visibility = View.GONE
        profileBinding.layoutMetrics.visibility = View.GONE

        setupUserData()

        profileBinding.btnSaveProfile.setOnClickListener { saveProfile() }

        profileBinding.btnLogout.setOnClickListener {
            sessionManager.logout()
            FirebaseAuth.getInstance().signOut()
            Toast.makeText(requireContext(), "Logged out of RouteWise SA", Toast.LENGTH_SHORT).show()

            val intent = Intent(requireContext(), LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            requireActivity().finish()
        }
    }

    private fun saveProfile() {
        val name = profileBinding.etFullName.text.toString().trim()
        val phone = profileBinding.etPhone.text.toString().trim()
        val vehicle = profileBinding.etVehicle.text.toString().trim()
        val plate = profileBinding.etPlate.text.toString().trim()

        profileBinding.etFullName.error = null
        profileBinding.etPhone.error = null
        if (!InputValidator.isValidName(name)) {
            profileBinding.etFullName.error = "Enter your full name (2 to ${InputValidator.MAX_NAME_LENGTH} characters)"
            profileBinding.etFullName.requestFocus()
            return
        }
        if (!InputValidator.isValidOptionalPhone(phone)) {
            profileBinding.etPhone.error = "Enter a valid phone number, e.g. +27 82 456 7890"
            profileBinding.etPhone.requestFocus()
            return
        }
        if (vehicle.length > 60 || plate.length > 15) {
            Toast.makeText(requireContext(), "Vehicle or number plate is too long", Toast.LENGTH_SHORT).show()
            return
        }

        sessionManager.saveProfile(name, phone, vehicle, plate)
        profileBinding.tvProfileName.text = name
        profileBinding.tvProfileInitial.text = name.take(1).uppercase(Locale.getDefault())

        // Best effort: keep the Firestore profile in step with the phone
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            try {
                FirebaseFirestore.getInstance().collection("users").document(uid).set(
                    hashMapOf(
                        "fullName" to name,
                        "phoneNumber" to phone,
                        "vehicleType" to vehicle,
                        "licensePlate" to plate
                    ),
                    SetOptions.merge()
                )
            } catch (_: Exception) {
                // Saved on the phone already
            }
        }
        Toast.makeText(requireContext(), "Profile saved", Toast.LENGTH_SHORT).show()
    }

    private fun setupUserData() {
        val userName = sessionManager.getUserName()
        profileBinding.tvProfileName.text = userName
        profileBinding.tvProfileEmail.text = sessionManager.getUserEmail()
        profileBinding.etFullName.setText(userName)
        profileBinding.etPhone.setText(sessionManager.getUserPhone())
        profileBinding.etVehicle.setText(sessionManager.getVehicle())
        profileBinding.etPlate.setText(sessionManager.getPlate())

        profileBinding.tvProfileInitial.text =
            if (userName.isNotEmpty()) userName.take(1).uppercase(Locale.getDefault()) else "?"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
