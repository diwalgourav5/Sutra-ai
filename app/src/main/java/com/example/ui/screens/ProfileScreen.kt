package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AppLanguage
import com.example.data.model.AppThemeMode
import com.example.data.model.AvatarPreset
import com.example.data.model.TextSizeOption
import com.example.data.model.UserProfileEntity
import com.example.ui.components.ProfileAvatarBadge

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    activeProfile: UserProfileEntity,
    allProfiles: List<UserProfileEntity>,
    onSaveProfileDetails: (
        username: String,
        roleOrFocus: String,
        bioOrContext: String,
        avatarPreset: AvatarPreset,
        customAvatarUri: String?
    ) -> Unit,
    onCreateNewProfile: (
        username: String,
        roleOrFocus: String,
        bioOrContext: String,
        avatarPreset: AvatarPreset,
        language: AppLanguage,
        themeMode: AppThemeMode
    ) -> Unit,
    onSwitchProfile: (Long) -> Unit,
    onDeleteProfile: (Long) -> Unit,
    onPickCustomAvatarPhoto: () -> Unit,
    onUpdateTheme: (AppThemeMode) -> Unit,
    onUpdateLanguage: (AppLanguage) -> Unit,
    onUpdateTextSize: (TextSizeOption) -> Unit,
    onUpsertExtensibleAttribute: (String, String) -> Unit,
    onRemoveExtensibleAttribute: (String) -> Unit
) {
    var username by rememberSaveable(activeProfile.id) { mutableStateOf(activeProfile.username) }
    var roleOrFocus by rememberSaveable(activeProfile.id) { mutableStateOf(activeProfile.roleOrFocus) }
    var bioOrContext by rememberSaveable(activeProfile.id) { mutableStateOf(activeProfile.bioOrContext) }
    var selectedAvatar by remember(activeProfile.id, activeProfile.avatarPreset) {
        mutableStateOf(activeProfile.parsedAvatarPreset)
    }

    LaunchedEffect(activeProfile.id, activeProfile.customAvatarUri) {
        if (!activeProfile.customAvatarUri.isNullOrBlank() &&
            activeProfile.parsedAvatarPreset == AvatarPreset.CUSTOM_PHOTO
        ) {
            selectedAvatar = AvatarPreset.CUSTOM_PHOTO
        }
    }

    var showCreateProfileDialog by rememberSaveable { mutableStateOf(false) }
    var showAddAttributeDialog by rememberSaveable { mutableStateOf(false) }

    val customAttributes = remember(activeProfile.extensibleAttributesJson) {
        activeProfile.getExtensibleAttributesMap()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("profile_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Active Profile Overview Banner
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ProfileAvatarBadge(
                        profile = activeProfile.copy(avatarPreset = selectedAvatar.name),
                        size = 74.dp
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = username.ifBlank { "Sutra User" },
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = roleOrFocus.ifBlank { "Learner & Explorer" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)
                            ) {
                                Text(
                                    text = activeProfile.parsedLanguage.displayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)
                            ) {
                                Text(
                                    text = activeProfile.parsedThemeMode.displayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Profile Switcher & Create New Profile
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Profiles (${allProfiles.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Each profile stores its own username, avatar, language & theme",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(
                            onClick = { showCreateProfileDialog = true },
                            modifier = Modifier.testTag("create_new_profile_button")
                        ) {
                            Icon(
                                Icons.Default.PersonAdd,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("New Profile")
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        allProfiles.forEach { prof ->
                            val isCurrent = prof.id == activeProfile.id
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isCurrent) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (isCurrent) 1.5.dp else 1.dp,
                                    color = if (isCurrent) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                    }
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { onSwitchProfile(prof.id) }
                                    .testTag("switch_profile_${prof.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ProfileAvatarBadge(profile = prof, size = 30.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = prof.username,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${prof.parsedLanguage.nativeBadge} • ${prof.parsedThemeMode.name}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (!isCurrent && allProfiles.size > 1) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        IconButton(
                                            onClick = { onDeleteProfile(prof.id) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.DeleteOutline,
                                                contentDescription = "Delete profile",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Avatar Customizer + Gallery Photo Upload
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Choose Avatar",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedButton(
                            onClick = {
                                selectedAvatar = AvatarPreset.CUSTOM_PHOTO
                                onPickCustomAvatarPhoto()
                            },
                            modifier = Modifier.testTag("upload_custom_avatar_button")
                        ) {
                            Icon(
                                Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Gallery Photo")
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AvatarPreset.entries.forEach { preset ->
                            val isSelected = selectedAvatar == preset
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .width(88.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (isSelected) {
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                        } else {
                                            Color.Transparent
                                        }
                                    )
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                        },
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clickable {
                                        selectedAvatar = preset
                                        if (preset == AvatarPreset.CUSTOM_PHOTO &&
                                            activeProfile.customAvatarUri.isNullOrBlank()
                                        ) {
                                            onPickCustomAvatarPhoto()
                                        }
                                    }
                                    .padding(10.dp)
                                    .testTag("avatar_preset_${preset.name.lowercase()}")
                            ) {
                                ProfileAvatarBadge(
                                    profile = activeProfile.copy(avatarPreset = preset.name),
                                    size = 46.dp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = preset.title,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Identity & AI Personalization Details
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Profile Identity & AI Context",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username / Display Name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_username_input")
                    )

                    OutlinedTextField(
                        value = roleOrFocus,
                        onValueChange = { roleOrFocus = it },
                        label = { Text("Role or Study Focus") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_role_input")
                    )

                    // Quick Role Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val roles = listOf(
                            "School / College Student",
                            "Software Developer",
                            "JEE / GATE / Exam Aspirant",
                            "Data Scientist & Researcher",
                            "Writer & Content Creator"
                        )
                        roles.forEach { presetRole ->
                            AssistChip(
                                onClick = { roleOrFocus = presetRole },
                                label = {
                                    Text(presetRole, style = MaterialTheme.typography.labelSmall)
                                }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = bioOrContext,
                        onValueChange = { bioOrContext = it },
                        label = { Text("Personal Bio & Learning Goals (Used to tailor AI answers)") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_bio_input")
                    )

                    Button(
                        onClick = {
                            onSaveProfileDetails(
                                username,
                                roleOrFocus,
                                bioOrContext,
                                selectedAvatar,
                                activeProfile.customAvatarUri
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("save_profile_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Profile Changes", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 5. Profile-Stored Basic Preferences (Theme, Language, Text Size)
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Palette,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Profile Preferences (Theme & Language)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Preferred Response & Voice Language",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppLanguage.entries.forEach { lang ->
                            val selected = activeProfile.parsedLanguage == lang
                            FilterChip(
                                selected = selected,
                                onClick = { onUpdateLanguage(lang) },
                                label = { Text(lang.displayName) },
                                leadingIcon = if (selected) {
                                    {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else {
                                    {
                                        Icon(
                                            Icons.Default.Translate,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                },
                                modifier = Modifier.testTag("profile_lang_${lang.name.lowercase()}")
                            )
                        }
                    }

                    Text(
                        text = "Profile Theme Mode",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppThemeMode.entries.forEach { mode ->
                            val selected = activeProfile.parsedThemeMode == mode
                            FilterChip(
                                selected = selected,
                                onClick = { onUpdateTheme(mode) },
                                label = { Text(mode.displayName) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer
                                ),
                                modifier = Modifier.testTag("profile_theme_${mode.name.lowercase()}")
                            )
                        }
                    }

                    Text(
                        text = "Profile Typography Scale",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextSizeOption.entries.forEach { sizeOpt ->
                            val selected = activeProfile.parsedTextSize == sizeOpt
                            FilterChip(
                                selected = selected,
                                onClick = { onUpdateTextSize(sizeOpt) },
                                label = { Text(sizeOpt.displayName) }
                            )
                        }
                    }
                }
            }
        }

        // 6. Extensible Profile Attributes (Future Expansion Architecture)
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Extension,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Custom Profile Attributes",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Extensible key-value traits shared with Sutra AI",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = { showAddAttributeDialog = true },
                            modifier = Modifier.testTag("add_profile_attribute_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Trait")
                        }
                    }

                    if (customAttributes.isEmpty()) {
                        Text(
                            text = "No custom attributes added yet. Tap 'Add Trait' to store custom learning preferences, target exams, or coding languages.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            customAttributes.forEach { (key, value) ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "$key: ",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = value,
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove attribute $key",
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clickable { onRemoveExtensibleAttribute(key) },
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Create New Profile Dialog
    if (showCreateProfileDialog) {
        var newName by rememberSaveable { mutableStateOf("") }
        var newRole by rememberSaveable { mutableStateOf("Student") }
        var newBio by rememberSaveable { mutableStateOf("") }
        var newAvatar by remember { mutableStateOf(AvatarPreset.QUANTUM_CODER) }
        var newLang by remember { mutableStateOf(AppLanguage.ENGLISH) }
        var newTheme by remember { mutableStateOf(AppThemeMode.DARK) }

        AlertDialog(
            onDismissRequest = { showCreateProfileDialog = false },
            title = { Text("Create New User Profile") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Username") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_profile_username_input")
                    )
                    OutlinedTextField(
                        value = newRole,
                        onValueChange = { newRole = it },
                        label = { Text("Role / Focus") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newBio,
                        onValueChange = { newBio = it },
                        label = { Text("Bio / Study Context") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Avatar:", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AvatarPreset.entries.filter { it != AvatarPreset.CUSTOM_PHOTO }.forEach { preset ->
                            FilterChip(
                                selected = newAvatar == preset,
                                onClick = { newAvatar = preset },
                                label = { Text("${preset.emojiBadge} ${preset.title}") }
                            )
                        }
                    }
                    Text("Language:", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AppLanguage.entries.forEach { lang ->
                            FilterChip(
                                selected = newLang == lang,
                                onClick = { newLang = lang },
                                label = { Text(lang.nativeBadge) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCreateNewProfile(newName, newRole, newBio, newAvatar, newLang, newTheme)
                        showCreateProfileDialog = false
                    },
                    modifier = Modifier.testTag("confirm_create_profile_button")
                ) {
                    Text("Create Profile")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateProfileDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add Extensible Attribute Dialog
    if (showAddAttributeDialog) {
        var attrKey by rememberSaveable { mutableStateOf("") }
        var attrVal by rememberSaveable { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddAttributeDialog = false },
            title = { Text("Add Custom Profile Attribute") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Add any custom preference or personal attribute (e.g. 'Target Exam' = 'JEE Advanced', 'Math Notation' = 'Detailed LaTeX').",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = attrKey,
                        onValueChange = { attrKey = it },
                        label = { Text("Attribute Name (e.g. Target Exam)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("attr_key_input")
                    )
                    OutlinedTextField(
                        value = attrVal,
                        onValueChange = { attrVal = it },
                        label = { Text("Attribute Value (e.g. JEE Advanced)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("attr_value_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (attrKey.isNotBlank() && attrVal.isNotBlank()) {
                            onUpsertExtensibleAttribute(attrKey, attrVal)
                        }
                        showAddAttributeDialog = false
                    },
                    modifier = Modifier.testTag("confirm_add_attr_button")
                ) {
                    Text("Save Trait")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddAttributeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
