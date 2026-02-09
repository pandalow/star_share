package com.star.share.counter.api;

import com.star.share.counter.entity.CountsResponse;
import com.star.share.counter.schema.CounterSchema;
import com.star.share.counter.service.CounterService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/v1/counter")
public class CounterController {
    private final CounterService counterService;
    public CounterController(CounterService counterService) {
        this.counterService = counterService;
    }

    @GetMapping("/{etype}/{eid}")
    public ResponseEntity<CountsResponse> getCounts(
            @PathVariable("etype") String entityType,
            @PathVariable("eid") String entityId,
            @RequestParam(value = "metrics", required = false) String metricsStr
    ){
        List<String> metrics;
        if(metricsStr == null || metricsStr.isBlank()){
            metrics = new ArrayList<>(CounterSchema.SUPPORTED_COUNTERS);
        }else{
            metrics = Arrays.stream(metricsStr.split(","))
                    .map(String::trim)
                    .filter(CounterSchema.SUPPORTED_COUNTERS::contains)
                    .toList();
        }
        Map<String, Long> counts = counterService.getCounts(entityType, entityId, metrics);

        return ResponseEntity.ok(new CountsResponse(entityType, entityId, counts));
    }
}
