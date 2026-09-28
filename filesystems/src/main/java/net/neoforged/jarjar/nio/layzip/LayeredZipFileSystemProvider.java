package net.neoforged.jarjar.nio.layzip;

import net.neoforged.jarjar.nio.pathfs.PathFileSystemProvider;
import net.neoforged.jarjar.nio.pathfs.PathPath;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class LayeredZipFileSystemProvider extends PathFileSystemProvider
{
    public static final String SCHEME = "jij";
    // A bare '~' can occur in filenames; only '~/' separates archive layers.
    public static final String URI_SPLIT_REGEX = COMPONENT_SEPERATOR + "(?=/)";


    @Override
    public String getScheme()
    {
        return SCHEME;
    }

    @Override
    public FileSystem newFileSystem(final URI uri, final Map<String, ?> env) throws IOException
    {
        if (env.containsKey("packagePath"))
        { //User requests specific package as a target;
            try
            {
                return super.newFileSystem(uri, env);
            } catch (Exception e)
            {
                throw new UncheckedIOException("Failed to create intermediary FS.",
                        new IOException("Failed to process data.", e));
            }
        }

        final UriSection[] sections = parseUriSections(uri);

        FileSystem workingSystem = FileSystems.getDefault(); //Grab the normal disk FS.
        String keyPrefix = "";

        if (sections.length > 1)
        {
            final AdaptedURIWithPrefixSelection adaptedURI = adaptUriSections(sections);
            keyPrefix = adaptedURI.getPrefix();
            workingSystem = adaptedURI.getFileSystem();
        }

        final UriSection lastSection = sections[sections.length - 1];
        final Path lastPath = lastSection.toPath(workingSystem).toAbsolutePath();
        return getOrCreateNewSystem(keyPrefix, lastPath);
    }

    private static UriSection[] parseUriSections(final URI uri)
    {
        // Split before decoding so escaped '~/' characters do not introduce an archive boundary.
        return Arrays.stream(uri.getRawSchemeSpecificPart().split(URI_SPLIT_REGEX))
                .map(UriSection::new)
                .toArray(UriSection[]::new);
    }

    private static final class UriSection
    {
        private final URI fileUri;

        private UriSection(final String rawSection)
        {
            this.fileUri = URI.create("file:" + rawSection);
        }

        private String getKey()
        {
            return fileUri.getRawSchemeSpecificPart();
        }

        private Path toPath(final FileSystem workingSystem)
        {
            if (workingSystem == FileSystems.getDefault() && !fileUri.isOpaque())
            {
                // The native provider understands file URI syntax, such as /C:/... on Windows.
                return workingSystem.provider().getPath(fileUri);
            }
            // Relative disk paths and paths inside ZIPs use their filesystem's path syntax.
            return workingSystem.getPath(fileUri.getSchemeSpecificPart());
        }
    }

    private FileSystem getOrCreateNewSystem(final Path path)
    {
        return getOrCreateNewSystem("", path);
    }

    private FileSystem getOrCreateNewSystem(String keyPrefix, final Path path)
    {
        final Map<String, Object> args = new HashMap<>();
        args.put("packagePath", path.toAbsolutePath());

        try
        {
            final String key = keyPrefix + path.toString().replace("\\", "/");
            // Pass the scheme and key separately so filenames like "my archive.zip" are escaped.
            // Parsing a concatenated URI string would reject the space with URISyntaxException.
            return super.newFileSystem(new URI(super.getScheme(), key, null), args);
        } catch (Exception e)
        {
            throw new UncheckedIOException("Failed to create intermediary FS.", new IOException("Failed to process " +
                                                                                                "data.", e));
        }
    }

    @Override
    public Path getPath(final URI uri)
    {
        // Registered URIs are opaque keys; a '~' in the key is not an archive boundary.
        final Optional<FileSystem> existing = getFileSystemFromKey(makeKey(uri));
        if (existing.isPresent())
            return existing.get().getPath("/");

        final UriSection[] sections = parseUriSections(uri);
        if (sections.length == 1)
            return super.getPath(uri);

        FileSystem workingSystem = FileSystems.getDefault(); //Grab the normal disk FS.
        if (sections.length > 1)
        {
            for (int i = 0; i < sections.length - 1; i++)
            {
                final UriSection section = sections[i];
                final Path path = section.toPath(workingSystem);
                workingSystem = getOrCreateNewSystem(path);
            }
        }

        final UriSection lastSection = sections[sections.length - 1];
        return lastSection.toPath(workingSystem);
    }

    @Override
    public FileSystem getFileSystem(final URI uri)
    {
        final Optional<FileSystem> existing = getFileSystemFromKey(makeKey(uri));
        if (existing.isPresent())
            return existing.get();

        final UriSection[] sections = parseUriSections(uri);
        if (sections.length == 1)
        {
            return super.getFileSystem(uri);
        }

        FileSystem workingSystem = FileSystems.getDefault(); //Grab the normal disk FS.
        if (sections.length > 1)
        {
            for (int i = 0; i < sections.length - 1; i++)
            {
                final UriSection section = sections[i];
                final Path path = section.toPath(workingSystem);
                workingSystem = getOrCreateNewSystem(path);
            }
        }

        final UriSection lastSection = sections[sections.length - 1];
        final Path lastPath = lastSection.toPath(workingSystem);
        return getOrCreateNewSystem(lastPath);
    }

    @Override
    protected URI buildUriFor(final PathPath path) throws URISyntaxException, IllegalArgumentException
    {
        String prefix = "";

        prefix = buildPrefixFor(path.getFileSystem().getTarget());

        return URI.create(String.format("%s:%s%s", SCHEME, prefix, path)
                                .replace(String.format("%s/", PATH_SEPERATOR), PATH_SEPERATOR));
    }

    protected String buildPrefixFor(final Path path) {
        if (path instanceof PathPath) {
            return buildPrefixFor(((PathPath) path).getFileSystem().getTarget()) + PATH_SEPERATOR + path.toAbsolutePath();
        }

        return path.toAbsolutePath().toUri().getRawSchemeSpecificPart();
    }

    @SuppressWarnings("resource")
    @Override
    public Path adaptResolvedPath(final PathPath path)
    {
        if (!path.toString().contains(PATH_SEPERATOR))
            return path;

        final Path workingPath = path.getFileSystem()
                                     .getPath(path.toString()
                                                  .substring(0, path.toString().lastIndexOf(PATH_SEPERATOR)) + PATH_SEPERATOR);
        final FileSystem workingSystem;
        try
        {
            workingSystem = FileSystems.newFileSystem(workingPath.toUri(), new HashMap<>());
            return workingSystem.getPath(path.endsWith(PATH_SEPERATOR) ? "/" : path.toString()
                                                                                   .substring(path.toString()
                                                                                                  .lastIndexOf(PATH_SEPERATOR) + 2));
        } catch (IOException e)
        {
            throw new IllegalArgumentException("Failed to get sub file system for path!", e);
        }
    }

    @Override
    public String[] adaptPathParts(final String longstring, final String[] pathParts)
    {
        if (!longstring.endsWith(PATH_SEPERATOR))
            return pathParts;

        pathParts[pathParts.length - 1] = pathParts[pathParts.length - 1] + "/";
        return pathParts;
    }

    private AdaptedURIWithPrefixSelection adaptUriSections(final UriSection[] sections) {
        String keyPrefix = "";
        FileSystem workingSystem = FileSystems.getDefault();

        //First try a reverse lookup of a key based approach:
        final String rootKey = sections[0].getKey();
        final Optional<FileSystem> rootKnownCandidateSystem = super.getFileSystemFromKey(rootKey);
        if (rootKnownCandidateSystem.isPresent()) {
            //Okey special case: We have a file system in the root that is known to us.
            //We will recursively resolve this untill we have handled all sections:
            //First deal with the case that we do not have any other paths:
            if (sections.length == 1) {
                return new AdaptedURIWithPrefixSelection(rootKnownCandidateSystem.get(), rootKey);
            }

            workingSystem = rootKnownCandidateSystem.get();
            keyPrefix = rootKey + PATH_SEPERATOR;

            for (int i = 1; i < sections.length - 2; i++)
            {
                final Path path = sections[i].toPath(workingSystem).toAbsolutePath();
                workingSystem = getOrCreateNewSystem(keyPrefix, path);
                keyPrefix += path.toString().replace("\\", "/") + PATH_SEPERATOR;
            }

            return new AdaptedURIWithPrefixSelection(workingSystem, sections[sections.length - 1].getKey());
        }

        //This is now a special case here, we might be in native land so we need to deal with it.
        for (int i = 0; i < sections.length - 1; i++)
        {
            final UriSection section = sections[i];
            final Path path = section.toPath(workingSystem).toAbsolutePath();
            workingSystem = getOrCreateNewSystem(keyPrefix, path);
            keyPrefix += path.toString().replace("\\", "/") + PATH_SEPERATOR;
        }

        return new AdaptedURIWithPrefixSelection(workingSystem, keyPrefix);
    }

    private final class AdaptedURIWithPrefixSelection {
        private final String prefix;
        private final FileSystem fileSystem;

        private AdaptedURIWithPrefixSelection(final FileSystem fileSystem, final String prefix)
        {
            this.prefix = prefix;
            this.fileSystem = fileSystem;
        }

        public String getPrefix()
        {
            return prefix;
        }

        public FileSystem getFileSystem()
        {
            return fileSystem;
        }
    }
}
