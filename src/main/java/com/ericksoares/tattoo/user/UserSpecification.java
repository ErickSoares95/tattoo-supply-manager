package com.ericksoares.tattoo.user;

import org.springframework.data.jpa.domain.Specification;

class UserSpecification {

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