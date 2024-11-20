package main.frame.userservice.service;

import main.frame.userservice.dto.request.RegisterRequest;
import main.frame.userservice.model.User;
import main.frame.shared.dto.UserDTO;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

public interface UserService {
    Optional<UserDTO> getById(Long id);
    boolean deleteUser(Long id);
    void createUser(RegisterRequest registerRequest);
    List<UserDTO> getAllUsers();
  Optional<UserDTO> updateUser(Long id, UserDTO userDTO);
    Optional<UserDTO> updateUserRoles(Long userId, List<String> roleNames);
    Optional<User> findByEmail(String email);
 //   void userBan(Long id);
    Optional<User> getUserByPrincipal(Principal principal);
}
