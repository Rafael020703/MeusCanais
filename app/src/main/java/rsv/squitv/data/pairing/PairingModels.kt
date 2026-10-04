package rsv.squitv.data.pairing

import java.security.SecureRandom
import java.util.concurrent.atomic.AtomicBoolean

enum class PairingState {
    WAITING,
    CONNECTED,
    AUTHENTICATING,
    SUCCESS,
    ERROR,
    EXPIRED,
    CANCELLED
}

data class PairingSession(
    val sessionId: String,
    val token: String,
    val pairingCode: String,
    val expirationTime: Long,
    val isUsed: AtomicBoolean = AtomicBoolean(false)
) {
    fun isExpired(now: Long = System.currentTimeMillis()): Boolean {
        return now > expirationTime
    }

    fun tryConsume(): Boolean {
        if (isExpired()) return false
        return isUsed.compareAndSet(false, true)
    }

    companion object {
        private val secureRandom = SecureRandom()

        fun generate(): PairingSession {
            val sessionId = generateRandomHex(16)
            val token = generateRandomHex(32)
            val pairingCode = generatePairingCode()
            val expirationTime = System.currentTimeMillis() + 5 * 60 * 1000L // 5 minutes
            return PairingSession(sessionId, token, pairingCode, expirationTime)
        }

        private fun generateRandomHex(byteCount: Int): String {
            val bytes = ByteArray(byteCount)
            secureRandom.nextBytes(bytes)
            return bytes.joinToString("") { "%02x".format(it) }
        }

        private fun generatePairingCode(): String {
            val part1 = (1000 + secureRandom.nextInt(9000))
            val part2 = (1000 + secureRandom.nextInt(9000))
            return "$part1-$part2"
        }
    }
}
