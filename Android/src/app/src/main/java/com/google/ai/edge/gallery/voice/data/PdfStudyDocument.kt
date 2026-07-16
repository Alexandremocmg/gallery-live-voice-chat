package com.google.ai.edge.gallery.voice.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.Locale
import kotlin.math.ln
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

enum class PdfPageKind {
  TEXT,
  SCANNED,
}

data class PdfPageText(
  val pageNumber: Int,
  val text: String,
  val sourceWasScanned: Boolean = false,
) {
  val kind: PdfPageKind
    get() = if (sourceWasScanned) PdfPageKind.SCANNED else PdfPageKind.TEXT
}

data class PdfStudyDocument(
  val displayName: String,
  val sourceUri: Uri,
  val pages: List<PdfPageText>,
) {
  val pageCount: Int
    get() = pages.size

  val scannedPageCount: Int
    get() = pages.count { it.kind == PdfPageKind.SCANNED }

  val hasScannedPages: Boolean
    get() = scannedPageCount > 0

  fun relevantContext(query: String, maxPages: Int = 4): String {
    return selectedPages(query, maxPages).joinToString("\n\n") { page ->
      if (page.text.isBlank()) {
        "[Pagina ${page.pageNumber}: pagina escaneada enviada como imagem ao modelo]"
      } else if (page.sourceWasScanned) {
        "[Pagina ${page.pageNumber}: OCR local de pagina escaneada]\n${page.text.take(MAX_PAGE_CONTEXT_CHARS)}"
      } else {
        "[Pagina ${page.pageNumber}]\n${page.text.take(MAX_PAGE_CONTEXT_CHARS)}"
      }
    }
  }

  fun relevantPageNumbers(query: String, maxPages: Int = 4): List<Int> =
    selectedPages(query, maxPages).map { it.pageNumber }

  fun scannedPageNumbers(query: String, maxPages: Int = 2): List<Int> {
    val selected = selectedPages(query, maxOf(maxPages, 4)).filter { it.kind == PdfPageKind.SCANNED }
    return selected.take(maxPages).ifEmpty {
      pages.filter { it.kind == PdfPageKind.SCANNED }.take(maxPages)
    }.map { it.pageNumber }
  }

  private fun selectedPages(query: String, maxPages: Int): List<PdfPageText> {
    if (pages.isEmpty()) return emptyList()
    val scored = scorePages(query)
    val positive = scored.filter { it.second > 0.0 }
    return (positive.ifEmpty { scored }).take(maxPages).map { it.first }
  }

  private fun scorePages(query: String): List<Pair<PdfPageText, Double>> {
    val queryTerms = tokenize(query).distinct()
    if (queryTerms.isEmpty()) return pages.map { it to 0.0 }
    val pageTerms = pages.associateWith { tokenize(it.text) }
    val averageLength = pageTerms.values.map { it.size }.average().takeIf { !it.isNaN() && it > 0 } ?: 1.0
    val documentCount = pages.size.toDouble().coerceAtLeast(1.0)

    return pages
      .map { page ->
        val terms = pageTerms.getValue(page)
        val frequencies = terms.groupingBy { it }.eachCount()
        val score = queryTerms.sumOf { term ->
          val termFrequency = frequencies[term]?.toDouble() ?: 0.0
          if (termFrequency == 0.0) return@sumOf 0.0
          val documentFrequency = pageTerms.values.count { term in it }.toDouble()
          val inverseDocumentFrequency = ln(1.0 + (documentCount - documentFrequency + 0.5) / (documentFrequency + 0.5))
          val lengthNormalization = termFrequency + BM25_K1 * (1.0 - BM25_B + BM25_B * terms.size / averageLength)
          inverseDocumentFrequency * (termFrequency * (BM25_K1 + 1.0)) / lengthNormalization
        }
        page to score
      }
      .sortedWith(compareByDescending<Pair<PdfPageText, Double>> { it.second }.thenBy { it.first.pageNumber })
  }

  private fun tokenize(value: String): List<String> =
    value
      .lowercase(Locale.ROOT)
      .split(Regex("[^\\p{L}\\p{N}]+"))
      .filter { it.length >= 3 }
}

