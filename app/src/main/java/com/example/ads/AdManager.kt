package com.example.ads

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class RewardedAdRequest(
    val title: String,
    val rewardRewardType: RewardType,
    val onRewardEarned: (Boolean) -> Unit
)

enum class RewardType {
    EXTRA_LIFE,
    EXTRA_HINT
}

object AdManager {

    // Official Google AdMob Test Ad Unit IDs
    const val TEST_BANNER_ID = "ca-app-pub-3940256099942544/6300978111"
    const val TEST_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"
    const val TEST_REWARDED_ID = "ca-app-pub-3940256099942544/5224354917"

    private val _activeRewardedAd = MutableStateFlow<RewardedAdRequest?>(null)
    val activeRewardedAd: StateFlow<RewardedAdRequest?> = _activeRewardedAd.asStateFlow()

    private var lastInterstitialTime = 0L
    private const val INTERSTITIAL_COOLDOWN_MS = 60_000L // At most once every 60 seconds

    /**
     * Triggers a rewarded video ad modal with interactive countdown and reward callback.
     */
    fun showRewardedAd(title: String, rewardType: RewardType, onReward: (Boolean) -> Unit) {
        _activeRewardedAd.value = RewardedAdRequest(
            title = title,
            rewardRewardType = rewardType,
            onRewardEarned = onReward
        )
    }

    fun completeRewardedAd(earned: Boolean) {
        val current = _activeRewardedAd.value
        _activeRewardedAd.value = null
        current?.onRewardEarned?.invoke(earned)
    }

    /**
     * Checks if interstitial ad can be displayed between levels.
     * Never interrupts active gameplay.
     */
    fun shouldShowInterstitial(): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastInterstitialTime > INTERSTITIAL_COOLDOWN_MS) {
            lastInterstitialTime = now
            return true
        }
        return false
    }
}
