package io.quarkus.oidc.client.reactive.filter;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;

import io.quarkus.oidc.client.reactive.filter.runtime.AbstractOidcClientRequestReactiveFilter;

@Priority(Priorities.AUTHENTICATION)
public class OidcClientRequestReactiveFilter extends AbstractOidcClientRequestReactiveFilter {

}
