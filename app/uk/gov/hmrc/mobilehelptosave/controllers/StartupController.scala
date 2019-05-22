/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.mobilehelptosave.controllers

import play.api.libs.json.Json
import play.api.mvc._
import uk.gov.hmrc.mobilehelptosave.config.StartupControllerConfig
import uk.gov.hmrc.mobilehelptosave.domain._
import uk.gov.hmrc.mobilehelptosave.services.UserService
import uk.gov.hmrc.play.bootstrap.controller.BackendBaseController

import scala.concurrent.{ExecutionContext, Future}

class StartupController(
  userService:              UserService[Future],
  authorisedWithIds:        AuthorisedWithIds,
  config:                   StartupControllerConfig,
  val controllerComponents: ControllerComponents
)(implicit ec:              ExecutionContext)
    extends BackendBaseController {

  val startup: Action[AnyContent] = if (!config.shuttering.shuttered) {
    authorisedWithIds.async { implicit request =>
      val responseF = userService.userDetails(request.nino).map { userOrError =>
        StartupResponse(
          shuttering         = config.shuttering,
          infoUrl            = Some(config.helpToSaveInfoUrl),
          accessAccountUrl   = Some(config.helpToSaveAccessAccountUrl),
          accountPayInUrl    = Some(config.helpToSaveAccountPayInUrl),
          user               = userOrError.right.toOption,
          userError          = userOrError.left.toOption
        )
      }
      responseF.map(response => Ok(Json.toJson(response)))
    }
  } else {
    Action { implicit request =>
      val response =
        StartupResponse(
          shuttering         = config.shuttering,
          infoUrl            = None,
          accessAccountUrl   = None,
          accountPayInUrl    = None,
          user               = None,
          userError          = None
        )

      Ok(Json.toJson(response))
    }
  }

}
