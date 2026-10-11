(function () {
    'use strict';

    if (window.__calcbox_injected) {
        return;
    }
    window.__calcbox_injected = true;

    /**
     * Injects CSS to suppress app upsell banners, mobile header overlays,
     * and enforce dark theme consistency to match the calculator vault aesthetic.
     */
    function injectStyles() {
        try {
            if (!document.getElementById('__calcbox_injected_styles')) {
                const style = document.createElement('style');
                style.id = '__calcbox_injected_styles';
                style.textContent = `
                    /* Suppress "Open in App" banners, upsell banners, and store redirects */
                    div[data-testid="app-upsell"],
                    div[data-testid="mobile-app-banner"],
                    a[href*="play.google.com/store/apps"],
                    a[href*="itunes.apple.com"],
                    a[href*="apps.apple.com"],
                    a[href*="instagram.com/download"],
                    .app-banner,
                    .native-banner,
                    .mobile-banner,
                    .open-in-app,
                    .get-the-app {
                        display: none !important;
                        visibility: hidden !important;
                        height: 0 !important;
                        max-height: 0 !important;
                        opacity: 0 !important;
                        pointer-events: none !important;
                        overflow: hidden !important;
                    }

                    /* Dark theme integration matching vault aesthetic */
                    :root {
                        --primary-background: #000000 !important;
                        --secondary-background: #121212 !important;
                    }
                    html, body {
                        background-color: #000000 !important;
                        color: #f5f5f5 !important;
                        -webkit-tap-highlight-color: transparent !important;
                    }
                `;
                (document.head || document.documentElement).appendChild(style);
            }
        } catch (_) {}
    }

    /**
     * Suppresses "Open in App" floating headers, banners, and app install popups.
     */
    function suppressAppBanners() {
        try {
            // Find links pointing to Play Store / App Store / app intent schemes
            const storeLinks = document.querySelectorAll(
                'a[href*="play.google.com"], a[href*="itunes.apple.com"], a[href*="apps.apple.com"], a[href*="instagram://"]'
            );
            storeLinks.forEach(function (link) {
                if (link.dataset.calcboxProcessed) return;
                link.dataset.calcboxProcessed = 'true';

                let el = link;
                let banner = null;
                for (let i = 0; i < 6 && el && el !== document.body; i++) {
                    const pos = window.getComputedStyle(el).position;
                    if (pos === 'fixed' || pos === 'sticky') {
                        banner = el;
                        break;
                    }
                    el = el.parentElement;
                }
                const target = banner || link.closest('div[role="banner"]') || link.parentElement;
                if (target && target !== document.body) {
                    target.style.setProperty('display', 'none', 'important');
                    target.style.setProperty('visibility', 'hidden', 'important');
                    target.style.setProperty('pointer-events', 'none', 'important');
                }
            });

            // Suppress dialog overlays promoting the app installation
            const dialogs = document.querySelectorAll('div[role="dialog"], div[role="alertdialog"], [role="dialog"]');
            dialogs.forEach(function (dialog) {
                const text = (dialog.textContent || '').toLowerCase();
                const isAppUpsell =
                    text.includes('open in the instagram app') ||
                    text.includes('open in app') ||
                    text.includes('get the instagram app') ||
                    text.includes('experience instagram on the app') ||
                    text.includes('use the app') ||
                    text.includes('get the app');

                if (isAppUpsell) {
                    const closeBtn = dialog.querySelector('button[aria-label="Close"], button[aria-label="Dismiss"]') ||
                        Array.from(dialog.querySelectorAll('button')).find(function (b) {
                            const t = (b.textContent || '').toLowerCase();
                            return t.includes('close') || t.includes('not now') || t.includes('dismiss');
                        });

                    if (closeBtn) {
                        if (!closeBtn.dataset.calcboxClicked) {
                            closeBtn.dataset.calcboxClicked = 'true';
                            closeBtn.click();
                        }
                    } else {
                        dialog.style.setProperty('display', 'none', 'important');
                        const parent = dialog.parentElement;
                        if (parent && parent !== document.body && window.getComputedStyle(parent).position === 'fixed') {
                            parent.style.setProperty('display', 'none', 'important');
                        }
                    }
                }
            });
        } catch (_) {}
    }

    /**
     * Auto-dismisses "Save Your Login Info?" and "Turn on Notifications?" prompts by clicking "Not Now".
     */
    function dismissPopups() {
        try {
            const containers = document.querySelectorAll('div[role="dialog"], div[role="alertdialog"], [role="dialog"]');
            containers.forEach(function (container) {
                const text = (container.textContent || '').toLowerCase();
                const isLoginInfo =
                    text.includes('save your login info') ||
                    text.includes('save login info') ||
                    text.includes('save login');
                const isNotifications =
                    text.includes('turn on notifications') ||
                    (text.includes('notifications') && text.includes('know right away'));

                if (isLoginInfo || isNotifications) {
                    const buttons = container.querySelectorAll('button, div[role="button"]');
                    for (let i = 0; i < buttons.length; i++) {
                        const btn = buttons[i];
                        const btnText = (btn.textContent || '').trim().toLowerCase();
                        if (btnText === 'not now' || btnText === 'cancel' || btnText === 'dismiss') {
                            if (!btn.dataset.calcboxClicked) {
                                btn.dataset.calcboxClicked = 'true';
                                btn.click();
                            }
                            break;
                        }
                    }
                }
            });

            // Standalone "Not Now" buttons on confirmation views
            const standaloneButtons = document.querySelectorAll('button, div[role="button"]');
            standaloneButtons.forEach(function (btn) {
                const btnText = (btn.textContent || '').trim().toLowerCase();
                if (btnText === 'not now') {
                    const container = btn.closest('div[role="dialog"], div[role="alertdialog"], [role="dialog"], header, nav');
                    const containerText = (container ? container.textContent : '').toLowerCase();
                    if (
                        containerText.includes('notification') ||
                        containerText.includes('login') ||
                        containerText.includes('save')
                    ) {
                        if (!btn.dataset.calcboxClicked) {
                            btn.dataset.calcboxClicked = 'true';
                            btn.click();
                        }
                    }
                }
            });
        } catch (_) {}
    }

    function runPass() {
        injectStyles();
        suppressAppBanners();
        dismissPopups();
    }

    // Run initial pass immediately
    runPass();

    // Run on DOM ready if still loading
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', runPass);
    }

    // Modern MutationObserver to continuously watch dynamic React DOM changes
    let debounceTimer = null;
    const observer = new MutationObserver(function () {
        if (debounceTimer) return;
        debounceTimer = setTimeout(function () {
            debounceTimer = null;
            runPass();
        }, 120);
    });

    try {
        const target = document.documentElement || document.body;
        if (target) {
            observer.observe(target, {
                childList: true,
                subtree: true
            });
        }
    } catch (_) {}
})();
