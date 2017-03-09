/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.util.filesystem;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

public class CommonFileSystemPathRO implements FileSystemPathRO {
    private File path;

    public CommonFileSystemPathRO(File path) {
        this.path = path;
    }

    public boolean isDirectory() {
        return path.isDirectory();
    }

    public FileSystemPathRO[] listFiles() {
        File[] files = path.listFiles();
        FileSystemPathRO[] res = new FileSystemPathRO[files.length];
        for (int i = 0; i < files.length; i++) {
            res[i] = new CommonFileSystemPathRO(files[i]);
        }
        return res;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CommonFileSystemPathRO that = (CommonFileSystemPathRO) o;

        if (path != null ? !path.equals(that.path) : that.path != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return path != null ? path.hashCode() : 0;
    }

    @Override
    public String toString() {
        return path.getAbsolutePath();

    }

    public String getName() {
        return path.getName();
    }

    public BufferedReader createBufferedReader() throws FileNotFoundException {
        return new BufferedReader(new FileReader(path));
    }

    public FileSystemPathRO getParentFile() {
        return new CommonFileSystemPathRO(path.getParentFile());
    }

    public FileSystemPathRO getChildFile(String childName) {
        return new CommonFileSystemPathRO(new File(path, childName));
    }

    public boolean exists() {
        return path.exists();
    }
}
