package email.forwader

import email.forwarder.EmailInfo
import email.forwarder.Handler._
import io.circe.generic.auto._
import io.circe.parser._
import org.scalatest.{Matchers, OptionValues, WordSpecLike}

import scala.io.Source

class JsonToNewMailTest extends WordSpecLike
  with Matchers
  with OptionValues {

  "The email messages" should {
    "be json decoded and turned into a new email" in {
      val testData = Source.fromInputStream(this.getClass().getResourceAsStream("/test-email.json")).getLines().mkString("\n")
      val expected = """From: forwarder@test.org
                       |Reply-To: Remco Beckers <remco.beckers@gmail.com>
                       |X-Original-To: test@test.org
                       |To: test@gmail.com
                       |Subject: FWD: Test
                       |Content-Type: multipart/alternative; boundary=089e012292687ad7b3053cb47d84
                       |MIME-Version: 1.0
                       |
                       |--089e012292687ad7b3053cb47d84
                       |Content-Type: text/plain; charset=UTF-8
                       |
                       |test
                       |
                       |--089e012292687ad7b3053cb47d84
                       |Content-Type: text/html; charset=UTF-8
                       |
                       |<div dir="ltr">test</div>
                       |
                       |--089e012292687ad7b3053cb47d84--
                       |""".stripMargin

      val result = decode[EmailInfo](testData).map(createNewMail(_)).getOrElse("")

      result.replaceAll("\\r", "") shouldBe expected
    }
  }
}
