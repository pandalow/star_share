package com.star.share.common.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class OutboxMessageUtil {
    private OutboxMessageUtil() { // Utility class, prevent instantiation
    }

    /**
     * Extract affected rows from a Canal outbox message.
     * @param objectMapper JSON object mapper for parsing the message
     * @param message The raw JSON message from the Canal outbox topic
     * @return A list of JsonNode objects representing the affected rows, or an empty list if the message is not relevant or cannot be parsed
     */

    public static List<JsonNode> extractRows(ObjectMapper objectMapper, String message) {
        try {
            JsonNode root = objectMapper.readTree(message);

            JsonNode table = root.get("table");
            if (table == null || !"outbox".equals(table.asText())) {
                return Collections.emptyList();
            }

            JsonNode type = root.get("type");
            if (type == null || (!"INSERT".equals(type.asText()) && !"UPDATE".equals(type.asText()))) {
                return Collections.emptyList();
            }

            JsonNode data = root.get("data");
            if (data == null || !data.isArray()) {
                return Collections.emptyList();
            }
            List<JsonNode> rows = new ArrayList<>();
            data.forEach(rows::add);
            return rows;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}

