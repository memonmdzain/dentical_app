package com.dentical.staff.ui.roles

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
import com.dentical.staff.data.local.entities.RoleEntity
import com.dentical.staff.ui.theme.DenticalBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoleListScreen(
    onAddRole: () -> Unit,
    onEditRole: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: RoleManagementViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var confirmDeleteRole by remember { mutableStateOf<RoleEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Roles") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DenticalBlue, titleContentColor = MaterialTheme.colorScheme.onPrimary, navigationIconContentColor = MaterialTheme.colorScheme.onPrimary)
            )
        },
        floatingActionButton = {
            if (state.canCreate) {
                FloatingActionButton(onClick = onAddRole, containerColor = DenticalBlue) {
                    Icon(Icons.Default.Add, contentDescription = "Add Role", tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.roles, key = { it.id }) { role ->
                    RoleCard(
                        role      = role,
                        canUpdate = state.canUpdate,
                        canDelete = state.canDelete,
                        onEdit    = { onEditRole(role.id) },
                        onDelete  = { confirmDeleteRole = role }
                    )
                }
            }
        }
    }

    confirmDeleteRole?.let { role ->
        AlertDialog(
            onDismissRequest = { confirmDeleteRole = null },
            title = { Text("Delete Role") },
            text  = { Text("Delete role \"${role.name}\"? Users with only this role will lose access.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteRole(role)
                    confirmDeleteRole = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteRole = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun RoleCard(
    role: RoleEntity,
    canUpdate: Boolean,
    canDelete: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
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
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(role.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (role.isSystem) {
                        Icon(Icons.Default.Lock, contentDescription = "System role", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                role.description?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (canUpdate) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = DenticalBlue)
                }
            }
            if (canDelete && !role.isSystem) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
