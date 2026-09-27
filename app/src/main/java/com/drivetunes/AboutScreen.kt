package com.drivetunes

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

const val DEV_EMAIL = "hh.programming.services@gmail.com"
private const val WALLET_ADDRESS = "0xE10d0f7747228373Be4BcC102f577B16a2CCcc49"

private const val URL_MAIN_PAGE = "https://www.facebook.com/share/18wHhTEVHB/"
private const val URL_HBH = "https://www.facebook.com/HBH690"
private const val URL_HAZEM = "https://www.facebook.com/profile.php?id=100013074136300"
private const val URL_GITHUB = "https://github.com/H-H-ps/DriveTunes"

// ---------- helpers ----------

@Suppress("DEPRECATION")
private fun appVersion(ctx: Context): String = try {
    ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "1.0"
} catch (e: Exception) {
    "1.0"
}

private fun openUrl(ctx: Context, url: String) {
    try {
        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(ctx, R.string.about_no_link, Toast.LENGTH_SHORT).show()
    }
}

/** Opens the user's mail app with a ready-made message. Returns false if no mail app exists. */
private fun sendFeedback(ctx: Context, text: String): Boolean {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:")
        putExtra(Intent.EXTRA_EMAIL, arrayOf(DEV_EMAIL))
        putExtra(Intent.EXTRA_SUBJECT, ctx.getString(R.string.about_feedback_subject))
        putExtra(Intent.EXTRA_TEXT, text)
    }
    return try {
        ctx.startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        false
    }
}

private fun copyText(ctx: Context, label: String, text: String) {
    ctx.getSystemService(ClipboardManager::class.java)
        ?.setPrimaryClip(ClipData.newPlainText(label, text))
}

// ---------- screen ----------

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val version = remember { appVersion(ctx) }
    var showFeedback by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .background(Bg)
            .systemBarsPadding()
    ) {
        Row(
            Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
            }
            Text(
                stringResource(R.string.about_title),
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HeaderCard(version)

            // Feedback button
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(PanelBg)
                    .clickable { showFeedback = true }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Ic.Chat, contentDescription = null, tint = Violet, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
                Text(
                    stringResource(R.string.about_feedback),
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
            }

            CreditsCard()
            SupportCard()
            OpenSourceCard()
            PrivacyCard()

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showFeedback) {
        FeedbackDialog(onDismiss = { showFeedback = false })
    }
}

// ---------- building blocks ----------

@Composable
private fun PanelCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(PanelBg)
            .padding(16.dp),
        content = content
    )
}

@Composable
private fun SectionTitle(icon: ImageVector, text: String, tint: Color = Violet) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun LinkRow(icon: ImageVector, title: String, subtitle: String?, url: String) {
    val ctx = LocalContext.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .clickable { openUrl(ctx, url) }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Cyan, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            if (subtitle != null) Text(subtitle, color = Muted, fontSize = 12.sp)
        }
        Icon(Ic.OpenNew, contentDescription = null, tint = Muted, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun HeaderCard(version: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(PanelBg)
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(R.drawable.hi),
            contentDescription = "H&H Programming Services",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .width(240.dp)
                .aspectRatio(858f / 414f)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.about_version, version),
            color = Muted,
            fontSize = 14.sp
        )
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.about_developed_by),
            style = TextStyle(
                brush = Brush.horizontalGradient(listOf(Violet, Pink, Cyan)),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

@Composable
private fun CreditsCard() {
    PanelCard {
        SectionTitle(Ic.Bulb, stringResource(R.string.about_idea_impl))
        Spacer(Modifier.height(10.dp))
        LinkRow(Ic.Person, "Mr. HBH", "Facebook", URL_HBH)

        Spacer(Modifier.height(18.dp))
        SectionTitle(Ic.Mail, stringResource(R.string.about_contact))
        Spacer(Modifier.height(10.dp))
        LinkRow(Ic.Flag, stringResource(R.string.about_main_page), "Facebook", URL_MAIN_PAGE)
        Spacer(Modifier.height(8.dp))
        LinkRow(Ic.Person, "Hazem", "Facebook", URL_HAZEM)
    }
}

@Composable
private fun SupportCard() {
    val ctx = LocalContext.current
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(2000)
            copied = false
        }
    }

    PanelCard {
        SectionTitle(Ic.Heart, stringResource(R.string.about_support_title), Pink)
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.about_support_text),
            color = Muted,
            fontSize = 14.sp,
            lineHeight = 21.sp
        )
        Spacer(Modifier.height(16.dp))

        // The QR keeps a white background on purpose: scanners need the contrast.
        Box(
            Modifier
                .align(Alignment.CenterHorizontally)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .padding(10.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.donate_qr),
                contentDescription = stringResource(R.string.about_qr_desc),
                modifier = Modifier.size(200.dp)
            )
        }

        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.about_wallet_label), color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(6.dp))
        SelectionContainer {
            Text(
                WALLET_ADDRESS,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Bg)
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                style = TextStyle(
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    textDirection = TextDirection.Ltr
                )
            )
        }

        Spacer(Modifier.height(10.dp))
        Button(
            onClick = {
                copyText(ctx, "USDT (Ethereum)", WALLET_ADDRESS)
                copied = true
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (copied) Cyan else Violet,
                contentColor = if (copied) Bg else Color.White
            )
        ) {
            Icon(
                if (copied) Ic.Check else Ic.Copy,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(stringResource(if (copied) R.string.about_copied else R.string.about_copy))
        }

        Spacer(Modifier.height(10.dp))
        Text(
            stringResource(R.string.about_network_warning),
            color = Muted,
            fontSize = 12.sp,
            lineHeight = 18.sp
        )
    }
}

