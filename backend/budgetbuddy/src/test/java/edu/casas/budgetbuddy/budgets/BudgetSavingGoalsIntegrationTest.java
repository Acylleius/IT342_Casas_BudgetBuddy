package edu.casas.budgetbuddy.budgets;

import com.fasterxml.jackson.databind.JsonNode;
import edu.casas.budgetbuddy.TestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BudgetSavingGoalsIntegrationTest extends TestSupport {
    @Test
    void createPersonalWeeklyBudget() throws Exception {
        Session user = register("budget-weekly@mail.com");
        mockMvc.perform(post("/api/v1/budgets")
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(budgetJson("Weekly Food", 1000, "WEEKLY", "Food")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.scope").value("PERSONAL"))
                .andExpect(jsonPath("$.data.period").value("WEEKLY"));
    }

    @Test
    void createPersonalMonthlyBudget() throws Exception {
        Session user = register("budget-monthly@mail.com");
        mockMvc.perform(post("/api/v1/budgets")
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(budgetJson("Monthly General", 5000, "MONTHLY", null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.period").value("MONTHLY"));
    }

    @Test
    void createGroupWeeklyAndMonthlyBudgets() throws Exception {
        Session owner = register("group-budget-owner@mail.com");
        Long groupId = createGroup(owner);
        mockMvc.perform(post("/api/v1/groups/%d/budgets".formatted(groupId))
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(budgetJson("Weekly Grocery", 1000, "WEEKLY", "Grocery")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.scope").value("GROUP"));
        mockMvc.perform(post("/api/v1/groups/%d/budgets".formatted(groupId))
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(budgetJson("Monthly Rent", 7000, "MONTHLY", "Rent")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.period").value("MONTHLY"));
    }

    @Test
    void nonMemberCannotViewGroupBudget() throws Exception {
        Session owner = register("group-budget-owner2@mail.com");
        Session outsider = register("group-budget-outsider@mail.com");
        Long groupId = createGroup(owner);
        createGroupBudget(owner, groupId, 1000, "MONTHLY", "Food");
        mockMvc.perform(get("/api/v1/groups/%d/budgets".formatted(groupId))
                        .header("Authorization", bearer(outsider)))
                .andExpect(status().isForbidden());
    }

    @Test
    void personalBudgetTracksExpenses() throws Exception {
        Session user = register("personal-track@mail.com");
        createPersonalBudget(user, 1000, "MONTHLY", "Food");
        createPersonalExpense(user, 250, "Food");
        mockMvc.perform(get("/api/v1/budgets/tracking").header("Authorization", bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].budget.spentAmount").value(250))
                .andExpect(jsonPath("$.data[0].relatedTransactions.length()").value(1));
    }

    @Test
    void groupBudgetTracksGroupExpensesOnly() throws Exception {
        Session owner = register("group-track@mail.com");
        Long groupId = createGroup(owner);
        createGroupBudget(owner, groupId, 1000, "MONTHLY", "Rent");
        createGroupTransaction(owner, groupId, "INCOME", 900, "Rent");
        createGroupTransaction(owner, groupId, "EXPENSE", 600, "Rent");
        mockMvc.perform(get("/api/v1/groups/%d/budgets/tracking".formatted(groupId))
                        .header("Authorization", bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].budget.spentAmount").value(600))
                .andExpect(jsonPath("$.data[0].contributors.length()").value(1));
    }

    @Test
    void warningAndExceededNotificationsAreCreatedWithoutSpam() throws Exception {
        Session user = register("budget-alerts@mail.com");
        createPersonalBudget(user, 100, "MONTHLY", "Food");
        createPersonalExpense(user, 80, "Food");
        createPersonalExpense(user, 1, "Food");
        mockMvc.perform(get("/api/v1/inbox").header("Authorization", bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.type=='BUDGET_WARNING')].type").isNotEmpty())
                .andExpect(jsonPath("$.data[?(@.type=='BUDGET_WARNING')]", hasSize(1)));
        createPersonalExpense(user, 20, "Food");
        createPersonalExpense(user, 1, "Food");
        mockMvc.perform(get("/api/v1/inbox").header("Authorization", bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.type=='BUDGET_EXCEEDED')]", hasSize(1)));
    }

    @Test
    void groupBudgetExceededNotifiesAcceptedMembersOnly() throws Exception {
        Session owner = register("group-alert-owner@mail.com");
        Session member = register("group-alert-member@mail.com");
        Session pending = register("group-alert-pending@mail.com");
        Long groupId = createGroup(owner);
        addMember(owner, groupId, member.email());
        invite(owner, groupId, pending.email());
        createGroupBudget(owner, groupId, 100, "MONTHLY", "Food");
        createGroupTransaction(owner, groupId, "EXPENSE", 120, "Food");
        mockMvc.perform(get("/api/v1/inbox").header("Authorization", bearer(member)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.type=='GROUP_BUDGET_EXCEEDED')].type").isNotEmpty());
        mockMvc.perform(get("/api/v1/inbox").header("Authorization", bearer(pending)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.type=='GROUP_BUDGET_EXCEEDED')]").isEmpty());
    }

    @Test
    void personalSavingGoalCrudAndCompletionNotification() throws Exception {
        Session user = register("saving-personal@mail.com");
        Long goalId = createPersonalGoal(user, "Emergency Fund", 1000, 100);
        mockMvc.perform(put("/api/v1/saving-goals/%d".formatted(goalId))
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(goalJson("Emergency Fund", 1000, 1000)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
        mockMvc.perform(get("/api/v1/inbox").header("Authorization", bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.type=='SAVING_GOAL_COMPLETED')].type").isNotEmpty());
        mockMvc.perform(delete("/api/v1/saving-goals/%d".formatted(goalId)).header("Authorization", bearer(user)))
                .andExpect(status().isOk());
    }

    @Test
    void groupSavingGoalContributionCompletionAndNonMemberAccess() throws Exception {
        Session owner = register("saving-group-owner@mail.com");
        Session member = register("saving-group-member@mail.com");
        Session outsider = register("saving-group-outsider@mail.com");
        Long groupId = createGroup(owner);
        addMember(owner, groupId, member.email());
        Long goalId = createGroupGoal(owner, groupId, "Trip Fund", 500, 100);
        mockMvc.perform(get("/api/v1/groups/%d/saving-goals".formatted(groupId))
                        .header("Authorization", bearer(outsider)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/groups/%d/saving-goals/%d/contribute".formatted(groupId, goalId))
                        .header("Authorization", bearer(member))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":400,\"note\":\"share\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
        mockMvc.perform(get("/api/v1/inbox").header("Authorization", bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.type=='GROUP_SAVING_GOAL_COMPLETED')].type").isNotEmpty());
    }

    private Long createPersonalBudget(Session user, int amount, String period, String category) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/budgets")
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(budgetJson("Budget", amount, period, category)))
                .andReturn();
        return root(result).at("/data/id").asLong();
    }

    private Long createGroupBudget(Session user, Long groupId, int amount, String period, String category) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/groups/%d/budgets".formatted(groupId))
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(budgetJson("Group Budget", amount, period, category)))
                .andReturn();
        return root(result).at("/data/id").asLong();
    }

    private Long createPersonalGoal(Session user, String title, int target, int current) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/saving-goals")
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(goalJson(title, target, current)))
                .andReturn();
        return root(result).at("/data/id").asLong();
    }

    private Long createGroupGoal(Session user, Long groupId, String title, int target, int current) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/groups/%d/saving-goals".formatted(groupId))
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(goalJson(title, target, current)))
                .andReturn();
        return root(result).at("/data/id").asLong();
    }

    private Long createGroup(Session owner) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/groups")
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Roommates\",\"description\":\"Budget group\"}"))
                .andReturn();
        return root(result).at("/data/id").asLong();
    }

    private void addMember(Session owner, Long groupId, String email) throws Exception {
        mockMvc.perform(post("/api/v1/groups/%d/members".formatted(groupId))
                .header("Authorization", bearer(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\"}".formatted(email)));
    }

    private void invite(Session owner, Long groupId, String email) throws Exception {
        mockMvc.perform(post("/api/v1/groups/%d/invitations".formatted(groupId))
                .header("Authorization", bearer(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\"}".formatted(email)));
    }

    private void createPersonalExpense(Session user, int amount, String category) throws Exception {
        mockMvc.perform(post("/api/v1/transactions")
                .header("Authorization", bearer(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"EXPENSE\",\"amount\":%d,\"category\":\"%s\"}".formatted(amount, category)));
    }

    private void createGroupTransaction(Session user, Long groupId, String type, int amount, String category) throws Exception {
        mockMvc.perform(post("/api/v1/groups/%d/transactions".formatted(groupId))
                .header("Authorization", bearer(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"%s\",\"amount\":%d,\"category\":\"%s\",\"actorUserId\":%d}"
                        .formatted(type, amount, category, user.userId())));
    }

    private String budgetJson(String name, int amount, String period, String category) {
        String categoryPart = category == null ? "" : ",\"category\":\"%s\"".formatted(category);
        return "{\"name\":\"%s\",\"limitAmount\":%d,\"period\":\"%s\"%s}"
                .formatted(name, amount, period, categoryPart);
    }

    private String goalJson(String title, int target, int current) {
        return "{\"title\":\"%s\",\"targetAmount\":%d,\"currentAmount\":%d}"
                .formatted(title, target, current);
    }

    private JsonNode root(MvcResult result) throws Exception {
        return mapper.readTree(result.getResponse().getContentAsString());
    }
}
