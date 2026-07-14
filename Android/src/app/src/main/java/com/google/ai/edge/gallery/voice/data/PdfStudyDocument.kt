package com.google.ai.edge.gallery.voice.data

import android.content.Context
import android.net.Uri
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

data class PdfPageText(
  val pageNumber: Int,
  val text: String,
)

data class PdfStudyDocument(
  val displayName: String,
  val pages: List<PdfPageText>,
) {
  val pageCount: Int
    get() = pages.size

  fun relevantContext(query: String, maxPages: Int = 4): String {
    val terms = query
      .lowercase(Locale.ROOT)
      .split(Regex("[^\\p{L}\\p{N}]+"))
      .filter { it.length >= 3 }
      .distinct()

    val scoredPages = pages
      .map { page ->
        val normalizedText = page.text.lowercase(Locale.ROOT)
        val score = terms.sumOf { term ->
          Regex("\\b${Regex.escape(term)}\\b").findAll(normalizedText).count()
        }
        page to score
      }
      .sortedWith(compareByDescending<Pair<PdfPageText, Int>> { it.second }.thenBy { it.first.pageNumber })

    val selected = scoredPages
      .filter { it.second > 0 }
      .take(maxPages)
      .ifEmpty { scoredPages.take(maxPages) }

    return selected.joinToString("\n\n") { (page, _) ->
      "[Pagina ${page.pageNumber}]\n${page.text.take(6000)}"
    }
  }
}

object PdfStudyDocumentReader {
  suspend fun read(context: Context, uri: Uri, fallbackName: String): PdfStudyDocument =
    withContext(Dispatchers.IO) {
      PDFBoxResourceLoader.init(context.applicationContext)
      val input = context.contentResolver.openInputStream(uri)
        ?: error("Nao foi possivel abrir o PDF selecionado.")

      input.use { stream ->
        PDDocument.load(stream).use { document ->
          val stripper = PDFTextStripper()
          val pages = buildList {
            for (pageNumber in 1..document.numberOfPages) {
              stripper.startPage = pageNumber
              stripper.endPage = pageNumber
              val text = stripper.getText(document).trim()
              if (text.isNotBlank()) {
                add(PdfPageText(pageNumber = pageNumber, text = text))
              }
            }
          }

          if (pages.isEmpty()) {
            error("Este PDF nao possui texto selecionavel. PDFs escaneados ficarao para uma etapa futura.")
          }

          PdfStudyDocument(
            displayName = fallbackName.ifBlank { "Documento PDF" },
            pages = pages,
          )
        }
      }
    }
}
