package com.userservice.user.service;

import com.userservice.user.dto.UserRequest;
import com.userservice.user.dto.UserResponse;
import com.userservice.user.dto.UserUpdateRequest;
import com.userservice.user.entity.User;
import com.userservice.user.exception.DuplicateResourceException;
import com.userservice.user.exception.ResourceNotFoundException;
import com.userservice.user.mapper.UserMapper;
import com.userservice.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Cacheable(value = "users", key = "#id", unless = "#result == null")
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        
        log.debug("Fetching user with id: {}", id);
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        
        stopWatch.stop();
        log.debug("Retrieved user with id: {} in {} ms", id, stopWatch.getTotalTimeMillis());
        
        return userMapper.toResponse(user);
    }

    @Cacheable(value = "users", key = "'username:' + #username", unless = "#result == null")
    @Transactional(readOnly = true)
    public UserResponse getUserByUsername(String username) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        
        log.debug("Fetching user with username: {}", username);
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        
        stopWatch.stop();
        log.debug("Retrieved user with username: {} in {} ms", username, stopWatch.getTotalTimeMillis());
        
        return userMapper.toResponse(user);
    }

    @Cacheable(value = "users", key = "'email:' + #email", unless = "#result == null")
    @Transactional(readOnly = true)
    public UserResponse getUserByEmail(String email) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        
        log.debug("Fetching user with email: {}", email);
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        
        stopWatch.stop();
        log.debug("Retrieved user with email: {} in {} ms", email, stopWatch.getTotalTimeMillis());
        
        return userMapper.toResponse(user);
    }

    @Cacheable(value = "userList", key = "'all:' + #sortBy + ':' + #sortDir")
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        
        log.debug("Fetching all users");
        
        List<User> users = userRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        List<UserResponse> result = users.stream()
                .map(userMapper::toResponse)
                .collect(Collectors.toList());
        
        stopWatch.stop();
        log.debug("Retrieved {} users in {} ms", result.size(), stopWatch.getTotalTimeMillis());
        
        return result;
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> getUsersPaginated(int page, int size, String sortBy, String sortDir) {
        log.debug("Fetching users paginated - page: {}, size: {}, sortBy: {}, sortDir: {}", 
                  page, size, sortBy, sortDir);
        
        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? 
                Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        
        return userRepository.findAll(pageable)
                .map(userMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> searchUsers(String searchTerm, int page, int size) {
        log.debug("Searching users with term: {}", searchTerm);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "username"));
        return userRepository.searchUsers(searchTerm, pageable)
                .map(userMapper::toResponse);
    }

    @CacheEvict(value = {"users", "userList"}, allEntries = true)
    public UserResponse createUser(UserRequest userRequest) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        
        log.info("Creating new user with username: {}", userRequest.getUsername());
        
        validateUserUniqueness(userRequest.getUsername(), userRequest.getEmail());
        
        User user = userMapper.toEntity(userRequest);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        
        User savedUser = userRepository.save(user);
        
        stopWatch.stop();
        log.info("Successfully created user with id: {} in {} ms", savedUser.getId(), stopWatch.getTotalTimeMillis());
        
        // Async cache warming
        warmupUserCacheAsync(savedUser.getId());
        
        return userMapper.toResponse(savedUser);
    }

    @CachePut(value = "users", key = "#id")
    @CacheEvict(value = "userList", allEntries = true)
    public UserResponse updateUser(Long id, UserUpdateRequest updateRequest) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        
        log.info("Updating user with id: {}", id);
        
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        
        validateUpdateUniqueness(existingUser, updateRequest);
        
        userMapper.updateEntity(existingUser, updateRequest);
        existingUser.setUpdatedAt(LocalDateTime.now());
        
        User updatedUser = userRepository.save(existingUser);
        
        stopWatch.stop();
        log.info("Successfully updated user with id: {} in {} ms", updatedUser.getId(), stopWatch.getTotalTimeMillis());
        
        return userMapper.toResponse(updatedUser);
    }

    @CacheEvict(value = {"users", "userList"}, allEntries = true)
    public void deleteUser(Long id) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        
        log.info("Deleting user with id: {}", id);
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        
        userRepository.delete(user);
        
        stopWatch.stop();
        log.info("Successfully deleted user with id: {} in {} ms", id, stopWatch.getTotalTimeMillis());
    }

    @CachePut(value = "users", key = "#id")
    @CacheEvict(value = "userList", allEntries = true)
    public UserResponse updateUserStatus(Long id, User.UserStatus status) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        
        log.info("Updating status for user with id: {} to: {}", id, status);
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        
        user.setStatus(status);
        user.setUpdatedAt(LocalDateTime.now());
        User updatedUser = userRepository.save(user);
        
        stopWatch.stop();
        log.info("Successfully updated status for user with id: {} in {} ms", id, stopWatch.getTotalTimeMillis());
        
        return userMapper.toResponse(updatedUser);
    }

    @Cacheable(value = "userStats", key = "'total'")
    @Transactional(readOnly = true)
    public long getTotalUsersCount() {
        return userRepository.count();
    }

    @Cacheable(value = "userStats", key = "'status:' + #status")
    @Transactional(readOnly = true)
    public long getUsersCountByStatus(User.UserStatus status) {
        return userRepository.countByStatus(status);
    }
    
    @Async
    public CompletableFuture<Void> warmupUserCacheAsync(Long userId) {
        try {
            log.debug("Warming up cache for user: {}", userId);
            getUserById(userId);
            log.debug("Cache warmed up for user: {}", userId);
        } catch (Exception e) {
            log.error("Error warming up cache for user: {}", userId, e);
        }
        return CompletableFuture.completedFuture(null);
    }
    
    public void warmupUserCache(Long userId) {
        warmupUserCacheAsync(userId).join();
    }

    private void validateUserUniqueness(String username, String email) {
        if (userRepository.existsByUsername(username)) {
            throw new DuplicateResourceException("User", "username", username);
        }
        
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("User", "email", email);
        }
    }

    private void validateUpdateUniqueness(User existingUser, UserUpdateRequest updateRequest) {
        if (updateRequest.getUsername() != null && 
            !existingUser.getUsername().equals(updateRequest.getUsername()) &&
            userRepository.existsByUsername(updateRequest.getUsername())) {
            throw new DuplicateResourceException("User", "username", updateRequest.getUsername());
        }
        
        if (updateRequest.getEmail() != null && 
            !existingUser.getEmail().equals(updateRequest.getEmail()) &&
            userRepository.existsByEmail(updateRequest.getEmail())) {
            throw new DuplicateResourceException("User", "email", updateRequest.getEmail());
        }
    }
}
