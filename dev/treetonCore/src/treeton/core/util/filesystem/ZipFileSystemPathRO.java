/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.util.filesystem;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.EnumSet;

public class ZipFileSystemPathRO implements FileSystemPathRO {
    private Path path;

    public ZipFileSystemPathRO(Path path) {
        this.path = path;
    }

    public boolean isDirectory() {
        return Files.isDirectory(path);
    }

    public FileSystemPathRO[] listFiles() throws IOException {
        final ArrayList<FileSystemPathRO> res = new ArrayList<FileSystemPathRO>();
        Files.walkFileTree(path, EnumSet.noneOf(FileVisitOption.class), 1, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                res.add(new ZipFileSystemPathRO(file));
                return FileVisitResult.CONTINUE;
            }
        });

        return res.toArray(new FileSystemPathRO[res.size()]);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ZipFileSystemPathRO that = (ZipFileSystemPathRO) o;

        if (path != null ? !path.equals(that.path) : that.path != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return path != null ? path.hashCode() : 0;
    }

    @Override
    public String toString() {
        return path.toUri().toString();

    }

    public String getName() {
        return path.getFileName().toString();
    }

    public BufferedReader createBufferedReader() throws IOException {
        return Files.newBufferedReader(path, Charset.defaultCharset());
    }

    public FileSystemPathRO getParentFile() {
        return new ZipFileSystemPathRO(path.getParent());
    }

    public FileSystemPathRO getChildFile(String childName) {
        return new ZipFileSystemPathRO(path.getFileSystem().getPath(path.toString(), childName));
    }

    public boolean exists() {
        return Files.exists(path);
    }
}
