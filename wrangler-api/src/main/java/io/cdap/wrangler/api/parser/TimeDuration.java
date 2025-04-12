package io.cdap.wrangler.api.parser;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Represents a token for time durations (e.g., 150ms, 2h).
 */
public class TimeDuration implements Token {

    private final long milliseconds;

    public TimeDuration(String value) {
        this.milliseconds = parseTimeDuration(value);
    }

    private long parseTimeDuration(String value) {
        if (value.endsWith("ms")) {
            return Long.parseLong(value.replace("ms", ""));
        } else if (value.endsWith("s")) {
            return Long.parseLong(value.replace("s", "")) * 1000;
        } else if (value.endsWith("m")) {
            return Long.parseLong(value.replace("m", "")) * 1000 * 60;
        } else if (value.endsWith("h")) {
            return Long.parseLong(value.replace("h", "")) * 1000 * 60 * 60;
        } else if (value.endsWith("d")) {
            return Long.parseLong(value.replace("d", "")) * 1000 * 60 * 60 * 24;
        } else {
            throw new IllegalArgumentException("Invalid time duration: " + value);
        }
    }

    public long getMilliseconds() {
        return milliseconds;
    }

    @Override
    public Object value() {
        return milliseconds;
    }

    @Override
    public TokenType type() {
        return TokenType.TIME_DURATION;
    }

    @Override
    public JsonElement toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("type", "TIME_DURATION");
        json.addProperty("value", milliseconds);
        return json;
    }
}