/*
 * Copyright © 2017-2019 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package io.cdap.wrangler.api.parser;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.cdap.wrangler.api.annotations.PublicEvolving;

/**
 * Represents a token for byte sizes (e.g., 10KB, 1GB).
 */
@PublicEvolving
public class ByteSize implements Token {
    private final long bytes;
    private final String originalValue;

    public ByteSize(String value) {
        this.originalValue = value;
        this.bytes = parseByteSize(value);
    }

    private long parseByteSize(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Byte size cannot be null or empty");
        }

        // Normalize input by removing all whitespace and converting to uppercase
        value = value.replaceAll("\\s+", "").toUpperCase();

        // Extract numeric part and unit part using regex that handles decimals
        String[] parts = value.split("(?<=\\d)(?=B|K|M|G|T)");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid byte size format: " + value);
        }

        // Parse the numeric part, allowing for decimals
        double numericValue;
        try {
            numericValue = Double.parseDouble(parts[0]);
            if (numericValue < 0) {
                throw new IllegalArgumentException("Byte size cannot be negative: " + value);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid numeric value: " + parts[0]);
        }

        // Convert based on unit
        switch (parts[1]) {
            case "B":
                return (long) numericValue;
            case "KB":
                return (long) (numericValue * 1024);
            case "MB":
                return (long) (numericValue * 1024 * 1024); 
            case "GB":
                return (long) (numericValue * 1024 * 1024 * 1024);
            case "TB":
                return (long) (numericValue * 1024L * 1024 * 1024 * 1024);
            default:
                throw new IllegalArgumentException("Invalid byte size unit: " + parts[1]);
        }
    }

    /**
     * Returns the size in bytes.
     *
     * @return size in bytes 
     */
    public long getBytes() {
        return bytes;
    }

    @Override
    public Object value() {
        return bytes;
    }

    @Override
    public TokenType type() {
        return TokenType.BYTE_SIZE;
    }

    @Override
    public JsonElement toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("type", "BYTE_SIZE");
        json.addProperty("value", originalValue);
        json.addProperty("bytes", bytes);
        return json;
    }
}
