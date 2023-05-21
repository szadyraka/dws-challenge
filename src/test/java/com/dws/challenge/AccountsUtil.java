package com.dws.challenge;

import com.dws.challenge.domain.Account;
import com.dws.challenge.service.AccountsService;

import java.math.BigDecimal;

public class AccountsUtil {

    public static String SOURCE_ACCOUNT_ID = "ID-1";
    public static BigDecimal SOURCE_ACCOUNT_BALANCE = new BigDecimal("549.50");

    public static String TARGET_ACCOUNT_ID = "ID-2";
    public static BigDecimal TARGET_ACCOUNT_BALANCE = new BigDecimal("450.50");

    public static void createAccount(String accountId, BigDecimal balance, AccountsService accountsService) {
        Account sourceAccount = new Account(accountId);
        sourceAccount.setBalance(balance);
        accountsService.createAccount(sourceAccount);
    }
}
