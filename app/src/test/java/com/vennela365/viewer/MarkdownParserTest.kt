package com.vennela365.viewer

import com.vennela365.viewer.parser.MarkdownParser
import com.vennela365.viewer.parser.MdBlock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownParserTest {

    @Test
    fun `parses ATX headings with levels`() {
        val blocks = MarkdownParser.parse("# Title\n\n### Deep")
        assertEquals(2, blocks.size)
        assertEquals(MdBlock.Heading(1, "Title"), blocks[0])
        assertEquals(MdBlock.Heading(3, "Deep"), blocks[1])
    }

    @Test
    fun `groups consecutive lines into one paragraph`() {
        val blocks = MarkdownParser.parse("first line\nsecond line\n\nnew paragraph")
        assertEquals(2, blocks.size)
        val first = blocks[0] as MdBlock.Paragraph
        assertEquals("first line second line", first.spans.joinToString("") { it.text })
    }

    @Test
    fun `parses fenced code with language`() {
        val blocks = MarkdownParser.parse("```kotlin\nval x = 1\n```")
        val code = blocks.single() as MdBlock.Code
        assertEquals("kotlin", code.language)
        assertEquals("val x = 1", code.code)
    }

    @Test
    fun `parses bullets and ordered items`() {
        val blocks = MarkdownParser.parse("- one\n- two\n\n1. first\n2. second")
        assertEquals(4, blocks.size)
        assertTrue(blocks[0] is MdBlock.Bullet)
        assertEquals("2", (blocks[3] as MdBlock.Ordered).number)
    }

    @Test
    fun `parses tables`() {
        val blocks = MarkdownParser.parse("| A | B |\n|---|---|\n| 1 | 2 |")
        val table = blocks.single() as MdBlock.Table
        assertEquals(listOf("A", "B"), table.header)
        assertEquals(listOf("1", "2"), table.rows.single())
    }

    @Test
    fun `parses inline emphasis and links`() {
        val spans = MarkdownParser.parseInline("a **bold** and `code` and [site](https://x.y)")
        assertTrue(spans.any { it.text == "bold" && it.bold })
        assertTrue(spans.any { it.text == "code" && it.code })
        assertTrue(spans.any { it.text == "site" && it.link == "https://x.y" })
    }

    @Test
    fun `underscores inside identifiers are not italics`() {
        val spans = MarkdownParser.parseInline("some_variable_name")
        assertEquals("some_variable_name", spans.joinToString("") { it.text })
        assertTrue(spans.none { it.italic })
    }

    @Test
    fun `horizontal rule is not parsed as a heading`() {
        val blocks = MarkdownParser.parse("---")
        assertEquals(MdBlock.Rule, blocks.single())
    }

    @Test
    fun `blank input yields no blocks`() {
        assertTrue(MarkdownParser.parse("   \n\n ").isEmpty())
    }

    /*
     * Regression guards.
     *
     * The reader used to freeze on malformed documents because the parser
     * could spin forever and the page composed every block at once. These
     * cases pin down "always terminates, always bounded".
     */

    @Test
    fun `unterminated code fence still terminates`() {
        val blocks = MarkdownParser.parse("```kotlin\nval x = 1\nval y = 2")
        val code = blocks.single() as MdBlock.Code
        assertEquals("kotlin", code.language)
        assertTrue(code.code.contains("val y = 2"))
    }

    @Test
    fun `unterminated inline markers do not hang`() {
        val spans = MarkdownParser.parseInline("**bold never closed and `code never closed")
        assertTrue(spans.isNotEmpty())
    }

    @Test
    fun `pathological input terminates quickly`() {
        val hostile = buildString {
            repeat(400) {
                append("> quote line\n")
                append("- bullet *with* _unclosed\n")
                append("| a | b |\n")
                append("```\n")
                append("plain text line\n")
            }
        }
        val started = System.nanoTime()
        val blocks = MarkdownParser.parse(hostile)
        val elapsedMs = (System.nanoTime() - started) / 1_000_000
        assertTrue("parser took ${elapsedMs}ms", elapsedMs < 2000)
        assertTrue(blocks.isNotEmpty())
    }

    @Test
    fun `deeply nested structures terminate`() {
        val nested = buildString {
            repeat(200) { append(">") }
            append(" quoted")
            repeat(200) { append("- item\n  ") }
        }
        val started = System.nanoTime()
        MarkdownParser.parse(nested)
        val elapsedMs = (System.nanoTime() - started) / 1_000_000
        assertTrue("parser took ${elapsedMs}ms", elapsedMs < 2000)
    }

    @Test
    fun `crlf and lone carriage returns are normalised`() {
        val blocks = MarkdownParser.parse("# Title\r\n\r\nbody\r")
        assertEquals(2, blocks.size)
        assertEquals(MdBlock.Heading(1, "Title"), blocks[0])
    }
}
