package com.lib.automix.utils

/**
 * desc:
 **
 * user: xujj
 * time: 2024/3/28 17:46
 **/
class AutoMixConfigUtils {

    companion object {
        private var instance: AutoMixConfigUtils? = null

        private fun getInstance(): AutoMixConfigUtils {
            if (instance == null) {
                synchronized(AutoMixConfigUtils::class.java) {
                    instance = AutoMixConfigUtils()
                }
            }
            return instance!!
        }

        fun bindApp(config: AbstractAutoMixConfig) {
            getInstance().setAutoMixConfig(config)
        }

        fun getConfig(): AbstractAutoMixConfig? {
            return getInstance().getAutoMixConfig()
        }

        fun isAutoMix(): Boolean {
            return getConfig()?.isAutoMix ?: false
        }

        fun isAutoSync(): Boolean {
            return isAutoMix() && (getConfig()?.isAutoSync ?: true)
        }

        fun getCurrentDeck(): Boolean {
            return getConfig()?.currentDeck ?: true
        }

        fun updatePlayState(isDeckA: Boolean, state: Int, playStateChanged: Boolean) {
            getConfig()?.onAutoMixListener?.onPlayStateChanged(
                isDeckA, state, playStateChanged
            )
        }

        fun fadeVolume(isFading: Boolean) {
            getConfig()?.onAutoMixListener?.fadeVolume(isFading)
        }

        fun onNotifyPlay() {
            getConfig()?.onAutoMixListener?.onNotifyPlay()
        }
    }

    private var config: AbstractAutoMixConfig? = null

    fun setAutoMixConfig(config: AbstractAutoMixConfig) {
        this.config = config
    }

    fun getAutoMixConfig(): AbstractAutoMixConfig? {
        return config
    }
}