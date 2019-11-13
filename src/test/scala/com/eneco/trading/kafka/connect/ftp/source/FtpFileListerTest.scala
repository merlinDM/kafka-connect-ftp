package com.eneco.trading.kafka.connect.ftp.source

import com.typesafe.scalalogging.StrictLogging
import org.apache.commons.net.ftp.{FTPClient, FTPFile}
import org.scalatest.mockito.MockitoSugar
import org.mockito.Mockito._
import org.scalatest.{BeforeAndAfter, FunSuite, Matchers}

/**
  * Created by jhofman on 13/03/2017.
  */
class FtpFileListerTest extends FunSuite with Matchers with BeforeAndAfter with StrictLogging with MockitoSugar {

  def mockFile(name: String) = {
    val f = mock[FTPFile]
    when(f.isFile).thenReturn(true)
    when(f.isDirectory).thenReturn(false)
    when(f.getName()).thenReturn(name)
    f
  }

  def mockDir(name: String) = {
    val f = mock[FTPFile]
    when(f.isFile).thenReturn(false)
    when(f.isDirectory).thenReturn(true)
    when(f.getName()).thenReturn(name)
    f
  }

  test("Fixed path") {
    val ftp = mock[FTPClient]
    val file = mockFile("file.txt")
    val thisDir = mockDir(".")
    val parentDir = mockDir("..")

    when(ftp.listFiles("/a/path/")).thenReturn(Array[FTPFile](file, thisDir, parentDir))

    FtpFileLister(ftp).listFiles("/a/path/file.txt").toList should contain theSameElementsAs List(
      AbsoluteFtpFile(file, "/a/path/")
    )}

  test("Glob path") {
    val ftp = mock[FTPClient]

    /**
      * /a
      *   /dira
      *        /path/file1.txt
      *   /dirb
      *       /nopath/file2.txt
      *       /path/file3.txt
      *       /path/file4.csv
      */

    val dira = mockDir("dira")
    val dirb = mockDir("dirb")
    val thisDir = mockDir(".")
    val parentDir = mockDir("..")
    when(ftp.listFiles("/a/")).thenReturn(Array[FTPFile](dira, dirb, thisDir, parentDir))

    val path = mockDir("path")
    when(ftp.listFiles("/a/dira/")).thenReturn(Array[FTPFile](path, thisDir, parentDir))

    val file1 = mockFile("file1.txt")
    when(ftp.listFiles("/a/dira/path/")).thenReturn(Array[FTPFile](file1, thisDir, parentDir))

    val nopath = mockDir("nopath")
    when(ftp.listFiles("/a/dirb/")).thenReturn(Array[FTPFile](nopath, path, thisDir, parentDir))
    when(ftp.listFiles("/a/dirb/nopath/")).thenThrow(new RuntimeException("Should not list this directory"))

    val file3 = mockFile("file3.txt")
    val file4 = mockFile("file4.csv")
    when(ftp.listFiles("/a/dirb/path/")).thenReturn(Array[FTPFile](file3, file4, thisDir, parentDir))

    FtpFileLister(ftp).listFiles("/a/dir?/path/*.txt").toList should contain theSameElementsAs Seq(
      AbsoluteFtpFile(file1, "/a/dira/path/"),
      AbsoluteFtpFile(file3, "/a/dirb/path/")
    )
  }
}
