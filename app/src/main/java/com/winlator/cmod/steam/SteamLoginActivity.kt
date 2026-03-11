package com.winlator.cmod.steam

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.winlator.cmod.steam.enums.LoginResult
import com.winlator.cmod.steam.enums.LoginScreen
import com.winlator.cmod.steam.ui.SteamLoginViewModel
import com.winlator.cmod.steam.ui.components.QrCodeImage
import timber.log.Timber

import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import android.content.res.Configuration
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.winlator.cmod.R
import androidx.compose.ui.text.style.TextAlign

class SteamLoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Ensure SteamService is running
        try {
            val intent = android.content.Intent(this, com.winlator.cmod.steam.service.SteamService::class.java)
            startForegroundService(intent)
        } catch (e: Exception) {
            Timber.e(e, "Failed to start SteamService from SteamLoginActivity")
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        setContent {
            SteamLoginTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val viewModel: SteamLoginViewModel = viewModel()
                    LoginContent(viewModel)
                }
            }
        }
    }

    @Composable
    fun SteamLoginTheme(content: @Composable () -> Unit) {
        val darkColorScheme = darkColorScheme(
            primary = colorResource(R.color.settings_accent),
            onPrimary = colorResource(R.color.settings_text_primary),
            secondary = colorResource(R.color.settings_text_secondary),
            background = colorResource(R.color.window_background_color_dark),
            surface = colorResource(R.color.settings_card_surface),
            onSurface = colorResource(R.color.settings_text_primary),
            outline = colorResource(R.color.settings_outline)
        )

        MaterialTheme(
            colorScheme = darkColorScheme,
            typography = Typography(),
            content = content
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun LoginContent(viewModel: SteamLoginViewModel) {
        val state by viewModel.loginState.collectAsState()
        val configuration = LocalConfiguration.current
        val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        var passwordVisible by remember { mutableStateOf(false) }

        LaunchedEffect(state.loginResult) {
            if (state.loginResult == LoginResult.Success) {
                Timber.i("User logged in, finishing activity")
                finish()
            }
        }

        // Auto-start QR loading if on credential screen
        LaunchedEffect(state.loginScreen) {
            if (state.loginScreen == LoginScreen.CREDENTIAL) {
                viewModel.onStartQrLogin()
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Steam Login", color = MaterialTheme.colorScheme.onPrimary) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = colorResource(R.color.settings_section_surface)
                    ),
                    navigationIcon = {
                        if (state.loginScreen == LoginScreen.TWO_FACTOR) {
                            IconButton(onClick = { viewModel.onShowLoginScreen(LoginScreen.CREDENTIAL) }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                    }
                )
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                if (state.loginScreen == LoginScreen.TWO_FACTOR) {
                    TwoFactorLogin(state, viewModel)
                } else {
                    if (isLandscape) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.weight(1.2f).fillMaxHeight()) {
                                CredentialLogin(state, viewModel, passwordVisible, true) { passwordVisible = !passwordVisible }
                            }
                            
                            VerticalDivider(
                                modifier = Modifier.fillMaxHeight().padding(vertical = 24.dp, horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outline
                            )
                            
                            Box(modifier = Modifier.weight(0.8f).fillMaxHeight()) {
                                QrLogin(state, viewModel, true)
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CredentialLogin(state, viewModel, passwordVisible, false) { passwordVisible = !passwordVisible }
                            
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 16.dp, horizontal = 32.dp),
                                color = MaterialTheme.colorScheme.outline
                            )
                            
                            QrLogin(state, viewModel, false)
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun CredentialLogin(state: com.winlator.cmod.steam.ui.data.UserLoginState, viewModel: SteamLoginViewModel, passwordVisible: Boolean, isLandscape: Boolean, onTogglePassword: () -> Unit) {
        var showRetryButton by remember { mutableStateOf(false) }
        
        LaunchedEffect(state.isSteamConnected) {
            if (!state.isSteamConnected) {
                kotlinx.coroutines.delay(15000)
                if (!state.isSteamConnected) {
                    showRetryButton = true
                }
            } else {
                showRetryButton = false
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isLandscape) 4.dp else 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!state.isSteamConnected) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Connecting...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                }
                
                if (showRetryButton) {
                    TextButton(onClick = { 
                        showRetryButton = false
                        viewModel.retryConnection() 
                    }) {
                        Text("Retry", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            OutlinedTextField(
                value = state.username,
                onValueChange = { viewModel.setUsername(it) },
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoggingIn,
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.secondary
                )
            )
            Spacer(Modifier.height(if (isLandscape) 4.dp else 8.dp))
            OutlinedTextField(
                value = state.password,
                onValueChange = { viewModel.setPassword(it) },
                label = { Text("Password") },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = onTogglePassword) {
                        Icon(if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, null, tint = MaterialTheme.colorScheme.secondary)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoggingIn,
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.secondary
                )
            )
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = state.rememberSession, 
                    onCheckedChange = { viewModel.setRememberSession(it) }, 
                    enabled = !state.isLoggingIn,
                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.scale(0.85f)
                )
                Text("Remember Me", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
            }

            Spacer(Modifier.height(if (isLandscape) 8.dp else 16.dp))

            Button(
                onClick = { viewModel.onCredentialLogin() },
                modifier = Modifier.fillMaxWidth().height(44.dp),
                enabled = state.username.isNotEmpty() && state.password.isNotEmpty() && !state.isLoggingIn && state.isSteamConnected,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = MaterialTheme.shapes.medium
            ) {
                if (state.isLoggingIn) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                } else {
                    Text("Login", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.titleSmall)
                }
            }

            Spacer(Modifier.height(if (isLandscape) 4.dp else 16.dp))
            TextButton(onClick = { finish() }, modifier = Modifier.height(32.dp)) {
                Text("Cancel", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodySmall)
            }
        }
    }

    @Composable
    fun QrLogin(state: com.winlator.cmod.steam.ui.data.UserLoginState, viewModel: SteamLoginViewModel, isLandscape: Boolean) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier
                    .padding(if (isLandscape) 4.dp else 32.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Sign in with QR", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(if (isLandscape) 8.dp else 16.dp))
                
                if (state.qrCode != null) {
                    QrCodeImage(content = state.qrCode!!, size = if (isLandscape) 160.dp else 220.dp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Scan with Steam app", 
                        style = MaterialTheme.typography.labelSmall, 
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                } else if (state.isQrFailed) {
                    Text("Failed to load QR", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { viewModel.onQrRetry() }, shape = MaterialTheme.shapes.medium, modifier = Modifier.height(36.dp)) {
                        Text("Retry", style = MaterialTheme.typography.bodySmall)
                    }
                } else {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                }
            }
        }
    }

    @Composable
    fun TwoFactorLogin(state: com.winlator.cmod.steam.ui.data.UserLoginState, viewModel: SteamLoginViewModel) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val methodText = when (state.lastTwoFactorMethod) {
                "steam_guard" -> "Confirm login in your Steam mobile app"
                "email_code" -> "Enter the code sent to your email (${state.email})"
                else -> "Enter your Steam Guard code"
            }
            
            Text("Two-Factor Authentication", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(16.dp))
            Text(methodText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            
            if (state.lastTwoFactorMethod != "steam_guard") {
                Spacer(Modifier.height(24.dp))
                OutlinedTextField(
                    value = state.twoFactorCode,
                    onValueChange = { viewModel.setTwoFactorCode(it) },
                    label = { Text("Code") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    enabled = !state.isLoggingIn,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = { viewModel.submitTwoFactor() },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    enabled = state.twoFactorCode.length >= 5 && !state.isLoggingIn,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("Submit", style = MaterialTheme.typography.titleMedium)
                }
            } else {
                Spacer(Modifier.height(32.dp))
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(16.dp))
                Text("Waiting for confirmation...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
            
            Spacer(Modifier.height(32.dp))
            TextButton(onClick = { viewModel.onShowLoginScreen(LoginScreen.CREDENTIAL) }) {
                Text("Cancel", color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}
