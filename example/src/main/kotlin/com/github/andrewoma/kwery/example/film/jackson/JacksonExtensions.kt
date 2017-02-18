/*
 * Copyright (c) 2015 Andrew O'Malley
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.github.andrewoma.kwery.example.film.jackson

import com.fasterxml.jackson.annotation.JsonFilter
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonObjectFormatVisitor
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.ser.PropertyFilter
import com.fasterxml.jackson.databind.ser.PropertyWriter
import com.github.andrewoma.kwery.example.film.model.AttributeSet
import com.github.andrewoma.kwery.example.film.model.HasAttributeSet
import java.net.URL

inline fun <reified T : Any> ObjectMapper.withObjectStream(url: URL, f: (Sequence<T>) -> Unit) {
    val parser = this.factory.createParser(url)
    check(parser.nextToken() == JsonToken.START_ARRAY) { "Expected an array" }
    check(parser.nextToken() == JsonToken.START_OBJECT) { "Expected an object" }

    try {
        val iterator = this.readValues<T>(parser, T::class.java)
        f(object : Sequence<T> {
            override fun iterator() = iterator
        })
    } finally {
        parser.close()
    }
}

// TODO ... sort out how to handle deserialisation of unspecified values into non-null fields

class AttributeSetFilter : PropertyFilter {
    override fun serializeAsField(pojo: Any, generator: JsonGenerator, provider: SerializerProvider, writer: PropertyWriter) {
        val partial = pojo as HasAttributeSet
        if (partial.attributeSet() == AttributeSet.All || writer.name.equals("id")) {
            writer.serializeAsField(pojo, generator, provider)
        } else {
            writer.serializeAsOmittedField(pojo, generator, provider)
        }
    }

    override fun serializeAsElement(p0: Any?, p1: JsonGenerator?, p2: SerializerProvider?, p3: PropertyWriter?) {
        throw UnsupportedOperationException()
    }

    override fun depositSchemaProperty(p0: PropertyWriter?, p1: ObjectNode?, p2: SerializerProvider?) {
        throw UnsupportedOperationException()
    }

    override fun depositSchemaProperty(p0: PropertyWriter?, p1: JsonObjectFormatVisitor?, p2: SerializerProvider?) {
        throw UnsupportedOperationException()
    }
}

@JsonFilter("Attribute set filter")
class AttributeSetFilterMixIn
