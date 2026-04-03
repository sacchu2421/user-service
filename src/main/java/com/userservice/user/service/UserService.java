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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        log.debug("Fetching user with id: {}", id);
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        
        return userMapper.toResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserByUsername(String username) {
        log.debug("Fetching user with username: {}", username);
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        
        return userMapper.toResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserByEmail(String email) {
        log.debug("Fetching user with email: {}", email);
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        
        return userMapper.toResponse(user);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        log.debug("Fetching all users");
        
        List<User> users = userRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        return users.stream()
                .map(userMapper::toResponse)
                .collect(Collectors.toList());
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

    public UserResponse createUser(UserRequest userRequest) {
        log.info("Creating new user with username: {}", userRequest.getUsername());
        
        validateUserUniqueness(userRequest.getUsername(), userRequest.getEmail());
        
        User user = userMapper.toEntity(userRequest);
        User savedUser = userRepository.save(user);
        
        log.info("Successfully created user with id: {}", savedUser.getId());
        return userMapper.toResponse(savedUser);
    }

    public UserResponse updateUser(Long id, UserUpdateRequest updateRequest) {
        log.info("Updating user with id: {}", id);
        
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        
        validateUpdateUniqueness(existingUser, updateRequest);
        
        userMapper.updateEntity(existingUser, updateRequest);
        User updatedUser = userRepository.save(existingUser);
        
        log.info("Successfully updated user with id: {}", updatedUser.getId());
        return userMapper.toResponse(updatedUser);
    }

    public void deleteUser(Long id) {
        log.info("Deleting user with id: {}", id);
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        
        userRepository.delete(user);
        log.info("Successfully deleted user with id: {}", id);
    }

    public UserResponse updateUserStatus(Long id, User.UserStatus status) {
        log.info("Updating status for user with id: {} to: {}", id, status);
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        
        user.setStatus(status);
        User updatedUser = userRepository.save(user);
        
        log.info("Successfully updated status for user with id: {}", id);
        return userMapper.toResponse(updatedUser);
    }

    @Transactional(readOnly = true)
    public long getTotalUsersCount() {
        return userRepository.count();
    }

    @Transactional(readOnly = true)
    public long getUsersCountByStatus(User.UserStatus status) {
        return userRepository.countByStatus(status);
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
