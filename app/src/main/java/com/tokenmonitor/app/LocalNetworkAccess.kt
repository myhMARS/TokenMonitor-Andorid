package com.tokenmonitor.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import java.net.URI

/**
 * Android 17 (API 37) blocks traffic to private LAN addresses for apps targeting
 * SDK 37 until ACCESS_LOCAL_NETWORK is granted. This app's primary deployment
 * connects to a hub on the local network, so the permission is requested only
 * when the configured host actually looks like a LAN address; a public hub keeps
 * the old zero-prompt flow.
 */
internal fun hasLocalNetworkPermission(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < 37) return true
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_LOCAL_NETWORK
    ) == PackageManager.PERMISSION_GRANTED
}

internal fun requiresLocalNetworkPermission(hubUrl: String): Boolean {
    if (Build.VERSION.SDK_INT < 37) return false
    val host = hubHost(hubUrl) ?: return false
    return isLanHost(host)
}

private fun hubHost(url: String): String? {
    val trimmed = url.trim()
    if (trimmed.isEmpty()) return null
    val withScheme = if ("://" in trimmed) trimmed else "http://$trimmed"
    return try {
        URI(withScheme).host?.trim()?.takeIf { it.isNotEmpty() }
    } catch (_: Exception) {
        null
    }
}

internal fun isLanHost(rawHost: String): Boolean {
    val host = rawHost.trim().removePrefix("[").removeSuffix("]").trimEnd('.').lowercase()
    if (host.isEmpty()) return false
    if (host == "localhost" || host == "::1" || host == "0:0:0:0:0:0:0:1") return false
    if (host.startsWith("::ffff:")) return isLanHost(host.removePrefix("::ffff:"))

    if (host.endsWith(".local") || host.endsWith(".lan") ||
        host.endsWith(".internal") || host.endsWith(".home.arpa")
    ) {
        return true
    }
    // A single-label hostname is resolved through mDNS/DHCP, i.e. on the LAN.
    if (!host.contains('.') && !host.contains(':')) return true

    if (host.contains(':')) {
        // Unique-local fc00::/7 and link-local fe80::/10.
        return host.startsWith("fc") || host.startsWith("fd") ||
            host.startsWith("fe8") || host.startsWith("fe9") ||
            host.startsWith("fea") || host.startsWith("feb")
    }

    val parts = host.split('.')
    if (parts.size != 4) return false
    val octets = parts.map { it.toIntOrNull()?.takeIf { value -> value in 0..255 } ?: return false }
    val first = octets[0]
    val second = octets[1]
    return first == 10 ||
        (first == 172 && second in 16..31) ||
        (first == 192 && second == 168) ||
        (first == 169 && second == 254) ||
        (first == 100 && second in 64..127)
}
