package com.itnoduck.acmate.oj.client;

import com.itnoduck.acmate.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.List;

@Component
public class CodeforcesApiClient {

    private static final Logger log = LoggerFactory.getLogger(CodeforcesApiClient.class);
    private static final String API_BASE = "https://codeforces.com/api";
    private static final TypeReference<CodeforcesApiResponse<CodeforcesSubmissionDto>> SUBMISSION_TYPE =
            new TypeReference<>() {};

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public CodeforcesApiClient(RestClient restClient, ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    public List<CodeforcesSubmissionDto> fetchSubmissions(String handle, int from, int count) {
        String url = API_BASE + "/user.status?handle=" + handle +
                "&from=" + from + "&count=" + count;
        log.debug("CF API request: handle={} from={} count={}", handle, from, count);

        String raw;
        try {
            raw = restClient.get().uri(url).retrieve().body(String.class);
        } catch (RestClientException e) {
            log.warn("CF API unreachable for handle={}: {}", handle, e.getMessage());
            throw new BusinessException(503, "Codeforces 服务暂时不可达，请稍后重试");
        }

        CodeforcesApiResponse<CodeforcesSubmissionDto> resp;
        try {
            resp = objectMapper.readValue(raw, SUBMISSION_TYPE);
        } catch (JacksonException e) {
            log.warn("CF API unparseable response for handle={}", handle);
            throw new BusinessException(502, "Codeforces 返回了无法解析的数据");
        }

        if ("FAILED".equals(resp.getStatus())) {
            String comment = resp.getComment() != null ? resp.getComment() : "";
            if (comment.toLowerCase().contains("not found")) {
                throw new BusinessException(404, "Codeforces 账号 " + handle + " 不存在");
            }
            if (comment.toLowerCase().contains("limit") || comment.toLowerCase().contains("too many")) {
                throw new BusinessException(429, "Codeforces API 请求频率过高，请稍后重试");
            }
            throw new BusinessException(502, "Codeforces API 错误: " + comment);
        }

        if (resp.getResult() == null) return Collections.emptyList();
        return resp.getResult();
    }
}
