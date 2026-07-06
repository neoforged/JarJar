package net.neoforged.jarjar.selector;

import com.google.common.collect.ImmutableList;
import net.neoforged.jarjar.selection.JarSelector;
import net.neoforged.jarjar.metadata.*;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class JarSelectorTest {

    @Test
    public void doesSelectRestrictedRange() throws InvalidVersionSpecificationException {
        final List<SelectionSource> sources = new ArrayList<>();
        sources.add(createSource("outer_lower_version", createArtifact("[1.0.0,)", "1.0.0"), "test.one"));
        sources.add(createSource("outer_higher_version", createArtifact("[1.0.1,)", "1.0.1"), "test.one"));

        final List<SelectionSource> selectedSources = JarSelector.detectAndSelect(
                sources,
                SelectionSource::getResource,
                SelectionSource::getInternal,
                SelectionSource::getName,
                (Function<Collection<JarSelector.ResolutionFailureInformation<SelectionSource>>, IllegalStateException>) resolutionFailureInformations -> {
                    throw new IllegalStateException("Failed");
                }
        );

        Assertions.assertEquals(1, selectedSources.size());
    }

    @Test
    public void twoExactSameInnersResolvesToOne() throws InvalidVersionSpecificationException {
        final List<SelectionSource> sources = new ArrayList<>();
        sources.add(createSource("outer_one", createArtifact("[1.0.0,)", "1.0.0"), "test.one"));
        sources.add(createSource("outer_two", createArtifact("[1.0.0,)", "1.0.0"), "test.one"));

        final List<SelectionSource> selectedSources = JarSelector.detectAndSelect(
                sources,
                SelectionSource::getResource,
                SelectionSource::getInternal,
                SelectionSource::getName,
                (Function<Collection<JarSelector.ResolutionFailureInformation<SelectionSource>>, IllegalStateException>) resolutionFailureInformations -> {
                    throw new IllegalStateException("Failed");
                }
        );

        Assertions.assertEquals(1, selectedSources.size());
    }

    @Test
    public void doesSelectThrowFailureIfNotUniteable() throws InvalidVersionSpecificationException {
        final List<SelectionSource> sources = new ArrayList<>();
        sources.add(createSource("outer_lower_version", createArtifact("[1.0.0,1.5.0)", "1.0.0"), "test.one"));
        sources.add(createSource("outer_higher_version", createArtifact("[2.0.1,)", "1.0.1"), "test.one"));

        Assertions.assertThrows(IllegalStateException.class, () -> {
            JarSelector.detectAndSelect(
                    sources,
                    SelectionSource::getResource,
                    SelectionSource::getInternal,
                    SelectionSource::getName,
                    (Function<Collection<JarSelector.ResolutionFailureInformation<SelectionSource>>, IllegalStateException>) resolutionFailureInformations -> {
                        throw new IllegalStateException("Failed");
                    }
            );
        });
    }

    @Test
    public void doesSelectThrowFailureIfNoArtifactFound() throws InvalidVersionSpecificationException {
        final List<SelectionSource> sources = new ArrayList<>();
        sources.add(createSource("outer_lower_version", createArtifact("[1.0.0,1.5.0)", "1.0.0"), "test.one"));
        sources.add(createSource("outer_higher_version", createArtifact("[1.4.0,1.6.0)", "1.5.5"), "test.one"));

        Assertions.assertThrows(IllegalStateException.class, () -> {
            JarSelector.detectAndSelect(
                    sources,
                    SelectionSource::getResource,
                    SelectionSource::getInternal,
                    SelectionSource::getName,
                    (Function<Collection<JarSelector.ResolutionFailureInformation<SelectionSource>>, IllegalStateException>) resolutionFailureInformations -> {
                        throw new IllegalStateException("Failed");
                    }
            );
        });
    }

    @Test
    public void selectSelectsNothingWhenNoInput() {
        final List<SelectionSource> sources = new ArrayList<>();

        final List<SelectionSource> selectedSources = JarSelector.detectAndSelect(
                sources,
                SelectionSource::getResource,
                SelectionSource::getInternal,
                SelectionSource::getName,
                (Function<Collection<JarSelector.ResolutionFailureInformation<SelectionSource>>, IllegalStateException>) resolutionFailureInformations -> {
                    throw new IllegalStateException("Failed");
                }
        );

        Assertions.assertEquals(0, selectedSources.size());
    }

    @Test
    public void doesSelectSelectAllDifferentArtifacts() throws InvalidVersionSpecificationException {
        final List<SelectionSource> sources = new ArrayList<>();
        sources.add(createSource("outer_lower_version", createArtifact("[1.0.0,)", "1.0.0"), "test.one"));
        sources.add(createSource("outer_higher_version", createArtifact("test.two", "[1.0.1,)", "1.0.1"), "test.two"));

        final List<SelectionSource> selectedSources = JarSelector.detectAndSelect(
                sources,
                SelectionSource::getResource,
                SelectionSource::getInternal,
                SelectionSource::getName,
                (Function<Collection<JarSelector.ResolutionFailureInformation<SelectionSource>>, IllegalStateException>) resolutionFailureInformations -> {
                    throw new IllegalStateException("Failed");
                }
        );

        Assertions.assertEquals(2, selectedSources.size());
    }

    @Test
    public void doesSelectAllDifferentClassifiers() throws InvalidVersionSpecificationException {
        final List<SelectionSource> sources = new ArrayList<>();
        sources.add(createSource("outer_client_classifier", createArtifact("test.one", "artifact", "client", "[1.0.0,1.5.0)", "1.0.0", "test.one-client"), "test.one-client"));
        sources.add(createSource("outer_server_classifier", createArtifact("test.one", "artifact", "server", "[2.0.0,)", "2.0.0", "test.one-server"), "test.one-server"));

        final List<SelectionSource> selectedSources = JarSelector.detectAndSelect(
                sources,
                SelectionSource::getResource,
                SelectionSource::getInternal,
                SelectionSource::getName,
                (Function<Collection<JarSelector.ResolutionFailureInformation<SelectionSource>>, IllegalStateException>) resolutionFailureInformations -> {
                    throw new IllegalStateException("Failed");
                }
        );

        Assertions.assertEquals(2, selectedSources.size());
        final Set<String> selectedNames = selectedNames(selectedSources);
        Assertions.assertTrue(selectedNames.contains("test.one-client"));
        Assertions.assertTrue(selectedNames.contains("test.one-server"));
    }

    @Test
    public void doesSelectClassifierAndUnclassifiedArtifactsSeparately() throws InvalidVersionSpecificationException {
        final List<SelectionSource> sources = new ArrayList<>();
        sources.add(createSource("outer_unclassified", createArtifact("test.one", "artifact", null, "[1.0.0,1.5.0)", "1.0.0", "test.one"), "test.one"));
        sources.add(createSource("outer_client_classifier", createArtifact("test.one", "artifact", "client", "[2.0.0,)", "2.0.0", "test.one-client"), "test.one-client"));

        final List<SelectionSource> selectedSources = JarSelector.detectAndSelect(
                sources,
                SelectionSource::getResource,
                SelectionSource::getInternal,
                SelectionSource::getName,
                (Function<Collection<JarSelector.ResolutionFailureInformation<SelectionSource>>, IllegalStateException>) resolutionFailureInformations -> {
                    throw new IllegalStateException("Failed");
                }
        );

        Assertions.assertEquals(2, selectedSources.size());
        final Set<String> selectedNames = selectedNames(selectedSources);
        Assertions.assertTrue(selectedNames.contains("test.one"));
        Assertions.assertTrue(selectedNames.contains("test.one-client"));
    }

    private SelectionSource createSource(final String outer_name, final ContainedJarMetadata artifact, final String inner_name) throws InvalidVersionSpecificationException {
        return new SelectionSource(
                outer_name,
                new Metadata(ImmutableList.of(artifact)),
                new SelectionSource(inner_name));
    }

    private ContainedJarMetadata createArtifact(final String spec, final String version) throws InvalidVersionSpecificationException {
        return createArtifact("test.one", spec, version);
    }

    private ContainedJarMetadata createArtifact(final String name, final String spec, final String version) throws InvalidVersionSpecificationException {
        return createArtifact(name, "artifact", null, spec, version, name);
    }

    private ContainedJarMetadata createArtifact(final String group, final String artifact, final String classifier, final String spec, final String version, final String path) throws InvalidVersionSpecificationException {
        return new ContainedJarMetadata(
                new ContainedJarIdentifier(group, artifact, classifier),
                new ContainedVersion(VersionRange.createFromVersionSpec(spec), new DefaultArtifactVersion(version)),
                path,
                false
        );
    }

    private Set<String> selectedNames(final List<SelectionSource> selectedSources) {
        return selectedSources.stream().map(SelectionSource::getName).collect(Collectors.toSet());
    }

    private final class SelectionSource {

        private final String name;
        private Metadata metadata = null;
        private SelectionSource internalSource = null;

        public SelectionSource(final String name, final Metadata metadata, final SelectionSource source) {
            this.metadata = metadata;
            this.internalSource = source;
            this.name = name;
        }

        public SelectionSource(final String name) {
            this.name = name;
        }

        public Optional<InputStream> getResource(final String path) {
            if (path.contains("metadata") && metadata != null) {
                return Optional.of(MetadataIOHandler.toInputStream(metadata));
            }

            return Optional.empty();
        }

        public Optional<SelectionSource> getInternal(final String path) {
            if (internalSource != null && path.contains(internalSource.getName())) {
                return Optional.of(internalSource);
            }

            return Optional.empty();
        }

        public String getName() {
            return name;
        }
    }
}
