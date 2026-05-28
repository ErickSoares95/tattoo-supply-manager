package com.ericksoares.tattoo.user.presentation.controller;

import com.ericksoares.tattoo.user.application.dto.request.CreateUserRequest;
import com.ericksoares.tattoo.user.application.dto.request.UserFilterRequest;
import com.ericksoares.tattoo.user.application.dto.response.UserResponse;
import com.ericksoares.tattoo.user.application.service.CreateUserService;
import com.ericksoares.tattoo.user.application.service.FindAllUsersService;
import com.ericksoares.tattoo.user.application.service.FindUserByIdService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@CrossOrigin(origins = "*", maxAge = 3600)
@RequiredArgsConstructor
public class UserController {

    private final FindAllUsersService findAllUsersService;
    private final FindUserByIdService findUserByIdService;
    private final CreateUserService createUserService;

    @GetMapping
    public ResponseEntity<Page<UserResponse>> findAll(
            @ModelAttribute UserFilterRequest filter,
            Pageable pageable
    ) {
        return ResponseEntity.ok(
                findAllUsersService.execute(filter, pageable)
        );
    }

    @GetMapping("/{id}")
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

}
