package com.ishaan.paperBird

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.ishaan.paperBird.ui.components.LockScreen
import com.ishaan.paperBird.ui.screens.LetterViewModel
import com.ishaan.paperBird.ui.theme.AccentColors
import com.ishaan.paperBird.ui.theme.LocalCategoryColors
import com.ishaan.paperBird.ui.theme.paperBirdTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: LetterViewModel = hiltViewModel()
            val settingsRepo = viewModel.settingsRepository

            val darkThemeOverride by settingsRepo.darkTheme.collectAsState(initial = null)
            val accentName by settingsRepo.accentColor.collectAsState(initial = "Rose")
            val accent = AccentColors.all[accentName] ?: AccentColors.Rose
            val systemDark = isSystemInDarkTheme()

            // null = user hasn't picked → follow system
            val useDark = darkThemeOverride ?: systemDark
            val categoryColors by settingsRepo.allCategoryColors.collectAsState(initial = emptyMap())

            // Privacy Lock State
            val appLockEnabled by settingsRepo.appLockEnabled.collectAsState(initial = null)
            val useBiometrics by settingsRepo.useBiometrics.collectAsState(initial = false)
            val usePin by settingsRepo.usePin.collectAsState(initial = false)
            val appPinHash by settingsRepo.appPinHash.collectAsState(initial = null)

            var isUnlocked by remember(appLockEnabled == null) {
                mutableStateOf(appLockEnabled == false)
            }

            paperBirdTheme(darkTheme = useDark, accent = accent) {
                CompositionLocalProvider(LocalCategoryColors provides categoryColors) {
                    val needsLock = appLockEnabled == true && (useBiometrics || usePin)
                    
                    if (appLockEnabled != null && needsLock && !isUnlocked) {
                        LockScreen(
                            useBiometrics = useBiometrics,
                            onAuthenticateBiometric = {
                                showBiometricPrompt { isUnlocked = true }
                            },
                            onPinEntered = { pin ->
                                if (settingsRepo.verifyPin(pin, appPinHash)) {
                                    isUnlocked = true
                                    true
                                } else false
                            },
                            onUnlocked = { isUnlocked = true }
                        )
                    } else if (appLockEnabled != null) {
                        PaperBirdApp(viewModel = viewModel)
                    }
                }
            }
        }
    }

    private fun showBiometricPrompt(onSuccess: () -> Unit) {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock paperBird")
            .setSubtitle("Use your biometric credential")
            .setNegativeButtonText("Use PIN")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}
