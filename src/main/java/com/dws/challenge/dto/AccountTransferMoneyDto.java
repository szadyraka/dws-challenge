package com.dws.challenge.dto;

import javax.validation.constraints.Digits;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;

import lombok.Data;

@Data
public class AccountTransferMoneyDto {

    @NotEmpty
    private String targetAccountId;

    @NotNull
    @Positive
    @Digits(integer = 9, fraction = 2)
    private BigDecimal amount;

}
