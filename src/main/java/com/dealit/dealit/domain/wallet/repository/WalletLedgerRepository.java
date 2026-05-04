package com.dealit.dealit.domain.wallet.repository;

import com.dealit.dealit.domain.wallet.entity.WalletLedger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletLedgerRepository extends JpaRepository<WalletLedger, Long> {

	Page<WalletLedger> findAllByMemberId(Long memberId, Pageable pageable);
}
