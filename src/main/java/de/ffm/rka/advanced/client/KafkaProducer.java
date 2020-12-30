package de.ffm.rka.advanced.client;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.time.Instant;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;

public class KafkaProducer {
    public static void main(String[] args) throws InterruptedException {
        Properties kafkaProperties = new Properties();
        kafkaProperties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:9092");
        kafkaProperties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        kafkaProperties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        kafkaProperties.setProperty(ProducerConfig.ACKS_CONFIG, "all"); //alle Replicas müssen einen Ack schicken,
                                                                        //dass Daten empfangen wurden
        kafkaProperties.setProperty(ProducerConfig.LINGER_MS_CONFIG, "1");
        kafkaProperties.setProperty(ProducerConfig.RETRIES_CONFIG, "3");
        kafkaProperties.setProperty(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true"); //Obwohl es laut Zeile 17
                                                                                       //3 Versuche gibts, etwas zu schicken
                                                                                       //Schickt der Producer exakt nur eine
                                                                                       //Nachricht ab
        Producer<String, String> producer = new org.apache.kafka.clients.producer.KafkaProducer<>(kafkaProperties);
        for (int i=0; i<100; i++){
            producer.send(newProducerRecord("Roman"));
            Thread.sleep(100);
            producer.send(newProducerRecord("Ella"));
            Thread.sleep(100);
            producer.send(newProducerRecord("Gast"));
            Thread.sleep(100);
        }
        producer.close();


    }

    private static ProducerRecord<String, String> newProducerRecord(String key){
        ObjectNode jsonValue = JsonNodeFactory.instance.objectNode();
        Integer amount = ThreadLocalRandom.current().nextInt(0, 100);
        Instant now = Instant.now();
        jsonValue.put("name", key);
        jsonValue.put("amount", amount);
        jsonValue.put("time", now.toString());
        return new ProducerRecord<>("bank-transactions", key, jsonValue.toString());
    }
}
