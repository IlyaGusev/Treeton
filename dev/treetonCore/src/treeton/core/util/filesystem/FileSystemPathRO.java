/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.util.filesystem;

import java.io.BufferedReader;
import java.io.IOException;

public interface FileSystemPathRO {
    boolean isDirectory();

    FileSystemPathRO[] listFiles() throws IOException;

    String getName();

    BufferedReader createBufferedReader() throws IOException;

    FileSystemPathRO getChildFile(String childName);

    boolean exists();
}
