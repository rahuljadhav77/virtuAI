package com.virtualization.service;

import com.virtualization.entity.TrafficLogEntity;
import com.virtualization.model.VirtualRequest;
import com.virtualization.model.VirtualResponse;
import com.virtualization.repository.TrafficLogRepository;
import org.springframework.stereotype.Service;

@Service
public class RecorderService {
    private final TrafficLogRepository repository;
    private boolean recordingEnabled = false;

    public RecorderService(TrafficLogRepository repository) {
        this.repository = repository;
    }

    public void setRecordingEnabled(boolean enabled) {
        this.recordingEnabled = enabled;
    }

    public boolean isRecordingEnabled() {
        return recordingEnabled;
    }

    public void logTraffic(VirtualRequest request, VirtualResponse response) {
        if (!recordingEnabled) return;

        TrafficLogEntity log = new TrafficLogEntity();
        log.setMethod(request.getMethod());
        log.setPath(request.getPath());
        log.setRequestBody(request.getBody());
        log.setResponseBody(response.getBody());
        log.setStatusCode(response.getStatusCode());
        log.setServiceId(response.getServiceId());
        repository.save(log);
    }
}
