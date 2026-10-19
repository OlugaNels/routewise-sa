package com.routewise.sa.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.routewise.sa.databinding.FragmentChatbotBinding

class ChatbotFragment : Fragment() {

    private var _binding: FragmentChatbotBinding? = null
    private val binding get() = _binding!!
    
    private val adapter = ChatAdapter()
    private val messages = mutableListOf<ChatMessage>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatbotBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvChatMessages.layoutManager = LinearLayoutManager(requireContext())
        binding.rvChatMessages.adapter = adapter

        addMessage("ai", "Sawubona! I am your **RouteWise AI Road Assistant**. I can share road safety tips and load shedding advice. For live road reports, check the Alerts and Map tabs. How can I help you?")

        binding.btnSendMessage.setOnClickListener {
            val text = binding.etChatInput.text.toString()
            if (text.isNotBlank()) {
                addMessage("user", text)
                binding.etChatInput.text.clear()
                
                // Simple built-in answers (no live traffic data)
                binding.root.postDelayed({
                    val reply = when {
                        text.contains("robot", true) || text.contains("stop", true) -> 
                            "🇿🇦 **When a traffic robot is dead in SA:** Treat it as a **4-Way Stop**! The first to stop has right of way."
                        text.contains("n1", true) -> 
                            "I cannot look up live conditions yet. Open the Alerts tab and search for N1 to see reports from other drivers."
                        else -> "I can answer questions about road safety and load shedding. For live reports, use the Alerts and Map tabs. Drive safely!"
                    }
                    addMessage("ai", reply)
                }, 800)
            }
        }
    }

    private fun addMessage(sender: String, text: String) {
        messages.add(ChatMessage(System.currentTimeMillis().toString(), sender, text))
        adapter.submitList(messages.toList())
        binding.rvChatMessages.scrollToPosition(messages.size - 1)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
