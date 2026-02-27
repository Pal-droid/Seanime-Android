package com.seanime.app

import android.webkit.WebView

object DualModeManager {

    fun inject(view: WebView) {
        val js = """
            javascript:(function() {
                if (window.__seanimeDualModeInjected) return;
                window.__seanimeDualModeInjected = true;

                const style = document.createElement('style');
                style.innerHTML = `
                    .seanime-dual-mode-active [data-chapter-vertical-reader-inner-container="true"] {
                        display: grid !important;
                        grid-template-columns: 1fr 1fr !important;
                        direction: rtl !important;
                        column-gap: 0 !important;
                        row-gap: 0 !important;
                    }
                    .seanime-dual-mode-active [data-chapter-vertical-reader-inner-container="true"] > * {
                        direction: ltr !important;
                        margin: 0 !important;
                    }
                    .seanime-dual-mode-active [data-chapter-page-container="true"] {
                        height: 100vh !important;
                        display: flex !important;
                        justify-content: center !important;
                        align-items: center !important;
                    }
                    .seanime-dual-mode-active [data-chapter-page-container="true"] img {
                        max-height: 100vh !important;
                        width: auto !important;
                        object-fit: contain !important;
                    }
                    .seanime-dual-mode-active [data-chapter-vertical-reader-inner-container-spacer="true"] {
                        display: none !important;
                    }
                `;
                document.head.appendChild(style);

                function toggleLandscape(enable) {
                    if (window.OrientationBridge) {
                        window.OrientationBridge.setLandscape(enable);
                    }
                }

                function applyDualMode() {
                    document.body.classList.add('seanime-dual-mode-active');
                    toggleLandscape(true);
                }

                function removeDualMode() {
                    document.body.classList.remove('seanime-dual-mode-active');
                    toggleLandscape(false);
                }

                function setupUI() {
                    const doubleBtn = document.querySelector('button[value="double-page"]');
                    if (doubleBtn && !doubleBtn.dataset.hijacked) {
                        doubleBtn.dataset.hijacked = "true";
                        const parentLabel = doubleBtn.closest('label.UI-RadioGroup__itemContainer');
                        if (parentLabel) {
                            const span = parentLabel.querySelector('span > span:last-child');
                            if (span) span.innerText = "Double Page (Landscape)";

                            const toggleHandler = (e) => {
                                e.preventDefault();
                                e.stopPropagation();
                                if (e.type !== 'click') return;

                                localStorage.setItem('customDualMode', 'true');
                                applyDualMode();
                            };
                            parentLabel.addEventListener('click', toggleHandler, true);
                        }
                    }

                    ['paged', 'long-strip'].forEach(val => {
                        const btn = document.querySelector('button[value="' + val + '"]');
                        if (btn && !btn.dataset.hijacked) {
                            btn.dataset.hijacked = "true";
                            btn.closest('label.UI-RadioGroup__itemContainer')?.addEventListener('click', () => {
                                localStorage.setItem('customDualMode', 'false');
                                removeDualMode();
                            }, true);
                        }
                    });
                }

                const observer = new MutationObserver(() => {
                    setupUI();
                    const pages = document.querySelectorAll('div[data-chapter-page-container="true"]');
                    const isDual = localStorage.getItem('customDualMode') === 'true';

                    if (pages.length > 0 && isDual) {
                        applyDualMode();
                    } else if (pages.length === 0) {
                        removeDualMode();
                    }
                });

                observer.observe(document.body, { childList: true, subtree: true });
            })();
        """.trimIndent()
        view.evaluateJavascript(js, null)
    }
}