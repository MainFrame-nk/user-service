package main.frame.userservice.model;

import jakarta.persistence.*;
import lombok.*;
import main.frame.shared.dto.UserDTO;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@Entity
@Table(name = "users")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", unique = true)
    private String email;

    @Column(name = "password", length = 1000, nullable = false)
    private String password;

    @Column(name = "username")
    private String username;

    @Column(name = "phoneNumber")
    private String phoneNumber;

    @Column(name = "active")
    private boolean active;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "users_roles",
            joinColumns = @JoinColumn(name = "user_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "role_id", referencedColumnName = "id"))
    private Set<Role> roles = new HashSet<>();

    private LocalDateTime dateOfCreated;

    @PrePersist
    private void init() {
        dateOfCreated = LocalDateTime.now();
    }

//
//    public String getAllRolesWithOutBrackets (Set<Role> roles){
//        return roles.stream().map(Role::getRole).map(x->x.substring(5)).collect(Collectors.joining(", "));
//    }
//    public UserDTO toUserDTO() {
//        return UserDTO.toUserDTO(this);
//    }
//
//    public static UserDTO toUserDTO(User user) {
//        return UserDTO.builder()
//                .id(user.getId())
//                .email(user.getEmail())
//                .username(user.getUsername())
//                .phoneNumber(user.getPhoneNumber())
//                .active(user.isActive())
//                .dateOfCreated(user.getDateOfCreated())
//                .roles(user.getRoles().stream().map(Role::toRoleDto).collect(Collectors.toSet()))
//                .build();
//    }

    // Преобразование сущности User в UserDTO
    public UserDTO toUserDTO() {
        return new UserDTO(
                this.id,
                this.email,
                this.username,
                this.password,
                this.roles.stream()
                        .map(Role::toRoleDTO)  // Преобразование Role в RoleDTO
                        .collect(Collectors.toSet())
        );
    }


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return getRoles();
    }

    @Override
    public String getUsername() {
        return email;
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
        return active;
    }
}
