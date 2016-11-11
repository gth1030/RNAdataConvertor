
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

import java.io.*;
import java.util.Map;
import java.util.Set;


/**
 * Created by kitae on 7/15/16.
 */
public class RNAdataConvertor {

    /**
     * Parse arguments to define program environment.
     * @param args program arguments
     */
    void setUpArgParser(String[] args) {
        ArgumentParser argpharser = ArgumentParsers.newArgumentParser("argReader").
                description("Read arguments to get configuration file and program setup");
        argpharser.addArgument("--verbose").action(Arguments.storeTrue()).help("produce more detailed program statement.");
        argpharser.addArgument("configFile").type(String.class).nargs("?").
                help("path for configuration file. Eample configuration file can be found in program's example folder.");
        try {
            Namespace nameSpace = argpharser.parseArgs(args);
            isVerbose = nameSpace.get("verbose");
            configPath = nameSpace.get("configFile");
        } catch (ArgumentParserException e) {
            argpharser.handleError(e);
            forceEndProgram();
        }
    }


    /**
     * default constructor. Sets up temporary directory for temp files.
     */
    public RNAdataConvertor() {
        if (tempFile == null) {
            tempFile = new File(tempfilePath);
            tempFile.mkdir();
        }
    }

    /**
     * Terminates program in case of an error. All temp files are removed.
     */
    static void forceEndProgram() {
        try {
            deleteFile(tempFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Program terminated.");
        System.exit(1);
    }

    /**
     * Remove files recursively.
     * @param file file to be removed.
     * @throws IOException
     */
    static void deleteFile(File file) throws IOException {
        if (file.isDirectory()) {
            for (File c : file.listFiles())
                deleteFile(c);
        }
        if (!file.delete())
            throw new FileNotFoundException("Failed to delete file: " + file);
    }





    public static void main(String[] args) {
        RNAdataConvertor convertor = new RNAdataConvertor();
        convertor.setUpArgParser(args);
        Set<ExpListKeeper> expKeeperSet = FilePharser.parseConfig(configPath);
        FilePharser.generateTempFile(tempfilePath, expKeeperSet);
        for (ExpListKeeper expList : expKeeperSet) {
            try {
                RscriptRunner.Rconnection(RscriptPath, new File(".").getCanonicalPath(), expList.expName);
            } catch (IOException e) {
                System.out.println("IOException while getting path location for configuration file.");
                e.printStackTrace();
                forceEndProgram();
            }
            Map<String, String> geneNameToIdMap = FilePharser.generateGeneNameToIDmap(expList.geneNameToIdPath);
            FilePharser.generateTsvFromData(tempfilePath + "/" + expList.expName + "/anal.csv", System.getProperty("user.dir") + "/" + expList.expName + "_result.tsv", geneNameToIdMap);
        }
        try {
            deleteFile(tempFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("TSV file is properly generated.");
        System.exit(0);

    }

    /** temporary file directory that contains all temporary files. **/
    public static File tempFile;
    /** temporary file directory path. **/
    private static String tempfilePath = "./src/main/resources/temp";
    /** Configuration file path. Default is set to example folder, but fed by program arguments. **/
    private static String configPath = "./examples/configuration.config";
    /** if true, print out more detailed program progress **/
    static boolean isVerbose = false;
    /** Path to Rscript **/
    private static String RscriptPath = "./src/main/resources/mRNAFeatureCountAnalyzer.r";

}
