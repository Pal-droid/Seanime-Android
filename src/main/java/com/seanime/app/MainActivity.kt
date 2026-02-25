package com.seanime.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import java.io.File
import java.io.FileOutputStream
import kotlin.concurrent.thread

class MainActivity : Activity() {

    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Setup WebView first so the user sees something
        setupWebView()

        startSeanimeService()
    
    }

    private fun startSeanimeService() {
        val intent = Intent(this, SeanimeService::class.java)
        // Note: startForegroundService is required for Android 8.0+
        startForegroundService(intent)
    }

    private fun setupWebView() {
        webView = WebView(this)
        setContentView(webView)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            cacheMode = WebSettings.LOAD_DEFAULT
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onReceivedError(
                view: WebView?,
                errorCode: Int,
                description: String?,
                failingUrl: String?
            ) {
                // If the server isn't up yet, retry every 2 seconds
                view?.postDelayed({
                    view.reload()
                }, 2000)
            }
        }

        // Use 127.0.0.1 instead of localhost for better Android compatibility
        webView.postDelayed({
            webView.loadUrl("http://127.0.0.1:43211")
        }, 2000)
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}