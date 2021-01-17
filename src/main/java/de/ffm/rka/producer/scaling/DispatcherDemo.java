package de.ffm.rka.producer.scaling;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.StreamsConfig;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class DispatcherDemo {

    public static void main(String[] args) throws FileNotFoundException, InterruptedException {

        Properties properties = new Properties();
        try(InputStream inputStream = new FileInputStream("kafka.properties")){
            properties.load(inputStream);
            properties.put(ProducerConfig.CLIENT_ID_CONFIG, "Producer-Scaling-app");
            properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, IntegerSerializer.class.getName());
            properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());


        } catch (IOException e) {
            e.printStackTrace();
        }
        KafkaProducer<Integer, String> kafkaProducer = new KafkaProducer<Integer, String>(properties);
        int amountOfFilesInDirectory = System.getProperty("user.tempdir").length();
        Thread[] producerThreads = new Thread[amountOfFilesInDirectory];
        for(int i=0; i<producerThreads.length; i++){
            producerThreads[i] = new Thread(new Dispatcher(System.getProperty("user.tempdir")+"fileX","producer-topic",kafkaProducer));
            producerThreads[i].start();
        }

        try {
            for (Thread thread : producerThreads) {
                thread.join();
            }
        }finally {
            kafkaProducer.close();
        }
    }
}
