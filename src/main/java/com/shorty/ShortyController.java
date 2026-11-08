package com.shorty;

import java.net.URI;
import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
public class ShortyController {

    private static final String ALPHABET = "abcdefghijkmnpqrstuvwxyz23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final LinkRepository repo;

    public ShortyController(LinkRepository repo) {
        this.repo = repo;
    }

    public record ShortenRequest(String url) {}

    public record LinkView(String code, String shortUrl, String url) {}

    @PostMapping("/api/shorten")
    public LinkView shorten(@RequestBody ShortenRequest req) {
        String url = clean(req.url());
        if (url == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please enter a valid http or https link.");
        }
        String code;
        do {
            code = newCode();
        } while (repo.existsById(code));
        repo.save(new Link(code, url));
        return view(new Link(code, url));
    }

    @GetMapping("/api/links")
    public List<LinkView> recent() {
        return repo.findTop10ByOrderByCreatedAtDesc().stream().map(this::view).toList();
    }

    @GetMapping("/{code:[a-z2-9]{6}}")
    public ResponseEntity<Void> open(@PathVariable String code) {
        return repo.findById(code)
                .map(l -> ResponseEntity.status(HttpStatus.FOUND).location(URI.create(l.getUrl())).<Void>build())
                .orElse(ResponseEntity.notFound().build());
    }

    private LinkView view(Link l) {
        String shortUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/" + l.getCode()).toUriString();
        return new LinkView(l.getCode(), shortUrl, l.getUrl());
    }

    private static String newCode() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    private static String clean(String raw) {
        if (raw == null) return null;
        String u = raw.trim();
        if (u.isEmpty() || u.length() > 2048) return null;
        if (!u.matches("^[a-zA-Z][a-zA-Z0-9+.-]*:.*")) u = "https://" + u;
        try {
            URI uri = new URI(u);
            String scheme = uri.getScheme();
            boolean ok = ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                    && uri.getHost() != null;
            return ok ? uri.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
