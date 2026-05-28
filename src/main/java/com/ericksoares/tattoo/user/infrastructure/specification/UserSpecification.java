package com.ericksoares.tattoo.user.infrastructure.specification;

import com.ericksoares.tattoo.user.domain.entity.User;
import com.ericksoares.tattoo.user.domain.enums.UserStatus;
import com.ericksoares.tattoo.user.domain.enums.UserType;
import org.springframework.data.jpa.domain.Specification;

public class UserSpecification {

    public static Specification<User> usernameContains(String username) {
        return (root, query, cb) ->
                username == null ? null :
                        cb.like(
                                cb.lower(root.get("username")),
                                "%" + username.toLowerCase() + "%"
                        );
    }

    public static Specification<User> emailContains(String email) {
        return (root, query, cb) ->
                email == null ? null :
                        cb.like(
                                cb.lower(root.get("email")),
                                "%" + email.toLowerCase() + "%"
                        );
    }

    public static Specification<User> hasStatus(UserStatus status) {
        return (root, query, cb) ->
                status == null ? null :
                        cb.equal(root.get("userStatus"), status);
    }

    public static Specification<User> hasType(UserType type) {
        return (root, query, cb) ->
                type == null ? null :
                        cb.equal(root.get("userType"), type);
    }
}