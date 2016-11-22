package com.example;

import org.apache.tomcat.jdbc.pool.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.provider.token.store.JdbcTokenStore;
import org.springframework.stereotype.Component;

/**
 * Created by sunxiaopeng on 2016/11/22.
 */

@Component
public class AccountTokenStore extends JdbcTokenStore { // 1

    @Autowired
    private DataSource dataSource; // 2

    public AccountTokenStore(DataSource dataSource) {
        super(dataSource);
    }
}
