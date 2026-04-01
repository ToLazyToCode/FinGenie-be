package fingenie.com.fingenie.service;

import fingenie.com.fingenie.entity.FriendStreak;
import fingenie.com.fingenie.dto.FriendStreakRequest;
import fingenie.com.fingenie.dto.FriendStreakResponse;
import fingenie.com.fingenie.repository.FriendStreakRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class FriendStreakService {

    private final FriendStreakRepository repository;

    public FriendStreakService(FriendStreakRepository repository) {
        this.repository = repository;
    }

    public FriendStreakResponse create(@NonNull FriendStreakRequest request) {
        FriendStreak fs = new FriendStreak();
        fs.setFriendshipId(request.getFriendshipId());
        fs.setCurrentStreak(request.getCurrentStreak());
        fs.setLastActiveDate(LocalDate.now());

        return toResponse(repository.save(fs));
    }

    public List<FriendStreakResponse> getAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    public FriendStreakResponse getById(@NonNull Long id) {
        return repository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new RuntimeException("FriendStreak not found"));
    }

    public void delete(@NonNull Long id) {
        repository.deleteById(id);
    }

    private FriendStreakResponse toResponse(@NonNull FriendStreak fs) {
        FriendStreakResponse res = new FriendStreakResponse();
        res.setFriendStreakId(fs.getId());  // Changed from getFriendStreakId() to getId()
        res.setFriendshipId(fs.getFriendshipId());
        res.setCurrentStreak(fs.getCurrentStreak());
        res.setLastActiveDate(fs.getLastActiveDate());
        return res;
    }
}
