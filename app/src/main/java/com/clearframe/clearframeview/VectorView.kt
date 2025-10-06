package com.clearframe.clearframeview
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.*
class VectorView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {
    private val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    @Volatile private var model: Model = Cfvx.unitCube()
    @Volatile private var angle: Float = 0f
    fun setModel(m: Model) { model = m; invalidate() }
    fun setAngle(a: Float) { angle = a; invalidate() }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w * 0.5f
        val cy = h * 0.5f
        val scale = min(w, h) * 0.8f
        val sinY = sin(angle); val cosY = cos(angle)
        val sinX = sin(angle*0.7f); val cosX = cos(angle*0.7f)
        fun project(v: Vertex): Pair<Float, Float> {
            val xY = v.x * cosY + v.z * sinY
            val zY = -v.x * sinY + v.z * cosY
            val yX = v.y * cosX - zY * sinX
            return Pair(cx + xY * scale, cy - yX * scale)
        }
        val verts2d = model.vertices.map { v -> project(v) }
        for (e in model.edges) {
            val (x1, y1) = verts2d[e.a]
            val (x2, y2) = verts2d[e.b]
            canvas.drawLine(x1, y1, x2, y2, paint)
        }
    }
}
