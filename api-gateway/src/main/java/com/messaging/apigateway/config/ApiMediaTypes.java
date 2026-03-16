package com.messaging.apigateway.config;

/**
 * Versioned media type constraints for header-based API versioning.
 */
public final class ApiMediaTypes {
    public static final String V1_JSON = "application/vnd.messaging.v1+json";

    public static final String[] SUPPORTED_VERSIONS = { V1_JSON };
    public static final String ACCEPT_HEADER = "Accept";

    private ApiMediaTypes() {}
}
