package io.cdap.wrangler.api.parser;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Represents a token for byte sizes (e.g., 10KB, 1GB).
 */
public class ByteSize implements Token {

    private final long bytes;

    public ByteSize(String value) {
        this.bytes = parseByteSize(value);
    }

    private long parseByteSize(String value) {
        if (value.endsWith("KB")) {
            return Long.parseLong(value.replace("KB", "")) * 1024;
        } else if (value.endsWith("MB")) {
            return Long.parseLong(value.replace("MB", "")) * 1024 * 1024;
        } else if (value.endsWith("GB")) {
            return Long.parseLong(value.replace("GB", "")) * 1024 * 1024 * 1024;
        } else if (value.endsWith("TB")) {
            return Long.parseLong(value.replace("TB", "")) * 1024L * 1024 * 1024 * 1024;
        } else if (value.endsWith("B")) {
            return Long.parseLong(value.replace("B", ""));
        } else {
            throw new IllegalArgumentException("Invalid byte size: " + value);
        }
    }

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
        json.addProperty("value", bytes);
        return json;
    }
}