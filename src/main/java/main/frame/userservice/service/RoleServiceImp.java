package main.frame.userservice.service;


import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import main.frame.userservice.model.Role;
import main.frame.shared.dto.RoleDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class RoleServiceImp implements RoleService {

    @PersistenceContext
    private EntityManager entityManager;

    public Optional<Role> findById(int id) {
        return Optional.ofNullable(entityManager.find(Role.class, id));
    }

    public Optional<Role> findByName(String name) {
        return entityManager.createQuery("SELECT r FROM Role r WHERE r.name = :name", Role.class)
                .setParameter("name", name)
                .getResultStream()
                .findFirst();
    }

    @Override
    @Transactional
    public List<Role> getAllRoles() {
        return entityManager.createQuery("SELECT r FROM Role r", Role.class)
                .getResultList();
    }

    public RoleDTO getRoleById(Long id) {
        Role role = entityManager.find(Role.class, id); // Получаем сущность Role по ID
        if (role == null) {
            throw new RuntimeException("Role not found"); // Обработка случая, когда роль не найдена
        }
        return role.toRoleDTO(); // Преобразуем сущность в DTO
    }

    // Преобразование RoleDTO в сущность Role для сохранения в базе данных
    public Role convertToEntity(RoleDTO roleDTO) {
        Role role = new Role();
        role.setId(roleDTO.getId());
        role.setName(roleDTO.getName());
        return role;
    }

//    // Пример метода для сохранения роли
//    public RoleDTO saveRole(RoleDTO roleDTO) {
//        Role role = convertToEntity(roleDTO);  // Преобразуем DTO в сущность
//        Role savedRole = roleRepository.save(role);  // Сохраняем роль в базе данных
//        return savedRole.toRoleDTO();  // Возвращаем сохранённую роль в виде DTO
//    }
}
