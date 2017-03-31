/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.prosody.mdlcompiler;

import gnu.getopt.Getopt;
import java_cup.runtime.Symbol;
import treeton.core.util.Utils;
import treeton.prosody.mdlcompiler.grammar.ast.CompilationUnit;
import treeton.prosody.mdlcompiler.grammar.ast.MeterDescription;
import treeton.prosody.mdlcompiler.fsm.MdlFSMBuilder;
import treeton.prosody.mdlcompiler.fsm.Meter;

import java.io.File;
import java.io.FilenameFilter;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

public class MdlCompiler {
    /** made to switch on and off the debug messages. */
    public static final boolean DEBUG = false;

    public static String nl = System.getProperty("line.separator");

    /** Source path. The list of all source directories to compile. */
    private List<File> srcPath;

    /** Encoding of source files */
    private String encoding;

    /**
     * Key - (File) mdl file<br>
     * Value - (String) package name. ex: <code>pkg1.pkg2.pkg3</code>
     */
    private Map<File, String> sourceFilePackages = new HashMap<File, String>();

    /**
     * Key - (File) mdl file<br>
     * Value - (CompilationUnit) parced grammar
     */
    private Map<File, CompilationUnit> sourceFileUnits = new HashMap<File, CompilationUnit>();

    /**
     * Key - (File) mdl file<br>
     * Value - (CompilationUnit) parced grammar
     */
    private List<CompileMessage> unparsedSourceFilesErrors = new ArrayList<CompileMessage>();

    private File outDir;

    /**
     * Constructs instance of compiler.
     *
     * @param srcPath
     *            The list of all source directories to compile
     * @param encoding
     *            Encoding of source files
     * @param outDir
     *            output directory
     * @throws TreevialCompilerException
     *             if some problem occured
     */
    public MdlCompiler(List<File> srcPath, String encoding, File outDir)
            throws TreevialCompilerException {
        this.srcPath = srcPath;
        this.outDir = outDir;

        this.encoding = encoding;
        this.sourceFilePackages = new HashMap<File, String>();
        this.sourceFileUnits = new HashMap<File, CompilationUnit>();
        // collect source files
        for (File srcDir : srcPath) {
            String srcDirStr = srcDir.getPath();

            if (srcDir.isFile() && srcDirStr.toLowerCase().endsWith(".mdl")) {
                if (this.sourceFilePackages.containsKey(srcDir))
                    throw new TreevialCompilerException(
                            "Attempt to add grammar file which is already added.");
                String pkg = "";
                if (pkg.startsWith("."))
                    pkg = pkg.substring(1);
                this.sourceFilePackages.put(srcDir, pkg);

            } else {
                List<File> srcFiles = Utils.listFiles(srcDir, new FilenameFilter() {
                    public boolean accept(File dir, String fName) {
                        return fName.toLowerCase().endsWith(".mdl");
                    }
                }, true);
                for (File srcFile : srcFiles) {
                    if (this.sourceFilePackages.containsKey(srcFile))
                        throw new TreevialCompilerException(
                                "Attempt to add grammar file which is already added.");
                    String pkg = srcFile.getParent().substring(srcDirStr.length())
                            .replace('/', '.').replace('\\', '.');
                    if (pkg.startsWith("."))
                        pkg = pkg.substring(1);
                    this.sourceFilePackages.put(srcFile, pkg);
                }
            }
        }
    }

    public MessageCollector getAllMessages() {
        MessageCollector mc = new MessageCollector();
        for (Map.Entry<File, CompilationUnit> unitEntry : getSourceFileUnits()
                .entrySet()) {
            unitEntry.getValue().visit(mc, true);
        }
        for (CompileMessage message : unparsedSourceFilesErrors) {
            mc.addError(message);
        }
        return mc;
    }

    public void addUnparsedSourceFilesError(CompileMessage message) {
        unparsedSourceFilesErrors.add(message);
    }

    public void parse() throws Exception {
        for (Map.Entry<File, String> entry : this.sourceFilePackages.entrySet()) {
            MdlParser p = new MdlParserCustomized(entry.getKey(), this,
                    this.encoding);
            try {
                Symbol result = (DEBUG) ? p.debug_parse() : p.parse();
                this.sourceFileUnits.put(entry.getKey(),
                        (CompilationUnit) result.value);
            } catch (RuntimeException e) { // todo remove obsolete code. Now
                System.out.println("Parse error. Skipping" + entry.getKey());
                e.printStackTrace();
            }
        }
    }

    public void doSemanticCheck() {
        /*SemanticCheckStage1.execute(this);//todo
          SemanticCheckStage2.execute(this);*/
    }

