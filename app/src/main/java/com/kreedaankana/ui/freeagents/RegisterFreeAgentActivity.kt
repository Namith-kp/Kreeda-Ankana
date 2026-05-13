package com.kreedaankana.ui.freeagents

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.kreedaankana.data.model.FreeAgent
import com.kreedaankana.data.repository.FreeAgentRepository
import com.kreedaankana.databinding.ActivityRegisterFreeAgentBinding
import kotlinx.coroutines.launch

class RegisterFreeAgentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterFreeAgentBinding
    private val repository = FreeAgentRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterFreeAgentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        setupDropdowns()

        binding.btnRegister.setOnClickListener {
            registerAgent()
        }
    }

    private fun setupDropdowns() {
        val sports = arrayOf("Cricket", "Football", "Badminton", "Basketball", "Tennis", "Volleyball")
        val sportAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, sports)
        binding.tvSport.setAdapter(sportAdapter)

        val skills = arrayOf("Beginner", "Intermediate", "Advanced", "Professional")
        val skillAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, skills)
        binding.tvSkillLevel.setAdapter(skillAdapter)
    }

    private fun registerAgent() {
        val name = binding.etPlayerName.text.toString().trim()
        val sport = binding.tvSport.text.toString().trim()
        val role = binding.etRole.text.toString().trim()
        val skill = binding.tvSkillLevel.text.toString().trim()
        val availability = binding.etAvailability.text.toString().trim()
        val location = binding.etLocation.text.toString().trim()
        val contactInfo = binding.etContactInfo.text.toString().trim()

        if (name.isEmpty() || sport.isEmpty() || role.isEmpty() || skill.isEmpty() || availability.isEmpty() || location.isEmpty() || contactInfo.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val agent = FreeAgent(
            playerName = name,
            sport = sport,
            role = role,
            skillLevel = skill,
            availability = availability,
            location = location,
            contactInfo = contactInfo
        )

        binding.progressBar.visibility = android.view.View.VISIBLE
        binding.btnRegister.isEnabled = false

        lifecycleScope.launch {
            repository.registerAsFreeAgent(agent).onSuccess {
                binding.progressBar.visibility = android.view.View.GONE
                Toast.makeText(this@RegisterFreeAgentActivity, "Registered successfully!", Toast.LENGTH_SHORT).show()
                finish()
            }.onFailure {
                binding.progressBar.visibility = android.view.View.GONE
                binding.btnRegister.isEnabled = true
                Toast.makeText(this@RegisterFreeAgentActivity, "Registration failed", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
