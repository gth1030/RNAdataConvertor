import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.*;
import java.util.*;

/**
 * Created by kitae on 7/15/16.
 */
public class FilePharser {


    /**
     * Convert information from Rscript to tsv file for uploader. Also calculates rank and percentile.
     * Order of geneNames are already sorted for order which depends on Rscript. By default the order is assigned by P-value.
     * @param inputFilePath  path to datafile.
     * @param destinationFilePath Tsv file destination.
     * @param geneNameToIDmap Map that maps gene name to Entrez ID.
     */
    static void generateTsvFromData(String inputFilePath, String destinationFilePath, Map<String, String> geneNameToIDmap) {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(destinationFilePath));
            CSVParser parser  = new CSVParser(new BufferedReader(new FileReader(inputFilePath)), CSVFormat.EXCEL.withHeader());
            List<CSVRecord> records = parser.getRecords();
            Map<String, Integer> headerMap = parser.getHeaderMap();
            Map<String, GeneData> geneDataMap = new HashMap<String, GeneData>();
            Set<GeneData> duplicateGenes = new HashSet<GeneData>();
            Set<String> unmappedGenes = new HashSet<String>();
            bw.write("GeneName\tGeneID\tFoldChange\tPvalue\trank\tpercentile\n");
            String prevVal = null;
            int rank = 1;
            int rankIncrement = 0;
            for (int i = 1; i < records.size(); i++) {
                CSVRecord line = records.get(i);
                if (geneNameToIDmap.containsKey(line.get(""))) {
                    if (prevVal != null && !prevVal.equals(line.get("pvalue"))) {
                        rank += rankIncrement;
                        rankIncrement = 1;
                    } else {
                        rankIncrement += 1;
                    }
                    prevVal = line.get("pvalue");
                    GeneData geneData = new GeneData(line.get(""), geneNameToIDmap.get(line.get("")), line.get("log2FoldChange"), line.get("pvalue"), rank);
                    if (geneDataSanityCheck(geneData, geneDataMap, duplicateGenes)) {
                        geneDataMap.put(geneData.geneID, geneData);
                    }
                } else {
                    unmappedGenes.add(line.get(""));
                }
            }
            int totalGeneData = geneDataMap.size();
            for (GeneData geneData : geneDataMap.values()) {
                geneData.percentile = Float.toString((float) (totalGeneData - Integer.parseInt(geneData.rank)) / (float) totalGeneData);
                bw.write(geneData.toString() + "\n");
            }
            bw.flush();
            bw.close();
            if (unmappedGenes.size() > 0) {
                System.out.println("Number of unmapped genes: " + unmappedGenes.size() + "/" + totalGeneData);
            }
            if (duplicateGenes.size() > 0) {
                System.out.println("Number of duplicate gene ID detected: " + duplicateGenes.size());
                System.out.println("Significant genes are: ");
                for (GeneData geneData : duplicateGenes) {
                    if (Float.parseFloat(geneData.pvalue) < .05) {
                        System.out.print(geneData.geneName + " pvalue=" + geneData.pvalue + " ");
                    }
                }
                System.out.print("\n");
            }
            if (RNAdataConvertor.isVerbose) {
                System.out.println("Unmapped gene names are: " + unmappedGenes.toString());
                System.out.println("Duplicate gene names are: " + duplicateGenes.toString());
            }

        } catch (IOException e) {
            System.err.println("IOException occured while reading and writing for input file of " + inputFilePath + ". " + e);
            System.err.println("Check if Rscript is available for the commandline!");
            RNAdataConvertor.forceEndProgram();
        }
    }


    /** Creates a map that maps official gene name to Entrez gene ID.
     *
     * @param geneNameToIDFilepath TSV file that maps Gene name to gene ID.
     * @return map that contains gene name as a key, and gene ID as a value.
     */
    static Map<String, String> generateGeneNameToIDmap (String geneNameToIDFilepath) {
        Map<String, String> geneNameToIDmap = new HashMap<String, String>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(geneNameToIDFilepath));
            String line = br.readLine();
            while((line = br.readLine()) != null) {
                try {
                    geneNameToIDmap.put(line.split("\t")[0].trim(), line.split("\t")[1].trim());
                } catch (IndexOutOfBoundsException iobe){
                    if (line.split("\t").length > 0) {
                        System.err.println("geneName with " + line.split("\t")[0] + " is unmapped in geneNameMapping file.");
                    }
                    continue;
                }
            }
        } catch (IOException e) {
            System.err.println("IOException while reading gene name to ID map file. " + e);
            RNAdataConvertor.forceEndProgram();
        }
        return geneNameToIDmap;
    }

    /**
     * Parse configuration file to find a path to gene name to ID file, and data files.
     * @param configurationPath path to configuration file. Example of configuration file can be found in example folder.
     * @return returns set that contains sets of experiment to be analyzed.
     */
    static Set<ExpListKeeper> parseConfig (String configurationPath) {
        Map<String, ExpListKeeper> expMap = new HashMap<String, ExpListKeeper>();
        try {
            BufferedReader configReader = new BufferedReader(new FileReader(configurationPath));
            String line;
            String expName = null;
            while((line = configReader.readLine()) != null) {
                if (line.length() > 1 && line.substring(0,1).equals("#")) {
                    continue;
                }
                if (line.length() > 7 && line.substring(0, 7).equals("expName")) {
                    if (line.substring(line.indexOf(":")).trim().equals("")) {
                        System.out.println("Unspecified expname!");
                        RNAdataConvertor.forceEndProgram();
                    }
                    expName = line.substring(line.indexOf(":") + 1).trim();
                    if (expMap.containsKey(expName)) {
                        System.out.println("Duplicate expName found!");
                        RNAdataConvertor.forceEndProgram();
                    }
                    expMap.put(expName, new ExpListKeeper(expName));
                } else if (line.length() > 7 && line.substring(0, 7).equals("control") && expName != null) {
                    expMap.get(expName).control.add(getTruePath(configurationPath, line.substring(line.indexOf(":") + 1).trim()));
                } else if (line.length() > 10 && line.substring(0, 10).equals("experiment") && expName != null) {
                    expMap.get(expName).experiment.add(getTruePath(configurationPath, line.substring(line.indexOf(":") + 1).trim()));
                } else if (line.length() > 16 && line.substring(0, 16).equals("geneNameToIdpath") && expName != null) {
                    expMap.get(expName).geneNameToIdPath = getTruePath(configurationPath, line.substring(line.indexOf(":") + 1).trim());
                } else {
                    if (line.trim().equals("")) { continue; }
                    System.out.println("unrecognized line is dected! " + line);
                }
            }
        } catch (IOException e) {
            System.err.println("Error while reading configuration file.");
            e.printStackTrace();
            RNAdataConvertor.forceEndProgram();
        }
        return new HashSet<ExpListKeeper>(expMap.values());
    }

    /**
     * The function generates temporary file for Rscript. The temporary file provides path to data file in a format that
     * Rscript can take.
     * @param tempFileLoc destination path for tempFile.
     * @param expSet Set of object that contains experiment name and data path for each experiment.
     */
    static void generateTempFile(String tempFileLoc, Set<ExpListKeeper> expSet) {
        try {
            for (ExpListKeeper expList : expSet) {
                new File(tempFileLoc + "/" + expList.expName).mkdir();
                BufferedWriter expbw = new BufferedWriter(new FileWriter(tempFileLoc + "/" + expList.expName + "/exp.temp"));
                for (String exp : expList.experiment) {
                    expbw.write(exp + "\n");
                }
                BufferedWriter conbw = new BufferedWriter(new FileWriter(tempFileLoc + "/" + expList.expName + "/con.temp"));
                for (String con : expList.control) {
                    conbw.write(con + "\n");
                }
                expbw.flush();
                expbw.close();
                conbw.flush();
                conbw.close();
            }

        } catch (IOException e) {
            System.err.println("IOException while writing temporary file for Rscript!");
            e.printStackTrace();
            RNAdataConvertor.forceEndProgram();
        }
    }

    /**
     * The function provides absolute path for given path in configuration file. The absolute path is constructed if relative
     * path is used in configuration file. Relative path is converted to absolute path based on configuration file's absolute path.
     * @param configPath path to configuration file.
     * @param filePath path of a file to be fixed if relative path is used.
     * @return non relative path of a filepath
     */
    static String getTruePath(String configPath, String filePath) {
        if (filePath.length() > 1 && filePath.substring(0, 2).equals("./")) {
            try {
                return new File(configPath).getParentFile().getCanonicalPath() + filePath.substring(1);
            } catch (IOException e) {
                System.err.println("IOException while extracting absolute path of configuration file!");
                e.printStackTrace();
                RNAdataConvertor.forceEndProgram();
            }
        }
        return filePath;
    }


    /**
     * Checks if there are genes that maps to same geneID, if there are ones, use one with more significant p-value.
     * @param geneData individual gene data
     * @param geneMap gene mapping that contains genes that are added to maps so far.
     * @param duplicateOnes set that contains all of duplicate geneData.
     * @return true if gene needs to be added to the map, false, if gene is duplicate and does not needed to be added.
     */
    static boolean geneDataSanityCheck(GeneData geneData, Map<String, GeneData> geneMap, Set<GeneData> duplicateOnes) {
        if (geneMap.containsKey(geneData.geneID)) {
            if (Float.parseFloat(geneData.pvalue) >= Float.parseFloat(geneMap.get(geneData.geneID).pvalue)) {
                duplicateOnes.add(geneData);
                return false;
            } else {
                duplicateOnes.add(geneMap.get(geneData.geneID));
                geneMap.remove(geneData.geneID);
                return true;
            }
        }
        return true;
    }

}
