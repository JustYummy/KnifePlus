/*
 * Copyright (C) 2015 Matthew Lee
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.justyummy.knife

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.text.*
import android.text.method.MovementMethod
import android.text.style.*
import android.util.AttributeSet
import android.util.Log
import android.view.KeyEvent
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import io.github.justyummy.knife.KnifeArrowKeyMovementMethod
import io.github.justyummy.knife.KnifeBulletSpan
import io.github.justyummy.knife.KnifeNumberedSpan
import java.util.*
import kotlin.collections.ArrayList


open class KnifeText : EditText, TextWatcher {
    private var bulletColor = 0
    private var bulletRadius = 0
    private var bulletGapWidth = 0
    private var historyEnable = true
    private var historySize = 100
    private var linkColor = 0
    private var linkUnderline = true
    private var quoteColor = 0
    private var quoteStripeWidth = 0
    private var quoteGapWidth = 0
    //新增 Numbered
    private var numberedColor = 0
    private var numberedGap = 0f
    private var numberedTextSize = 0f

    data class SelectionHistory(val spannableString: SpannableStringBuilder, val selectionStart: Int, val selectionEnd: Int)

    private val historyList = LinkedList<SelectionHistory>()

//    private val historyList = LinkedList<SpannableStringBuilder>()

    private var historyWorking = false
    private var historyCursor = 0

    private var inputBefore: SpannableStringBuilder? = null
    private var inputLast: Editable? = null


    //构造函数
    constructor(context: Context) : super(context) {
        init(null)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        init(attrs)
    }

    //初始化
    private fun init(attrs: AttributeSet?) {
        val array = context.obtainStyledAttributes(attrs, R.styleable.KnifeText)
        bulletColor = array.getColor(R.styleable.KnifeText_bulletColor, 0)
        bulletRadius = array.getDimensionPixelSize(R.styleable.KnifeText_bulletRadius, 0)
        bulletGapWidth = array.getDimensionPixelSize(R.styleable.KnifeText_bulletGapWidth, 0)
        historyEnable = array.getBoolean(R.styleable.KnifeText_historyEnable, true)
        historySize = array.getInt(R.styleable.KnifeText_historySize, 100)
        linkColor = array.getColor(R.styleable.KnifeText_linkColor, 0)
        linkUnderline = array.getBoolean(R.styleable.KnifeText_linkUnderline, true)
        quoteColor = array.getColor(R.styleable.KnifeText_quoteColor, 0)
        quoteStripeWidth = array.getDimensionPixelSize(R.styleable.KnifeText_quoteStripeWidth, 0)
        quoteGapWidth = array.getDimensionPixelSize(R.styleable.KnifeText_quoteCapWidth, 0)
        //新增 Numbered
        numberedColor = array.getColor(R.styleable.KnifeText_numberedColor, 0)
        numberedTextSize = textSize
        numberedGap = 20f

        array.recycle()

        if (historyEnable && historySize <= 0) {
            throw IllegalArgumentException("historySize must > 0")
        }

    }


//    // Rewrite Selection @KnifeSelection
//    override fun setSelection(start: Int, stop: Int) {
//        ZKnifeSelection.setSelection(text, start, stop)
//    }
//
//    override fun setSelection(index: Int) {
//        ZKnifeSelection.setSelection(text, index)
//    }
//
//    override fun selectAll() {
//        ZKnifeSelection.selectAll(text)
//    }
//
//    override fun extendSelection(index: Int) {
//        ZKnifeSelection.extendSelection(text, index)
//    }


//    override fun onTouchEvent(event: MotionEvent?): Boolean {
//        val action = event!!.action
//
//        if (action == MotionEvent.ACTION_MOVE) {
//            Log.d("MOVE", "MMMMMMMM")
//        }
//        return super.onTouchEvent(event)
//    }

    override fun getDefaultMovementMethod(): MovementMethod {
//        return super.getDefaultMovementMethod()
//        return LinkMovementMethod.getInstance()
        return KnifeArrowKeyMovementMethod.instance
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        addTextChangedListener(this)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        removeTextChangedListener(this)
    }

    // StyleSpan ===================================================================================

    fun bold(valid: Boolean) {
        if (valid) {
            styleValid(Typeface.BOLD, selectionStart, selectionEnd)
        } else {
            styleInvalid(Typeface.BOLD, selectionStart, selectionEnd)
        }
    }

    fun italic(valid: Boolean) {
        if (valid) {
            styleValid(Typeface.ITALIC, selectionStart, selectionEnd)
        } else {
            styleInvalid(Typeface.ITALIC, selectionStart, selectionEnd)
        }
    }

    private fun styleValid(style: Int, start: Int, end: Int) {
        when (style) {
            Typeface.NORMAL, Typeface.BOLD, Typeface.ITALIC, Typeface.BOLD_ITALIC -> {
            }
            else -> return
        }

        if (start >= end) {
            return
        }

        editableText.setSpan(StyleSpan(style), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    private fun styleInvalid(style: Int, start: Int, end: Int) {
        when (style) {
            Typeface.NORMAL, Typeface.BOLD, Typeface.ITALIC, Typeface.BOLD_ITALIC -> {
            }
            else -> return
        }

        if (start >= end) {
            return
        }
        //获取样式
        val spans = editableText.getSpans(start, end, StyleSpan::class.java)
        val list = ArrayList<KnifePart>()

        for (span in spans) {
            if (span.style == style) {
                list.add(KnifePart(editableText.getSpanStart(span), editableText.getSpanEnd(span)))
                editableText.removeSpan(span)
            }
        }

        for (part in list) {
            if (part.isValid) {
                if (part.start < start) {
                    styleValid(style, part.start, start)
                }

                if (part.end > end) {
                    styleValid(style, end, part.end)
                }
            }
        }
    }

    private fun containStyle(style: Int, start: Int, end: Int): Boolean {
        when (style) {
            Typeface.NORMAL, Typeface.BOLD, Typeface.ITALIC, Typeface.BOLD_ITALIC -> {
            }
            else -> return false
        }

        if (start > end) {
            return false
        }

        if (start == end) {
            return if (start - 1 < 0 || start + 1 > editableText.length) {
                false
            } else {
                val before = editableText.getSpans(start - 1, start, StyleSpan::class.java)
                val after = editableText.getSpans(start, start + 1, StyleSpan::class.java)
                before.isNotEmpty() && after.isNotEmpty() && before[0].style == style && after[0].style == style
            }
        } else {
            val builder = StringBuilder()

            // Make sure no duplicate characters be added
            for (i in start until end) {
                val spans = editableText.getSpans(i, i + 1, StyleSpan::class.java)
                for (span in spans) {
                    if (span.style == style) {
                        builder.append(editableText.subSequence(i, i + 1).toString())
                        break
                    }
                }
            }

            return editableText.subSequence(start, end).toString() == builder.toString()
        }
    }

    // UnderlineSpan ===============================================================================

    fun underline(valid: Boolean) {
        if (valid) {
            underlineValid(selectionStart, selectionEnd)
        } else {
            underlineInvalid(selectionStart, selectionEnd)
        }
    }

    private fun underlineValid(start: Int, end: Int) {
        if (start >= end) {
            return
        }

        editableText.setSpan(UnderlineSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    private fun underlineInvalid(start: Int, end: Int) {
        if (start >= end) {
            return
        }

        val spans = editableText.getSpans(start, end, UnderlineSpan::class.java)
        val list = ArrayList<KnifePart>()

        for (span in spans) {
            list.add(KnifePart(editableText.getSpanStart(span), editableText.getSpanEnd(span)))
            editableText.removeSpan(span)
        }

        for (part in list) {
            if (part.isValid) {
                if (part.start < start) {
                    underlineValid(part.start, start)
                }

                if (part.end > end) {
                    underlineValid(end, part.end)
                }
            }
        }
    }

    private fun containUnderline(start: Int, end: Int): Boolean {
        if (start > end) {
            return false
        }

        if (start == end) {
            return if (start - 1 < 0 || start + 1 > editableText.length) {
                false
            } else {
                val before = editableText.getSpans(start - 1, start, UnderlineSpan::class.java)
                val after = editableText.getSpans(start, start + 1, UnderlineSpan::class.java)
                before.isNotEmpty() && after.isNotEmpty()
            }
        } else {
            val builder = StringBuilder()

            for (i in start until end) {
                if (editableText.getSpans(i, i + 1, UnderlineSpan::class.java).isNotEmpty()) {
                    builder.append(editableText.subSequence(i, i + 1).toString())
                }
            }

            return editableText.subSequence(start, end).toString() == builder.toString()
        }
    }

    // StrikethroughSpan ===========================================================================

    fun strikethrough(valid: Boolean) {
        if (valid) {
            strikethroughValid(selectionStart, selectionEnd)
        } else {
            strikethroughInvalid(selectionStart, selectionEnd)
        }
    }

    private fun strikethroughValid(start: Int, end: Int) {
        if (start >= end) {
            return
        }

        editableText.setSpan(StrikethroughSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    private fun strikethroughInvalid(start: Int, end: Int) {
        if (start >= end) {
            return
        }

        val spans = editableText.getSpans(start, end, StrikethroughSpan::class.java)
        val list = ArrayList<KnifePart>()

        for (span in spans) {
            list.add(KnifePart(editableText.getSpanStart(span), editableText.getSpanEnd(span)))
            editableText.removeSpan(span)
        }

        for (part in list) {
            if (part.isValid) {
                if (part.start < start) {
                    strikethroughValid(part.start, start)
                }

                if (part.end > end) {
                    strikethroughValid(end, part.end)
                }
            }
        }
    }

    private fun containStrikethrough(start: Int, end: Int): Boolean {
        if (start > end) {
            return false
        }

        if (start == end) {
            return if (start - 1 < 0 || start + 1 > editableText.length) {
                false
            } else {
                val before = editableText.getSpans(start - 1, start, StrikethroughSpan::class.java)
                val after = editableText.getSpans(start, start + 1, StrikethroughSpan::class.java)
                before.isNotEmpty() && after.isNotEmpty()
            }
        } else {
            val builder = StringBuilder()

            for (i in start until end) {
                if (editableText.getSpans(i, i + 1, StrikethroughSpan::class.java).isNotEmpty()) {
                    builder.append(editableText.subSequence(i, i + 1).toString())
                }
            }

            return editableText.subSequence(start, end).toString() == builder.toString()
        }
    }

    // BulletSpan ==================================================================================

    fun bullet(valid: Boolean) {
        numberedInvalid()
        quoteInvalid()
        if (valid) {
            bulletValid()
        } else {
            val lines = TextUtils.split(editableText.toString(), "\n")

            var lineTextStart = 0
            var lineTextEnd = 0
            for (i in lines.indices) {

                var lineStart = 0
                for (j in 0 until i) {
                    lineStart += lines[j].length + 1
                }

                val lineEnd = lineStart + lines[i].length
                if (lineStart > lineEnd) {
                    continue
                }

                if (lineStart <= selectionStart && selectionEnd <= lineEnd) {
                    lineTextStart = lineStart
                    lineTextEnd = lineEnd
                } else if (selectionStart <= lineStart && lineEnd <= selectionEnd) {
                    lineTextStart = lineStart
                    lineTextEnd = lineEnd
                }
            }

            // If the line is empty
            if (lineTextStart == lineTextEnd) {
                removeTextChangedListener(this)
                editableText.insert(selectionStart, "\u200b")
                bulletValid()
                addTextChangedListener(this)
            } else {
                val currentLineText = getCurrentLineText(getCurrentLine(selectionStart))
                val linePreSize = selectionStart - lineTextStart
                val lineNexSize = lineTextEnd - selectionStart

                if (currentLineText == "\u200b") {
                    if (linePreSize == 0) {
                        removeTextChangedListener(this)
                        bulletInvalid()
                        editableText.delete(selectionStart, selectionStart + 1)
                        addTextChangedListener(this)
                    } else if (lineNexSize == 0) {
                        removeTextChangedListener(this)
                        bulletInvalid()
                        editableText.delete(selectionStart - 1, selectionStart)
                        addTextChangedListener(this)
                    }
                } else {
                    bulletInvalid()
                }
            }
        }
    }

    private fun bulletValid() {
        val lines = TextUtils.split(editableText.toString(), "\n")

        for (i in lines.indices) {
            if (containBullet(i)) {
                continue
            }

            var lineStart = 0
            for (j in 0 until i) {
                lineStart += lines[j].length + 1 // \n
            }

            val lineEnd = lineStart + lines[i].length
            if (lineStart >= lineEnd) {
                continue
            }

            // Find selection area inside
            var bulletStart = 0
            var bulletEnd = 0
            if (lineStart <= selectionStart && selectionEnd <= lineEnd) {
                bulletStart = lineStart
                bulletEnd = lineEnd
            } else if (selectionStart <= lineStart && lineEnd <= selectionEnd) {
                bulletStart = lineStart
                bulletEnd = lineEnd
            }

            if (bulletStart < bulletEnd) {
                editableText.setSpan(KnifeBulletSpan(bulletColor, bulletRadius, bulletGapWidth), bulletStart, bulletEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
    }

    private fun bulletInvalid() {
        val lines = TextUtils.split(editableText.toString(), "\n")

        for (i in lines.indices) {
            if (!containBullet(i)) {
                continue
            }

            var lineStart = 0
            for (j in 0 until i) {
                lineStart += lines[j].length + 1
            }

            val lineEnd = lineStart + lines[i].length
            if (lineStart >= lineEnd) {
                continue
            }

            var bulletStart = 0
            var bulletEnd = 0
            if (lineStart <= selectionStart && selectionEnd <= lineEnd) {
                bulletStart = lineStart
                bulletEnd = lineEnd
            } else if (selectionStart <= lineStart && lineEnd <= selectionEnd) {
                bulletStart = lineStart
                bulletEnd = lineEnd
            }

            if (bulletStart < bulletEnd) {
                val spans = editableText.getSpans(bulletStart, bulletEnd, KnifeBulletSpan::class.java)
                for (span in spans) {
                    editableText.removeSpan(span)
                }
            }
        }
    }

    private fun containBullet(): Boolean {
        val lines = TextUtils.split(editableText.toString(), "\n")
        val list = ArrayList<Int>()

        for (i in lines.indices) {
            var lineStart = 0
            for (j in 0 until i) {
                lineStart += lines[j].length + 1
            }

            val lineEnd = lineStart + lines[i].length
            if (lineStart >= lineEnd) {
                continue
            }

            if (lineStart <= selectionStart && selectionEnd <= lineEnd) {
                list.add(i)
            } else if (selectionStart <= lineStart && lineEnd <= selectionEnd) {
                list.add(i)
            }
        }

        for (i in list) {
            if (!containBullet(i)) {
                return false
            }
        }

        return true
    }

    private fun containBullet(index: Int): Boolean {
        val lines = TextUtils.split(editableText.toString(), "\n")
        if (index < 0 || index >= lines.size) {
            return false
        }

        var start = 0
        for (i in 0 until index) {
            start += lines[i].length + 1
        }

        val end = start + lines[index].length
        if (start >= end) {
            return false
        }

        val spans = editableText.getSpans(start, end, KnifeBulletSpan::class.java)
        return spans.isNotEmpty()
    }

    //新增
    // NumberedSpan ==================================================================================

    fun numbered(valid: Boolean) {
        bulletInvalid()
        quoteInvalid()
        if (valid) {
            numberedValid()
        } else {
            // If the line is empty add an Blank
            val lines = TextUtils.split(editableText.toString(), "\n")

            // find the current line's text range
            var lineTextStart = 0
            var lineTextEnd = 0
            for (i in lines.indices) {

                var lineStart = 0
                for (j in 0 until i) {
                    lineStart += lines[j].length + 1
                }

                val lineEnd = lineStart + lines[i].length
                if (lineStart > lineEnd) {
                    continue
                }

                if (lineStart <= selectionStart && selectionEnd <= lineEnd) {
                    lineTextStart = lineStart
                    lineTextEnd = lineEnd
                } else if (selectionStart <= lineStart && lineEnd <= selectionEnd) {
                    lineTextStart = lineStart
                    lineTextEnd = lineEnd
                }
            }

            // If the line is empty
            if (lineTextStart == lineTextEnd) {
                removeTextChangedListener(this)
                editableText.insert(selectionStart, "\u200b")
                numberedValid()
                addTextChangedListener(this)
            } else {
                // If have \u200b we should delete it
                val currentLineText = getCurrentLineText(getCurrentLine(selectionStart))
                val linePreSize = selectionStart - lineTextStart
                val lineNexSize = lineTextEnd - selectionStart

                if (currentLineText == "\u200b") {
                    if (linePreSize == 0) {
                        removeTextChangedListener(this)
                        numberedInvalid()
                        editableText.delete(selectionStart, selectionStart + 1)
                        addTextChangedListener(this)
                    } else if (lineNexSize == 0) {
                        removeTextChangedListener(this)
                        numberedInvalid()
                        editableText.delete(selectionStart - 1, selectionStart)
                        addTextChangedListener(this)
                    }
                } else {
                    numberedInvalid()
                }
            }
        }
    }

    // to save every point of the text
    data class NumberedSpanPoint(var index: Int, var start: Int, var end: Int, var IsNumbered: Boolean)

    // to save the Numbered text point
    data class NumberedPoint(var start: Int, var end: Int, var index: Int)

    private fun removeSelectedLinesSpan() {

        val lines = TextUtils.split(editableText.toString(), "\n")
        for (i in lines.indices) {
            var lineStart = 0
            for (j in 0 until i) {
                lineStart += lines[j].length + 1
            }
            val lineEnd = lineStart + lines[i].length

            var numberedStart = 0
            var numberedEnd = 0
            if (lineStart <= selectionStart && selectionEnd <= lineEnd) {
                numberedStart = lineStart
                numberedEnd = lineEnd
            } else if (selectionStart <= lineStart && lineEnd <= selectionEnd) {
                numberedStart = lineStart
                numberedEnd = lineEnd
            }
            if (numberedStart != 0 && numberedEnd != 0 && containNumbered(i)) {
                val spans = editableText.getSpans(numberedStart, numberedEnd, KnifeNumberedSpan::class.java)
                for (span in spans) {
                    editableText.removeSpan(span)
                }
            }
        }
    }

    private fun numberedValid() {
        // If we selected multi-lines and there are Span inside remove it
        removeSelectedLinesSpan()
        // get every single line on the text
        val lines = TextUtils.split(editableText.toString(), "\n")

        // save every single point
        val numberedSpanLists = mutableListOf<NumberedSpanPoint>()
        for (i in lines.indices) {
            var lineStart = 0
            for (j in 0 until i) {
                lineStart += lines[j].length + 1
            }

            val lineEnd = lineStart + lines[i].length

            if (containNumbered(i)) {
                numberedSpanLists.add(NumberedSpanPoint(i, lineStart, lineEnd, true))
            } else {
                numberedSpanLists.add(NumberedSpanPoint(i, lineStart, lineEnd, false))
            }
        }

        // save every Numbered point
        val numberedLists = mutableListOf<NumberedPoint>()
        for (item in numberedSpanLists) {
            if (item.IsNumbered) {
                when {
                    numberedLists.isEmpty() -> numberedLists.add(NumberedPoint(item.start, item.end, 1))
                    item.start == (numberedLists.last().end + 1) -> numberedLists.add(NumberedPoint(item.start, item.end, numberedLists.last().index + 1))
                    else -> numberedLists.add(NumberedPoint(item.start, item.end, 1))
                }
            }
        }

        val selectedList = mutableListOf<NumberedPoint>()
        //获取被选择的行


        // find the list that need to make change
        val changeLists = mutableListOf<NumberedPoint>()


        for (i in lines.indices) {
//            Log.d("Index", "$i")

            if (containNumbered(i)) {
                continue
            }

            var lineStart = 0
            for (j in 0 until i) {
                lineStart += lines[j].length + 1
            }

            val lineEnd = lineStart + lines[i].length

            if (lineStart >= lineEnd) {
                continue
            }
            //TODO("在selection里面有多行时候操作")
            // Find selection area inside
            var numberedStart = 0
            var numberedEnd = 0
            if (lineStart <= selectionStart && selectionEnd <= lineEnd) {
                numberedStart = lineStart
                numberedEnd = lineEnd
            } else if (selectionStart <= lineStart && lineEnd <= selectionEnd) {
                numberedStart = lineStart
                numberedEnd = lineEnd
            }

            var numberedLine = 1

            var cursor = -1
            var cursorValue = 0

            var neverUsedFlag = true

            for (item in numberedLists) {
                when {
                    (item.end + 1) == numberedStart -> {
                        numberedLine = item.index + 1
                        cursor = numberedEnd + 1
                        cursorValue = numberedLine + 1
                        neverUsedFlag = false
                    }
                    cursor == item.start -> {
                        changeLists.add(NumberedPoint(item.start, item.end, cursorValue))
                        cursor = item.end + 1
                        cursorValue += 1
                    }
                }
            }

            if (neverUsedFlag) {
                for (item in numberedLists) {
                    when {
                        item.start == (numberedEnd + 1) -> {
                            // 设置删除多行
                            val curIndex = if (selectedList.isNotEmpty() && selectedList.last().end + 1 == numberedStart) selectedList.last().index + 2 else 2
                            changeLists.add(NumberedPoint(item.start, item.end, curIndex))
                            cursorValue = curIndex + 1
                            cursor = item.end + 1
                        }
                        cursor == item.start -> {
                            changeLists.add(NumberedPoint(item.start, item.end, cursorValue))
                            cursor = item.end + 1
                            cursorValue++
                        }
                    }
                }
            }

            // If we select multi-lines we should set the index right
            if (selectedList.isNotEmpty() && selectedList.last().end + 1 == numberedStart) {
                numberedLine = selectedList.last().index + 1
                Log.d("Doing", "it")
                selectedList.add(NumberedPoint(numberedStart, numberedEnd, numberedLine))
            }

            Log.d("Numbered Point", "$numberedStart, $numberedEnd, $numberedLine")
            selectedList.add(NumberedPoint(numberedStart, numberedEnd, numberedLine))

            if (numberedStart < numberedEnd) {
                editableText.setSpan(KnifeNumberedSpan(numberedColor, numberedGap, numberedTextSize, numberedLine), numberedStart, numberedEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

                if (changeLists.isEmpty()) {
                    continue
                }
                //remove old style
                val spans = editableText.getSpans(changeLists[0].start, changeLists.last().end, KnifeNumberedSpan::class.java)
                for (span in spans) {
                    editableText.removeSpan(span)
                }
                //修改改变项
                for (item in changeLists) {
                    editableText.setSpan(KnifeNumberedSpan(numberedColor, numberedGap, numberedTextSize, item.index), item.start, item.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
        }
    }

    private fun numberedInvalid() {
        val lines = TextUtils.split(editableText.toString(), "\n")

        // Save every point to the List
        val numberedSpanLists = mutableListOf<NumberedSpanPoint>()
        for (i in lines.indices) {

            var lineStart = 0
            for (j in 0 until i) {
                lineStart += lines[j].length + 1
            }

            val lineEnd = lineStart + lines[i].length

            if (containNumbered(i)) {
                numberedSpanLists.add(NumberedSpanPoint(i, lineStart, lineEnd, true))
            } else {
                numberedSpanLists.add(NumberedSpanPoint(i, lineStart, lineEnd, false))
            }
        }

        // Save the Numbered point and their index
        val numberedLists = mutableListOf<NumberedPoint>()

        for (item in numberedSpanLists) {
            if (item.IsNumbered) {
                when {
                    numberedLists.isEmpty() -> numberedLists.add(NumberedPoint(item.start, item.end, 1))
                    item.start == (numberedLists.last().end + 1) -> numberedLists.add(NumberedPoint(item.start, item.end, numberedLists.last().index + 1))
                    else -> numberedLists.add(NumberedPoint(item.start, item.end, 1))
                }
            }
        }

        val changeLists = mutableListOf<NumberedPoint>()
        for (i in lines.indices) {

            if (!containNumbered(i)) {
                continue
            }

            var lineStart = 0
            for (j in 0 until i) {
                lineStart += lines[j].length + 1
            }

            val lineEnd = lineStart + lines[i].length

            if (lineStart >= lineEnd) {
                continue
            }

            // Find selection area inside
            var numberedStart = 0
            var numberedEnd = 0
            if (lineStart <= selectionStart && selectionEnd <= lineEnd) {
                numberedStart = lineStart
                numberedEnd = lineEnd
            } else if (selectionStart <= lineStart && lineEnd <= selectionEnd) {
                numberedStart = lineStart
                numberedEnd = lineEnd
            }

            // If not the current selected line, turn back
            if (numberedEnd == 0) {
                continue
            }

            // If this is not the last selection change the list
            if (changeLists.isNotEmpty()) {
                changeLists.clear()
            }

//            Log.d("Lists", "$lineStart $lineEnd $numberedStart $numberedEnd")
            var cursor = -1
            var cursorValue = 0

            for (item in numberedLists) {
//                Log.d("Item List", "${item.start} ${item.end}")
                when {
                    item.start == numberedStart -> {
                        cursor = numberedEnd + 1
                        cursorValue += 1
                    }
                    item.start == cursor -> {
                        changeLists.add(NumberedPoint(item.start, item.end, cursorValue))
                        cursor = item.end + 1
                        cursorValue += 1
                    }
                }
            }
        }

        for (i in lines.indices) {
            if (!containNumbered(i)) {
                continue
            }

            var lineStart = 0
            for (j in 0 until i) {
                lineStart += lines[j].length + 1
            }

            val lineEnd = lineStart + lines[i].length
            if (lineStart >= lineEnd) {
                continue
            }

            var numberedStart = 0
            var numberedEnd = 0
            if (lineStart <= selectionStart && selectionEnd <= lineEnd) {
                numberedStart = lineStart
                numberedEnd = lineEnd
            } else if (selectionStart <= lineStart && lineEnd <= selectionEnd) {
                numberedStart = lineStart
                numberedEnd = lineEnd
            }

            if (numberedStart < numberedEnd) {
                val spans = editableText.getSpans(numberedStart, numberedEnd, KnifeNumberedSpan::class.java)
                for (span in spans) {
                    editableText.removeSpan(span)
                }

                // If is empty do noting
                if (changeLists.isEmpty()) {
                    continue
                }
                val spansAdd = editableText.getSpans(changeLists[0].start, changeLists.last().end, KnifeNumberedSpan::class.java)
                for (span in spansAdd) {
                    editableText.removeSpan(span)
                }
                //修改改变项
                for (item in changeLists) {
                    editableText.setSpan(KnifeNumberedSpan(numberedColor, numberedGap, numberedTextSize, item.index), item.start, item.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
        }
    }

    private fun containNumbered(): Boolean {
        val lines = TextUtils.split(editableText.toString(), "\n")
        val list = ArrayList<Int>()

        for (i in lines.indices) {
            var lineStart = 0
            for (j in 0 until i) {
                lineStart += lines[j].length + 1
            }

            val lineEnd = lineStart + lines[i].length
            if (lineStart >= lineEnd) {
                continue
            }

            if (lineStart <= selectionStart && selectionEnd <= lineEnd) {
                list.add(i)
            } else if (selectionStart <= lineStart && lineEnd <= selectionEnd) {
                list.add(i)
            }
        }

        for (i in list) {
            if (!containNumbered(i)) {
                return false
            }
        }

        return true
    }

    private fun containNumbered(index: Int): Boolean {
        val lines = TextUtils.split(editableText.toString(), "\n")

        if (index < 0 || index >= lines.size) {
            return false
        }

        var start = 0
        for (i in 0 until index) {
            start += lines[i].length + 1
        }

        val end = start + lines[index].length
        if (start >= end) {
            return false
        }

        val spans = editableText.getSpans(start, end, KnifeNumberedSpan::class.java)
        return spans.isNotEmpty()
    }

    // QuoteSpan ===================================================================================

    fun quote(valid: Boolean) {
        numberedInvalid()
        bulletInvalid()
        if (valid) {
            quoteValid()
        } else {
            val lines = TextUtils.split(editableText.toString(), "\n")

            var lineTextStart = 0
            var lineTextEnd = 0
            for (i in lines.indices) {

                var lineStart = 0
                for (j in 0 until i) {
                    lineStart += lines[j].length + 1
                }

                val lineEnd = lineStart + lines[i].length
                if (lineStart > lineEnd) {
                    continue
                }

                if (lineStart <= selectionStart && selectionEnd <= lineEnd) {
                    lineTextStart = lineStart
                    lineTextEnd = lineEnd
                } else if (selectionStart <= lineStart && lineEnd <= selectionEnd) {
                    lineTextStart = lineStart
                    lineTextEnd = lineEnd
                }
            }

            if (lineTextStart == lineTextEnd) {
                removeTextChangedListener(this)
                editableText.insert(selectionStart, "\u200b")
                quoteValid()
                addTextChangedListener(this)
            } else {
                val currentLineText = getCurrentLineText(getCurrentLine(selectionStart))
                val linePreSize = selectionStart - lineTextStart
                val lineNexSize = lineTextEnd - selectionStart

                if (currentLineText == "\u200b") {
                    if (linePreSize == 0) {
                        removeTextChangedListener(this)
                        quoteInvalid()
                        editableText.delete(selectionStart, selectionStart + 1)
                        addTextChangedListener(this)
                    } else if (lineNexSize == 0) {
                        removeTextChangedListener(this)
                        quoteInvalid()
                        editableText.delete(selectionStart - 1, selectionStart)
                        addTextChangedListener(this)
                    }
                } else {
                    quoteInvalid()
                }
            }
        }
    }

    private fun quoteValid() {
        val lines = TextUtils.split(editableText.toString(), "\n")

        for (i in lines.indices) {
            if (containQuote(i)) {
                continue
            }

            var lineStart = 0
            for (j in 0 until i) {
                lineStart += lines[j].length + 1 // \n
            }

            val lineEnd = lineStart + lines[i].length
            if (lineStart >= lineEnd) {
                continue
            }

            var quoteStart = 0
            var quoteEnd = 0
            if (lineStart <= selectionStart && selectionEnd <= lineEnd) {
                quoteStart = lineStart
                quoteEnd = lineEnd
            } else if (selectionStart <= lineStart && lineEnd <= selectionEnd) {
                quoteStart = lineStart
                quoteEnd = lineEnd
            }

            if (quoteStart < quoteEnd) {
                editableText.setSpan(KnifeQuoteSpan(quoteColor, quoteStripeWidth, quoteGapWidth), quoteStart, quoteEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
    }

    private fun quoteInvalid() {
        val lines = TextUtils.split(editableText.toString(), "\n")

        for (i in lines.indices) {
            if (!containQuote(i)) {
                continue
            }

            var lineStart = 0
            for (j in 0 until i) {
                lineStart += lines[j].length + 1
            }

            val lineEnd = lineStart + lines[i].length
            if (lineStart >= lineEnd) {
                continue
            }

            var quoteStart = 0
            var quoteEnd = 0
            if (lineStart <= selectionStart && selectionEnd <= lineEnd) {
                quoteStart = lineStart
                quoteEnd = lineEnd
            } else if (selectionStart <= lineStart && lineEnd <= selectionEnd) {
                quoteStart = lineStart
                quoteEnd = lineEnd
            }

            if (quoteStart < quoteEnd) {
                val spans = editableText.getSpans(quoteStart, quoteEnd, QuoteSpan::class.java)
                for (span in spans) {
                    editableText.removeSpan(span)
                }
            }
        }
    }

    private fun containQuote(): Boolean {
        val lines = TextUtils.split(editableText.toString(), "\n")
        val list = ArrayList<Int>()

        for (i in lines.indices) {
            var lineStart = 0
            for (j in 0 until i) {
                lineStart += lines[j].length + 1
            }

            val lineEnd = lineStart + lines[i].length
            if (lineStart >= lineEnd) {
                continue
            }

            if (lineStart <= selectionStart && selectionEnd <= lineEnd) {
                list.add(i)
            } else if (selectionStart <= lineStart && lineEnd <= selectionEnd) {
                list.add(i)
            }
        }

        for (i in list) {
            if (!containQuote(i)) {
                return false
            }
        }

        return true
    }

    private fun containQuote(index: Int): Boolean {
        val lines = TextUtils.split(editableText.toString(), "\n")
        if (index < 0 || index >= lines.size) {
            return false
        }

        var start = 0
        for (i in 0 until index) {
            start += lines[i].length + 1
        }

        val end = start + lines[index].length
        if (start >= end) {
            return false
        }

        val spans = editableText.getSpans(start, end, QuoteSpan::class.java)
        return spans.isNotEmpty()
    }

    // URLSpan =====================================================================================

    // When KnifeText lose focus, use this method
    fun link(default_title: String?, link: String?) {
        // if the link is empty, add title or do nothing
        if (link.isNullOrEmpty()) {
            if (!default_title.isNullOrEmpty()) {
                removeTextChangedListener(this)
                text?.insert(selectionStart, default_title)
                addTextChangedListener(this)
            }
        } else {

            if (selectionEnd > selectionStart) {
                removeTextChangedListener(this)
                text?.delete(selectionStart, selectionEnd)
                addTextChangedListener(this)
            }

            val currentLine = getCurrentLine(selectionStart)
            val containsSignal = getSignal(currentLine)
            val indexPosition = getLineIndexPosition(selectionStart)
            val currentText = getCurrentLineText(currentLine)

            var startBefore = selectionStart
            val titleBefore = if (!default_title.isNullOrEmpty()) default_title else link
            var endBefore = startBefore + titleBefore.length

            removeTextChangedListener(this)
            if (containsSignal > 0 && currentText == "\u200b") {
                if (indexPosition.preIndex == 1 && indexPosition.nexIndex == 0) {
                    text?.replace(selectionStart - 1, selectionStart, titleBefore)
                    startBefore -= 1
                    endBefore -= 1
                } else {
                    text?.replace(selectionStart, selectionStart + 1, titleBefore)
                }
            } else {
                text?.insert(startBefore, titleBefore)
            }

            val tempLink = if (link.startsWith("http://") || link.startsWith("https://")) {
                "$link"
            } else {
                "https://$link"
            }

            linkValid(link, startBefore, endBefore)
            addTextChangedListener(this)

            if (default_title.isNullOrEmpty()) {
                // doing in other thread
                Thread(Runnable {

                    val title = WebSideInfo.getTitle(tempLink)

                    if (title != null) {
                        // doing in the main thread
                        post {
                            Log.d("Title", "$title A")

                            val end = startBefore + title.length

                            removeTextChangedListener(this)
                            text?.replace(startBefore, endBefore, title)
                            addTextChangedListener(this)

                            if (!TextUtils.isEmpty(link.trim { it <= ' ' })) {
                                linkValid(tempLink, startBefore, end)
                            } else {
                                linkInvalid(startBefore, end)
                            }
                        }
                    }
                }).start()
            }
        }
    }

    private fun linkValid(link: String, start: Int, end: Int) {
        if (start >= end) {
            return
        }

        linkInvalid(start, end)

        editableText.setSpan(KnifeURLSpan(link, linkColor, linkUnderline), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    // Remove all span in selection, not like the boldInvalid()
    private fun linkInvalid(start: Int, end: Int) {
        if (start >= end) {
            return
        }

        val spans = editableText.getSpans(start, end, URLSpan::class.java)
        for (span in spans) {
            editableText.removeSpan(span)
        }
    }

    private fun containLink(start: Int, end: Int): Boolean {
        if (start > end) {
            return false
        }

        if (start == end) {
            return if (start - 1 < 0 || start + 1 > editableText.length) {
                false
            } else {
                val before = editableText.getSpans(start - 1, start, URLSpan::class.java)
                val after = editableText.getSpans(start, start + 1, URLSpan::class.java)
                before.isNotEmpty() && after.isNotEmpty()
            }
        } else {
            val builder = StringBuilder()

            for (i in start until end) {
                if (editableText.getSpans(i, i + 1, URLSpan::class.java).isNotEmpty()) {
                    builder.append(editableText.subSequence(i, i + 1).toString())
                }
            }

            return editableText.subSequence(start, end).toString() == builder.toString()
        }
    }

    // To detection the delete Key, If is the first index than do something
    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        if (event!!.keyCode == KeyEvent.KEYCODE_DEL && event.action != KeyEvent.ACTION_UP) {
            val lineIndexPosition = getLineIndexPosition(selectionStart)
//            Toast.makeText(context, "|${lineIndexPosition.start}|${lineIndexPosition.end}|${lineIndexPosition.preIndex}|${lineIndexPosition.nexIndex}", Toast.LENGTH_SHORT).show()
            if (lineIndexPosition.start == 0 && lineIndexPosition.preIndex == 0) {
                doingInvalid(getSignal(0))
            }
        }
        return super.dispatchKeyEvent(event)
    }

    // Redo/Undo ===================================================================================

    /* 🍎🍎🍎🍎🍎🍎 TEXT CHANGED START*/

    // If we entered
    private var enterFlag: Boolean = false

    //    TODO("If we selected multi-lines")
    // Get the current Line
    private fun getCurrentLine(Index: Int): Int {
        val lines = TextUtils.split(editableText.toString(), "\n")

        // counting the current line
        var currentLine = -1
        for (i in lines.indices) {
            var lineStart = 0
            for (j in 0 until i) {
                lineStart += lines[j].length + 1
            }

            val lineEnd = lineStart + lines[i].length
            if (lineStart > lineEnd) {
                continue
            }

            if (Index in lineStart..lineEnd) {
                currentLine = i
            }
        }
        return currentLine
    }

    // Return the Numbered list
    /* If is Numbered Index > 0
    *  If is Bullet   Index = -2
    *  If is Quote    Index = -3
    *  Else Nothing   Index = -1*/
    private fun getMarkList(): MutableList<Int> {
        val lines = TextUtils.split(editableText.toString(), "\n")

        val numberedList = mutableListOf<Int>()
        // counting the current line

        var lastIndex = -1
        for (i in lines.indices) {
//            var lineStart = 0
//            for (j in 0 until i) {
//                lineStart += lines[j].length + 1
//            }
//
//            val lineEnd = lineStart + lines[i].length
//            Log.d("Span", "$lineStart $lineEnd ${whatSpanIs(lineStart, lineEnd)}")

            if (containNumbered(i)) {
                if (lastIndex > 0) {
                    lastIndex++
                } else {
                    lastIndex = 1
                }
            } else if (containBullet(i)) {
                lastIndex = -2
            } else if (containQuote(i)) {
                lastIndex = -3
            } else {
                lastIndex = -1
            }
            numberedList.add(lastIndex)
        }
//        for (i in numberedList.indices){
//            Log.d("SSSkkkk", "$i ${numberedList[i]}")
//        }

//        if (numberedList.isEmpty()){
//            numberedList.add(-1)
//        }
        return numberedList
    }

