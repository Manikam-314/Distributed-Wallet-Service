package com.practice.practice.model;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;
import java.util.List;
@Entity
@Table(name="wallet")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WalletEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double balance;

    @Version
    private Long version;

    @OneToOne
    @JoinColumn(name = "user_id")  
    @JsonBackReference// explicit mapping
    private UserEntity user;

    // One wallet → many transactions
    @OneToMany(mappedBy = "senderWallet")
    private List<Transaction> sentTransactions;

    @OneToMany(mappedBy = "receiverWallet")
    private List<Transaction> receivedTransactions;
}
