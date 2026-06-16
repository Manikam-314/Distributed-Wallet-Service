package com.programming.techie.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "wallets")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WalletEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private Long userId;

    private java.math.BigDecimal balance;

    @Version
    private Long version;
}
//
//    @OneToOne
//    @JoinColumn(name = "user_id")
//    @JsonBackReference // explicit mapping
//    private UserEntity user;
//
//    // One wallet → many transactions
//    @OneToMany(mappedBy = "senderWallet")
//    @JsonIgnore
//    private List<Transaction> sentTransactions;
//
//    @OneToMany(mappedBy = "receiverWallet")
//    @JsonIgnore
//    private List<Transaction> receivedTransactions;
//}
