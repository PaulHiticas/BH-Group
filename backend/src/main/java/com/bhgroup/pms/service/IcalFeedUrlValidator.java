package com.bhgroup.pms.service;

import com.bhgroup.pms.common.exception.BadRequestException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;

/**
 * SSRF guard for admin-supplied iCal feed URLs. Only http(s) URLs whose host
 * resolves exclusively to public, routable addresses are allowed - anything
 * that resolves to loopback, link-local (including the 169.254.169.254 cloud
 * metadata endpoint), private-network, or any-local addresses is rejected.
 * Used both when a feed is registered (fast feedback, no network call yet)
 * and again right before every fetch - including after each redirect hop,
 * since a public URL can redirect to an internal one.
 */
final class IcalFeedUrlValidator {

    private static final String REJECTION_MESSAGE = "URL invalid sau adresă interzisă";

    private IcalFeedUrlValidator() {
    }

    static void validate(String rawUrl) {
        URI uri;
        try {
            uri = new URI(rawUrl);
        } catch (URISyntaxException ex) {
            throw new BadRequestException(REJECTION_MESSAGE);
        }

        String scheme = uri.getScheme();
        if (scheme == null || !("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))) {
            throw new BadRequestException(REJECTION_MESSAGE);
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new BadRequestException(REJECTION_MESSAGE);
        }

        InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(host);
        } catch (UnknownHostException ex) {
            throw new BadRequestException(REJECTION_MESSAGE);
        }
        if (addresses.length == 0) {
            throw new BadRequestException(REJECTION_MESSAGE);
        }

        for (InetAddress address : addresses) {
            if (isForbidden(address)) {
                throw new BadRequestException(REJECTION_MESSAGE);
            }
        }
    }

    private static boolean isForbidden(InetAddress address) {
        if (address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isAnyLocalAddress()) {
            return true;
        }
        byte[] bytes = address.getAddress();
        if (bytes.length == 4) {
            return isForbiddenIpv4(bytes);
        }
        if (bytes.length == 16) {
            return isForbiddenIpv6(bytes);
        }
        return true;
    }

    /** 10/8, 172.16/12, 192.168/16, 169.254/16 (incl. 169.254.169.254 cloud metadata), 127/8, 0/8. */
    private static boolean isForbiddenIpv4(byte[] b) {
        int a0 = b[0] & 0xFF;
        int a1 = b[1] & 0xFF;
        if (a0 == 10) return true;
        if (a0 == 172 && a1 >= 16 && a1 <= 31) return true;
        if (a0 == 192 && a1 == 168) return true;
        if (a0 == 169 && a1 == 254) return true;
        if (a0 == 127) return true;
        if (a0 == 0) return true;
        return false;
    }

    /** fc00::/7 (unique local), fe80::/10 (link-local), and IPv4-mapped addresses unwrapped and re-checked. */
    private static boolean isForbiddenIpv6(byte[] b) {
        if ((b[0] & 0xFE) == 0xFC) return true;
        if (b[0] == (byte) 0xFE && (b[1] & 0xC0) == 0x80) return true;
        if (isIpv4Mapped(b)) {
            return isForbiddenIpv4(new byte[]{b[12], b[13], b[14], b[15]});
        }
        return false;
    }

    private static boolean isIpv4Mapped(byte[] b) {
        for (int i = 0; i < 10; i++) {
            if (b[i] != 0) return false;
        }
        return (b[10] & 0xFF) == 0xFF && (b[11] & 0xFF) == 0xFF;
    }
}
