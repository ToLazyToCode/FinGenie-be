package fingenie.com.fingenie.service;

import fingenie.com.fingenie.entity.UserLevel;
import fingenie.com.fingenie.dto.UserLevelRequest;
import fingenie.com.fingenie.dto.UserLevelResponse;
import fingenie.com.fingenie.repository.UserLevelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for user level management.
 * OSIV-SAFE: All public methods have explicit transaction boundaries.
 */
@Service
public class UserLevelService {

    private final UserLevelRepository repository;

    public UserLevelService(UserLevelRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public UserLevelResponse create(UserLevelRequest request) {
        UserLevel level = repository.findByAccountId(request.getAccountId())
                .orElseGet(UserLevel::new);

        level.setAccountId(request.getAccountId());
        level.setCurrentLevel(request.getCurrentLevel());
        level.setCurrentXp(request.getCurrentXp());
        level.setLifetimeXp(request.getLifetimeXp());

        UserLevel saved = repository.save(level);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<UserLevelResponse> getAll() {
        return repository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserLevelResponse getById(Long accountId) {
        UserLevel level = repository.findByAccountId(accountId)
                .orElseThrow(() -> new RuntimeException("UserLevel not found"));
        return toResponse(level);
    }

    @Transactional
    public void delete(Long accountId) {
        repository.deleteByAccountId(accountId);
    }

    private UserLevelResponse toResponse(UserLevel level) {
        UserLevelResponse res = new UserLevelResponse();
        res.setAccountId(level.getAccountId());
        res.setCurrentLevel(level.getCurrentLevel());
        res.setCurrentXp(level.getCurrentXp());
        res.setLifetimeXp(level.getLifetimeXp());
        res.setUpdatedAt(level.getUpdatedAt() != null ? level.getUpdatedAt().toLocalDateTime() : null);
        return res;
    }
}
