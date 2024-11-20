package main.frame.userservice.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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
    public Optional<User> getUserByPrincipal(Principal principal) {
        if (principal == null) return Optional.empty();
        return findByEmail(principal.getName());
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
    public Optional<User> findByEmail(String email) {
        // Если пусто, то надо возвращать либо пустое либо 403 ошибку, если доступа нет
        return entityManager.createQuery("select u from User u left join fetch u.roles where u.email=:email", User.class)
                .setParameter("email", email)
                .getResultStream()
                .findFirst();
    }

    public UserDTO getUserDTOByEmail(String email) {
        User user = this.findByEmail(email) // Предположим, этот метод возвращает Optional<User>
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь с email: " + email + " не найден!"));
        return user.toUserDTO();  // Преобразуем сущность в DTO
    }

    // Пример метода для преобразования DTO в сущность
    public User convertToEntity(UserDTO userDTO) {
        User user = new User();
        user.setId(userDTO.getId());
        user.setEmail(userDTO.getEmail());
        user.setUsername(userDTO.getUsername());
        user.setRoles(userDTO.getRoles().stream()
                .map(roleDTO -> new Role(roleDTO.getId(), roleDTO.getName())) // Преобразуем RoleDTO в Role
                .collect(Collectors.toSet()));
        return user;
    }
}
