package com.dws.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import static com.dws.challenge.AccountsUtil.SOURCE_ACCOUNT_BALANCE;
import static com.dws.challenge.AccountsUtil.TARGET_ACCOUNT_BALANCE;
import static com.dws.challenge.AccountsUtil.SOURCE_ACCOUNT_ID;
import static com.dws.challenge.AccountsUtil.TARGET_ACCOUNT_ID;

import java.math.BigDecimal;

import com.dws.challenge.domain.Account;
import com.dws.challenge.service.AccountsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@WebAppConfiguration
class AccountsControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private AccountsService accountsService;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private static final String ACCOUNT_TRANSFER_MONEY_URI = "/v1/accounts/" + SOURCE_ACCOUNT_ID + "/transfer";

    @BeforeEach
    void prepareMockMvc() {
        this.mockMvc = webAppContextSetup(this.webApplicationContext).build();

        // Reset the existing accounts before each test.
        accountsService.getAccountsRepository().clearAccounts();
    }

    @Test
    void createAccount() throws Exception {
        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isCreated());

        Account account = accountsService.getAccount("Id-123");
        assertThat(account.getAccountId()).isEqualTo("Id-123");
        assertThat(account.getBalance()).isEqualByComparingTo("1000");
    }

    @Test
    void createDuplicateAccount() throws Exception {
        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isCreated());

        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isBadRequest());
    }

    @Test
    void createAccountNoAccountId() throws Exception {
        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"balance\":1000}")).andExpect(status().isBadRequest());
    }

    @Test
    void createAccountNoBalance() throws Exception {
        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":\"Id-123\"}")).andExpect(status().isBadRequest());
    }

    @Test
    void createAccountNoBody() throws Exception {
        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createAccountNegativeBalance() throws Exception {
        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":\"Id-123\",\"balance\":-1000}")).andExpect(status().isBadRequest());
    }

    @Test
    void createAccountEmptyAccountId() throws Exception {
        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":\"\",\"balance\":1000}")).andExpect(status().isBadRequest());
    }

    @Test
    void getAccount() throws Exception {
        String uniqueAccountId = "Id-" + System.currentTimeMillis();
        Account account = new Account(uniqueAccountId, new BigDecimal("123.45"));
        this.accountsService.createAccount(account);
        this.mockMvc.perform(get("/v1/accounts/" + uniqueAccountId))
                .andExpect(status().isOk())
                .andExpect(
                        content().string("{\"accountId\":\"" + uniqueAccountId + "\",\"balance\":123.45}"));
    }

    @Test
    public void transfer() throws Exception {
        AccountsUtil.createAccount(SOURCE_ACCOUNT_ID, SOURCE_ACCOUNT_BALANCE, accountsService);
        AccountsUtil.createAccount(TARGET_ACCOUNT_ID, TARGET_ACCOUNT_BALANCE, accountsService);

        this.mockMvc.perform(post(ACCOUNT_TRANSFER_MONEY_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetAccountId\":\"" + TARGET_ACCOUNT_ID + "\"," + "\"amount\":150.50}"))
                .andExpect(status().isOk());

        Account sourceAccount = accountsService.getAccount(SOURCE_ACCOUNT_ID);
        assertThat(sourceAccount.getBalance()).isEqualTo(new BigDecimal("399.00"));

        Account targetAccount = accountsService.getAccount(TARGET_ACCOUNT_ID);
        assertThat(targetAccount.getBalance()).isEqualTo(new BigDecimal("601.00"));
    }

    @Test
    public void transferNegativeAmount() throws Exception {
        this.mockMvc.perform(post(ACCOUNT_TRANSFER_MONEY_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetAccountId\":\"" + TARGET_ACCOUNT_ID + "\"," + "\"amount\" : -10.50}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void transferEmptyAccountId() throws Exception {
        this.mockMvc.perform(post(ACCOUNT_TRANSFER_MONEY_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetAccountId\":\"\", \"amount\":10.50}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void transferNullAccountId() throws Exception {
        this.mockMvc.perform(post(ACCOUNT_TRANSFER_MONEY_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetAccountId\" : null, \"amount\":10.50}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void transferWrongRequestBody() throws Exception {
        this.mockMvc.perform(post(ACCOUNT_TRANSFER_MONEY_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetAccountId\":\"" + TARGET_ACCOUNT_ID + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void transferEmptyRequestBody() throws Exception {
        this.mockMvc.perform(post(ACCOUNT_TRANSFER_MONEY_URI)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void transferWrongAmountDigits() throws Exception {
        this.mockMvc.perform(post(ACCOUNT_TRANSFER_MONEY_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetAccountId\":\"" + TARGET_ACCOUNT_ID + "\"," + "\"amount\":10.555}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void transferToTheSameAccount() throws Exception {
        AccountsUtil.createAccount(SOURCE_ACCOUNT_ID, SOURCE_ACCOUNT_BALANCE, accountsService);

        this.mockMvc.perform(post(ACCOUNT_TRANSFER_MONEY_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetAccountId\":\"" + SOURCE_ACCOUNT_ID + "\"," + "\"amount\":120.34}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Accounts for money transfer must be different: " +
                        "sourceAccountId = " + SOURCE_ACCOUNT_ID + ", " +
                        "targetAccountId = " + SOURCE_ACCOUNT_ID));
    }

    @Test
    public void transferToMissingAccount() throws Exception {
        AccountsUtil.createAccount(SOURCE_ACCOUNT_ID, SOURCE_ACCOUNT_BALANCE, accountsService);

        this.mockMvc.perform(post(ACCOUNT_TRANSFER_MONEY_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetAccountId\":\"" + TARGET_ACCOUNT_ID + "\"," + "\"amount\":10.55}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Account id = " + TARGET_ACCOUNT_ID + " not found!"));
    }

    @Test
    public void transferWithNotEnoughAccountBalance() throws Exception {
        AccountsUtil.createAccount(SOURCE_ACCOUNT_ID, SOURCE_ACCOUNT_BALANCE, accountsService);
        AccountsUtil.createAccount(TARGET_ACCOUNT_ID, TARGET_ACCOUNT_BALANCE, accountsService);

        this.mockMvc.perform(post(ACCOUNT_TRANSFER_MONEY_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetAccountId\":\"" + TARGET_ACCOUNT_ID + "\"," + "\"amount\":750.34}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Not enough funds on the account id = " + SOURCE_ACCOUNT_ID));
    }

}
