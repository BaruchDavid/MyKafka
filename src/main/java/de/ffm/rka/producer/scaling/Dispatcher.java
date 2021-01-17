package de.ffm.rka.producer.scaling;



import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.io.File;
import java.util.Scanner;

public class Dispatcher implements Runnable {

    private String fileLocation;
    private String topicName;
    private KafkaProducer<Integer, String> kafkaProducer;

    public Dispatcher(String fileLocation, String topicName, KafkaProducer<Integer, String> kafkaProducer) {
        this.fileLocation = fileLocation;
        this.topicName = topicName;
        this.kafkaProducer = kafkaProducer;
    }

    @Override
    public void run() {
        sendWithoutSafity();
    }

    /**
     * wenn das I/O Thread nicht senden kann und Retry fehlschlägt,
     * dann gibts ne Exception und Daten gehen verloren
     */
    private void sendWithoutSafity(){
        System.out.println("starte thread: " + Thread.currentThread().getName());
        File file = new File(fileLocation);

        try(Scanner scanner = new Scanner(file)) {
            while (scanner.hasNextLine()){
                final String line = scanner.nextLine();
                kafkaProducer.send(new ProducerRecord<>("file-topi", null, line));
            }
        } catch(Exception ex){

        }
    }

    /**
     * hier gibts keine Blockierung und I/O Thread ruft das ihm übergebene
     * Lambda dann auf. sobald er einen ACK vom Broker erhält oder
     * einen Fehlerfall hat
     */
    private void sendWithSafity(){
        System.out.println("starte thread: " + Thread.currentThread().getName());
        File file = new File(fileLocation);

        try(Scanner scanner = new Scanner(file)) {
            while (scanner.hasNextLine()){
                final String line = scanner.nextLine();
                kafkaProducer.send(new ProducerRecord<>("file-topi", null, line),
                        (recordData, exception) ->{
                            if(exception==null){
                                System.out.println("kein Fehler ist aufgetretten, alles wurde an den Broker versendet");
                            } else {
                                System.out.println("Fehler beim senden an den Broker");
                                System.out.println("speichere nicht verschickte Daten irgendwo ab");
                            }
                        });
            }
        } catch(Exception ex){

        }
    }
}
