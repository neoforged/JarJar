package net.neoforged.jarjar.metadata;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public final class ContainedJarMetadata {
    private final ContainedJarIdentifier identifier;
    private final ContainedVersion version;
    private final String path;
    private final boolean isObfuscated;
    private final Map<String, ?> customMetadata;

    public ContainedJarMetadata(ContainedJarIdentifier identifier, ContainedVersion version, String path, boolean isObfuscated, Map<String, ?> customMetadata) {
        this.identifier = identifier;
        this.version = version;
        this.path = path;
        this.isObfuscated = isObfuscated;
        this.customMetadata = customMetadata;
    }

    public ContainedJarMetadata(ContainedJarIdentifier identifier, ContainedVersion version, String path, boolean isObfuscated) {
        this(identifier, version, path, isObfuscated, Collections.emptyMap());
    }

    public ContainedJarIdentifier identifier() {
        return identifier;
    }

    public ContainedVersion version() {
        return version;
    }

    public String path() {
        return path;
    }

    public boolean isObfuscated() {
        return isObfuscated;
    }

    public Map<String, ?> getCustomMetadata() {
        return customMetadata;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        final ContainedJarMetadata that = (ContainedJarMetadata) obj;
        return Objects.equals(this.identifier, that.identifier) &&
                Objects.equals(this.version, that.version) &&
                Objects.equals(this.path, that.path) &&
                this.isObfuscated == that.isObfuscated &&
                Objects.equals(this.customMetadata, that.customMetadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier, version, path, isObfuscated, customMetadata);
    }

    @Override
    public String toString() {
        return "ContainedJarMetadata{" +
                "identifier=" + identifier +
                ", version=" + version +
                ", path='" + path + '\'' +
                ", isObfuscated=" + isObfuscated +
                ", customMetadata=" + customMetadata +
                '}';
    }
}