//    private fun whatSpanIs(start: Int, end: Int): Int {
//        return when {
//            editableText.getSpans(start, end, KnifeNumberedSpan::class.java).isNotEmpty() -> 1
//            editableText.getSpans(start, end, KnifeBulletSpan::class.java).isNotEmpty() -> 2
//            editableText.getSpans(start, end, KnifeQuoteSpan::class.java).isNotEmpty() -> 3
//            else -> 0
//        }
//    }

    /* The line start and end position, and the cursor positions of the line
    * Eg. "1234|56"
    * preIndex = 4, nexIndex = 2, start = 0, end = 6
    * */
    data class LineIndexPosition(val preIndex: Int, val nexIndex: Int, val start: Int, val end: Int)

    private fun getLineIndexPosition(Index: Int): LineIndexPosition {
        // index of the line start and
        val lines = TextUtils.split(editableText.toString(), "\n")

        // counting the current line
        var preIndex = -1
        var nexIndex = -1
        var start = 0
        var end = 0
        for (i in lines.indices) {
            var lineStart = 0
            for (j in 0 until i) {
                lineStart += lines[j].length + 1
            }

            val lineEnd = lineStart + lines[i].length
            if (lineStart > lineEnd) {
                continue
            }

            if (Index in lineStart..lineEnd) {
                start = lineStart
                end = lineEnd
                preIndex = Index - lineStart
                nexIndex = lineEnd - Index
            }
        }
        return LineIndexPosition(preIndex, nexIndex, start, end)
    }

    // Get the current line text before doing something
    private fun getCurrentLineText(Index: Int): String {
        val lines = TextUtils.split(editableText.toString(), "\n")
        // If the index is -1 that means the text is empty
        return if (Index == -1) {
            ""
        } else lines[Index]
    }

    // If we delete text
    private var deleteText = ""
    // Save the select history
    private var historySelectionStart = -1
    private var historySelectionEnd = -1
    // The Numbered List Before change
    private var beforeChangedList = mutableListOf<Int>()
    // If we add something before \u200b or after \u200b
    private var actionNearSpecialSymbol = 0
    // The text before change
    private var beforeChangeTextIfDelete = ""
    private var beforeChangeTextIfAdd = ""
    // Is we add or delete something, if we add > 0 or < 0
    private var isTextAddOrDelete = 0
    // The selected Line list
    private var selectedLinesNumberList = mutableListOf<Int>()
    // Selected multi line Index
    private var beforeChangeIndexPositionStart = LineIndexPosition(-1, -1, -1, -1)
    private var beforeChangeIndexPositionEnd = LineIndexPosition(-1, -1, -1, -1)
    // Selected Text
    private var selectedText = ""

    override fun beforeTextChanged(text: CharSequence, start: Int, count: Int, after: Int) {

        beforeChangedList = getMarkList()

//        Log.d("Span", "7 10 ${editableText.getSpans(8, 9, KnifeBulletSpan::class.java).isNotEmpty()}")

        val mySelectionEnd = start + count
        val selectedLines = TextUtils.split(editableText.subSequence(start, mySelectionEnd).toString(), "\n")
        selectedText = editableText.subSequence(start, mySelectionEnd).toString()
        val currentLine = getCurrentLine(start)

        var tempLine = currentLine
        selectedLinesNumberList.clear()
        Log.d("🍎selectedLines", "${selectedLines.size}")
        if (selectedLines.size > 1) {
            for (index in 0 until selectedLines.size) {
                selectedLinesNumberList.add(tempLine)
                tempLine++
            }
            beforeChangeIndexPositionStart = getLineIndexPosition(start)
            beforeChangeIndexPositionEnd = getLineIndexPosition(mySelectionEnd)
        }

        val beforeIndexPosition = getLineIndexPosition(start)


        beforeChangeTextIfDelete = if (count > 0) getCurrentLineText(currentLine) else ""

        beforeChangeTextIfAdd = if (after > 0) getCurrentLineText(currentLine) else ""

        Log.d("beforeChangeTextIfDelete", "|$count $after|")

        if (after > 0) isTextAddOrDelete = after else if (count > 0) isTextAddOrDelete = -count

        // To delete the \u200b
        actionNearSpecialSymbol = if (beforeIndexPosition.preIndex == 1 && beforeIndexPosition.nexIndex == 0 && text.subSequence(start - 1, start).toString() == "\u200b" && after > 0) {
            -1
        } else if (beforeIndexPosition.preIndex == 0 && beforeIndexPosition.nexIndex == 1 && text.subSequence(start, start + 1).toString() == "\u200b" && after > 0) {
            1
        } else {
            0
        }

        Log.d("beforeTextChanged", "start $start count $count  after $after before ${beforeIndexPosition.preIndex} next ${beforeIndexPosition.nexIndex} actionNearSpecialSymbol $actionNearSpecialSymbol")

        // Do Delete
        if (count > 0 && after == 0) {
            if (count == 1) {
                historySelectionStart = start + count
                historySelectionEnd = historySelectionStart
            } else {
                historySelectionStart = start
                historySelectionEnd = start + count
            }
            //Do Add
        } else if (after > 0 && count == 0) {
            historySelectionStart = start
            historySelectionEnd = historySelectionStart
        } else {
            historySelectionStart = -1
            historySelectionEnd = -1
        }

        // If we delete something, than record it
        deleteText = if (count > 0) text.subSequence(start, start + count).toString() else ""

        // Undo and Redo
        if (!historyEnable || historyWorking) {
            return
        }

        inputBefore = SpannableStringBuilder(text)

    }

    private var currentIndexPosition = LineIndexPosition(-1, -1, -1, -1)

    override fun onTextChanged(text: CharSequence, start: Int, before: Int, count: Int) {
        val currentLine = getCurrentLine(start)

        currentIndexPosition = getLineIndexPosition(selectionStart)

        Log.d("On", "start $start selectionStart $selectionStart CurrentLine $currentLine IndexPosition Before ${currentIndexPosition.preIndex} Next ${currentIndexPosition.nexIndex}")
        // If we add something
        val addedText = if (count > 0) text.toString().substring(start, start + count) else ""

        enterFlag = addedText == "\n"
    }


    private var afterChangedList = mutableListOf<Int>()

    private fun getSignal(currentLine: Int): Int {
        return when {
            containNumbered(currentLine) -> 1
            containBullet(currentLine) -> 2
            containQuote(currentLine) -> 3
            else -> 0
        }
    }

    private fun doingValid(signal: Int) {
        when {
            signal > 0 -> numberedValid()
            signal == -2 -> bulletValid()
            signal == -3 -> quoteValid()
        }
    }

    private fun doingInvalid(signal: Int) {
        when {
            signal > 0 -> numberedInvalid()
            signal == -2 -> bulletInvalid()
            signal == -3 -> quoteInvalid()
        }
    }

    @SuppressLint("SimpleDateFormat")
    override fun afterTextChanged(text: Editable?) {
//        Log.d("🍎TextSize", "|$textSize|")
//        Log.d("🍎Selected Lines", "${selectedLinesNumberList.size}")
        selectedLinesNumberList.forEach { Log.d("\uD83C\uDF4ESelected Lines", "$it") }

        val currentLine = getCurrentLine(selectionStart)

        afterChangedList = getMarkList()

        // 如果选择的是单行
        if (selectedLinesNumberList.size <= 1 || deleteText == "\n") {
            Log.d("selectedLinesNumberList", "${selectedLinesNumberList.size}")
            // If we add something nearby the \u200b

            if (actionNearSpecialSymbol == -1 && !enterFlag && isTextAddOrDelete > 0 && selectedText != "\u200b") {
                val containSignal = beforeChangedList[currentLine]
                Log.d("Delete", "-1 $beforeChangeTextIfAdd")
                val tempLength = selectionStart - isTextAddOrDelete
                Log.d("TempLength", "$tempLength, $selectionStart")
                removeTextChangedListener(this)
                text?.delete(tempLength - 1, tempLength)
                doingValid(containSignal)
                addTextChangedListener(this)
            } else if (actionNearSpecialSymbol == 1 && !enterFlag && selectedText != "\u200b") {
                val containSignal = beforeChangedList[currentLine]
                removeTextChangedListener(this)
                text?.delete(selectionStart, selectionStart + 1)
                doingValid(containSignal)
                addTextChangedListener(this)
                Log.d("Delete", "1")
            }


            /* Use the total line number
            * The total line is decrease */
//            for (i in 0 until beforeChangedList.size){
//                Log.d("AAA", "$i ${beforeChangedList[i]}")
//            }

//            val doEnterFlag = if (beforeChangedList.size == 0) false else beforeChangedList[currentLine - 1] != -1

//            Log.d("CurrentLineX", "$currentLine ${beforeChangedList.size}")
            var doEnterFlag = false
            if (beforeChangedList.size != 0 && currentLine > 0) {
                if (beforeChangedList[currentLine - 1] != -1) {
                    doEnterFlag = true
                }
            }
            if (beforeChangedList.size > afterChangedList.size) {
                Log.d("行数减少", "减少${beforeChangedList.size - afterChangedList.size}行")
                if (beforeChangedList.size >= currentLine + 2) {
                    val beforeLineSignal = beforeChangedList[currentLine + 1]
                    Log.d("Single", "${currentLine + 1} $beforeLineSignal")
                    if (beforeLineSignal != -1) {
                        Log.d("执行", "5 Number 删除标注的序号")
                        if (deleteText == "\n") {
                            removeTextChangedListener(this)
                            text?.insert(selectionStart, "\n")
                            doingInvalid(beforeLineSignal)
                            addTextChangedListener(this)

                            //如果改行是空行并且有格式
                            if (selectionEnd < editableText.length) {
                                if (editableText.subSequence(selectionEnd, selectionEnd + 1).toString() == "\u200b") {
                                    removeTextChangedListener(this)
                                    text?.delete(selectionEnd, selectionEnd + 1)
                                    addTextChangedListener(this)
                                }
                            }
                        }
                    } else if (containNumbered(currentLine)) {
                        if (beforeChangeTextIfDelete == "\u200b" && currentIndexPosition.nexIndex != 0) {
                            removeTextChangedListener(this)
                            text?.delete(selectionStart - 1, selectionStart)
                            addTextChangedListener(this)
                        }
                        numberedInvalid()
                        numberedValid()
                        Log.d("执行", "6 Number 删除回车键")
                    }
                }
                // 如果内容不变
            } else if (beforeChangedList.size == afterChangedList.size) {
                Log.d("行数不变", "不变")
                Log.d("CurrentLine", "|$beforeChangeTextIfDelete| ${beforeChangeTextIfDelete.length}")


                if (currentIndexPosition.preIndex == 0 && currentIndexPosition.nexIndex == 0 && beforeChangedList[getCurrentLine(selectionStart)] != -1 && deleteText != "\u200b") {
                    // 删除有格式整行
                    // && getCurrentLineText(getCurrentLine(selectionStart)) != "\u200b
                    Log.d("执行", "10 处理整行 $deleteText")
                    val lineSignal = beforeChangedList[getCurrentLine(selectionStart)]
                    removeTextChangedListener(this)
                    text?.insert(selectionStart, "\u200b")
                    doingValid(lineSignal)
                    addTextChangedListener(this)
                } else if (currentIndexPosition.preIndex == 0 && currentIndexPosition.nexIndex == 0 && containNumbered(currentLine + 1)) {
                    //说明： 将当前行删除完    currentIndexPosition.preIndex == 0
                    //                      currentIndexPosition.nexIndex == 0
                    //      并且该行在格式内    containNumbered(currentLine + 1)
                    //操作： 将当前行设置为无格式
                    //      更新下一行
                    //      更新光标

                    Log.d("执行", "4 删除格式内行，更新下面的样式 ${containNumbered(currentLine)}  |$beforeChangeTextIfAdd|")
                    numberedInvalid()
                    setSelection(selectionStart + 1)
                    numberedInvalid()
                    numberedValid()
                    setSelection(selectionStart - 1)
                } else if (afterChangedList[getCurrentLine(selectionStart)] != -1 && currentIndexPosition.preIndex == isTextAddOrDelete) {
                    //在当前行前面插入
                    Log.d("执行", "8 在行首插入数据 $selectionStart")
                    val lineSignal = afterChangedList[getCurrentLine(selectionStart)]
                    doingInvalid(lineSignal)
                    doingValid(lineSignal)
                } else if (afterChangedList[getCurrentLine(selectionStart)] != -1 && currentIndexPosition.preIndex != 0 && currentIndexPosition.nexIndex == 0) {
                    Log.d("执行", "末尾插入数据")
                    val lineSignal = afterChangedList[getCurrentLine(selectionStart)]
                    doingInvalid(lineSignal)
                    doingValid(lineSignal)
                }
                // 如果内容增加
            } else if (beforeChangedList.size < afterChangedList.size && enterFlag && doEnterFlag) {
                enterFlag = false
                Log.d("行数增加", "增加${afterChangedList.size - beforeChangedList.size}行")
                // 若在有格式的行中回车
                if (afterChangedList[currentLine - 1] != -1) {
                    val aboveLineText = getCurrentLineText(currentLine - 1)
                    val beforeLineSignal = afterChangedList[currentLine - 1]
                    Log.d("Current Line Text", "|$aboveLineText|")
                    //TODO("获取改行的数据，如果是\u200b执行2，如果是结尾执行1， 如果是文本中执行3")
                    if (aboveLineText != "\u200b" && currentIndexPosition.nexIndex == 0) {
                        //说明： 在不为空     contrastText != "D\n"
                        //      有格式的文本  containNumbered(currentLine - 1)
                        //      末尾         currentIndexPosition.nexIndex == 0
                        //      按动回车键    enterFlag
                        //操作： 空字符串无法设置SPAN，所以sp添加一个字符串，并设置格式
                        Log.d("执行", "1 在有格式的内容末尾，按动回车，进入下一行")

                        removeTextChangedListener(this)
                        text?.insert(selectionStart, "\u200b")
                        doingValid(beforeLineSignal)
                        addTextChangedListener(this)
                        //回车，第二次回车
                    } else if (aboveLineText == "\u200b") {
                        //说明： 在有格式       containNumbered(currentLine - 1)
                        //      没有内容的行上  contrastText == "D\n"
                        //      按动回车键      enterFlag
                        //操作： 撤销回车键
                        //      删除格式
                        Log.d("执行", "2 回车，删除内容为空的序号")
                        removeTextChangedListener(this)
                        text?.delete(selectionStart - 2, selectionStart)
                        doingInvalid(beforeLineSignal)
                        addTextChangedListener(this)

                        var currentLineSignal = -1

//                        Log.d("TESt","$currentLine ${beforeChangedList.size} ${afterChangedList.size}")
                        if (currentLine + 1 < afterChangedList.size) {
                            currentLineSignal = afterChangedList[currentLine + 1]
                            Log.d("CurrentLIne", "$currentLine")
                        }
                        Log.d("AAAd", "$currentLineSignal")
                        if (currentLineSignal != -1) {
                            setSelection(selectionStart + 1)
                            doingInvalid(currentLineSignal)
                            doingValid(currentLineSignal)
                            setSelection(selectionStart - 1)
                        }
                        Log.d("Enter Enter", "ONE")
                        historySelectionStart = selectionStart + 1
                        historySelectionEnd = historySelectionStart
                    } else if (currentIndexPosition.preIndex == 0 && currentIndexPosition.nexIndex > 0) {
                        //说明： 在有格式的文本 containNumbered(currentLine - 1)
                        //      回车          enterFlag
                        //      当前位于行首   currentIndexPosition.preIndex == 0
                        //      该行内容不为空  currentIndexPosition.nexIndex > 0
                        //操作： 重新设置上一行格式
                        //      设置当前行格式
                        Log.d("执行", "3 在有格式的文本中回车")

                        setSelection(selectionStart - 2)
                        doingInvalid(beforeLineSignal)
                        doingValid(beforeLineSignal)
                        setSelection(selectionStart + 2)
                        doingValid(beforeLineSignal)
                    }
                } else if (afterChangedList[currentLine] != -1 && currentIndexPosition.preIndex == 0) {
                    //说明： 在有格式的行    containNumbered(currentLine)
                    //      回车           enterFlag
                    //      位于行首        currentIndexPosition.preIndex == 0
                    //操作： 在改行保留格式
                    //      下一行继续
                    // 在D前面回车
                    val lineSignal = afterChangedList[currentLine]
                    if (beforeChangeTextIfAdd == "\u200b") {
                        removeTextChangedListener(this)
                        text?.delete(selectionStart - 1, selectionStart + 1)
                        doingInvalid(lineSignal)
                        addTextChangedListener(this)
                        if (containNumbered(getCurrentLine(selectionEnd + 1))) {
                            setSelection(selectionEnd + 1)
                            doingInvalid(lineSignal)
                            doingValid(lineSignal)
                            setSelection(selectionEnd - 1)
                        }
                    } else {
                        setSelection(selectionStart - 1)
                        removeTextChangedListener(this)
                        text?.insert(selectionStart, "\u200b")
                        doingValid(lineSignal)
                        addTextChangedListener(this)
                        setSelection(selectionStart + 1)
                    }
                }
            }
        } else if (selectedLinesNumberList.size > 1 && deleteText != "\n") {
            // If we select multiLine
            Log.d("selectedLinesNumberList2", "${selectedLinesNumberList.size}")
            Log.d("ka", "|$deleteText|")
            val selectedStartLine = if (getCurrentLine(selectionStart) == -1) 1 else getCurrentLine(selectionStart)
//            val selectedEndLine = selectedStartLine + selectedLinesNumberList.size - 1
            Log.d("Index", "${beforeChangeIndexPositionStart.preIndex} ${beforeChangeIndexPositionStart.nexIndex}")
//            for (currentLineIndex in selectedStartLine..selectedEndLine) {
//                Log.d("Contain", "$currentLineIndex is ${beforeChangedList[currentLineIndex]}")
//            }
            Log.d("Index", "${beforeChangeIndexPositionEnd.preIndex} ${beforeChangeIndexPositionEnd.nexIndex}")

            if (beforeChangedList[selectedStartLine] != -1) {
                if (beforeChangeIndexPositionStart.preIndex == 0) {
                    if (beforeChangeIndexPositionEnd.nexIndex == 0) {
                        // 删除整行
//                        removeTextChangedListener(this)
                        if (beforeChangeIndexPositionEnd.preIndex == 0) {
                            Log.d("DOing", "1")
                            removeTextChangedListener(this)
                            text?.insert(selectionStart, "\u200b\n")
                            setSelection(selectionStart - 1)
                            val lineSignal = beforeChangedList[getCurrentLine(selectionStart)]
                            doingValid(lineSignal)
                            addTextChangedListener(this)
                        } else {
                            Log.d("DOing", "12")
                            if (getCurrentLineText(currentLine) == "") {
                                removeTextChangedListener(this)
                                text?.insert(selectionStart, "\u200b")
                                addTextChangedListener(this)
                            }
                            val lineSignal = beforeChangedList[getCurrentLine(selectionStart)]
                            Log.d("LLL", "$lineSignal")
                            doingValid(lineSignal)

                            if (afterChangedList[currentLine + 1] > 0) {
                                setSelection(selectionStart + 1)
                                numberedInvalid()
                                numberedValid()
                                setSelection(selectionStart - 1)
                            }
                        }
//                        addTextChangedListener(this)
                    } else if (beforeChangeIndexPositionEnd.preIndex == 0) {
                        Log.d("DOing", "13")
                        removeTextChangedListener(this)
                        text?.insert(selectionStart, "\u200b\n")
                        setSelection(selectionStart - 1)
                        val lineSignal = beforeChangedList[getCurrentLine(selectionStart)]
                        doingValid(lineSignal)
                        addTextChangedListener(this)
                    } else if (beforeChangeIndexPositionEnd.preIndex > 0) {
                        Log.d("DOing", "14")
                        val lineSignal = beforeChangedList[getCurrentLine(selectionStart)]
                        numberedInvalid()
                        quoteInvalid()
                        bulletInvalid()
                        doingValid(lineSignal)
                    }
                } else if (beforeChangeIndexPositionStart.nexIndex > 0) {
                    Log.d("DOing", "2")
                    if (beforeChangeIndexPositionEnd.preIndex == 0) {
                        Log.d("DOing", "21")
                        removeTextChangedListener(this)
                        text?.insert(selectionStart, "\n")
                        addTextChangedListener(this)
                        setSelection(selectionStart - 1)
                        if (containNumbered(selectedStartLine + 1)) {
                            Log.d("DOing", "211")
                            setSelection(selectionStart + 1)
                            val lineSignal = beforeChangedList[getCurrentLine(selectionStart)]
                            doingInvalid(lineSignal)
                            doingValid(lineSignal)
                            setSelection(selectionStart - 1)
                        }
                    } else if (beforeChangeIndexPositionEnd.preIndex > 0) {
                        Log.d("DOing", "22")
                        val lineSignal = beforeChangedList[currentLine]

                        bulletInvalid()
                        numberedInvalid()
                        quoteInvalid()

                        doingValid(lineSignal)
                        if (containNumbered(currentLine + 1)) {
                            val lineNexSize = beforeChangeIndexPositionEnd.nexIndex + 1
                            setSelection(selectionStart + lineNexSize)
                            numberedInvalid()
                            numberedValid()
                            setSelection(selectionStart - lineNexSize)
                        }
                    }
                }
            } else if (beforeChangedList[selectedStartLine] == -1) {
                Log.d("Start is", "${beforeChangedList[selectedStartLine]}")
                // TODD("Get span, 设置增加的数据为当前格式")
                Log.d("DOing", "3")

                bulletInvalid()
                numberedInvalid()
                quoteInvalid()

                if (containNumbered(currentLine + 1)) {
                    val lineNexSize = beforeChangeIndexPositionEnd.nexIndex + 1
                    setSelection(selectionStart + lineNexSize)
                    numberedInvalid()
                    numberedValid()
                    setSelection(selectionStart - lineNexSize)
                }
            }
        }

        Log.d("Result Line", "$selectionStart")

        // Do Noting
        if (!historyEnable || historyWorking) {
            return
        }

        inputLast = SpannableStringBuilder(text)
        if (text != null && text.toString() == inputBefore!!.toString()) {
            return
        }

        if (historyList.size >= historySize) {
            historyList.removeAt(0)
        }

        historyList.add(SelectionHistory(inputBefore!!, historySelectionStart, historySelectionEnd))
        Log.d("ADD History", "$historySelectionStart $historySelectionEnd")
        historyCursor = historyList.size
    }

    //TODO("记录样式 getSpan")
    fun redo() {
        if (!redoValid()) {
            return
        }

        historyWorking = true

        if (historyCursor >= historyList.size - 1) {
            historyCursor = historyList.size
            text = inputLast
            setSelection(editableText.length)
            Log.d("Doing 1", "$")
        } else {
            historyCursor++
//            text = historyList[historyCursor]
            text = historyList[historyCursor].spannableString
            Log.d("Doing 2", "${historyList[historyCursor].selectionStart}")
            setSelection(historyList[historyCursor].selectionStart, historyList[historyCursor].selectionEnd)
        }

        historyWorking = false
    }

    fun undo() {
        if (!undoValid()) {
            return
        }

        historyWorking = true

        historyCursor--
//        text = historyList[historyCursor]
        text = historyList[historyCursor].spannableString
//        setSelection(editableText.length)
        setSelection(historyList[historyCursor].selectionStart, historyList[historyCursor].selectionEnd)
        Log.d("unDoing", "${historyList[historyCursor].selectionStart} ${historyList[historyCursor].selectionEnd}")

        historyWorking = false
    }

    private fun redoValid(): Boolean {
        return if (!historyEnable || historySize <= 0 || historyList.size <= 0 || historyWorking) {
            false
        } else historyCursor < historyList.size - 1 || historyCursor >= historyList.size - 1 && inputLast != null

    }

    private fun undoValid(): Boolean {
        if (!historyEnable || historySize <= 0 || historyWorking) {
            return false
        }

        return !(historyList.size <= 0 || historyCursor <= 0)

    }

    fun clearHistory() {
        historyList.clear()
    }

