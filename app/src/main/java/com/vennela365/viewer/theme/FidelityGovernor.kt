package com.vennela365.viewer.theme

import android.content.ComponentCallbacks2
import android.content.Context
import android.os.PowerManager
import androidx.annotation.FloatRange
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Three-tier hardware fidelity classification based on calculated Visual Fidelity Index (Φ).
 */
enum class FidelityTier(
    val label: String,
    val description: String,
    val maxFps: Int,
    val blurPasses: Int,
    val enablesChromaticDispersion: Boolean,
    val enablesSpecularHighlights: Boolean
) {
    TIER_1_ULTRA(
        label = "Ultra Glass",
        description = "Full chromatic dispersion, specular highlights, 4-pass Dual-Kawase @ 120Hz",
        maxFps = 120,
        blurPasses = 4,
        enablesChromaticDispersion = true,
        enablesSpecularHighlights = true
    ),
    TIER_2_BALANCED(
        label = "Balanced Glass",
        description = "Locked static light vector, 2-pass downsample blur, analytical bevels @ 60Hz",
        maxFps = 60,
        blurPasses = 2,
        enablesChromaticDispersion = false,
        enablesSpecularHighlights = true
    ),
    TIER_3_PERFORMANCE(
        label = "Performance Glass",
        description = "Zero-copy pre-baked lookup, zero per-frame blur passes (<0.4ms GPU)",
        maxFps = 60,
        blurPasses = 0,
        enablesChromaticDispersion = false,
        enablesSpecularHighlights = false
    );

    companion object {
        fun fromPhi(phi: Float): FidelityTier = when {
            phi >= 0.75f -> TIER_1_ULTRA
            phi >= 0.45f -> TIER_2_BALANCED
            else -> TIER_3_PERFORMANCE
        }
    }
}

/** User preference mode for fidelity. */
enum class FidelityPreference(val id: String, val label: String) {
    AUTO("auto", "Automatic (Adaptive)"),
    FORCE_ULTRA("ultra", "Force Ultra (120 FPS Glass)"),
    FORCE_PERFORMANCE("perf", "Force Low-Power (<0.4ms)")
}

/**
 * Continuous Hardware Fidelity Scaler (Φ).
 *
 * Evaluates real-time device resource constraints before every VSync cycle:
 *   Φ = clamp( (T_target / T_ema) * (1 - M_used / M_limit)^α * (1 - Θ_thermal), 0.0, 1.0 )
 */
object FidelityGovernor {

    private val _fidelityIndex = MutableStateFlow(1.0f)
    val fidelityIndex: StateFlow<Float> = _fidelityIndex.asStateFlow()

    private val _activeTier = MutableStateFlow(FidelityTier.TIER_1_ULTRA)
    val activeTier: StateFlow<FidelityTier> = _activeTier.asStateFlow()

    private val _preference = MutableStateFlow(FidelityPreference.AUTO)
    val preference: StateFlow<FidelityPreference> = _preference.asStateFlow()

    private var isLowMemoryTriggered = false
    private var isPowerSaveActive = false

    /**
     * Mathematical pure calculation for continuous fidelity index Φ.
     *
     * @param targetFrameTimeMs Ideal frame time (e.g. 8.33ms for 120Hz, 16.66ms for 60Hz)
     * @param emaFrameTimeMs Exponential moving average of measured frame rendering times
     * @param usedMemoryBytes Active JVM heap memory currently allocated
     * @param limitMemoryBytes Maximum allowable JVM heap limit (Runtime.getRuntime().maxMemory())
     * @param thermalThrottling Normalized thermal throttling penalty in [0.0, 1.0]
     * @param alpha Sensitivity exponent for memory pressure (default 1.25)
     */
    fun computePhi(
        targetFrameTimeMs: Float,
        emaFrameTimeMs: Float,
        usedMemoryBytes: Long,
        limitMemoryBytes: Long,
        thermalThrottling: Float = 0.0f,
        alpha: Float = 1.25f
    ): Float {
        val safeEma = max(emaFrameTimeMs, 1.0f)
        val frameRatio = min(targetFrameTimeMs / safeEma, 1.0f)

        val memRatio = if (limitMemoryBytes > 0) {
            val ratio = usedMemoryBytes.toDouble() / limitMemoryBytes.toDouble()
            max(0.0, min(1.0, 1.0 - ratio)).toFloat()
        } else {
            1.0f
        }

        val memTerm = memRatio.toDouble().pow(alpha.toDouble()).toFloat()
        val thermalTerm = max(0.0f, min(1.0f, 1.0f - thermalThrottling))

        val rawPhi = frameRatio * memTerm * thermalTerm
        return max(0.0f, min(1.0f, rawPhi))
    }

    /**
     * Updates governor state from runtime hardware measurements.
     */
    fun updateMetrics(
        context: Context? = null,
        measuredFrameTimeMs: Float = 8.33f,
        targetFrameTimeMs: Float = 8.33f
    ) {
        val pref = _preference.value
        if (pref == FidelityPreference.FORCE_ULTRA) {
            _fidelityIndex.value = 1.0f
            _activeTier.value = FidelityTier.TIER_1_ULTRA
            return
        }
        if (pref == FidelityPreference.FORCE_PERFORMANCE) {
            _fidelityIndex.value = 0.2f
            _activeTier.value = FidelityTier.TIER_3_PERFORMANCE
            return
        }

        val runtime = Runtime.getRuntime()
        val usedMem = runtime.totalMemory() - runtime.freeMemory()
        val maxMem = runtime.maxMemory()

        var thermalPenalty = 0.0f
        if (isPowerSaveActive) thermalPenalty += 0.35f
        if (isLowMemoryTriggered) thermalPenalty += 0.45f

        val calculatedPhi = computePhi(
            targetFrameTimeMs = targetFrameTimeMs,
            emaFrameTimeMs = measuredFrameTimeMs,
            usedMemoryBytes = usedMem,
            limitMemoryBytes = maxMem,
            thermalThrottling = thermalPenalty
        )

        _fidelityIndex.value = calculatedPhi
        _activeTier.value = FidelityTier.fromPhi(calculatedPhi)
    }

    /** Called from Application or ComponentCallbacks2 when OS memory pressure changes. */
    fun onTrimMemory(level: Int) {
        isLowMemoryTriggered = when (level) {
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL,
            ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> true
            else -> false
        }
        updateMetrics()
    }

    fun setPreference(pref: FidelityPreference) {
        _preference.value = pref
        updateMetrics()
    }

    fun setPowerSave(active: Boolean) {
        isPowerSaveActive = active
        updateMetrics()
    }
}

