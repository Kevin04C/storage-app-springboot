package com.stonestorage.storage.domain.entity;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class FileNode {

    private final String name;
    private final NodeType type;
    private final long size;
    private final Instant lastModified;
    private final String thumbnailUrl;

    public enum NodeType {
        FILE, FOLDER
    }
}
