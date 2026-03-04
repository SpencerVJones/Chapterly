package com.example.chapterly.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.chapterly.data.auth.AuthState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val AccountPanelColor = Color(0xFFF2EEF8)
private val AccountDividerColor = Color(0xFFE4DDEF)
private val AccountAccentColor = Color(0xFF6B54C8)
private val AccountAccentSoft = Color(0xFFE1D8FA)
private val AccountSuccessColor = Color(0xFF2E9B4B)
private val AccountSuccessSoft = Color(0xFFDDEEDF)

private data class AuthCallbacks(
    val onEmailChanged: (String) -> Unit,
    val onPasswordChanged: (String) -> Unit,
    val onSignIn: () -> Unit,
    val onSignUp: () -> Unit,
    val onSignOut: () -> Unit,
    val onUpdateDisplayName: (String) -> Unit,
    val onClearOfflineCache: () -> Unit,
    val onDeleteAccount: () -> Unit,
)

private data class SettingRowModel(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val iconContainerColor: Color = Color(0xFFE8E4F2),
    val iconTint: Color = AccountAccentColor,
    val trailingText: String? = null,
    val highlightColor: Color? = null,
    val onClick: (() -> Unit)? = null,
)

@Suppress("FunctionName")
@Composable
fun AuthRoute(
    onOpenReadingHub: () -> Unit = {},
    onOpenRecommendations: () -> Unit = {},
    onOpenClubs: () -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    onOpenCreatorMode: () -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AuthScreen(
        uiState = uiState,
        callbacks =
            AuthCallbacks(
                onEmailChanged = viewModel::onEmailChanged,
                onPasswordChanged = viewModel::onPasswordChanged,
                onSignIn = viewModel::signIn,
                onSignUp = viewModel::signUp,
                onSignOut = viewModel::signOut,
                onUpdateDisplayName = viewModel::updateDisplayName,
                onClearOfflineCache = viewModel::clearOfflineCache,
                onDeleteAccount = viewModel::deleteAccount,
            ),
        onOpenReadingHub = onOpenReadingHub,
        onOpenRecommendations = onOpenRecommendations,
        onOpenClubs = onOpenClubs,
        onOpenNotifications = onOpenNotifications,
        onOpenCreatorMode = onOpenCreatorMode,
    )
}

