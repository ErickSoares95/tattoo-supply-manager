package com.ericksoares.tattoo.user.presentation.controller;

import com.ericksoares.tattoo.shared.security.model.AuthenticatedUser;
import com.ericksoares.tattoo.user.application.dto.request.ChangePasswordRequest;
import com.ericksoares.tattoo.user.application.dto.request.CreateUserRequest;
import com.ericksoares.tattoo.user.application.dto.request.UpdateUserRequest;
import com.ericksoares.tattoo.user.application.dto.request.UserFilterRequest;
import com.ericksoares.tattoo.user.application.dto.response.UserResponse;
import com.ericksoares.tattoo.user.application.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@CrossOrigin(origins = "*", maxAge = 3600)
@RequiredArgsConstructor
public class UserController {

    private final FindAllUsersService findAllUsersService;
    private final FindUserByIdService findUserByIdService;
    private final CreateUserService createUserService;
    private final UpdateUserService updateUserService;
    private final DeleteUserService deleteUserService;
    private final ChangePasswordService changePasswordService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserResponse>> findAll(
            @ModelAttribute UserFilterRequest filter,
            Pageable pageable
    ) {
        return ResponseEntity.ok(
                findAllUsersService.execute(filter, pageable)
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> findById(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                findUserByIdService.execute(id)
        );
    }

    @PostMapping
    public ResponseEntity<UserResponse> create(
            @Valid @RequestBody CreateUserRequest request
    ) {

        UserResponse response =
                createUserService.execute(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> update(
            @PathVariable Long id,
            @RequestBody @Valid UpdateUserRequest request
    ) {

        return ResponseEntity.ok(
                updateUserService.execute(id, request)
        );
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id
    ) {

        deleteUserService.execute(id);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/me/password")
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {

        changePasswordService.execute(
                authenticatedUser.getUser().getId(),
                request
        );

        return ResponseEntity.noContent().build();
    }

}
