package com.vennela365.viewer

import android.content.Context
import android.net.Uri
import com.vennela365.viewer.parser.PresentationParser
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * End-to-end tests for the presentation reader: they write real OOXML and
 * OpenDocument archives and assert the text that genuinely comes back out, so
 * the parser is exercised exactly as it is in production.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class PresentationParserTest {

    private val context: Context get() = RuntimeEnvironment.getApplication()

    private fun slideXml(title: String, body: List<String>): String {
        val runs = body.joinToString("") { "<a:p><a:r><a:t>$it</a:t></a:r></a:p>" }
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <p:sld xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main"
                   xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">
              <p:cSld><p:spTree><p:sp><p:txBody>
                <a:p><a:r><a:t>$title</a:t></a:r></a:p>
                $runs
              </p:txBody></p:sp></p:spTree></p:cSld>
            </p:sld>
        """.trimIndent()
    }

    /** Writes a deck. [separator] lets a test mimic a Windows-written archive. */
    private fun writeDeck(
        name: String,
        entries: Map<String, String>,
        separator: Char = '/'
    ): Uri {
        val file = File(context.cacheDir, name)
        file.parentFile?.mkdirs()
        ZipOutputStream(file.outputStream().buffered()).use { zip ->
            entries.forEach { (path, content) ->
                zip.putNextEntry(ZipEntry(path.replace('/', separator)))
                zip.write(content.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
        }
        return Uri.fromFile(file)
    }

    private fun ooxmlDeck(): Uri = writeDeck(
        "deck.pptx",
        mapOf(
            "[Content_Types].xml" to "<Types/>",
            "docProps/core.xml" to
                "<cp:coreProperties xmlns:dc=\"http://purl.org/dc/elements/1.1/\">" +
                "<dc:title>Launch Deck</dc:title><dc:creator>Vennela Team</dc:creator>" +
                "</cp:coreProperties>",
            "ppt/slides/slide1.xml" to slideXml("Opening", listOf("First point", "Second point")),
            "ppt/slides/slide2.xml" to slideXml("Storage", listOf("SD cards and OTG")),
            "ppt/slides/slide10.xml" to slideXml("Closing", listOf("Thank you")),
            "ppt/notesSlides/notesSlide1.xml" to slideXml("Notes", listOf("Mention offline mode"))
        )
    )

    @Test
    fun `parses every slide with its real title and body`() = runTest {
        val deck = PresentationParser.parse(context, ooxmlDeck(), "pptx")
        assertNotNull("a valid pptx must parse", deck)
        assertEquals(3, deck!!.slides.size)
        assertEquals("Opening", deck.slides[0].title)
        assertEquals(listOf("First point", "Second point"), deck.slides[0].body)
        assertEquals("Storage", deck.slides[1].title)
    }

    @Test
    fun `slides are ordered numerically not lexically`() = runTest {
        // slide10 must come after slide2, which a lexical sort would reverse.
        val deck = PresentationParser.parse(context, ooxmlDeck(), "pptx")!!
        assertEquals(listOf("Opening", "Storage", "Closing"), deck.slides.map { it.title })
        assertEquals(listOf(1, 2, 3), deck.slides.map { it.number })
    }

    @Test
    fun `document properties are read from the file`() = runTest {
        val deck = PresentationParser.parse(context, ooxmlDeck(), "pptx")!!
        assertEquals("Launch Deck", deck.title)
        assertEquals("Vennela Team", deck.author)
    }

    @Test
    fun `speaker notes are attached to their slide`() = runTest {
        val deck = PresentationParser.parse(context, ooxmlDeck(), "pptx")!!
        assertTrue(
            "notes must contain the real note text",
            deck.slides[0].notes.orEmpty().contains("Mention offline mode")
        )
    }

    @Test
    fun `slides without notes report none`() = runTest {
        val deck = PresentationParser.parse(context, ooxmlDeck(), "pptx")!!
        assertNull("slide 2 has no notes in the file", deck.slides[1].notes)
    }

    @Test
    fun `archives written with backslash separators still parse`() = runTest {
        // ZIP forbids `\`, but Windows-hosted writers emit it, so the reader
        // normalises entry names instead of silently returning an empty deck.
        val uri = writeDeck(
            "windows-deck.pptx",
            mapOf(
                "ppt/slides/slide1.xml" to slideXml("Windows Writer", listOf("Body line"))
            ),
            separator = '\\'
        )
        val deck = PresentationParser.parse(context, uri, "pptx")
        assertNotNull("a backslash-separated archive must still parse", deck)
        assertEquals("Windows Writer", deck!!.slides[0].title)
    }

    @Test
    fun `markup is unescaped and whitespace collapsed`() = runTest {
        val uri = writeDeck(
            "escaped.pptx",
            mapOf(
                "ppt/slides/slide1.xml" to slideXml(
                    "R&amp;D    report",
                    listOf("a &lt; b &amp;&amp; c &gt; d")
                )
            )
        )
        val deck = PresentationParser.parse(context, uri, "pptx")!!
        assertEquals("R&D report", deck.slides[0].title)
        assertEquals(listOf("a < b && c > d"), deck.slides[0].body)
    }

    @Test
    fun `a slide with only a title reports an empty body`() = runTest {
        val uri = writeDeck(
            "titleless.pptx",
            mapOf("ppt/slides/slide1.xml" to slideXml("Only a title", emptyList()))
        )
        val deck = PresentationParser.parse(context, uri, "pptx")!!
        assertEquals("Only a title", deck.slides[0].title)
        assertTrue(deck.slides[0].body.isEmpty())
    }

    @Test
    fun `an archive with no slides does not fabricate a deck`() = runTest {
        val uri = writeDeck("empty.pptx", mapOf("[Content_Types].xml" to "<Types/>"))
        assertNull("an archive with no slides must not invent one",
            PresentationParser.parse(context, uri, "pptx"))
    }

    @Test
    fun `a file that is not a zip is refused`() = runTest {
        val file = File(context.cacheDir, "not-a-deck.pptx")
        file.writeText("this is plain text, not an archive")
        assertNull(PresentationParser.parse(context, Uri.fromFile(file), "pptx"))
    }

    @Test
    fun `a missing file is refused`() = runTest {
        val missing = File(context.cacheDir, "gone.pptx")
        assertNull(PresentationParser.parse(context, Uri.fromFile(missing), "pptx"))
    }

    @Test
    fun `a null uri is refused`() = runTest {
        assertNull(PresentationParser.parse(context, null, "pptx"))
    }

    @Test
    fun `open document decks parse their pages`() = runTest {
        val content = """
            <?xml version="1.0" encoding="UTF-8"?>
            <office:document-content xmlns:office="urn:oasis" xmlns:text="urn:text"
                                     xmlns:draw="urn:draw" xmlns:dc="urn:dc" xmlns:meta="urn:meta">
              <office:body>
                <draw:page>
                  <text:p>Impress Opening</text:p>
                  <text:p>First bullet</text:p>
                </draw:page>
                <draw:page>
                  <text:p>Impress Closing</text:p>
                </draw:page>
              </office:body>
            </office:document-content>
        """.trimIndent()
        val uri = writeDeck("deck.odp", mapOf("content.xml" to content))

        val deck = PresentationParser.parse(context, uri, "odp")
        assertNotNull(deck)
        assertEquals(2, deck!!.slides.size)
        assertEquals("Impress Opening", deck.slides[0].title)
        assertEquals(listOf("First bullet"), deck.slides[0].body)
        assertEquals("Impress Closing", deck.slides[1].title)
    }

    @Test
    fun `binary powerpoint is reported as unparseable rather than faked`() = runTest {
        // Legacy .ppt is an OLE container, not a zip, so no slide text exists
        // to recover and the reader must say so by returning null.
        val file = File(context.cacheDir, "legacy.ppt")
        file.writeBytes(byteArrayOf(0xD0.toByte(), 0xCF.toByte(), 0x11, 0xE0.toByte(), 0, 0))
        assertNull(PresentationParser.parse(context, Uri.fromFile(file), "ppt"))
    }

    @Test
    fun `canParse only claims the formats it truly decomposes` () {
        assertTrue(PresentationParser.canParse("pptx"))
        assertTrue(PresentationParser.canParse(".PPTX"))
        assertTrue(PresentationParser.canParse("odp"))
        assertTrue(PresentationParser.canParse("fodp"))
        assertEquals(false, PresentationParser.canParse("ppt"))
        assertEquals(false, PresentationParser.canParse("key"))
        assertEquals(false, PresentationParser.canParse("txt"))
    }
}
