package fingenie.com.fingenie.service;

import fingenie.com.fingenie.entity.Streak;
import fingenie.com.fingenie.dto.StreakRequest;
import fingenie.com.fingenie.dto.StreakResponse;
import fingenie.com.fingenie.repository.StreakRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StreakService {

    private final StreakRepository repository;

    public StreakService(StreakRepository repository) {
        this.repository = repository;
    }

    public StreakResponse create(StreakRequest request) {
        Streak streak = new Streak();
        streak.setAccountId(request.getAccountId());
        streak.setCurrentStreak(request.getCurrentStreak());
        streak.setLongestStreak(request.getLongestStreak());
        streak.setLastActiveDate(request.getLastActiveDate());

        return toResponse(repository.save(streak));
    }

    public List<StreakResponse> getAll() {
        return repository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public StreakResponse getById(Long id) {
        return repository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new RuntimeException("Streak not found"));
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    private StreakResponse toResponse(Streak streak) {
        StreakResponse res = new StreakResponse();
        res.setStreakId(streak.getId());
        res.setAccountId(streak.getAccountId());
        res.setCurrentStreak(streak.getCurrentStreak());
        res.setLongestStreak(streak.getLongestStreak());
        res.setLastActiveDate(streak.getLastActiveDate());
        return res;
    }
}
