package fingenie.com.fingenie.service;

import fingenie.com.fingenie.entity.UserAchievement;
import fingenie.com.fingenie.dto.UserAchievementRequest;
import fingenie.com.fingenie.dto.UserAchievementResponse;
import fingenie.com.fingenie.repository.UserAchievementRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserAchievementService {

    private final UserAchievementRepository repository;

    public UserAchievementService(UserAchievementRepository repository) {
        this.repository = repository;
    }

    public UserAchievementResponse create(@NonNull UserAchievementRequest request) {
        UserAchievement ua = new UserAchievement();
        ua.setAccountId(request.getAccountId());
        ua.setAchievementId(request.getAchievementId());
        ua.setProgressValue(request.getProgressValue());
        ua.setIsUnlocked(request.getIsUnlocked());

        if (Boolean.TRUE.equals(request.getIsUnlocked())) {
            ua.setUnlockedAt(LocalDateTime.now());
        }

        return toResponse(repository.save(ua));
    }

    public List<UserAchievementResponse> getAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    public UserAchievementResponse getById(@NonNull Long id) {
        return repository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new RuntimeException("UserAchievement not found"));
    }

    public void delete(@NonNull Long id) {
        repository.deleteById(id);
    }

    private UserAchievementResponse toResponse(@NonNull UserAchievement ua) {
        UserAchievementResponse res = new UserAchievementResponse();
        res.setUserAchievementId(ua.getId());  // Changed from getUserAchievementId() to getId()
        res.setAccountId(ua.getAccountId());
        res.setAchievementId(ua.getAchievementId());
        res.setProgressValue(ua.getProgressValue());
        res.setIsUnlocked(ua.getIsUnlocked());
        res.setUnlockedAt(ua.getUnlockedAt());
        return res;
    }
}
