package rsv.squitv.data.pairing

import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class PairingSessionTest {

    @Test
    fun `session generates secure token and valid pairing code`() {
        val session1 = PairingSession.generate()
        val session2 = PairingSession.generate()

        assertNotNull(session1.sessionId)
        assertNotNull(session1.token)
        assertTrue(session1.token.length >= 32)
        assertTrue(session1.pairingCode.matches(Regex("\\d{4}-\\d{4}")))
        assertNotEquals(session1.token, session2.token)
        assertNotEquals(session1.pairingCode, session2.pairingCode)
    }

    @Test
    fun `session expiration works correctly`() {
        val pastTime = System.currentTimeMillis() - 1000L
        val expiredSession = PairingSession("1", "token", "1234-5678", pastTime)
        assertTrue(expiredSession.isExpired())
        assertFalse(expiredSession.tryConsume())

        val futureTime = System.currentTimeMillis() + 60000L
        val activeSession = PairingSession("2", "token", "1234-5678", futureTime)
        assertFalse(activeSession.isExpired())
        assertTrue(activeSession.tryConsume())
    }

    @Test
    fun `single use enforcement works atomically`() {
        val futureTime = System.currentTimeMillis() + 60000L
        val session = PairingSession("1", "token", "1234-5678", futureTime)

        assertTrue(session.tryConsume())
        assertFalse(session.tryConsume()) // Second attempt must return false
    }

    @Test
    fun `concurrent consumption allows only one winner`() {
        val futureTime = System.currentTimeMillis() + 60000L
        val session = PairingSession("1", "token", "1234-5678", futureTime)

        val threadCount = 20
        val executor = Executors.newFixedThreadPool(threadCount)
        val latch = CountDownLatch(1)
        val successCount = AtomicInteger(0)

        for (i in 0 until threadCount) {
            executor.submit {
                latch.await()
                if (session.tryConsume()) {
                    successCount.incrementAndGet()
                }
            }
        }

        latch.countDown()
        executor.shutdown()
        assertTrue(executor.awaitTermination(2, TimeUnit.SECONDS))

        assertEquals(1, successCount.get())
    }
}
