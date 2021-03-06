// Copyright (C) 2019  UANGEL
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU Lesser General Public License as published
// by the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU Lesser General Public License for more details.
//
// You should have received a copy of the GNU General Lesser Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.

package com.thing2x.rptsvr.repo.db

import java.text.SimpleDateFormat

import akka.stream.Materializer
import com.thing2x.rptsvr.{Repository, RepositoryContext}
import com.thing2x.smqd.Smqd
import com.thing2x.smqd.util.ConfigUtil._
import com.typesafe.config.Config
import com.typesafe.scalalogging.StrictLogging
import slick.jdbc.JdbcProfile

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

class DBRepositoryContext(val repository: Repository, val smqd: Smqd, config: Config) extends RepositoryContext with StrictLogging {

  private var forceUpperCasesForTableName = false

  val profile: JdbcProfile =
  {
    config.getString("database.driver") match {
      case "org.h2.Driver" =>
        forceUpperCasesForTableName = true
        slick.jdbc.H2Profile
      case "oracle.jdbc.OracleDriver" =>
        forceUpperCasesForTableName = true
        slick.jdbc.OracleProfile
      case "com.mysql.jdbc.Driver" =>
        forceUpperCasesForTableName = false
        slick.jdbc.MySQLProfile
    }
  }

  logger.info(s"Database Profile = $profile")
  val readOnly: Boolean = config.getOptionBoolean("database.readOnly").getOrElse(false)
  val dateFormat: SimpleDateFormat = new SimpleDateFormat(config.getString("formats.date"))
  val datetimeFormat: SimpleDateFormat = new SimpleDateFormat(config.getString("formats.datetime"))
  val executionContext: ExecutionContext = smqd.Implicit.gloablDispatcher
  val materializer: Materializer = smqd.Implicit.materializer

  private val deferedBlock = new ListBuffer[() => Unit]

  import profile.api._

  private var _database: Database = _
  def database: Database = _database

  def open()(block: => Unit): Unit = {
    _database = Database.forConfig("database", config)
    defer(block)
  }

  def close(): Unit = {
    for (block <- deferedBlock) block()
    _database.close()
  }

  def defer(block: => Unit): Unit = {
    deferedBlock += ( () => block )
  }

  private[db] def run[T](act: DBIO[T]): Future[T] = _database.run(act)

  private[db] def runSync[T](act: DBIO[T], timeout: Duration): T = Await.result(_database.run(act), timeout)

  def table(tn: String): String = {
    if (forceUpperCasesForTableName)
      tn.toUpperCase
    else
      tn
  }
}
