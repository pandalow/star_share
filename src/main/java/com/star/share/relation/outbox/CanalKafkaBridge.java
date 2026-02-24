package com.star.share.relation.outbox;


import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.client.CanalConnectors;
import com.alibaba.otter.canal.protocol.CanalEntry;
import com.alibaba.otter.canal.protocol.Message;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.core.task.TaskExecutor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.net.InetSocketAddress;

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
     *
     * @param kafka        Kafka template for sending messages
     * @param objectMapper JSON object mapper for serializing messages
     * @param enabled      Whether the Canal bridge is enabled
     * @param host         Canal host
     * @param port         Canal port
     * @param destination  Canal destination (instance name)
     * @param username     Canal username
     * @param password     Canal password
     * @param filter       Canal filter for selecting which database/table changes to capture
     * @param batchSize    Number of messages to fetch in each batch from Canal
     * @param intervalMs   Interval in milliseconds to wait before fetching the next batch of messages from Canal
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

    /**
     * Start the Canal to Kafka bridge. This method will create a Canal connector, connect to the Canal server,
     */
    @Override
    public void start() {
        if (running) {
            log.info("Canal bridge start skipped because it's already running");
            return;
        }
        running = true;

        taskExecutor.execute(() -> {
            try {
                // Create and connect the Canal connector (SingleConnector for standalone mode)
                connector = CanalConnectors.newSingleConnector(
                        new InetSocketAddress(host, port), destination, username, password
                );
                log.info("Connecting to Canal server: host = {} port = {} destination = {}", host, port, destination);

                // Connect to Canal and subscribe to the specified filter (e.g., "db\\.table")
                connector.connect();
                connector.subscribe(filter);
                // Roll back any previous unacknowledged messages to start fresh
                connector.rollback();
                log.info("Canal connector connected and subscribed: dest = {} filter = {}", destination, filter);

                // Main loop to fetch messages from Canal and send them to Kafka
                while (running) {
                    // Fetch a batch of messages from Canal without acknowledging them yet
                    Message message = connector.getWithoutAck(batchSize); // Fetch a batch of messages from Canal
                    long batchId = message.getId();

                    if (batchId == -1 || message.getEntries() == null || message.getEntries().isEmpty()) {
                        try {
                            Thread.sleep(intervalMs); // Sleep for the configured interval before fetching the next batch
                        } catch (InterruptedException e) {
                            continue;
                        }
                    }
                    for (CanalEntry.Entry entry : message.getEntries()) {

                        CanalEntry.RowChange rowChange;
                        try {
                            rowChange = CanalEntry.RowChange.parseFrom(entry.getStoreValue());
                        } catch (Exception e) {
                            log.error("Error parsing Canal entry: {}", e.getMessage());
                            continue; // Skip this entry and continue with the next one
                        }

                        CanalEntry.EventType eventType = rowChange.getEventType();
                        if (eventType != CanalEntry.EventType.INSERT && eventType != CanalEntry.EventType.UPDATE) {
                            continue;
                        }

                        // For each row change, create a JSON object to represent the change and send it to Kafka
                        ArrayNode dataArray = objectMapper.createArrayNode();
                        for (CanalEntry.RowData rowData : rowChange.getRowDatasList()) {
                            ObjectNode rowNode = objectMapper.createObjectNode();
                            for (CanalEntry.Column col : rowData.getAfterColumnsList()) {

                                if ("payload".equalsIgnoreCase(col.getName())) {
                                    rowNode.put("payload", col.getValue());
                                }
                            }
                            dataArray.add(rowNode);
                        }

                        ObjectNode msgNode = objectMapper.createObjectNode();
                        msgNode.put("table", entry.getHeader().getTableName());
                        msgNode.put("type", eventType == CanalEntry.EventType.INSERT ? "INSERT" : "UPDATE");
                        msgNode.set("data", dataArray);


                        try {
                            String json = objectMapper.writeValueAsString(msgNode);
                            kafka.send(OutboxTopics.CANAL_OUTBOX, json);
                        } catch (Exception e) {
                            log.error("Error sending message to Kafka: {}", e.getMessage());
                        }
                    }
                    connector.ack(batchId); // Acknowledge the batch of messages after processing
                }

            } catch (Exception e) {
                log.error("Error in Canal bridge thread", e);
            } finally {
                if (connector != null) {
                    try {
                        connector.disconnect();
                        log.info("Canal connector disconnected: dest = {}", destination);
                    } catch (Exception e) {
                        log.error("Error disconnecting Canal connector: dest = {} err = {}", destination, e.getMessage());
                    }
                }
            }
        });
    }

    /**
     * Stop the Canal to Kafka bridge. This method will set the running flag to false, which will cause the main loop in the start method to exit and clean up resources.
     */
    @Override
    public void stop() {
        running = false;
    }

    /**
     * Check if the Canal to Kafka bridge is currently running.
     * @return true if the bridge is running, false otherwise
     */
    @Override
    public boolean isRunning() {
        return running;
    }

}
