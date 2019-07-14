package io.github.justyummy.knife

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.os.Parcel
import android.text.Layout
import android.text.Spanned
import android.text.style.BulletSpan
import io.github.justyummy.knife.SpanSequence

class KnifeBulletSpan : BulletSpan {

    private var bulletColor = DEFAULT_COLOR
    private var bulletR = DEFAULT_RADIUS
    private var bulletGapWidth = DEFAULT_GAP_WIDTH

    override fun getSpanTypeId(): Int {
        return SpanSequence.KNIFE_BULLET_SPAN
    }

    constructor(bulletColor: Int, bulletRadius: Int, bulletGapWidth: Int) {
        this.bulletColor = if (bulletColor != 0) bulletColor else DEFAULT_COLOR
        this.bulletR = if (bulletRadius != 0) bulletRadius else DEFAULT_RADIUS
        this.bulletGapWidth = if (bulletGapWidth != 0) bulletGapWidth else DEFAULT_GAP_WIDTH
    }

    constructor() {
        this.bulletColor = DEFAULT_COLOR
        this.bulletR = DEFAULT_RADIUS
        this.bulletGapWidth = DEFAULT_GAP_WIDTH
    }

    constructor(src: Parcel) : super(src) {
        this.bulletColor = src.readInt()
        this.bulletR = src.readInt()
        this.bulletGapWidth = src.readInt()
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        super.writeToParcel(dest, flags)
        dest.writeInt(bulletColor)
        dest.writeInt(bulletR)
        dest.writeInt(bulletGapWidth)
    }

    override fun getLeadingMargin(first: Boolean): Int {
        return 2 * bulletR + bulletGapWidth * 2
    }


    override fun drawLeadingMargin(c: Canvas, p: Paint, x: Int, dir: Int,
                                   top: Int, baseline: Int, bottom: Int,
                                   text: CharSequence, start: Int, end: Int,
                                   first: Boolean, l: Layout?) {
        if ((text as Spanned).getSpanStart(this) == start) {
            val style = p.style

            val oldColor = p.color
            p.color = bulletColor
            p.style = Paint.Style.FILL

            if (c.isHardwareAccelerated) {
                if (bulletPath == null) {
                    bulletPath = Path()
                    // Bullet is slightly better to avoid aliasing artifacts on mdpi devices.
                    bulletPath!!.addCircle(0.0f, 0.0f, bulletR.toFloat(), Path.Direction.CW)
                }

                c.save()

//                Log.d("Measure", "top $top bottom $bottom x $x baseline $baseline start $start end $end dir $dir" )

                // dy According to the  "android:lineSpacingExtra"
                c.translate((x + dir * bulletR).toFloat(), (top + baseline + 11) / 2.0f)
                c.drawPath(bulletPath!!, p)
                c.restore()
            } else {
                c.drawCircle((x + dir * bulletR).toFloat(), (top + baseline + 11) / 2.0f, bulletR.toFloat(), p)
            }

            p.color = oldColor
            p.style = style
        }
    }

    companion object {
        private const val DEFAULT_COLOR = 0
        private const val DEFAULT_RADIUS = 3
        private const val DEFAULT_GAP_WIDTH = 2
        private var bulletPath: Path? = null
    }
}


