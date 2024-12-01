package main.frame.userservice.service;

import main.frame.userservice.dto.request.RegisterRequest;
import main.frame.userservice.model.User;
import main.frame.shared.dto.UserDTO;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserService {
    Optional<UserDTO> getById(Long id);
    boolean deleteUser(Long id);
    void createUser(RegisterRequest registerRequest);
    void changePassword(Long userId, String oldPassword, String newPassword);
    List<UserDTO> getAllUsers();
  Optional<UserDTO> updateUser(Long id, UserDTO userDTO);
    Optional<UserDTO> updateUserRoles(Long userId, List<String> roleNames);
    Optional<UserDTO> findByEmail(String email);
 //   void userBan(Long id);
    Optional<UserDTO> getUserByPrincipal(Principal principal);
    void activateUser(Long userId);
    void deactivateUser(Long userId);
    List<UserDTO> searchUsers(String email, String username, String phoneNumber, Boolean active, LocalDateTime dateOfCreated, String roleName);
    List<UserDTO> getUsersByRole(String roleName);

}
