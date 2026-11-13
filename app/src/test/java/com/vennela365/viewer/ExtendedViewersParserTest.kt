package com.vennela365.viewer

import androidx.compose.ui.geometry.Offset
import com.vennela365.viewer.parser.DataNodeKind
import com.vennela365.viewer.parser.GeometryAndGeoParser
import com.vennela365.viewer.parser.NotebookOutput
import com.vennela365.viewer.parser.NotebookParser
import com.vennela365.viewer.parser.PublicationParser
import com.vennela365.viewer.parser.StructuredDataParser
import com.vennela365.viewer.ui.screens.PdfAnnotationMemory
import com.vennela365.viewer.ui.screens.PdfBookmarkMemory
import com.vennela365.viewer.ui.screens.PdfInkStroke
import com.vennela365.viewer.ui.screens.PdfViewMode
import com.vennela365.viewer.ui.screens.ShowcasePdfPageTexts
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ExtendedViewersParserTest {

    @Test
    fun pdfProStudio_managesAnnotationsUndoRedoBookmarksAndShowcaseCorpus() {
        PdfAnnotationMemory.resetAllForTest()
        PdfBookmarkMemory.resetAllForTest()
        val fileKey = "sample_test_pdf"

        val stroke1 = PdfInkStroke(
            points = listOf(Offset(0.1f, 0.1f), Offset(0.5f, 0.5f)),
            colorArgb = 0xFFFFD60A.toInt(),
            normalizedStrokeWidth = 0.005f,
            isHighlighter = false
        )
        val stroke2 = PdfInkStroke(
            points = listOf(Offset(0.2f, 0.3f), Offset(0.8f, 0.3f)),
            colorArgb = 0xFF0A84FF.toInt(),
            normalizedStrokeWidth = 0.025f,
            isHighlighter = true
        )

        PdfAnnotationMemory.addStroke(fileKey, 0, stroke1)
        PdfAnnotationMemory.addStroke(fileKey, 0, stroke2)
        assertEquals(2, PdfAnnotationMemory.strokes(fileKey, 0).size)
        assertEquals(1, PdfAnnotationMemory.totalAnnotatedPages(fileKey, 5))

        assertTrue(PdfAnnotationMemory.undo(fileKey, 0))
        assertEquals(1, PdfAnnotationMemory.strokes(fileKey, 0).size)

        assertTrue(PdfAnnotationMemory.redo(fileKey, 0))
        assertEquals(2, PdfAnnotationMemory.strokes(fileKey, 0).size)

        PdfAnnotationMemory.setNote(fileKey, 2, "Review financial table on page 3")
        assertEquals("Review financial table on page 3", PdfAnnotationMemory.note(fileKey, 2)?.text)
        assertEquals(2, PdfAnnotationMemory.totalAnnotatedPages(fileKey, 5))

        PdfAnnotationMemory.clearPage(fileKey, 0)
        assertTrue(PdfAnnotationMemory.strokes(fileKey, 0).isEmpty())

        assertTrue(PdfBookmarkMemory.toggle(fileKey, 3))
        assertTrue(PdfBookmarkMemory.isBookmarked(fileKey, 3))
        assertEquals(listOf(3), PdfBookmarkMemory.bookmarkedPages(fileKey))
        assertFalse(PdfBookmarkMemory.toggle(fileKey, 3))
        assertFalse(PdfBookmarkMemory.isBookmarked(fileKey, 3))

        assertEquals(4, PdfViewMode.entries.size)
        assertEquals(5, ShowcasePdfPageTexts.size)
        assertTrue(ShowcasePdfPageTexts.all { it.isNotBlank() })
    }

    @Test
    fun epubParser_readsSpineOrderAndMetadata() {
        val container = """
            <?xml version="1.0"?>
            <container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
              <rootfiles>
                <rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/>
              </rootfiles>
            </container>
        """.trimIndent()
        assertEquals("OEBPS/content.opf", PublicationParser.extractOpfPathFromContainer(container))

        val opf = """
            <package version="3.0">
              <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
                <dc:title>Moonlit Lake</dc:title>
                <dc:creator>Ada Lovelace</dc:creator>
                <dc:language>en</dc:language>
              </metadata>
              <manifest>
                <item id="ch2" href="text/ch2.xhtml" media-type="application/xhtml+xml"/>
                <item id="ch1" href="text/ch1.xhtml" media-type="application/xhtml+xml"/>
              </manifest>
              <spine>
                <itemref idref="ch1"/>
                <itemref idref="ch2"/>
              </spine>
            </package>
        """.trimIndent()

        val entries = mapOf(
            "OEBPS/text/ch1.xhtml" to "<html><body><h1>Arrival</h1><p>First chapter content goes here cleanly.</p></body></html>",
            "OEBPS/text/ch2.xhtml" to "<html><body><h1>Departure</h1><p>Second chapter follows spine order.</p></body></html>"
        )

        val doc = PublicationParser.parseEpubContents("OEBPS/content.opf", opf, entries, fallbackTitle = "fallback.epub")
        assertEquals("Moonlit Lake", doc.title)
        assertEquals("Ada Lovelace", doc.author)
        assertEquals("EN", doc.language)
        assertEquals(2, doc.chapters.size)
        assertEquals("Arrival", doc.chapters[0].title)
        assertEquals("Departure", doc.chapters[1].title)
    }

    @Test
    fun fb2Parser_extractsTitleAuthorAndSections() {
        val fb2 = """
            <?xml version="1.0" encoding="utf-8"?>
            <FictionBook>
              <description>
                <title-info>
                  <book-title>Starlight Manual</book-title>
                  <author><first-name>Alan</first-name><last-name>Turing</last-name></author>
                  <lang>en</lang>
                </title-info>
              </description>
              <body>
                <section>
                  <title><p>Part One</p></title>
                  <p>Universal computation begins cleanly here.</p>
                </section>
              </body>
            </FictionBook>
        """.trimIndent()

        val doc = PublicationParser.parseFb2Xml(fb2, "starlight.fb2")
        assertEquals("Starlight Manual", doc.title)
        assertEquals("Alan Turing", doc.author)
        assertEquals(1, doc.chapters.size)
        assertEquals("Part One", doc.chapters[0].title)
        assertTrue(doc.chapters[0].body.contains("Universal computation"))
    }

    @Test
    fun notebookParser_parsesCellsOutputsAndRmd() {
        val ipynb = """
            {
              "nbformat": 4,
              "nbformat_minor": 5,
              "metadata": { "kernelspec": { "display_name": "Python 3", "language": "python" } },
              "cells": [
                { "cell_type": "markdown", "source": ["# Heading\n", "Summary text"] },
                {
                  "cell_type": "code",
                  "execution_count": 3,
                  "source": "print(42)",
                  "outputs": [
                    { "output_type": "stream", "name": "stdout", "text": "42\n" }
                  ]
                }
              ]
            }
        """.trimIndent()

        val nb = NotebookParser.parse(ipynb, "ipynb")
        assertNull(nb.error)
        assertEquals("Python 3", nb.kernelName)
        assertEquals(2, nb.cells.size)
        assertEquals(1, nb.markdownCount)
        assertEquals(1, nb.codeCount)
        assertEquals(3, nb.cells[1].executionCount)
        val out = nb.cells[1].outputs.first() as NotebookOutput.TextStream
        assertEquals("42", out.text)

        val qmd = """
            # Quarto Report
            ```{python}
            x = 1 + 1
            ```
        """.trimIndent()
        val parsedQmd = NotebookParser.parse(qmd, "qmd")
        assertEquals(2, parsedQmd.cells.size)
        assertEquals("code", parsedQmd.cells[1].cellType)
    }

    @Test
    fun structuredData_parsesJsoncYamlAndSvgSanitizesScripts() {
        val jsonc = """
            // User config comment
            {
              "app": "Vennela",
              "features": [true, 365]
            }
        """.trimIndent()
        val res = StructuredDataParser.parseStructuredTree(jsonc, "jsonc")
        assertNull(res.error)
        assertEquals(1, res.roots.size)
        assertEquals(DataNodeKind.OBJECT, res.roots[0].kind)
        assertEquals(2, res.roots[0].children.size)

        val svg = """
            <svg viewBox="0 0 100 100" onload="alert(1)">
              <script>alert('xss')</script>
              <path d="M0 0 L10 10" fill="#38BDF8"/>
              <circle cx="50" cy="50" r="20" stroke="#F43F5E"/>
            </svg>
        """.trimIndent()
        val inspected = StructuredDataParser.inspectAndSanitizeSvg(svg)
        assertFalse(inspected.rawSvg.contains("<script", ignoreCase = true))
        assertFalse(inspected.rawSvg.contains("onload=", ignoreCase = true))
        assertEquals("0 0 100 100", inspected.viewBox)
        assertEquals(1, inspected.pathCount)
        assertEquals(1, inspected.shapeCount)
        assertTrue(inspected.paletteHexes.contains("#38BDF8"))
        assertTrue(inspected.paletteHexes.contains("#F43F5E"))
    }

    @Test
    fun timedTracks_parsesSrtLrcAndM3u() {
        val srt = """
            1
            00:00:01,000 --> 00:00:03,500
            Welcome to Vennela 365

            2
            00:00:04,000 --> 00:00:06,000
            Offline crystal viewer
        """.trimIndent()
        val cues = StructuredDataParser.parseTimedTracks(srt, "srt")
        assertEquals(2, cues.size)
        assertEquals("Welcome to Vennela 365", cues[0].text)

        val lrc = "[00:12.34]Moonlight reflects tonight"
        val lyrics = StructuredDataParser.parseTimedTracks(lrc, "lrc")
        assertEquals(1, lyrics.size)
        assertEquals("00:12.34", lyrics[0].timeRange)

        val m3u = "#EXTM3U\n#EXTINF:123,Ambient Track\nhttps://example.com/audio.mp3"
        val playlist = StructuredDataParser.parseTimedTracks(m3u, "m3u")
        assertEquals(1, playlist.size)
        assertEquals("Ambient Track", playlist[0].speakerOrMeta)
    }

    @Test
    fun geometryAndGeo_parsesObjAndGeoJson() {
        val obj = """
            v 0 0 0
            v 1 0 0
            v 0 1 0
            f 1 2 3
        """.trimIndent()
        val mesh = GeometryAndGeoParser.parseMesh(obj, "obj")
        assertFalse(mesh.isBinaryOrUnsupported)
        assertEquals(3, mesh.vertices.size)
        assertEquals(1, mesh.faceCount)
        assertEquals(3, mesh.edges.size)

        val geojson = """
            {
              "type": "FeatureCollection",
              "features": [
                {
                  "type": "Feature",
                  "properties": { "name": "Summit" },
                  "geometry": { "type": "Point", "coordinates": [-122.4194, 37.7749, 100.0] }
                },
                {
                  "type": "Feature",
                  "properties": { "name": "Trail" },
                  "geometry": { "type": "LineString", "coordinates": [[-122.4194, 37.7749], [-122.4094, 37.7849]] }
                }
              ]
            }
        """.trimIndent()
        val geo = GeometryAndGeoParser.parseGeo(geojson, "geojson")
        assertEquals(1, geo.waypoints.size)
        assertEquals(1, geo.polylines.size)
        assertTrue(geo.totalDistanceKm > 0.5)
    }
}
