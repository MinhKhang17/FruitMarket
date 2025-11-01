package com.example.fruitmarket.service;

import com.example.fruitmarket.model.User_detail;
import com.example.fruitmarket.model.User;
import com.example.fruitmarket.model.VerificationToken;
import com.example.fruitmarket.repository.UserDetailRepo;
import com.example.fruitmarket.repository.UserRepository;
import com.example.fruitmarket.repository.VerificationTokenRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final VerificationTokenRepository tokenRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final UserDetailRepo userDetailRepo;


    @Value("${app.base-url}")
    private String baseUrl;

    @Override
    @Transactional
    public User register(User user) {
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Tên đăng nhập đã tồn tại.");
        }

        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email này đã được sử dụng.");
        }

        if (userRepository.findByPhone(user.getPhone()).isPresent()) {
            throw new IllegalArgumentException("Số điện thoại này đã tồn tại.");
        }

        user.setRole("CUSTOMER");
        user.setStatus("PENDING");
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        User saved = userRepository.save(user);

        String token = createVerificationToken(saved);
        String link = baseUrl + "/auth/verify?token=" + token;
        String content = String.format("""
        Xin chào %s,
        
        Cảm ơn bạn đã đăng ký tài khoản tại FruitMarket.
        Vui lòng xác thực tài khoản bằng cách nhấn vào liên kết sau:
        %s
        
        Nếu bạn không đăng ký, vui lòng bỏ qua email này.
        """, saved.getUsername(), link);

        emailService.sendEmail(saved.getEmail(), "Xác thực email - FruitMarket", content);

        return saved;
    }


    @Override
    public String createVerificationToken(User user) {
        // xóa token cũ
        tokenRepository.deleteByUserId(user.getId());
        String token = UUID.randomUUID().toString();
        VerificationToken vt = VerificationToken.builder()
                .token(token)
                .user(user)
                .expiryDate(LocalDateTime.now().plusDays(1))
                .build();
        tokenRepository.save(vt);
        return token;
    }

    @Override
    @Transactional
    public boolean verifyToken(String token) {
        Optional<VerificationToken> opt = tokenRepository.findByToken(token);
        if (opt.isEmpty()) return false;
        VerificationToken vt = opt.get();
        if (vt.getExpiryDate().isBefore(LocalDateTime.now())) {
            return false;
        }
        User user = vt.getUser();
        user.setStatus("ACTIVE");
        userRepository.save(user);
        tokenRepository.delete(vt);
        return true;
    }

    @Override
    public User login(String username, String rawPassword) {
        Optional<User> opt = userRepository.findByUsername(username);
        if (opt.isEmpty()) return null;
        User user = opt.get();
        if (!"ACTIVE".equals(user.getStatus())) return null;
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) return null;
        return user;
    }

    @Override
    public User findByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    @Override
    public List<User_detail> getUserDetailFromSession(HttpSession session) {
        User user = (User)session.getAttribute("loggedUser");
        return userDetailRepo.findAllByUser(user);
    }


}
