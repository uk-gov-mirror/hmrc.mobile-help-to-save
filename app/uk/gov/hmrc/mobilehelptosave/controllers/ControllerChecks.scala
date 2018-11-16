package uk.gov.hmrc.mobilehelptosave.controllers

import play.api.LoggerLike
import play.api.libs.json.Json
import play.api.mvc.{Result, Results}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.mobilehelptosave.domain.{ErrorBody, Shuttering}

import scala.concurrent.Future
import scala.concurrent.Future.successful
import scala.util.{Failure, Success, Try}

trait ControllerChecks extends Results {

  private final val WebServerIsDown = new Status(521)

  def logger: LoggerLike

  def withShuttering(shuttering: Shuttering)(fn: => Future[Result]): Future[Result] = {
    if (shuttering.shuttered) successful(WebServerIsDown(Json.toJson(shuttering))) else fn
  }

  def withValidNino(nino: String)(fn: Nino => Future[Result]): Future[Result] = {
    HmrcNinoDefinition.regex.findFirstIn(nino) map (n => Right(Try(Nino(n)))) getOrElse {
      Left(s""""$nino" does not match NINO validation regex""")
    } match {
      case Right(Success(parsedNino)) => fn(parsedNino)
      case Right(Failure(exception))  => successful(BadRequest(Json.toJson(ErrorBody("NINO_INVALID", exception.getMessage))))
      case Left(validationError)      => successful(BadRequest(Json.toJson(ErrorBody("NINO_INVALID", validationError))))
    }
  }

  def withMatchingNinos(nino: Nino)(fn: Nino => Future[Result])(implicit request: RequestWithIds[_]): Future[Result] = {
    if (nino == request.nino) fn(nino) else {
      logger.warn(s"Attempt by ${request.nino} to access ${nino.value}'s data")
      successful(Forbidden)
    }
  }

  def verifyingMatchingNino(shuttering: Shuttering, ninoString: String)(fn: Nino => Future[Result])(implicit request: RequestWithIds[_]): Future[Result] = {
    withShuttering(shuttering) {
      withValidNino(ninoString) { validNino =>
        withMatchingNinos(validNino) { verifiedUserNino =>
          fn(verifiedUserNino)
        }
      }
    }
  }
}
