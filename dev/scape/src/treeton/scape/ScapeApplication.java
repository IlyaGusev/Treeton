/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.scape;

import treeton.core.config.BasicConfiguration;
import treeton.core.config.context.ContextConfiguration;
import treeton.core.model.TrnTypeStorage;
import treeton.core.scape.ParseException;
import treeton.core.util.sut;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

public class ScapeApplication {
    Map<String, ScapePackage> packages = new HashMap<String, ScapePackage>();

    TrnTypeStorage types;

    public ScapeApplication(List<URL> searchPath, TrnTypeStorage types) throws IOException, ScapeException {
        StringBuffer buf = new StringBuffer();

        for (URL cur : searchPath) {
            RandomAccessFile file = new RandomAccessFile(cur.getPath(), "r");
            sut.skipSpacesEndls(file);
            long pos1 = file.getChannel().position();
            sut.skipTillByte(file, new byte[]{'{'});
            long pos2 = file.getChannel().position();
            file.getChannel().position(pos1);
            byte[] arr = new byte[(int) (pos2 - pos1 + 1)];
            file.readFully(arr);
            file.close();

            buf.setLength(0);
            char[] carr = new String(arr).toCharArray();
            int pl;
            try {
                pl = ScapePhase.readInPackage(0, carr, carr.length - 1, buf);
            } catch (ParseException e) {
                System.out.println("File " + cur.getPath() + " has wrong package description");
                continue;
            }

            String _package = buf.toString();
            buf.setLength(0);

            String name;
            boolean isProgram = false;

            try {
                ScapePhase.readInName(carr, pl, carr.length - 1, buf, null);
            } catch (ParseException e) {
                buf.setLength(0);
                try {
                    ScapeProgram.readInName(carr, pl, carr.length - 1, buf);
                    isProgram = true;
                } catch (ParseException e1) {
                    System.out.println("File " + cur.getPath() + " doesn't seem to contain scape phase or scape program");
                    continue;
                }
            }

            name = buf.toString();

            ScapePackage p = getPackage(_package, true);
            if (p == null) {
                System.out.println("File " + cur.getPath() + " has wrong package description");
                continue;
            }

            Map<String, URL> locations;

            if (isProgram) {
                locations = p.programLocations;
            } else {
                locations = p.phasesLocations;
            }

            if (locations.containsKey(name)) {
                throw new ScapeException("Duplicate phases' names were located. Files: " + locations.get(name).getPath() + " and " + cur.getPath());
            }

            locations.put(name, cur);
        }
        this.types = types;
    }

    public static void main(String args[]) throws Exception {
        BasicConfiguration.createInstance();
        ContextConfiguration.createInstance();
        List<URL> urls = sut.retrieveURLsFromFolder(
                new File("c:/temp/scapeTest/"),
                new String[]{"scape"});
        new ScapeApplication(urls, ContextConfiguration.trnsManager().get("Common.Russian").getTypes()).createProgram("Test");
        System.out.println("Success");
    }

    ScapePackage getPackage(String packageName, boolean autoFill) {
        StringTokenizer st = new StringTokenizer(packageName, ".", false);
        ScapePackage cur = null;
        if (st.hasMoreTokens()) {
            String s = st.nextToken();

            cur = packages.get(s);
            if (cur == null) {
                if (!autoFill) {
                    return null;
                }
                cur = new ScapePackage(s);
                packages.put(s, cur);
            }
            while (st.hasMoreTokens()) {
                s = st.nextToken();
                ScapePackage np = cur.descendants.get(s);
                if (np == null) {
                    if (!autoFill) {
                        return null;
                    }
                    np = new ScapePackage(s);
                    cur.descendants.put(s, np);
                }
                cur = np;
            }
        }
        return cur;
    }

    public ScapeProgram createProgram(String name) throws ScapeException, IOException, ParseException {
        int i = name.lastIndexOf(".");
        if (i == -1) {
            throw new ScapeException("No package specified");
        }
        String _package = name.substring(0, i);
        ScapePackage p = getPackage(_package, false);
        if (p == null) {
            throw new ScapeException("Unable to find a package called " + _package);
        }

        URL programURL = p.programLocations.get(name.substring(i + 1));
        if (programURL == null) {
            throw new ScapeException("Unable to find a program called " + name.substring(i + 1) + " in package " + p.getName());
        }

        return new ScapeProgram(programURL, this);
    }

    public TrnTypeStorage getTypes() {
        return types;
    }

    public URL getPhaseLocation(String name, ScapePackage defaultPackage) throws ScapeException {
        int i = name.lastIndexOf(".");
        ScapePackage p;
        String phaseName;
        if (i == -1) {
            p = defaultPackage;
            if (p == null) {
                throw new ScapeException("No default package specified");
            }
            phaseName = name;
        } else {
            String _package = name.substring(0, i);
            p = getPackage(_package, false);
            if (p == null) {
                throw new ScapeException("Unable to find a package called " + _package);
            }
            phaseName = name.substring(i + 1);
        }

        URL programURL = p.phasesLocations.get(name.substring(i + 1));
        if (programURL == null) {
            throw new ScapeException("Unable to find a phase called " + phaseName + " in package " + p.getName());
        }
        return programURL;
    }
}
