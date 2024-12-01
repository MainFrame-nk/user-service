package main.frame.userservice.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import main.frame.userservice.dto.request.RegisterRequest;
import main.frame.userservice.model.Role;
import main.frame.userservice.model.User;
import main.frame.shared.dto.UserDTO;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImp implements UserService {
    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    @Override
    public boolean deleteUser(Long id) {
        User user = entityManager.find(User.class, id);
        if (user != null) {
            log.info("Пользователь удален. UserName: {}. Email: {}. ID: {}", user.getUsername(), user.getEmail(), user.getId());
            entityManager.remove(user);
            return true; // Удаление прошло успешно
        } else {
            log.error("Ошибка! Пользователь не найден!");
            return false; // Пользователь не найден
        }
    }

    // Обновление данных пользователя
    @Transactional
    @Override
    public Optional<UserDTO> updateUser(Long id, UserDTO userDTO) {
        User user = entityManager.find(User.class, id);
        if (user != null) {
            user.setEmail(userDTO.getEmail());
            user.setUsername(userDTO.getUsername());
            // Дополнительно обновляем другие поля
            entityManager.merge(user);
            return Optional.of(user.toUserDTO());
        }
        return Optional.empty();
    }

    @Transactional
    @Override
    public Optional<UserDTO> updateUserRoles(Long userId, List<String> roleNames) {
        User user = entityManager.find(User.class, userId);

        if (user == null) {
            log.error("Пользователь с ID {} не найден", userId);
            return Optional.empty();
        }

        // Загрузка ролей из базы данных
        List<Role> roles = entityManager.createQuery(
                        "SELECT r FROM Role r WHERE r.name IN :roleNames", Role.class)
                .setParameter("roleNames", roleNames)
                .getResultList();

        if (roles.isEmpty()) {
            log.error("Роли не найдены: {}", roleNames);
            return Optional.empty();
        }

        user.setRoles(new HashSet<>(roles));  // Обновляем роли пользователя
        entityManager.merge(user);  // Сохраняем изменения
        log.info("Роли пользователя с ID {} успешно обновлены: {}", userId, roleNames);

        return Optional.of(user.toUserDTO());
    }


    @Transactional
    @Override
    public void createUser(RegisterRequest registerRequest) {
        if (findByEmail(registerRequest.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Пользователь с таким email уже существует!");
        }
        // Нужно возвращать не ошибку, а в ответе, если пользователь есть

        User newUser = new User();
        newUser.setEmail(registerRequest.getEmail());
        newUser.setPassword(new BCryptPasswordEncoder().encode(registerRequest.getPassword()));
        newUser.setRoles(Set.of(new Role(1, "ROLE_USER"))); // Пример роли по умолчанию
        entityManager.persist(newUser);

        log.info("Пользователь успешно создан: {}", newUser.getEmail());
    }


    @Override
    public Optional<UserDTO> getUserByPrincipal(Principal principal) {
        if (principal == null) return Optional.empty();
        return findByEmail(principal.getName());
    }

    @Transactional
    @Override
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        // Поиск пользователя по ID
        User user = entityManager.find(User.class, userId);
        if (user == null) {
            throw new UsernameNotFoundException("Пользователь с ID: " + userId + " не найден!");
        }

        // Проверяем текущий пароль
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new IllegalArgumentException("Неверный текущий пароль!");
        }

        // Устанавливаем новый зашифрованный пароль
        user.setPassword(passwordEncoder.encode(newPassword));
        entityManager.merge(user); // Обновляем запись в базе

        log.info("Пароль успешно изменен для пользователя с ID: {}", userId);
    }


    @Override
    @Transactional
    public void activateUser(Long userId) {
        User user = entityManager.find(User.class, userId);
        if (user == null) {
            throw new UsernameNotFoundException("Пользователь с id: " + userId + " не найден!");
        }
        user.setActive(true);
        entityManager.merge(user);
    }


    @Override
    @Transactional
    public void deactivateUser(Long userId) {
        User user = entityManager.find(User.class, userId);
        if (user == null) {
            throw new UsernameNotFoundException("Пользователь с id: " + userId + " не найден!");
        }
        user.setActive(false);
        entityManager.merge(user);
    }


    @Override
    public List<UserDTO> searchUsers(String email, String username, String phoneNumber, Boolean active, LocalDateTime dateOfCreated, String roleName) {
        StringBuilder queryBuilder = new StringBuilder("SELECT DISTINCT u FROM User u LEFT JOIN u.roles r WHERE 1=1");

        // Добавляем условия только для ненулевых параметров
        if (email != null && !email.isEmpty()) {
            queryBuilder.append(" AND LOWER(u.email) LIKE :email");
        }
        if (username != null && !username.isEmpty()) {
            queryBuilder.append(" AND LOWER(u.username) LIKE :username");
        }
        if (phoneNumber != null && !phoneNumber.isEmpty()) {
            queryBuilder.append(" AND u.phoneNumber LIKE :phoneNumber");
        }
        if (active != null) {
            queryBuilder.append(" AND u.active = :active");
        }
        if (dateOfCreated != null) {
            queryBuilder.append(" AND u.dateOfCreated >= :dateOfCreated");
        }
        if (roleName != null && !roleName.isEmpty()) {
            queryBuilder.append(" AND r.name = :roleName");
        }

        TypedQuery<User> query = entityManager.createQuery(queryBuilder.toString(), User.class);

        // Устанавливаем параметры только для ненулевых значений
        if (email != null && !email.isEmpty()) {
            query.setParameter("email", "%" + email.toLowerCase() + "%");
        }
        if (username != null && !username.isEmpty()) {
            query.setParameter("username", "%" + username.toLowerCase() + "%");
        }
        if (phoneNumber != null && !phoneNumber.isEmpty()) {
            query.setParameter("phoneNumber", "%" + phoneNumber + "%");
        }
        if (active != null) {
            query.setParameter("active", active);
        }
        if (dateOfCreated != null) {
            query.setParameter("dateOfCreated", dateOfCreated);
        }
        if (roleName != null && !roleName.isEmpty()) {
            query.setParameter("roleName", roleName);
        }

        return query.getResultList().stream().map(User::toUserDTO).toList();
    }


    @Override
    public List<UserDTO> getUsersByRole(String roleName) {
        TypedQuery<User> query = entityManager.createQuery(
                "SELECT u FROM User u JOIN u.roles r WHERE r.name = :roleName", User.class
        );
        query.setParameter("roleName", roleName);
        return query.getResultList().stream().map(User::toUserDTO).toList();
    }



