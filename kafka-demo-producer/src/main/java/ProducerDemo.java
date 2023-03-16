import avro.PersonActivity;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.kafka.clients.producer.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.kafka.common.serialization.StringSerializer;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class ProducerDemo {
    private static final Logger log = LoggerFactory.getLogger(ProducerDemo.class);

    public static void main(String[] args) {
        log.info("I am a Kafka Producer");

        String bootstrapServers = "127.0.0.1:9091,127.0.0.1:9093,127.0.0.1:9094";
        String topic = "test-person-activity-partitions-replication-qwe";
        String schemaUrl = "http://localhost:8081";

        // create Producer properties
        Properties properties = new Properties();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class.getName());
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put("schema.registry.url", schemaUrl);

        KafkaProducer<String, PersonActivity> producer = new KafkaProducer<>(properties);

        try {
            int i = 0;
            while (true) {
                List<String> activites = Arrays.asList("activty 1", "activty 2", "activty 3", "activty 4", "activty 5");

                CharSequence key = "key" + (int) (Math.random() * 10);
                ProducerRecord<String, PersonActivity> producerRecord = new ProducerRecord<>(topic,
                        key.toString(),
                        new PersonActivity(
                                key,
                                activites.get((int) (Math.random() * 4)),
                                LocalDateTime.now().toString()));

                // send data - asynchronous
                producer.send(producerRecord);

                log.info("Message has been sent to first topic");

                // flush data - synchronous
                producer.flush();

                if (i++ == 25) {
                    if (Math.random() > 0.9) {
                        Thread.sleep(5000);
                    }

                    Thread.sleep(1000);
                    i = 0;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            producer.close();
        }
    }
}