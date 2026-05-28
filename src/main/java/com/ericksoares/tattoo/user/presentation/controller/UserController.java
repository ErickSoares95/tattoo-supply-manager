package com.ericksoares.tattoo.user.presentation.controller;

import com.ericksoares.tattoo.user.application.dto.UserFilterRequest;
import com.ericksoares.tattoo.user.application.dto.UserResponse;
import com.ericksoares.tattoo.user.application.service.FindAllUsersService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@CrossOrigin(origins = "*", maxAge = 3600)
public class UserController {

    private final FindAllUsersService findAllUsersService;

    public UserController(FindAllUsersService findAllUsersService) {
        this.findAllUsersService = findAllUsersService;
    }

    @GetMapping
    public ResponseEntity<Page<UserResponse>> findAll(
            @ModelAttribute UserFilterRequest filter,
            Pageable pageable
    ) {
        return ResponseEntity.ok(
                findAllUsersService.execute(filter, pageable)
        );
    }
}
