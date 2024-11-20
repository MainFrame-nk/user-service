package main.frame.userservice.model;

import jakarta.persistence.*;
import lombok.*;
import main.frame.shared.dto.RoleDTO;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.GrantedAuthority;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role implements GrantedAuthority {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Override
    public String toString() {
        return name.substring(5);
    }

    @Override
    public String getAuthority() {
        return name;
    }

    @Bean
    public Role roleWithOutPrefix(Role role) {
        return Role.builder()
                .name(role.getName().substring(5))
                .build();
    }
    // Преобразование Role в RoleDTO
    public RoleDTO toRoleDTO() {
        return new RoleDTO(this.id, this.name);
    }
}
