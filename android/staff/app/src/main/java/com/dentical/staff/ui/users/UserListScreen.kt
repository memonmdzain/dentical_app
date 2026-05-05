package com.dentical.staff.ui.users

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dentical.staff.data.local.entities.UserWithRoles
import com.dentical.staff.ui.theme.DenticalBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserListScreen(
    onAddUser: () -> Unit,
    onEditUser: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: UserManagementViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var confirmDeactivateUser by remember { mutableStateOf<UserWithRoles?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Users") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DenticalBlue, titleContentColor = MaterialTheme.colorScheme.onPrimary, navigationIconContentColor = MaterialTheme.colorScheme.onPrimary)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddUser, containerColor = DenticalBlue) {
                Icon(Icons.Default.Add, contentDescription = "Add User", tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = viewModel::onSearchChange,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search by name or username…") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true
            )

            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (state.users.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No users found", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.users, key = { it.user.id }) { userWithRoles ->
                        UserCard(
                            userWithRoles  = userWithRoles,
                            onEdit         = { onEditUser(userWithRoles.user.id) },
                            onToggleActive = { confirmDeactivateUser = userWithRoles }
                        )
                    }
                }
            }
        }
    }

    confirmDeactivateUser?.let { u ->
        AlertDialog(
            onDismissRequest = { confirmDeactivateUser = null },
            title = { Text(if (u.user.isActive) "Deactivate User" else "Activate User") },
            text  = {
                Text(
                    if (u.user.isActive)
                        "Deactivate ${u.user.fullName}? They won't be able to log in."
                    else
                        "Activate ${u.user.fullName}?"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.toggleActive(u.user.id, u.user.isActive)
                    confirmDeactivateUser = null
                }) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeactivateUser = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun UserCard(
    userWithRoles: UserWithRoles,
    onEdit: () -> Unit,
    onToggleActive: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(userWithRoles.user.fullName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("@${userWithRoles.user.username}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    userWithRoles.roles.forEach { role ->
                        SuggestionChip(
                            onClick = {},
                            label = { Text(role.name, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                    if (!userWithRoles.user.isActive) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text("INACTIVE", style = MaterialTheme.typography.labelSmall) },
                            colors = SuggestionChipDefaults.suggestionChipColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                        )
                    }
                }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = DenticalBlue)
            }
            IconButton(onClick = onToggleActive) {
                Icon(
                    if (userWithRoles.user.isActive) Icons.Default.PersonOff else Icons.Default.PersonAdd,
                    contentDescription = if (userWithRoles.user.isActive) "Deactivate" else "Activate",
                    tint = if (userWithRoles.user.isActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
