package com.qcdfz.fengaiagent.tools;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Guards shared by agent tools. Tool arguments come from the model, which may be steered by
 * fetched web content, so every path and URL is treated as untrusted input.
 */
public final class ToolSafety {

    private ToolSafety() {
    }

    /**
     * Resolves {@code fileName} inside {@code baseDir} and rejects anything that would escape it,
     * such as {@code ../x}, absolute paths or an empty name.
     */
    public static Path resolveInside(String baseDir, String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("File name must not be empty");
        }
        Path base = Path.of(baseDir).toAbsolutePath().normalize();
        Path candidate = Path.of(fileName);
        if (candidate.isAbsolute()) {
            throw new IllegalArgumentException("Absolute paths are not allowed: " + fileName);
        }
        Path resolved = base.resolve(candidate).normalize();
        if (!resolved.startsWith(base) || resolved.equals(base)) {
            throw new IllegalArgumentException("Path escapes the tool directory: " + fileName);
        }
        return resolved;
    }

    /**
     * Accepts only http/https URLs whose host resolves exclusively to public addresses.
     * Callers must also disable automatic redirects, otherwise a public URL could redirect to an
     * internal one. DNS rebinding between this check and the request is not covered.
     */
    public static URI requirePublicHttpUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("URL must not be empty");
        }
        URI uri;
        try {
            uri = URI.create(url.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid URL: " + url);
        }
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!scheme.equals("http") && !scheme.equals("https")) {
            throw new IllegalArgumentException("Only http and https URLs are allowed: " + url);
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("URL has no host: " + url);
        }
        InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(host);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Cannot resolve host: " + host);
        }
        for (InetAddress address : addresses) {
            if (isNonPublic(address)) {
                throw new IllegalArgumentException("Internal or reserved address is not allowed: " + host);
            }
        }
        return uri;
    }

    static boolean isNonPublic(InetAddress address) {
        if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                || address.isSiteLocalAddress() || address.isMulticastAddress()) {
            return true;
        }
        byte[] b = address.getAddress();
        if (address instanceof Inet4Address) {
            int first = b[0] & 0xFF;
            int second = b[1] & 0xFF;
            return first == 0                                   // 0.0.0.0/8
                    || (first == 100 && second >= 64 && second <= 127) // 100.64.0.0/10 carrier NAT
                    || (first == 198 && (second == 18 || second == 19)) // 198.18.0.0/15 benchmarking
                    || first >= 240;                            // 240.0.0.0/4 reserved
        }
        if (address instanceof Inet6Address) {
            return (b[0] & 0xFE) == 0xFC;                       // fc00::/7 unique local
        }
        return false;
    }
}
