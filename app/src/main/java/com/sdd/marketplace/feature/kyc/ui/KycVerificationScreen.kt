package com.sdd.marketplace.feature.kyc.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.sdd.marketplace.core.ui.components.SddButton
import com.sdd.marketplace.core.ui.components.SddTextField
import com.sdd.marketplace.core.ui.theme.SddPink
import com.sdd.marketplace.domain.model.*
import com.sdd.marketplace.feature.kyc.viewmodel.KycEvent
import com.sdd.marketplace.feature.kyc.viewmodel.KycViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KycVerificationScreen(navController: NavController, viewModel: KycViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is KycEvent.ShowMessage -> {
                    snackbarHostState.showSnackbar(event.message)
                    navController.popBackStack()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Filled.ArrowBack, "Back") } },
                title = { Text("KYC Verification", fontWeight = FontWeight.Bold) }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            uiState.submission?.let { submission ->
                if (submission.status != KycStatus.NOT_SUBMITTED) {
                    KycStatusCard(submission)
                    return@Column
                }
            }
            KycStepContent(
                step = uiState.step,
                uiState = uiState,
                viewModel = viewModel
            )
        }
    }
}

@Composable
fun KycStatusCard(submission: KycSubmission) {
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        val (icon, color) = when (submission.status) {
            KycStatus.PENDING -> Pair(Icons.Outlined.HourglassTop, SddPink)
            KycStatus.APPROVED -> Pair(Icons.Filled.Verified, androidx.compose.ui.graphics.Color(0xFF4CAF50))
            KycStatus.REJECTED -> Pair(Icons.Filled.Cancel, MaterialTheme.colorScheme.error)
            else -> Pair(Icons.Outlined.Info, MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(icon, "Status", tint = color, modifier = Modifier.size(72.dp))
        Spacer(Modifier.height(16.dp))
        Text(submission.status.label, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = color)
        Spacer(Modifier.height(8.dp))
        Text(
            when (submission.status) {
                KycStatus.PENDING -> "Your documents are under review. This usually takes 1-3 business days."
                KycStatus.APPROVED -> "Your identity has been verified. You are now a trusted seller!"
                KycStatus.REJECTED -> "Your submission was rejected. ${submission.rejectionReason ?: "Please resubmit with valid documents."}"
                else -> ""
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun KycStepContent(step: Int, uiState: com.sdd.marketplace.feature.kyc.viewmodel.KycUiState, viewModel: KycViewModel) {
    val frontLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.setFrontImage(it) }
    }
    val backLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.setBackImage(it) }
    }
    val selfieLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.setSelfieImage(it) }
    }

    // Progress indicator
    LinearProgressIndicator(
        progress = { (step + 1).toFloat() / 3f },
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
        color = SddPink
    )
    Text("Step ${step + 1} of 3", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 24.dp))
    Spacer(Modifier.height(8.dp))

    when (step) {
        0 -> PersonalInfoStep(uiState, viewModel)
        1 -> DocumentUploadStep(uiState, viewModel, frontLauncher, backLauncher)
        2 -> SelfieStep(uiState, viewModel, selfieLauncher)
    }
}

