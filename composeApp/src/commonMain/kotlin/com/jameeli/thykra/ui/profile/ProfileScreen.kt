package com.jameeli.thykra.ui.profile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jameeli.thykra.auth.AuthViewModel
import com.jameeli.thykra.ui.theme.ThykraColors
import com.jameeli.thykra.ui.theme.LegacyIcons
import com.jameeli.thykra.ui.theme.ThykraIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreenContent(authViewModel: AuthViewModel, onNavigateBack: () -> Unit) {
    var displayName by remember { mutableStateOf("") }
    var isEditing by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = ThykraColors.WarmWhite,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Profile",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = ThykraIcons.Back,
                            contentDescription = "Back",
                            tint = ThykraColors.DeepNavy
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ThykraColors.WarmWhite,
                    titleContentColor = ThykraColors.DeepNavy
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Profile card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ThykraColors.Sandy),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Avatar circle
                    val initial = displayName.firstOrNull()?.uppercase() ?: "?"
                    Surface(
                        shape = CircleShape,
                        color = ThykraColors.SkyBlue,
                        modifier = Modifier.size(80.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = initial,
                                color = Color.White,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    if (isEditing) {
                        OutlinedTextField(
                            value = displayName,
                            onValueChange = { displayName = it },
                            label = { Text("Display Name") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ThykraColors.SkyBlue,
                                focusedLabelColor = ThykraColors.SkyBlue,
                                cursorColor = ThykraColors.SkyBlue
                            )
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { isEditing = false },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ThykraColors.SkyBlue,
                                contentColor = Color.White
                            )
                        ) {
                            Text("Save")
                        }
                    } else {
                        Text(
                            text = displayName.ifEmpty { "No name set" },
                            style = MaterialTheme.typography.headlineMedium,
                            color = ThykraColors.DeepNavy
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { isEditing = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = ThykraColors.SkyBlue
                            )
                        ) {
                            Icon(
                                imageVector = LegacyIcons.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Edit Profile")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Coming Soon: Your Recaps
            ComingSoonCard(
                icon = LegacyIcons.CameraAlt,
                iconTint = ThykraColors.SunriseOrange,
                title = "Your Recaps",
                description = "Relive your best travel moments"
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Coming Soon: Trip Activity
            ComingSoonCard(
                icon = ThykraIcons.Chapters,
                iconTint = ThykraColors.SkyBlue,
                title = "Trip Activity",
                description = "See what's happening across your trips"
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Logout button
            OutlinedButton(
                onClick = { authViewModel.logout() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = ThykraColors.SoftRed
                )
            ) {
                Icon(
                    imageVector = LegacyIcons.Logout,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Logout")
            }
        }
    }
}

@Composable
private fun ComingSoonCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ThykraColors.Sandy),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = iconTint
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = ThykraColors.DeepNavy
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = ThykraColors.MutedSlate
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = ThykraColors.SunriseOrange.copy(alpha = 0.15f)
            ) {
                Text(
                    text = "Coming Soon",
                    style = MaterialTheme.typography.labelSmall,
                    color = ThykraColors.SunriseOrange,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
