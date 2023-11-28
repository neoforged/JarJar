package net.neoforged.jarjar.selection;

import net.neoforged.jarjar.metadata.ContainedJarMetadata;

import java.util.Objects;

public final class JarSelectionResult<T> {
    private final T result;
    private final ContainedJarMetadata metadata;

    JarSelectionResult(T result, ContainedJarMetadata metadata) {
        this.result = result;
        this.metadata = metadata;
    }

    public T getResult() {
        return result;
    }

    public ContainedJarMetadata getMetadata() {
        return metadata;
    }

    @Override
    public String toString() {
        return "SelectionResult{" +
                "result=" + result +
                ", metadata=" + metadata +
                '}';
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        JarSelectionResult<?> that = (JarSelectionResult<?>) object;
        return Objects.equals(result, that.result) && Objects.equals(metadata, that.metadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(result, metadata);
    }
}
