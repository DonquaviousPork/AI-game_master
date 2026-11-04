package gamemaster.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.List;

/** Tiny builder for the JSON-schema object that describes a tool's arguments. */
final class Schema {

    private final JsonObject properties = new JsonObject();
    private final JsonArray required = new JsonArray();

    static Schema object() { return new Schema(); }

    Schema string(String name, String description, boolean isRequired) {
        return add(name, "string", description, isRequired, null);
    }

    Schema integer(String name, String description, boolean isRequired) {
        return add(name, "integer", description, isRequired, null);
    }

    Schema choice(String name, String description, List<String> values, boolean isRequired) {
        return add(name, "string", description, isRequired, values);
    }

    private Schema add(String name, String type, String description, boolean isRequired, List<String> values) {
        JsonObject p = new JsonObject();
        p.addProperty("type", type);
        p.addProperty("description", description);
        if (values != null) {
            JsonArray allowed = new JsonArray();
            values.forEach(allowed::add);
            p.add("enum", allowed);
        }
        properties.add(name, p);
        if (isRequired) {
            required.add(name);
        }
        return this;
    }

    JsonObject build() {
        JsonObject o = new JsonObject();
        o.addProperty("type", "object");
        o.add("properties", properties);
        o.add("required", required);
        return o;
    }
}
