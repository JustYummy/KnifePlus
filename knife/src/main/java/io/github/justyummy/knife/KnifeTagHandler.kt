package io.github.justyummy.knife

import android.text.*
import android.text.style.AlignmentSpan
import io.github.justyummy.knife.KnifeBulletSpan
import io.github.justyummy.knife.KnifeNumberedSpan
import org.xml.sax.XMLReader

class KnifeTagHandler : Html.TagHandler {

    private class Numbered
    private class Bullet
    private class Newline(val mNumNewlines: Int)


    override fun handleTag(opening: Boolean, tag: String, output: Editable, xmlReader: XMLReader) {
        if (opening) {
//            Log.d("Open TAG", tag)
            when {
                tag.equals("ol", ignoreCase = true) -> {
                    startBlockElement(output)
                }
                tag.equals("nu", ignoreCase = true) -> {
                    startNumbered(output)
                }
                tag.equals("bu", ignoreCase = true) -> {
                    startBullet(output)
                }
            }
        } else {
            when {
                tag.equals("ol", ignoreCase = true) -> {
                    endBlockElement(output)
                }
                tag.equals("nu", ignoreCase = true) -> {
                    endNumbered(output)
                }
                tag.equals("bu", ignoreCase = true) -> {
                    endBullet(output)
                }

            }
        }
    }


    private fun startBlockElement(text: Editable) {
        appendNewlines(text, 1)
        start(text, Newline(1))
    }

    private fun endBlockElement(text: Editable) {
        val n = getLast(text, Newline::class.java)
        if (n != null) {
            appendNewlines(text, n.mNumNewlines)
            text.removeSpan(n)
        }

        val a = getLast(text, Layout.Alignment::class.java)
        if (a != null) {
            setSpanFromMark(text, a, AlignmentSpan.Standard(Layout.Alignment.ALIGN_NORMAL))
        }
    }

    private fun appendNewlines(text: Editable, minNewline: Int) {
        val len = text.length

        if (len == 0) {
            return
        }

        var existingNewlines = 0
        var i = len - 1
        while (i >= 0 && text[i] == '\n') {
            existingNewlines++
            i--
        }

        for (j in existingNewlines until minNewline) {
            text.append("\n")
        }
    }


    private fun startNumbered(text: Editable) {
        startBlockElement(text)
        start(text, Numbered())
    }

    //设置Span
    private fun endNumbered(text: Editable) {
        endBlockElement(text)
        end(text, Numbered::class.java, KnifeNumberedSpan())
    }

    private fun startBullet(text: Editable) {
        startBlockElement(text)
        start(text, Bullet())
    }

    private fun endBullet(text: Editable) {
        endBlockElement(text)
        end(text, Bullet::class.java, KnifeBulletSpan())
    }

    private fun start(text: Editable, mark: Any) {
        val len = text.length
        text.setSpan(mark, len, len, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
    }

    private fun end(text: Editable, kind: Class<*>, repl: Any) {
//        val len = text.length
        val obj = getLast(text, kind)
        if (obj != null) {
            setSpanFromMark(text, obj, repl)
        }
    }

    private fun setSpanFromMark(text: Spannable, mark: Any, vararg spans: Any) {
        val where = text.getSpanStart(mark)
        text.removeSpan(mark)
        val len = text.length
        if (where != len) {
            for (span in spans) {
                text.setSpan(span, where, len, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
    }

    private fun <T> getLast(text: Spanned, kind: Class<T>): T? {
        /*
         * This knows that the last returned object from getSpans()
         * will be the most recently added.
         */
        val objs = text.getSpans(0, text.length, kind)

        return if (objs.isEmpty()) {
            null
        } else {
            objs[objs.size - 1]
        }
    }
}

