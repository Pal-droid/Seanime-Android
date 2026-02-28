package com.seanime.app

import android.webkit.WebView

object UIPatches {

    fun inject(webView: WebView) {
        injectSettingsFix(webView)
    }

    private fun injectSettingsFix(webView: WebView) {
        val js = """
            (function() {
                var style = document.getElementById('__seanime_settings_fix');
                if (style) return;
                style = document.createElement('style');
                style.id = '__seanime_settings_fix';
                style.textContent = `
                    [data-open-issue-recorder-button="true"] {
                        transform: none !important;
                        transition: background-color 0.2s ease, box-shadow 0.2s ease !important;
                    }
                    [data-open-issue-recorder-button="true"]:hover {
                        transform: none !important;
                    }
                    .UI-Card__content .pb-3.flex.gap-2 {
                        flex-wrap: wrap !important;
                        overflow: hidden !important;
                    }
                    .UI-Card__content .pb-3.flex.gap-2 button {
                        flex-shrink: 1 !important;
                        min-width: 0 !important;
                        max-width: 100% !important;
                    }
                `;
                document.head.appendChild(style);
            })();
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }
}