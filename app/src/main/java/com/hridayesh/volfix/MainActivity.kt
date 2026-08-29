package com.hridayesh.volfix

import android.app.Activity
import android.os.Bundle

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        finish() // opens and instantly closes — just to "wake" the app
    }
}