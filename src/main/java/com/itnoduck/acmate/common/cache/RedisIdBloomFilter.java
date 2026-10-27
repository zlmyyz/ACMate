package com.itnoduck.acmate.common.cache;

import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.LongFunction;

/** Redis bitmap Bloom filters for persisted auto-increment IDs. */
@Component
public class RedisIdBloomFilter {

    private static final long BIT_MODULUS = (1L << 24) - 1;
    private static final int HASH_COUNT = 7;
    private static final int ID_BATCH_SIZE = 1000;
    private static final DefaultRedisScript<Long> MIGHT_CONTAIN_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('GET', KEYS[1]) ~= '1' then return 1 end; " +
                    "local maxId = tonumber(redis.call('GET', KEYS[2]) or '-1'); " +
                    "if maxId < 0 or tonumber(ARGV[1]) > maxId then return 1 end; " +
                    "if redis.call('EXISTS', KEYS[3]) == 0 then return 1 end; " +
                    "for i = 2, #ARGV do " +
                    "if redis.call('GETBIT', KEYS[3], tonumber(ARGV[i])) == 0 then return 0 end; " +
                    "end; return 1", Long.class);

    private final StringRedisTemplate redis;

    public RedisIdBloomFilter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /**
     * Builds the filter on first boot and incrementally indexes newly allocated IDs on later boots.
     * IDs above the indexed maximum always pass through to MySQL until the next rebuild.
     */
    public boolean ensureInitialized(String namespace, long databaseMaxId,
                                     LongFunction<? extends Collection<Long>> idsAfter) {
        String bitmapKey = bitmapKey(namespace);
        String readyKey = readyKey(namespace);
        String maxIdKey = maxIdKey(namespace);
        try {
            boolean ready = "1".equals(redis.opsForValue().get(readyKey));
            Boolean bitmapExists = redis.hasKey(bitmapKey);
            String indexedText = redis.opsForValue().get(maxIdKey);
            long indexedMax = indexedText == null ? -1 : Long.parseLong(indexedText);
            boolean rebuild = !ready || !Boolean.TRUE.equals(bitmapExists) || indexedMax < 0
                    || databaseMaxId < indexedMax;

            if (rebuild) {
                redis.delete(readyKey);
                redis.delete(bitmapKey);
                redis.delete(maxIdKey);
                redis.opsForValue().setBit(bitmapKey, 0, true); // Reserve bit zero as an empty-filter marker.
                indexRows(bitmapKey, namespace, 0, databaseMaxId, idsAfter);
                redis.opsForValue().set(maxIdKey, Long.toString(databaseMaxId));
                redis.opsForValue().set(readyKey, "1");
                return true;
            }

            if (databaseMaxId > indexedMax) {
                indexRows(bitmapKey, namespace, indexedMax, databaseMaxId, idsAfter);
                // Publish the new maximum only after all new IDs have reached the bitmap.
                redis.opsForValue().set(maxIdKey, Long.toString(databaseMaxId));
            }
            return true;
        } catch (Exception ignored) {
            // If initialization fails, the Lua membership check fails open to MySQL.
            try { redis.delete(readyKey); } catch (Exception ignoredAgain) { }
            return false;
        }
    }

    private void indexRows(String bitmapKey, String namespace, long afterId, long maximumId,
                           LongFunction<? extends Collection<Long>> idsAfter) {
        long cursor = afterId;
        while (cursor < maximumId) {
            Collection<Long> batch = idsAfter.apply(cursor);
            if (batch == null || batch.isEmpty()) return;
            addAll(bitmapKey, namespace, batch);
            long currentCursor = cursor;
            long nextCursor = batch.stream().filter(id -> id != null && id > currentCursor)
                    .mapToLong(Long::longValue).max().orElse(cursor);
            if (nextCursor <= cursor) return;
            cursor = nextCursor;
        }
    }

    /** Returns false only when the filter proves that a positive ID was not in the indexed database. */
    public boolean mightContain(String namespace, long id) {
        if (id <= 0) return false;
        try {
            List<Object> arguments = new ArrayList<>(HASH_COUNT + 1);
            arguments.add(Long.toString(id));
            for (long bit : bitPositions(namespace, id)) arguments.add(Long.toString(bit));
            Long result = redis.execute(MIGHT_CONTAIN_SCRIPT,
                    List.of(readyKey(namespace), maxIdKey(namespace), bitmapKey(namespace)), arguments.toArray());
            return !Long.valueOf(0).equals(result);
        } catch (Exception ignored) {
            return true;
        }
    }

    private void addAll(String bitmapKey, String namespace, Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) return;
        byte[] rawKey = redis.getStringSerializer().serialize(bitmapKey);
        List<Long> batch = new ArrayList<>(ID_BATCH_SIZE);
        for (Long id : ids) {
            if (id == null || id <= 0) continue;
            batch.add(id);
            if (batch.size() == ID_BATCH_SIZE) {
                addBatch(rawKey, namespace, batch);
                batch.clear();
            }
        }
        if (!batch.isEmpty()) addBatch(rawKey, namespace, batch);
    }

    private void addBatch(byte[] rawKey, String namespace, List<Long> ids) {
        redis.executePipelined((RedisCallback<Object>) connection -> {
            for (long id : ids) {
                for (long bit : bitPositions(namespace, id)) connection.setBit(rawKey, bit, true);
            }
            return null;
        });
    }

    private long[] bitPositions(String namespace, long id) {
        byte[] digest;
        try {
            digest = MessageDigest.getInstance("SHA-256")
                    .digest((namespace + ":" + id).getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
        ByteBuffer hash = ByteBuffer.wrap(digest);
        long first = hash.getLong();
        long second = hash.getLong() | 1L;
        long[] positions = new long[HASH_COUNT];
        for (int i = 0; i < HASH_COUNT; i++) {
            long combined = first + (long) i * second;
            positions[i] = 1 + Long.remainderUnsigned(combined, BIT_MODULUS);
        }
        return positions;
    }

    private String bitmapKey(String namespace) { return "acmate:bloom:" + namespace + ":v1:bits"; }
    private String readyKey(String namespace) { return "acmate:bloom:" + namespace + ":v1:ready"; }
    private String maxIdKey(String namespace) { return "acmate:bloom:" + namespace + ":v1:max-id"; }
}
