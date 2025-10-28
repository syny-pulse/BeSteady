package com.besteady.ui.drill

import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.besteady.databinding.FragmentStartDrillBinding
import com.besteady.R
import com.besteady.bluetooth.BluetoothClassicViewModel
import com.besteady.bluetooth.FireEventType
import kotlinx.coroutines.launch
import com.besteady.ui.widgets.FireRiskGauge
import android.animation.ValueAnimator
import android.view.animation.LinearInterpolator

class StartDrillFragment : Fragment() {

    private val TAG = "StartDrillFragment"
    
    private var _binding: FragmentStartDrillBinding? = null
    private val binding get() = _binding!!

    private lateinit var bluetoothViewModel: BluetoothClassicViewModel
    
    private var drillTimer: CountDownTimer? = null
    private var drillStartTime: Long = 0L
    private var emergencyCallTime: Long = 0L
    private var policeArrivalTime: Long = 0L
    private var evacuationTime: Long = 0L

    private var isDrillActive = false

    // Gauge state
    private var gauge: FireRiskGauge? = null
    private var currentGaugeAngle: Float = 0f
    private val minDangerAngleWhileActive: Float = 1f // never fully return to 0 until stop

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStartDrillBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get Bluetooth ViewModel
        bluetoothViewModel = ViewModelProvider(requireActivity())[BluetoothClassicViewModel::class.java]
        
        // Gauge reference
        gauge = binding.root.findViewById(R.id.fireRiskGauge)

        setupClickListeners()
        observeFireEvents()
        updateUI()

