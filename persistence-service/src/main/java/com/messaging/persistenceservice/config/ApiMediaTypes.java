package com.messaging.persistenceservice.config;

/**
 * Versioned media type constraints for header-based API versioning.
 */
public final class ApiMediaTypes {
    public static final String V1_JSON = "application/vnd.messaging.v1+json";

    public static final String[] SUPPORTED_VERSIONS = { V1_JSON };

    private ApiMediaTypes() {}
}
