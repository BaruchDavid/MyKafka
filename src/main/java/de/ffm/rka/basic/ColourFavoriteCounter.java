package de.ffm.rka.basic;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Produced;

import java.util.Arrays;
import java.util.Properties;

public class ColourFavoriteCounter {

    public static void main(String[] args) {

        Properties properties = new Properties();
        properties.put(StreamsConfig.APPLICATION_ID_CONFIG, "favorite-colour-app");
        properties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        properties.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        properties.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        /*wichtige Einstellung für EXACTLY_ONCE: commit.intervals.ms*/
        properties.put(StreamsConfig.PROCESSING_GUARANTEE_CONFIG, StreamsConfig.EXACTLY_ONCE);
        //disable cache to show all steps involed in the transformation
        properties.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, "0");
        StreamsBuilder streamsBuilder = new StreamsBuilder();

        /**
         * Aus den Eingaben: Roman, Schwarz;  Peter, Blau; Sven, Rot
         * wird ein Stream erzeugt mit Key-Value <User,Colour>
         */
        final KStream<String, String> textLine = streamsBuilder.stream("favorite-colour-input");
        textLine.peek((key, value) -> System.out.println("TEXT-LINE KEY "+ key + "TEXT-LINE VALUE" + value));
        final KStream<String, String> userColourStream = textLine.filter((key, value) -> value.contains(","))
                .selectKey((key, value) -> value.split(",")[0].toLowerCase())
                .peek((key, value) -> System.out.println("NACH DEM SELECT-KEY KEY "+ key + "  VALUE " + value))
                .mapValues(value -> value.split(",")[1].toLowerCase())
                .peek((key, value) -> System.out.println("NACH DEM MAP-VALUES KEY "+ key + " VALUE " + value))
                .filter((key, value) -> Arrays.asList("green", "blue", "red").contains(value));
        /**
         * diesen <User,Colour> Stream in eine Topic leiten
         */
        userColourStream.to("user-colour-topic");

        /**
         * Jetzt wird die Topic mit dem Aufbau <User,Colour> verarbeitet.
         * Bedienung dabei ist, dass Value-Angabe 'Colour' sich ändern kann.
         * Daher wird der Stream in einer KTable abgelegt, da diese den aktuallisierten Wert
         * unter dem Key den alten Value-Wert überschreibt
         */
        final KTable<String, String> colourCountTable = streamsBuilder.table("user-colour-topic");

        /**
         * groupBy (colour1, colour2)
         * colour1 ist der Schlüssel: z.b Schwarz
         * colour2 ist der Wert, also auch Schwarz
         * .count() auf den Value beziehen
         */

        final KTable<String, Long> colourCount = colourCountTable.groupBy((user, colour) -> new KeyValue<>(colour, colour))
                                            .count();
        colourCount.toStream().peek((key, value)  -> System.out.println("NACH DEM GROUP BY KEY "+ key + " VALUE "+value));

        /**
         * aus dem KTable zurück in den Stream
         */
        final KStream<String, Long> stringLongKStream = colourCount.toStream();

        stringLongKStream.to("favorite-colour-output", Produced.with(Serdes.String(), Serdes.Long()));
        final Topology topology = streamsBuilder.build();
        KafkaStreams kafkaStreams = new KafkaStreams(topology, properties);
        kafkaStreams.cleanUp();
        kafkaStreams.start();


        Runtime.getRuntime().addShutdownHook(new Thread(kafkaStreams::close));

    }
}
