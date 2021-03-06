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

import java.util.Base64

import com.thing2x.rptsvr.DataTypeResource
import slick.lifted.ProvenShape

import scala.concurrent.Future

//    create table JIDataType (
//        id number(19,0) not null,
//        type number(3,0),
//        maxLength number(10,0),
//        decimals number(10,0),
//        regularExpr nvarchar2(255),
//        minValue raw(1000),
//        max_value raw(1000),
//        strictMin number(1,0),
//        strictMax number(1,0),
//        primary key (id)
//    );
final case class JIDataType( dataType: Int,
                             maxLength: Option[Int],
                             decimals: Option[Int],
                             regularExpr: Option[String],
                             minValue: Option[Array[Byte]],
                             maxValue: Option[Array[Byte]],
                             strictMin: Boolean,
                             strictMax: Boolean,
                             id: Long = 0L)

trait JIDataTypeSupport { mySelf: DBRepository =>
  import dbContext.profile.api._

  final class JIDataTypeTable(tag: Tag) extends Table[JIDataType](tag, dbContext.table("JIDataType")) {
    def dataType = column[Int]("TYPE")
    def maxLength = column[Int]("MAXLENGTH")
    def decimals = column[Int]("DECIMALS")
    def regularExpr = column[String]("REGULAREXPR")
    def minValue = column[Array[Byte]]("MINVALUE")
    def maxValue = column[Array[Byte]]("MAX_VALUE")
    def strictMin = column[Boolean]("STRICTMIN")
    def strictMax = column[Boolean]("STRICTMAX")
    def id = column[Long]("ID", O.PrimaryKey)

    def * : ProvenShape[JIDataType] = (dataType, maxLength.?, decimals.?, regularExpr.?, minValue.?, maxValue.?, strictMin, strictMax, id)<>( JIDataType.tupled, JIDataType.unapply )
  }

  def selectDataTypeModel(path: String): Future[DataTypeResource] = selectDataTypeModel(Left(path))

  def selectDataTypeModel(id: Long): Future[DataTypeResource] = selectDataTypeModel(Right(id))

  private def selectDataTypeModel(pathOrId: Either[String, Long]): Future[DataTypeResource] = {
    val action = pathOrId match {
      case Left(path) =>
        val (folderPath, name) = splitPath(path)
        for {
          folder   <- resourceFolders.filter(_.uri === folderPath)
          resource <- resources.filter(_.parentFolder === folder.id).filter(_.name === name)
          dt       <- dataTypes.filter(_.id === resource.id)
        } yield (dt, resource, folder)
      case Right(id) =>
        for {
          dt       <- dataTypes.filter(_.id === id)
          resource <- resources.filter(_.id === id)
          folder   <- resource.parentFolderFk
        } yield (dt, resource, folder)
    }

    dbContext.run(action.result.head).map { case (dt, resource, folder) =>
      val fr = DataTypeResource( s"${folder.uri}/${resource.name}", resource.label)
      fr.typeId = dt.dataType
      fr.maxLength = dt.maxLength
      fr.decimals = dt.decimals
      fr.regularExpr = dt.regularExpr
      fr.minValue = dt.minValue.map(new String(_))
      fr.maxValue = dt.maxValue.map(new String(_))
      fr.strictMin = dt.strictMin
      fr.strictMax = dt.strictMax
      fr.creationDate = resource.creationDate
      fr.updateDate = resource.updateDate
      fr.description = resource.description
      fr
    }
  }

  def selectDataType(path: String): Future[JIDataType] = selectDataType(Left(path))

  def selectDataType(id: Long): Future[JIDataType] = selectDataType(Right(id))

  private def selectDataType(pathOrId: Either[String, Long]): Future[JIDataType] = {
    val action = pathOrId match {
      case Left(path) =>
        val (folderPath, name) = splitPath(path)
        for {
          folder   <- resourceFolders.filter(_.uri === folderPath)
          resource <- resources.filter(_.parentFolder === folder.id).filter(_.name === name)
          dt       <- dataTypes.filter(_.id === resource.id)
        } yield dt
      case Right(id) =>
        for {
          dt       <- dataTypes.filter(_.id === id)
        } yield dt
    }

    dbContext.run(action.result.head)
  }

  def insertDataType(dt: DataTypeResource): Future[Long] = {
    val (folderPath, name) = splitPath(dt.uri)
    val minValue = dt.minValue.map( d => Base64.getDecoder.decode(d) )
    val maxValue = dt.maxValue.map( d => Base64.getDecoder.decode(d) )
    for {
      folderId   <- insertResourceFolderIfNotExists(folderPath)
      resourceId <- insertResource( JIResource(name, folderId, None, dt.label, dt.description, DBResourceTypes.dataType, version = dt.version + 1))
      dtId       <- insertDataType( JIDataType(dt.typeId, dt.maxLength, dt.decimals, dt.regularExpr, minValue, maxValue, dt.strictMin, dt.strictMax, resourceId) )
    } yield dtId
  }

  def insertDataType(dt: JIDataType): Future[Long] = {
    val action = dataTypes += dt
    dbContext.run(action).map( _ => dt.id)
  }
}
