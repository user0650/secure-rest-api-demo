package com.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * Created by sun on 2016/11/20.
 */

@RestController
@RequestMapping("/api/accounts")
public class AccountController { // 1

    @Autowired
    private AccountRepository accountRepository; // 2

    @RequestMapping(method = RequestMethod.POST, value = "/add")
    public Account add(@RequestBody Account account){ // 3

        if(accountRepository.findByUsername(account.getUsername()) != null){
            throw new IllegalArgumentException("用户已经存在！");
        }// 4


        account.setId(0); // 5
        accountRepository.save(account); // 6

        return account; // 7
    }

    @RequestMapping(method = RequestMethod.GET, value = "/{username}")
    public Account account(@PathVariable String username) { // 8

        Account account = accountRepository.findByUsername(username);
        if(account == null){
            throw new IllegalArgumentException("用户不存在！");
        }

        return account;
    }

    @RequestMapping(method = RequestMethod.PUT, value = "/update")
    public Account update(@RequestBody Account account) { // 9

        Account me = accountRepository.findByUsername(account.getUsername());
        if(me == null){
            throw new IllegalArgumentException("用户不存在！");
        }

        me.setPassword(account.getPassword());
        me.setEmail(account.getEmail());
        me.setFirstName(account.getFirstName());
        me.setLastName(account.getLastName());
        me.setAge(account.getAge());
        me.setGender(account.getGender());

        me = accountRepository.save(me); // 10

        return me;

    }

    @RequestMapping(method = RequestMethod.DELETE, value = "/{username}")
    public Account delete(@PathVariable String username) { // 11

        Account account = accountRepository.findByUsername(username);
        if(account == null){
            throw new IllegalArgumentException("用户不存在！");
        }

        accountRepository.delete(account); // 12

        return account;
    }

}
