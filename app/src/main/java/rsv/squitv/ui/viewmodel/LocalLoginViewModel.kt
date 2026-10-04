package rsv.squitv.ui.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import rsv.squitv.data.pairing.PairingRepository
import rsv.squitv.data.pairing.PairingSession
import rsv.squitv.data.pairing.PairingState
import javax.inject.Inject

@HiltViewModel
class LocalLoginViewModel @Inject constructor(
    private val pairingRepository: PairingRepository
) : ViewModel() {

    val currentSession: StateFlow<PairingSession?> = pairingRepository.currentSession
    val serverStatus: StateFlow<PairingState> = pairingRepository.serverStatus
    val serverPort: StateFlow<Int> = pairingRepository.serverPort
    val localIp: StateFlow<String> = pairingRepository.localIp

    init {
        startPairing()
    }

    fun startPairing() {
        pairingRepository.startPairingSession()
    }

    fun cancelPairing() {
        pairingRepository.cancelPairing()
    }

    fun handleDeepLink(token: String): Boolean {
        return pairingRepository.handleDeepLinkToken(token)
    }

    override fun onCleared() {
        super.onCleared()
        pairingRepository.stopPairingSession()
    }
}
