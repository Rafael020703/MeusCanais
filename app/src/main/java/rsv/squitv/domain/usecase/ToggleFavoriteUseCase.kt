package rsv.squitv.domain.usecase

import rsv.squitv.core.data.repository.UserRepository
import rsv.squitv.data.local.entities.PendingSyncEntity
import rsv.squitv.data.repository.FirebaseRepository
import rsv.squitv.domain.model.IptvItem
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

class ToggleFavoriteUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val firebaseRepository: FirebaseRepository
) {
    suspend operator fun invoke(item: IptvItem, isFav: Boolean, profileId: String?, providerHash: String?) {
        userRepository.updateFavorite(item, isFav)
        
        // Delta sync to Firebase
        if (profileId != null) {
            val success = if (isFav) {
                firebaseRepository.addFavorite(profileId, item, providerHash)
            } else {
                firebaseRepository.removeFavorite(profileId, item.id, providerHash)
            }
            
            if (!success && providerHash != null) {
                // Add to outbox for later retry
                val type = if (isFav) "FAVORITE_ADD" else "FAVORITE_REMOVE"
                val payload = if (isFav) Json.encodeToString(item) else ""
                userRepository.insertPendingSync(PendingSyncEntity(type, providerHash, item.id, payload))
            } else if (success && providerHash != null) {
                // Clean up any pending if now successful
                val type = if (isFav) "FAVORITE_ADD" else "FAVORITE_REMOVE"
                userRepository.deletePendingSync(type, providerHash, item.id)
                // Also delete the opposite if we just changed state
                val oppositeType = if (isFav) "FAVORITE_REMOVE" else "FAVORITE_ADD"
                userRepository.deletePendingSync(oppositeType, providerHash, item.id)
            }
        }
    }
}
