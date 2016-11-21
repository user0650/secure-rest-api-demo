package com.example;

import org.springframework.hateoas.ResourceSupport;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;

/**
 * Created by sunxiaopeng on 2016/11/21.
 */
public class AccountResource extends ResourceSupport { // 1

    private final Account account;

    public AccountResource(Account account) {
        this.account = account;
        this.add(ControllerLinkBuilder.linkTo(ControllerLinkBuilder.methodOn(AccountController.class).add(null)).withRel("add"));
        this.add(ControllerLinkBuilder.linkTo(ControllerLinkBuilder.methodOn(AccountController.class).account(account.getUsername())).withSelfRel());
        this.add(ControllerLinkBuilder.linkTo(ControllerLinkBuilder.methodOn(AccountController.class).update(null)).withRel("update"));
        this.add(ControllerLinkBuilder.linkTo(ControllerLinkBuilder.methodOn(AccountController.class).delete(account.getUsername())).withRel("delete"));
        // 2
    }

    public Account getAccount() {
        return account;
    }

}
