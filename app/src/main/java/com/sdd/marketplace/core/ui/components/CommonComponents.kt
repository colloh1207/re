package com.sdd.marketplace.core.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import com.sdd.marketplace.core.ui.theme.*
import com.valentinilk.shimmer.shimmer

@Composable
fun SddLogo(modifier: Modifier = Modifier, size: Dp = 80.dp) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(size).clip(RoundedCornerShape(size / 4)).background(SddPink),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.ShoppingBag, "Sdd", tint = Color.White, modifier = Modifier.size(size * 0.6f))
        }
        Spacer(Modifier.height(4.dp))
        Text("Sdd", style = MaterialTheme.typography.headlineMedium, color = SddPink, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun VerifiedBadge(modifier: Modifier = Modifier) {
    Icon(
        Icons.Filled.Verified, "Verified",
        tint = SddPink, modifier = modifier.size(16.dp)
    )
}

@Composable
fun SddButton(
    text: String, onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    containerColor: Color = SddPink
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(52.dp),
        enabled = enabled && !isLoading,
        colors = ButtonDefaults.buttonColors(containerColor = containerColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        if (isLoading) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
        else Text(text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun SddOutlineButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(52.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, SddPink),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(text, color = SddPink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun SddTextField(
    value: String, onValueChange: (String) -> Unit,
    label: String, modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isPassword: Boolean = false,
    keyboardOptions: androidx.compose.foundation.text.KeyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default,
    singleLine: Boolean = true
) {
    var showPassword by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value, onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        leadingIcon = leadingIcon,
        trailingIcon = if (isPassword) {
            { IconButton(onClick = { showPassword = !showPassword }) {
                Icon(if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, "Toggle password")
            }}
        } else trailingIcon,
        visualTransformation = if (isPassword && !showPassword) androidx.compose.ui.text.input.PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = keyboardOptions,
        singleLine = singleLine,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = SddPink,
            focusedLabelColor = SddPink,
            cursorColor = SddPink
        )
    )
}

@Composable
fun ProductCard(
    imageUrl: String?,
    title: String,
    price: Double,
    sellerName: String,
    sellerAvatarUrl: String?,
    isVerified: Boolean,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            Box {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = title,
                    modifier = Modifier.fillMaxWidth().height(140.dp),
                    contentScale = ContentScale.Crop
                )
                IconButton(
                    onClick = onFavoriteClick,
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
                ) {
                    Icon(
                        if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        "Favorite",
                        tint = if (isFavorite) SddPink else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Column(Modifier.padding(8.dp)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 2)
                Text("₹${String.format("%.0f", price)}", color = SddPink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = sellerAvatarUrl,
                        contentDescription = sellerName,
                        modifier = Modifier.size(16.dp).clip(CircleShape)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(sellerName, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    if (isVerified) { Spacer(Modifier.width(2.dp)); VerifiedBadge() }
                }
            }
        }
    }
}

@Composable
fun ShimmerProductCard(modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.shimmer()) {
            Box(Modifier.fillMaxWidth().height(140.dp).background(MaterialTheme.colorScheme.surfaceVariant))
            Column(Modifier.padding(8.dp)) {
                Box(Modifier.fillMaxWidth(0.8f).height(14.dp).background(MaterialTheme.colorScheme.surfaceVariant))
                Spacer(Modifier.height(4.dp))
                Box(Modifier.fillMaxWidth(0.4f).height(14.dp).background(MaterialTheme.colorScheme.surfaceVariant))
            }
        }
    }
}

@Composable
fun StarRating(rating: Double, maxStars: Int = 5, size: Dp = 16.dp) {
    Row {
        repeat(maxStars) { i ->
            Icon(
                if (i < rating.toInt()) Icons.Filled.Star else Icons.Filled.StarBorder,
                "Star", tint = StarYellow, modifier = Modifier.size(size)
            )
        }
    }
}

@Composable
fun OnlineIndicator(isOnline: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(10.dp).clip(CircleShape)
            .background(if (isOnline) OnlineGreen else Color.Gray)
    )
}

@Composable
fun ErrorMessage(message: String, onRetry: (() -> Unit)? = null) {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Filled.ErrorOutline, "Error", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(8.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        onRetry?.let {
            Spacer(Modifier.height(16.dp))
            Button(onClick = it, colors = ButtonDefaults.buttonColors(containerColor = SddPink)) { Text("Retry") }
        }
    }
}

@Composable
fun EmptyState(title: String, subtitle: String, icon: @Composable () -> Unit = { Icon(Icons.Filled.Inbox, "Empty", modifier = Modifier.size(64.dp)) }) {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        icon()
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
