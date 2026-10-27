package com.itnoduck.acmate.common.cache;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.itnoduck.acmate.discussion.entity.Post;
import com.itnoduck.acmate.discussion.mapper.PostMapper;
import com.itnoduck.acmate.discussion.service.DiscussionService;
import com.itnoduck.acmate.leaderboard.service.LeaderboardService;
import com.itnoduck.acmate.problem.dto.ProblemQueryRequest;
import com.itnoduck.acmate.problem.entity.Problem;
import com.itnoduck.acmate.problem.mapper.ProblemMapper;
import com.itnoduck.acmate.problem.service.ProblemQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/** Preloads the public first-page responses most often requested by the home page. */
@Component
public class PublicReadCacheWarmup {

    private static final Logger log = LoggerFactory.getLogger(PublicReadCacheWarmup.class);
    private static final List<String> POST_TYPES = List.of(
            "", "SOLUTION", "QUESTION", "CONTEST_SUMMARY", "TRAINING_EXPERIENCE", "ANNOUNCEMENT", "OTHER");

    private final DiscussionService discussions;
    private final ProblemQueryService problems;
    private final LeaderboardService leaderboard;
    private final RedisJsonCache redisCache;
    private final RedisIdBloomFilter idBloomFilter;
    private final PostMapper postMapper;
    private final ProblemMapper problemMapper;

    public PublicReadCacheWarmup(DiscussionService discussions, ProblemQueryService problems,
                                 LeaderboardService leaderboard, RedisJsonCache redisCache,
                                 RedisIdBloomFilter idBloomFilter, PostMapper postMapper,
                                 ProblemMapper problemMapper) {
        this.discussions = discussions;
        this.problems = problems;
        this.leaderboard = leaderboard;
        this.redisCache = redisCache;
        this.idBloomFilter = idBloomFilter;
        this.postMapper = postMapper;
        this.problemMapper = problemMapper;
    }

    // Broad cache and Bloom prewarming disabled; public reads use MySQL.
    public void warmPublicReadCache() {
        if (!redisCache.isAvailable()) {
            log.warn("Redis is unavailable or authentication failed; public data will use MySQL until Redis is configured");
            return;
        }
        try {
            boolean postsReady = idBloomFilter.ensureInitialized("posts", maxPostId(), this::postIdsAfter);
            boolean problemsReady = idBloomFilter.ensureInitialized("problems", maxProblemId(), this::problemIdsAfter);
            if (!postsReady || !problemsReady) {
                log.warn("Redis ID Bloom filter initialization did not complete; detail lookups will fall back to MySQL");
            }

            for (String postType : POST_TYPES) {
                discussions.listPosts(postType, null, null, 1, 5);
                discussions.countPosts(postType, null, null);
            }
            discussions.listPosts(null, null, null, 1, 20);

            ProblemQueryRequest problemRequest = new ProblemQueryRequest();
            problemRequest.setPage(1);
            problemRequest.setSize(20);
            problems.listProblems(problemRequest);

            for (String period : List.of("total", "7d", "30d")) {
                leaderboard.getLeaderboard(period, null, 1, 20);
                leaderboard.countLeaderboard(period);
            }
            log.info("Preloaded first-page public discussion, problem, and leaderboard data into Redis");
        } catch (RuntimeException ex) {
            // Cache warmup must not interfere with a healthy application startup.
            log.warn("Public Redis cache warmup did not complete; requests will populate caches on demand", ex);
        }
    }

    private long maxPostId() {
        Object value = postMapper.selectObjs(new QueryWrapper<Post>().select("COALESCE(MAX(id), 0)")).stream()
                .findFirst().orElse(0L);
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private long maxProblemId() {
        Object value = problemMapper.selectObjs(new QueryWrapper<Problem>().select("COALESCE(MAX(id), 0)")).stream()
                .findFirst().orElse(0L);
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private List<Long> postIdsAfter(long id) {
        return postMapper.selectObjs(new QueryWrapper<Post>().select("id").gt("id", id).orderByAsc("id")
                        .last("LIMIT 1000"))
                .stream().filter(Number.class::isInstance).map(Number.class::cast).map(Number::longValue).toList();
    }

    private List<Long> problemIdsAfter(long id) {
        return problemMapper.selectObjs(new QueryWrapper<Problem>().select("id").gt("id", id).orderByAsc("id")
                        .last("LIMIT 1000"))
                .stream().filter(Number.class::isInstance).map(Number.class::cast).map(Number::longValue).toList();
    }
}
