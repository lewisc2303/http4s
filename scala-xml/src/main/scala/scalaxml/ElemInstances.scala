package org.http4s
package scalaxml

import java.io.StringReader

import cats._
import cats.effect._
import headers.`Content-Type`

import scala.util.control.NonFatal
import scala.xml._

import javax.xml.parsers.SAXParserFactory

trait ElemInstances {
  protected def saxFactory: SAXParserFactory

  implicit def xmlEncoder[F[_]: Applicative](implicit charset: Charset = DefaultCharset): EntityEncoder[F, Elem] =
    EntityEncoder.stringEncoder[F]
      .contramap[Elem](xml => xml.buildString(false))
      .withContentType(`Content-Type`(MediaType.`application/xml`))

  /**
   * Handles a message body as XML.
   *
   * TODO Not an ideal implementation.  Would be much better with an asynchronous XML parser, such as Aalto.
   *
   * @return an XML element
   */
  implicit def xml[F[_]](implicit F: Sync[F]): EntityDecoder[F, Elem] = {
    import EntityDecoder._
    decodeBy(MediaType.`text/xml`, MediaType.`text/html`, MediaType.`application/xml`){ msg =>
      collectBinary(msg).flatMap[DecodeFailure, Elem] { arr =>
        val source = new InputSource(new StringReader(new String(arr.toArray, msg.charset.getOrElse(Charset.`US-ASCII`).nioCharset)))
        val saxParser = saxFactory.newSAXParser()
        try DecodeResult.success(F.pure(XML.loadXML(source, saxParser)))
        catch {
          case e: SAXParseException =>
            DecodeResult.failure(MalformedMessageBodyFailure("Invalid XML", Some(e)))
          case NonFatal(e) => DecodeResult(F.raiseError(e))
        }
      }
    }
  }
}
