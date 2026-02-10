package com.star.share.relation.outbox;


import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.client.CanalConnectors;
import com.alibaba.otter.canal.protocol.CanalEntry;
import com.alibaba.otter.canal.protocol.Message;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.core.task.TaskExecutor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class CanalKafkaBridge implements SmartLifecycle {
    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final String host;
    private final int port;
    private final String destination;
    private final String username;
    private final String password;
    private final String filter;
    private final int batchSize;
    private final long intervalMs;
    private volatile boolean running;
    private final TaskExecutor taskExecutor;
    private CanalConnector connector;
    private static final Logger log = LoggerFactory.getLogger(CanalKafkaBridge.class);

    /**
     * Canal to Kafka bridge constructor.
     * @param kafka Kafka template for sending messages
     * @param objectMapper JSON object mapper for serializing messages
     * @param enabled Whether the Canal bridge is enabled
     * @param host Canal host
     * @param port Canal port
     * @param destination Canal destination (instance name)
     * @param username Canal username
     * @param password Canal password
     * @param filter Canal filter for selecting which database/table changes to capture
     * @param batchSize Number of messages to fetch in each batch from Canal
     * @param intervalMs Interval in milliseconds to wait before fetching the next batch of messages from Canal
     */
    public CanalKafkaBridge(KafkaTemplate<String, String> kafka,
                            ObjectMapper objectMapper,
                            @Qualifier("taskExecutor") TaskExecutor taskExecutor,
                            @Value("${canal.enabled}") boolean enabled,
                            @Value("${canal.host}") String host,
                            @Value("${canal.port}") int port,
                            @Value("${canal.destination}") String destination,
                            @Value("${canal.username}") String username,
                            @Value("${canal.password}") String password,
                            @Value("${canal.filter}") String filter,
                            @Value("${canal.batchSize}") int batchSize,
                            @Value("${canal.intervalMs}") long intervalMs) {
        this.kafka = kafka;
        this.objectMapper = objectMapper;
        this.taskExecutor = taskExecutor;
        this.enabled = enabled;
        this.host = host;
        this.port = port;
        this.destination = destination;
        this.username = username;
        this.password = password;
        this.filter = filter;
        this.batchSize = batchSize;
        this.intervalMs = intervalMs;
    }



}
