package com.seanime.app

import android.webkit.WebView

object VideoControlInjector {

    fun inject(view: WebView) {
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
                top.__seanimeBlockHide = false;
                bot.__seanimeBlockHide = false;
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

                container.addEventListener('click', function(e) {
                    if (!isEntryRoute()) return;

                    var top = getTopBar();
                    var bot = getBottomBar();
                    if (!top || !bot) return;

                    var currentlyVisible = !isHideTransform(top.style.transform);

                    if (currentlyVisible) {
                        top.__seanimeBlockHide = true;
                        bot.__seanimeBlockHide = true;
                        scheduleHide();
                    } else {
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

            new MutationObserver(function() {
                if (!patched) patchPlayer();
            }).observe(document.body, { childList: true, subtree: true });

            patchPlayer();
        })();
        """.trimIndent()

        view.evaluateJavascript(js, null)
    }
}