    /**
     * Parse arguments passed to main method and creates MdlCompiler.<br>
     * Prints messages in standart output in case of errors or warnings.
     *
     * @param args
     *            command line arguments
     * @return compiler instance or <code>null</code> if errors occured.
     */
    public static MdlCompiler parseArgs(String[] args) {
        List<File> sPath = new ArrayList<File>();
        File outDir = null;
        String encoding = "UTF-8";
        Getopt g = new Getopt("MdlCompiler", args, "s:d:e:");
        int c;
        String arg;
        while ((c = g.getopt()) != -1)
            switch (c) {
                case 's':
                    arg = g.getOptarg();
                    debug("option " + (char) c + " with an argument of "
                            + ((arg != null) ? arg : "null"));
                    if (arg != null && arg.trim().length() > 0) {
                        sPath = parseSrcPath(arg);
                        if (sPath == null)
                            return null;
                    }
                    break;
                case 'd':
                    arg = g.getOptarg();
                    debug("option " + (char) c + " with an argument of "
                            + ((arg != null) ? arg : "null"));
                    if (arg != null && arg.trim().length() > 0) {
                        File f = new File(arg);
                        if (f.isDirectory())
                            outDir = f;
                        else if (!f.exists()) {
                            f.mkdir();
                            outDir = f;
                        } else
                            outDir = null;
                    } else {
                        outDir = null;
                    }
                    break;
                case 'e':
                    arg = g.getOptarg();
                    debug("option " + (char) c + " with an argument of "
                            + ((arg != null) ? arg : "null"));
                    encoding = arg;
                    break;
                case '?':
                    debug("getopt() returned " + c + nl);
                    break; // getopt() already printed an error
                default:
                    debug("getopt() returned " + c + nl);
            }

        if (sPath.size() == 0) {
            error("Can't find any element of source path.");
            return null;
        }
        try {
            return new MdlCompiler(sPath, encoding, outDir);
        } catch (TreevialCompilerException e) {
            System.out.println("Internal error: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Performs parsing and checking of source path given as string in form:<br>
     * <code>directory1;directory2;...;directoryN</code><br>
     * If any errors found this method returns
     * <code>null</null> and prints error.
     *
     * @param srcPathString -
     *            source path to be parsed
     * @return List of files that are corrspond to source path entries
     *         preserving order as they were given or <code>null</code> if errors found.
     */
    private static List<File> parseSrcPath(String srcPathString) {
        List<File> result = new ArrayList<File>();
        StringTokenizer stok = new StringTokenizer(srcPathString, ";");
        if (stok.countTokens()==1) {
            String token = stok.nextToken();
            File f = findFile(token, false);
            if (f!=null) {
                result.add(f);
                debug("Source path entry '" + token
                        + "' added to source path as '"
                        + f.toURI().toString() + "'");
            } else {
                f = findFile(token, true);
                if (f != null) {
                    if (!checkSourcePathEntry(result, f)) {
                        // now we return NULL in any error in source path
                        return null;
                    } else {
                        result.add(f);
                        debug("Source path entry '" + token
                                + "' added to source path as '"
                                + f.toURI().toString() + "'");
                    }
                } else {
                    warning("Can't find source path '" + token + "'");
                }
            }
        }

        while (stok.hasMoreTokens()) {
            String token = stok.nextToken();
            debug("Adding source path entry " + token + " to source path...");
            // firstly, check source path entry directly
            File f = findFile(token, true);
            if (f != null) {
                if (!checkSourcePathEntry(result, f)) {
                    // now we return NULL in any error in source path
                    return null;
                } else {
                    result.add(f);
                    debug("Source path entry '" + token
                            + "' added to source path as '"
                            + f.toURI().toString() + "'");
                }
            } else {
                warning("Can't find source path '" + token + "'");
            }
        }
        return result;
    }

    /**
     * Checks whether is's allowed to add given source directory in given list
     * of source directories.
     * <ul>
     * Checks operation for following errors
     * <li>given directory already exists in list</li>
     * <li>given directory is a subdirectory of one of dirctories in the list</li>
     * <li>given directory is a superdirectory of one of dirctories in the list</li>
     * </ul>
     *
     * @param sPath
     *            list of directories
     * @param f
     *            directory to add
     * @return <code>true</code> if no errors detected, otherwise
     *         <code>false</code>
     */
    private static boolean checkSourcePathEntry(List<File> sPath, File f) {
        for (File exF : sPath) {
            if (exF.equals(f)) {
                warning("Found duplicate entry in source path '" + f.getPath()
                        + "'.");
                return false;
            }
            String exF_path = (System.getProperty("os.name")
                    .startsWith("Windows")) ? exF.getPath().toLowerCase() : exF
                    .getPath();
            String f_path = (System.getProperty("os.name")
                    .startsWith("Windows")) ? f.getPath().toLowerCase() : f
                    .getPath();
            if (exF_path.startsWith(f_path)) {
                error("Source path entry '" + exF.getPath()
                        + "' is a subset of '" + f.getPath() + "'");
                return false;
            }
            if (f_path.startsWith(exF_path)) {
                error("Source path entry '" + f.getPath()
                        + "' is a subset of '" + exF.getPath() + "'");
                return false;
            }
        }
        return true;
    }

    /**
     * This method is trying to find a file by given string. First it trying to
     * find a file as absolute path then relatively to current user directory
     * then in classpath.
     */
    private static File findFile(String fPath, boolean isDirectory) {
        // check path as absolute path
        File f = new File(fPath);
        if (f.exists() && (f.isFile() ^ isDirectory))
            return f;
        // check path relatively to current directory
        f = new File(System.getProperty("user.dir"), fPath);
        if (f.exists() && (f.isFile() ^ isDirectory))
            return f;
        // looking file in classpath
        URL url = MdlCompiler.class.getResource(fPath);
        if (url != null) {
            try {
                f = new File(url.toURI());
                if (f.exists() && (f.isFile() ^ isDirectory))
                    return f;
            } catch (URISyntaxException e) {
                return null;
            }
        }
        return null;
    }

    @SuppressWarnings( { "StringConcatenationInsideStringBufferAppend" })
    public static void printUsage(PrintStream pStream) {
        PrintStream ps = (pStream == null) ? System.out : pStream;
        ps.append("MdlCompiler v0.1" + nl);
        ps.append("Usage: java MdlCompiler [-options]" + nl);
        ps.append("where:" + nl);
        ps.append("-s <path>         Where to find input source files." + nl);
        ps.append("                  Defined like Java classpath." + nl);
        ps.append("                  Default: current dirrectory." + nl);
        ps.append("-d <directory>    Where to place generated classes." + nl);
        ps.append("                  Default: is current dirrectory." + nl);
        ps.append("-e <encoding>     Character encoding used by source files."
                + nl);
        ps.append("                  Default: system default encoding." + nl);
        ps.append("" + nl);
    }

    private static void debug(String s) {
        if (DEBUG)
            System.out.print("DEBUG (MdlCompiler): " + s + nl);
    }

    private static void error(String s) {
        System.out.print("MdlCompiler ERROR: " + s + nl);
    }

    private static void warning(String s) {
        System.out.print("MdlCompiler WARNING: " + s + nl);
    }

    public String getEncoding() {
        return this.encoding;
    }

    public Map<File, String> getSourceFilePackages() {
        return this.sourceFilePackages;
    }

    public String getSourceFilePackage(File sourceFile) {
        return this.sourceFilePackages.get(sourceFile);
    }

    public Map<File, CompilationUnit> getSourceFileUnits() {
        return this.sourceFileUnits;
    }

    public CompilationUnit getSourceFileUnit(File sourceFile) {
        return this.sourceFileUnits.get(sourceFile);
    }

    public List<File> getSrcPath() {
        return this.srcPath;
    }

    public File getOutDir() {
        return outDir;
    }

    public void printMessages(PrintStream ps) {
        MessageCollector mc = getAllMessages();
        mc.printErrors(ps);
        mc.printWarnings(ps);
        ps.println("Compiled " + sourceFilePackages.size() + " files. Found "
                + mc.getErrors().size() + " errors, " + mc.getWarnings().size()
                + " warnings.");
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage(null);
        } else {
            MdlCompiler compiler = parseArgs(args);
            if (compiler != null) {
                try {
                    // BaseNode parseTree = compiler.parse();
                    compiler.parse();
                    // System.out.println(parseTree.dumpParseTree(""));
                    compiler.doSemanticCheck();
                    // print errors
                    compiler.printMessages(System.err);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                MdlFSMBuilder fsmBuilder = new MdlFSMBuilder();

                CompilationUnit compilationUnit = compiler.getSourceFileUnits().values().iterator().next();

                int i=0;
                for (MeterDescription description : compilationUnit.getMeterDescriptionList()) {
                    Meter meter = new Meter(description,fsmBuilder, true, i++);
                    System.out.println(meter);
                }
            }
        }
    }

    public List<CompileMessage> getUnparsedSourceFilesErrors() {
        return unparsedSourceFilesErrors;
    }
}
