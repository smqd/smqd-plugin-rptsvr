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

import com.thing2x.rptsvr.InputControlResource
import slick.lifted.ProvenShape

import scala.concurrent.Future


// create table JIInputControlQueryColumn (
//        input_control_id number(19,0) not null,
//        query_column nvarchar2(200) not null,
//        column_index number(10,0) not null,
//        primary key (input_control_id, column_index)
//    );
final case class JIInputControlQueryColumn ( inputControlId: Long,
                                             queryColumn: String,
                                             columnIndex: Int)


trait JIInputControlQueryColumnSupport { mySelf: DBRepository =>
  import dbContext.profile.api._

  final class JIInputControlQueryColumnTable(tag: Tag) extends Table[JIInputControlQueryColumn](tag, dbContext.table("JIInputControlQueryColumn")) {
    def inputControlId = column[Long]("INPUT_CONTROL_ID")
    def queryColumn = column[String]("QUERY_COLUMN")
    def columnIndex = column[Int]("COLUMN_INDEX")

    def pk = primaryKey("JIINPUTCONTROLQUERYCOLUMN_PK", (inputControlId, columnIndex))
    def inputControlIdFk = foreignKey("JIINPUTCONTROLQUERYCOLUMN_INPUT_CONTROL_ID_FK", inputControlId, inputControls)(_.id)

    def * : ProvenShape[JIInputControlQueryColumn] = (inputControlId, queryColumn, columnIndex).mapTo[JIInputControlQueryColumn]
  }

  def selectInputControlQueryColumnModel(path: String): Future[Seq[InputControlResource]] = selectInputControlQueryColumnModel(Left(path))

  def selectInputControlQueryColumnModel(id: Long): Future[Seq[InputControlResource]] = selectInputControlQueryColumnModel(Right(id))

  private def selectInputControlQueryColumnModel(pathOrId: Either[String, Long]): Future[Seq[InputControlResource]] = {
    val action = pathOrId match {
      case Left(path) =>
        val (folderPath, name) = splitPath(path)
        for {
          folder   <- resourceFolders.filter(_.uri === folderPath)
          resource <- resources.filter(_.parentFolder === folder.id).filter(_.name === name)
          ic       <- inputControls.filter(_.id === resource.id)
          icqc     <- inputControlQueryColumns.filter(_.inputControlId === ic.id).sortBy(_.columnIndex)
        } yield (icqc, ic, resource, folder)
      case Right(id) =>
        for {
          icqc     <- inputControlQueryColumns.filter(_.inputControlId === id).sortBy(_.columnIndex)
          ic       <- icqc.inputControlIdFk
          resource <- ic.idFk
          folder   <- resource.parentFolderFk
        } yield (icqc, ic, resource, folder)
    }
    dbContext.run(action.result).map{ //x => case (icqc, ic, resource, folder) =>
      ???
    }
  }

  def selectInputControlQueryColumn(path: String): Future[Seq[JIInputControlQueryColumn]] = selectInputControlQueryColumn(Left(path))

  def selectInputControlQueryColumn(id: Long): Future[Seq[JIInputControlQueryColumn]] = selectInputControlQueryColumn(Right(id))

  private def selectInputControlQueryColumn(pathOrId: Either[String, Long]): Future[Seq[JIInputControlQueryColumn]] = {
    val action = pathOrId match {
      case Left(path) =>
        val (folderPath, name) = splitPath(path)
        for {
          folder   <- resourceFolders.filter(_.uri === folderPath)
          resource <- resources.filter(_.parentFolder === folder.id).filter(_.name === name)
          ic       <- inputControls.filter(_.id === resource.id)
          icqc     <- inputControlQueryColumns.filter(_.inputControlId === ic.id).sortBy(_.columnIndex)
        } yield icqc
      case Right(id) =>
        for {
          icqc     <- inputControlQueryColumns.filter(_.inputControlId === id).sortBy(_.columnIndex)
        } yield icqc
    }
    dbContext.run(action.result)
  }

  def insertInputControlQueryColumn(icqcList: Seq[JIInputControlQueryColumn]): Future[Option[Int]] = {
    val action = inputControlQueryColumns ++= icqcList
    dbContext.run(action)
  }
}
