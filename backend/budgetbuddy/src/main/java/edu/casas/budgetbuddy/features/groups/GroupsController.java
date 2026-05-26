package edu.casas.budgetbuddy.features.groups;

import edu.casas.budgetbuddy.features.auth.AuthService;
import edu.casas.budgetbuddy.features.groups.GroupsDtos.AddMemberRequest;
import edu.casas.budgetbuddy.features.groups.GroupsDtos.GroupRequest;
import edu.casas.budgetbuddy.shared.store.BudgetBuddyStore.UserRecord;
import edu.casas.budgetbuddy.shared.utils.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/groups")
public class GroupsController {
    private final AuthService authService;
    private final GroupsService groupsService;
    private final GroupActivityService groupActivityService;

    public GroupsController(AuthService authService, GroupsService groupsService,
                            GroupActivityService groupActivityService) {
        this.authService = authService;
        this.groupsService = groupsService;
        this.groupActivityService = groupActivityService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<GroupsDtos.GroupDto>> create(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody GroupRequest request) {
        UserRecord user = authService.requireUser(authorization);
        GroupsDtos.GroupDto group = groupsService.create(user.id(), request.name(), request.description());
        groupActivityService.log(group.id(), user.id(), "GROUP_CREATED", "GROUP", group.id(),
                null, group.name(), groupActivityService.displayName(user.id()) + " created group " + group.name());
        return ResponseEntity.status(201).body(ApiResponse.success(group, "Group created"));
    }

    @GetMapping
    public ApiResponse<?> list(@RequestHeader(value = "Authorization", required = false) String authorization) {
        UserRecord user = authService.requireUser(authorization);
        return ApiResponse.success(groupsService.list(user.id()), "Groups loaded");
    }

    @GetMapping("/{groupId}")
    public ApiResponse<GroupsDtos.GroupDetailDto> detail(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long groupId) {
        UserRecord user = authService.requireUser(authorization);
        return ApiResponse.success(groupsService.detail(user.id(), groupId), "Group loaded");
    }

    @PutMapping("/{groupId}")
    public ApiResponse<GroupsDtos.GroupDto> update(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long groupId,
            @Valid @RequestBody GroupRequest request) {
        UserRecord user = authService.requireUser(authorization);
        return ApiResponse.success(groupsService.update(user.id(), groupId, request.name(), request.description()),
                "Group updated");
    }

    @PostMapping("/{groupId}/members")
    public ApiResponse<Void> addMember(@RequestHeader(value = "Authorization", required = false) String authorization,
                                       @PathVariable Long groupId,
                                       @Valid @RequestBody AddMemberRequest request) {
        UserRecord user = authService.requireUser(authorization);
        groupsService.addMember(user.id(), groupId, request.email());
        return ApiResponse.success(null, "Member added");
    }

    @GetMapping("/{groupId}/history")
    public ApiResponse<?> history(@RequestHeader(value = "Authorization", required = false) String authorization,
                                  @PathVariable Long groupId) {
        UserRecord user = authService.requireUser(authorization);
        return ApiResponse.success(groupActivityService.history(user.id(), groupId), "Group history loaded");
    }

    @DeleteMapping("/{groupId}/members/{memberUserId}")
    public ApiResponse<Void> removeMember(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long groupId,
            @PathVariable Long memberUserId) {
        UserRecord user = authService.requireUser(authorization);
        groupsService.removeMember(user.id(), groupId, memberUserId);
        return ApiResponse.success(null, "Member removed");
    }

    @DeleteMapping("/{groupId}")
    public ApiResponse<Void> delete(@RequestHeader(value = "Authorization", required = false) String authorization,
                                    @PathVariable Long groupId) {
        UserRecord user = authService.requireUser(authorization);
        groupsService.softDelete(user.id(), groupId);
        return ApiResponse.success(null, "Group deleted");
    }
}
