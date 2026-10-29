package com.queueless.repository;

import com.queueless.model.CampusService;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CampusServiceRepository extends JpaRepository<CampusService, Long> {

    List<CampusService> findByActiveTrueOrderByServiceNameAsc();

    List<CampusService> findAllByOrderByServiceNameAsc();

    boolean existsByServiceNameIgnoreCase(String serviceName);

    /**
     * Locks the service row so two students (or an admin and a student) cannot change the
     * same queue at the same time. This is what keeps token numbers unique.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from CampusService s where s.id = :id")
    Optional<CampusService> findByIdForUpdate(@Param("id") Long id);
}
