package com.practice.practice.service;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import com.practice.practice.dto.DepositRequest;
import com.practice.practice.dto.TransferRequest;
import com.practice.practice.model.WalletEntity;
import com.practice.practice.repository.WalletRepository;
import jakarta.transaction.Transactional;

@Service
public class WalletService {
    private final WalletRepository walletRepository;

    public WalletService(WalletRepository walletRepository){
        this.walletRepository= walletRepository;
    }

    public WalletEntity deposit(DepositRequest depositRequest){
         // 1. Find existing wallet
        WalletEntity wallet = walletRepository.findById(depositRequest.getWalletId())
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        // 2. Update balance
        wallet.setBalance(wallet.getBalance() + depositRequest.getAmount());

        // 3. Save wallet
        return walletRepository.save(wallet);

        // save in DB
    }
  // ================= TRANSFER =================
@Transactional
public void transfer(TransferRequest request){

    try {

        WalletEntity sender = walletRepository.findById(request.getSenderWalletId())
                .orElseThrow(() -> new RuntimeException("Sender wallet not found"));

        WalletEntity receiver = walletRepository.findById(request.getReceiverWalletId())
                .orElseThrow(() -> new RuntimeException("Receiver wallet not found"));

        if(sender.getBalance() < request.getAmount()){
            throw new RuntimeException("Insufficient balance");
        }

        sender.setBalance(sender.getBalance() - request.getAmount());
        receiver.setBalance(receiver.getBalance() + request.getAmount());

        walletRepository.save(sender);
        walletRepository.save(receiver);

    } catch (ObjectOptimisticLockingFailureException e) {
        throw new RuntimeException("Transaction failed — wallet updated by another request. Try again.");
    }
}

    // ================= CHECK BALANCE =================
    public Double getBalance(Long walletId){

        WalletEntity wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        return wallet.getBalance();
    }
}