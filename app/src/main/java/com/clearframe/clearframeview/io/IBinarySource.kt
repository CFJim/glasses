package com.clearframe.clearframeview.io
interface IBinarySource {
    fun start(onFrame: (ByteArray) -> Unit)
    fun stop()
}