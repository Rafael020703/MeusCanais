package com.meuscanais.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meuscanais.R
import com.meuscanais.core.ui.components.buttons.AppButton
import com.meuscanais.core.ui.components.inputs.AppTextField
import com.meuscanais.core.ui.theme.*
import com.meuscanais.ui.dashboard.PortalBackground
import kotlinx.coroutines.delay

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoginSuccess: () -> Unit
) {
    val username by viewModel.username.collectAsState()
    val password by viewModel.password.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val usernameFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        viewModel.loginSuccess.collect {
            onLoginSuccess()
        }
        delay(500)
        try { usernameFocusRequester.requestFocus() } catch (_: Exception) {}
    }

    LoginContent(
        username = username,
        password = password,
        isLoading = isLoading,
        errorMessage = errorMessage,
        onUsernameChange = viewModel::onUsernameChanged,
        onPasswordChange = viewModel::onPasswordChanged,
        onLoginClick = viewModel::login,
        onRestoreClick = { viewModel.restoreBackup(isAuto = false) },
        usernameFocusRequester = usernameFocusRequester,
        passwordFocusRequester = passwordFocusRequester
    )
}

@Composable
fun LoginContent(
    username: String,
    password: String,
    isLoading: Boolean,
    errorMessage: String?,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    onRestoreClick: () -> Unit,
    usernameFocusRequester: FocusRequester,
    passwordFocusRequester: FocusRequester
) {
    val tokens = AppDesignSystem
    val responsive = tokens.responsive

    val institutionalWidth = responsive.dp(500.dp)
    val panelWidth = responsive.dp(450.dp)
    val horizontalGap = responsive.dp(48.dp)
    val horizontalPadding = responsive.dp(tokens.spacing.large)

    val institutionalIconSize = responsive.dp(80.dp)
    val displayTitleSize = responsive.sp(48.sp)
    val displaySubtitleSize = responsive.sp(16.sp)
    val panelTitleSize = responsive.sp(28.sp)
    val panelSubtitleSize = responsive.sp(14.sp)

    PortalBackground(
        showAtmosphere = true,
        atmosphereUrl = "https://images.unsplash.com/photo-1540747913346-19e32dc3e97e?auto=format&fit=crop&q=80&w=1920"
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (responsive.heightDp < responsive.dp(600.dp)) Modifier.verticalScroll(rememberScrollState()) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .wrapContentWidth()
                    .padding(horizontal = horizontalPadding),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // --- LADO ESQUERDO: INSTITUCIONAL ---
                Column(
                    modifier = Modifier.width(institutionalWidth),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(institutionalIconSize)
                            .background(tokens.colors.primary.copy(alpha = 0.1f), CircleShape)
                            .border(responsive.dp(2.dp), tokens.colors.primary.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LiveTv,
                            contentDescription = null,
                            modifier = Modifier.size(institutionalIconSize * 0.6f),
                            tint = tokens.colors.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(responsive.dp(tokens.spacing.medium)))

                    Text(
                        text = stringResource(R.string.institutional_title),
                        style = tokens.typography.display.copy(
                            fontSize = displayTitleSize,
                            fontWeight = FontWeight.Black,
                            letterSpacing = responsive.sp(4.sp)
                        ),
                        color = tokens.colors.textPrimary,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = stringResource(R.string.institutional_desc),
                        style = tokens.typography.body.copy(
                            fontSize = displaySubtitleSize,
                            lineHeight = displaySubtitleSize * 1.3f
                        ),
                        color = tokens.colors.textSecondary,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.width(horizontalGap))

                // --- LADO DIREITO: PAINEL DE LOGIN ---
                Surface(
                    modifier = Modifier
                        .width(panelWidth)
                        .border(
                            responsive.dp(1.5.dp),
                            tokens.colors.primary.copy(alpha = 0.3f),
                            tokens.shapes.extraLarge
                        ),
                    shape = tokens.shapes.extraLarge,
                    color = Color.Black.copy(alpha = 0.65f),
                    shadowElevation = responsive.dp(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(responsive.dp(tokens.spacing.extraLarge * 1.5f)),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = stringResource(R.string.login_welcome_back),
                            style = tokens.typography.headline.copy(
                                fontSize = panelTitleSize,
                                fontWeight = FontWeight.Black
                            ),
                            color = tokens.colors.textPrimary
                        )

                        Text(
                            text = stringResource(R.string.login_subtitle_instruction),
                            style = tokens.typography.body.copy(fontSize = panelSubtitleSize),
                            color = tokens.colors.textSecondary,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(responsive.dp(tokens.spacing.extraLarge)))

                        // FIELDS
                        AppTextField(
                            value = username,
                            onValueChange = onUsernameChange,
                            placeholder = stringResource(R.string.username_label).uppercase(),
                            modifier = Modifier.focusRequester(usernameFocusRequester),
                            leadingIcon = { Icon(Icons.Rounded.Person, null, tint = tokens.colors.textSecondary, modifier = Modifier.size(responsive.dp(20.dp))) },
                            imeAction = ImeAction.Next,
                            keyboardActions = KeyboardActions(onNext = { passwordFocusRequester.requestFocus() })
                        )

                        Spacer(modifier = Modifier.height(responsive.dp(tokens.spacing.medium)))

                        AppTextField(
                            value = password,
                            onValueChange = onPasswordChange,
                            placeholder = stringResource(R.string.password_label).uppercase(),
                            isPassword = true,
                            modifier = Modifier.focusRequester(passwordFocusRequester),
                            leadingIcon = { Icon(Icons.Rounded.Lock, null, tint = tokens.colors.textSecondary, modifier = Modifier.size(responsive.dp(20.dp))) },
                            imeAction = ImeAction.Go,
                            keyboardActions = KeyboardActions(onGo = { if (!isLoading) onLoginClick() })
                        )

                        // ACTIONS
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = onRestoreClick,
                                contentPadding = PaddingValues(responsive.dp(4.dp))
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = stringResource(R.string.restore_backup_label),
                                        style = tokens.typography.label.copy(fontSize = responsive.sp(11.sp)),
                                        color = tokens.colors.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(responsive.dp(6.dp)))
                                    Icon(
                                        imageVector = Icons.Rounded.CloudDownload,
                                        contentDescription = null,
                                        tint = tokens.colors.primary,
                                        modifier = Modifier.size(responsive.dp(14.dp))
                                    )
                                }
                            }
                        }

                        if (errorMessage != null) {
                            Spacer(modifier = Modifier.height(responsive.dp(tokens.spacing.small)))
                            Surface(
                                color = tokens.colors.error.copy(alpha = 0.1f),
                                shape = tokens.shapes.small,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = errorMessage.uppercase(),
                                    color = tokens.colors.error,
                                    style = tokens.typography.caption.copy(fontSize = responsive.sp(10.sp)),
                                    fontWeight = FontWeight.Black,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(responsive.dp(tokens.spacing.small))
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(responsive.dp(tokens.spacing.extraLarge)))

                        // LOGIN BUTTON (ENTRAR)
                        AppButton(
                            text = if (isLoading) stringResource(R.string.msg_connecting).uppercase() else stringResource(R.string.login_button).uppercase(),
                            onClick = onLoginClick,
                            enabled = !isLoading,
                            icon = Icons.AutoMirrored.Rounded.ArrowForward,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(responsive.dp(64.dp))
                        )
                    }
                }
            }

            // FOOTER
            Text(
                text = "MEUS CANAIS • V1.0 PREMIUM",
                style = tokens.typography.caption.copy(fontSize = responsive.sp(11.sp)),
                color = tokens.colors.textSecondary.copy(alpha = 0.3f),
                fontWeight = FontWeight.Bold,
                letterSpacing = responsive.sp(2.sp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = responsive.dp(tokens.spacing.medium))
            )
        }
    }
}
