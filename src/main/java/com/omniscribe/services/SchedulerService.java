package com.omniscribe.services;

import com.omniscribe.models.ChunkMessage;

public interface SchedulerService {

    void processIngressMessage(ChunkMessage message);
}
