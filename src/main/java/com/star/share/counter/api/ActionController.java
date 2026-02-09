package com.star.share.counter.api;

import com.star.share.auth.token.JwtService;
import com.star.share.counter.entity.ActionRequest;
import com.star.share.counter.service.CounterService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/action")
public class ActionController {
    private final CounterService counterService;
    private final JwtService jwtService;

    public ActionController(CounterService counterService, JwtService jwtService) {
        this.counterService = counterService;
        this.jwtService = jwtService;
    }

    /**
     * like operation for entity,
     * @param request the request body contains the entity type and entity id
     * @param jwt the jwt token contains the user id
     * @return a map contains the changed status and the current liked status
     */
    @PostMapping("/like")
    public ResponseEntity<Map<String, Object>> like (@Valid @RequestBody ActionRequest request,
                                                     @AuthenticationPrincipal Jwt jwt){
        long uid = jwtService.extractUserId(jwt);
        boolean changed = counterService.like(request.getEntityType(),
                request.getEntityId(),uid);
        return ResponseEntity.ok(Map.of(
                "changed", changed,
                "liked", counterService.isLiked(request.getEntityType(),request.getEntityId(), uid)
        ));
    }

    /**
     * unlike operation for entity
     */
    @PostMapping("/unlike")
    public ResponseEntity<Map<String, Object>> unlike(@Valid @RequestBody ActionRequest request,
                                                      @AuthenticationPrincipal Jwt jwt){
        long uid = jwtService.extractUserId(jwt);
        boolean changed = counterService.unlike(request.getEntityType(),
                request.getEntityId(),uid);
        return ResponseEntity.ok(Map.of(
                "changed", changed,
                "liked", counterService.isLiked(request.getEntityType(),request.getEntityId(), uid)
        ));
    }

    @PostMapping("/fav")
    public ResponseEntity<Map<String, Object>> fav(@Valid @RequestBody ActionRequest request,
                                                   @AuthenticationPrincipal Jwt jwt){
        long uid = jwtService.extractUserId(jwt);
        boolean changed = counterService.fav(request.getEntityType(),
                request.getEntityId(),uid);
        return ResponseEntity.ok(Map.of(
                "changed", changed,
                "faved", counterService.isFaved(request.getEntityType(),request.getEntityId(), uid)
        ));
    }

    @PostMapping("/unfav")
    public ResponseEntity<Map<String, Object>> unfav(@Valid @RequestBody ActionRequest request,
                                                     @AuthenticationPrincipal Jwt jwt){
        long uid = jwtService.extractUserId(jwt);
        boolean changed = counterService.unfav(request.getEntityType(),
                request.getEntityId(),uid);
        return ResponseEntity.ok(Map.of(
                "changed", changed,
                "faved", counterService.isFaved(request.getEntityType(),request.getEntityId(), uid)
        ));
    }



}