@Composable
fun PersonalInfoStep(uiState: com.sdd.marketplace.feature.kyc.viewmodel.KycUiState, viewModel: KycViewModel) {
    var legalName by remember { mutableStateOf(uiState.personalInfo.legalFullName) }
    var dob by remember { mutableStateOf(uiState.personalInfo.dateOfBirth) }
    var nationality by remember { mutableStateOf(uiState.personalInfo.nationality) }
    var address by remember { mutableStateOf(uiState.personalInfo.addressLine1) }
    var city by remember { mutableStateOf(uiState.personalInfo.city) }
    var state by remember { mutableStateOf(uiState.personalInfo.state) }
    var postalCode by remember { mutableStateOf(uiState.personalInfo.postalCode) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
        Text("Personal Information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("This information must match your official documents exactly.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(20.dp))
        SddTextField(legalName, { legalName = it }, "Legal Full Name")
        Spacer(Modifier.height(12.dp))
        SddTextField(dob, { dob = it }, "Date of Birth (YYYY-MM-DD)")
        Spacer(Modifier.height(12.dp))
        SddTextField(nationality, { nationality = it }, "Nationality")
        Spacer(Modifier.height(12.dp))
        SddTextField(address, { address = it }, "Address Line 1")
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SddTextField(city, { city = it }, "City", modifier = Modifier.weight(1f))
            SddTextField(state, { state = it }, "State", modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        SddTextField(postalCode, { postalCode = it }, "Postal Code")
        Spacer(Modifier.height(24.dp))
        SddButton("Next: Upload Documents", onClick = {
            viewModel.updatePersonalInfo(KycPersonalInfo(
                legalName, dob, "other", nationality, address, null, city, state, postalCode, "IN", null
            ))
            viewModel.nextStep()
        }, enabled = legalName.isNotBlank() && dob.isNotBlank() && address.isNotBlank())
    }
}

@Composable
fun DocumentUploadStep(
    uiState: com.sdd.marketplace.feature.kyc.viewmodel.KycUiState,
    viewModel: KycViewModel,
    frontLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    backLauncher: androidx.activity.result.ActivityResultLauncher<String>
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
        Text("Upload Documents", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("Upload clear, unobstructed photos of your government-issued ID.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(20.dp))
        Text("Document Type", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        KycDocumentType.values().forEach { docType ->
            Row(
                Modifier.fillMaxWidth().clickable { viewModel.selectDocType(docType) }.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = uiState.selectedDocType == docType, onClick = { viewModel.selectDocType(docType) }, colors = RadioButtonDefaults.colors(selectedColor = SddPink))
                Text(docType.label)
            }
        }
        Spacer(Modifier.height(20.dp))
        DocumentUploadBox("Front Side", uiState.frontImageUri) { frontLauncher.launch("image/*") }
        Spacer(Modifier.height(12.dp))
        DocumentUploadBox("Back Side (if applicable)", uiState.backImageUri) { backLauncher.launch("image/*") }
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { viewModel.prevStep() }, modifier = Modifier.weight(1f)) { Text("Back") }
            Button(
                onClick = { viewModel.nextStep() },
                modifier = Modifier.weight(1f),
                enabled = uiState.frontImageUri != null,
                colors = ButtonDefaults.buttonColors(containerColor = SddPink)
            ) { Text("Next: Selfie") }
        }
    }
}

@Composable
fun DocumentUploadBox(label: String, uri: android.net.Uri?, onUpload: () -> Unit) {
    Column {
        Text(label, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(12.dp))
                .border(2.dp, SddPink.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .clickable(onClick = onUpload),
            contentAlignment = Alignment.Center
        ) {
            if (uri != null) {
                AsyncImage(model = uri, contentDescription = label, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.Upload, "Upload", tint = SddPink, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("Tap to upload", color = SddPink)
                }
            }
        }
    }
}

@Composable
fun SelfieStep(
    uiState: com.sdd.marketplace.feature.kyc.viewmodel.KycUiState,
    viewModel: KycViewModel,
    selfieLauncher: androidx.activity.result.ActivityResultLauncher<String>
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
        Text("Take a Selfie", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("Take a clear selfie to confirm your identity matches your documents.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(20.dp))
        DocumentUploadBox("Your Selfie", uiState.selfieUri) { selfieLauncher.launch("image/*") }
        Spacer(Modifier.height(16.dp))
        listOf("Face clearly visible, no glasses or mask", "Good lighting, no flash", "Neutral background", "Same as your ID photo").forEach {
            Row(Modifier.padding(vertical = 2.dp)) {
                Icon(Icons.Filled.Check, "Check", tint = androidx.compose.ui.graphics.Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(it, style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { viewModel.prevStep() }, modifier = Modifier.weight(1f)) { Text("Back") }
            Button(
                onClick = { viewModel.submitKyc() },
                modifier = Modifier.weight(1f),
                enabled = !uiState.isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = SddPink)
            ) {
                if (uiState.isLoading) CircularProgressIndicator(Modifier.size(20.dp), color = androidx.compose.ui.graphics.Color.White)
                else Text("Submit for Review")
            }
        }
        uiState.error?.let { Spacer(Modifier.height(8.dp)); Text(it, color = MaterialTheme.colorScheme.error) }
    }
}
