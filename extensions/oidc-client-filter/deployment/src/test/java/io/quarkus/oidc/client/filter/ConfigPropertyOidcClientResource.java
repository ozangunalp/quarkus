package io.quarkus.oidc.client.filter;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RestClient;

@Path("/config-property-oidc-client")
public class ConfigPropertyOidcClientResource {

    @Inject
    @RestClient
    ProtectedResourceServiceConfigPropertyOidcClient protectedResourceServiceConfigPropertyOidcClient;

    @GET
    @Path("user-name")
    public String userName() {
        return protectedResourceServiceConfigPropertyOidcClient.getUserName();
    }
}
