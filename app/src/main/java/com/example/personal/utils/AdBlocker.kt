package com.example.personal.utils

import android.content.Context
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Enhanced Ad Blocker with comprehensive blocking rules
 * - Blocks known ad networks, trackers, popups
 * - Prevents redirections and link shorteners
 * - Blocks crypto miners and malware
 * - Detects and blocks suspicious URL patterns
 */
object AdBlocker {
    private const val TAG = "AdBlocker"
    
    private val adHosts = mutableSetOf<String>()
    private var isInitialized = false
    
    // Comprehensive redirect/shortener patterns
    private val redirectPatterns = listOf(
        // URL Shorteners
        "bit.ly", "goo.gl", "tinyurl.com", "ow.ly", "is.gd", "buff.ly",
        "adf.ly", "bc.vc", "ouo.io", "ouo.press", "shorte.st", "sh.st",
        "adfoc.us", "j.gs", "q.gs", "ay.gy", "zo.ee", "linkshrink.net",
        "shrinkme.io", "exe.io", "fc.lc", "gestyy.com", "za.gl",
        "cutt.ly", "shorturl.at", "rebrand.ly", "t.co", "v.gd",
        
        // Redirect patterns
        "redirect", "/go/", "/out/", "/outgoing/", "/exit/",
        "/away/", "/leave/", "/link/", "/click/", "track",
        "url=http", "url=https", "goto=", "dest=", "target=",
        "redirect_to=", "return_url=", "next=", "continue="
    )
    
    // Popup/Ad keywords in URLs
    private val adKeywords = listOf(
        "popup", "popunder", "popad", "interstitial", "preroll",
        "banner", "sponsor", "promo", "advert", "adsense",
        "/ads/", "/ad/", "/adx/", "/adsrv/", "/adserv/",
        "doubleclick", "googlesyndication", "googleadservices",
        "adservice", "pagead", "pubads", "/adframe/",
        "adserver", "adnetwork", "adexchange", "adzerk",
        "adform", "adnxs", "openx", "pubmatic", "rubiconproject",
        "tapad", "turn.com", "advertising.com", "casalemedia",
        "mathtag", "bluekai", "quantcast", "scorecardresearch"
    )
    
    // Block these file types often used for tracking
    private val blockedFilePatterns = listOf(
        "beacon.js", "tracker.js", "pixel.gif", "pixel.png",
        "spacer.gif", "blank.gif", "1x1.gif", "1x1.png",
        "tracking.js", "analytics.js", "adsbygoogle.js"
    )
    
    // Suspicious domains to always block
    private val alwaysBlockDomains = setOf(
        // Major Ad Networks
        "doubleclick.net", "googleadservices.com", "googlesyndication.com",
        "pagead2.googlesyndication.com", "adservice.google.com",
        "ads.google.com", "adsense.google.com", "adwords.google.com",
        
        // Popup Networks
        "popads.net", "popcash.net", "popunder.net", "propellerads.com",
        "trafficjunky.com", "exoclick.com", "juicyads.com", "adsterra.com",
        "revenuehits.com", "hilltopads.com", "clickadu.com", "clickaine.com",
        "popmyads.com", "adcash.com", "mgid.com", "taboola.com",
        "outbrain.com", "revcontent.com", "content.ad", "zergnet.com",
        
        // Mobile Ad Networks
        "admob.com", "mopub.com", "applovin.com", "unityads.unity3d.com",
        "chartboost.com", "vungle.com", "ironsrc.com", "tapjoy.com",
        "inmobi.com", "adcolony.com", "fyber.com", "smaato.com",
        
        // Tracking & Analytics (aggressive blocking)
        "google-analytics.com", "analytics.google.com", "hotjar.com",
        "mixpanel.com", "amplitude.com", "segment.io", "segment.com",
        "branch.io", "appsflyer.com", "adjust.com", "kochava.com",
        "singular.net", "clearbrain.com", "leanplum.com",
        
        // Retargeting
        "criteo.com", "adroll.com", "media.net", "retargetlinks.com",
        "steelhousemedia.com", "perfectaudience.com",
        
        // Social Media Tracking
        "connect.facebook.net", "pixel.facebook.com", "facebook.net/tr",
        "ads.twitter.com", "analytics.twitter.com", "ads.linkedin.com",
        
        // Crypto Mining
        "coinhive.com", "coin-hive.com", "jsecoin.com", "crypto-loot.com",
        "cryptonight.wasm", "authedmine.com", "ppoi.org",
        
        // Overlay & Interstitial
        "optimizely.com", "crazyegg.com", "clicktale.com",
        "sessioncam.com", "fullstory.com", "mouseflow.com"
    )
    
    // Streaming site specific ad patterns
    private val streamingSiteAdPatterns = listOf(
        // Common streaming site ad paths
        "/ads/", "/adv/", "/advert/", "/banners/",
        "streamtape.com/ads", "vidoza.net/ads", "doodstream.com/ads",
        "upstream.to/ads", "filemoon.sx/ads", "streamwish.to/ads",
        "voe.sx/ads", "mixdrop.co/ads", "fembed.com/ads",
        
        // Popup triggers
        "onclick", "onmousedown", "popunder", "popundr",
        "window.open", "tabunder", "newwindow"
    )

