package com.matejdro.pebblenotificationcenter.notification

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ReplaceRegexesTest {
   @Test
   fun `Do not do anything when the regex does not match`() {
      replaceRegexes("abc", listOf("def" to "ghi")) shouldBe "abc"
   }

   @Test
   fun `Replace simple text`() {
      replaceRegexes("abc", listOf("a" to "e")) shouldBe "ebc"
   }

   @Test
   fun `Replace regex with literal`() {
      replaceRegexes("abc", listOf("a.." to "e")) shouldBe "e"
   }

   @Test
   fun `Replace regex with capturing group`() {
      replaceRegexes("abc", listOf("a.(.)" to "e$1")) shouldBe "ec"
   }

   @Test
   fun `Throw error on invalid capturing groups`() {
      shouldThrow<IndexOutOfBoundsException> {
         replaceRegexes("abc", listOf("a.(.)" to "e$9")) shouldBe "a"
      }
   }

   @Test
   fun `Allow special whitespace in the literals`() {
      replaceRegexes("abc", listOf("a.." to "\\\\t\\\\r\\\\n")) shouldBe "\t\r\n"
   }
}
