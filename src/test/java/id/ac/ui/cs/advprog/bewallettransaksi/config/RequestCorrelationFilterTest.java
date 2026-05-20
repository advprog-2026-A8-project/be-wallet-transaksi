package id.ac.ui.cs.advprog.bewallettransaksi.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestCorrelationFilterTest {

    private final RequestCorrelationFilter filter = new RequestCorrelationFilter();

    @Test
    void doFilterInternal_WithExistingRequestId_ShouldReuseHeaderAndClearMdcAfterChain()
            throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/wallet/test");
        request.addHeader("X-Request-Id", "req-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> requestIdInsideChain = new AtomicReference<>();
        AtomicReference<String> httpMethodInsideChain = new AtomicReference<>();
        AtomicReference<String> httpPathInsideChain = new AtomicReference<>();

        FilterChain filterChain = (servletRequest, servletResponse) -> {
            requestIdInsideChain.set(MDC.get("requestId"));
            httpMethodInsideChain.set(MDC.get("httpMethod"));
            httpPathInsideChain.set(MDC.get("httpPath"));
        };

        filter.doFilter(request, response, filterChain);

        assertEquals("req-123", requestIdInsideChain.get());
        assertEquals("GET", httpMethodInsideChain.get());
        assertEquals("/wallet/test", httpPathInsideChain.get());
        assertEquals("req-123", response.getHeader("X-Request-Id"));
        assertNull(MDC.get("requestId"));
        assertNull(MDC.get("httpMethod"));
        assertNull(MDC.get("httpPath"));
    }

    @Test
    void doFilterInternal_WithoutRequestId_ShouldGenerateHeader()
            throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/wallet/pay");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> requestIdInsideChain = new AtomicReference<>();

        FilterChain filterChain = (servletRequest, servletResponse) ->
                requestIdInsideChain.set(MDC.get("requestId"));

        filter.doFilter(request, response, filterChain);

        String generatedRequestId = response.getHeader("X-Request-Id");
        assertNotNull(generatedRequestId);
        assertTrue(!generatedRequestId.isBlank());
        assertEquals(generatedRequestId, requestIdInsideChain.get());
        assertNull(MDC.get("requestId"));
    }
}