// Helper ======================================================================================

    operator fun contains(format: Int): Boolean {
        return when (format) {
            FORMAT_BOLD -> containStyle(Typeface.BOLD, selectionStart, selectionEnd)
            FORMAT_ITALIC -> containStyle(Typeface.ITALIC, selectionStart, selectionEnd)
            FORMAT_UNDERLINED -> containUnderline(selectionStart, selectionEnd)
            FORMAT_STRIKETHROUGH -> containStrikethrough(selectionStart, selectionEnd)
            FORMAT_BULLET -> containBullet()
            FORMAT_QUOTE -> containQuote()
            FORMAT_LINK -> containLink(selectionStart, selectionEnd)
            //ADD
            FORMAT_NUMBERED -> containNumbered()
            else -> false
        }
    }

    fun clearFormats() {
        setText(editableText.toString())
        setSelection(editableText.length)
    }

    fun hideSoftInput() {
        clearFocus()
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, 0)
    }

    fun showSoftInput() {
        requestFocus()
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
    }

    fun fromHtml(source: String) {
        val builder = SpannableStringBuilder()
        builder.append(KnifeParser.fromHtml(source))
        switchToKnifeStyle(builder, 0, builder.length)
        //        Log.d("Builder size", String.valueOf(builder.length()));
        text = builder
    }

    fun toHtml(): String {
        return KnifeParser.toHtml(editableText)
    }

    private fun switchToKnifeStyle(editable: Editable, start: Int, end: Int) {
        //ADD Numbered Type
        //To save the list of Numbered
        val knifeNumberedLists = ArrayList<Int>()
        //To locate the current index
        var knifeNumberedIndex = 0
        val numberedSpans = editable.getSpans(start, end, KnifeNumberedSpan::class.java)
        for (span in numberedSpans) {
            val spanStart = editable.getSpanStart(span)
            //spanEnd -1
            var spanEnd = editable.getSpanEnd(span) - 1
//            Log.d("SPan", "start $spanStart end $spanEnd")
            if (knifeNumberedLists.isEmpty()) {
                knifeNumberedIndex = 1
            } else {
                val lastIndex = knifeNumberedLists[knifeNumberedLists.size - 1]
                //lastIndex + 1
                if (spanStart == lastIndex + 1) {
                    knifeNumberedIndex += 1
                } else {
                    knifeNumberedIndex = 1
                }
            }
            knifeNumberedLists.add(spanEnd)

            spanEnd = if (0 < spanEnd && spanEnd < editable.length && editable[spanEnd] == '\n') spanEnd - 1 else spanEnd
            editable.removeSpan(span)
//            Log.d("Span Numbered", "$spanStart $spanEnd")
            editable.setSpan(KnifeNumberedSpan(numberedColor, numberedGap, numberedTextSize, knifeNumberedIndex), spanStart, spanEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        }

        val knifeBulletSpans = editable.getSpans(start, end, KnifeBulletSpan::class.java)
        for (span in knifeBulletSpans) {
            val spanStart = editable.getSpanStart(span)
            var spanEnd = editable.getSpanEnd(span) - 1
            spanEnd = if (0 < spanEnd && spanEnd < editable.length && editable[spanEnd] == '\n') spanEnd - 1 else spanEnd
            editable.removeSpan(span)
//            Log.d("Span Bullet", "$spanStart $spanEnd")
            editable.setSpan(KnifeBulletSpan(bulletColor, bulletRadius, bulletGapWidth), spanStart, spanEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        val quoteSpans = editable.getSpans(start, end, QuoteSpan::class.java)
        for (span in quoteSpans) {
            val spanStart = editable.getSpanStart(span)
            var spanEnd = editable.getSpanEnd(span) - 1
            spanEnd = if (0 < spanEnd && spanEnd < editable.length && editable[spanEnd] == '\n') spanEnd - 1 else spanEnd
            editable.removeSpan(span)
//            Log.d("Span Quote", "$spanStart $spanEnd")
            editable.setSpan(KnifeQuoteSpan(quoteColor, quoteStripeWidth, quoteGapWidth), spanStart, spanEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        // URLSpan =====================================================================================
        val urlSpans = editable.getSpans(start, end, URLSpan::class.java)
        for (span in urlSpans) {
            val spanStart = editable.getSpanStart(span)
            val spanEnd = editable.getSpanEnd(span)
            editable.removeSpan(span)
            editable.setSpan(KnifeURLSpan(span.url, linkColor, linkUnderline), spanStart, spanEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    companion object {
        const val FORMAT_BOLD = 0x01
        const val FORMAT_ITALIC = 0x02
        const val FORMAT_UNDERLINED = 0x03
        const val FORMAT_STRIKETHROUGH = 0x04
        const val FORMAT_BULLET = 0x05
        const val FORMAT_QUOTE = 0x06
        const val FORMAT_LINK = 0x07
        //新增 Numbered
        const val FORMAT_NUMBERED = 0x08
    }
}

