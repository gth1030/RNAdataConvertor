import java.util.ArrayList;

/**
 * Created by kitae on 7/19/16.
 */
public class ExpListKeeper {

    public ExpListKeeper(String experimentName) {
        expName = experimentName;
    }


    /** TSV file that maps official gene name to Entrez gene ID **/
    String geneNameToIdPath;
    /** unique official experiment name to identify each experiment. **/
    String expName;
    /** path to control data files. **/
    ArrayList<String> control = new ArrayList<String>();
    /** path to experiment data files. **/
    ArrayList<String> experiment = new ArrayList<String>();
}
