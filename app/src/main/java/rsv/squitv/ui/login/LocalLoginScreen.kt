package rsv.squitv.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import rsv.squitv.core.ui.components.buttons.AppButton
import rsv.squitv.data.pairing.PairingState
import rsv.squitv.ui.viewmodel.LocalLoginViewModel

@Composable
fun LocalLoginScreen(
    viewModel: LocalLoginViewModel,
    onBack: () -> Unit
) {
    val status by viewModel.serverStatus.collectAsStateWithLifecycle()
    val port by viewModel.serverPort.collectAsStateWithLifecycle()
    val ip by viewModel.localIp.collectAsStateWithLifecycle()
    val session by viewModel.currentSession.collectAsStateWithLifecycle()

    val url = when {
        port > 0 -> "http://$ip:$port"
        status == PairingState.ERROR -> "Erro ao iniciar servidor"
        else -> "Carregando..."
    }
    val code = session?.pairingCode ?: "---- ----"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(600.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF1E293B))
                .padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Login via Rede Local",
                color = Color(0xFF38BDF8),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Conecte seu celular ou computador à mesma rede Wi-Fi da TV e acesse:",
                color = Color(0xFF94A3B8),
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // URL Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0F172A))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = url,
                    color = Color(0xFFF8FAFC),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Code Box
            Text(
                text = "Código de Pareamento",
                color = Color(0xFF94A3B8),
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0F172A))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = code,
                    color = Color(0xFF4ADE80),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Status message
            val statusText = when (status) {
                PairingState.WAITING -> "Aguardando conexão..."
                PairingState.CONNECTED -> "Dispositivo conectado."
                PairingState.AUTHENTICATING -> "Autenticando credenciais..."
                PairingState.SUCCESS -> "Login realizado com sucesso!"
                PairingState.ERROR -> "Não foi possível iniciar o servidor. A porta 1234 está ocupada. Feche o aplicativo que a utiliza e tente novamente."
                PairingState.EXPIRED -> "Sessão expirada. Reinicie o pareamento."
                PairingState.CANCELLED -> "Pareamento cancelado."
            }

            val statusColor = when (status) {
                PairingState.SUCCESS -> Color(0xFF4ADE80)
                PairingState.ERROR, PairingState.EXPIRED -> Color(0xFFF87171)
                else -> Color(0xFFCBD5E1)
            }

            Text(
                text = statusText,
                color = statusColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            AppButton(
                text = "Cancelar",
                onClick = {
                    viewModel.cancelPairing()
                    onBack()
                },
                modifier = Modifier.width(200.dp)
            )
        }
    }
}
