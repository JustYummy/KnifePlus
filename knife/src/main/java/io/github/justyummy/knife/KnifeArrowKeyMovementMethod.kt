package io.github.justyummy.knife

import android.graphics.Rect
import android.text.Spannable
import android.text.method.ArrowKeyMovementMethod
import android.text.method.MovementMethod
import android.view.MotionEvent
import android.widget.TextView


class KnifeArrowKeyMovementMethod : ArrowKeyMovementMethod() {

    override fun onTouchEvent(widget: TextView, buffer: Spannable, event: MotionEvent): Boolean {
        return false
    }

    companion object {

        private var sInstance: KnifeArrowKeyMovementMethod? = null

        private val sLineBounds = Rect()

        val instance: MovementMethod
            @Synchronized get() {
                if (sInstance == null) {
                    sInstance = KnifeArrowKeyMovementMethod()
                }
                return sInstance as KnifeArrowKeyMovementMethod
            }
    }
}