/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.util;

import javax.swing.filechooser.FileFilter;
import java.io.File;

public class CommonFileFilter extends FileFilter {
    protected String description;
    protected String[] extensions;

    public CommonFileFilter(String description, String extensions) {
        this.description = (description != null) ? description : "";
        this.extensions = extensions.toLowerCase().split(" +");
    }

    public boolean accept(File f) {
        String fname = f.getName().toLowerCase();
        int extPos = fname.lastIndexOf('.');
        String ext = (extPos < 0) ? "" : fname.substring(extPos + 1);
        return f.isDirectory() || extInArray(ext);
    }

    public String getDescription() {
        return description;
    }

    protected boolean extInArray(String ext) {
        boolean rslt = false;
        if (ext != null && this.extensions != null) {
            for (int i = 0; i < extensions.length; i++) {
                if (ext.equals(this.extensions[i])) {
                    rslt = true;
                    break;
                }
            }
        }
        return rslt;
    }
}
