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

package com.thing2x.rptsvr

import akka.http.scaladsl.model.{HttpCharsets, MediaType}
import io.circe.syntax._
import io.circe.{ACursor, DecodingFailure, Json}

import scala.collection.mutable

object ReportUnitResource {
  def apply(uri: String, label: String)(implicit context: RepositoryContext): ReportUnitResource = new ReportUnitResource(uri, label)
}

class ReportUnitResource(val uri: String, val label: String)(implicit context: RepositoryContext) extends Resource {
  override val resourceType: String = "reportUnit"
  override val mediaType: MediaType.WithFixedCharset = MediaType.applicationWithFixedCharset("repository.reportUnit+json", HttpCharsets.`UTF-8`)

  var alwaysPromptControls: Boolean = true
  var controlsLayout: String = "popupScreen"
  var jrxml: Option[FileResource] = None
  var resources: Map[String, FileResource] = Map.empty
  var inputControls: Seq[InputControlResource] = Seq.empty
  var dataSource: Option[DataSourceResource] = None
  var query: Option[QueryResource] = None

  def conrolsLayoutId: Int = {
    controlsLayout match {
      case "separatePage" => 1
      case "topOfPage" => 2
      case "inPage" => 3
      case _ => 0 // popupScreen (default)
    }
  }

  override def encodeFields(expanded: Boolean): Map[String, Json] = {
    val map: mutable.Map[String, Json] = mutable.Map.empty

    map("alwaysPromptControls") = Json.fromBoolean(alwaysPromptControls)
    map("controlsLayout")       = Json.fromString(controlsLayout)

    if (inputControls.nonEmpty) {
      map("inputControls") = inputControls.map( input =>
        if (expanded) {
          Json.obj("inputControl" -> input.asJson(expanded))
        }
        else {
          Json.obj("inputControlReference" -> Json.obj("uri" -> Json.fromString(input.uri)))
        }).asJson
    }

    if (jrxml.isDefined) {
      map("jrxml") = if (expanded) {
        Json.obj("jrxmlFile" -> jrxml.get.asJson(expanded))
      }
      else {
        Json.obj("jrxmlFileReference" -> Json.obj(
          "uri" -> Json.fromString(jrxml.get.uri)
        ))
      }
    }

    if (resources.nonEmpty) {
      map("resources") = Json.obj(
        "resource" ->
          resources.map { case (name, r) =>
            Json.obj("name" -> Json.fromString(name), "file" -> (
              if (expanded)
                Json.obj( "fileResource" -> r.asJson(expanded))
              else
                Json.obj("fileReference" -> Json.obj("uri" -> Json.fromString(r.uri)))
              ))
          }.asJson)
    }

    if (dataSource.isDefined) {
      val ds = dataSource.get
      map("dataSource") = if (expanded) {
        Json.obj(
          ds.resourceType -> ds.asJson(expanded)
        )
      }
      else {
        Json.obj(
          "dataSourceReference" -> Json.obj("uri" -> Json.fromString(ds.uri))
        )
      }
    }

    if (query.isDefined) {
      val q = query.get
      map("query") = if (expanded) {
        Json.obj (
          q.resourceType -> q.asJson(expanded)
        )
      }
      else {
        Json.obj(
          "queryReference" -> Json.obj("uri" -> Json.fromString(q.uri))
        )
      }
    }
    Map(map.toSeq:_*)
  }

  override def decodeFields(cur: ACursor): Either[DecodingFailure, Resource] = {
    alwaysPromptControls = cur.downField("alwaysPromptControls").as[Boolean].right.get
    controlsLayout = cur.downField("controlsLayout").as[String].right.get

    decodeReferencedResource[FileResource](cur.downField("jrxml"), "jrxmlFile", "file") match {
      case Right(r) => jrxml = Some(r)
      case _ => jrxml = None
    }

    var resourceCur = cur.downField("resources").downField("resource").downArray
    var resourceMap: Map[String, FileResource] = Map.empty

    while( resourceCur.succeeded ) {
      val name = resourceCur.downField("name").as[String].right.get
      decodeReferencedResource[FileResource](resourceCur.downField("file"), "fileResource", "fileReference", "file") match {
        case Right(r) => resourceMap ++= Map(name -> r)
        case Left(e) =>  logger.error(s"Sub-resource loading failure ${e.message}")
      }
      resourceCur = resourceCur.right
    }
    resources = resourceMap

    var inputControlCur = cur.downField("inputControls").downArray
    var inputs: Seq[InputControlResource] = Seq.empty
    while( inputControlCur.succeeded ) {
      decodeReferencedResource[InputControlResource](inputControlCur, "inputControl", "inputControl") match {
        case Right(r) => inputs ++= Seq(r)
        case Left(e) => logger.error(s"Sub-input control loading failure ${e.message}")
      }
      inputControlCur = inputControlCur.right
    }
    inputControls = inputs

    val dataSourceCur = cur.downField("dataSource")
    if (dataSourceCur.succeeded) {
      val dsRefCur = dataSourceCur.downField("dataSourceReference").downField("uri")
      if (dsRefCur.succeeded)
        referencedResource[DataSourceResource](dsRefCur.as[String].right.get) match {
          case Right(r) => dataSource = Some(r)
          case _ => dataSource = None
        }
    }
    Right(this)
  }

  def write(writer: ResourceWriter): Unit = {
    writer.writeMeta(this)
    jrxml match {
      case Some(r) =>
        context.repository.setResource(r.uri, r, createFolders = true, overwrite = true)
      case _ =>
    }

    resources.foreach{ case(_, r) =>
      context.repository.setResource(r.uri, r, createFolders = true, overwrite = true)
    }

    inputControls.foreach { r =>
      if (!r.isReferenced)
        context.repository.setResource(r.uri, r, createFolders = true, overwrite = true)
    }

    dataSource match {
      case Some(r) =>
        if (!r.isReferenced)
          context.repository.setResource(r.uri, r, createFolders=true, overwrite = true)
      case _ =>
    }
  }
}

