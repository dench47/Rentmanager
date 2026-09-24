package com.rentmanager.app.ui.landlord.tenants

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rentmanager.app.R
import com.rentmanager.app.data.model.TenantDocumentDto
import com.rentmanager.app.ui.theme.Graphite
import com.rentmanager.app.ui.theme.GreyText
import com.rentmanager.app.ui.theme.Headline2MobStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

private val DocCardGrey = Color(0xFFEFEFEF)
private val DocDividerGrey = Color(0xFFDBDBDB)
private val DocDate = DateTimeFormatter.ofPattern("dd.MM.yyyy")

/** Тип документа для подписи: JPG, PDF… (с сервера либо по расширению имени). */
fun docTypeLabel(doc: TenantDocumentDto): String {
    val explicit = doc.fileType?.trim().orEmpty()
    if (explicit.isNotEmpty()) return explicit.uppercase()
    val ext = doc.name.substringAfterLast('.', "")
    return if (ext.isEmpty()) "ФАЙЛ" else ext.uppercase()
}

/** «1 файл» / «2 файла» / «5 файлов» — счётчик кнопки в карточке (2983:42232). */
fun pluralFiles(n: Int): String {
    val mod10 = n % 10
    val mod100 = n % 100
    val word = when {
        mod10 == 1 && mod100 != 11 -> "файл"
        mod10 in 2..4 && mod100 !in 12..14 -> "файла"
        else -> "файлов"
    }
    return "" + n + " " + word
}

/** Подпись строки документа: «JPG · добавлен 09.09.2026» (дата — CreatedAt). */
fun docSubtitle(doc: TenantDocumentDto): String {
    val date = doc.createdAt?.let { iso ->
        runCatching { OffsetDateTime.parse(iso).toLocalDate() }
            .recoverCatching { LocalDate.parse(iso.take(10)) }
            .getOrNull()
    } ?: LocalDate.now()
    return docTypeLabel(doc) + " · добавлен " + date.format(DocDate)
}

/** Имя файла из системного пикера (галерея/файл). */
fun docDisplayName(context: Context, uri: Uri): String = runCatching {
    context.contentResolver.query(
        uri,
        arrayOf(android.provider.OpenableColumns.DISPLAY_NAME, android.provider.OpenableColumns.SIZE),
        null, null, null
    )?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
}.getOrNull().orEmpty()

private fun mimeForName(name: String): String = when (name.substringAfterLast('.', "").lowercase()) {
    "jpg", "jpeg" -> "image/jpeg"
    "png" -> "image/png"
    "webp" -> "image/webp"
    "heic" -> "image/heic"
    "pdf" -> "application/pdf"
    else -> "*/*"
}

/**
 * Открытие документа системным просмотрщиком (аннотация Вики 2983:44932):
 * ссылку S3 напрямую не открыть — файл скачивается в кэш, затем ACTION_VIEW
 * с content:// через FileProvider (cache-path в file_paths.xml уже расшарен).
 */
suspend fun openDocumentFromUrl(context: Context, url: String, fileName: String): Boolean =
    withContext(Dispatchers.IO) {
        runCatching {
            // Стейдж (ещё не прикреплённый файл): content:// из пикера или
            // FileProvider-ссылка камеры — открываем напрямую, без скачивания
            if (url.startsWith("content://") || url.startsWith("file://")) {
                val local = Uri.parse(url)
                val localIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(local, mimeForName(fileName))
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(localIntent)
                return@runCatching true
            }
            val dir = File(context.cacheDir, "docs").apply { mkdirs() }
            val safe = fileName
                .map { ch -> if (ch.isLetterOrDigit() || ch in "._- ") ch else '_' }
                .joinToString("")
                .ifBlank { "document" }
            val file = File(dir, safe)
            if (!file.exists() || file.length() == 0L) {
                java.net.URL(url).openStream().use { input ->
                    file.outputStream().use { out -> input.copyTo(out) }
                }
            }
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context, context.packageName + ".fileprovider", file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeForName(safe))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }

/**
 * Строка документа (2983:42232/44896): превью 40×40 r8 #EFEFEF с глифом +
 * 8 + имя 15/600 (одна строка, многоточие — аннотация 2983:45726) + 4 +
 * подпись 13/400; в редактировании справа корзина 24 (тап — удаление сразу),
 * в шите корзины нет (колонка шире). Низ строки — дивайдер #DBDBDB через 12.
 */
@Composable
fun DocumentRow(
    doc: TenantDocumentDto,
    onOpen: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DocCardGrey),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Image(
                    painter = painterResource(R.drawable.ic_doc_thumb),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    doc.name,
                    style = Headline2MobStyle.copy(lineHeight = 18.2.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    docSubtitle(doc),
                    fontSize = 13.sp,
                    lineHeight = 15.7.sp,
                    letterSpacing = (-0.4).sp,
                    color = GreyText,
                    maxLines = 1
                )
            }
            if (onDelete != null) {
                Spacer(Modifier.width(10.dp))
                Icon(
                    painter = painterResource(R.drawable.ic_trash_graphite),
                    contentDescription = "Удалить документ",
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onDelete() }
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = DocDividerGrey)
    }
}

/**
 * Шит «Документы» (2983:44896): каркас TenantActionModalSheet (ручка 16+4+16,
 * поля 20/20/36), заголовок 20/600, зазор 20, строки 52 (40 + 12 + дивайдер).
 * Тап по строке — открытие документа (аннотация 2983:44932).
 */
@Composable
fun TenantDocumentsSheet(
    documents: List<TenantDocumentDto>,
    onOpen: (TenantDocumentDto) -> Unit,
    onDismiss: () -> Unit
) {
    TenantActionModalSheet(onDismiss = onDismiss) {
        Text(
            "Документы",
            fontSize = 20.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.3).sp,
            color = Graphite
        )
        Spacer(Modifier.height(20.dp))
        // Длинный список не должен вылезать за экран — шит остаётся прокручиваемым
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            documents.forEach { doc ->
                DocumentRow(doc = doc, onOpen = { onOpen(doc) })
            }
        }
    }
}
