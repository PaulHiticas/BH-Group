package com.bhgroup.pms.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bhgroup.pms.common.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * SSRF guard for admin-supplied iCal feed URLs: only public http(s) hosts
 * may be fetched. Literal IPs are used throughout (rather than hostnames
 * needing a real DNS lookup) so these stay fast and network-independent.
 */
class IcalFeedUrlValidatorTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "http://127.0.0.1/feed.ics",
            "https://127.0.0.1/feed.ics",
            "http://localhost/feed.ics",
            "http://[::1]/feed.ics",
            "http://169.254.169.254/latest/meta-data/",
            "http://169.254.1.1/feed.ics",
            "http://10.0.0.5/feed.ics",
            "http://172.16.0.5/feed.ics",
            "http://172.31.255.255/feed.ics",
            "http://192.168.1.10/feed.ics",
            "http://0.0.0.0/feed.ics",
            "http://[fe80::1]/feed.ics",
            "http://[fc00::1]/feed.ics",
            "http://[::ffff:127.0.0.1]/feed.ics",
    })
    void validate_rejectsInternalAndLinkLocalAddresses(String url) {
        assertThatThrownBy(() -> IcalFeedUrlValidator.validate(url))
                .isInstanceOf(BadRequestException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "file:///etc/passwd",
            "ftp://203.0.113.10/feed.ics",
            "gopher://203.0.113.10/feed.ics",
            "not-a-url",
    })
    void validate_rejectsNonHttpSchemes(String url) {
        assertThatThrownBy(() -> IcalFeedUrlValidator.validate(url))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void validate_acceptsAPublicIpAddress() {
        // 203.0.113.0/24 is the RFC 5737 TEST-NET-3 documentation range:
        // not loopback/link-local/private, so it must pass the IP check
        // without making a real network call.
        assertThatCode(() -> IcalFeedUrlValidator.validate("http://203.0.113.10/feed.ics"))
                .doesNotThrowAnyException();
        assertThatCode(() -> IcalFeedUrlValidator.validate("https://203.0.113.10/feed.ics"))
                .doesNotThrowAnyException();
    }
}
