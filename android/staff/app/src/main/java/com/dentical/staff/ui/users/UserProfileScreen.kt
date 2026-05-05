package com.dentical.staff.ui.users

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dentical.staff.ui.theme.DenticalBlue

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun UserProfileScreen(
    onLogout: () -> Unit,
    onBack: () -> Unit,
    viewModel: UserProfileViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showCurrentPw by remember { mutableStateOf(false) }
    var showNewPw by remember { mutableStateOf(false) }
    var showConfirmPw by remember { mutableStateOf(false) }

    LaunchedEffect(state.successMessage) {
        if (state.successMessage != null) viewModel.clearMessages()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Profile") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DenticalBlue, titleContentColor = MaterialTheme.colorScheme.onPrimary, navigationIconContentColor = MaterialTheme.colorScheme.onPrimary)
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            state.currentUser?.let { userWithRoles ->
                // Profile info card
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Account Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        HorizontalDivider()
                        ProfileRow("Full Name", userWithRoles.user.fullName)
                        ProfileRow("Username", "@${userWithRoles.user.username}")
                        Text("Roles", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            userWithRoles.roles.forEach { role ->
                                SuggestionChip(onClick = {}, label = { Text(role.name) })
                            }
                        }
                    }
                }

                // Change password card
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Change Password", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        HorizontalDivider()

                        PasswordField("Current Password", state.currentPassword, showCurrentPw, { showCurrentPw = !showCurrentPw }, viewModel::onCurrentPasswordChange)
                        PasswordField("New Password", state.newPassword, showNewPw, { showNewPw = !showNewPw }, viewModel::onNewPasswordChange)
                        PasswordField("Confirm Password", state.confirmPassword, showConfirmPw, { showConfirmPw = !showConfirmPw }, viewModel::onConfirmPasswordChange)

                        state.errorMessage?.let {
                            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                        state.successMessage?.let {
                            Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                        }

                        Button(
                            onClick = viewModel::changePassword,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !state.isLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = DenticalBlue)
                        ) {
                            if (state.isLoading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                            else Text("Update Password")
                        }
                    }
                }
            }

            // Logout
            OutlinedButton(
                onClick = {
                    viewModel.logout()
                    onLogout()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) { Text("Logout") }
        }
    }
}

@Composable
private fun ProfileRow(label: String, value: String) {
    Column(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun PasswordField(
    label: String,
    value: String,
    showPassword: Boolean,
    onToggle: () -> Unit,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = onToggle) {
                Icon(if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
            }
        }
    )
}
