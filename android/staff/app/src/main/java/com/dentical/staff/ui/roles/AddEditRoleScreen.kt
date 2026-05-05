package com.dentical.staff.ui.roles

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dentical.staff.data.local.entities.PermissionFlags
import com.dentical.staff.ui.theme.DenticalBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditRoleScreen(
    editingRoleId: Long = -1L,
    onSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: AddEditRoleViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(editingRoleId) {
        if (editingRoleId != -1L) viewModel.loadRole(editingRoleId)
    }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onSaved()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(if (state.isEditMode) "Edit Role" else "New Role")
                        if (state.isSystem) Icon(Icons.Default.Lock, null, modifier = Modifier.size(18.dp))
                    }
                },
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
                value = state.name,
                onValueChange = viewModel::onNameChange,
                label = { Text("Role Name *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !state.isSystem
            )

            OutlinedTextField(
                value = state.description,
                onValueChange = viewModel::onDescriptionChange,
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 2,
                enabled = !state.isSystem
            )

            Text("Permissions", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

            // Header row
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                Text("Resource", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                listOf("C", "R", "U", "D").forEach { flag ->
                    Text(flag, Modifier.width(40.dp), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                }
            }
            HorizontalDivider()

            ALL_RESOURCES.forEach { resource ->
                val perms = state.permissions[resource] ?: PermissionFlags()
                PermissionRow(
                    resource  = resource,
                    perms     = perms,
                    enabled   = !state.isSystem,
                    onToggle  = { flag -> viewModel.togglePermission(resource, flag) }
                )
                HorizontalDivider(thickness = 0.5.dp)
            }

            state.errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            if (!state.isSystem) {
                Button(
                    onClick = { viewModel.save(editingRoleId) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = DenticalBlue)
                ) {
                    if (state.isLoading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    else Text(if (state.isEditMode) "Save Changes" else "Create Role")
                }
            } else {
                Text(
                    "System roles cannot be edited.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PermissionRow(
    resource: String,
    perms: PermissionFlags,
    enabled: Boolean,
    onToggle: (String) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            resource.replaceFirstChar { it.uppercase() },
            Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium
        )
        Checkbox(checked = perms.canCreate, onCheckedChange = { onToggle("create") }, enabled = enabled, modifier = Modifier.width(40.dp))
        Checkbox(checked = perms.canRead,   onCheckedChange = { onToggle("read")   }, enabled = enabled, modifier = Modifier.width(40.dp))
        Checkbox(checked = perms.canUpdate, onCheckedChange = { onToggle("update") }, enabled = enabled, modifier = Modifier.width(40.dp))
        Checkbox(checked = perms.canDelete, onCheckedChange = { onToggle("delete") }, enabled = enabled, modifier = Modifier.width(40.dp))
    }
}
