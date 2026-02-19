    package com.practice.practice.service;

    import com.practice.practice.dto.RegisterUserRequest;
    import com.practice.practice.model.UserEntity;
    import com.practice.practice.model.WalletEntity;
    import com.practice.practice.repository.UserRepository;
    import com.practice.practice.repository.WalletRepository;
    import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

    @Service
    public class UserService {

        private final UserRepository userRepository;
        private final WalletRepository walletRepository;

        // single constructor injection
        public UserService(UserRepository userRepository,
                        WalletRepository walletRepository) {
            this.userRepository = userRepository;
            this.walletRepository = walletRepository;
        }
@Transactional
        public UserEntity register(RegisterUserRequest registerUser) {

            // 1. Convert DTO → Entity
            UserEntity user = new UserEntity();
            user.setName(registerUser.getName());
            user.setEmail(registerUser.getEmail());
            user.setPassword(registerUser.getPassword());

            // 2. Save user first
            UserEntity savedUser = userRepository.save(user);

            // 3. Create wallet automatically
            WalletEntity wallet = new WalletEntity();
            wallet.setBalance(0.0);
            wallet.setUser(savedUser);

           
        WalletEntity savedWallet = walletRepository.save(wallet);

        // ⭐ VERY IMPORTANT — link wallet back to user
        savedUser.setWallet(savedWallet);

        return savedUser;
        }
    }























































    // public List<UserEntity> getUsers(){
    //     return userRepository.findAll();
    // }

    // public UserEntity getUserById(Long id) {
    //     return userRepository.findById(id)
    //             .orElseThrow(() -> new RuntimeException("User not found with id: " + id));

    // }

    // public UserEntity updateUser(Long id,UserEntity user) {

    //     UserEntity exist=userRepository.findById(id)
    //             .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    //     exist.setName(user.getName());
    //     exist.setEmail(user.getEmail());
    //     exist.setPassword(user.getPassword());
    //     userRepository.save(exist);
    //     return userRepository.save(exist);
    // }
