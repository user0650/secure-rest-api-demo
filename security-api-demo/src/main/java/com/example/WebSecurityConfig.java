package com.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;

/**
 * Created by sunxiaopeng on 2016/11/21.
 */

@Configuration
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter { // 1

    @Override
    protected void configure(HttpSecurity http) throws Exception { // 2
        http
                .httpBasic().and().csrf().disable() // 3
                .authorizeRequests()
                .antMatchers("/api/accounts/add").anonymous() // 4
                .antMatchers("/api/accounts/me").hasAuthority("USER") // 5
                .antMatchers("/api/accounts/", "/api/accounts/{username}", "/api/accounts/update").hasAuthority("ADMIN") // 6
                ;
    }

//    @Autowired
//    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
//        auth
//                .inMemoryAuthentication()
//                .withUser("user").password("user").roles("USER") // 7
//                .and()
//                .withUser("admin").password("admin").roles("ADMIN", "USER"); // 8
//    }

    @Autowired
    private AccountUserDetailsService accountUserDetailsService; // 9
    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(accountUserDetailsService);
    }

}
