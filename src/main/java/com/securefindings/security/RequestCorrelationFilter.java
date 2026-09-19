package com.securefindings.security;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public final class RequestCorrelationFilter
                extends OncePerRequestFilter {

        public static final String HEADER_NAME = "X-Request-ID";

        public static final String REQUEST_ID_ATTRIBUTE = RequestCorrelationFilter.class.getName() + ".requestId";

        public static final String MDC_KEY = "requestId";

        private static final Pattern VALID_REQUEST_ID = Pattern.compile(
                        "[A-Za-z0-9][A-Za-z0-9._-]{0,63}");

        @Override
        protected void doFilterInternal(
                        HttpServletRequest request,
                        HttpServletResponse response,
                        FilterChain filterChain)
                        throws ServletException, IOException {

                String requestId = resolveRequestId(request);

                request.setAttribute(
                                REQUEST_ID_ATTRIBUTE,
                                requestId);

                response.setHeader(
                                HEADER_NAME,
                                requestId);

                try (MDC.MDCCloseable ignored = MDC.putCloseable(
                                MDC_KEY,
                                requestId)) {

                        filterChain.doFilter(request, response);
                } finally {
                        request.removeAttribute(REQUEST_ID_ATTRIBUTE);
                }
        }

        private String resolveRequestId(
                        HttpServletRequest request) {

                String requestedId = request.getHeader(HEADER_NAME);

                if (requestedId != null
                                && VALID_REQUEST_ID.matcher(requestedId).matches()) {
                        return requestedId;
                }

                return UUID.randomUUID().toString();
        }

        public static String currentRequestId(
                        HttpServletRequest request) {

                Object requestId = request.getAttribute(
                                REQUEST_ID_ATTRIBUTE);

                return requestId instanceof String value
                                ? value
                                : null;
        }

        public static String currentRequestId() {
                return MDC.get(MDC_KEY);
        }
}