@Composable
private fun OpenSourceCard() {
    PanelCard {
        SectionTitle(Ic.Code, stringResource(R.string.about_opensource_title), Cyan)
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.about_opensource_text),
            color = Muted,
            fontSize = 14.sp,
            lineHeight = 21.sp
        )
        Spacer(Modifier.height(12.dp))
        LinkRow(Ic.Code, stringResource(R.string.about_github), "github.com/H-H-ps/DriveTunes", URL_GITHUB)
    }
}

@Composable
private fun PrivacyCard() {
    var open by remember { mutableStateOf(false) }
    PanelCard {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { open = !open },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Ic.Shield, contentDescription = null, tint = Cyan, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                stringResource(R.string.about_privacy_title),
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Icon(
                if (open) Ic.ExpandLess else Ic.ExpandMore,
                contentDescription = null,
                tint = Muted
            )
        }
        AnimatedVisibility(visible = open) {
            Column(
                Modifier.padding(top = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                PolicyBlock(R.string.pp_summary_title, R.string.pp_summary_body)
                PolicyBlock(R.string.pp_access_title, R.string.pp_access_body)
                PolicyBlock(R.string.pp_storage_title, R.string.pp_storage_body)
                PolicyBlock(R.string.pp_third_title, R.string.pp_third_body)
                PolicyBlock(R.string.pp_open_title, R.string.pp_open_body)
                PolicyBlock(R.string.pp_contact_title, R.string.pp_contact_body)
                LinkRow(Ic.Code, stringResource(R.string.about_github), "github.com/H-H-ps/DriveTunes", URL_GITHUB)
            }
        }
    }
}

@Composable
private fun PolicyBlock(@StringRes title: Int, @StringRes body: Int) {
    Column {
        Text(stringResource(title), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text(stringResource(body), color = Muted, fontSize = 13.sp, lineHeight = 20.sp)
    }
}

@Composable
private fun FeedbackDialog(onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PanelBg,
        titleContentColor = Color.White,
        textContentColor = Color.White,
        title = { Text(stringResource(R.string.about_feedback_title), fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text(stringResource(R.string.about_feedback_hint)) },
                minLines = 4,
                maxLines = 8,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Violet,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                    cursorColor = Pink,
                    focusedPlaceholderColor = Muted,
                    unfocusedPlaceholderColor = Muted
                )
            )
        },
        confirmButton = {
            TextButton(
                enabled = text.isNotBlank(),
                onClick = {
                    if (sendFeedback(ctx, text.trim())) onDismiss()
                    else Toast.makeText(ctx, R.string.about_no_mail, Toast.LENGTH_LONG).show()
                }
            ) { Text(stringResource(R.string.about_send)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.about_cancel), color = Muted) }
        }
    )
}
