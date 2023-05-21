package com.dws.challenge.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Data
public class Account {

    @NotNull
    @NotEmpty
    private final String accountId;

    @NotNull
    @Min(value = 0, message = "Initial balance must be positive.")
    private volatile BigDecimal balance;

    public Account(String accountId) {
        this.accountId = accountId;
        this.balance = BigDecimal.ZERO;
    }

    @JsonCreator
    public Account(@JsonProperty("accountId") String accountId,
                   @JsonProperty("balance") BigDecimal balance) {
        this.accountId = accountId;
        this.balance = balance;
    }

    public synchronized boolean withdraw(BigDecimal amount) {
        if (this.balance.compareTo(amount) < 0) {
            return false;
        }
        this.balance = this.balance.subtract(amount);
        return true;
    }

    public synchronized boolean deposit(BigDecimal amount) {
        this.balance = this.balance.add(amount);
        return true;
    }

    public synchronized void setBalance(BigDecimal balance) {
        this.balance = balance;
    }
}
