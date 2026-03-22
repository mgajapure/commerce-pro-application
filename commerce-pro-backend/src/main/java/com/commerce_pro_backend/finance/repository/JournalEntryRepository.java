package com.commerce_pro_backend.finance.repository;

import com.commerce_pro_backend.finance.entity.JournalEntry;
import com.commerce_pro_backend.finance.enums.JournalEntryType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface JournalEntryRepository extends JpaRepository<JournalEntry, String>, JpaSpecificationExecutor<JournalEntry> {
    Optional<JournalEntry> findByEntryRef(String ref);
    Page<JournalEntry> findByPeriodId(String periodId, Pageable pageable);
    List<JournalEntry> findBySourceTypeAndSourceId(String sourceType, String sourceId);
    Page<JournalEntry> findByEntryType(JournalEntryType type, Pageable pageable);

    @Query("SELECT j FROM JournalEntry j WHERE j.entryDate BETWEEN :from AND :to AND j.status = 'POSTED'")
    List<JournalEntry> findPostedInRange(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
