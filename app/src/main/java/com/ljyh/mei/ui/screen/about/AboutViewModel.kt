package com.ljyh.mei.ui.screen.about

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ljyh.mei.BuildConfig
import com.ljyh.mei.constants.BetaUpdatesEnabledKey
import com.ljyh.mei.di.repository.CachedLyricRepository
import com.ljyh.mei.di.repository.QQSongRepository
import com.ljyh.mei.utils.VersionUpdateChecker
import com.ljyh.mei.utils.VersionUpdateResult
import com.ljyh.mei.utils.dataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AboutViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val cachedLyricRepo: CachedLyricRepository,
    private val qqSongRepo: QQSongRepository
) : ViewModel() {

    var lastResult: String = ""
        private set

    private val _isCheckingUpdate = MutableStateFlow(false)
    val isCheckingUpdate: StateFlow<Boolean> = _isCheckingUpdate.asStateFlow()

    private val _isSavingBetaPreference = MutableStateFlow(false)
    val isSavingBetaPreference: StateFlow<Boolean> = _isSavingBetaPreference.asStateFlow()

    private val _updateResult = MutableStateFlow<VersionUpdateResult?>(null)
    val updateResult: StateFlow<VersionUpdateResult?> = _updateResult.asStateFlow()

    fun setBetaUpdatesEnabled(enabled: Boolean) {
        if (_isCheckingUpdate.value || _isSavingBetaPreference.value) return
        _isSavingBetaPreference.value = true
        viewModelScope.launch {
            try {
                appContext.dataStore.edit { preferences ->
                    preferences[BetaUpdatesEnabledKey] = enabled
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                _updateResult.value = VersionUpdateResult.Failed
            } finally {
                _isSavingBetaPreference.value = false
            }
        }
    }

    fun checkForUpdates() {
        if (_isCheckingUpdate.value || _isSavingBetaPreference.value) return
        _isCheckingUpdate.value = true
        viewModelScope.launch {
            try {
                val checkBetaUpdates = appContext.dataStore.data.first()[BetaUpdatesEnabledKey] ?: false
                _updateResult.value = VersionUpdateChecker.check(
                    BuildConfig.VERSION_NAME,
                    betaUpdatesEnabled = checkBetaUpdates,
                )
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                _updateResult.value = VersionUpdateResult.Failed
            } finally {
                _isCheckingUpdate.value = false
            }
        }
    }

    fun dismissUpdateResult() {
        _updateResult.value = null
    }

    fun deleteAllCachedLyrics() {
        viewModelScope.launch {
            try {
                cachedLyricRepo.deleteAll()
                lastResult = "AI歌词数据已清除"
            } catch (e: Exception) {
                lastResult = "清除失败: ${e.message}"
            }
        }
    }

    fun deleteAllQQSongs() {
        viewModelScope.launch {
            try {
                qqSongRepo.deleteAll()
                lastResult = "QQ音乐ID数据已清除"
            } catch (e: Exception) {
                lastResult = "清除失败: ${e.message}"
            }
        }
    }
}
