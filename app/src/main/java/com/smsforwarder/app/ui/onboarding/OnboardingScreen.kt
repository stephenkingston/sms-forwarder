package com.smsforwarder.app.ui.onboarding

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    vm: OnboardingViewModel = viewModel(factory = OnboardingViewModel.factory(LocalContext.current))
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    val state by vm.state.collectAsState()

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results.values.all { it }
        vm.onPermissionResult(granted)
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Header()

            StepCard(
                index = 1,
                title = "Sender Gmail account",
                subtitle = "The Gmail account that will send the forwarded emails."
            ) {
                OutlinedTextField(
                    value = state.senderEmail,
                    onValueChange = vm::onSenderChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Gmail address") },
                    placeholder = { Text("you@gmail.com") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    leadingIcon = { Icon(Icons.Default.Email, null) }
                )
            }

            StepCard(
                index = 2,
                title = "App password",
                subtitle = "Gmail requires an app password (not your normal password). Make sure 2-Step Verification is enabled first."
            ) {
                AppPasswordHelp(
                    onOpen2FA = { uriHandler.openUri("https://myaccount.google.com/signinoptions/twosv") },
                    onOpenAppPasswords = { uriHandler.openUri("https://myaccount.google.com/apppasswords") },
                    onOpenGuide = { uriHandler.openUri("https://support.google.com/accounts/answer/185833") }
                )
                Spacer(Modifier.height(12.dp))
                var visible by remember { mutableStateOf(false) }
                OutlinedTextField(
                    value = state.appPassword,
                    onValueChange = vm::onPasswordChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("16-character app password") },
                    placeholder = { Text("xxxx xxxx xxxx xxxx") },
                    singleLine = true,
                    visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                    leadingIcon = { Icon(Icons.Default.Lock, null) },
                    trailingIcon = {
                        IconButton(onClick = { visible = !visible }) {
                            Icon(
                                if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (visible) "Hide" else "Show"
                            )
                        }
                    }
                )
            }

            StepCard(
                index = 3,
                title = "Receiving email",
                subtitle = "Where forwarded SMS messages will be delivered. Any provider works."
            ) {
                OutlinedTextField(
                    value = state.recipientEmail,
                    onValueChange = vm::onRecipientChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Recipient email") },
                    placeholder = { Text("inbox@example.com") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    leadingIcon = { Icon(Icons.Default.Inbox, null) }
                )
            }

            StepCard(
                index = 4,
                title = "Test connection",
                subtitle = "We'll send a test email to confirm your credentials work."
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { scope.launch { vm.sendTest() } },
                        enabled = state.canTest && !state.testing,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (state.testing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.size(8.dp))
                        }
                        Text(if (state.testing) "Sending…" else "Send test email")
                    }
                }
                state.testResult?.let { TestResultRow(it) }
            }

            StepCard(
                index = 5,
                title = "Permissions",
                subtitle = "Allow the app to read incoming SMS so it can forward them."
            ) {
                Button(
                    onClick = {
                        val perms = mutableListOf(
                            Manifest.permission.RECEIVE_SMS
                        )
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            perms += Manifest.permission.POST_NOTIFICATIONS
                        }
                        smsPermissionLauncher.launch(perms.toTypedArray())
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Sms, null)
                    Spacer(Modifier.size(8.dp))
                    Text(if (state.permissionsGranted) "Permissions granted" else "Grant SMS permission")
                }
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = { openBatteryOptimizationSettings(context) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Disable battery optimization (recommended)")
                }
            }

            Button(
                onClick = {
                    vm.saveAndFinish()
                    onComplete()
                },
                enabled = state.canFinish,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(Icons.Default.CheckCircle, null)
                Spacer(Modifier.size(8.dp))
                Text("Finish setup", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun Header() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Forum,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Spacer(Modifier.size(12.dp))
            Column {
                Text(
                    "SMS Forwarder",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Forward incoming SMS to email",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StepCard(
    index: Int,
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            index.toString(),
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Spacer(Modifier.size(12.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
private fun AppPasswordHelp(
    onOpen2FA: () -> Unit,
    onOpenAppPasswords: () -> Unit,
    onOpenGuide: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            HelpLink("1. Enable 2-Step Verification", onOpen2FA)
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            HelpLink("2. Create app password", onOpenAppPasswords)
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            HelpLink("Full Google guide", onOpenGuide)
        }
    }
}

@Composable
private fun HelpLink(label: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 10.dp, horizontal = 4.dp)
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Icon(Icons.Default.OpenInNew, null, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun TestResultRow(result: TestStatus) {
    Spacer(Modifier.height(12.dp))
    Surface(
        color = if (result.success) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (result.success) Icons.Default.CheckCircle else Icons.Default.Email,
                contentDescription = null,
                tint = if (result.success) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(Modifier.size(10.dp))
            Text(
                result.message,
                color = if (result.success) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

private fun openBatteryOptimizationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
        data = Uri.parse("package:${context.packageName}")
    }
    runCatching { context.startActivity(intent) }
}
