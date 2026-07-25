package com.omniscribe.services;

import com.omniscribe.models.ChunkMessage;

public interface WorkerService {

    void processJobMessage(ChunkMessage message);
}