@Suppress("FunctionName")
@Composable
private fun AuthScreen(
    uiState: AuthUiState,
    callbacks: AuthCallbacks,
    onOpenReadingHub: () -> Unit = {},
    onOpenRecommendations: () -> Unit = {},
    onOpenClubs: () -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    onOpenCreatorMode: () -> Unit = {},
) {
    val scrollState = rememberScrollState()
    var showEditProfileDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteAccountDialog by rememberSaveable { mutableStateOf(false) }
    var displayNameDraft by rememberSaveable { mutableStateOf("") }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(20.dp),
        verticalArrangement = Arrangement.Top,
    ) {
        when (val authState = uiState.authState) {
            AuthState.Unavailable -> unavailableCard()
            AuthState.SignedOut -> signedOutForm(uiState = uiState, callbacks = callbacks)
            is AuthState.SignedIn ->
                signedInDashboard(
                    uiState = uiState,
                    authState = authState,
                    onManageAccount = {
                        displayNameDraft = authState.displayName?.ifBlank { "Reader" } ?: "Reader"
                        showEditProfileDialog = true
                    },
                    onSignOut = callbacks.onSignOut,
                    onClearOfflineCache = callbacks.onClearOfflineCache,
                    onDeleteAccount = { showDeleteAccountDialog = true },
                    onOpenReadingHub = onOpenReadingHub,
                    onOpenRecommendations = onOpenRecommendations,
                    onOpenClubs = onOpenClubs,
                    onOpenNotifications = onOpenNotifications,
                    onOpenCreatorMode = onOpenCreatorMode,
                )
        }

        uiState.errorMessage?.takeIf { it.isNotBlank() }?.let { errorMessage ->
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }

    if (showEditProfileDialog) {
        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text("Manage account") },
            text = {
                Column {
                    Text(
                        text = "Update the display name shown on your profile.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = displayNameDraft,
                        onValueChange = { displayNameDraft = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Display name") },
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        callbacks.onUpdateDisplayName(displayNameDraft)
                        showEditProfileDialog = false
                    },
                    enabled = !uiState.isSubmitting,
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            title = { Text("Delete account") },
            text = {
                Text(
                    "This permanently removes your Firebase account. " +
                        "If Firebase requires a recent login, you may need to sign in again before deleting.",
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        callbacks.onDeleteAccount()
                        showDeleteAccountDialog = false
                    },
                    enabled = !uiState.isSubmitting,
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                        ),
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun unavailableCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "Account",
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Firebase Auth is not configured yet.",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Add a valid google-services.json to app/, enable Email/Password auth in Firebase, and rebuild to unlock sign-in and cloud sync.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun signedOutForm(
    uiState: AuthUiState,
    callbacks: AuthCallbacks,
) {
    Text(
        text = "Account",
        style = MaterialTheme.typography.headlineMedium,
    )

    Spacer(modifier = Modifier.height(16.dp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Sign in to sync favorites and history across devices.",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(14.dp))
            OutlinedTextField(
                value = uiState.email,
                onValueChange = callbacks.onEmailChanged,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Email") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = uiState.password,
                onValueChange = callbacks.onPasswordChanged,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = callbacks.onSignIn,
                enabled = !uiState.isSubmitting,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (uiState.isSubmitting) "Signing in..." else "Sign in")
            }

            TextButton(
                onClick = callbacks.onSignUp,
                enabled = !uiState.isSubmitting,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Create account")
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun signedInDashboard(
    uiState: AuthUiState,
    authState: AuthState.SignedIn,
    onManageAccount: () -> Unit,
    onSignOut: () -> Unit,
    onClearOfflineCache: () -> Unit,
    onDeleteAccount: () -> Unit,
    onOpenReadingHub: () -> Unit,
    onOpenRecommendations: () -> Unit,
    onOpenClubs: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenCreatorMode: () -> Unit,
) {
    val displayName = authState.displayName?.takeIf { it.isNotBlank() } ?: "Reader"
    val email = authState.email ?: "No email available"
    val initials = profileInitials(displayName)
    val memberSince = formatMemberSince(authState.createdAtMillis)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Account",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.weight(1f))
        Surface(
            shape = CircleShape,
            color = AccountPanelColor,
        ) {
            IconButton(
                onClick = onManageAccount,
                modifier = Modifier.size(56.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = "Manage account",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(18.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Surface(
            modifier = Modifier.size(108.dp),
            shape = CircleShape,
            color = AccountAccentColor,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = initials,
                    style = MaterialTheme.typography.displaySmall,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = email,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(12.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StatusChip(
                    label = "Sync: On",
                    accent = AccountSuccessColor,
                    containerColor = AccountPanelColor,
                    icon = Icons.Outlined.CheckCircle,
                )
                StatusChip(
                    label = "Plan: Free",
                    containerColor = AccountPanelColor,
                )
                StatusChip(
                    label = memberSince,
                    containerColor = AccountPanelColor,
                    multiLine = true,
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(18.dp))

    FilledTonalButton(
        onClick = onManageAccount,
        shape = RoundedCornerShape(18.dp),
        colors =
            ButtonDefaults.filledTonalButtonColors(
                containerColor = AccountPanelColor,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
    ) {
        Text("Manage account")
    }

    Spacer(modifier = Modifier.height(22.dp))

    StatsGrid(stats = uiState.stats)

    Spacer(modifier = Modifier.height(24.dp))

    SectionTitle("Reader tools")
    SettingsCard(
        rows =
            listOf(
                SettingRowModel(
                    title = "Reading hub",
                    subtitle = "Goals, streaks, progress, quick notes",
                    icon = Icons.Outlined.History,
                    iconContainerColor = Color(0xFFE8E4F2),
                    iconTint = Color(0xFF7E69D0),
                    onClick = onOpenReadingHub,
                ),
                SettingRowModel(
                    title = "Recommendations",
                    subtitle = "Because you liked X",
                    icon = Icons.Outlined.StarBorder,
                    iconContainerColor = Color(0xFFE8E4F2),
                    iconTint = Color(0xFFE0A14A),
                    onClick = onOpenRecommendations,
                ),
                SettingRowModel(
                    title = "Book clubs",
                    subtitle = "Create, join, and manage groups",
                    icon = Icons.Outlined.BookmarkBorder,
                    iconContainerColor = Color(0xFFE8E4F2),
                    iconTint = Color(0xFF5D7BD8),
                    onClick = onOpenClubs,
                ),
                SettingRowModel(
                    title = "Creator mode",
                    subtitle = "Custom books and import pipeline",
                    icon = Icons.Outlined.Edit,
                    iconContainerColor = Color(0xFFE8E4F2),
                    iconTint = AccountAccentColor,
                    onClick = onOpenCreatorMode,
                ),
            ),
    )

    Spacer(modifier = Modifier.height(18.dp))

    SectionTitle("Preferences")
    SettingsCard(
        rows =
            listOf(
                SettingRowModel(
                    title = "Appearance",
                    subtitle = "Theme & typography",
                    icon = Icons.Outlined.Tune,
                    iconContainerColor = Color(0xFFE8E4F2),
                    iconTint = Color(0xFF8C93B7),
                ),
                SettingRowModel(
                    title = "Notifications",
                    subtitle = "Reminders & updates",
                    icon = Icons.Outlined.Notifications,
                    iconContainerColor = Color(0xFFE8E4F2),
                    iconTint = Color(0xFF6B68A8),
                    onClick = onOpenNotifications,
                ),
            ),
    )

    Spacer(modifier = Modifier.height(18.dp))

    SectionTitle("Storage & Sync")
    SettingsCard(
        rows =
            listOf(
                SettingRowModel(
                    title = "Sync",
                    subtitle = "On • Firestore connected",
                    icon = Icons.Outlined.CheckCircle,
                    iconContainerColor = Color(0xFFE8E4F2),
                    iconTint = Color(0xFF8C93B7),
                    trailingText = "On",
                    highlightColor = AccountSuccessColor,
                ),
                SettingRowModel(
                    title = "Offline cache",
                    subtitle = "Used: ${uiState.stats.cachedBooks} books",
                    icon = Icons.Outlined.Storage,
                    iconContainerColor = Color(0xFFE8E4F2),
                    iconTint = Color(0xFF5A74B2),
                    trailingText = "Clear",
                    onClick = onClearOfflineCache,
                ),
            ),
    )

    Spacer(modifier = Modifier.height(18.dp))

    SectionTitle("Support")
    SettingsCard(
        rows =
            listOf(
                SettingRowModel(
                    title = "Help & feedback",
                    subtitle = "Report bugs or request features",
                    icon = Icons.AutoMirrored.Outlined.HelpOutline,
                    iconContainerColor = Color(0xFFE8E4F2),
                    iconTint = Color(0xFF6B68A8),
                ),
                SettingRowModel(
                    title = "About Chapterly",
                    subtitle = "Version, licenses, credits",
                    icon = Icons.Outlined.Info,
                    iconContainerColor = Color(0xFFE8E4F2),
                    iconTint = Color(0xFF6B68A8),
                ),
            ),
    )

    Spacer(modifier = Modifier.height(18.dp))

    SectionTitle("Account actions")
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AccountPanelColor),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Button(
                onClick = onSignOut,
                modifier = Modifier.fillMaxWidth(),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = AccountAccentSoft,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                shape = RoundedCornerShape(18.dp),
            ) {
                Text("Sign out")
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onDeleteAccount,
                modifier = Modifier.fillMaxWidth(),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF4DFE5),
                        contentColor = Color(0xFF8B2F49),
                    ),
                shape = RoundedCornerShape(18.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.DeleteForever,
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Delete account")
            }
        }
    }
}

@Composable
private fun StatsGrid(stats: AccountStatsUi) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                modifier = Modifier.weight(1f),
                title = "Favorites",
                value = stats.favorites,
                icon = Icons.Filled.Favorite,
                accent = Color(0xFFE28A98),
            )
            StatCard(
                modifier = Modifier.weight(1f),
                title = "Lists",
                value = stats.lists,
                icon = Icons.Outlined.BookmarkBorder,
                accent = Color(0xFF5D7BD8),
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                modifier = Modifier.weight(1f),
                title = "Reviews",
                value = stats.reviews,
                icon = Icons.Outlined.StarBorder,
                accent = Color(0xFFE0A14A),
            )
            StatCard(
                modifier = Modifier.weight(1f),
                title = "History",
                value = stats.history,
                icon = Icons.Outlined.History,
                accent = Color(0xFF7E69D0),
            )
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: Int,
    icon: ImageVector,
    accent: Color,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = AccountPanelColor),
        shape = RoundedCornerShape(22.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.45f),
            ) {
                Box(
                    modifier = Modifier.size(42.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accent,
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = value.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.secondary,
        fontWeight = FontWeight.Medium,
    )
}

@Composable
private fun SettingsCard(rows: List<SettingRowModel>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AccountPanelColor),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            rows.forEachIndexed { index, row ->
                SettingsRow(row = row)
                if (index != rows.lastIndex) {
                    Spacer(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(AccountDividerColor),
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsRow(row: SettingRowModel) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = CircleShape,
            color = row.iconContainerColor,
        ) {
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = row.icon,
                    contentDescription = null,
                    tint = row.iconTint,
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = row.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = row.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        row.trailingText?.let { trailingText ->
            if (row.onClick != null) {
                FilledTonalButton(
                    onClick = row.onClick,
                    shape = RoundedCornerShape(18.dp),
                    colors =
                        ButtonDefaults.filledTonalButtonColors(
                            containerColor = AccountAccentSoft,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                ) { Text(trailingText) }
            } else {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color =
                        if (row.highlightColor != null) {
                            AccountSuccessSoft
                        } else {
                            AccountAccentSoft
                        },
                ) {
                    Text(
                        text = trailingText,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        color = row.highlightColor ?: MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        } ?: Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StatusChip(
    label: String,
    accent: Color? = null,
    icon: ImageVector? = null,
    containerColor: Color = AccountPanelColor,
    multiLine: Boolean = false,
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = containerColor,
        border = BorderStroke(1.dp, AccountDividerColor.copy(alpha = 0.7f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = accent ?: MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = if (multiLine) 2 else 1,
            )
        }
    }
}

private fun profileInitials(displayName: String): String {
    return displayName
        .split(" ")
        .filter { part -> part.isNotBlank() }
        .take(2)
        .joinToString(separator = "") { part -> part.first().uppercase() }
        .ifBlank { "R" }
}

private fun formatMemberSince(createdAtMillis: Long?): String {
    val year =
        createdAtMillis?.let { timestamp ->
            SimpleDateFormat("yyyy", Locale.getDefault()).format(Date(timestamp))
        } ?: "Unknown"
    return "Member since $year"
}
