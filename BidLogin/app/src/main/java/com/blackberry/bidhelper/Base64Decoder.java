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

/**
 * A platform-independent interface for simple Base64 decoding, needed
 * due to Java's lack of a cross-platform Base64 facility.
 */
interface Base64Decoder {
    /**
     * Returns the bytes which have been decoded from the
     * specified Base64 string, or null if a decoding error
     * occurred.
     *
     * @param s Base64 string to be decoded.
     * @return the bytes which have been decoded from the
     * specified Base64 string, or null if a decoding error
     * occurred.
     */
    public byte[] decode(String s);
}