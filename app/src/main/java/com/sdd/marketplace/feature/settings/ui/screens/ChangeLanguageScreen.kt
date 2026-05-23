package com.sdd.marketplace.feature.settings.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.sdd.marketplace.core.util.LanguageManager
import com.sdd.marketplace.core.ui.theme.SddPink
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LanguageViewModel @Inject constructor(
    private val languageManager: LanguageManager
) : ViewModel() {
    fun getSaved(): String = languageManager.getSavedLanguage()
    fun save(code: String) = languageManager.saveLanguage(code)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangeLanguageScreen(
    navController: NavController,
    viewModel: LanguageViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var selectedLanguage by remember { mutableStateOf(viewModel.getSaved()) }
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Language", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Filled.ArrowBack, "Back") } }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            item {
                Card(Modifier.fillMaxWidth().padding(16.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Language, "Language", tint = SddPink)
                            Spacer(Modifier.width(8.dp))
                            Text("App Language", fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Select your preferred language. The app will restart to apply the change.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            items(LanguageManager.SUPPORTED_LANGUAGES) { lang ->
                Row(
                    Modifier.fillMaxWidth()
                        .clickable { selectedLanguage = lang.code }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(lang.flag, style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(lang.name, fontWeight = FontWeight.Medium)
                        Text(lang.nativeName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    RadioButton(
                        selected = selectedLanguage == lang.code,
                        onClick = { selectedLanguage = lang.code },
                        colors = RadioButtonDefaults.colors(selectedColor = SddPink)
                    )
                }
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            }
            item {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        viewModel.save(selectedLanguage)
                        navController.popBackStack()
                    },
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SddPink)
                ) {
                    Icon(Icons.Filled.Check, "Apply", modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Apply Language")
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}
