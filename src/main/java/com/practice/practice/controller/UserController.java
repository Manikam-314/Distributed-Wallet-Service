package com.practice.practice.controller;


import com.practice.practice.dto.RegisterUserRequest;
import com.practice.practice.model.UserEntity;
import com.practice.practice.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public  UserController(UserService userService) {
        this.userService = userService;
    }
    @PostMapping("/register")
    public ResponseEntity<UserEntity> register(@RequestBody RegisterUserRequest registerRequest){
       UserEntity userEntity= userService.register(registerRequest);
       return ResponseEntity.ok(userEntity);
    }}


// POST /users/register
// POST /wallet/deposit
// POST /wallet/transfer
// GET /wallet/balance
// GET /transactions





































    // @GetMapping("/getById")
    // public ResponseEntity<List<UserEntity>> getUsers(){
    //     return ResponseEntity.ok(userService.getUsers());
    // }

    // @GetMapping("/{id}")
    // public ResponseEntity<UserEntity> getUserById(@PathVariable Long id){
    //     UserEntity userEntity=userService.getUserById(id);
    //     return ResponseEntity.ok(userEntity);
    // }

    // @PutMapping("/{id}")
    // public ResponseEntity<UserEntity> updateUser(@PathVariable Long id, @RequestBody UserEntity user){
    //     UserEntity userEntity=userService.updateUser(id ,user);
    //     return ResponseEntity.ok(userEntity);
    // }


