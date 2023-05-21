package com.dws.challenge.exception;

public class NotEnoughFundsException extends AccountException {

    public NotEnoughFundsException(String accountId) {
        super("Not enough funds on the account id = " + accountId);
    }
}
