/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.gui.util;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.io.File;

public class FileDialogUtil {

    public String lastDirectory;

    public FileDialogUtil(String lastDir) {
        UIManager.put("FileChooser.cancelButtonText", "Отказаться");
        UIManager.put("FileChooser.lookInLabelText", "Папка:");
        this.lastDirectory = lastDir != null ? lastDir : "";
    }

    public File openFileDialog(JComponent parent, String title, FileFilter filter) {
        File rslt = null;

        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle(title);
        if (filter != null) {
            fc.addChoosableFileFilter(filter);
        }
        if (lastDirectory.length() > 0) {
            fc.setCurrentDirectory(new File(lastDirectory));
        }
        fc.setApproveButtonText("Открыть");
        int returnVal = fc.showOpenDialog(parent);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            String p = file.getParent();
            if (p != null) {
                lastDirectory = p;
            }
            rslt = file;
        }
        return rslt;
    }

    public File[] openFileDialogMultiple(JComponent parent, String title, FileFilter filter) {
        File[] rslt = null;

        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle(title);
        fc.setMultiSelectionEnabled(true);
        if (filter != null) {
            fc.addChoosableFileFilter(filter);
        }
        if (lastDirectory.length() > 0) {
            fc.setCurrentDirectory(new File(lastDirectory));
        }
        fc.setApproveButtonText("Открыть");
        int returnVal = fc.showOpenDialog(parent);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File[] files = fc.getSelectedFiles();
            if (files.length > 0) {
                String p = files[0].getParent();
                if (p != null) {
                    lastDirectory = p;
                }
            }
            rslt = files;
        }
        return rslt;
    }

    public File saveFileDialog(JComponent parent, String title, String saveAs, FileFilter filter) {
        File rslt = null;

        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle(title);
        if (filter != null) {
            fc.addChoosableFileFilter(filter);
        }
        if (saveAs != null) {
            fc.setSelectedFile(new File(saveAs));
        } else {
            if (lastDirectory.length() > 0) {
                fc.setCurrentDirectory(new File(lastDirectory));
            }
        }
        fc.setApproveButtonText("Сохранить");
        int returnVal = fc.showOpenDialog(parent);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            String p = file.getParent();
            if (p != null) {
                lastDirectory = p;
            }
            rslt = file;
        }
        return rslt;
    }
}
