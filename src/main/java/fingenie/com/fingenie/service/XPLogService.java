package fingenie.com.fingenie.service;

import fingenie.com.fingenie.entity.XPLog;
import fingenie.com.fingenie.dto.XPLogRequest;
import fingenie.com.fingenie.dto.XPLogResponse;
import fingenie.com.fingenie.repository.XPLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for XP log management.
 * OSIV-SAFE: All public methods have explicit transaction boundaries.
 */
@Service
public class XPLogService {

    private final XPLogRepository repository;

    public XPLogService(XPLogRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public XPLogResponse create(XPLogRequest request) {
        XPLog xpLog = new XPLog();
        xpLog.setAccountId(request.getAccountId());
        xpLog.setSourceType(request.getSourceType());
        xpLog.setSourceId(request.getSourceId());
        xpLog.setXpAmount(request.getXpAmount());
        xpLog.setDescription(request.getDescription());

        XPLog saved = repository.save(xpLog);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<XPLogResponse> getAll() {
        return repository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public XPLogResponse getById(Long id) {
        XPLog xpLog = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("XPLog not found"));
        return toResponse(xpLog);
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }

    private XPLogResponse toResponse(XPLog xpLog) {
        XPLogResponse res = new XPLogResponse();
        res.setXpLogId(xpLog.getId());  // Changed from getXpLogId() to getId()
        res.setAccountId(xpLog.getAccountId());
        res.setSourceType(xpLog.getSourceType());
        res.setSourceId(xpLog.getSourceId());
        res.setXpAmount(xpLog.getXpAmount());
        res.setDescription(xpLog.getDescription());
        res.setCreatedAt(xpLog.getCreatedAt() != null ? xpLog.getCreatedAt().toLocalDateTime() : null);
        return res;
    }
}
