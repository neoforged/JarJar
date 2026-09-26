package net.neoforged.jarjar.nio.layfs;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

public class TestRegisteredLayeredZipFS {
    private static final String ENTRY_NAME = "message.txt";
    private static final String ENTRY_CONTENTS = "Hello from the archive";

    @TempDir
    Path temporaryDirectory;

    private Path archiveFixture;

    @BeforeEach
    public void createArchiveFixture() throws IOException {
        archiveFixture = temporaryDirectory.resolve("archive.zip");
        try (ZipOutputStream archive = new ZipOutputStream(Files.newOutputStream(archiveFixture))) {
            archive.putNextEntry(new ZipEntry(ENTRY_NAME));
            archive.write(ENTRY_CONTENTS.getBytes(StandardCharsets.UTF_8));
            archive.closeEntry();
        }
    }

    @Test
    public void testTildeInArchiveNameWithoutPackagePath() throws IOException {
        // The on-disk archive filename contains '~'; without packagePath, the URI must locate that exact file.
        final Path archiveWithTilde = temporaryDirectory.resolve("archive~test.zip");
        Files.move(archiveFixture, archiveWithTilde);

        // Windows file paths use backslashes, which URI.create rejects; URI paths require forward slashes.
        final String archivePath = archiveWithTilde.toString().replace("\\", "/");
        // The 'jij:' scheme selects JarJar's archive filesystem provider.
        final URI archiveUri = URI.create("jij:" + archivePath);
        final Map<String, Path> environment = Collections.emptyMap();
        final List<String> expectedLines = Collections.singletonList(ENTRY_CONTENTS);

        try (FileSystem openedFileSystem = FileSystems.newFileSystem(archiveUri, environment)) {
            final FileSystem resolvedFileSystem = FileSystems.getFileSystem(archiveUri);
            assertSame(openedFileSystem, resolvedFileSystem,
                    "Looking up the same URI must return the FileSystem object created by newFileSystem");

            final Path archiveRoot = Paths.get(archiveUri);
            assertSame(openedFileSystem, archiveRoot.getFileSystem(),
                    "The path returned by Paths.get must belong to the FileSystem object created by newFileSystem");

            // JarJar uses '~/' to separate the archive URI from the path to a file inside it.
            final URI entryUri = URI.create(archiveUri + "~/" + ENTRY_NAME);
            // The 'jij:' scheme makes Paths.get call LayeredZipFileSystemProvider.getPath,
            // exercising JarJar's parsing of the filename tilde and the '~/' separator.
            final Path entryPath = Paths.get(entryUri);
            final List<String> actualLines = Files.readAllLines(entryPath);
            assertEquals(expectedLines, actualLines,
                    "Entry lookup must distinguish '~' in the archive name from '~/' before the entry path");
        }
    }

    @Test
    public void testRegisteredUriWithTildeInJarName() throws IOException {
        // The URI's filename contains '~'. Lookup must still find the archive supplied by packagePath (issue #6).
        final URI registrationUri = URI.create("jij:/mod~test.jar");
        final Map<String, Path> environment = Collections.singletonMap("packagePath", archiveFixture);
        final List<String> expectedLines = Collections.singletonList(ENTRY_CONTENTS);

        try (FileSystem registeredFileSystem = FileSystems.newFileSystem(registrationUri, environment)) {
            final FileSystem resolvedFileSystem = FileSystems.getFileSystem(registrationUri);
            assertSame(registeredFileSystem, resolvedFileSystem,
                    "Looking up the same URI must return the FileSystem object created by newFileSystem");

            final Path archiveRoot = Paths.get(registrationUri);
            final Path entryPath = archiveRoot.resolve(ENTRY_NAME);
            final List<String> actualLines = Files.readAllLines(entryPath);
            assertEquals(expectedLines, actualLines,
                    "Resolving an entry from Paths.get(registrationUri) must read the archive supplied by packagePath");
        }
    }

    @Test
    public void testRegisteredUriWithTildeInDirectoryName() throws IOException {
        // The URI's directory name ends in '~', so its '~/' could be mistaken for an archive boundary.
        final URI registrationUri = URI.create("jij:/mods~/mod.jar");
        final Map<String, Path> environment = Collections.singletonMap("packagePath", archiveFixture);
        final List<String> expectedLines = Collections.singletonList(ENTRY_CONTENTS);

        try (FileSystem registeredFileSystem = FileSystems.newFileSystem(registrationUri, environment)) {
            final FileSystem resolvedFileSystem = FileSystems.getFileSystem(registrationUri);
            assertSame(registeredFileSystem, resolvedFileSystem,
                    "Looking up the same URI must return the FileSystem object created by newFileSystem");

            final Path archiveRoot = Paths.get(registrationUri);
            final Path entryPath = archiveRoot.resolve(ENTRY_NAME);
            final List<String> actualLines = Files.readAllLines(entryPath);
            assertEquals(expectedLines, actualLines,
                    "Resolving an entry from Paths.get(registrationUri) must read the archive supplied by packagePath");
        }
    }

    @Test
    public void testRegisteredUriWithHomeDirectoryPrefix() throws IOException {
        // Home-directory notation starts with '~/', which must not be parsed as an empty outer archive.
        // packagePath supplies the actual file, so lookup must preserve this registered key.
        final URI registrationUri = URI.create("jij:~/mods/mod.jar");
        final Map<String, Path> environment = Collections.singletonMap("packagePath", archiveFixture);
        final List<String> expectedLines = Collections.singletonList(ENTRY_CONTENTS);

        try (FileSystem registeredFileSystem = FileSystems.newFileSystem(registrationUri, environment)) {
            final FileSystem resolvedFileSystem = FileSystems.getFileSystem(registrationUri);
            assertSame(registeredFileSystem, resolvedFileSystem,
                    "Looking up the same URI must return the FileSystem object created by newFileSystem");

            final Path archiveRoot = Paths.get(registrationUri);
            final Path entryPath = archiveRoot.resolve(ENTRY_NAME);
            final List<String> actualLines = Files.readAllLines(entryPath);
            assertEquals(expectedLines, actualLines,
                    "Resolving an entry from Paths.get(registrationUri) must read the archive supplied by packagePath");
        }
    }
}
