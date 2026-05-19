package com.example.taskboard.user;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public UserSummary me() {
        AppUserDetails u = CurrentUser.get();
        return new UserSummary(u.getUserId(), u.getUsername(), u.getUser().displayName());
    }

    @GetMapping("/users/search")
    public List<UserSummary> search(@RequestParam("q") String q) {
        if (q == null || q.isBlank()) {
            return List.of();
        }
        return userRepository.searchByUsername(q.trim(), 20);
    }
}
