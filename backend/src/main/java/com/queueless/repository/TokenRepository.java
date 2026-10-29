package com.queueless.repository;

import com.queueless.model.Token;
import com.queueless.model.TokenStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TokenRepository extends JpaRepository<Token, Long> {

    @Query("select max(t.tokenNumber) from Token t where t.service.id = :serviceId and t.queueDate = :day")
    Integer findMaxTokenNumber(@Param("serviceId") Long serviceId, @Param("day") LocalDate day);

    long countByService_IdAndQueueDateAndStatus(Long serviceId, LocalDate queueDate, TokenStatus status);

    Optional<Token> findFirstByService_IdAndQueueDateAndStatusOrderByTokenNumberDesc(
            Long serviceId, LocalDate queueDate, TokenStatus status);

    boolean existsByUser_IdAndService_IdAndQueueDateAndStatusIn(
            Long userId, Long serviceId, LocalDate queueDate, Collection<TokenStatus> statuses);

    /** Tokens in one state for one counter and day, in queue order, with the student loaded. */
    @Query("select t from Token t join fetch t.user "
            + "where t.service.id = :serviceId and t.queueDate = :day and t.status = :status "
            + "order by t.tokenNumber asc")
    List<Token> findQueue(@Param("serviceId") Long serviceId,
                          @Param("day") LocalDate day,
                          @Param("status") TokenStatus status);

    @Query("select count(t) from Token t "
            + "where t.service.id = :serviceId and t.queueDate = :day "
            + "and t.status = com.queueless.model.TokenStatus.WAITING and t.tokenNumber < :number")
    long countWaitingBefore(@Param("serviceId") Long serviceId,
                            @Param("day") LocalDate day,
                            @Param("number") int number);

    @Query("select t from Token t join fetch t.service where t.user.id = :userId order by t.createdTime desc")
    List<Token> findForUser(@Param("userId") Long userId, Pageable pageable);

    @Query("select t from Token t join fetch t.service join fetch t.user where t.id = :id")
    Optional<Token> findDetailed(@Param("id") Long id);

    /** Tokens taken per hour of the day for one counter since a date. Rows are [hour, count]. */
    @Query("select extract(hour from t.createdTime), count(t) from Token t "
            + "where t.service.id = :serviceId and t.createdTime >= :since "
            + "group by extract(hour from t.createdTime)")
    List<Object[]> countByHour(@Param("serviceId") Long serviceId, @Param("since") LocalDateTime since);

    /** One row: [average seconds at the counter, number of samples]. */
    @Query("select avg(t.durationSeconds), count(t.durationSeconds) from Token t "
            + "where t.service.id = :serviceId "
            + "and t.status = com.queueless.model.TokenStatus.COMPLETED "
            + "and t.completedTime >= :since and t.durationSeconds is not null")
    List<Object[]> durationStats(@Param("serviceId") Long serviceId, @Param("since") LocalDateTime since);

    /** Anything still waiting or being served from an earlier day can no longer be honoured. */
    @Modifying
    @Query("update Token t set t.status = com.queueless.model.TokenStatus.CANCELLED "
            + "where t.queueDate < :today and t.status in "
            + "(com.queueless.model.TokenStatus.WAITING, com.queueless.model.TokenStatus.SERVING)")
    int expireOlderThan(@Param("today") LocalDate today);
}
