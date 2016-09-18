package email

package object forwarder {
  private final val PassedStatus = "PASS"

  case class EmailInfo(content : String, notificationType: String, receipt: EmailReceipt, mail: Mail)
  case class EmailReceipt(spfVerdict: Verdict, spamVerdict: Verdict, dkimVerdict: Verdict, virusVerdict: Verdict) {
    val checksPassed = spfVerdict.passed && dkimVerdict.passed && spamVerdict.passed && virusVerdict.passed
  }
  case class Mail(commonHeaders: CommonHeaders)
  case class CommonHeaders(subject: String, to: List[String], from: List[String])
  case class Verdict(status: String) {
    def passed: Boolean = this.status == PassedStatus
  }
}
