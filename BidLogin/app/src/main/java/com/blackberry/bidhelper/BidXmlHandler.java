/* Copyright (c) 2011-2016 BlackBerry Limited.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.blackberry.bidhelper;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.DefaultHandler;

import java.util.Stack;

/**
 * Base functionality for platform-independent XML parsing of BID reports.
 * This class attempts to ease the pain of the antiquated and awkward SAX
 * parser interface. The reason for relying on SAX is because it is
 * cross-platform and because the DOM parser would be overkill for BIDE
 * reports.
 */
abstract class BidXmlHandler extends DefaultHandler {
    /**
     * Bit flag for the JBIDE report.
     */
    public static final int JBIDE_XML_TYPE = 1 << 0;

    /**
     * Bit flag for the KBIDE report.
     */
    public static final int KBIDE_XML_TYPE = 1 << 1;

    /**
     * Bit flag for the TZ report.
     */
    public static final int TZ_XML_TYPE = 1 << 2;

    /**
     * Bit flag for the TZ report signature.
     */
    public static final int TZ_SIG_XML_TYPE = 1 << 3;

    private int xmlType;
    private Stack<String> tagNames;
    private Stack<AttributesImpl> attributes;

    /**
     * Creates a new <code>BidXmlHandler</code>.
     */
    BidXmlHandler() {
        tagNames = new Stack<String>();
        attributes = new Stack<AttributesImpl>();
    }

    /**
     * Sets the XML type currently being processed.
     *
     * @param xmlType one of the following values:
     *                <ul>
     *                <li><code>JBIDE_XML_TYPE</code></li>
     *                <li><code>KBIDE_XML_TYPE</code></li>
     *                <li><code>TZ_XML_TYPE</code></li>
     *                <li><code>TZ_SIG_XML_TYPE</code></li>
     *                </ul>
     */
    final void setXmlType(int xmlType) {
        this.xmlType = xmlType;
    }

    /**
     * Pushes the specified tag name and corresponding
     * attributes. To be called from <code>startElement</code>.
     *
     * @param tagName the name of the element.
     * @param atts    the corresponding attributes.
     */
    final void push(String tagName, Attributes atts) {
        tagNames.push(tagName);
        attributes.push(new AttributesImpl(atts));
    }

    /**
     * Pops the tag name and attributes from the top of
     * the stack. To be called from <code>endElement</code>.
     */
    final void pop() {
        tagNames.pop();
        attributes.pop();
    }

    /**
     * Returns <code>true</code> if the given type flags and tag name
     * match what is currently being processed, <code>false</code>
     * otherwise. Multiple types can be combined to effect a logical
     * disjunction.
     *
     * @param typeFlags a bitwise combination of the following values:
     *                  <ul>
     *                  <li><code>JBIDE_XML_TYPE</code></li>
     *                  <li><code>KBIDE_XML_TYPE</code></li>
     *                  <li><code>TZ_XML_TYPE</code></li>
     *                  <li><code>TZ_SIG_XML_TYPE</code></li>
     *                  </ul>
     * @param tagName   the name of the element.
     * @return <code>true</code> if the given type flags and tag name
     * match what is currently being processed, <code>false</code>
     * otherwise.
     */
    public final boolean isTypeAndTag(int typeFlags, String tagName) {
        String head = tagNames.peek();
        return (typeFlags & xmlType) != 0 && tagName.equals(head);
    }

    /**
     * Returns <code>true</code> if the given type flags, tag name,
     * and parent name match what is currently being processed,
     * <code>false</code> otherwise. Multiple types can be combined
     * to effect a logical disjunction.
     * <p/>
     * Returns <code>false</code> if the depth of element nesting is
     * less than 2.
     *
     * @param typeFlags  a bitwise combination of the following values:
     *                   <ul>
     *                   <li><code>JBIDE_XML_TYPE</code></li>
     *                   <li><code>KBIDE_XML_TYPE</code></li>
     *                   <li><code>TZ_XML_TYPE</code></li>
     *                   <li><code>TZ_SIG_XML_TYPE</code></li>
     *                   </ul>
     * @param tagName    the name of the current element.
     * @param parentName the name of the parent element.
     * @return <code>true</code> if the given type flags, tag name,
     * and parent name match what is currently being processed,
     * <code>false</code> otherwise.
     */
    public final boolean isTypeTagAndParent(int typeFlags, String tagName, String parentName) {
        int size = tagNames.size();
        boolean matchingParent = size > 1 && parentName.equals(tagNames.get(size - 2));
        return isTypeAndTag(typeFlags, tagName) && matchingParent;
    }

    /**
     * Returns the attributes corresponding to the current element.
     *
     * @return the attributes corresponding to the current element.
     */
    public final Attributes getAttributes() {
        return attributes.peek();
    }

    /**
     * Called when processing begins for the set of BID XML documents.
     *
     * @throws org.xml.sax.SAXException if a parsing error occurs.
     */
    public abstract void startDocumentSet() throws SAXException;

    /**
     * Called when processing ends for the set of BID XML documents.
     *
     * @throws org.xml.sax.SAXException if a parsing error occurs.
     */
    public abstract void endDocumentSet() throws SAXException;
}