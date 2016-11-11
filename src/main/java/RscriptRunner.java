import org.rosuda.JRI.Rengine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;


/**
 * Created by kitae on 7/18/16.
 */
public class RscriptRunner {

    /**
     * Runs Rscipt based on commandline command. Rscript must be available for commandline.
     * @param rscriptPath path to Rscript
     * @param programLoc path to this program
     * @param expName unique experiment name
     */
    static void Rconnection(String rscriptPath, String programLoc, String expName) {
        try {
            System.out.println("Initializing Rscript for " + expName + " . . . " + rscriptPath);
            System.out.println("This might take a few minutes . . .");
            Runtime rt = Runtime.getRuntime();
            Process process = rt.exec("Rscript --vanilla " + rscriptPath + " " + programLoc +" " + expName );
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            process.waitFor();
            String line;
            if (RNAdataConvertor.isVerbose) {
                while ((line = errorReader.readLine()) != null) {
                    System.out.println(line);
                }
            }
            System.out.println("Rscript is complete!");
        } catch (IOException e) {
            System.err.println("IOException during connection to Rscript.");
            e.printStackTrace();
            RNAdataConvertor.forceEndProgram();
        } catch (InterruptedException e) {
            System.err.println("There was an interruption while running a Rscript!");
            e.printStackTrace();
            RNAdataConvertor.forceEndProgram();
        }
    }

}
