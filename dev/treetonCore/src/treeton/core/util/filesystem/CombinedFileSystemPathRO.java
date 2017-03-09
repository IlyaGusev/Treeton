/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.util.filesystem;

import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.util.*;

public class CombinedFileSystemPathRO implements FileSystemPathRO {
    private static final Logger logger = Logger.getLogger(CombinedFileSystemPathRO.class);
    private List<ZipFileSystemPathRO> zipRoots = new ArrayList<ZipFileSystemPathRO>();
    private CommonFileSystemPathRO commonRoot;

    public CombinedFileSystemPathRO(File commonDir, File zipDir) throws IOException {
        commonRoot = new CommonFileSystemPathRO(commonDir);
        File[] zipFiles = zipDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".zip");
            }
        });
        if (zipFiles == null)
            zipFiles = new File[0];
        Arrays.sort(zipFiles, new Comparator<File>() {
            public int compare(File o1, File o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        for (File zipFile : zipFiles) {
            URI url = FileSystems.getDefault().getPath(zipFile.getPath()).toUri();
            URI uri = URI.create("jar:" + url);
            FileSystem matrix_fs;
            try {
                matrix_fs = FileSystems.getFileSystem(uri);
            } catch (FileSystemNotFoundException e) {
                Map<String, String> params = new HashMap<String, String>();
                params.put("encoding", "CP866");
                matrix_fs = FileSystems.newFileSystem(uri, params, null);
            }
            zipRoots.add(new ZipFileSystemPathRO(matrix_fs.getPath("/")));
        }

        logger.info("Combined file system instantiated with commonRoot=" + commonRoot + " and zipRoots=" + zipRoots);
    }

    public boolean isDirectory() {
        return true;
    }

    public FileSystemPathRO[] listFiles() throws IOException {
        Collection<FileSystemPathRO> res = new ArrayList<FileSystemPathRO>();
        for (ZipFileSystemPathRO zipRoot : zipRoots) {
            Collections.addAll(res, zipRoot.listFiles());
        }
        Collections.addAll(res, commonRoot.listFiles());

        return res.toArray(new FileSystemPathRO[res.size()]);

    }

    public String getName() {
        return "/";
    }

    public BufferedReader createBufferedReader() throws IOException {
        throw new UnsupportedOperationException("Cannot create Reader for directory!");
    }

    public FileSystemPathRO getChildFile(String childName) {
        for (ZipFileSystemPathRO zipRoot : zipRoots) {
            FileSystemPathRO child = zipRoot.getChildFile(childName);
            if (child.exists())
                return child;
        }
        return commonRoot.getChildFile(childName);
    }

    public boolean exists() {
        return true;
    }
}
