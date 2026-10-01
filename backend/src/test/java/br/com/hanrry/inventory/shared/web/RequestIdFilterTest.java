package br.com.hanrry.inventory.shared.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestIdFilterTest {

    private final RequestIdFilter filter = new RequestIdFilter();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void shouldGenerateUuidHeaderAndIgnoreIncomingRequestId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestIdFilter.REQUEST_ID_HEADER, "client-forged-id");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> mdcDuringChain = new AtomicReference<>();

        filter.doFilter(request, response, (req, res) -> mdcDuringChain.set(MDC.get(RequestIdFilter.REQUEST_ID_MDC_KEY)));

        String header = response.getHeader(RequestIdFilter.REQUEST_ID_HEADER);
        UUID parsed = UUID.fromString(header);
        assertEquals(parsed.toString(), header);
        assertNotEquals("client-forged-id", header);
        assertEquals(header, mdcDuringChain.get());
        assertNull(MDC.get(RequestIdFilter.REQUEST_ID_MDC_KEY));
    }

    @Test
    void shouldKeepSameRequestIdOnResponseAndMdcDuringChain() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(new MockHttpServletRequest(), response, (req, res) -> {
            assertEquals(response.getHeader(RequestIdFilter.REQUEST_ID_HEADER), MDC.get(RequestIdFilter.REQUEST_ID_MDC_KEY));
        });

        assertTrue(response.getHeader(RequestIdFilter.REQUEST_ID_HEADER).contains("-"));
    }

    @Test
    void shouldClearMdcWhenChainThrows() throws Exception {
        try {
            filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), (FilterChain) (req, res) -> {
                throw new IllegalStateException("boom");
            });
        } catch (IllegalStateException ignored) {
            // expected
        }

        assertNull(MDC.get(RequestIdFilter.REQUEST_ID_MDC_KEY));
    }
}
