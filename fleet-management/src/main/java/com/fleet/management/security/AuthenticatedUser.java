package com.fleet.management.security;

import com.fleet.management.model.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

/**
 * Custom UserDetails implementation that holds a reference to the JPA User entity.
 * This allows BaseEntity audit callbacks to access the full User entity
 * from the SecurityContext without needing to query the repository.
 */
@Getter
public class AuthenticatedUser implements UserDetails {

    private final User user;
    private final Collection<? extends GrantedAuthority> authorities;

    public AuthenticatedUser(User user, Collection<? extends GrantedAuthority> authorities) {
        this.user = user;
        this.authorities = authorities;
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.getActivo() != null && user.getActivo();
    }
}
