package fingenie.com.fingenie.repository;

import fingenie.com.fingenie.entity.UserSpendingRoutine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserSpendingRoutineRepository extends JpaRepository<UserSpendingRoutine, Long> {
    List<UserSpendingRoutine> findByUserIdAndIsActiveTrue(Long userId);
    
    List<UserSpendingRoutine> findByUserIdAndRoutineTypeAndIsActiveTrue(Long userId, String routineType);
    
    Optional<UserSpendingRoutine> findByUserIdAndCategoryIdAndIsActiveTrue(Long userId, Long categoryId);
    
    @Query("SELECT usr FROM UserSpendingRoutine usr WHERE usr.userId = :userId AND usr.isActive = true ORDER BY usr.confidenceScore DESC")
    List<UserSpendingRoutine> findActiveRoutinesByConfidence(@Param("userId") Long userId);
    
    @Query("SELECT usr FROM UserSpendingRoutine usr WHERE usr.userId = :userId AND usr.routineType = :routineType AND usr.isActive = true ORDER BY usr.frequencyCount DESC")
    List<UserSpendingRoutine> findMostFrequentRoutines(@Param("userId") Long userId, @Param("routineType") String routineType);
}
