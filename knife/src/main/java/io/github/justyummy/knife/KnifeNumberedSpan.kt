package io.github.justyummy.knife

import android.graphics.Canvas
import android.graphics.Paint
import android.os.Parcel
import android.text.Layout
import android.text.Spanned
import android.text.style.BulletSpan


class KnifeNumberedSpan : BulletSpan {
    private var numberedColor = DEFAULT_COLOR
    private var numberedGap = DEFAULT_GAP
    private var numberedTextSize = DEFAULT_TEXT_SIZE
    private var numberedLines = DEFAULT_LINES
    private var textShowWidth: Float = DEFAULT_SHOW_WIDTH

    constructor(numberedColor: Int, numberedGap: Float, numberedTextSize: Float, numberedLines: Int) {
        this.numberedColor = if (numberedColor != 0) numberedColor else DEFAULT_COLOR
        this.numberedGap = if (numberedGap != 0f) numberedGap else DEFAULT_GAP
        this.numberedTextSize = if (numberedTextSize != 0f) numberedTextSize else DEFAULT_TEXT_SIZE
        this.numberedLines = if (numberedLines != 0) numberedLines else DEFAULT_LINES
    }

    constructor() {
        this.numberedColor = DEFAULT_COLOR
        this.numberedGap = DEFAULT_GAP
        this.numberedTextSize = DEFAULT_TEXT_SIZE
        this.numberedLines = DEFAULT_LINES
    }

    constructor(src: Parcel) : super(src) {
        this.numberedColor = src.readInt()
        this.numberedGap = src.readFloat()
        this.numberedTextSize = src.readFloat()
        this.numberedLines = src.readInt()
    }

    override fun getSpanTypeId(): Int {
        return SpanSequence.KNIFE_NUMBER_SPAN
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        super.writeToParcel(dest, flags)
        dest.writeInt(numberedColor)
        dest.writeFloat(numberedGap)
        dest.writeFloat(numberedTextSize)
        dest.writeInt(numberedLines)
    }

    override fun getLeadingMargin(first: Boolean): Int {
        // Use TextSize to find out the gap
        val pointSize = numberedTextSize * 0.26
        val numberSize = numberedTextSize * 0.555

        val tempLength = numberedLines.toString().length
        val fontSize = if (tempLength <= 2) 2 * numberSize else tempLength * numberSize

        return (numberedGap + pointSize + fontSize).toInt()
    }


    override fun drawLeadingMargin(c: Canvas, p: Paint, x: Int, dir: Int,
                                   top: Int, baseline: Int, bottom: Int,
                                   text: CharSequence, start: Int, end: Int,
                                   first: Boolean, l: Layout?) {

        if ((text as Spanned).getSpanStart(this) == start) {
            if (text.getSpanStart(this) == start) {
//                val outSize = determineTextSize(text, start, end, p.textSize)

                val style = p.style

                val oldColor = p.color
                p.color = numberedColor
                p.style = Paint.Style.FILL
                textShowWidth = p.measureText("$numberedLines.")

//                p.textSize = (bottom - top) * 0.8f     // text size setting
//                Log.d("Size2", (bottom - top).toString())
//                Log.d("TextSize", p.textSize.toString() + " " + ((bottom - top)).toString())
                if (c.isHardwareAccelerated) {
                    c.save()
                    c.drawText("$numberedLines.", x.toFloat(), baseline.toFloat(), p)
//                    c.translate((x + dir * 2).toFloat(), (top + bottom) / 2.0f)
                    c.restore()
                } else {
                    c.drawText("$numberedLines.", x.toFloat(), baseline.toFloat(), p)
                }

//                Log.d("TextSize", "${p.textSize}")
//                p.textSize = 47f
                p.color = oldColor
                p.style = style
            }
        }
    }

    companion object {
        private const val DEFAULT_COLOR = 0
        private const val DEFAULT_GAP = 20f
        private const val DEFAULT_TEXT_SIZE = 47f
        private const val DEFAULT_LINES = 1
        private const val DEFAULT_SHOW_WIDTH = 80f
    }
}

