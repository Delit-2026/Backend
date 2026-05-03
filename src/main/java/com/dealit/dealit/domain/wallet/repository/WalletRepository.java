package com.dealit.dealit.domain.wallet.repository;

import com.dealit.dealit.domain.wallet.entity.Wallet;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface WalletRepository extends JpaRepository<Wallet, Long> {

	Optional<Wallet> findByMemberId(Long memberId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<Wallet> findWithLockByMemberId(Long memberId);
}
