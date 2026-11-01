package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.R
import com.example.data.AppRepository
import com.example.ui.components.NewUserBadgePill
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationScreen(
    repository: AppRepository,
    onRegistrationComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val currentUser by repository.currentUser.collectAsState()

    var displayName by remember { mutableStateOf(currentUser.fullName) }
    var username by remember { mutableStateOf(currentUser.username) }
    var bio by remember { mutableStateOf(currentUser.bio) }

    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedAvatarUriString by remember { mutableStateOf(currentUser.avatarUrl) }

    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showCameraRationale by remember { mutableStateOf(false) }

    // Camera capture launcher (direct Bitmap capture from hardware camera)
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            capturedBitmap = bitmap
            // Persist captured camera bitmap to application cache file
            try {
                val cacheFile = File(context.cacheDir, "camera_avatar_${System.currentTimeMillis()}.jpg")
                FileOutputStream(cacheFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
                }
                selectedAvatarUriString = Uri.fromFile(cacheFile).toString()
                errorMessage = null
            } catch (e: Exception) {
                errorMessage = "Failed to save camera image: ${e.message}"
            }
        }
    }

    // Camera permission request launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showCameraRationale = false
            takePictureLauncher.launch(null)
        } else {
            showCameraRationale = true
            Toast.makeText(context, context.getString(R.string.permission_camera_rationale), Toast.LENGTH_LONG).show()
        }
    }

    // Optional Gallery Picker fallback
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedAvatarUriString = uri.toString()
            capturedBitmap = null
            errorMessage = null
        }
    }

    fun requestCamera() {
        val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            takePictureLauncher.launch(null)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Validation
    val isDisplayNameValid = displayName.trim().isNotBlank() && displayName.length <= 40
    val isUsernameValid = username.matches(Regex("^[a-zA-Z0-9_]{3,20}$"))
    val canSubmit = isDisplayNameValid && isUsernameValid && !isSubmitting

    Scaffold(
        containerColor = LuxDarkBackground,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header / Brand Title
            Spacer(modifier = Modifier.height(10.dp))
            NewUserBadgePill()
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.title_registration),
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
            )

            Text(
                text = stringResource(R.string.subtitle_registration),
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp, bottom = 24.dp)
            )

            // Avatar Camera Selector
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                // Outer glow border
                Box(
                    modifier = Modifier
                        .size(136.dp)
                        .clip(CircleShape)
                        .border(
                            width = 3.dp,
                            brush = Brush.sweepGradient(
                                listOf(LuxGoldBright, LuxNeonPurple, LuxCyan, LuxGoldBright)
                            ),
                            shape = CircleShape
                        )
                        .background(LuxCardBackground),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        capturedBitmap != null -> {
                            Image(
                                bitmap = capturedBitmap!!.asImageBitmap(),
                                contentDescription = "Captured Profile Photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .testTag("reg_avatar_preview_captured")
                            )
                        }
                        selectedAvatarUriString.isNotBlank() -> {
                            AsyncImage(
                                model = selectedAvatarUriString,
                                contentDescription = "Profile Avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .testTag("reg_avatar_preview_image")
                            )
                        }
                        else -> {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Default Profile",
                                tint = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(64.dp)
                            )
                        }
                    }
                }

                // Camera Action Floating Button
                Surface(
                    color = LuxNeonPurple,
                    shape = CircleShape,
                    border = BorderStroke(2.dp, Color.White),
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (-4).dp, y = (-4).dp)
                        .size(44.dp)
                        .clickable { requestCamera() }
                        .testTag("reg_camera_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = stringResource(R.string.btn_take_photo),
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Quick Photo Actions Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(bottom = 20.dp)
            ) {
                FilledTonalButton(
                    onClick = { requestCamera() },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = LuxSurfaceDark,
                        contentColor = LuxGoldBright
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("reg_take_photo_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.btn_take_photo),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = {
                        galleryLauncher.launch(
                            androidx.activity.result.PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    },
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("reg_gallery_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Gallery",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp
                    )
                }
            }

            // Camera Permission Rationale Banner
            AnimatedVisibility(visible = showCameraRationale) {
                Surface(
                    color = LuxRed.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, LuxRed.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = LuxRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.permission_camera_rationale),
                            color = Color.White,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { requestCamera() }) {
                            Text("Grant", color = LuxGoldBright, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Display Name Input
            OutlinedTextField(
                value = displayName,
                onValueChange = {
                    if (it.length <= 40) displayName = it
                    errorMessage = null
                },
                label = { Text(stringResource(R.string.label_display_name)) },
                placeholder = { Text(stringResource(R.string.placeholder_display_name)) },
                leadingIcon = {
                    Icon(Icons.Default.Person, contentDescription = null, tint = LuxNeonPurple)
                },
                trailingIcon = {
                    if (isDisplayNameValid) {
                        Icon(Icons.Default.Check, contentDescription = "Valid", tint = LuxEmerald)
                    }
                },
                isError = displayName.isNotBlank() && !isDisplayNameValid,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LuxNeonPurple,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedLabelColor = LuxNeonPurple,
                    unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = LuxSurfaceDark,
                    unfocusedContainerColor = LuxSurfaceDark
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp)
                    .testTag("reg_display_name_input")
            )

            // Username Input
            OutlinedTextField(
                value = username,
                onValueChange = {
                    val sanitized = it.filter { ch -> ch.isLetterOrDigit() || ch == '_' }
                    if (sanitized.length <= 20) username = sanitized
                    errorMessage = null
                },
                label = { Text(stringResource(R.string.label_username)) },
                placeholder = { Text(stringResource(R.string.placeholder_username)) },
                leadingIcon = {
                    Icon(Icons.Default.AlternateEmail, contentDescription = null, tint = LuxGold)
                },
                trailingIcon = {
                    if (isUsernameValid) {
                        Icon(Icons.Default.Check, contentDescription = "Valid", tint = LuxEmerald)
                    }
                },
                supportingText = {
                    if (username.isNotBlank() && !isUsernameValid) {
                        Text(
                            text = stringResource(R.string.error_username_invalid),
                            color = LuxRed,
                            fontSize = 11.sp
                        )
                    } else {
                        Text(
                            text = "Visible as @${username.ifBlank { "username" }} across lounges & chatrooms",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp
                        )
                    }
                },
                isError = username.isNotBlank() && !isUsernameValid,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Ascii,
                    imeAction = ImeAction.Next
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LuxGold,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedLabelColor = LuxGold,
                    unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = LuxSurfaceDark,
                    unfocusedContainerColor = LuxSurfaceDark
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp)
                    .testTag("reg_username_input")
            )

            // Bio Input (Optional)
            OutlinedTextField(
                value = bio,
                onValueChange = {
                    if (it.length <= 160) bio = it
                },
                label = { Text(stringResource(R.string.label_bio)) },
                placeholder = { Text(stringResource(R.string.placeholder_bio)) },
                supportingText = {
                    Text(
                        text = "${bio.length}/160",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 11.sp,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                minLines = 2,
                maxLines = 3,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LuxCyan,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedLabelColor = LuxCyan,
                    unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = LuxSurfaceDark,
                    unfocusedContainerColor = LuxSurfaceDark
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .testTag("reg_bio_input")
            )

            // Error Banner
            AnimatedVisibility(
                visible = errorMessage != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    color = LuxRed.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, LuxRed),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = Color(0xFFFCA5A5),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Submit Button
            Button(
                onClick = {
                    focusManager.clearFocus()
                    if (!canSubmit) {
                        if (!isDisplayNameValid) errorMessage = context.getString(R.string.error_display_name_required)
                        else if (!isUsernameValid) errorMessage = context.getString(R.string.error_username_invalid)
                        return@Button
                    }

                    isSubmitting = true
                    coroutineScope.launch {
                        val result = repository.registerUserProfile(
                            displayName = displayName,
                            username = username,
                            avatarUri = selectedAvatarUriString,
                            bio = bio
                        )
                        isSubmitting = false
                        if (result.isSuccess) {
                            Toast.makeText(context, "Welcome to NOICE LIVE, @$username!", Toast.LENGTH_SHORT).show()
                            onRegistrationComplete()
                        } else {
                            errorMessage = result.exceptionOrNull()?.localizedMessage
                                ?: "Registration failed. Please check network."
                        }
                    }
                },
                enabled = canSubmit,
                colors = ButtonDefaults.buttonColors(
                    containerColor = LuxGold,
                    contentColor = Color.Black,
                    disabledContainerColor = Color.White.copy(alpha = 0.12f),
                    disabledContentColor = Color.White.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("reg_submit_button")
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        color = Color.Black,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Saving Profile...",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = stringResource(R.string.btn_complete_registration),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.3.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
