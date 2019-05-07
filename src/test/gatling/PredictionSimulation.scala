import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.http.protocol.HttpProtocolBuilder

import scala.concurrent.duration._

class PredictionSimulation extends Simulation {

  val host: String = System.getProperty("host", "tf-serving-knative-demo")
  val gateway: String = System.getProperty("gateway", host)
  val path: String = System.getProperty("path", "/model/predict")
  val numUsers = Integer.getInteger("users", 10).toInt
  val seconds = Integer.getInteger("seconds", 30).toInt

  val protocol: HttpProtocolBuilder = http
    .baseUrl("http://" + gateway)
    .virtualHost(host)
    .acceptHeader("*/*")
    .userAgentHeader("Gatling")

  val scn = scenario("Prediction")
    .repeat(10, "attempts") {
      exec(http("request")
        .post(path)
        .body(RawFileBody("draw_circle.json"))
        .asJson
        .check(status.is(200)))
      .pause(5)
    }

  setUp(
    scn
      .inject(
        constantConcurrentUsers(numUsers) during (seconds seconds)))
    .protocols(protocol)
}
