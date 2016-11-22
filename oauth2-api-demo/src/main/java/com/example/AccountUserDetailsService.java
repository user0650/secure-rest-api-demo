package com.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Created by sunxiaopeng on 2016/11/21.
 */

@Service
public class AccountUserDetailsService implements UserDetailsService {

    @Autowired
    private AccountRepository accountRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        if(username.equals("admin")){
            return new User(username, username, AuthorityUtils.createAuthorityList("ADMIN", "USER"));
        }

        Account account = accountRepository.findByUsername(username);
        if(account == null)
            throw new UsernameNotFoundException("用户[" + username + "]不存在！");

        return new User(username, account.getPassword() != null ? account.getPassword() : username, AuthorityUtils.createAuthorityList("USER"));
    }

}