//    @Override
//    public void userBan(Long id) {
//        User user = userRepository.findById(id).orElse(null);
//        if (user != null) {
//            if (user.isActive()) {
//                user.setActive(false);
//                log.info("Пользователь заблокирован! id = {}; email: {}", user.getId(), user.getEmail());
//            } else {
//                user.setActive(true);
//                log.info("Пользователь разблокирован! id = {}; email: {}", user.getId(), user.getEmail());
//            }
//            userRepository.save(user);
//        }
//    }

    @Override
    public Optional<UserDTO> getById(Long id) {
        User user = entityManager.find(User.class, id);
        if (user == null) {
            throw new UsernameNotFoundException("Пользователь с id: " + id + " не найден!");
        }
        return Optional.of(user.toUserDTO()); // Обернуть результат в Optional
    }

    // Получение всех пользователей
    @Override
    public List<UserDTO> getAllUsers() {
        return entityManager.createQuery("SELECT u FROM User u", User.class)
                .getResultList().stream()
                .map(User::toUserDTO)
                .sorted(Comparator.comparing(UserDTO::getId))
                .collect(Collectors.toList());
    }



//    @Transactional
//    @Override
//    public UserDTO updateUser(UserDTO userDTO, Long id) {
//        User user = userRepository
//                .findById(id)
//                .orElseThrow(() -> new UserNotFoundException("Пользователя: " + userDTO.getEmail() + " не найдено"));
//        if (user != null) {
//            user.setName(userDTO.getName());
//            user.setNickname(userDTO.getNickname());
//            user.setLogin(userDTO.getLogin());
//            user.setEmail(userDTO.getEmail());
//            user.setPhoneNumber(userDTO.getPhoneNumber());
//            if (!passwordEncoder.matches(passwordEncoder.encode(userDTO.getPassword()), user.getPassword())) {
//                user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
//            }
//            user.setRoles(userDTO.getRoles().stream().map(x -> roleRepository.findByRole(x.getRole())).collect(Collectors.toSet()));
//            userRepository.save(user);
//        }
//        return userDTO;
//    }

    @Override
    public Optional<UserDTO> findByEmail(String email) {
        // Выполняем запрос и получаем User
        Optional<User> userOptional = entityManager.createQuery("select u from User u left join fetch u.roles where u.email=:email", User.class)
                .setParameter("email", email)
                .getResultStream()
                .findFirst();

        // Если пользователь найден, преобразуем в UserDTO и возвращаем
        return userOptional.map(User::toUserDTO);  // Предполагается, что у User есть метод toUserDTO
    }

    public boolean userExists(Long userId) {
        return userRepository.existsById(userId);
    }


//    public UserDTO getUserDTOByEmail(String email) {
//        User user = this.findByEmail(email) // Предположим, этот метод возвращает Optional<User>
//                .orElseThrow(() -> new UsernameNotFoundException("Пользователь с email: " + email + " не найден!"));
//        return user.toUserDTO();  // Преобразуем сущность в DTO
//    }

    // Пример метода для преобразования DTO в сущность
//    public User convertToEntity(UserDTO userDTO) {
//        User user = new User();
//        user.setId(userDTO.getId());
//        user.setEmail(userDTO.getEmail());
//        user.setUsername(userDTO.getUsername());
//        user.setRoles(userDTO.getRoles().stream()
//                .map(roleDTO -> new Role(roleDTO.getId(), roleDTO.getName())) // Преобразуем RoleDTO в Role
//                .collect(Collectors.toSet()));
//        return user;
//    }
}
