package com.star.share.relation.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
@Mapper
public interface OutboxMapper {

    /**
     * INSERT a OUTBOX event into the database.
     * @param id the unique identifier for the outbox event
     * @param aggregateType the type of the aggregate root associated with the event
     * @param aggregateId the unique identifier of the aggregate root associated with the event
     * @param type the type of the event
     * @param payload the serialized payload of the event
     * @return the number of rows affected by the insert operation
     */
    int insert(@Param("id") Long id,
               @Param("aggregateType") String aggregateType,
               @Param("aggregateId") Long aggregateId,
               @Param("type") String type,
               @Param("payload") String payload);
}
