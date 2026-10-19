package com.routewise.sa.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.materialswitch.MaterialSwitch
import com.routewise.sa.R
import com.routewise.sa.data.SettingsManager

class SettingsFragment : Fragment() {

    private var btnUnitKm: TextView? = null
    private var btnUnitMph: TextView? = null
    private lateinit var settings: SettingsManager

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
    }

    private fun setupToggles(root: View) {
        data class Row(val id: Int, val key: String, val title: String, val desc: String)

        val rows = listOf(
            Row(R.id.settingLiveTraffic, SettingsManager.KEY_LIVE_TRAFFIC, "Live Traffic Layer", "Show congestion and incidents on the map"),
            Row(R.id.settingNightTheme, SettingsManager.KEY_NIGHT_THEME, "Night Map Theme", "Use a dark map for night driving"),
            Row(R.id.settingSpeedWarnings, SettingsManager.KEY_SPEED_WARNINGS, "Speed Warnings", "Warn me when I go over the limit"),
            Row(R.id.settingTurnByTurn, SettingsManager.KEY_TURN_BY_TURN, "Turn-by-Turn Voice", "Speak directions while navigating"),
            Row(R.id.settingAccidentWarnings, SettingsManager.KEY_ACCIDENT_WARNINGS, "Accident Warnings", "Voice alert for accidents ahead"),
            Row(R.id.settingPoliceAlerts, SettingsManager.KEY_POLICE_ALERTS, "Police Alerts", "Voice alert for speed traps ahead"),
            Row(R.id.settingRoadworkAlerts, SettingsManager.KEY_ROADWORK_ALERTS, "Roadwork Alerts", "Voice alert for roadworks ahead")
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
                Toast.makeText(requireContext(), "${row.title} ${if (checked) "on" else "off"}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun chooseUnit(isKm: Boolean) {
        settings.useKm = isKm
        updateUnitSelection(isKm)
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

    override fun onDestroyView() {
        super.onDestroyView()
        btnUnitKm = null
        btnUnitMph = null
    }
}
