package de.ffm.rka.basic;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Produced;

import java.util.*;

public class StreamsStarter {

    public static void main(String[] args) {

        Properties properties = new Properties();
        properties.put(StreamsConfig.APPLICATION_ID_CONFIG, "streams-app");
        properties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        properties.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        properties.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        StreamsBuilder streamsBuilder = new StreamsBuilder();

        //gib mir Stream von der Topic und spreichere in Stream.
        //Key ist in der Config als String und Value ist in der Config als String
        //Inhalt ist: <null, "kafka Kafka1 Streams>
        KStream<String, String> wordCountInputStream = streamsBuilder.stream("word-count-input");
        //tue was mit den Daten und speichere im neuen Stream. Der andere bleibt unveränderlich
        //Inhalt ist: <null, "kafka kafka streams>
        final KTable<String, Long> countKTable = wordCountInputStream.mapValues(value -> value.toLowerCase())
                .peek((key, value)
                        -> System.out.println("MAP-VALUES KEY:" + key + " VALUE: " + value) )
                //wörter aufsplitten. So hat man drei unterschiedliche Nachrichten in der Tabelle
                //Inhalt ist: <null, kafka>
                //            <null, kafka>
                //            <null, streams>
                .flatMapValues(value -> Arrays.asList(value.split(" ")))
                .peek((key, value)
                        -> System.out.println("FLAT-MAP-VALUES KEY:" + key + " VALUE: " + value) )
                //der Wert wird auch als Key festgelegt
                //ergebnis ist, dass jeder value im key-bereich auch diesen value-wert hat
                //<kafka, kafka> <kafka, kafka> <streams, streams>
                //beim arbeiten mit dem Key, wird es in die internet REPARTIONLOG-TOPIC reingeschrieben
                .selectKey((ignoredKey, value) -> value)
                .peek((key, value)
                        -> System.out.println("SELECT-KEY-VALUES KEY:" + key + " VALUE: " + value) )
                //jetzt werden Values nach den Key gruppiert
                //(<kafka, kafka>,<kafka, kafka>) ->gruppe1
                //(<streams, streams>) ->gruppe2
                .groupByKey()
                //jetzt die key-value-paare aus den jeweiligen gruppen gezählt
                //<kafka, 2>, <streams, 1>
                //Das Ergebnis von allen Transformationen ist ein KTable<String, Long>
                //Intern wird es wegen der Aggregation in eine interne changelog-topic geschrieben
                .count();

        countKTable.toStream().to("word-count-output", Produced.with(Serdes.String(), Serdes.Long()));
        Topology topology = streamsBuilder.build();
        KafkaStreams kafkaStreams = new KafkaStreams(topology, properties);
        kafkaStreams.start();

        System.out.println("CURRENT TOPOLOGY: " + topology.toString());

        Runtime.getRuntime().addShutdownHook(new Thread(kafkaStreams::close));

    }
}
