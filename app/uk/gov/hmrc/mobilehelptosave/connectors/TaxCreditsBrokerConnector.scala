/*
 * Copyright 2018 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.mobilehelptosave.connectors

import java.net.URL

import com.google.inject.ImplementedBy
import javax.inject.{Inject, Singleton}
import org.joda.time.DateTime
import play.api.LoggerLike
import play.api.libs.json._
import uk.gov.hmrc.config.TaxCreditsBrokerConnectorConfig
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http._
import uk.gov.hmrc.mobilehelptosave.domain.ErrorInfo
import uk.gov.hmrc.play.encoding.UriPathEncoding.encodePathSegments

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[TaxCreditsBrokerConnectorImpl])
trait TaxCreditsBrokerConnector {

  def previousPayments(nino: Nino)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[ErrorInfo, Seq[Payment]]]

}

@Singleton
class TaxCreditsBrokerConnectorImpl @Inject() (
  logger: LoggerLike,
  config: TaxCreditsBrokerConnectorConfig,
  http: CoreGet
) extends TaxCreditsBrokerConnector {

  override def previousPayments(nino: Nino)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[ErrorInfo, Seq[Payment]]] =
    http.GET[JsValue](previousPaymentsUrl(nino).toString).map { jsonBody =>
      if ((jsonBody \ "excluded").asOpt[Boolean].contains(true)) {
        Right(Seq.empty)
      } else {
        Right((jsonBody \ "workingTaxCredit" \ "previousPaymentSeq").asOpt[JsValue].fold(Seq.empty[Payment])(_.as[Seq[Payment]]))
      }
    } recover {
      case e@(_: HttpException | _: Upstream4xxResponse | _: Upstream5xxResponse | _: JsResultException) =>
        logger.warn("Couldn't get payments from tax-credits-broker service", e)
        Left(ErrorInfo.General)
    }

  private def previousPaymentsUrl(nino: Nino) =
    new URL(
      config.taxCreditsBrokerBaseUrl,
      encodePathSegments("tcs", nino.value, "payment-summary")
    )
}

case class Payment(amount: BigDecimal, paymentDate: DateTime)

object Payment {
  implicit val reads: Reads[Payment] = Json.reads[Payment]
}
