package com.rentmanager.app.ui.components

import android.annotation.SuppressLint
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rentmanager.app.ui.theme.InterFontFamily

@Composable
fun StepProgressBar(currentStep: Int, totalSteps: Int = 3, @SuppressLint("ModifierParameter") modifier: Modifier = Modifier) {
    if (totalSteps <= 0) return
    val segmentWidth = if (totalSteps == 2) 156.dp else if (totalSteps == 3) 116.dp else (311 / totalSteps).dp
    Row(modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.Center) {
        repeat(totalSteps) { index ->
            val isActive = (index + 1) <= currentStep
            Box(modifier = Modifier.width(segmentWidth).height(2.dp)
                .clip(RoundedCornerShape(4.dp)).background(if (isActive) Color(0xFF151515) else Color(0xFFD3D3D3)))
            if (index < totalSteps - 1) Spacer(modifier = Modifier.width(3.dp))
        }
    }
}

@Composable
fun BackButton(text: String = "Назад", onClick: () -> Unit, @SuppressLint("ModifierParameter") modifier: Modifier = Modifier) {
    TextButton(onClick = onClick, modifier = modifier) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, text, Modifier.size(24.dp), tint = Color(0xFFFFFFFF))
        Spacer(Modifier.width(4.dp))
        Text(text, color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Normal, letterSpacing = (-0.4).sp)
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, isLoading: Boolean = false) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(modifier = modifier.width(311.dp).height(48.dp).clip(RoundedCornerShape(100.dp))
        .background(if (enabled && !isLoading) Color(0xFF212121) else Color(0xFF212121).copy(alpha = 0.5f))
        .then(if (enabled && !isLoading) Modifier.clickable(interactionSource, indication = null, onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) CircularProgressIndicator(Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
        else Text(text, color = if (enabled) Color.White else Color.White.copy(alpha = 0.6f), fontSize = 16.sp, fontWeight = FontWeight.Medium, letterSpacing = (-0.4).sp, lineHeight = 20.sp)
    }
}

@Composable
fun RoleButton(text: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier.width(172.5.dp).height(54.dp).clip(RoundedCornerShape(100.dp))
        .background(if (isSelected) Color(0xFF212121) else Color.Transparent)
        .then(if (!isSelected) Modifier.border(0.5.dp, Color.Black, RoundedCornerShape(100.dp)) else Modifier)
        .padding(horizontal = 32.dp, vertical = 10.dp).clickable(onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, color = if (isSelected) Color.White else Color(0xFF151515), fontSize = 16.sp, fontWeight = FontWeight.Medium, letterSpacing = (-0.4).sp, lineHeight = 20.sp)
    }
}

@Composable
fun DashboardCard(@DrawableRes iconRes: Int, title: String, showBadge: Boolean = false, onClick: () -> Unit, @SuppressLint("ModifierParameter") modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().height(124.dp).clip(RoundedCornerShape(30.dp)).background(Color(0xFFEFEFEF)).clickable { onClick() }
        .padding(horizontal = 20.dp, vertical = 15.dp), contentAlignment = Alignment.TopCenter
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(50.dp), Alignment.Center) {
                Image(painter = painterResource(iconRes), contentDescription = title, modifier = Modifier.size(50.dp), contentScale = ContentScale.Fit)
                if (showBadge) Box(Modifier.size(12.dp).clip(CircleShape).background(Color(0xFF9ED091)).align(Alignment.TopEnd))
            }
            Spacer(Modifier.height(8.dp))
            Text(title, fontSize = 15.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold, fontFamily = InterFontFamily, color = Color(0xFF212121), letterSpacing = (-0.4).sp, textAlign = TextAlign.Center, maxLines = 2)
        }
    }
}

