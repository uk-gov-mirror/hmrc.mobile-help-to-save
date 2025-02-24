/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.mobilehelptosave.wiring
import com.softwaremill.macwire.wire
import play.api.BuiltInComponents
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.audit.http.config.AuditingConfig
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.audit.DefaultAuditConnector
import uk.gov.hmrc.play.bootstrap.config.{AppName, AuditingConfigProvider, RunMode}
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpAuditing

trait AuditWiring {
  self: BuiltInComponents =>

  private lazy val appName:    String         = AppName.fromConfiguration(configuration)
  lazy val auditConnector:     AuditConnector = wire[DefaultAuditConnector]
  lazy val runMode:            RunMode        = new RunMode(configuration, environment.mode)
  lazy val httpAuditing:       HttpAuditing   = wire[DefaultHttpAuditing]
  lazy val httpAuditingConfig: AuditingConfig = wire[AuditingConfigProvider].get
}
