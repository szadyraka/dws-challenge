package com.dws.challenge.exception;

public class AccountNotFoundException extends AccountException {

    public AccountNotFoundException(String accountId) {
        super("Account id = " + accountId + " not found!");
    }
}
