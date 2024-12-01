package main.frame.userservice.service;

import main.frame.shared.dto.RoleDTO;
import main.frame.shared.dto.UserDTO;
import main.frame.userservice.model.Role;
import main.frame.userservice.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserDetailsService implements org.springframework.security.core.userdetails.UserDetailsService {
    private final UserService userService;

    public UserDetailsService(UserService userService) {
        this.userService = userService;
    }

//    @Override
//    @Transactional
//    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
//        User user = userService.findByEmail(email)
//                .orElseThrow(() -> new UsernameNotFoundException(String.format("Пользователь '%s' не найден!", email)));
//
//        return new org.springframework.security.core.userdetails.User(user.getEmail(), user.getPassword(),
//                mapRoleAuthority(user.getRoles()));
//    }

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        UserDTO user = userService.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(String.format("Пользователь '%s' не найден!", email)));

        // Преобразуем роли из RoleDTO в Role
        Set<Role> roles = user.getRoles().stream()
                .map(this::convertToRole) // Маппим из RoleDTO в Role
                .collect(Collectors.toSet());

        return new org.springframework.security.core.userdetails.User(user.getEmail(), user.getPassword(),
                mapRoleAuthority(roles));
    }

    // Преобразование RoleDTO в Role
    private Role convertToRole(RoleDTO roleDTO) {
        // Здесь нужно преобразовать RoleDTO в Role, например:
        return new Role(roleDTO.getId(), roleDTO.getName());
    }

    private Collection<? extends GrantedAuthority> mapRoleAuthority (Collection<Role> roles) {
        return roles.stream().map(r -> new SimpleGrantedAuthority(r.getName())).collect(Collectors.toList());
    }
}