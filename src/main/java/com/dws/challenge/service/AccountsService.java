package com.dws.challenge.service;

import com.dws.challenge.domain.Account;
import com.dws.challenge.exception.DuplicateAccountIdException;
import com.dws.challenge.exception.NotEnoughFundsException;
import com.dws.challenge.exception.AccountNotFoundException;
import com.dws.challenge.repository.AccountsRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.Getter;

import java.math.BigDecimal;

@Service
public class AccountsService {

    @Getter
    private final AccountsRepository accountsRepository;

    private final NotificationService notificationService;

    @Autowired
    public AccountsService(AccountsRepository accountsRepository, NotificationService notificationService) {
        this.accountsRepository = accountsRepository;
        this.notificationService = notificationService;
    }

    public void createAccount(Account account) {
        this.accountsRepository.createAccount(account);
    }

    public Account getAccount(String accountId) {
        return this.accountsRepository.getAccount(accountId);
    }

    /*
     * To avoid a deadlock we need always to acquire the account locks in the same order.
     */
    public void transfer(String sourceAccountId, String targetAccountId, BigDecimal amount) {
        verifyAccountIds(sourceAccountId, targetAccountId);

        Account sourceAccount = getAccountById(sourceAccountId);
        Account targetAccount = getAccountById(targetAccountId);

        Account firstAccountLock;
        Account secondAccountLock;

        if (sourceAccountId.compareTo(targetAccountId) < 0) {
            firstAccountLock = sourceAccount;
            secondAccountLock = targetAccount;
        } else {
            firstAccountLock = targetAccount;
            secondAccountLock = sourceAccount;
        }
        
        synchronized(firstAccountLock) {
            synchronized (secondAccountLock) {
                if (sourceAccount.withdraw(amount)) {
                    targetAccount.deposit(amount);
                    sendTransferNotification(sourceAccount, targetAccount, amount);
                } else {
                    throw new NotEnoughFundsException(sourceAccountId);
                }
            }
        }
    }

    private void sendTransferNotification(Account sourceAccount, Account targetAccount, BigDecimal amount) {
        notificationService.notifyAboutTransfer(sourceAccount,
                "Sent " + amount + " to the account id = " + targetAccount.getAccountId());
        notificationService.notifyAboutTransfer(targetAccount,
                "Received " + amount + " from the account id = " + sourceAccount.getAccountId());
    }

    private Account getAccountById(String accountId) {
        Account account = getAccount(accountId);
        if (account == null) {
            throw new AccountNotFoundException(accountId);
        }
        return account;
    }

    private void verifyAccountIds(String sourceAccountId, String targetAccountId) {
        if (sourceAccountId.equals(targetAccountId)) {
            throw new DuplicateAccountIdException(
                    String.format("Accounts for money transfer must be different: " +
                            "sourceAccountId = %s, targetAccountId = %s", sourceAccountId, targetAccountId)
            );
        }
    }
}
