package com.gd

import java.util

import org.apache.kafka.connect.data.Schema
import org.apache.kafka.connect.source.SourceRecord
import org.scalatest._

import scala.collection.mutable

class JsonSourceRecordConverterTest extends FlatSpec {

  behavior of "JsonSourceRecordConverterTest"

  private def fixture = new {

    // this is not valid json
    val input: String = """
                  |{"type": "click", "ip": "127.0.0.1", "event_time": 1571173944, "url": "https://example-054.com/"}
                  |{"type": "click", "ip": "127.0.0.1", "event_time": 1571173944, "url": "https://example-018.com/"}
                  |{"type": "click", "ip": "127.0.0.1", "event_time": 1571173944, "url": "https://example-069.com/"}
                  |{"type": "click",}
                  |{"type": "click", "ip": "127.0.0.1", "event_time": 1571173944, "url": "https://example-006.com/"}
                  |{"type": "click", "ip": "127.0.0.1", "event_time": 1571173944, "url": "https://example-016.com/"}
                  |{"type": "click", "ip": "127.0.0.1", "event_time": 1571173944, "url": "https://example-051.com/"}
                  |""".stripMargin

    val record = new SourceRecord(
      null, // source part
      null, // source off
      "topic", //topic
      Schema.STRING_SCHEMA, // key sch
      "key", // key
      Schema.BYTES_SCHEMA, // val sch
      input.getBytes // val
    )

    val configs = new util.HashMap[String, String]()

  }

  it should "skip empty and erroneous rows" in {
    val f = fixture
    val converter = new JsonSourceRecordConverter()
    converter.configure(f.configs)
    val res = converter.convert(f.record)

    assert(res.size() == 6)
  }

  it should "preserve key and value data types" in {
    import scala.collection.JavaConverters._

    val f = fixture
    val converter = new JsonSourceRecordConverter()
    converter.configure(f.configs)
    val res: mutable.Seq[SourceRecord] = converter.convert(f.record).asScala

    res.foreach(record => {
      assert(record.keySchema() == Schema.STRING_SCHEMA)
      assert(record.key().isInstanceOf[String])
      assert(record.valueSchema() == Schema.BYTES_SCHEMA)
      assert(record.value().isInstanceOf[Array[Byte]])
    })
  }

}