    fun loadAdBlockRules(context: Context) {
        if (isInitialized) return
        
        try {
            context.assets.open("adblock_rules.txt").use {
                BufferedReader(InputStreamReader(it)).forEachLine { line ->
                    val trimmed = line.trim()
                    if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                        adHosts.add(trimmed.lowercase())
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load adblock rules file", e)
        }
        
        // Add built-in always block domains
        adHosts.addAll(alwaysBlockDomains)
        
        isInitialized = true
        Log.d(TAG, "AdBlocker initialized with ${adHosts.size} blocked domains")
    }

    /**
     * Check if URL should be blocked (comprehensive check)
     */
    fun isAd(url: String): Boolean {
        val lowerUrl = url.lowercase()
        
        // Check against blocked hosts
        if (adHosts.any { lowerUrl.contains(it) }) return true
        
        // Check against ad keywords
        if (adKeywords.any { lowerUrl.contains(it) }) return true
        
        // Check against blocked file patterns
        if (blockedFilePatterns.any { lowerUrl.contains(it) }) return true
        
        // Check streaming site specific patterns
        if (streamingSiteAdPatterns.any { lowerUrl.contains(it) }) return true
        
        return false
    }
    
    /**
     * Check if URL is a redirect/shortener
     */
    fun isRedirect(url: String): Boolean {
        val lowerUrl = url.lowercase()
        return redirectPatterns.any { lowerUrl.contains(it) }
    }
    
    /**
     * Check if URL is suspicious (popup triggers, etc.)
     */
    fun isSuspicious(url: String): Boolean {
        val lowerUrl = url.lowercase()
        
        // Empty or javascript URLs
        if (lowerUrl.isEmpty() || lowerUrl.startsWith("javascript:")) return true
        
        // Data URLs (potential tracking pixels)
        if (lowerUrl.startsWith("data:") && !lowerUrl.contains("video")) return true
        
        // About:blank frequently used for popups
        if (lowerUrl == "about:blank") return true
        
        return false
    }
    
    /**
     * Check if navigation should be blocked (combines all checks)
     */
    fun shouldBlockNavigation(url: String, currentUrl: String?): Boolean {
        // Always allow same-domain navigation
        if (currentUrl != null && isSameDomain(url, currentUrl)) {
            return false
        }
        
        return isAd(url) || isRedirect(url) || isSuspicious(url)
    }
    
    /**
     * Check if two URLs are from the same domain
     */
    private fun isSameDomain(url1: String, url2: String): Boolean {
        return try {
            val domain1 = extractDomain(url1)
            val domain2 = extractDomain(url2)
            domain1.isNotEmpty() && domain1 == domain2
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Extract root domain from URL
     */
    private fun extractDomain(url: String): String {
        return try {
            val host = java.net.URL(url).host
            val parts = host.split(".")
            if (parts.size >= 2) {
                "${parts[parts.size - 2]}.${parts[parts.size - 1]}"
            } else {
                host
            }
        } catch (e: Exception) {
            ""
        }
    }

    fun getBlockedDomainsCount(): Int = adHosts.size
    
    /**
     * JavaScript to inject into pages to block inline ads and popups
     */
    fun getAdBlockScript(): String = """
        (function() {
            // Block popup functions
            window.open = function() { return null; };
            window.alert = function() {};
            window.confirm = function() { return false; };
            window.prompt = function() { return null; };
            
            // Remove common ad elements
            var adSelectors = [
                '[id*="ad"]', '[class*="ad-"]', '[class*="ads-"]',
                '[id*="banner"]', '[class*="banner"]',
                '[id*="popup"]', '[class*="popup"]',
                '[class*="overlay"]', '[id*="overlay"]',
                '[class*="modal"]', '[id*="modal"]',
                'iframe[src*="ads"]', 'iframe[src*="doubleclick"]',
                'iframe[src*="googlesyndication"]',
                '[class*="sponsor"]', '[id*="sponsor"]',
                'ins.adsbygoogle', '.adsbygoogle',
                '[data-ad]', '[data-ads]', '[data-advertiser]'
            ];
            
            function removeAds() {
                adSelectors.forEach(function(selector) {
                    try {
                        document.querySelectorAll(selector).forEach(function(el) {
                            if (el.offsetWidth > 0 || el.offsetHeight > 0) {
                                el.style.display = 'none';
                                el.style.visibility = 'hidden';
                            }
                        });
                    } catch(e) {}
                });
            }
            
            // Block onclick handlers that might open popups
            document.addEventListener('click', function(e) {
                var el = e.target;
                while(el) {
                    if (el.onclick || el.hasAttribute('onclick') || 
                        el.hasAttribute('data-popup') || el.hasAttribute('data-link')) {
                        var onclick = el.getAttribute('onclick') || '';
                        if (onclick.indexOf('window.open') !== -1 || 
                            onclick.indexOf('popup') !== -1) {
                            e.stopPropagation();
                            return false;
                        }
                    }
                    el = el.parentElement;
                }
            }, true);
            
            // Run immediately and on DOM changes
            removeAds();
            var observer = new MutationObserver(removeAds);
            observer.observe(document.body || document.documentElement, 
                { childList: true, subtree: true });
            
            // Also run periodically for dynamic content
            setInterval(removeAds, 2000);
        })();
    """.trimIndent()
}