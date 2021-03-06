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

import org.http4s.headers.`Content-Type`
import org.specs2.mutable.Specification

class ContentTypeHeaderSpec extends Specification with HeaderParserHelper[`Content-Type`] {
  def hparse(value: String): ParseResult[`Content-Type`] =
    `Content-Type`.parse(value)

  def simple = `Content-Type`(MediaType.text.html)
  def charset = `Content-Type`(MediaType.text.html, Charset.`UTF-8`)
  def extensions = `Content-Type`(MediaType.text.html.withExtensions(Map("foo" -> "bar")))
  def extensionsandset =
    `Content-Type`(MediaType.text.html.withExtensions(Map("foo" -> "bar")), Charset.`UTF-8`)
  def multipart =
    `Content-Type`(
      MediaType.multipart.`form-data`.withExtensions(Map("boundary" -> "aLotOfMoose")),
      Charset.`UTF-8`)

  "ContentType Header" should {
    "Generate the correct values" in {
      simple.value must be_==("text/html")
      charset.value must be_==("""text/html; charset=UTF-8""")
      extensions.value must be_==("""text/html; foo="bar"""")
      extensionsandset.value must be_==("""text/html; foo="bar"; charset=UTF-8""")
      multipart.value must be_==("""multipart/form-data; boundary="aLotOfMoose"; charset=UTF-8""")
    }

    "Parse correctly" in {
      parse(simple.value) must be_==(simple)
      parse(charset.value) must be_==(charset)
      parse(extensions.value) must be_==(extensions)
      parse(extensionsandset.value) must be_==(extensionsandset)
      parse(multipart.value) must be_==(multipart)
    }
  }
}
