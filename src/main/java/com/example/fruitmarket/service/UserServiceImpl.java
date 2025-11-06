package com.example.fruitmarket.service;

import com.example.fruitmarket.dto.UserResponse;
import com.example.fruitmarket.enums.UserStatus;
import com.example.fruitmarket.model.*;
import com.example.fruitmarket.repository.*;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.stream.Collectors.toList;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final VerificationTokenRepository tokenRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final UserDetailRepo userDetailRepo;
    private final DistrictRepo districtRepo;
    private final WardRepo wardRepo;
    private final ProvinceRepo provinceRepo;


    @Value("${app.base-url}")
    private String baseUrl;

    @Override
    @Transactional
    public Users register(Users user) {
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
        user.setStatus(UserStatus.PENDING);
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        Users saved = userRepository.save(user);

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
    public String createVerificationToken(Users user) {
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
        Users user = vt.getUser();
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
        tokenRepository.delete(vt);
        return true;
    }

    @Override
    public Users login(String username, String rawPassword) {
        Optional<Users> opt = userRepository.findByUsername(username);
        if (opt.isEmpty()) return null;
        Users user = opt.get();
        if (user.getStatus() != UserStatus.ACTIVE) return null;
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) return null;
        return user;
    }

    @Override
    public Users findByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    @Override
    public List<User_detail> getUserDetailFromSession(HttpSession session) {
        Users users = (Users)session.getAttribute("loggedUser");
        return userDetailRepo.findAllByUser(users);
    }

    @Override
    public User_detail findUserDetalById(Long addressId) {
        return userDetailRepo.findById(addressId).orElseThrow();
    }

    @Override
    public User_detail saveUserDetail(User_detail userDetail) {
        return userDetailRepo.save(userDetail);

    }

    @Override
    public Users updateProfile(int userId, String email, String phone) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));

        // Kiểm tra trùng email/phone của user khác
        userRepository.findByEmail(email).ifPresent(u -> {
            if (u.getId() != userId) throw new IllegalArgumentException("Email này đã được sử dụng.");
        });
        userRepository.findByPhone(phone).ifPresent(u -> {
            if (u.getId() != userId) throw new IllegalArgumentException("Số điện thoại này đã tồn tại.");
        });

        user.setEmail(email);
        user.setPhone(phone);
        Users saved = userRepository.save(user);
        return saved;
    }

    @Override
    public void changePassword(int userId, String oldPassword, String newPassword) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu cũ không chính xác.");
        }
        if (oldPassword.equals(newPassword)) {
            throw new IllegalArgumentException("Mật khẩu mới phải khác mật khẩu cũ.");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Override
    public Users updateUserStatus(int id, UserStatus status) {
        Users user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));
        user.setStatus(status);
        return userRepository.save(user);
    }

    @Override
    public List<UserResponse> getAllUsers() {
        List<Users> users = userRepository.findAll();
        List<UserResponse> userResponseList = users.stream()
                .map(u -> new UserResponse(
                        u.getId(),
                        u.getUsername(),
                        u.getEmail(),
                        u.getPhone(),
                        u.getRole(),
                        u.getStatus())
                ).toList();
        return  userResponseList;
    }

    @Override
    public UserResponse findUserById(int id) {
        Users user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.getStatus()
        );
    }

    @Override
    @Transactional
    public void updateAddress(Long addressId,
                              Integer provinceId,
                              Integer districtId,
                              String wardCode,
                              String address,
                              String phone) {

        User_detail ud = userDetailRepo.findById(addressId)
                .orElseThrow(() -> new IllegalArgumentException("Address not found: " + addressId));

        // ===== 1) Cập nhật text fields =====
        if (address != null) {
            ud.setAddress(address.trim());
        }
        if (phone != null) {
            ud.setPhone(phone.trim());
        }

        // Lấy hiện trạng
        District currentDistrict = ud.getDistrict();
        Ward currentWard = ud.getWard();

        // ===== 2) Cập nhật theo provinceId (nếu có) =====
        if (provinceId != null) {
            Province newProvince = provinceRepo.findById(provinceId)
                    .orElseThrow(() -> new IllegalArgumentException("Province not found: " + provinceId));
            ud.setProvince(newProvince);

            // Nếu quận hiện tại không thuộc tỉnh mới -> clear district/ward
            if (currentDistrict != null &&
                    !currentDistrict.getProvince().getProvinceId().equals(provinceId)) {
                ud.setDistrict(null);
                ud.setWard(null);
                currentWard = null;
            }
        }

        // ===== 3) Cập nhật theo districtId (nếu có) =====
        if (districtId != null) {
            District newDistrict = districtRepo.findById(districtId)
                    .orElseThrow(() -> new IllegalArgumentException("District not found: " + districtId));

            // Đồng bộ province theo district nếu cần
            if (ud.getProvince() == null ||
                    !newDistrict.getProvince().getProvinceId().equals(ud.getProvince().getProvinceId())) {
                ud.setProvince(newDistrict.getProvince());
            }
            ud.setDistrict(newDistrict);

            // Nếu ward hiện tại không thuộc district mới -> clear ward
            if (currentWard != null &&
                    !currentWard.getDistrict().getDistrictId().equals(districtId)) {
                ud.setWard(null);
            }
        } else if (provinceId != null) {
            // Có set tỉnh nhưng không set quận -> bắt buộc clear quận/phường (vì lệ thuộc tỉnh)
            ud.setDistrict(null);
            ud.setWard(null);
        }

        // ===== 4) Cập nhật theo wardCode (nếu có) =====
        if (wardCode != null) {
            String code = wardCode.trim();
            if (code.isEmpty()) {
                ud.setWard(null);
            } else {
                Ward newWard = wardRepo.findById(code)
                        .orElseThrow(() -> new IllegalArgumentException("Ward not found: " + code));
                // Nếu chưa có district hoặc ward không cùng district -> set lại district
                if (ud.getDistrict() == null ||
                        !newWard.getDistrict().getDistrictId().equals(ud.getDistrict().getDistrictId())) {
                    ud.setDistrict(newWard.getDistrict());
                }
                // Đồng bộ province theo ward->district nếu cần
                if (ud.getProvince() == null ||
                        !newWard.getDistrict().getProvince().getProvinceId().equals(ud.getProvince().getProvinceId())) {
                    ud.setProvince(newWard.getDistrict().getProvince());
                }
                ud.setWard(newWard);
            }
        }

        userDetailRepo.save(ud);
    }
}
