package com.astrazeneca.vardict.integrationtests;

import com.astrazeneca.vardict.CmdParser;
import com.astrazeneca.vardict.Configuration;
import com.astrazeneca.vardict.VarDictLauncher;
import com.astrazeneca.vardict.data.ReferenceResource;
import com.astrazeneca.vardict.data.scopedata.GlobalReadOnlyScope;
import com.astrazeneca.vardict.integrationtests.utils.CSVReferenceManager;
import com.astrazeneca.vardict.integrationtests.utils.VarDictInput;

import java.io.*;
import java.util.Arrays;
import java.util.Scanner;

/**
 * Standalone runner for VarDict's integration test cases (no TestNG/Mockito required).
 * Reproduces IntegrationTest exactly: CSV-mocked reference, BAM resources, byte-compare stdout to the
 * expected output stored in each testdata/integrationtestcases/*.txt file.
 */
public class IntegrationRunner {
    static final String CASES = "testdata/integrationtestcases/";
    static final String RES = "src/test/resources/com/astrazeneca/vardict/integrationtests/";
    static final String DEFAULT_ARGS = "-z -c 1 -S 2 -E 3 -g 4 -G ";

    // ReferenceResource whose retrieveSubSeq is served from the CSV reference (as IntegrationTest mocks).
    static class CsvRef extends ReferenceResource {
        final String fastaName;
        CsvRef(String fastaName) { this.fastaName = fastaName; }
        @Override
        public String[] retrieveSubSeq(String fasta, String chr, int start, int end) {
            return CSVReferenceManager.getReader(fastaName).queryFasta(chr, start, end);
        }
    }

    static File makeBed(VarDictInput in) throws IOException {
        File bed = File.createTempFile("tmpbed", ".bed"); bed.deleteOnExit();
        try (FileWriter w = new FileWriter(bed)) {
            w.write(in.startAmp == null
                ? in.chr + "\t" + in.start + "\t" + in.end + "\ttestbed"
                : in.chr + "\t" + in.start + "\t" + in.end + "\ttestbed\t0\t.\t" + in.startAmp + "\t" + in.endAmp);
        }
        return bed;
    }

    static String bamPath(String bam) {
        String[] bams = bam.split("\\|");
        if (bams.length > 1) return RES + bams[0] + "|" + RES + bams[1];
        return RES + bam;
    }

    public static void main(String[] args) throws Exception {
        CSVReferenceManager.init();
        File dir = new File(CASES);
        File[] files = dir.listFiles((d, n) -> n.endsWith(".txt"));
        Arrays.sort(files);
        int pass = 0, fail = 0;
        StringBuilder fails = new StringBuilder();
        PrintStream realOut = System.out;
        for (File f : files) {
            VarDictInput in; StringBuilder expected = new StringBuilder();
            try (Scanner s = new Scanner(f)) {
                in = VarDictInput.fromCSVLine(s.nextLine());
                while (s.hasNext()) expected.append(s.nextLine()).append("\n");
            }
            File bed = makeBed(in);
            String cmd = bed + " -b " + bamPath(in.bam) + " "
                    + (in.args.isEmpty() ? "" : in.args + " ") + DEFAULT_ARGS + in.fasta + ".csv";
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            boolean ok;
            try {
                System.setOut(new PrintStream(buf));
                Configuration conf = new CmdParser().parseParams(cmd.split("\\s"));
                new VarDictLauncher(new CsvRef(in.fasta)).start(conf);
                ok = expected.toString().equals(buf.toString());
            } catch (Throwable t) {
                ok = false; buf.reset(); new PrintStream(buf).println("EXCEPTION: " + t);
            } finally {
                System.setOut(realOut);
                GlobalReadOnlyScope.clear();
            }
            if (ok) { pass++; }
            else { fail++; fails.append("  FAIL: ").append(f.getName()).append("\n"); }
        }
        realOut.println("Integration tests: " + pass + " passed, " + fail + " failed, of " + files.length);
        if (fail > 0) realOut.print(fails);
        System.exit(fail == 0 ? 0 : 1);
    }
}
