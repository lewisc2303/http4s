/*
 * Copyright 2013 http4s.org
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

package org.http4s
package parser

import cats.data.NonEmptyList

import java.net.InetAddress
import org.http4s.headers._
import org.http4s.EntityTag.{Strong, Weak}
import org.typelevel.ci.CIString

class SimpleHeadersSpec extends Http4sSpec {
  "SimpleHeaders" should {
    "parse Accept-Patch" in {
      val header =
        `Accept-Patch`(
          NonEmptyList.of(new MediaType("text", "example", extensions = Map("charset" -> "utf-8"))))
      HttpHeaderParser.parseHeader(header.toRaw) must beRight(header)
      val multipleMediaTypes =
        `Accept-Patch`(
          NonEmptyList
            .of(new MediaType("application", "example"), new MediaType("text", "example")))
      HttpHeaderParser.parseHeader(multipleMediaTypes.toRaw) must beRight(multipleMediaTypes)

      val bad = Header(header.name.toString, "foo; bar")
      HttpHeaderParser.parseHeader(bad) must beLeft
    }

    "parse Access-Control-Allow-Headers" in {
      val header = `Access-Control-Allow-Headers`(
        NonEmptyList.of(
          CIString("Accept"),
          CIString("Expires"),
          CIString("X-Custom-Header"),
          CIString("*")
        )
      )
      HttpHeaderParser.parseHeader(header.toRaw) must beRight(header)

      val invalidHeader = Header(header.name.toString, "(non-token-name), non[&token]name")
      HttpHeaderParser.parseHeader(invalidHeader) must beLeft
    }

    "parse Access-Control-Expose-Headers" in {
      val header = `Access-Control-Expose-Headers`(
        NonEmptyList.of(
          CIString("Content-Length"),
          CIString("Authorization"),
          CIString("X-Custom-Header"),
          CIString("*")
        )
      )
      HttpHeaderParser.parseHeader(header.toRaw) must beRight(header)

      val invalidHeader = Header(header.name.toString, "(non-token-name), non[&token]name")
      HttpHeaderParser.parseHeader(invalidHeader) must beLeft
    }

    "parse Connection" in {
      val header = Connection(CIString("closed"))
      HttpHeaderParser.parseHeader(header.toRaw) must beRight(header)
    }

    "parse Content-Length" in {
      val header = `Content-Length`.unsafeFromLong(4)
      HttpHeaderParser.parseHeader(header.toRaw) must beRight(header)

      val bad = Header(header.name.toString, "foo")
      HttpHeaderParser.parseHeader(bad) must beLeft
    }

    "parse Content-Encoding" in {
      val header = `Content-Encoding`(ContentCoding.`pack200-gzip`)
      HttpHeaderParser.parseHeader(header.toRaw) must beRight(header)
    }

    "parse Content-Disposition" in {
      val header = `Content-Disposition`("foo", Map("one" -> "two", "three" -> "four"))
      HttpHeaderParser.parseHeader(header.toRaw) must beRight(header)

      val bad = Header(header.name.toString, "foo; bar")
      HttpHeaderParser.parseHeader(bad) must beLeft
    }

    "parse Date" in { // mills are lost, get rid of them
      val header = Date(HttpDate.Epoch).toRaw.parsed
      HttpHeaderParser.parseHeader(header.toRaw) must beRight(header)

      val bad = Header(header.name.toString, "foo")
      HttpHeaderParser.parseHeader(bad) must beLeft
    }

    "parse Host" in {
      val header1 = Host("foo", Some(5))
      HttpHeaderParser.parseHeader(header1.toRaw) must beRight(header1)

      val header2 = Host("foo", None)
      HttpHeaderParser.parseHeader(header2.toRaw) must beRight(header2)

      val bad = Header(header1.name.toString, "foo:bar")
      HttpHeaderParser.parseHeader(bad) must beLeft
    }

    "parse Access-Control-Allow-Credentials" in {
      val header = `Access-Control-Allow-Credentials`().toRaw.parsed
      HttpHeaderParser.parseHeader(header.toRaw) must beRight(header)

      val bad = Header(header.name.toString, "false")
      HttpHeaderParser.parseHeader(bad) must beLeft
      // it is case sensitive
      val bad2 = Header(header.name.toString, "True")
      HttpHeaderParser.parseHeader(bad2) must beLeft
    }

    "parse Last-Modified" in {
      val header = `Last-Modified`(HttpDate.Epoch).toRaw.parsed
      HttpHeaderParser.parseHeader(header.toRaw) must beRight(header)

      val bad = Header(header.name.toString, "foo")
      HttpHeaderParser.parseHeader(bad) must beLeft
    }

    "parse If-Modified-Since" in {
      val header = `If-Modified-Since`(HttpDate.Epoch).toRaw.parsed
      HttpHeaderParser.parseHeader(header.toRaw) must beRight(header)

      val bad = Header(header.name.toString, "foo")
      HttpHeaderParser.parseHeader(bad) must beLeft
    }

    "parse ETag" in {
      EntityTag("hash", Weak).toString() must_== "W/\"hash\""
      EntityTag("hash", Strong).toString() must_== "\"hash\""

      val headers = Seq(ETag("hash"), ETag("hash", Weak))

      foreach(headers) { header =>
        HttpHeaderParser.parseHeader(header.toRaw) must beRight(header)
      }
    }

    "parse If-None-Match" in {
      val headers = Seq(
        `If-None-Match`(EntityTag("hash")),
        `If-None-Match`(EntityTag("123-999")),
        `If-None-Match`(EntityTag("123-999"), EntityTag("hash")),
        `If-None-Match`(EntityTag("123-999", Weak), EntityTag("hash")),
        `If-None-Match`.`*`
      )
      foreach(headers) { header =>
        HttpHeaderParser.parseHeader(header.toRaw) must beRight(header)
      }
    }

    "parse Max-Forwards" in {
      val headers = Seq(
        `Max-Forwards`.unsafeFromLong(0),
        `Max-Forwards`.unsafeFromLong(100)
      )
      foreach(headers) { header =>
        HttpHeaderParser.parseHeader(header.toRaw) must beRight(header)
      }
    }

    "parse Transfer-Encoding" in {
      val header = `Transfer-Encoding`(TransferCoding.chunked)
      HttpHeaderParser.parseHeader(header.toRaw) must beRight(header)

      val header2 = `Transfer-Encoding`(TransferCoding.compress)
      HttpHeaderParser.parseHeader(header2.toRaw) must beRight(header2)
    }

    "parse User-Agent" in {
      val header = `User-Agent`(ProductId("foo", Some("bar")), List(ProductComment("foo")))
      header.value must_== "foo/bar (foo)"

      HttpHeaderParser.parseHeader(header.toRaw) must beRight(header)

      val header2 =
        `User-Agent`(
          ProductId("foo", None),
          List(ProductId("bar", Some("biz")), ProductComment("blah")))
      header2.value must_== "foo bar/biz (blah)"
      HttpHeaderParser.parseHeader(header2.toRaw) must beRight(header2)

      val headerstr = "Mozilla/5.0 (Android; Mobile; rv:30.0) Gecko/30.0 Firefox/30.0"
      val parsed = HttpHeaderParser.parseHeader(Header.Raw(`User-Agent`.name, headerstr))
      parsed must beRight(
        `User-Agent`(
          ProductId("Mozilla", Some("5.0")),
          List(
            ProductComment("Android; Mobile; rv:30.0"),
            ProductId("Gecko", Some("30.0")),
            ProductId("Firefox", Some("30.0"))
          )
        )
      )
      parsed.map(_.value) must_== Right(headerstr)
    }

    "parse Server" in {
      val header = Server(ProductId("foo", Some("bar")), List(ProductComment("foo")))
      header.value must_== "foo/bar (foo)"

      HttpHeaderParser.parseHeader(header.toRaw) must beRight(header)

      val header2 =
        Server(ProductId("foo"), List(ProductId("bar", Some("biz")), ProductComment("blah")))
      header2.value must_== "foo bar/biz (blah)"
      HttpHeaderParser.parseHeader(header2.toRaw) must beRight(header2)

      val headerstr = "nginx/1.14.0 (Ubuntu)"
      HttpHeaderParser.parseHeader(Header.Raw(Server.name, headerstr)) must beRight(
        Server(
          ProductId("nginx", Some("1.14.0")),
          List(
            ProductComment("Ubuntu")
          )
        )
      )

      val headerstr2 = "CERN/3.0 libwww/2.17"
      HttpHeaderParser.parseHeader(Header.Raw(Server.name, headerstr2)) must beRight(
        Server(
          ProductId("CERN", Some("3.0")),
          List(
            ProductId("libwww", Some("2.17"))
          )
        )
      )
    }

    "parse X-Forwarded-For" in {
      // ipv4
      val header2 = `X-Forwarded-For`(
        NonEmptyList.of(Some(InetAddress.getLocalHost), Some(InetAddress.getLoopbackAddress)))
      HttpHeaderParser.parseHeader(header2.toRaw) must beRight(header2)

      // ipv6
      val header3 = `X-Forwarded-For`(
        NonEmptyList.of(
          Some(InetAddress.getByName("::1")),
          Some(InetAddress.getByName("2001:0db8:85a3:0000:0000:8a2e:0370:7334"))))
      HttpHeaderParser.parseHeader(header3.toRaw) must beRight(header3)

      // "unknown"
      val header4 = `X-Forwarded-For`(NonEmptyList.of(None))
      HttpHeaderParser.parseHeader(header4.toRaw) must beRight(header4)

      val bad = Header("x-forwarded-for", "foo")
      HttpHeaderParser.parseHeader(bad) must beLeft

      val bad2 = Header("x-forwarded-for", "256.56.56.56")
      HttpHeaderParser.parseHeader(bad2) must beLeft
    }
  }
}
