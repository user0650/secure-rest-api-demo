package com.example;

import org.springframework.data.repository.CrudRepository;

/**
 * Created by sun on 2016/11/20.
 */
public interface AccountRepository extends CrudRepository<Account, Long> {

    Account findByUsername(String username);

}
