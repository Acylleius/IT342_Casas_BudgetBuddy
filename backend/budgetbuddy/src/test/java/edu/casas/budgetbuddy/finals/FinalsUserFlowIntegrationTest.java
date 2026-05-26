package edu.casas.budgetbuddy.finals;

import com.fasterxml.jackson.databind.JsonNode;
import edu.casas.budgetbuddy.TestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FinalsUserFlowIntegrationTest extends TestSupport {
    @Test
    void registeredUsersAreNormalUsers() throws Exception {
        Session user = register("normal-user@mail.com");
        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("USER"));
    }

    @Test
    void unregisteredEmailInviteIsRejected() throws Exception {
        Session owner = register("invite-owner@mail.com");
        Long groupId = createGroup(owner, "Roommates");
        mockMvc.perform(post("/api/v1/groups/%d/invitations".formatted(groupId))
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"missing@mail.com\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("This user is not registered in BudgetBuddy."));
    }

    @Test
    void acceptInvitationAddsMember() throws Exception {
        Fixture fixture = invitationFixture();
        Long invitationId = invite(fixture.owner(), fixture.groupId(), fixture.member().email());
        mockMvc.perform(post("/api/v1/invitations/%d/accept".formatted(invitationId))
                        .header("Authorization", bearer(fixture.member())))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/groups/%d".formatted(fixture.groupId()))
                        .header("Authorization", bearer(fixture.member())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.members.length()").value(2));
    }

    @Test
    void declineInvitationDoesNotAddMember() throws Exception {
        Fixture fixture = invitationFixture();
        Long invitationId = invite(fixture.owner(), fixture.groupId(), fixture.member().email());
        mockMvc.perform(post("/api/v1/invitations/%d/decline".formatted(invitationId))
                        .header("Authorization", bearer(fixture.member())))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/groups/%d".formatted(fixture.groupId()))
                        .header("Authorization", bearer(fixture.member())))
                .andExpect(status().isForbidden());
    }

    @Test
    void nonMemberCannotViewGroupHistory() throws Exception {
        Session owner = register("history-owner@mail.com");
        Session outsider = register("history-outsider@mail.com");
        Long groupId = createGroup(owner, "History Room");
        mockMvc.perform(get("/api/v1/groups/%d/history".formatted(groupId))
                        .header("Authorization", bearer(outsider)))
                .andExpect(status().isForbidden());
    }

    @Test
    void memberCanViewGroupHistory() throws Exception {
        Fixture fixture = acceptedFixture();
        mockMvc.perform(get("/api/v1/groups/%d/history".formatted(fixture.groupId()))
                        .header("Authorization", bearer(fixture.member())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").isNotEmpty());
    }

    @Test
    void groupHistoryOnlyShowsCurrentGroup() throws Exception {
        Session owner = register("history-filter@mail.com");
        Long firstGroup = createGroup(owner, "First Group");
        Long secondGroup = createGroup(owner, "Second Group");
        createGroupTransaction(owner, firstGroup, "EXPENSE", 6000, "Rent");
        createGroupTransaction(owner, secondGroup, "INCOME", 2000, "Contribution");
        mockMvc.perform(get("/api/v1/groups/%d/history".formatted(firstGroup))
                        .header("Authorization", bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].groupId").value(firstGroup));
    }

    @Test
    void inboxOnlyShowsCurrentUsersNotifications() throws Exception {
        Fixture fixture = invitationFixture();
        invite(fixture.owner(), fixture.groupId(), fixture.member().email());
        mockMvc.perform(get("/api/v1/inbox").header("Authorization", bearer(fixture.member())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.type=='GROUP_INVITE')].type").isNotEmpty());
        mockMvc.perform(get("/api/v1/inbox").header("Authorization", bearer(fixture.owner())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.type=='GROUP_INVITE')]").isEmpty());
    }

    @Test
    void acceptingInvitationMarksInviteNotificationRead() throws Exception {
        Fixture fixture = invitationFixture();
        Long invitationId = invite(fixture.owner(), fixture.groupId(), fixture.member().email());
        mockMvc.perform(post("/api/v1/invitations/%d/accept".formatted(invitationId))
                        .header("Authorization", bearer(fixture.member())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"));
        mockMvc.perform(get("/api/v1/inbox").header("Authorization", bearer(fixture.member())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.invitationId==%d && @.type=='GROUP_INVITE')].isRead".formatted(invitationId))
                        .value(org.hamcrest.Matchers.contains(true)))
                .andExpect(jsonPath("$.data[?(@.invitationId==%d && @.type=='GROUP_INVITE')].invitationStatus".formatted(invitationId))
                        .value(org.hamcrest.Matchers.contains("ACCEPTED")));
    }

    @Test
    void groupIncomeCreationWorks() throws Exception {
        Session owner = register("income-owner@mail.com");
        Long groupId = createGroup(owner, "Income Group");
        createGroupTransaction(owner, groupId, "INCOME", 2000, "Contribution");
        mockMvc.perform(get("/api/v1/groups/%d/transactions/summary".formatted(groupId))
                        .header("Authorization", bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalIncome").value(2000));
    }

    @Test
    void groupExpenseCreationWorks() throws Exception {
        Session owner = register("expense-owner@mail.com");
        Long groupId = createGroup(owner, "Expense Group");
        createGroupTransaction(owner, groupId, "EXPENSE", 6000, "Rent");
        mockMvc.perform(get("/api/v1/groups/%d/transactions/summary".formatted(groupId))
                        .header("Authorization", bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalExpenses").value(6000));
    }

    @Test
    void usernameAppearsInGroupHistoryDescription() throws Exception {
        Session owner = register("username-owner@mail.com");
        Long groupId = createGroup(owner, "Username Group");
        createGroupTransaction(owner, groupId, "EXPENSE", 250, "Food");
        mockMvc.perform(get("/api/v1/groups/%d/history".formatted(groupId))
                        .header("Authorization", bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].description").value(org.hamcrest.Matchers.containsString("Test User")));
    }

    @Test
    void adminDashboardEndpointsAreUnavailable() throws Exception {
        Session user = register("no-admin@mail.com");
        mockMvc.perform(get("/api/v1/admin/dashboard").header("Authorization", bearer(user)))
                .andExpect(status().isNotFound());
    }

    private Fixture invitationFixture() throws Exception {
        Session owner = register("fixture-owner@mail.com");
        Session member = register("fixture-member@mail.com");
        Long groupId = createGroup(owner, "Roommates");
        return new Fixture(owner, member, groupId);
    }

    private Fixture acceptedFixture() throws Exception {
        Fixture fixture = invitationFixture();
        Long invitationId = invite(fixture.owner(), fixture.groupId(), fixture.member().email());
        mockMvc.perform(post("/api/v1/invitations/%d/accept".formatted(invitationId))
                .header("Authorization", bearer(fixture.member())));
        return fixture;
    }

    private Long createGroup(Session owner, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/groups")
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","description":"Finals test"}
                                """.formatted(name)))
                .andReturn();
        return root(result).at("/data/id").asLong();
    }

    private Long invite(Session owner, Long groupId, String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/groups/%d/invitations".formatted(groupId))
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s"}
                                """.formatted(email)))
                .andReturn();
        return root(result).at("/data/id").asLong();
    }

    private void createGroupTransaction(Session owner, Long groupId, String type, int amount, String category) throws Exception {
        mockMvc.perform(post("/api/v1/groups/%d/transactions".formatted(groupId))
                .header("Authorization", bearer(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"type":"%s","amount":%d,"category":"%s","actorUserId":%d}
                        """.formatted(type, amount, category, owner.userId())));
    }

    private JsonNode root(MvcResult result) throws Exception {
        return mapper.readTree(result.getResponse().getContentAsString());
    }

    private record Fixture(Session owner, Session member, Long groupId) {
    }
}
