package email.forwarder

import java.io.IOException
import java.nio.ByteBuffer
import java.util.Properties

import com.amazonaws.regions.{Region, Regions}
import com.amazonaws.services.lambda.runtime.events.SNSEvent
import com.amazonaws.services.lambda.runtime.{Context, LambdaLogger, RequestHandler}
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient
import com.amazonaws.services.simpleemail.model.{RawMessage, SendRawEmailRequest}
import io.circe.generic.auto._
import io.circe.parser._

import scala.collection.JavaConverters._
import scala.util.matching.Regex

object Handler {
  val AwsRegion = Regions.EU_WEST_1

  val (forwardFrom, forwardTo) = {
    val properties = new Properties()
    properties.load(this.getClass.getClassLoader.getResourceAsStream("config.properties"))
    (properties.getProperty("from"), properties.getProperty("to"))
  }

  val ContentType = """(Content-Type:.*)""".r
  val ContentTransferEncoding = """(Content-Transfer-Encoding:.*)""".r
  val MimeVersion = """(MIME-Version:.*)""".r

  def createNewMail(emailInfo: EmailInfo) = {
    def addHeaderIfPresent(regex: Regex, from: String)(headers: String) = {
      regex.findFirstIn(from).map(h => s"$headers$h\r\n").getOrElse(headers)
    }

    val orgTo = emailInfo.mail.commonHeaders.to.mkString(",")
    val orgFrom = emailInfo.mail.commonHeaders.from.mkString(",")
    val headers =
      s"""From: ${Handler.forwardFrom}\r
          |Reply-To: $orgFrom\r
          |X-Original-To: $orgTo\r
          |To: ${Handler.forwardTo}\r
          |Subject: FWD: ${emailInfo.mail.commonHeaders.subject}\r
          |""".stripMargin
    val originalMail = emailInfo.content

    println(s"Original mail: $originalMail\r")
    val addContentType = addHeaderIfPresent(ContentType, originalMail) _
    val addEncoding = addHeaderIfPresent(ContentTransferEncoding, originalMail) _
    val addMimeVersion = addHeaderIfPresent(MimeVersion, originalMail) _

    val completeHeaders = (addContentType andThen addEncoding andThen addMimeVersion) (headers)
    // Needs to be embedded in first content part for multipart messages
    // val mailWithoutHeaders = s"------ Forwarded (from: $orgFrom, to: $orgTo) ------\n" +
    val mailWithoutHeaders = originalMail.split("\r\n\r\n").drop(1).mkString("\r\n\r\n")
    val newMail = completeHeaders + "\r\n" + mailWithoutHeaders

    println(s"New mail: $newMail\r")
    newMail
  }
}

class Handler extends RequestHandler[SNSEvent, Any] {
  import Handler._

  def handleRequest(event: SNSEvent, context: Context): Any = {
    val logger: LambdaLogger = context.getLogger
    event.getRecords.asScala.foreach { record =>
      val msg = record.getSNS.getMessage
      logger.log(s"Received message: $msg")
      try {
        forwardEmail(logger, msg)
      } catch {
        case e: IOException => logger.log(s"Exception while parsing/forwarding SNS message: ${e.getMessage}\r")
      }
    }

    ()
  }

  private def forwardEmail(logger: LambdaLogger, msg: String) = {
    decode[EmailInfo](msg).fold(
      error => logger.log(s"Failed to parse email '$msg': $error.\r"),
      emailInfo => sendMail(createNewMail(emailInfo))
    )
  }

  private def sendMail(mail: String) {
    try {
      // Instantiate an Amazon SES client, which will make the service call with the supplied AWS credentials.
      val client = new AmazonSimpleEmailServiceClient
      val region = Region.getRegion(Handler.AwsRegion)
      client.setRegion(region)

      // Send the email.
      val rawMessage = new RawMessage(ByteBuffer.wrap(mail.getBytes("utf8")))
      val rawEmailRequest = new SendRawEmailRequest(rawMessage)
      client.sendRawEmail(rawEmailRequest)
      println("Email sent!\r");
    } catch {
      case ex: Exception =>
        println("Send email Failed\r")
        System.err.println(s"Error message: ${ex.getMessage}\r")
        ex.printStackTrace()
    }
  }
}
