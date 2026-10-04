package rsv.squitv.domain.usecase

import rsv.squitv.core.domain.interactor.DnsManager
import rsv.squitv.data.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import timber.log.Timber
import javax.inject.Inject

/**
 * UseCase responsible for rotating the IPTV Base URL to the next available DNS
 * when a network connection failure occurs.
 */
class PerformDnsFailoverUseCase @Inject constructor(
    private val dnsManager: DnsManager,
    private val settingsRepository: SettingsRepository
) {
    /**
     * Executes the failover logic.
     * 
     * @return The new Base URL string, or null if credentials are not found.
     */
    suspend operator fun invoke(): String? {
        val settings = settingsRepository.settingsFlow.first()
        val creds = settings.credentials ?: return null
        
        val nextDns = dnsManager.getNextDns(creds.baseUrl)
        Timber.i("Performing DNS Failover: %s -> %s", creds.baseUrl, nextDns)
        
        // 1. Update Persistent Settings
        val updatedCreds = creds.copy(baseUrl = nextDns)
        settingsRepository.saveCredentials(updatedCreds)
        
        // 2. Propagate to Retrofit via DnsManager
        dnsManager.updateBaseUrl(nextDns)
        
        return nextDns
    }
}
