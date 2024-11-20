package main.frame.userservice.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import main.frame.userservice.dto.request.RegisterRequest;
import main.frame.userservice.model.User;
import main.frame.userservice.service.UserService;
import main.frame.shared.dto.UserDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@Slf4j
@RequestMapping("/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<String> createUser(@RequestBody RegisterRequest registerRequest) {
        userService.createUser(registerRequest);
        return ResponseEntity.ok("User registered successfully");
    }

    @GetMapping("/user")
    public ResponseEntity<UserDTO> getUserDetails(@AuthenticationPrincipal UserDetails userDetails) {
        System.out.println("UserDetails: " + userDetails); // Логируем полученные данные
        Optional<User> optionalUser = userService.findByEmail(userDetails.getUsername());
        if (optionalUser.isPresent()) {
            UserDTO userDTO = optionalUser.get().toUserDTO();
            return ResponseEntity.ok(userDTO);
        } else {
            System.out.println("User not found: " + userDetails.getUsername());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<User> findByEmail(@PathVariable String email) {
        Optional<User> userOptional = userService.findByEmail(email);
        return userOptional.map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> findById(@PathVariable Long id) {
        Optional<UserDTO> optionalUser = userService.getById(id);
        if (optionalUser.isPresent()) {
            return ResponseEntity.ok(optionalUser.get());
        } else {
            System.out.println("User not found with" + id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    // Удаление пользователя по ID
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteUser(@PathVariable Long id) {
        boolean isDeleted = userService.deleteUser(id);
        if (isDeleted) {
            return ResponseEntity.ok("User deleted successfully");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }
    }

    // Обновление данных пользователя
    @PutMapping("/{id}")
    public ResponseEntity<UserDTO> updateUser(@PathVariable Long id, @RequestBody UserDTO userDTO) {
        Optional<UserDTO> updatedUser = userService.updateUser(id, userDTO);
        return updatedUser.map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
    }

    @PutMapping("/{id}/roles")
    public ResponseEntity<UserDTO> updateUserRoles(
            @PathVariable Long id,
            @RequestBody List<String> roles) {
        Optional<UserDTO> updatedUser = userService.updateUserRoles(id, roles);

        // Если обновление не удалось, возвращаем ошибку 400
        return updatedUser.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(null));

    }

    // Получение всех пользователей
    @GetMapping("/")
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        List<UserDTO> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

//    @GetMapping("/test-user")
//    public ResponseEntity<String> testEndpoint(@RequestHeader Map<String, String> headers) {
//        headers.forEach((key, value) -> System.out.println(key + " -> " + value));
//        return ResponseEntity.ok("Headers logged");
//    }
//
//    @GetMapping("/debug-headers")
//    public ResponseEntity<?> debugHeaders(HttpServletRequest request) {
//        Enumeration<String> headers = request.getHeaderNames();
//        while (headers.hasMoreElements()) {
//            String header = headers.nextElement();
//            System.out.println(header + ": " + request.getHeader(header));
//        }
//        return ResponseEntity.ok().build();
//    }
}