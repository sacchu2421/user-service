package com.userservice.user.controller;

import com.userservice.user.dto.UserRequest;
import com.userservice.user.dto.UserResponse;
import com.userservice.user.dto.UserUpdateRequest;
import com.userservice.user.entity.User;
import com.userservice.user.service.UserService;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Validated
public class UserController {

    private final UserService userService;

    @PostMapping
    @Counted(value = "user.create.requests", description = "Number of user creation requests")
    @Timed(value = "user.create.duration", description = "Time taken to create user")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserRequest userRequest) {
        UserResponse createdUser = userService.createUser(userRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }

    @GetMapping("/{id}")
    @Counted(value = "user.read.requests", description = "Number of user read requests")
    @Timed(value = "user.read.duration", description = "Time taken to read user")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        UserResponse user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/username/{username}")
    @Counted(value = "user.read.requests", description = "Number of user read requests")
    @Timed(value = "user.read.duration", description = "Time taken to read user")
    public ResponseEntity<UserResponse> getUserByUsername(@PathVariable String username) {
        UserResponse user = userService.getUserByUsername(username);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/email/{email}")
    @Counted(value = "user.read.requests", description = "Number of user read requests")
    @Timed(value = "user.read.duration", description = "Time taken to read user")
    public ResponseEntity<UserResponse> getUserByEmail(@PathVariable String email) {
        UserResponse user = userService.getUserByEmail(email);
        return ResponseEntity.ok(user);
    }

    @GetMapping
    @Counted(value = "user.list.requests", description = "Number of user list requests")
    @Timed(value = "user.list.duration", description = "Time taken to list users")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/paginated")
    @Counted(value = "user.paginated.requests", description = "Number of user paginated requests")
    @Timed(value = "user.paginated.duration", description = "Time taken to get paginated users")
    public ResponseEntity<Page<UserResponse>> getUsersPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        // Validate page size
        if (size > 100) {
            size = 100;
        }
        
        Page<UserResponse> users = userService.getUsersPaginated(page, size, sortBy, sortDir);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/search")
    @Counted(value = "user.search.requests", description = "Number of user search requests")
    @Timed(value = "user.search.duration", description = "Time taken to search users")
    public ResponseEntity<Page<UserResponse>> searchUsers(
            @RequestParam String searchTerm,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        // Validate search term
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            throw new IllegalArgumentException("Search term cannot be empty");
        }
        
        // Validate page size
        if (size > 50) {
            size = 50;
        }
        
        Page<UserResponse> users = userService.searchUsers(searchTerm, page, size);
        return ResponseEntity.ok(users);
    }

    @PutMapping("/{id}")
    @Counted(value = "user.update.requests", description = "Number of user update requests")
    @Timed(value = "user.update.duration", description = "Time taken to update user")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest updateRequest) {
        
        UserResponse updatedUser = userService.updateUser(id, updateRequest);
        return ResponseEntity.ok(updatedUser);
    }

    @PatchMapping("/{id}/status")
    @Counted(value = "user.status.requests", description = "Number of user status update requests")
    @Timed(value = "user.status.duration", description = "Time taken to update user status")
    public ResponseEntity<UserResponse> updateUserStatus(
            @PathVariable Long id,
            @RequestParam User.UserStatus status) {
        
        UserResponse updatedUser = userService.updateUserStatus(id, status);
        return ResponseEntity.ok(updatedUser);
    }

    @DeleteMapping("/{id}")
    @Counted(value = "user.delete.requests", description = "Number of user delete requests")
    @Timed(value = "user.delete.duration", description = "Time taken to delete user")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats/total")
    @Counted(value = "user.stats.requests", description = "Number of user stats requests")
    @Timed(value = "user.stats.duration", description = "Time taken to get user stats")
    public ResponseEntity<Long> getTotalUsersCount() {
        long count = userService.getTotalUsersCount();
        return ResponseEntity.ok(count);
    }

    @GetMapping("/stats/status/{status}")
    @Counted(value = "user.stats.requests", description = "Number of user stats requests")
    @Timed(value = "user.stats.duration", description = "Time taken to get user stats")
    public ResponseEntity<Long> getUsersCountByStatus(@PathVariable User.UserStatus status) {
        long count = userService.getUsersCountByStatus(status);
        return ResponseEntity.ok(count);
    }
}
