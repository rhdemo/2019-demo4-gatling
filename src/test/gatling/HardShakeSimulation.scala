import io.gatling.core.Predef._
import org.apache.kafka.clients.producer.ProducerConfig
import scala.concurrent.duration._

import com.github.mnogu.gatling.kafka.Predef._

class HardShakeSimulation extends Simulation {

  val topic = System.getProperty("topic")
  val brokers = System.getProperty("brokers")
  val users = Integer.getInteger("users", 10).toInt
  val seconds = Integer.getInteger("seconds", 30).toInt

  val kafkaConf = kafka
    // Kafka topic name
    .topic(topic)
    // Kafka producer configs
    .properties(
      Map(
        ProducerConfig.ACKS_CONFIG -> "1",
        // list of Kafka broker hostname and port pairs
        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG -> brokers,

        // in most cases, StringSerializer or ByteArraySerializer
        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG ->
          "org.apache.kafka.common.serialization.StringSerializer",
        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG ->
          "org.apache.kafka.common.serialization.StringSerializer"))

  val scn = scenario("Hard Shake")
    .exec(
      kafka("request")
        // message to send
        .send[String]("{\"userid\": \"user1234\", \"machineid\": \"machine1234\", \"gesture\": \"gesture1234\"}"))

  setUp(
    scn
      .inject(constantUsersPerSec(users) during(seconds seconds)))
    .protocols(kafkaConf)
}
