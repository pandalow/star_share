package com.star.share.relation.outbox;

import com.star.share.relation.entity.RelationEvent;
import org.springframework.kafka.support.Acknowledgment;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.star.share.common.util.OutboxMessageUtil;
import com.star.share.relation.processer.RelationEventProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CanalOutboxConsumer {
    private final ObjectMapper objectMapper;
    private final RelationEventProcessor processor;

    /**
     * Consume message from Canal outbox topic,
     * Listen Canal -> Kafka -> Relation Service
     * Using manual acknowledgment to ensure at-least-once processing semantics.
     * @param message 
     * @param ack
     */
    @KafkaListener(topics = OutboxTopics.CANAL_OUTBOX, groupId = "relation-outbox-consumer")
    public void onMessage(String message, Acknowledgment ack){
        try{
            List<JsonNode> rows = OutboxMessageUtil.extractRows(objectMapper, message);
            if(rows.isEmpty()){
                ack.acknowledge();
                return;
            }

            for(JsonNode row: rows){
                JsonNode payloadNode = row.get("payload");
                if(payloadNode == null){
                    continue;
                }

                RelationEvent event = objectMapper.readValue(payloadNode.asText(), RelationEvent.class);
                processor.process(event);
            }

            ack.acknowledge();
        }catch (Exception e){
            // Ignored exception
        }
    }

}
