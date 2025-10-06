package com.clearframe.clearframeview
import java.nio.ByteBuffer
import java.nio.ByteOrder
data class Vertex(val x:Float, val y:Float, val z:Float)
data class Edge(val a:Int, val b:Int)
data class Model(val vertices: List<Vertex>, val edges: List<Edge>)
object Cfvx {
    fun parse(bytes: ByteArray): Model? {
        if (bytes.size < 10) return null
        val bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        if (!((bb.get().toInt() and 0xFF)==0x43 && (bb.get().toInt() and 0xFF)==0x46 &&
              (bb.get().toInt() and 0xFF)==0x56 && (bb.get().toInt() and 0xFF)==0x58)) return null
        val version = bb.short.toInt() and 0xFFFF
        if (version != 1) return null
        val vcnt = bb.short.toInt() and 0xFFFF
        val ecnt = bb.short.toInt() and 0xFFFF
        if (bytes.size < 10 + vcnt * 12 + ecnt * 4) return null
        val verts = ArrayList<Vertex>(vcnt)
        repeat(vcnt) { verts.add(Vertex(bb.float, bb.float, bb.float)) }
        val edges = ArrayList<Edge>(ecnt)
        repeat(ecnt) { edges.add(Edge(bb.short.toInt() and 0xFFFF, bb.short.toInt() and 0xFFFF)) }
        return Model(verts, edges)
    }
    fun unitCube(): Model {
        val v = listOf(
            Vertex(-0.5f,-0.5f,-0.5f), Vertex(0.5f,-0.5f,-0.5f),
            Vertex(0.5f,0.5f,-0.5f), Vertex(-0.5f,0.5f,-0.5f),
            Vertex(-0.5f,-0.5f,0.5f), Vertex(0.5f,-0.5f,0.5f),
            Vertex(0.5f,0.5f,0.5f), Vertex(-0.5f,0.5f,0.5f)
        )
        val eIdx = arrayOf(0 to 1,1 to 2,2 to 3,3 to 0,4 to 5,5 to 6,6 to 7,7 to 4,0 to 4,1 to 5,2 to 6,3 to 7)
        return Model(v, eIdx.map { Edge(it.first, it.second) })
    }
}
