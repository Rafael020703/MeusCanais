package rsv.squitv.data.pairing

import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import java.net.BindException
import java.net.Socket

class LocalPairingServerTest {

    private var server: LocalPairingServer? = null

    @After
    fun tearDown() {
        server?.stop()
        server = null
    }

    @Test
    fun `start uses default port 1234 and opens bound socket`() = runTest {
        server = LocalPairingServer(
            port = LocalPairingServer.DEFAULT_PAIRING_PORT,
            onLoginSubmitted = { _, _, _ -> LocalPairingServer.LoginResult.Success },
            getSessionStatus = { null }
        )

        val port = server!!.start()

        assertEquals(1234, port)
        assertEquals(1234, server!!.actualPort)
        assertTrue(server!!.isBound)

        val socket = Socket("127.0.0.1", port)
        assertTrue(socket.isConnected)
        socket.close()
    }

    @Test(expected = BindException::class)
    fun `start throws BindException when port 1234 is occupied`() = runTest {
        val server1 = LocalPairingServer(
            port = LocalPairingServer.DEFAULT_PAIRING_PORT,
            onLoginSubmitted = { _, _, _ -> LocalPairingServer.LoginResult.Success },
            getSessionStatus = { null }
        )
        server1.start()

        val server2 = LocalPairingServer(
            port = LocalPairingServer.DEFAULT_PAIRING_PORT,
            onLoginSubmitted = { _, _, _ -> LocalPairingServer.LoginResult.Success },
            getSessionStatus = { null }
        )
        try {
            server2.start()
        } finally {
            server1.stop()
        }
    }

    @Test
    fun `stop closes socket and resets state`() = runTest {
        server = LocalPairingServer(
            port = LocalPairingServer.DEFAULT_PAIRING_PORT,
            onLoginSubmitted = { _, _, _ -> LocalPairingServer.LoginResult.Success },
            getSessionStatus = { null }
        )

        val port = server!!.start()
        assertEquals(1234, port)
        assertTrue(server!!.isBound)

        server!!.stop()

        assertFalse(server!!.isBound)
        assertEquals(0, server!!.actualPort)
    }

    @Test
    fun `restart server reuses port 1234 successfully`() = runTest {
        server = LocalPairingServer(
            port = LocalPairingServer.DEFAULT_PAIRING_PORT,
            onLoginSubmitted = { _, _, _ -> LocalPairingServer.LoginResult.Success },
            getSessionStatus = { null }
        )

        val port1 = server!!.start()
        assertEquals(1234, port1)

        server!!.stop()
        assertFalse(server!!.isBound)

        val port2 = server!!.start()
        assertEquals(1234, port2)
        assertTrue(server!!.isBound)
    }
}
