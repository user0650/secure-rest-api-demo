package com.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resources;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by sun on 2016/11/20.
 */

@RestController
@RequestMapping("/api/accounts")
public class AccountController { // 1

    @Autowired
    private AccountRepository accountRepository; // 2

    @RequestMapping(method = RequestMethod.GET, value = "/me")
    public AccountResource me(Principal principal) { // 1
        String username = principal.getName(); // 2
        Account account = accountRepository.findByUsername(username);
        if(account == null)
            throw new IllegalArgumentException("用户不存在！");
        return new AccountResource(account);
    }


    @RequestMapping(method = RequestMethod.POST, value = "/add")
    public AccountResource add(@RequestBody Account account){ // 3

        if(accountRepository.findByUsername(account.getUsername()) != null){
            throw new IllegalArgumentException("用户已经存在！");
        }// 4

        account.setId(0); // 5
        accountRepository.save(account); // 6

        return new AccountResource(account); // 7
    }

    @RequestMapping(method = RequestMethod.GET, value = "/{username}")
    public AccountResource account(@PathVariable String username) { // 1
        Account account = accountRepository.findByUsername(username);
        if(account == null){
            throw new IllegalArgumentException("用户不存在！");
        }

        return new AccountResource(account);
    }

    @RequestMapping(method = RequestMethod.PUT, value = "/update")
    public AccountResource update(@RequestBody Account account) { // 9

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

        return new AccountResource(me);

    }

    @RequestMapping(method = RequestMethod.DELETE, value = "/{username}")
    public AccountResource delete(@PathVariable String username) { // 11

        Account account = accountRepository.findByUsername(username);
        if(account == null){
            throw new IllegalArgumentException("用户不存在！");
        }

        accountRepository.delete(account); // 12

        return new AccountResource(account);
    }

    @RequestMapping(method = RequestMethod.GET, value = "/")
    public Resources<AccountResource> list() {
        Iterable<Account> accounts = accountRepository.findAll();
        List<AccountResource> accountList = new ArrayList<>();
        accounts.forEach(account -> {
            accountList.add(new AccountResource(account));
        });
        return new Resources<>(accountList);
    }

}
