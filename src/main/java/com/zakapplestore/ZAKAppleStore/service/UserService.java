package com.zakapplestore.ZAKAppleStore.service;

import com.zakapplestore.ZAKAppleStore.dto.AddressRequest;
import com.zakapplestore.ZAKAppleStore.dto.AddressResponse;
import com.zakapplestore.ZAKAppleStore.dto.AdminCustomerResponse;
import com.zakapplestore.ZAKAppleStore.dto.LoginHistoryResponse;
import com.zakapplestore.ZAKAppleStore.dto.UpdateProfileRequest;
import com.zakapplestore.ZAKAppleStore.dto.UserProfileResponse;
import com.zakapplestore.ZAKAppleStore.entity.User;
import com.zakapplestore.ZAKAppleStore.entity.UserAddress;
import com.zakapplestore.ZAKAppleStore.entity.UserRole;
import com.zakapplestore.ZAKAppleStore.exception.ResourceNotFoundException;
import com.zakapplestore.ZAKAppleStore.repository.UserAddressRepository;
import com.zakapplestore.ZAKAppleStore.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserAddressRepository userAddressRepository;
    private final LoginHistoryService loginHistoryService;

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(String email) {
        User user = getUserByEmail(email);
        List<AddressResponse> addresses = userAddressRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toAddressResponse)
                .toList();

        return UserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .mobile(user.getMobile())
                .profileImage(user.getProfileImage())
                .emailVerified(user.isEmailVerified())
                .mobileVerified(user.isMobileVerified())
                .role(user.getRole())
                .status(user.getStatus())
                .provider(user.getProvider())
                .addresses(addresses)
                .build();
    }

    @Transactional
    public UserProfileResponse updateProfile(String email, UpdateProfileRequest request) {
        User user = getUserByEmail(email);
        user.setUsername(request.getUsername().trim());
        user.setMobile(request.getMobile());
        user.setProfileImage(request.getProfileImage());
        userRepository.save(user);
        return getProfile(email);
    }

    @Transactional
    public AddressResponse addAddress(String email, AddressRequest request) {
        User user = getUserByEmail(email);
        long existingCount = userAddressRepository.countByUserId(user.getId());
        boolean makeDefault = Boolean.TRUE.equals(request.getIsDefault()) || existingCount == 0;

        if (makeDefault) {
            userAddressRepository.clearDefaultForUser(user.getId());
        }

        UserAddress address = UserAddress.builder()
                .user(user)
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .addressLine1(request.getAddressLine1())
                .addressLine2(request.getAddressLine2())
                .city(request.getCity())
                .state(request.getState())
                .pinCode(request.getPinCode())
                .country(request.getCountry())
                .isDefault(makeDefault)
                .build();

        return toAddressResponse(userAddressRepository.save(address));
    }

    @Transactional(readOnly = true)
    public List<AddressResponse> getAddresses(String email) {
        User user = getUserByEmail(email);
        return userAddressRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toAddressResponse)
                .toList();
    }

    @Transactional
    public void deleteAddress(String email, UUID addressId) {
        User user = getUserByEmail(email);
        UserAddress address = userAddressRepository.findByIdAndUserId(addressId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
        userAddressRepository.delete(address);
    }

    @Transactional(readOnly = true)
    public List<LoginHistoryResponse> getLoginHistory(String email) {
        User user = getUserByEmail(email);
        return loginHistoryService.getRecentLogins(user);
    }

    @Transactional(readOnly = true)
    public List<AdminCustomerResponse> getRegisteredCustomers() {
        return userRepository.findByRoleOrderByCreatedAtDesc(UserRole.CUSTOMER)
                .stream()
                .map(user -> AdminCustomerResponse.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .mobile(user.getMobile())
                        .provider(user.getProvider())
                        .emailVerified(user.isEmailVerified())
                        .mobileVerified(user.isMobileVerified())
                        .status(user.getStatus())
                        .createdAt(user.getCreatedAt())
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public User getUserByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private AddressResponse toAddressResponse(UserAddress address) {
        return AddressResponse.builder()
                .id(address.getId())
                .fullName(address.getFullName())
                .phone(address.getPhone())
                .addressLine1(address.getAddressLine1())
                .addressLine2(address.getAddressLine2())
                .city(address.getCity())
                .state(address.getState())
                .pinCode(address.getPinCode())
                .country(address.getCountry())
                .isDefault(address.isDefault())
                .createdAt(address.getCreatedAt())
                .build();
    }
}
