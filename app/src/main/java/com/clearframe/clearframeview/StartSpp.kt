package com.clearframe.clearframeview
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class StartSpp : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startService(Intent(this, SppKeepaliveService::class.java))
        finish()
    }
}