        // Initial state
        updateGauge(0f, updateStatus = true, animate = false)
    }
    
    /**
     * Observe fire events from Bluetooth ESP32
     */
    private fun observeFireEvents() {
        lifecycleScope.launch {
            bluetoothViewModel.latestFireEvent.collect { fireEvent ->
                fireEvent?.let { event ->
                    when (event.type) {
                        FireEventType.FIRE_DETECTED -> {
                            if (!isDrillActive) {
                                Log.d(TAG, "🔥 Fire detected! Auto-starting drill...")
                                startDrill()
                                // Show visual notification
                                showFireAlert()
                            }
                        }
                        FireEventType.FIRE_CLEARED -> {
                            Log.d(TAG, "✅ Fire cleared!")
                            if (isDrillActive) {
                                // Optional: stop the drill automatically when fire is cleared
                                // For now, we let the user stop manually
                            }
                        }
                        FireEventType.PING -> {
                            Log.d(TAG, "📡 ESP32 connection healthy")
                        }
                        FireEventType.UNKNOWN -> {
                            Log.d(TAG, "Unknown message: ${event.message}")
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Show visual alert when fire is detected
     */
    private fun showFireAlert() {
        // Update status text to show fire alert
        binding.tvDrillStatus.text = "🔥 FIRE DETECTED - DRILL AUTO-STARTED"
        binding.tvDrillStatus.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
    }

    private fun setupClickListeners() {
        binding.btnStartDrill.setOnClickListener {
            startDrill()
        }

        binding.btnStopDrill.setOnClickListener {
            stopDrill()
        }

        binding.btnEmergencyCall.setOnClickListener {
            recordEmergencyCall()
        }

        binding.btnPoliceArrival.setOnClickListener {
            recordPoliceArrival()
        }

        binding.btnCompleteEvacuation.setOnClickListener {
            recordCompleteEvacuation()
        }
    }

    private fun startDrill() {
        if (isDrillActive) {
            Log.d(TAG, "Drill already active, skipping start")
            return
        }
        
        isDrillActive = true
        drillStartTime = System.currentTimeMillis()

        // Send command to ESP32 to turn off buzzer (optional)
        if (bluetoothViewModel.isConnected()) {
            bluetoothViewModel.sendMessage("STOP_ALARM\n")
        }

        startTimer()
        updateUI()

        // Animate gauge from 0 -> 180 over 25s
        binding.tvFireRiskStatus.text = "Status: Fire Detected – Danger Rising"
        gauge?.startFireProgress(25_000L)
        currentGaugeAngle = 180f

        // Simulate user responses (in real app, this would come from backend)
        simulateUserResponses()
        
        Log.d(TAG, "Drill started at ${System.currentTimeMillis()}")
    }

    private fun stopDrill() {
        // Navigate to stop drill dialog/form
        findNavController().navigate(resId = R.id.stopDrillDialog)
    }

    private fun startTimer() {
        drillTimer = object : CountDownTimer(Long.MAX_VALUE, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val elapsedTime = System.currentTimeMillis() - drillStartTime
                updateTimerDisplay(elapsedTime)
            }

            override fun onFinish() {
                // Timer finished
            }
        }.start()
    }

    private fun updateTimerDisplay(elapsedTime: Long) {
        val seconds = (elapsedTime / 1000) % 60
        val minutes = (elapsedTime / (1000 * 60)) % 60
        val hours = (elapsedTime / (1000 * 60 * 60))

        val timeString = String.format("%02d:%02d:%02d", hours, minutes, seconds)
        binding.tvTimer.text = timeString
    }

    private fun recordEmergencyCall() {
        emergencyCallTime = System.currentTimeMillis() - drillStartTime
        binding.btnEmergencyCall.isEnabled = false
        binding.btnEmergencyCall.text = "Called\n${formatTime(emergencyCallTime)}"

        // Move needle back 30° toward safe, but not below minimum while active
        nudgeGaugeTowardSafe(30f, "Status: Responders Notified")
    }

    private fun recordPoliceArrival() {
        policeArrivalTime = System.currentTimeMillis() - drillStartTime
        binding.btnPoliceArrival.isEnabled = false
        binding.btnPoliceArrival.text = "Arrived\n${formatTime(policeArrivalTime)}"

        // Move needle back 45° toward safe
        nudgeGaugeTowardSafe(45f, "Status: Responders On Scene")
    }

    private fun recordCompleteEvacuation() {
        evacuationTime = System.currentTimeMillis() - drillStartTime
        binding.btnCompleteEvacuation.isEnabled = false
        binding.btnCompleteEvacuation.text = "Evacuated\n${formatTime(evacuationTime)}"

        // Move needle back 60° toward safe
        nudgeGaugeTowardSafe(60f, "Status: Evacuation Underway")
    }

    private fun formatTime(milliseconds: Long): String {
        val seconds = milliseconds / 1000
        return String.format("%02d:%02d", seconds / 60, seconds % 60)
    }

    private fun updateUI() {
        if (isDrillActive) {
            binding.tvDrillStatus.text = "DRILL IN PROGRESS"
            binding.tvDrillStatus.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
            binding.btnStartDrill.visibility = View.GONE
            binding.btnStopDrill.visibility = View.VISIBLE

            // Enable timer buttons
            binding.btnEmergencyCall.isEnabled = true
            binding.btnPoliceArrival.isEnabled = true
            binding.btnCompleteEvacuation.isEnabled = true
        } else {
            binding.tvDrillStatus.text = "No active drill"
            binding.tvDrillStatus.setTextColor(resources.getColor(android.R.color.darker_gray, null))
            binding.btnStartDrill.visibility = View.VISIBLE
            binding.btnStopDrill.visibility = View.GONE
            binding.tvTimer.text = "00:00:00"

            // Reset timer buttons
            binding.btnEmergencyCall.isEnabled = false
            binding.btnPoliceArrival.isEnabled = false
            binding.btnCompleteEvacuation.isEnabled = false
            binding.btnEmergencyCall.text = "Emergency\nCall"
            binding.btnPoliceArrival.text = "Police\nArrival"
            binding.btnCompleteEvacuation.text = "Complete\nEvacuation"

            // Reset gauge and label
            updateGauge(0f, updateStatus = true, animate = true)
            binding.tvFireRiskStatus.text = "Status: Safe – Drill Completed"
        }
    }

    private fun nudgeGaugeTowardSafe(deltaDegrees: Float, statusText: String) {
        if (!isDrillActive) return
        val target = (currentGaugeAngle - deltaDegrees).coerceAtLeast(minDangerAngleWhileActive)
        updateGauge(target, updateStatus = true, animate = true)
        binding.tvFireRiskStatus.text = statusText
    }

    private fun updateGauge(targetAngle: Float, updateStatus: Boolean, animate: Boolean) {
        currentGaugeAngle = targetAngle.coerceIn(0f, 180f)
        val g = gauge ?: return
        if (animate) {
            g.animateToAngle(currentGaugeAngle, 600L)
        } else {
            g.currentRiskAngle = currentGaugeAngle
        }

        if (updateStatus) {
            val status = when {
                currentGaugeAngle < 60f -> "Status: Safe"
                currentGaugeAngle < 120f -> "Status: Moderate Risk"
                else -> "Status: Danger"
            }
            if (binding.tvFireRiskStatus.text.isNullOrEmpty()) {
                binding.tvFireRiskStatus.text = status
            }
        }
    }

    private fun simulateUserResponses() {
        // Simulate some users responding as safe
        binding.tvSafeCount.text = "12"
        binding.tvNotRespondedCount.text = "8"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        drillTimer?.cancel()
        _binding = null
    }

    fun resetDrill() {
        isDrillActive = false
        drillTimer?.cancel()
        emergencyCallTime = 0L
        policeArrivalTime = 0L
        evacuationTime = 0L
        updateUI()
    }
    
    /**
     * Public method to stop drill manually (called by StopDrillDialog)
     */
    fun stopDrillManually() {
        isDrillActive = false
        drillTimer?.cancel()
        updateUI()
        Log.d(TAG, "Drill stopped manually")
    }
}