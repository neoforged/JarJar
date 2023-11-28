package net.neoforged.jarjar.metadata.json;

import com.google.common.reflect.TypeToken;
import com.google.gson.*;
import net.neoforged.jarjar.metadata.ContainedJarIdentifier;
import net.neoforged.jarjar.metadata.ContainedJarMetadata;
import net.neoforged.jarjar.metadata.ContainedVersion;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;

public class ContainedJarMetadataSerializer implements JsonSerializer<ContainedJarMetadata>, JsonDeserializer<ContainedJarMetadata>
{
    @Override
    public ContainedJarMetadata deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) throws JsonParseException
    {
        if (!json.isJsonObject())
            throw new JsonParseException("Expected object");

        final JsonObject jsonObject = json.getAsJsonObject();
        final ContainedJarIdentifier containedJarIdentifier = context.deserialize(jsonObject.get("identifier"), ContainedJarIdentifier.class);
        final ContainedVersion version = context.deserialize(jsonObject.get("version"), ContainedVersion.class);
        final String path = jsonObject.get("path").getAsString();
        boolean isObfuscated = false;
        if (jsonObject.has("isObfuscated"))
            isObfuscated = jsonObject.get("isObfuscated").getAsBoolean();

        Map<String, ?> customMetadata = Collections.emptyMap();
        if (jsonObject.has("customMetadata")) {
            context.deserialize(jsonObject.get("customMetadata"), new TypeToken<Map<String, ?>>() {}.getType());
        }

        return new ContainedJarMetadata(containedJarIdentifier, version, path, isObfuscated, customMetadata);
    }

    @Override
    public JsonElement serialize(final ContainedJarMetadata src, final Type typeOfSrc, final JsonSerializationContext context)
    {
        final JsonObject jsonObject = new JsonObject();
        jsonObject.add("identifier", context.serialize(src.identifier()));
        jsonObject.add("version", context.serialize(src.version()));
        jsonObject.add("path", new JsonPrimitive(src.path()));
        jsonObject.add("isObfuscated", new JsonPrimitive(src.isObfuscated()));
        jsonObject.add("customMetadata", context.serialize(src.getCustomMetadata()));
        return jsonObject;
    }
}
