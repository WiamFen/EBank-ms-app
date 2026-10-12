package net.wiam.ebankservice.service;

import net.wiam.ebankservice.entities.BankAccount;
import net.wiam.ebankservice.repositories.BankAccountRepository;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class EbankService {
    private BankAccountRepository bankAccountRepository;

    public EbankService(BankAccountRepository bankAccountRepository) {
        this.bankAccountRepository = bankAccountRepository;
    }

    public List<BankAccount> getAllBankAccounts() {
        return bankAccountRepository.findAll();
    }

    public BankAccount getBankAccountById(String id) {
        return bankAccountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Account  not found"));
    }

    public BankAccount save(BankAccount bankAccount) {
        bankAccount.setId(UUID.randomUUID().toString());
        bankAccount.setCreatedAt(new Date());
        return bankAccountRepository.save(bankAccount);
    }
}
