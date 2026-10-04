package rsv.squitv.data.pairing

import kotlinx.coroutines.*
import timber.log.Timber
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean

class LocalPairingServer(
    private val port: Int = DEFAULT_PAIRING_PORT,
    private val onLoginSubmitted: suspend (username: String, password: String, token: String) -> LoginResult,
    private val getSessionStatus: () -> PairingSession?
) {
    private var serverSocket: ServerSocket? = null
    private val isRunning = AtomicBoolean(false)
    private var serverJob: Job? = null
    private val serverScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    var actualPort: Int = 0
        private set

    val isBound: Boolean
        get() = serverSocket?.isBound == true && serverSocket?.isClosed == false

    sealed interface LoginResult {
        data object Success : LoginResult
        data class Error(val message: String) : LoginResult
    }

    suspend fun start(): Int = withContext(Dispatchers.IO) {
        if (isRunning.get()) return@withContext actualPort

        val socket = ServerSocket(port).apply {
            reuseAddress = true
        }

        require(socket.isBound && !socket.isClosed && socket.localPort == port) {
            "ServerSocket failed to bind properly on port $port"
        }

        serverSocket = socket
        actualPort = socket.localPort
        isRunning.set(true)
        Timber.i("LocalPairingServer iniciado na porta %d", actualPort)

        serverJob = serverScope.launch(Dispatchers.IO) {
            while (isRunning.get() && serverSocket?.isClosed == false) {
                try {
                    val clientSocket = serverSocket?.accept() ?: break
                    launch(Dispatchers.IO) {
                        handleClient(clientSocket)
                    }
                } catch (e: Exception) {
                    if (isRunning.get()) {
                        Timber.w(e, "Erro ao aceitar conexão no servidor local")
                    }
                }
            }
        }

        actualPort
    }

    fun stop() {
        if (!isRunning.getAndSet(false)) return
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null
        serverJob?.cancel()
        actualPort = 0
        Timber.i("LocalPairingServer parado.")
    }

    private fun handleClient(socket: Socket) {
        socket.use { client ->
            client.soTimeout = 5000 // 5s timeout
            val input = BufferedReader(InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8))
            val output = PrintWriter(client.getOutputStream(), true)

            try {
                val requestLine = input.readLine() ?: return
                val parts = requestLine.split(" ")
                if (parts.size < 2) {
                    sendErrorResponse(output, 400, "Bad Request")
                    return
                }

                val method = parts[0].uppercase()
                val fullPath = parts[1]
                val path = fullPath.substringBefore("?")

                // Read headers with limit
                var contentLength = 0
                var line: String?
                var headerLinesRead = 0
                while (input.readLine().also { line = it } != null && line!!.isNotEmpty()) {
                    headerLinesRead++
                    if (headerLinesRead > 50) break // Header limit
                    if (line!!.startsWith("Content-Length:", ignoreCase = true)) {
                        contentLength = line!!.substringAfter(":").trim().toIntOrNull() ?: 0
                    }
                }

                when (method) {
                    "GET" -> {
                        when (path) {
                            "/" -> {
                                val session = getSessionStatus()
                                val token = session?.token ?: ""
                                val html = getHtmlPage(token, session?.pairingCode ?: "")
                                sendHtmlResponse(output, 200, html)
                            }
                            "/pair/status" -> {
                                val session = getSessionStatus()
                                val statusStr = when {
                                    session == null -> "EXPIRED"
                                    session.isExpired() -> "EXPIRED"
                                    session.isUsed.get() -> "SUCCESS"
                                    else -> "WAITING"
                                }
                                sendJsonResponse(output, 200, "{\"status\":\"$statusStr\"}")
                            }
                            else -> sendErrorResponse(output, 404, "Not Found")
                        }
                    }
                    "POST" -> {
                        when (path) {
                            "/pair/login" -> {
                                if (contentLength <= 0 || contentLength > 4096) {
                                    sendJsonResponse(output, 400, "{\"error\":\"Invalid body length\"}")
                                    return
                                }
                                val charBuf = CharArray(contentLength)
                                var charsRead = 0
                                while (charsRead < contentLength) {
                                    val read = input.read(charBuf, charsRead, contentLength - charsRead)
                                    if (read == -1) break
                                    charsRead += read
                                }
                                val body = String(charBuf, 0, charsRead)
                                val params = parseUrlEncoded(body)

                                val token = params["token"] ?: ""
                                val username = params["username"] ?: ""
                                val password = params["password"] ?: ""

                                if (token.isBlank() || username.isBlank() || password.isBlank()) {
                                    sendJsonResponse(output, 400, "{\"error\":\"Preencha todos os campos\"}")
                                    return
                                }

                                val result = runBlocking {
                                    onLoginSubmitted(username, password, token)
                                }

                                when (result) {
                                    is LoginResult.Success -> {
                                        sendJsonResponse(output, 200, "{\"success\":true,\"message\":\"Login realizado com sucesso!\"}")
                                    }
                                    is LoginResult.Error -> {
                                        sendJsonResponse(output, 400, "{\"success\":false,\"error\":\"${result.message}\"}")
                                    }
                                }
                            }
                            else -> sendErrorResponse(output, 404, "Not Found")
                        }
                    }
                    else -> {
                        output.println("HTTP/1.1 405 Method Not Allowed")
                        output.println("Allow: GET, POST")
                        output.println("Connection: close")
                        output.println()
                    }
                }
            } catch (e: Exception) {
                Timber.w(e, "Erro ao processar requisição HTTP no servidor local")
            }
        }
    }

    private fun parseUrlEncoded(body: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        for (pair in body.split("&")) {
            val idx = pair.indexOf("=")
            if (idx > 0) {
                val key = URLDecoder.decode(pair.substring(0, idx), "UTF-8")
                val value = URLDecoder.decode(pair.substring(idx + 1), "UTF-8")
                map[key] = value
            }
        }
        return map
    }

    private fun sendHtmlResponse(output: PrintWriter, code: Int, html: String) {
        output.println("HTTP/1.1 $code OK")
        output.println("Content-Type: text/html; charset=UTF-8")
        output.println("Content-Length: ${html.toByteArray(StandardCharsets.UTF_8).size}")
        output.println("Connection: close")
        output.println()
        output.print(html)
        output.flush()
    }

    private fun sendJsonResponse(output: PrintWriter, code: Int, json: String) {
        val statusText = if (code == 200) "OK" else "Bad Request"
        output.println("HTTP/1.1 $code $statusText")
        output.println("Content-Type: application/json; charset=UTF-8")
        output.println("Content-Length: ${json.toByteArray(StandardCharsets.UTF_8).size}")
        output.println("Connection: close")
        output.println()
        output.print(json)
        output.flush()
    }

    private fun sendErrorResponse(output: PrintWriter, code: Int, message: String) {
        output.println("HTTP/1.1 $code $message")
        output.println("Content-Type: text/plain; charset=UTF-8")
        output.println("Content-Length: ${message.length}")
        output.println("Connection: close")
        output.println()
        output.print(message)
        output.flush()
    }

    private fun getHtmlPage(token: String, pairingCode: String): String {
        return """
            <!DOCTYPE html>
            <html lang="pt-BR">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Squi TV — Conectar Aparelho</title>
                <style>
                    :root {
                        --bg-primary: #090d16;
                        --bg-card: #131d31;
                        --bg-input: #0b1220;
                        --border-color: #1e293b;
                        --border-focus: #38bdf8;
                        --text-main: #f8fafc;
                        --text-muted: #94a3b8;
                        --accent: #0284c7;
                        --accent-hover: #0369a1;
                        --success: #4ade80;
                        --error: #f87171;
                        --radius: 16px;
                    }
                    * { box-sizing: border-box; margin: 0; padding: 0; }
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
                        background: var(--bg-primary);
                        color: var(--text-main);
                        display: flex;
                        justify-content: center;
                        align-items: center;
                        min-height: 100vh;
                        padding: 16px;
                    }
                    .container {
                        width: 100%;
                        max-width: 440px;
                        background: var(--bg-card);
                        border: 1px solid var(--border-color);
                        border-radius: var(--radius);
                        padding: 32px 24px;
                        box-shadow: 0 20px 40px rgba(0, 0, 0, 0.4);
                    }
                    .brand {
                        text-align: center;
                        margin-bottom: 24px;
                    }
                    .brand h1 {
                        font-size: 24px;
                        font-weight: 800;
                        letter-spacing: 2px;
                        color: var(--text-main);
                        background: linear-gradient(135deg, #38bdf8, #818cf8);
                        -webkit-background-clip: text;
                        -webkit-text-fill-color: transparent;
                        margin-bottom: 6px;
                    }
                    .brand p {
                        font-size: 14px;
                        color: var(--text-muted);
                    }
                    .code-section {
                        background: var(--bg-input);
                        border: 1px solid var(--border-color);
                        border-radius: 12px;
                        padding: 16px;
                        text-align: center;
                        margin-bottom: 20px;
                    }
                    .code-label {
                        font-size: 12px;
                        text-transform: uppercase;
                        letter-spacing: 1.5px;
                        color: var(--text-muted);
                        margin-bottom: 6px;
                    }
                    .code-value {
                        font-size: 32px;
                        font-weight: 800;
                        letter-spacing: 6px;
                        color: var(--success);
                        font-family: monospace;
                    }
                    .url-section {
                        display: flex;
                        align-items: center;
                        justify-content: space-between;
                        background: var(--bg-input);
                        border: 1px solid var(--border-color);
                        border-radius: 10px;
                        padding: 10px 14px;
                        margin-bottom: 24px;
                        font-size: 13px;
                        color: var(--text-muted);
                    }
                    .url-text {
                        word-break: break-all;
                        font-family: monospace;
                        color: #38bdf8;
                    }
                    .copy-btn {
                        background: #1e293b;
                        color: var(--text-main);
                        border: 1px solid #334155;
                        padding: 6px 10px;
                        border-radius: 6px;
                        font-size: 12px;
                        cursor: pointer;
                        transition: background 0.2s;
                        margin-left: 8px;
                        white-space: nowrap;
                    }
                    .copy-btn:hover {
                        background: #334155;
                    }
                    .form-group {
                        margin-bottom: 16px;
                    }
                    label {
                        display: block;
                        font-size: 13px;
                        font-weight: 600;
                        color: var(--text-muted);
                        margin-bottom: 6px;
                    }
                    input {
                        width: 100%;
                        padding: 14px;
                        background: var(--bg-input);
                        border: 1px solid var(--border-color);
                        border-radius: 10px;
                        color: var(--text-main);
                        font-size: 15px;
                        transition: border-color 0.2s, box-shadow 0.2s;
                    }
                    input:focus {
                        outline: none;
                        border-color: var(--border-focus);
                        box-shadow: 0 0 0 3px rgba(56, 189, 248, 0.15);
                    }
                    .btn-primary {
                        width: 100%;
                        padding: 14px;
                        background: var(--accent);
                        color: white;
                        border: none;
                        border-radius: 10px;
                        font-size: 16px;
                        font-weight: 700;
                        cursor: pointer;
                        transition: background 0.2s, transform 0.1s;
                        margin-top: 8px;
                    }
                    .btn-primary:hover {
                        background: var(--accent-hover);
                    }
                    .btn-primary:active {
                        transform: scale(0.99);
                    }
                    .btn-primary:disabled {
                        opacity: 0.6;
                        cursor: not-allowed;
                    }
                    .btn-app {
                        display: block;
                        width: 100%;
                        padding: 12px;
                        background: transparent;
                        color: var(--text-muted);
                        border: 1px dashed var(--border-color);
                        border-radius: 10px;
                        font-size: 14px;
                        text-align: center;
                        text-decoration: none;
                        margin-top: 12px;
                        transition: color 0.2s, border-color 0.2s;
                    }
                    .btn-app:hover {
                        color: var(--text-main);
                        border-color: var(--border-focus);
                    }
                    #msg {
                        margin-top: 16px;
                        text-align: center;
                        font-size: 14px;
                        font-weight: 500;
                        min-height: 20px;
                    }
                    .error { color: var(--error); }
                    .success { color: var(--success); font-size: 16px; line-height: 1.5; }
                    .footer-note {
                        text-align: center;
                        font-size: 12px;
                        color: #64748b;
                        margin-top: 24px;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="brand">
                        <h1>SQUI TV</h1>
                        <p>Conectar ao Squi TV</p>
                    </div>

                    <div class="code-section">
                        <div class="code-label">Código de Pareamento</div>
                        <div class="code-value">$pairingCode</div>
                    </div>

                    <div class="url-section">
                        <span class="url-text" id="currentUrl"></span>
                        <button class="copy-btn" id="copyBtn" onclick="copyUrl()">Copiar URL</button>
                    </div>

                    <form id="loginForm">
                        <input type="hidden" name="token" id="token" value="$token">
                        
                        <div class="form-group">
                            <label for="username">Usuário / Xtream Username</label>
                            <input type="text" id="username" name="username" required autocomplete="username" placeholder="Digite seu usuário">
                        </div>
                        
                        <div class="form-group">
                            <label for="password">Senha / Xtream Password</label>
                            <input type="password" id="password" name="password" required autocomplete="current-password" placeholder="Digite sua senha">
                        </div>
                        
                        <button type="submit" id="submitBtn" class="btn-primary">Conectar ao Squi TV</button>
                    </form>

                    <a href="squitv://pair?token=$token" class="btn-app">Abrir no Aplicativo Squi TV</a>

                    <div id="msg"></div>

                    <div class="footer-note">Conexão local segura · Squi TV</div>
                </div>

                <script>
                    document.getElementById('currentUrl').textContent = window.location.href;

                    function copyUrl() {
                        const url = window.location.href;
                        navigator.clipboard.writeText(url).then(() => {
                            const btn = document.getElementById('copyBtn');
                            btn.textContent = 'Copiado!';
                            setTimeout(() => { btn.textContent = 'Copiar URL'; }, 2000);
                        }).catch(() => {
                            alert('Não foi possível copiar automaticamente.');
                        });
                    }

                    document.getElementById('loginForm').addEventListener('submit', async function(e) {
                        e.preventDefault();
                        const msg = document.getElementById('msg');
                        const btn = document.getElementById('submitBtn');
                        
                        msg.className = '';
                        msg.textContent = 'Conectando ao Squi TV...';
                        btn.disabled = true;
                        
                        const formData = new URLSearchParams(new FormData(this));
                        try {
                            const res = await fetch('/pair/login', {
                                method: 'POST',
                                body: formData
                            });
                            const data = await res.json();
                            if (res.ok && data.success) {
                                msg.className = 'success';
                                msg.textContent = 'Dispositivo conectado com sucesso! Pode fechar esta página.';
                                document.getElementById('loginForm').style.display = 'none';
                                document.querySelector('.btn-app').style.display = 'none';
                                document.querySelector('.code-section').style.display = 'none';
                                document.querySelector('.url-section').style.display = 'none';
                            } else {
                                msg.className = 'error';
                                msg.textContent = data.error || 'Falha ao autenticar. Verifique suas credenciais.';
                                btn.disabled = false;
                            }
                        } catch (err) {
                            msg.className = 'error';
                            msg.textContent = 'Erro de comunicação com a TV. Verifique sua rede.';
                            btn.disabled = false;
                        }
                    });
                </script>
            </body>
            </html>
        """.trimIndent()
    }

    companion object {
        const val DEFAULT_PAIRING_PORT = 1234

        fun getLocalIpAddress(): String {
            try {
                val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
                for (ntf in interfaces) {
                    if (!ntf.isUp || ntf.isLoopback) continue
                    val addresses = Collections.list(ntf.inetAddresses)
                    for (addr in addresses) {
                        if (!addr.isLoopbackAddress && addr is Inet4Address) {
                            val hostAddress = addr.hostAddress
                            if (hostAddress != null && !hostAddress.startsWith("169.254")) {
                                return hostAddress
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Erro ao obter IP local")
            }
            return "127.0.0.1"
        }
    }
}
