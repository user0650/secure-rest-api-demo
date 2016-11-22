package com.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfiguration;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.ClientRegistrationException;
import org.springframework.security.oauth2.provider.client.BaseClientDetails;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sunxiaopeng on 2016/11/22.
 */

@Configuration
@EnableAuthorizationServer
public class OAuth2Config extends AuthorizationServerConfigurerAdapter { // 1

    @Autowired
    private AccountUserDetailsService accountUserDetailsService;

    @Autowired
    private AuthenticationManager authenticationManager; // 2

    @Autowired
    private AccountTokenStore accountTokenStore;

    @Override
    public void configure(AuthorizationServerEndpointsConfigurer endpoints) throws Exception {
        endpoints
                .authenticationManager(this.authenticationManager) // 3
                .userDetailsService(accountUserDetailsService)
                .tokenStore(accountTokenStore)
        ;
    }

    @Override
    public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
        clients.withClientDetails(new ClientDetailsService() {
            @Override
            public ClientDetails loadClientByClientId(String clientId) throws ClientRegistrationException {
                BaseClientDetails client = new BaseClientDetails();

                client.setClientId("ios-client");
                client.setClientSecret("ios");

                List<String> resourceIds = new ArrayList<>();
                resourceIds.add("api-accounts");
                client.setResourceIds(resourceIds);

                List<String> scopes = new ArrayList<>();
                scopes.add("read");
                scopes.add("write");
                client.setScope(scopes);

                List<String> authorizedGrantTypes = new ArrayList<>();
                authorizedGrantTypes.add("password");
                authorizedGrantTypes.add("refresh_token");
                client.setAuthorizedGrantTypes(authorizedGrantTypes);

                client.setAuthorities(AuthorityUtils.createAuthorityList("IOS_USER"));
                client.setAccessTokenValiditySeconds(60 * 60 * 24);

                return client;
            }
        });

//        clients
//                .inMemory()
//
//                .withClient("ios-client")
//                .authorizedGrantTypes("password", "refresh_token")
//                .authorities("IOS_USER")
//                .scopes("read", "write")
//                .resourceIds("api-accounts")
//                .accessTokenValiditySeconds(60 * 60 * 24)
//                .secret("ios"); // 4
    }

}
