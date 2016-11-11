/**
 * Created by kitae on 7/16/16.
 */
public class GeneData {

    /**
     * Individual data point is represented as geneData.
     * @param gname Official gene name
     * @param gID Entrez Gene ID
     * @param fc log fold change value.
     * @param pval p-value of an experiment
     * @param rankval rank of the data point. By default it is built on p-value.
     */
    public GeneData(String gname, String gID, String fc, String pval, int rankval) {
        geneName = gname;
        geneID = gID;
        foldChange = fc;
        pvalue = verifyPval(pval);
        rank = Integer.toString(rankval);
    }

    /**
     * Checks if p-value is NA and if it is, return 1.
     * @param pval
     * @return filtered p-value.
     */
    private String verifyPval(String pval) {
        if (pval.equals("NA")) {
            return "1";
        }
        return pval;
    }

    /**
     * Provides String representaion of the object for tsv file formation.
     * @return String representation of the GeneData
     */
    @Override
    public String toString() {
        return geneName + "\t" + "GeneID:" + geneID + "\t" +foldChange + "\t" +pvalue + "\t" +rank + "\t" +percentile;
    }


    String geneName;
    String geneID;
    String foldChange;
    String pvalue;
    String rank;
    String percentile;

}
