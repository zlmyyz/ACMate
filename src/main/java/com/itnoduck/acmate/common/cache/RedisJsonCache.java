package com.itnoduck.acmate.common.cache;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/** Small JSON cache for public read models. Redis failures fall through to the database. */
@Component
public class RedisJsonCache {

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public RedisJsonCache(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    public boolean isAvailable() {
        try {
            var connection = redis.getConnectionFactory().getConnection();
            try { return "PONG".equalsIgnoreCase(connection.ping()); }
            finally { connection.close(); }
        } catch (Exception ignored) {
            return false;
        }
    }

    public <T> T get(String key, TypeReference<T> type) {
        try {
            String json = redis.opsForValue().get(key);
            if (json == null) return null;
            return objectMapper.readValue(json, type);
        } catch (Exception ignored) {
            return null;
        }
    }

    public void put(String key, Object value, Duration ttl) {
        try {
            redis.opsForValue().set(key, objectMapper.writeValueAsString(value), ttl);
        } catch (Exception ignored) {
            // This cache is an optimization; the database remains authoritative.
        }
    }

    /** Cache-aside: expiry is fixed at write time; Redis failure falls back to MySQL. */
    public <T> T getOrLoad(String key, TypeReference<T> type, Duration ttl, Supplier<T> loader) {
        T cached = get(key, type);
        if (cached != null) return cached;
        T loaded = loader.get();
        if (loaded != null) put(key, loaded, ttl);
        return loaded;
    }

    public static String keyFor(String namespace, String material) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(material.getBytes(StandardCharsets.UTF_8));
            return namespace + ":" + HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
