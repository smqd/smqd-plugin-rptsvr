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

package com.thing2x.rptsvr.export

import java.io.{ByteArrayOutputStream, File}

import akka.util.ByteString
import com.thing2x.rptsvr.engine.ReportExporter
import net.sf.jasperreports.engine.export.JRTextExporter
import net.sf.jasperreports.engine.{JasperPrint, JasperReportsContext}
import net.sf.jasperreports.export._

class TextExporter(jsContext: JasperReportsContext) extends ReportExporter {
  override def exportReport(jasperPrint: JasperPrint): ByteString = {
    val baos = new ByteArrayOutputStream()
    val exporter = new JRTextExporter(jsContext)
    exporter.setExporterInput(new SimpleExporterInput(jasperPrint))
    exporter.setExporterOutput(new SimpleWriterExporterOutput(baos))

    reportExportConfig.foreach( exporter.setConfiguration )
    exporterConfig.foreach( exporter.setConfiguration )

    exporter.exportReport()
    ByteString(baos.toByteArray)
  }

  override def exportReportToFile(jasperPrint: JasperPrint, destpath: String): File = {
    val exporter = new JRTextExporter(jsContext)
    exporter.setExporterInput(new SimpleExporterInput(jasperPrint))
    exporter.setExporterOutput(new SimpleWriterExporterOutput(destpath))

    reportExportConfig.foreach( exporter.setConfiguration )
    exporterConfig.foreach( exporter.setConfiguration )

    exporter.exportReport()

    new File(destpath)
  }

  def reportExportConfig: Option[TextReportConfiguration] = {
    val cfg = new SimpleTextReportConfiguration
    cfg.setPageHeightInChars(80)
    cfg.setPageWidthInChars(128)
    Some(cfg)
  }

  def exporterConfig: Option[SimpleTextExporterConfiguration] = {
    val cfg = new SimpleTextExporterConfiguration
    cfg.setLineSeparator("\n")
    cfg.setPageSeparator("----------------------------------------------------------\n")
    cfg.setTrimLineRight(true)
    Some(cfg)
  }
}
