package com.dws.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static com.dws.challenge.AccountsUtil.createAccount;

import static com.dws.challenge.AccountsUtil.SOURCE_ACCOUNT_BALANCE;
import static com.dws.challenge.AccountsUtil.TARGET_ACCOUNT_BALANCE;
import static com.dws.challenge.AccountsUtil.SOURCE_ACCOUNT_ID;
import static com.dws.challenge.AccountsUtil.TARGET_ACCOUNT_ID;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.dws.challenge.domain.Account;
import com.dws.challenge.exception.AccountNotFoundException;
import com.dws.challenge.exception.DuplicateAccountIdException;
import com.dws.challenge.exception.NotEnoughFundsException;
import com.dws.challenge.service.AccountsService;
import com.dws.challenge.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class AccountsServiceTest {

    @Autowired
    private AccountsService accountsService;

    @MockBean
    private NotificationService notificationService;

    @BeforeEach
    public void setup() {
        this.accountsService.getAccountsRepository().clearAccounts();
    }

    @Test
    void addAccount() {
        Account account = new Account("Id-123");
        account.setBalance(new BigDecimal(1000));
        this.accountsService.createAccount(account);

        assertThat(this.accountsService.getAccount("Id-123")).isEqualTo(account);
    }

    @Test
    void addAccount_failsOnDuplicateId() {
        String uniqueId = "Id-" + System.currentTimeMillis();
        Account account = new Account(uniqueId);
        this.accountsService.createAccount(account);

        try {
            this.accountsService.createAccount(account);
            fail("Should have failed when adding duplicate account");
        } catch (DuplicateAccountIdException ex) {
            assertThat(ex.getMessage()).isEqualTo("Account id " + uniqueId + " already exists!");
        }
    }

    @Test
    public void transfer_positive() {
        createAccount(SOURCE_ACCOUNT_ID, SOURCE_ACCOUNT_BALANCE, accountsService);
        createAccount(TARGET_ACCOUNT_ID, TARGET_ACCOUNT_BALANCE, accountsService);

        BigDecimal transferAmount = new BigDecimal("150.50");

        accountsService.transfer(SOURCE_ACCOUNT_ID, TARGET_ACCOUNT_ID, transferAmount);

        assertThat(accountsService.getAccount(SOURCE_ACCOUNT_ID).getBalance()).isEqualTo(new BigDecimal("399.00"));
        assertThat(accountsService.getAccount(TARGET_ACCOUNT_ID).getBalance()).isEqualTo(new BigDecimal("601.00"));

        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        verify(notificationService, times(2)).notifyAboutTransfer(any(Account.class), argument.capture());

        List<String> expectedNotifications = List.of(
                "Sent " + transferAmount + " to the account id = " + TARGET_ACCOUNT_ID,
                "Received " + transferAmount + " from the account id = " + SOURCE_ACCOUNT_ID);

        assertIterableEquals(expectedNotifications, argument.getAllValues());
    }

    @Test
    public void transfer_failsOnDuplicateId() {
        createAccount(SOURCE_ACCOUNT_ID, SOURCE_ACCOUNT_BALANCE, accountsService);

        DuplicateAccountIdException thrown = assertThrows(DuplicateAccountIdException.class,
                () -> accountsService.transfer(SOURCE_ACCOUNT_ID, SOURCE_ACCOUNT_ID, new BigDecimal("100.00")),
                "Should have failed when transferring from the same account");

        assertEquals(thrown.getMessage(), "Accounts for money transfer must be different: " +
                "sourceAccountId = " + SOURCE_ACCOUNT_ID + ", " +
                "targetAccountId = " + SOURCE_ACCOUNT_ID);
    }

    @Test
    public void transfer_failsOnMissingTargetAccount() {
        createAccount(SOURCE_ACCOUNT_ID, SOURCE_ACCOUNT_BALANCE, accountsService);

        AccountNotFoundException thrown = assertThrows(AccountNotFoundException.class,
                () -> accountsService.transfer(SOURCE_ACCOUNT_ID, TARGET_ACCOUNT_ID, new BigDecimal("100.00")),
                "Should have failed when transferring to the missing account");

        assertEquals(thrown.getMessage(), "Account id = " + TARGET_ACCOUNT_ID + " not found!");
    }

    @Test
    public void transfer_failsOnNotEnoughAccountBalance() {
        createAccount(SOURCE_ACCOUNT_ID, SOURCE_ACCOUNT_BALANCE, accountsService);
        createAccount(TARGET_ACCOUNT_ID, TARGET_ACCOUNT_BALANCE, accountsService);

        NotEnoughFundsException thrown = assertThrows(NotEnoughFundsException.class,
                () -> accountsService.transfer(SOURCE_ACCOUNT_ID, TARGET_ACCOUNT_ID, new BigDecimal("570.75")),
                "Should have failed when there are not enough funds on the source account");

        assertEquals(thrown.getMessage(), "Not enough funds on the account id = " + SOURCE_ACCOUNT_ID);
    }

    @Test
    public void transfer_positiveWithConcurrency() throws InterruptedException {
        createAccount(SOURCE_ACCOUNT_ID, SOURCE_ACCOUNT_BALANCE, accountsService);
        createAccount(TARGET_ACCOUNT_ID, TARGET_ACCOUNT_BALANCE, accountsService);

        int numberOfThreads = 1000;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        // Parallel execution of 1000 transfers for the source and the target accounts in both directions
        for (int i = 0; i < numberOfThreads/2; i++) {
            executorService.execute(() -> {
                latch.countDown();
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                accountsService.transfer(SOURCE_ACCOUNT_ID, TARGET_ACCOUNT_ID, new BigDecimal("0.5"));
            });
        }

        for (int i = 0; i < numberOfThreads/2; i++) {
            executorService.execute(() -> {
                latch.countDown();
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                accountsService.transfer(TARGET_ACCOUNT_ID, SOURCE_ACCOUNT_ID, new BigDecimal("1"));
            });
        }

        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.MINUTES);

        BigDecimal expectedSourceAccBalance = SOURCE_ACCOUNT_BALANCE.subtract(new BigDecimal("250")).add(new BigDecimal("500"));
        BigDecimal expectedTargetAccBalance = TARGET_ACCOUNT_BALANCE.subtract(new BigDecimal("500")).add(new BigDecimal("250"));

        assertEquals(expectedTargetAccBalance, accountsService.getAccount(TARGET_ACCOUNT_ID).getBalance());
        assertEquals(expectedSourceAccBalance, accountsService.getAccount(SOURCE_ACCOUNT_ID).getBalance());
    }
}
