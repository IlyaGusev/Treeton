/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.res;

import treeton.core.TreenotationStorage;
import treeton.core.config.context.ContextException;
import treeton.core.config.context.resources.ExecutionException;
import treeton.core.config.context.resources.Resource;
import treeton.core.config.context.resources.ResourceInstantiationException;
import treeton.core.config.context.resources.TextMarkingStorage;
import treeton.core.model.TreetonModelException;
import treeton.core.scape.ParseException;
import treeton.core.util.sut;
import treeton.scape.ScapeApplication;
import treeton.scape.ScapeException;
import treeton.scape.ScapeProgram;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

public class ScapeTransducer extends Resource {
    ScapeProgram program;

    protected String process(String text, TextMarkingStorage storage, Map<String, Object> params) throws ExecutionException {
        program.applyTo((TreenotationStorage) storage);
        return null;
    }

    protected void stop() {
    }

    protected void processTerminated() {
        program.reset();
    }

    protected void init() throws ResourceInstantiationException {
        try {
            URL path = new URL(getResContext().getFolder(), (String) getInitialParameters().get("rulesPath"));

            List<URL> urls = sut.retrieveURLsFromFolder(
                    new File(path.getPath()),
                    new String[]{"scape"});

            ScapeApplication app = new ScapeApplication(urls, getTrnContext().getTypes());
            program = app.createProgram((String) getInitialParameters().get("mainProgram"));
        } catch (MalformedURLException e) {
            throw new ResourceInstantiationException("Wrong path: " + getInitialParameters().get("rulesPath"));
        } catch (IOException e) {
            throw new ResourceInstantiationException("IO problems during instantiation of the ScapeTransducer.", e);
        } catch (ParseException e) {
            throw new ResourceInstantiationException("Parse Exception during instantiation of the ScapeTransducer.", e);
        } catch (ScapeException e) {
            throw new ResourceInstantiationException("ScapeException during instantiation of the ScapeTransducer.", e);
        } catch (TreetonModelException e) {
            throw new ResourceInstantiationException("TreetonModelException during instantiation of the ScapeTransducer.", e);
        } catch (ContextException e) {
            throw new ResourceInstantiationException("ContextException during instantiation of the ScapeTransducer.", e);
        }
    }

    protected void deInit() {
        program = null;
    }
}