object PdfStudyDocumentReader {
  suspend fun read(context: Context, uri: Uri, fallbackName: String): PdfStudyDocument =
    withContext(Dispatchers.IO) {
      PDFBoxResourceLoader.init(context.applicationContext)
      val input = context.contentResolver.openInputStream(uri)
        ?: error("Nao foi possivel abrir o PDF selecionado.")

      val extractedDocument = input.use { stream ->
        PDDocument.load(stream).use { document ->
          val stripper = PDFTextStripper()
          val pages = buildList {
            for (pageNumber in 1..document.numberOfPages) {
              stripper.startPage = pageNumber
              stripper.endPage = pageNumber
              val text = stripper.getText(document).trim()
              add(
                PdfPageText(
                  pageNumber = pageNumber,
                  text = text,
                  sourceWasScanned = text.isBlank(),
                )
              )
            }
          }
          if (pages.isEmpty()) error("O PDF nao possui paginas legiveis.")
          PdfStudyDocument(
            displayName = fallbackName.ifBlank { "Documento PDF" },
            sourceUri = uri,
            pages = pages,
          )
        }
      }

      val scannedPages =
        extractedDocument.pages.filter { it.sourceWasScanned }.take(MAX_OCR_PAGES)
      if (scannedPages.isEmpty()) return@withContext extractedDocument

      val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
      try {
        val ocrTextByPage = mutableMapOf<Int, String>()
        scannedPages.forEach { page ->
          val bitmap =
            renderScannedPages(
              context = context,
              document = extractedDocument,
              pageNumbers = listOf(page.pageNumber),
              maxDimension = OCR_RENDER_DIMENSION,
            ).firstOrNull() ?: return@forEach
          try {
            ocrTextByPage[page.pageNumber] = recognizeText(recognizer, bitmap)
          } finally {
            bitmap.recycle()
          }
        }
        extractedDocument.copy(
          pages =
            extractedDocument.pages.map { page ->
              page.copy(text = ocrTextByPage[page.pageNumber]?.trim().orEmpty().ifBlank { page.text })
            }
        )
      } finally {
        recognizer.close()
      }
    }

  private suspend fun recognizeText(recognizer: TextRecognizer, bitmap: Bitmap): String =
    suspendCancellableCoroutine { continuation ->
      recognizer
        .process(InputImage.fromBitmap(bitmap, 0))
        .addOnSuccessListener { result ->
          if (continuation.isActive) continuation.resume(result.text)
        }
        .addOnFailureListener {
          if (continuation.isActive) continuation.resume("")
        }
    }

  suspend fun renderScannedPages(
    context: Context,
    document: PdfStudyDocument,
    pageNumbers: List<Int>,
    maxDimension: Int = 1_280,
  ): List<Bitmap> = withContext(Dispatchers.IO) {
    if (pageNumbers.isEmpty()) return@withContext emptyList()
    val descriptor = context.contentResolver.openFileDescriptor(document.sourceUri, "r")
      ?: return@withContext emptyList()
    descriptor.use { file ->
      PdfRenderer(file).use { renderer ->
        val rendered = mutableListOf<Bitmap>()
        try {
          pageNumbers.distinct().forEach { pageNumber ->
            val index = pageNumber - 1
            if (index !in 0 until renderer.pageCount) return@forEach
            renderer.openPage(index).use { page ->
              val scale = (maxDimension.toFloat() / maxOf(page.width, page.height)).coerceAtMost(2f)
              val width = (page.width * scale).toInt().coerceAtLeast(1)
              val height = (page.height * scale).toInt().coerceAtLeast(1)
              val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
              try {
                bitmap.eraseColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                rendered.add(bitmap)
              } catch (error: Throwable) {
                bitmap.recycle()
                throw error
              }
            }
          }
          rendered
        } catch (error: Throwable) {
          rendered.forEach { bitmap -> if (!bitmap.isRecycled) bitmap.recycle() }
          throw error
        }
      }
    }
  }
}

private const val MAX_PAGE_CONTEXT_CHARS = 2_000
private const val BM25_K1 = 1.2
private const val BM25_B = 0.75
private const val MAX_OCR_PAGES = 40
private const val OCR_RENDER_DIMENSION = 1_600
