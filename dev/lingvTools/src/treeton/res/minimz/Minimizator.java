/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.res.minimz;

import treeton.core.TreenotationStorage;
import treeton.core.config.context.ContextException;
import treeton.core.config.context.resources.ExecutionException;
import treeton.core.config.context.resources.Resource;
import treeton.core.config.context.resources.ResourceInstantiationException;
import treeton.core.config.context.resources.TextMarkingStorage;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class Minimizator extends Resource {
    public Iterable<TypeMatrixTriplet> matrices;

    public String process(String text, TextMarkingStorage _storage, Map<String, Object> params) throws ExecutionException {
        TreenotationStorage storage = (TreenotationStorage) _storage;
        fireProgressStart();
        fireStatusChanged("Performing minimization...");

        long startTime = System.currentTimeMillis();

        for (TypeMatrixTriplet triplet : matrices) {
            Minimz.minimzByMatrix(storage, getTrnContext(), triplet);
        }

        long endTime = System.currentTimeMillis();

        fireStatusChanged(
                "Document completed in "
                        + NumberFormat.getInstance().format((double) (endTime - startTime) / 1000)
                        + " seconds!");
        fireProcessFinished();
        return null;
    }

    public void fireProgressStart() {
        if (getProgressListener() != null) {
            getProgressListener().progressStarted();
        }
    }

    public void fireProcessFinished() {
        if (getProgressListener() != null) {
            getProgressListener().progressFinished();
        }
    }

    public void fireStatusChanged(String s) {
        if (getProgressListener() != null) {
            getProgressListener().statusStringChanged(s);
        }
    }

    public void stop() {
    }

    public void processTerminated() {
    }

    public void init() throws ResourceInstantiationException {
        URL grammarURL;
        try {
            grammarURL = new URL(getResContext().getFolder(), (String) getInitialParameters().get("rulesPath"));
        } catch (ContextException e) {
            throw new ResourceInstantiationException("Wrong rulesPath parameter's value", e);
        } catch (MalformedURLException e) {
            throw new ResourceInstantiationException("Wrong rulesPath parameter's value", e);
        }
        init(grammarURL);
    }

    protected void init(URL grammarURL) throws ResourceInstantiationException {
        String gs = grammarURL.getPath();
        int lastSlash = Math.max(gs.lastIndexOf("/"), gs.lastIndexOf("\\"));
        if (lastSlash > 0) {
            String path = gs.substring(0, lastSlash);
            if ((path.startsWith("/") || path.startsWith("\\")) && path.contains(":")) {
                path = path.substring(1);
            }
            String javaFile = gs.substring(lastSlash + 1);
            int dot = javaFile.lastIndexOf(".");
            if (dot > 0) {
                File f = new File(path + "/" + javaFile);
                File fClass = new File(path + getPackage() + javaFile.substring(0, dot) + ".class");
                if (!fClass.exists() || f.lastModified() > fClass.lastModified()) {
                    List<String> args = new ArrayList<String>();
                    args.add("-sourcepath");
                    args.add(path);
                    args.add("-encoding");
                    args.add("UTF-8");
                    args.add("-d");
                    args.add(path);
                    args.add(path + "/" + javaFile);
                    if (com.sun.tools.javac.Main.compile(args.toArray(new String[args.size()])) != 0) {
                        throw new ResourceInstantiationException(null, "There were compile errors!");
                    }
                }
                try {
                    String clsFullName = this.getClass().getPackage().getName() + "." + javaFile.substring(0, dot);
                    if (clsFullName.equals(MinimzMorph.class.getName())) // increase load speed for the common case
                        matrices = new MinimzMorph();
                    else {
                        // URLClassLoader with URL like "file:/path" loades classes much much faster than with URL "file://path"
                        // it's supposed that the latter tries to use network
                        URLClassLoader loader = new URLClassLoader(new URL[]{new URL("file:/" + path + "/")});
                        Class c = loader.loadClass(clsFullName);
                        matrices = (Iterable<TypeMatrixTriplet>) c.newInstance();
                    }
                } catch (ClassNotFoundException e) {
                    throw new ResourceInstantiationException("Problem with java-class", e);
                } catch (InstantiationException e) {
                    throw new ResourceInstantiationException("Problem with java-class", e);
                } catch (IllegalAccessException e) {
                    throw new ResourceInstantiationException("Problem with java-class", e);
                } catch (MalformedURLException e) {
                    throw new ResourceInstantiationException("Problem with rules directory", e);
                }
            }
        }
    }

    protected String getPackage() {
        return "/treeton/res/";
    }

    public void deInit() {
        matrices = null;
    }
}
