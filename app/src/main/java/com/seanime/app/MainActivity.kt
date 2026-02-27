package com.seanime.app

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.webkit.*
import android.widget.FrameLayout

class MainActivity : Activity() {

    private lateinit var webView: WebView
    private lateinit var pipManager: PiPManager
    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null

    private val retryCountMap = mutableMapOf<WebView, Int>()
    private val MAX_RETRIES = 5

    inner class OrientationBridge {
        @JavascriptInterface
        fun setLandscape(landscape: Boolean) {
            runOnUiThread {
                requestedOrientation = if (landscape) {
                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                } else {
                    ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf("android.permission.POST_NOTIFICATIONS"), 101)
        }

        setupWebView()
        startSeanimeService()
    }

    private fun startSeanimeService() {
        val intent = Intent(this, SeanimeService::class.java)
        startForegroundService(intent)
    }

    private fun setupWebView() {
        webView = WebView(this)
        setContentView(webView)

        pipManager = PiPManager(this, webView)
        pipManager.registerBridge()

        webView.addJavascriptInterface(OrientationBridge(), "OrientationBridge")

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            cacheMode = WebSettings.LOAD_DEFAULT
            mediaPlaybackRequiresUserGesture = false
            userAgentString = userAgentString.replace("; wv", "")
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onReceivedError(view: WebView?, errorCode: Int, desc: String?, url: String?) {
                if (view != null && url != null && url == view.url) {
                    retry(view)
                }
            }

            override fun onReceivedError(view: WebView?, req: WebResourceRequest?, err: WebResourceError?) {
                if (req?.isForMainFrame == true) retry(view)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (view != null) retryCountMap[view] = 0
                FloatingPill.inject(webView)
                pipManager.injectHijacker()
                DualModeManager.inject(webView)
                injectVideoControlBehavior(webView)
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                if (customView != null) {
                    onHideCustomView()
                    return
                }
                customView = view
                customViewCallback = callback
                val decor = window.decorView as FrameLayout
                decor.addView(customView, FrameLayout.LayoutParams(-1, -1))
                webView.visibility = View.GONE
                toggleSystemBars(true)
            }

            override fun onHideCustomView() {
                val decor = window.decorView as FrameLayout
                decor.removeView(customView)
                customView = null
                customViewCallback?.onCustomViewHidden()
                webView.visibility = View.VISIBLE
                toggleSystemBars(false)
            }
        }

        webView.postDelayed({
            webView.loadUrl("http://127.0.0.1:43211")
        }, 1000)
    }

    private fun injectVideoControlBehavior(view: WebView) {
        // language=JavaScript
        val js = """
        (function() {
            if (window.__seanimeControlPatchActive) return;
            window.__seanimeControlPatchActive = true;

            var HIDE_DELAY_MS = 3000;
            var hideTimer = null;
            var patched = false;
            var observersAttached = false;

            function isEntryRoute() {
                var url = window.location.pathname + window.location.search;
                return url.indexOf('/entry') !== -1 && url.indexOf('id=') !== -1;
            }

            function getTopBar() {
                return document.querySelector('[data-vc-element="mobile-control-bar-top-section"]');
            }
            function getBottomBar() {
                return document.querySelector('[data-vc-element="mobile-control-bar-bottom-section"]');
            }

            function isHideTransform(transform) {
                if (!transform || transform === 'none' || transform === '') return false;
                var match = transform.match(/translateY\(([^)]+)\)/);
                if (!match) return false;
                return parseFloat(match[1]) !== 0;
            }

            function hideBars() {
                var top = getTopBar();
                var bot = getBottomBar();
                if (!top || !bot) return;
                // Unblock observers first so they don't fight us
                top.__seanimeBlockHide = false;
                bot.__seanimeBlockHide = false;
                // Actively push bars offscreen — matches what the native player does
                top.style.transform = 'translateY(-100%)';
                bot.style.transform = 'translateY(100%)';
            }

            function scheduleHide() {
                clearTimeout(hideTimer);
                hideTimer = setTimeout(function() {
                    hideBars();
                }, HIDE_DELAY_MS);
            }

            function patchPlayer() {
                if (!isEntryRoute()) return;
                if (patched) return;

                var topBar = getTopBar();
                var bottomBar = getBottomBar();
                if (!topBar || !bottomBar) return;

                patched = true;
                topBar.__seanimeBlockHide = false;
                bottomBar.__seanimeBlockHide = false;

                if (!observersAttached) {
                    observersAttached = true;

                    function watchBar(el) {
                        new MutationObserver(function(mutations) {
                            mutations.forEach(function(m) {
                                if (m.attributeName !== 'style') return;
                                if (!isEntryRoute()) return;
                                if (!el.__seanimeBlockHide) return;
                                // Native player tried to hide — force back visible
                                if (isHideTransform(el.style.transform)) {
                                    el.style.transform = 'translateY(0px)';
                                }
                            });
                        }).observe(el, { attributes: true, attributeFilter: ['style'] });
                    }

                    watchBar(topBar);
                    watchBar(bottomBar);
                }

                var container = document.querySelector('[data-vc-element="container"]');
                if (!container || container.__seanimeClickPatched) return;
                container.__seanimeClickPatched = true;

                // Bubble phase — does NOT interfere with pause/play or any other native handlers
                container.addEventListener('click', function(e) {
                    if (!isEntryRoute()) return;

                    var top = getTopBar();
                    var bot = getBottomBar();
                    if (!top || !bot) return;

                    var currentlyVisible = !isHideTransform(top.style.transform);

                    if (currentlyVisible) {
                        // Controls already visible: block native hide, restart our 3s timer
                        top.__seanimeBlockHide = true;
                        bot.__seanimeBlockHide = true;
                        scheduleHide();
                    } else {
                        // Controls were hidden: let native show them first, then block + start timer
                        setTimeout(function() {
                            var t = getTopBar();
                            var b = getBottomBar();
                            if (t && b) {
                                t.__seanimeBlockHide = true;
                                b.__seanimeBlockHide = true;
                                scheduleHide();
                            }
                        }, 100);
                    }
                }, false);
            }

            // Re-patch on SPA navigation
            var _pushState = history.pushState.bind(history);
            history.pushState = function() {
                _pushState.apply(history, arguments);
                patched = false;
                setTimeout(patchPlayer, 500);
            };
            var _replaceState = history.replaceState.bind(history);
            history.replaceState = function() {
                _replaceState.apply(history, arguments);
                patched = false;
                setTimeout(patchPlayer, 500);
            };
            window.addEventListener('popstate', function() {
                patched = false;
                setTimeout(patchPlayer, 500);
            });

            // Patch when player mounts into DOM
            new MutationObserver(function() {
                if (!patched) patchPlayer();
            }).observe(document.body, { childList: true, subtree: true });

            patchPlayer();
        })();
        """.trimIndent()

        view.evaluateJavascript(js, null)
    }

    private fun retry(view: WebView?) {
        view ?: return
        val count = retryCountMap.getOrDefault(view, 0)
        if (count >= MAX_RETRIES) return
        retryCountMap[view] = count + 1
        val delayMs = (count + 1) * 1000L
        view.postDelayed({ view.reload() }, delayMs)
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        pipManager.onPiPModeChanged(isInPictureInPictureMode)
    }

    private fun toggleSystemBars(hide: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let {
                if (hide) {
                    it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                    it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                } else {
                    it.show(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                }
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = if (hide) {
                (View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
            } else View.SYSTEM_UI_FLAG_VISIBLE
        }
    }

    override fun onBackPressed() {
        if (customView != null) {
            webView.webChromeClient?.onHideCustomView()
        } else if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}