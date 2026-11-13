package com.vennela365.viewer

import com.vennela365.viewer.parser.MarkdownParser
import com.vennela365.viewer.parser.MdBlock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Performance and correctness guards for the Markdown reader.
 *
 * The viewer must stay responsive on a low-end device, so parsing a large,
 * realistic document has to finish in a small fraction of a second. These
 * tests fail loudly if a future change reintroduces quadratic behaviour.
 */
class MarkdownPerformanceTest {

    /** A realistic document: headings, prose, lists, code fences and tables. */
    private fun buildLargeDocument(sections: Int): String = buildString {
        appendLine("# Large Document")
        appendLine()
        for (i in 1..sections) {
            appendLine("## Section $i")
            appendLine()
            appendLine(
                "This paragraph exercises **bold**, *italic*, `code` and a " +
                    "[link](https://example.com/$i) inside a normal sentence."
            )
            appendLine()
            appendLine("- first bullet for $i")
            appendLine("- second bullet for $i")
            appendLine()
            appendLine("| Col A | Col B | Col C |")
            appendLine("|-------|-------|-------|")
            appendLine("| $i | value $i | done |")
            appendLine()
            appendLine("```kotlin")
            appendLine("fun section$i() = println(\"section $i\")")
            appendLine("```")
            appendLine()
            appendLine("> A quote in section $i.")
            appendLine()
        }
    }

    private fun elapsedMillis(block: () -> Unit): Long {
        val start = System.nanoTime()
        block()
        return (System.nanoTime() - start) / 1_000_000
    }

    @Test
    fun `parsing a large document stays fast`() {
        val source = buildLargeDocument(400)
        assertTrue("fixture should be substantial", source.length > 80_000)

        var blocks = emptyList<MdBlock>()
        val first = elapsedMillis { blocks = MarkdownParser.parse(source) }
        assertTrue("parsed at least some blocks", blocks.isNotEmpty())

        // Generous ceiling for a cold JVM with a slow CI box; a quadratic
        // regression blows straight past it.
        assertTrue("first parse took ${first}ms, expected under 3000ms", first < 3_000)

        val second = elapsedMillis { MarkdownParser.parse(source) }
        assertTrue("warm parse took ${second}ms, expected under 1500ms", second < 1_500)
    }

    @Test
    fun `a very long single line does not explode`() {
        // Unbroken prose is the classic quadratic trap: it must still finish.
        val source = "word ".repeat(60_000)
        var count = 0
        val took = elapsedMillis { count = MarkdownParser.parse(source).size }
        assertEquals("one long line is a single paragraph", 1, count)
        assertTrue("parsing one huge line took ${took}ms", took < 3_000)
    }

    @Test
    fun `many unclosed code fences do not hang`() {
        val source = buildString {
            repeat(2_000) {
                appendLine("```")
                appendLine("unclosed fence $it")
            }
        }
        val took = elapsedMillis { MarkdownParser.parse(source) }
        assertTrue("unclosed fences took ${took}ms", took < 3_000)
    }

    @Test
    fun `deeply nested emphasis terminates`() {
        val source = "*".repeat(5_000) + " text " + "_".repeat(5_000)
        val took = elapsedMillis { MarkdownParser.parse(source) }
        assertTrue("emphasis-heavy parse took ${took}ms", took < 3_000)
    }
}
