package com.seanime.app

import android.webkit.WebView

object FloatingPill {

    fun inject(webView: WebView) {
        val js = """
            (function() {
                const PILL_ID = 'android-floating-pill';
                const MENU_ID = 'android-floating-menu';
                const BUTTON_SELECTOR = '.UI-AppSidebarTrigger__trigger';
                const READER_DRAWER_SELECTOR = 'div[data-chapter-reader-drawer-content="true"]';

                const ICON_BURGER = '<svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="4" y1="12" x2="20" y2="12"></line><line x1="4" y1="6" x2="20" y2="6"></line><line x1="4" y1="18" x2="20" y2="18"></line></svg>';
                const ICON_CLOSE  = '<svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>';

                let visibleNavSnapshot = [];
                let menuOpen = false;

                function setMenuBtnIcon(icon) {
                    const btn = document.getElementById('floating-menu-btn');
                    if (btn) btn.innerHTML = icon;
                }

                function snapshotNavItems() {
                    const items = [];
                    document.querySelectorAll('.UI-AppSidebar__sidebar a[data-vertical-menu-item-link]').forEach(a => {
                        const hiddenByApp = a.style.display === 'none' && !a.dataset.hiddenByUs;
                        if (hiddenByApp) return;
                        const label = a.getAttribute('data-vertical-menu-item-link');
                        const href = a.getAttribute('href');
                        const isCurrent = a.getAttribute('data-current') === 'true';
                        const svgEl = a.querySelector('svg');
                        const svg = svgEl ? svgEl.outerHTML : '';
                        if (label && href) items.push({ type: 'link', label, href, svg, isCurrent });
                    });
                    document.querySelectorAll('.UI-AppSidebar__sidebar button[data-vertical-menu-item-button]').forEach(btn => {
                        const hiddenByApp = btn.style.display === 'none' && !btn.dataset.hiddenByUs;
                        if (hiddenByApp) return;
                        const label = btn.getAttribute('data-vertical-menu-item-button');
                        const svgEl = btn.querySelector('svg');
                        const svg = svgEl ? svgEl.outerHTML : '';
                        if (label) items.push({ type: 'button', label, svg });
                    });
                    return items;
                }

                function updateUIState() {
                    const pill = document.getElementById(PILL_ID);
                    const isReaderOpen = !!document.querySelector(READER_DRAWER_SELECTOR);
                    visibleNavSnapshot = snapshotNavItems();
                    
                    document.querySelectorAll(BUTTON_SELECTOR).forEach(btn => {
                        if (btn.id !== 'floating-menu-btn') {
                            btn.dataset.hiddenByUs = 'true';
                            btn.style.setProperty('display', 'none', 'important');
                        }
                    });

                    if (pill) {
                        if (isReaderOpen || visibleNavSnapshot.length === 0) {
                            pill.style.opacity = '0';
                            pill.style.pointerEvents = 'none';
                        } else {
                            pill.style.opacity = '1';
                            pill.style.pointerEvents = 'auto';
                        }
                    }
                }

                function buildMenu(items) {
                    let menu = document.getElementById(MENU_ID);
                    if (!menu) return;
                    menu.innerHTML = '';
                    const reversedItems = [...items].reverse();

                    reversedItems.forEach((item, i) => {
                        const btn = document.createElement('div');
                        btn.className = 'float-nav-item';
                        const tooltip = document.createElement('span');
                        tooltip.className = 'float-nav-tooltip';
                        tooltip.textContent = item.label;
                        const iconWrap = document.createElement('div');
                        iconWrap.className = 'float-nav-icon' + (item.isCurrent ? ' float-nav-icon--active' : '');
                        iconWrap.innerHTML = item.svg || '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/></svg>';
                        
                        const svgInner = iconWrap.querySelector('svg');
                        if (svgInner) {
                            svgInner.setAttribute('width', '22');
                            svgInner.setAttribute('height', '22');
                            svgInner.style.cssText = 'width: 22px !important; height: 22px !important; display: block !important; margin: 0 !important;';
                        }

                        btn.appendChild(tooltip);
                        btn.appendChild(iconWrap);
                        menu.appendChild(btn);

                        btn.addEventListener('click', (e) => {
                            e.stopPropagation();
                            closeMenu(() => {
                                if (item.type === 'link') window.location.href = item.href;
                                else if (item.type === 'button') {
                                    const target = document.querySelector('.UI-AppSidebar__sidebar button[data-vertical-menu-item-button="' + CSS.escape(item.label) + '"]');
                                    if (target) target.click();
                                }
                            });
                        });
                    });
                }

                function openMenu() {
                    const menu = document.getElementById(MENU_ID);
                    if (!menu) return;
                    buildMenu(visibleNavSnapshot);
                    menu.style.display = 'flex';
                    menu.style.pointerEvents = 'auto';
                    menuOpen = true;
                    setMenuBtnIcon(ICON_CLOSE);
                    menu.querySelectorAll('.float-nav-item').forEach((el, i) => {
                        el.style.opacity = '0';
                        el.style.transform = 'translateY(10px)';
                        setTimeout(() => {
                            el.style.transition = 'opacity 0.2s ease, transform 0.25s cubic-bezier(0.17, 0.67, 0.83, 0.67)';
                            el.style.opacity = '1';
                            el.style.transform = 'translateY(0)';
                        }, i * 40);
                    });
                }

                function closeMenu(onComplete) {
                    const menu = document.getElementById(MENU_ID);
                    if (!menu) {
                        menuOpen = false;
                        setMenuBtnIcon(ICON_BURGER);
                        if (onComplete) onComplete();
                        return;
                    }
                    menuOpen = false;
                    menu.style.pointerEvents = 'none';
                    setMenuBtnIcon(ICON_BURGER);
                    const items = menu.querySelectorAll('.float-nav-item');
                    if (items.length === 0) {
                        menu.style.display = 'none';
                        if (onComplete) onComplete();
                        return;
                    }
                    const reversedItems = [...items].reverse();
                    reversedItems.forEach((el, i) => {
                        setTimeout(() => {
                            el.style.transition = 'opacity 0.15s ease, transform 0.18s ease';
                            el.style.opacity = '0';
                            el.style.transform = 'translateY(8px)';
                        }, i * 30);
                    });
                    const totalDuration = (reversedItems.length - 1) * 30 + 180;
                    setTimeout(() => {
                        menu.style.display = 'none';
                        if (onComplete) onComplete();
                    }, totalDuration);
                }

                if (!document.getElementById(PILL_ID)) {
                    const style = document.createElement('style');
                    style.textContent = `
                        #android-floating-menu { position: fixed; bottom: 84px; right: 24px; width: 52px; z-index: 999998; display: none; flex-direction: column; align-items: center; gap: 12px; }
                        .float-nav-item { display: flex; align-items: center; justify-content: center; position: relative; width: 100%; }
                        .float-nav-tooltip { position: absolute; right: 64px; background: rgba(15,15,15,0.95); color: white; font-size: 13px; font-weight: 500; padding: 6px 12px; border-radius: 8px; white-space: nowrap; border: 1px solid rgba(255,255,255,0.1); backdrop-filter: blur(10px); pointer-events: none; }
                        .float-nav-icon { width: 44px !important; height: 44px !important; border-radius: 12px; background: rgba(25,25,25,0.9); border: 1px solid rgba(255,255,255,0.1); display: grid !important; place-items: center !important; color: #a0a0a0; box-shadow: 0 4px 10px rgba(0,0,0,0.4); }
                        .float-nav-icon--active { background: rgba(255,255,255,0.15); color: white; border-color: rgba(255,255,255,0.3); }
                    `;
                    document.head.appendChild(style);

                    const menuContainer = document.createElement('div');
                    menuContainer.id = MENU_ID;
                    document.body.appendChild(menuContainer);

                    const container = document.createElement('div');
                    container.id = PILL_ID;
                    container.style.cssText = "position:fixed;bottom:24px;right:24px;z-index:999999;background:rgba(20,20,20,0.9);backdrop-filter:blur(10px);border-radius:16px;padding:4px;display:grid;place-items:center;width:52px;height:52px;border:1px solid rgba(255,255,255,0.1);";
                    container.innerHTML = '<button id="floating-menu-btn" style="background:none;border:none;color:#ccc;width:44px;height:44px;display:grid;place-items:center;">' + ICON_BURGER + '</button>';
                    document.body.appendChild(container);

                    document.getElementById('floating-menu-btn').addEventListener('click', (e) => {
                        e.stopPropagation();
                        menuOpen ? closeMenu() : openMenu();
                    });
                    document.addEventListener('click', () => { if (menuOpen) closeMenu(); });
                }
                const observer = new MutationObserver(() => updateUIState());
                observer.observe(document.body, { childList: true, subtree: true, attributes: true, attributeFilter: ['style', 'class'] });
                updateUIState();
            })();
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }
}