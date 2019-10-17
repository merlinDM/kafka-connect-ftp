package com.gd

import java.util

import com.eneco.trading.kafka.connect.ftp.source.SourceRecordConverter
import com.typesafe.scalalogging.StrictLogging
import org.apache.kafka.connect.data.Schema
import org.apache.kafka.connect.source.SourceRecord
import org.json4s._
import org.json4s.jackson.JsonMethods._

import scala.util.Try

// to many implicits in scope
import scala.collection.JavaConverters.seqAsJavaListConverter

class JsonSourceRecordConverter extends SourceRecordConverter with StrictLogging {

  private val keyField: String = "ip"

  override def convert(in: SourceRecord): util.List[SourceRecord] = {

    val body: String = in.valueSchema() match {
      case Schema.BYTES_SCHEMA =>
        val bytes = in.value().asInstanceOf[Array[Byte]]

        new String(bytes)
      case Schema.STRING_SCHEMA =>

        in.value().asInstanceOf[String]
      case _ =>

        ""
    }

    val lines = body.split("\\n")

    val arr: Array[SourceRecord] = lines.flatMap { line =>
      logger.debug(s"Parsing line: $line")

      Try(parse(line)).toOption.flatMap { parsed =>
        logger.trace(s"Successfully parsed line: $parsed")

        (parsed \ keyField).toSome map { jsonKey =>
          val key: String = compact(render(jsonKey))

          new SourceRecord(
            in.sourcePartition(),
            in.sourceOffset(),
            in.topic(),
            Schema.STRING_SCHEMA,
            key,
            Schema.BYTES_SCHEMA,
            line.getBytes)
        }
      }
    }

    val javaList: util.List[SourceRecord] = seqAsJavaListConverter(arr).asJava

    javaList
  }

  // TODO: set keyField value from configs
  override def configure(configs: util.Map[String, _]): Unit = {}
}
