package com.fleet.management.security;

import com.fleet.management.model.User;
import com.fleet.management.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con email: " + email));

        if (!user.getActivo()) {
            throw new UsernameNotFoundException("Usuario inactivo con email: " + email);
        }

        List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                .flatMap(role -> {
                    List<SimpleGrantedAuthority> perms = role.getPermissions().stream()
                            .map(perm -> new SimpleGrantedAuthority(perm.getName()))
                            .collect(Collectors.toList());
                    perms.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));
                    return perms.stream();
                })
                .collect(Collectors.toList());

        return new AuthenticatedUser(user, authorities);
    }
}
