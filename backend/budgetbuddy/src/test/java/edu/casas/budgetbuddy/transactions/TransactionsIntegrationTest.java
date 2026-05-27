package edu.casas.budgetbuddy.transactions;

import edu.casas.budgetbuddy.TestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TransactionsIntegrationTest extends TestSupport {
    @Test
    void tc12AddIncomeTransaction() throws Exception {
        Session session = register("income@mail.com");
        mockMvc.perform(post("/api/v1/transactions").header("Authorization", bearer(session))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"INCOME","amount":12500,"category":"Salary"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void tc13AddExpenseTransaction() throws Exception {
        Session session = register("expense@mail.com");
        mockMvc.perform(post("/api/v1/transactions").header("Authorization", bearer(session))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"EXPENSE","amount":500,"category":"Food"}
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void tc14AddTransactionWithZeroAmount() throws Exception {
        Session session = register("zero@mail.com");
        mockMvc.perform(post("/api/v1/transactions").header("Authorization", bearer(session))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"EXPENSE","amount":0,"category":"Food"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void tc15AddTransactionWithNoCategory() throws Exception {
        Session session = register("nocat@mail.com");
        mockMvc.perform(post("/api/v1/transactions").header("Authorization", bearer(session))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"EXPENSE","amount":10}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void tc16FetchTransactionList() throws Exception {
        Session session = register("list@mail.com");
        createTransaction(session, "INCOME", 100);
        mockMvc.perform(get("/api/v1/transactions").header("Authorization", bearer(session)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].category").value("SALARY"));
    }

    @Test
    void tc17FetchSummary() throws Exception {
        Session session = register("summary@mail.com");
        createTransaction(session, "INCOME", 300);
        createTransaction(session, "EXPENSE", 75);
        mockMvc.perform(get("/api/v1/transactions/summary").header("Authorization", bearer(session)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.balance").value(225));
    }

    @Test
    void tc18SoftDeleteTransaction() throws Exception {
        Session session = register("delete@mail.com");
        createTransaction(session, "EXPENSE", 75);
        mockMvc.perform(delete("/api/v1/transactions/1").header("Authorization", bearer(session)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/transactions").header("Authorization", bearer(session)))
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void tc19DeleteAnotherUsersTransaction() throws Exception {
        Session owner = register("owner@mail.com");
        Session other = register("other@mail.com");
        createTransaction(owner, "EXPENSE", 75);
        mockMvc.perform(delete("/api/v1/transactions/1").header("Authorization", bearer(other)))
                .andExpect(status().isForbidden());
    }

    private void createTransaction(Session session, String type, int amount) throws Exception {
        mockMvc.perform(post("/api/v1/transactions").header("Authorization", bearer(session))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"type":"%s","amount":%d,"category":"Salary"}
                        """.formatted(type, amount)));
    }
}
