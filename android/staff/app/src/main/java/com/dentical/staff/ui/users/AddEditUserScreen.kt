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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dentical.staff.ui.theme.DenticalBlue

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditUserScreen(
    editingUserId: Long = -1L,
    onSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: AddEditUserViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showPassword by remember { mutableStateOf(false) }

    LaunchedEffect(editingUserId) {
        if (editingUserId != -1L) viewModel.loadUser(editingUserId)
    }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onSaved()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditMode) "Edit User" else "Add User") },
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = state.fullName,
                onValueChange = viewModel::onFullNameChange,
                label = { Text("Full Name *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = state.username,
                onValueChange = viewModel::onUsernameChange,
                label = { Text("Username *") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isEditMode,
                singleLine = true
            )

            if (!state.isEditMode) {
                OutlinedTextField(
                    value = state.password,
                    onValueChange = viewModel::onPasswordChange,
                    label = { Text("Password *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null
                            )
                        }
                    }
                )
            }

            // Roles multi-select
            Text("Assign Roles *", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.availableRoles.forEach { role ->
                    val selected = role.id in state.selectedRoleIds
                    FilterChip(
                        selected = selected,
                        onClick  = { viewModel.toggleRole(role.id) },
                        label    = { Text(role.name) }
                    )
                }
            }

            // Active toggle (edit mode only)
            if (state.isEditMode) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Account Active", style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = state.isActive, onCheckedChange = viewModel::onActiveChange)
                }
            }

            state.errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = { viewModel.save(editingUserId) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = DenticalBlue)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(if (state.isEditMode) "Save Changes" else "Create User")
                }
            }
        }
    }
}
