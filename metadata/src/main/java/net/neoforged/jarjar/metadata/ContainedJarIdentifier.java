package net.neoforged.jarjar.metadata;

import java.util.Objects;
import java.util.Optional;

public final class ContainedJarIdentifier {
    private final String group;
    private final String artifact;
    // optional
    private final String classifier;

    public ContainedJarIdentifier(String group, String artifact) {
        this(group, artifact, null);
    }

    public ContainedJarIdentifier(String group, String artifact, String classifier) {
        this.group = group;
        this.artifact = artifact;
        this.classifier = classifier;
    }

    public String group() {
        return group;
    }

    public String artifact() {
        return artifact;
    }

    public Optional<String> classifier() {
        return Optional.ofNullable(classifier);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        final ContainedJarIdentifier that = (ContainedJarIdentifier) obj;
        return Objects.equals(this.group, that.group) &&
            Objects.equals(this.artifact, that.artifact) &&
            Objects.equals(this.classifier, that.classifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(group, artifact, classifier);
    }

    @Override
    public String toString() {
        return "ContainedJarIdentifier[" +
            "group=" + group + ", " +
            "artifact=" + artifact + ", " +
            "classifier=" + classifier + ']';
    }
}
