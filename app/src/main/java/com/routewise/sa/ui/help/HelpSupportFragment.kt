package com.routewise.sa.ui.help

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.lifecycle.lifecycleScope
import com.routewise.sa.R
import com.routewise.sa.data.SessionManager
import com.routewise.sa.util.InputValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import com.routewise.sa.databinding.FragmentHelpSupportBinding
import com.routewise.sa.databinding.ItemFaqBinding

class HelpSupportFragment : Fragment() {

    companion object {
        private const val TICKET_URL = "https://routewise-sentinel.onrender.com/api/admin/app-reports"
    }

    private var _binding: FragmentHelpSupportBinding? = null
    private val helpBinding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHelpSupportBinding.inflate(inflater, container, false)
        return helpBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupFaqs()
        setupForm()
        
        helpBinding.btnQuickAi.setOnClickListener {
            findNavController().navigate(R.id.navigation_chat)
        }
    }

    private fun setupFaqs() {
        val faqs = listOf(
            "Why require my location?" to "RouteWise needs GPS for turn-by-turn guidance and real-time hazard alerts.",
            "Is my data shared?" to "No. Your logs and details remain private and encrypted on your device.",
            "Load shedding handling?" to "Dead robots are automatically flagged as 4-Way Stops with voice warnings.",
            "Earn Impact Points?" to "Get +10 pts for hazard reports and +2 when others verify your alerts."
        )

        faqs.forEach { (q, a) ->
            val faqBinding = ItemFaqBinding.inflate(layoutInflater, helpBinding.containerFaq, true)
            faqBinding.tvFaqQuestion.text = q
            faqBinding.tvFaqAnswer.text = a
            
            faqBinding.layoutFaqHeader.setOnClickListener {
                val isVisible = faqBinding.tvFaqAnswer.visibility == View.VISIBLE
                faqBinding.tvFaqAnswer.visibility = if (isVisible) View.GONE else View.VISIBLE
                faqBinding.ivFaqChevron.rotation = if (isVisible) 0f else 90f
            }
        }
    }

    private fun setupForm() {
        val categories = arrayOf("Map Inaccuracy", "Voice Guidance Issue", "App Bug", "Feature Request")
        helpBinding.spinnerFaqCategory.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, categories)

        helpBinding.btnSubmitTicket.setOnClickListener {
            val message = helpBinding.etSupportDesc.text.toString().trim()
            if (message.isEmpty()) {
                helpBinding.etSupportDesc.error = "Description required"
                return@setOnClickListener
            }
            if (!InputValidator.isDescriptionWithinLimit(message)) {
                helpBinding.etSupportDesc.error = "Keep it to ${InputValidator.MAX_DESCRIPTION_LENGTH} characters or fewer"
                return@setOnClickListener
            }
            val category = helpBinding.spinnerFaqCategory.selectedItem?.toString() ?: "App Bug"
            val type = if (category == "App Bug") "bug" else "help"
            val userId = SessionManager(requireContext()).getUserId().ifEmpty { "anonymous" }
            sendTicket(userId, type, "[$category] $message")
        }
    }

    /** Sends the ticket to the RouteWise API (the Sentinel website's /api/admin/app-reports route). */
    private fun sendTicket(userId: String, type: String, message: String) {
        helpBinding.btnSubmitTicket.isEnabled = false
        Toast.makeText(requireContext(), "Sending your ticket...", Toast.LENGTH_SHORT).show()
        val appContext = requireContext().applicationContext
        viewLifecycleOwner.lifecycleScope.launch {
            val ok = withContext(Dispatchers.IO) {
                try {
                    val conn = URL(TICKET_URL).openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.connectTimeout = 30_000
                    conn.readTimeout = 60_000 // the free server can take about a minute to wake up
                    conn.doOutput = true
                    conn.setRequestProperty("Content-Type", "application/json")
                    val body = JSONObject().put("user_id", userId).put("type", type).put("message", message).toString()
                    conn.outputStream.use { it.write(body.toByteArray()) }
                    val code = conn.responseCode
                    conn.disconnect()
                    code in 200..299
                } catch (_: Exception) {
                    false
                }
            }
            Toast.makeText(
                appContext,
                if (ok) "Support ticket sent. Thank you!" else "Could not send the ticket. Check your connection and try again.",
                Toast.LENGTH_LONG
            ).show()
            _binding?.let {
                it.btnSubmitTicket.isEnabled = true
                if (ok) it.etSupportDesc.text.clear()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
