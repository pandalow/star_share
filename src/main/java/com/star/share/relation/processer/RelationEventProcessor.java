package com.star.share.relation.processer;

import com.star.share.relation.mapper.RelationMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RelationEventProcessor {
    private final RelationMapper mapper;
    private final StringRedisTemplate redis;


}
