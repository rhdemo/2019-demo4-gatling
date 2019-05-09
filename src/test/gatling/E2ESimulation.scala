import java.io.{File, FileInputStream}
import java.util.{Base64, Random, UUID}

import io.gatling.core.Predef._
import io.gatling.core.structure.{ChainBuilder, ScenarioBuilder}
import io.gatling.http.Predef._
import io.gatling.http.protocol.HttpProtocolBuilder

import scala.concurrent.duration._
import scala.util.parsing.json.JSON


class E2ESimulation extends Simulation {

  val DEV = "play-web-game-demo.apps.dev.openshift.redhatkeynote.com/game-socket"
  val LIVE = "play-web-game-demo.apps.live.openshift.redhatkeynote.com/game-socket"

  val DANCES = Array(
    "shake.json",
    "shake.json",
    "circle.json",
    "circle.json",
    "x.json",
    "x.json",
    "roll.json",
    "fever.json",
    "floss.json",
  )

  val BAD_DANCE = "bad-move.json"

  def getHost(arg: String): String = arg match {
    case "DEV" => DEV
    case "LIVE" => LIVE
    case _ => arg
  }

  val host: String = getHost(System.getProperty("host", LIVE))
  val numUsers: Int = Integer.getInteger("users", 5).toInt
  val numDances: Int = Integer.getInteger("dances", 5).toInt
  val percentBadDances: Int = Integer.getInteger("percentBadDances", 10).toInt

  val protocol: HttpProtocolBuilder = http
    .baseUrl("https://" + host)
    .acceptHeader("*/*")
    .doNotTrackHeader("1")
    .acceptLanguageHeader("en-US,en;q=0.5")
    .acceptEncodingHeader("gzip, deflate")
    .userAgentHeader("Gatling")
    .wsBaseUrl("wss://" + host)
    .wsReconnect
    .wsMaxReconnects(3)

  val scn: ScenarioBuilder = scenario("Connection-Scenario")
    .exec(Connection.connect)
    .pause(3)
    .exec(Dance.dance)
    .pause(10)
    .exec(ws("Close").close)

  setUp(
    scn.inject(rampUsers(numUsers) during (5 seconds)).protocols(protocol)
  )
  

  object Connection {
    val checkConfiguration = ws.checkTextMessage("checkConfiguration")
      .matching(
        jsonPath("$.type").is("configuration")
      )
      .check(
        jsonPath("$.playerId").saveAs("playerId"),
        jsonPath("$.gameId").saveAs("gameId")
      )
    val connect: ChainBuilder =
      exec(http("GET root").get("/")

      )
        .exec(ws("Connect to /game-socket").connect("/game-socket")
          .onConnected(
            doIfOrElse(session => session("playerId").asOption[String].forall(_.isEmpty)) {
              exec(ws("Connect")
                .sendText("""{"type": "connection"}""")
                .await(5 seconds)(checkConfiguration))
            } {
              exec(ws("Reconnect")
                .sendText("""{"type": "connection", "gameId": "${gameId}", "playerId": "${playerId}"}""")
                .await(5 seconds)(checkConfiguration))
            }
          ))
  }


  object Dance {

    private val random = new Random()

    val checkDance = ws.checkTextMessage("checkDance")
      .matching(
        jsonPath("$.type").is("motion_feedback")
      )
      .check(jsonPath("$").saveAs("motion_feedback"))
      .check(jsonPath("$.correct").exists)
      .check(jsonPath("$.gesture").exists)
      .check(jsonPath("$.probability").exists)
      .check(jsonPath("$.score").exists)
      .check(jsonPath("$.totalScore").exists)
      .check(jsonPath("$.correct")
        .transform((correct, session) => {
          val dance = session("dance").as[String]
          var success = true
          if (dance.startsWith("bad") && correct != "false") {
            success = false
          }
          if (!dance.startsWith("bad") && correct != "true") {
            success = false
          }
          if (success) "ok" else "ko"
        }).is("ok")
      )
      .check(jsonPath("$.prediction.candidate")
        .transform((candidate, session) => {
          val expectedDance = session("dance").as[String].replace(".json", "")
          val actualDance = candidate match {
            case "draw-circle" => "circle"
            case "draw-cross" => "x"
            case _ => candidate
          }
          var success = true
          if (!expectedDance.startsWith("bad") && expectedDance != actualDance) {
            success = false
          }
          if (success) "ok" else "ko"
        }).is("ok")
      )

    val dance: ChainBuilder =
      repeat(numDances, "attempts") {
        exec(session => {
          val uuid = UUID.randomUUID().toString
          var dance = DANCES(random.nextInt(DANCES.length))

          // Check if we should instead do a bad dance move
          if (random.nextInt(100) < percentBadDances) {
            dance = BAD_DANCE
          }
          println("Performing dance " + dance)

          session
            .set("uuid", uuid)
            .set("dance", dance)
        })
          .exec(ws("Dance move")
            .sendText(ElFileBody("${dance}"))
            .await(7 seconds)(checkDance)
          )
          .exec(session => {
            val motion_feedback = session("motion_feedback").as[String]
//            println(motion_feedback)
            session
          })
          .pause(4 + random.nextInt(3))
      }
  }

}
