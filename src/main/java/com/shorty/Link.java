package com.shorty;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.Instant;

@Entity
public class Link {

    @Id
    private String code;

    @Column(length = 2048, nullable = false)
    private String url;

    private Instant createdAt = Instant.now();

    protected Link() {}

    public Link(String code, String url) {
        this.code = code;
        this.url = url;
    }

    public String getCode() { return code; }
    public String getUrl() { return url; }
    public Instant getCreatedAt() { return createdAt; }
}
