package de.ffm.rka.advanced.topology;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.connect.json.JsonDeserializer;
import org.apache.kafka.connect.json.JsonSerializer;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Materialized;

import java.time.Instant;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;

public class KafkaStreamsTopology {

    public static void main(String[] args) {
        Properties kafkaProperties = new Properties();
        kafkaProperties.put(StreamsConfig.APPLICATION_ID_CONFIG, "bank-balance");
        kafkaProperties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:9092");

        //disable cache
        kafkaProperties.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, "0");

        //exactly-once
        kafkaProperties.put(StreamsConfig.PROCESSING_GUARANTEE_CONFIG, StreamsConfig.EXACTLY_ONCE);

        kafkaProperties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        Serializer<JsonNode> jsonNodeSerializer = new JsonSerializer();
        Deserializer<JsonNode> jsonNodeDeserializer = new JsonDeserializer();
        Serde jsonSerde = Serdes.serdeFrom(jsonNodeSerializer,jsonNodeDeserializer);

        StreamsBuilder streamsBuilder = new StreamsBuilder();

        final KStream<String, JsonNode> stream = streamsBuilder.stream("bank-transactions",Consumed.with(Serdes.String(), jsonSerde));

        ObjectNode initValue = JsonNodeFactory.instance.objectNode();
        Integer amount = ThreadLocalRandom.current().nextInt(0, 100);
        Instant now = Instant.now();
        initValue.put("count", "0");
        initValue.put("balance", "amount");
        initValue.put("time", Instant.ofEpochMilli(0L).toString());

        stream.groupByKey()
                .aggregate(() ->initValue,
                        (key, transaction, oldBalance) -> aggregateValue(transaction, oldBalance),
                        Materialized.with(String(), jsonSerde));


    }

    public static JsonNode aggregateValue(JsonNode transaction, JsonNode oldBalance){
        final ObjectNode jsonNodes = JsonNodeFactory.instance.objectNode();
        return jsonNodes;
    }

}
