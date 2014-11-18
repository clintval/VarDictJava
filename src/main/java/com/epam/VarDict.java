package com.epam;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.epam.VarDict.Flag;
import com.epam.VarDict.SamtoolsReader;
import com.epam.VarDict.Sclip;
import com.epam.VarDict.Variation;

public class VarDict {

    final static Pattern SN = Pattern.compile("\\s+SN:(\\S+)");
    final static Pattern LN = Pattern.compile("\\sLN:(\\d+)");

    public static void main(String[] args) throws Exception {

        String xx = "200SIxx4544IDyyy555";
        List<String> l = new LinkedList<>();
        Matcher xxm = Pattern.compile("(\\d+)([A-Z])").matcher(xx);
        while (xxm.find()) {
            l.add(xxm.group(1));
            l.add(xxm.group(2));
        }

        System.err.println(l);


        if (true)
            return;

        Matcher m = D_S_D_ID.matcher("123S77DIxxxxxxxxxxxxxx4456SIyyyy");
        if (m.find()) {
            String tslen = Integer.parseInt(m.group(1)) + Integer.parseInt(m.group(2)) + "S";
            System.err.println(m.replaceFirst(tslen));
        }


        System.err.println("asdfasdf\tSA:Z:ererer  asdasd".matches(".*\\tSA:Z:(\\S+).*"));



        System.err.println("123Ssdfgsdfg".matches("^(\\d+)S.*"));
        System.err.println("saffasf123S".matches("^.*(\\d+)S$"));

//      if ( $a[5] =~ /^(\d+)S/ ) {



        Matcher nmMatcher = NUMBER_MISMATCHES.matcher("nM:i:2345asdd");
        if (nmMatcher.find()) {
            System.err.println(nmMatcher.group(1));
        }



        Matcher matcher = INDEL.matcher("123I45D666ID");
        while(matcher.find()) {
            System.err.println(matcher.group(1));
        }

        Matcher matcher2 = INDEL.matcher("123I45D666ID");
        System.err.println(matcher2.matches());
//        if (matcher2.matches()) {
//            System.err.println("yyy");
//            for (int i = 1; i <= matcher2.groupCount(); i++) {
//                System.err.println(matcher2.group(i));
//            }
//        }

        if (true)
            return;

        Bam bam = new Bam("/Users/kirst/Work/bcbio.coverage/test/data/aligned-reads.bam");

        Map<String, Integer> chr = readCHR(bam.getBamX());

        for (Map.Entry<String, Integer> string : chr.entrySet()) {
            System.out.println(string.getKey() + " == " + string.getValue());
        }
    }

    public static Map<String, Integer> readCHR(String bam) throws IOException {
        try (SamtoolsReader reader = new SamtoolsReader("view", "-H", bam)) {
            Map<String, Integer> chrs = new HashMap<>();
            String line;
            while ((line = reader.read()) != null) {
                if (line.startsWith("@SQ")) {
                    Matcher sn = SN.matcher(line);
                    Matcher ln = LN.matcher(line);
                    if (sn.find() && ln.find()) {
                        chrs.put(sn.group(1), Integer.parseInt(ln.group(1)));
                    }
                }
            }
            return chrs;
        }
    }

    public static class Configuration {
        String rawBam;
        String delimiter;
        String bed;
        int numberNucleotideToExtend;
        boolean zeroBased;
        String ampliconBasedCalling; //-a
        int columnForChromosome = -1;
        String sampleNameRegexp; // -n
        String sampleName; //-N
        String fasta;
        Bam bam;
        Double downsampling;
        boolean chromosomeNameIsNumber;
        Integer mappingQuality;//-Q
        boolean  nonPrimaryAlignment; //-F
        boolean removeDuplicatedReads; //-t
        int mismatch; //-m, default = 8
        boolean y; //-y TODO ???
        int goodq; // -q, default = 23
        final int buffer = 200;
        int vext = 3; // -X, default 3
        int trimBasesAfter = 0; // -T, Trim bases after [INT] bases in the reads
        boolean performLocalRealignment; // -k, default false

        public boolean isColumnForChromosomeSet() {
            return columnForChromosome >= 0;
        }

        public boolean isDownsampling() {
            return downsampling != null;
        }

        public boolean hasMappingQuality() {
            return mappingQuality != null;
        }



    }

    public static class Region {
        final String chr;
        final int start;
        final int end;
        final String gene;
        int istart;
        int iend;

        public Region(String chr, int start, int end, String gene) {
            this.chr = chr;
            this.start = start;
            this.end = end;
            this.gene = gene;
        }

        public Region(String chr, int start, int end, String gene, int istart, int iend) {
            this(chr, start, end, gene);
            this.istart = istart;
            this.iend = iend;
        }

        public String getChr() {
            return chr;
        }

        public String getGene() {
            return gene;
        }

        public int getStart() {
            return start;
        }

        public int getEnd() {
            return end;
        }

        public int getIStart() {
            return istart;
        }

        public int getIEnd() {
            return iend;
        }
    }

    final static Comparator<Region> ISTART_COMPARATOR = new Comparator<Region>() {
        @Override
        public int compare(Region o1, Region o2) {
            return Integer.compare(o1.istart, o2.istart);
        }
    };

    public static void buildRegionsFromFile(Configuration conf) throws IOException {
        String a = conf.ampliconBasedCalling;
        List<String> segraw = new ArrayList<>();
        boolean zeroBased = conf.zeroBased;

        try (BufferedReader bedFileReader = new BufferedReader(new FileReader(conf.bed))) {
            String line;
            while ((line = bedFileReader.readLine()) != null) {
                if (line.startsWith("#")
                        || line.startsWith("browser")
                        || line.startsWith("track")) {
                    continue;
                }
                if (a == null) {
                    String[] ampl = line.split(conf.delimiter);
                    if (ampl.length == 8) {
                        try {
                            int a1 = Integer.parseInt(ampl[1]);
                            int a2 = Integer.parseInt(ampl[2]);
                            int a6 = Integer.parseInt(ampl[6]);
                            int a7 = Integer.parseInt(ampl[7]);
                            if (a6 > a1 && a7 < a2) {
                                a = "10:0.95";
                                zeroBased = true;
                            }
                        } catch (NumberFormatException e) {
                            continue;
                        }
                    }
                    segraw.add(line);
                }

            }
        }
        List<List<Region>> segs = new LinkedList<>();

        if (a != null) {
            Map<String, List<Region>> tsegs = new HashMap<>();
            for (String string : segraw) {
                String[] split = string.split(conf.delimiter);
                String chr = split[0];
                int start = Integer.parseInt(split[1]);
                int end = Integer.parseInt(split[2]);
                String gene = split[3];
                int istart = Integer.parseInt(split[6]);
                int iend = Integer.parseInt(split[7]);
                if (zeroBased) {
                    start++;
                    end++;
                }
                List<Region> list = tsegs.get(chr);
                if (list == null) {
                    list = new ArrayList<>();
                    tsegs.put(chr, list);
                }
                list.add(new Region(chr, start, end, gene, istart, iend));
            }
            for (Map.Entry<String, List<Region>> entry : tsegs.entrySet()) {
                List<Region> regions = entry.getValue();
                Collections.sort(regions, ISTART_COMPARATOR);
                String pchr = null;
                int pend = -1;
                List<Region> list = new LinkedList<>();
                segs.add(list);
                for (Region region : regions) {
                    if (pend != -1 && (!region.getChr().equals(pchr) || region.getIStart() > pend)) {
                        list = new LinkedList<>();
                        segs.add(list);
                    }
                    list.add(region);
                    pchr = region.getChr();
                    pend = region.getIEnd();
                }
            }
            ampVardict(segs);
        } else {
            BedRowFormat format = DEFAULT_BED_ROW_FORMAT;
            for (String seg : segraw) {
                String[] splitA = seg.split(conf.delimiter);
                if (!conf.isColumnForChromosomeSet() && splitA.length == 4) {
                    try {
                        int a1 = Integer.parseInt(splitA[1]);
                        int a2 = Integer.parseInt(splitA[2]);
                        if (a1 <= a2) {
                            format = CUSTOM_BED_ROW_FORMAT;
                        }
                    } catch (NumberFormatException e) {
                    }
                }
                String chr = splitA[format.chrColumn];
                int cdss = Integer.parseInt(splitA[format.startColumn]);
                int cdse = Integer.parseInt(splitA[format.endColumn]);
                String gene = format.geneColumn < splitA.length ? splitA[format.geneColumn] : chr;

                String[] starts = splitA[format.thickStartColumn].split(","); // TODO why?
                String[] ends = splitA[format.thickEndColumn].split(",");
                List<Region> cds = new LinkedList<>();
                for (int i = 0; i < starts.length; i++) {
                    int s = Integer.parseInt(starts[i]);
                    int e = Integer.parseInt(ends[i]);
                    if (cdss > e) {
                        continue;
                    }
                    if (cdse > e) {
                        break;
                    }
                    if (s < cdss)
                        s = cdss;
                    if (e > cdse)
                        e = cdse;
                    s -= conf.numberNucleotideToExtend;
                    e += conf.numberNucleotideToExtend;
                    if (zeroBased)
                        s++;
                    cds.add(new Region(chr, s, e, gene));
                }
                segs.add(cds);
            }
        }
    }

    public static class Bam {
        private final String[] bamNames;
        private final String[] bams;

        public Bam(String value) {
            bamNames = value.split("\\|");
            bams = bamNames[0].split(":");
        }

        public String getBam1() {
            return bamNames[0];
        }

        public String getBam2() {
            return hasBam2() ? bamNames[1] : null;
        }

        public String getBamX() {
            return bams[0];
        }

        public boolean hasBam2() {
            return bamNames.length > 1;
        }
    }

    public void finalCicle(List<List<Region>> segs, Map<String, Integer> chrs, Map<String, Integer> SPLICE, Configuration conf, String ampliconBasedCalling) throws IOException {
        for (List<Region> list : segs) {
            for (Region region : list) {
                if (conf.bam.hasBam2()) {
                    somdict(region, toVars(region, conf.bam.getBam1(), chrs, SPLICE, conf, ampliconBasedCalling), toVars(region, conf.bam.getBam2(), chrs, SPLICE, conf, ampliconBasedCalling));
                } else {
                    vardict(region, toVars(region, conf.bam.getBam1(), chrs, SPLICE, conf, ampliconBasedCalling));
                }
            }
        }
    }

    final static Pattern SAMPLE_PATTERN = Pattern.compile("([^\\/\\._]+).sorted[^\\/]*.bam");
    final static Pattern SAMPLE_PATTERN2 = Pattern.compile("([^\\/]+)[_\\.][^\\/]*bam");

    public static List<String> getSample(String bam1, String bam2, String sampleName, String regexp) {
        String sample = null;
        String samplem = "";

        if (sampleName != null) {
            sample = sampleName;
        } else if (regexp != null) {
            Pattern rn = Pattern.compile(regexp);
            Matcher m = rn.matcher(bam1);
            if (m.find()) {
                sample = m.group(1);
            }
        } else {
            Matcher matcher = SAMPLE_PATTERN.matcher(bam1);
            if (matcher.find()) {
                sample = matcher.group(1);
            } else {
                matcher = SAMPLE_PATTERN2.matcher(bam1);
                if (matcher.find()) {
                    sample = matcher.group(1);
                }
            }
        }

        if (bam2 != null) {
            if (regexp != null) {
                Pattern rn = Pattern.compile(regexp);
                Matcher m = rn.matcher(bam1);
                if (m.find()) {
                    sample = m.group(1);
                }
                m = rn.matcher(bam2);
                if (m.find()) {
                    samplem = m.group(1);
                }
            } else {
                if (sampleName != null) {
                    String[] split = sampleName.split("\\|");
                    sample = split[0];
                    if (split.length > 1) {
                        samplem = split[1];
                    } else {
                        samplem = split[0] + "_match";
                    }
                }
            }
        }

        return Collections.unmodifiableList(Arrays.asList(sample, samplem));
    }

    private void somdict(Region region, Object vars, Object vars2) {
        // TODO Auto-generated method stub

    }

    private void vardict(Region region, Object vars) {
        // TODO Auto-generated method stub

    }

    public static String[] retriveSubSeq(String fasta, String chr, int start, int end) throws IOException {

        try (SamtoolsReader reader = new SamtoolsReader("faidx", fasta, chr + ":" + start + "-" + end)) {
            String header = reader.read();
            String exon = reader.read();
            return new String[] { header, exon.replaceAll("\\s+", "") };
        }

    }

    final static Random RND = new Random(System.currentTimeMillis());
    private static final Pattern INDEL = Pattern.compile("(\\d+)[ID]");
    private static final Pattern NUMBER_MISMATCHES = Pattern.compile("(?i)NM:i:(\\d+)");
    private static final Pattern MATCH_INSERTION = Pattern.compile("(\\d+)[MI]");
    private static final Pattern SOFT_CLIPPED = Pattern.compile("(\\d+)[MIS]");
    private static final Pattern ALIGNED_LENGTH = Pattern.compile("(\\d+)[MD]");
    private static final Pattern CIGAR_PAIR = Pattern.compile("(\\d+)([A-Z])");



    public static <K, V> V getOrElse(Map<K, V> map, K key, V or) {
        V v = map.get(key);
        if (v == null) {
            v = or;
            map.put(key, v);
        }
        return v;
    }

    private static Variation getVariation(Sclip sclip, int idx, Character ch) {
        Map<Character, Variation> map = sclip.seq.get(idx);
        if (map == null) {
            map = new HashMap<>();
            sclip.seq.put(idx, map);
        }
        Variation variation = map.get(ch);
        if (variation == null) {
            variation = new Variation();
            map.put(ch, variation);
        }
        return variation;
    }


    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static void incCnt(Map cnts, Object key, int add) {
        Integer integer = (Integer)cnts.get(key);
        if (integer == null) {
            cnts.put(key, add);
        } else {
            cnts.put(key, integer + add);
        }
    }

    private static Variation getVariation(Map<Integer, Map<String, Variation>> hash, int start, String ref) {
        Map<String, Variation> map = hash.get(start - 1);
        if (map == null) {
            map = new HashMap<>();
            hash.put(start - 1, map);
        }
        Variation variation = map.get(ref);
        if (variation == null) {
            variation = new Variation();
            map.put(ref, variation);
        }
        return variation;
    }

    private static void subCnt(Variation vref, boolean dir, int rp, int q, int Q, int nm, Configuration conf) {
        // ref dir read_position quality
        vref.cnt--;
        vref.decDir(dir);
        vref.pmean -= rp;
        vref.qmean -= q;
        vref.Qmean -= Q;
        vref.nm -= nm;
        if (q >= conf.goodq) {
            vref.hicnt--;
        } else {
            vref.locnt--;
        }
    }


    private static void addCnt(Variation vref, boolean dir, int rp, int q, int Q, int nm, int goodq) {
        vref.cnt++;
        vref.incDir(dir);
        vref.pmean += rp;
        vref.qmean += q;
        vref.Qmean += Q;
        vref.nm += nm;
        if (q >= goodq) {
            vref.hicnt++;
        } else {
            vref.locnt++;
        }
    }

    public static class Sclip {
        Variation variation = new Variation();
        Map<Integer, Map<Character, Integer>> nt = new HashMap<>();
        Map<Integer, Map<Character, Variation>> seq = new HashMap<>();
    }


    public static class Variation {
        int cnt;
        Map<Boolean, int[]> dir = new HashMap<Boolean, int[]>() {{
            put(false, new int[1]);
            put(true, new int[1]);
        }};
        int pmean;
        int qmean;
        int Qmean;
        int nm;
        int locnt;
        int hicnt;

        boolean pstd;
        boolean qstd;
        int pp;
        int pq;
        int extracnt;

        public void incDir(boolean dir) {
            this.dir.get(dir)[0]++;
        }
        public void decDir(boolean dir) {
            this.dir.get(dir)[0]--;
        }
        public int getDir(boolean dir) {
            return this.dir.get(dir)[0];
        }
        public void addDir(boolean dir, int add) {
            this.dir.get(dir)[0] += add;
        }
        public void subDir(boolean dir, int sub) {
            this.dir.get(dir)[0] -= sub;
        }

    }

    public static class Flag {
        private int flag;

        public Flag(int flag) {
            this.flag = flag;
        }

        public boolean isSupplementaryAlignment() {
            return (flag & 0x800) != 0;
        }

        public boolean isUnmappedMate() {
            return (flag & 0x8) != 0;
        }

        public boolean isReverseStrand() {
            return (flag & 0x10) != 0;
        }

        public boolean isNotPrimaryAlignment() {
            return  (flag & 0x100) != 0;
        }

    }

    private static final Pattern D_S_D_ID = Pattern.compile("^(\\d+)S(\\d+)([ID])");
    private static final Pattern D_ID_D_S = Pattern.compile("(\\d+)([ID])(\\d+)S$");
    private static final Pattern D_S_D_M_ID = Pattern.compile("^(\\d+)S(\\d+)M(\\d+)([ID])");
    private static final Pattern D_ID_D_M_S = Pattern.compile("(\\d+)([ID])(\\d+)M(\\d+)S$");
    private static final Pattern D_M_D_ID = Pattern.compile("^(\\d+)M(\\d+)([ID])");
    private static final Pattern D_ID_D_M = Pattern.compile("(\\d+)([ID])(\\d+)M$");
    private static final Pattern D_M_D_DD_M_D_I_D_M_D_DD = Pattern.compile("^(.*?)(\\d+)M(\\d+)D(\\d)M(\\d+)I(\\d)M(\\d+)D");
    private static final Pattern D_M_D_DD_M_D_I_D_M_D_DD_prim = Pattern.compile("(\\d+)M(\\d+)D(\\d)M(\\d+)I(\\d)M(\\d+)D");
    private static final Pattern D_MIS = Pattern.compile("(\\d+)[MIS]");
    private static final Pattern D_MD = Pattern.compile("(\\d+)[MD]");
    private static final Pattern D_DD_D_M_D_DD_DI = Pattern.compile("(\\d+)D(\\d)M(\\d+)D(\\d+I)?");


    public static class SamtoolsReader implements AutoCloseable {

        private Process proc;
        private BufferedReader reader;

        public SamtoolsReader(String... args) throws IOException {
            List<String> list = new ArrayList<String>(1 + args.length);
            list.add("samtools");
            for (String arg : args) {
                list.add(arg);
            }
            ProcessBuilder builder = new ProcessBuilder(list);
            builder.redirectErrorStream(true);
            proc = builder.start();
            reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        }

        public String read() throws IOException {
            return reader.readLine();
        }

        @Override
        public void close() throws IOException {
            reader.close();
            proc.getInputStream().close();
            proc.getOutputStream().close();
            proc.destroy();
        }
    }


    private Object toVars(Region region, String bam, Map<String, Integer> chrs, Map<String, Integer> SPLICE, Configuration conf, String ampliconBasedCalling) throws IOException {
        String[] bams = bam.split(":");
        Map<Integer, Character> ref = new HashMap<>();
        Map<Integer, Map<String, Variation>> hash = new HashMap<>();
        Map<Integer, Map<String, Variation>> iHash = new HashMap<>();
        Map<Integer, Integer> cov = new HashMap<>();
        Map<Integer, Sclip> sclip3 = new HashMap<>();
        Map<Integer, Sclip> sclip5 = new HashMap<>();
        Map<Integer, Map<String, Integer>> inc = new HashMap<>();
        Map<Integer, Map<String, Integer>> mnp = new HashMap<>(); // Keep track of MNPs
        Map<Integer, Map<String, Integer>> dels5 = new HashMap<>(); // Keep track of MNPs
        for (String bami : bams) {
            int s_start = region.getStart() - conf.numberNucleotideToExtend - 700 < 1 ? 1 : region.getStart() - conf.numberNucleotideToExtend - 700;
            int len = chrs.containsKey(region.getChr()) ? chrs.get(region.getChr()) : 0;
            int s_end = region.getEnd() + conf.numberNucleotideToExtend + 700 > len ?
                    len : region.getEnd() + conf.numberNucleotideToExtend + 700;

            String[] subSeq = retriveSubSeq(conf.fasta, region.getChr(), s_start, s_end);
            String header = subSeq[0];
            String exon = subSeq[1];
            for(int i = s_start; i <= s_start + exon.length(); i++) { //TODO why '<=' ?
                ref.put(i, Character.toUpperCase(exon.charAt(i - s_start)));
            }

            String chr = region.getChr();
            if (conf.chromosomeNameIsNumber && chr .startsWith("chr")) {
                chr = region.getChr().substring("chr".length());
            }
            try (SamtoolsReader reader = new SamtoolsReader("faidx", "view", bami, chr + ":" + s_start + "-" + s_end)) {
                Map<String, Boolean> dup = new HashMap<>();
                int dupp = -1;
                String line;
                while ((line = reader.read()) != null) {
                    if (conf.isDownsampling() && RND.nextDouble() <= conf.downsampling) {
                        continue;
                    }
                    String[] a = line.split("\t");
                    Flag alignmentFlag = new Flag(Integer.parseInt(a[1]));

                    assert a.length > 4;
                    final int Qmean = Integer.parseInt(a[4]);
                    final String querySEQ = ampliconBasedCalling.length() > 9 ? a[9] : null;

                    if (conf.hasMappingQuality() && Qmean < conf.mappingQuality) { // ignore low mapping quality reads
                        continue;
                    }
                    if (conf.nonPrimaryAlignment && alignmentFlag.isNotPrimaryAlignment()) { // ignore "non-primary alignment"
                        continue;
                    }
                    if (alignmentFlag.isNotPrimaryAlignment() && "*".equals(querySEQ)) {
                        continue;
                    }

                    assert a.length > 6;
                    final String mrnm = a[6];
                    int position = Integer.parseInt(a[3]);
                    if (conf.removeDuplicatedReads) {
                        if (position != dupp) {
                            dup.clear();
                        }
                        if (mrnm.equals("=") && a.length > 7) {
                            if (dup.containsKey(position + "-" + a[7])) {
                                continue;
                            }
                            dup.put(position + "-" + a[7], Boolean.TRUE);
                            dupp = position;
                        }
                    }

                    int nm = 0;
                    int idlen = 0;
                    String cigarStr = a[5];
                    for (String s : globalFind(INDEL, cigarStr)) {
                        idlen += Integer.parseInt(s);
                    }
                    Matcher nmMatcher = NUMBER_MISMATCHES.matcher(line);
                    if (nmMatcher.find()) { // number of mismatches. Don't use NM since it includes gaps, which can be from indels
                        nm = Integer.parseInt(nmMatcher.group(1)) - idlen;
                        if (nm > conf.mismatch) { // edit distance - indels is the # of mismatches
                            continue;
                        }
                    } else {
                        if (conf.y && !cigarStr.equals("*")) {
                            System.err.println("No XM tag for mismatches. " + line);
                            continue;
                        }
                    }
                    int n = 0; // keep track the read position, including softclipped
                    int p = 0; // keep track the position in the alignment, excluding softclipped
                    boolean dir = alignmentFlag.isReverseStrand();
                    List<String> segs = globalFind(MATCH_INSERTION, cigarStr); // Only match and insertion counts toward read length
                    List<String> segs2 = globalFind(SOFT_CLIPPED, cigarStr); // For total length, including soft-clipped bases
                    int rlen = sum(segs); //The read length for matched bases
                    int rlen2 = sum(segs2); //The total length, including soft-clipped bases
                    if ( ampliconBasedCalling != null ) {
                        String[] split = ampliconBasedCalling.split(":");
                        int dis;
                        double ovlp;
                        try {
                            dis = Integer.parseInt(split[0]);
                            ovlp = Double.parseDouble(split.length > 1 ? split[1] : "");
                        } catch(NumberFormatException e) {
                            dis = 10;
                            ovlp = 0.95;
                        }
                        int rlen3 = sum(globalFind(ALIGNED_LENGTH, cigarStr)); // The total aligned length, excluding soft-clipped bases and insertions
                        int segstart = position;
                        int segend = segstart + rlen3 - 1;

                        if (cigarStr.matches("^(\\d+)S.*")) {
                            int ts1 = segstart > s_start ? segstart : s_start;
                            int te1 = segend < s_end ? segend : s_end;
                            if (Math.abs(ts1 - te1) / (double)(segend - segstart) > ovlp == false) {
                                continue;
                            }
                        } else if (cigarStr.matches("^.*(\\d+)S$")) {
                            int ts1 = segstart > s_start ? segstart : s_start;
                            int te1 = segend < s_end ? segend : s_end;
                            if (Math.abs(te1 - ts1) / (double)(segend - segstart) > ovlp == false) {
                                continue;
                            }

                        } else {
                          if (mrnm.equals("=") && a.length > 8) {
                              int isize = Integer.parseInt(a[8]);
                              if (isize > 0) {
                                  segend = segstart + isize -1;
                              } else {
                                  segstart = Integer.parseInt(a[7]);
                                  segend = segstart - isize - 1;
                              }
                          }
                          // No segment overlapping test since samtools should take care of it
                          int ts1 = segstart > s_start ? segstart : s_start;
                          int te1 = segend < s_end ? segend : s_end;
                          if ((((Math.abs(segstart - s_start) <= dis) && (Math.abs(segend - s_end) <= dis))
                                  && Math.abs(ts1 - te1) / (double)(segend - segstart) > ovlp) == false) {
                              continue;
                          }
                        }
                    }
                    if (alignmentFlag.isUnmappedMate()) {
                        // to be implemented
                    } else {
                        if (mrnm.equals("=")) {
                            if (line.matches(".*\\tSA:Z:(\\S+).*")) {
                                if (alignmentFlag.isSupplementaryAlignment()) { // the supplementary alignment
                                    continue; // Ignore the supplmentary for now so that it won't skew the coverage
                                }
                            }
                        }

                    }

                    //Modify the CIGAR for potential mis-alignment for indels at the end of reads to softclipping and let VarDict's algorithm to figure out indels
                    int offset = 0;
                    boolean flag = true;
                    while (flag) {
                        flag = false;
                        Matcher cm = D_S_D_ID.matcher(cigarStr);
                        if (cm.find()) {
                            String tslen = toInt(cm.group(1)) + (cm.group(3).equals("I") ? toInt(cm.group(2)) : 0) + "S";
                            position = cm.group(3).equals("D") ? 2 : 0;
                            cigarStr = cm.replaceFirst(tslen);
                            flag = true;
                        }
                        cm = D_ID_D_S.matcher(cigarStr);
                        if (cm.find()) {
                            String tslen = toInt(cm.group(3)) + (cm.group(2).equals("I") ? toInt(cm.group(1)) : 0) + "S";
                            cigarStr = cm.replaceFirst(tslen);
                            flag = true;
                        }
                        cm = D_S_D_M_ID.matcher(cigarStr);
                        if (cm.find()) {
                            int tmid = toInt(cm.group(2));
                            if (tmid <= 10 ) {
                                String tslen = toInt(cm.group(1)) + tmid + (cm.group(4).equals("I") ? toInt(cm.group(3)) : 0) + "S";
                                position = tmid + (cm.group(4).equals("D") ? toInt(cm.group(3)) : 0);
                                cigarStr = cm.replaceFirst(tslen);
                                flag = true;
                            }
                        }
                        cm = D_ID_D_M_S.matcher(cigarStr);
                        if (cm.find()) {
                            int tmid = toInt(cm.group(3));
                            if (tmid <= 10 ) {
                                String tslen = toInt(cm.group(4)) + tmid + (cm.group(2).equals("I") ? toInt(cm.group(1)) : 0) + "S";
                                cigarStr = cm.replaceFirst(tslen);
                                flag = true;
                            }
                        }
                        cm = D_M_D_ID.matcher(cigarStr);
                        if (cm.find()) {
                            int tmid = toInt(cm.group(1));
                            if (tmid <= 8 ) {
                                String tslen = tmid + (cm.group(3).equals("I") ? toInt(cm.group(2)) : 0) + "S";
                                position = tmid + (cm.group(3).equals("D") ? toInt(cm.group(2)) : 0);
                                cigarStr = cm.replaceFirst(tslen);
                                flag = true;
                            }
                        }
                        cm = D_ID_D_M.matcher(cigarStr);
                        if (cm.find()) {
                            int tmid = toInt(cm.group(3));
                            if (tmid <= 8 ) {
                                String tslen = tmid + (cm.group(2).equals("I") ? toInt(cm.group(1)) : 0) + "S";
                                cigarStr = cm.replaceFirst(tslen);
                                flag = true;
                            }
                        }

                        //Combine two deletions and insertion into one complex if they are close
                        cm = D_M_D_DD_M_D_I_D_M_D_DD.matcher(cigarStr);
                        if (cm.find()) {
                            int mid = toInt(cm.group(4)) + toInt(cm.group(6));
                            if (mid <= 10) {
                                int tslen = mid + toInt(cm.group(5));
                                int dlen = toInt(cm.group(3)) + mid + toInt(cm.group(7));
                                String ov5 = cm.group(1);
                                int rdoff = toInt(cm.group(2));
                                int refoff = position + rdoff;
                                int RDOFF = rdoff;
                                if (!ov5.isEmpty()) {
                                    Matcher matcher = D_MIS.matcher(ov5);
                                    while (matcher.find()) {
                                        rdoff += toInt(matcher.group(1));
                                    }
                                    matcher = D_MD.matcher(ov5);
                                    while (matcher.find()) {
                                        refoff += toInt(matcher.group(1));
                                    }
                                }
                                int rn = 0;
                                while (rdoff + rn < querySEQ.length()
                                        && Character.valueOf(querySEQ.charAt(rdoff + rn)).equals(ref.get(refoff + rn))) {
                                    rn++;
                                }
                                RDOFF += rn;
                                dlen -= rn;
                                tslen -= rn;
                                cigarStr = D_M_D_DD_M_D_I_D_M_D_DD_prim.matcher(cigarStr).replaceFirst(RDOFF + "M" + dlen + "D" + tslen + "I");
                                flag = true;
                            }
                        }
                        cm = D_DD_D_M_D_DD_DI.matcher(cigarStr);
                        if (cm.find()) {
                            int ilen = toInt(cm.group(2));
                            int dlen = ilen + toInt(cm.group(1)) + toInt(cm.group(3));
                            String istr = cm.group(4);
                            if (istr != null) {
                                ilen += toInt(istr.substring(0, istr.length() - 1));
                            }
                            cigarStr = cm.replaceFirst(dlen + "D" + ilen + "I");
                        }
                    }

                    List<String> cigar = new ArrayList<>();
                    Matcher cigarM = CIGAR_PAIR.matcher(cigarStr);
                    while (cigarM.find()) {
                        cigar.add(cigarM.group(1));
                        cigar.add(cigarM.group(2));
                    }

                    int start = position;
                    assert a.length > 10;
                    final String quality = a[10];

                    for(int ci = 0; ci < cigar.size(); ci += 2) {
                        int m = Integer.parseInt(cigar.get(ci));
                        String operation = cigar.get(ci + 1);
                        // Treat insertions at the edge as soft-clipping
                        if ( (ci == 0 || ci == cigar.size() - 2) && operation.equals("I")) {
                            operation = "S";
                        }

                        switch (operation) {
                            case "N":
                                String key = (start - 1) + "-" + (start + m - 1);
                                Integer splice = SPLICE.get(key);
                                if (splice == null) {
                                    SPLICE.put(key, 1);
                                } else {
                                    SPLICE.put(key, splice + 1);
                                }

                                start += m;
                                offset = 0;
                                continue;

                            case "S":
                                if (ci == 0) { // 5' soft clipped
                                    // align softclipped but matched sequences due to mis-softclipping
                                    while (m - 1 >= 0 && start - 1 > 0 && start - 1 <= chrs.get(chr)
                                            && ref.containsKey(start - 1)
                                            && ref.get(start - 1).equals(querySEQ.charAt(m - 1))
                                            && quality.charAt(m - 1) - 33 > 10) {
                                        Variation variation = getVariation(hash, start - 1, ref.get(start - 1).toString());
                                        if (variation.cnt != 0) {
                                            variation.cnt = 0;
                                        }
                                        addCnt(variation, dir, m, quality.charAt(m - 1) - 33, Qmean, nm, conf.goodq);
                                        incCnt(cov, start - 1, 1);
                                        start--;
                                        m--;
                                    }
                                    if (m > 0) {
                                        int q = 0;
                                        int qn = 0;
                                        int lowqcnt = 0;
                                        for (int si = m - 1; si >= 0; si--) {
                                            if (querySEQ.charAt(si) == 'N') {
                                                break;
                                            }
                                            int tq = quality.charAt(si) - 33;
                                            if (tq < 7)
                                                lowqcnt++;
                                            if (lowqcnt > 1)
                                                break;

                                            q += tq;
                                            qn++;
                                        }
                                        if (qn >= 1 && qn > lowqcnt && start >= s_start - conf.buffer && start <= s_end + conf.buffer) {
                                            Sclip sclip = sclip5.get(start);
                                            if (sclip == null) {
                                                sclip = new Sclip();
                                                sclip5.put(start, sclip);
                                            }
                                            for (int si = m - 1; m - si <= qn; si--) {
                                                Character ch = querySEQ.charAt(si);
                                                int idx = m - 1 - si;
                                                Map<Character, Integer> cnts = sclip.nt.get(idx);
                                                if (cnts == null) {
                                                    cnts = new HashMap<>();
                                                    sclip.nt.put(idx, cnts);
                                                }
                                                incCnt(cnts, ch, 1);
                                                Variation variation = getVariation(sclip, idx, ch);
                                                if (variation.cnt != 0) {
                                                    variation.cnt = 0;
                                                }
                                                addCnt(variation, dir, si - (m - qn), quality.charAt(si) - 33, Qmean, nm, conf.goodq);
                                            }
                                            if (sclip.variation.cnt != 0)
                                                sclip.variation.cnt = 0;
                                            addCnt(sclip.variation, dir, m, q / qn, Qmean, nm, conf.goodq);
                                        }

                                    }
                                    m = Integer.parseInt(cigar.get(ci));
                                } else if (ci == cigar.size() - 2) { // 3' soft clipped
                                    int qmean = quality.charAt(n) - 33;
                                    while (n < querySEQ.length()
                                            && ref.containsKey(start)
                                            && ref.get(start).equals(querySEQ.charAt(n))
                                            && qmean > 10) {

                                        Variation variation = getVariation(hash, start, ref.get(start).toString());
                                        if (variation.cnt != 0) {
                                            variation.cnt = 0;
                                        }
                                        addCnt(variation, dir, rlen2 - p, qmean, Qmean, nm, conf.goodq);
                                        incCnt(cov, start, 1);

                                        n++;
                                        start++;
                                        m--;
                                        p++;
                                    }
                                    if (querySEQ.length() - n > 0) {
                                        int q = 0;
                                        int qn = 0;
                                        int lowqcnt = 0;
                                        for (int si = 0; si < m; si++) {
                                            if ( querySEQ.charAt(n+si) == 'N' ) {
                                                break;
                                            }
                                            int tq = quality.charAt(n + si) - 33;
                                            if (tq < 7) {
                                                lowqcnt++;
                                            }
                                            if ( lowqcnt > 1 ) {
                                                break;
                                            }
                                            q += tq;
                                            qn++;
                                        }
                                        if (qn >= 1 && qn > lowqcnt && start >= s_start - conf.buffer && start <= s_end + conf.buffer) {
                                            Sclip sclip = sclip3.get(start);
                                            if (sclip == null) {
                                                sclip = new Sclip();
                                                sclip3.put(start, sclip);
                                            }
                                            for (int si = 0; si < qn; si++) {
                                                Character ch = querySEQ.charAt(n + si);
                                                int idx = si;
                                                Map<Character, Integer> cnts = sclip.nt.get(idx);
                                                if (cnts == null) {
                                                    cnts = new HashMap<>();
                                                    sclip.nt.put(idx, cnts);
                                                }
                                                incCnt(cnts, ch, 1);
                                                Variation variation = getVariation(sclip, idx, ch);
                                                if (variation.cnt != 0) {
                                                    variation.cnt = 0;
                                                }
                                                addCnt(variation, dir, qn -  si, quality.charAt(n + si) - 33, Qmean, nm, conf.goodq);
                                            }
                                            if (sclip.variation.cnt != 0)
                                                sclip.variation.cnt = 0;
                                            addCnt(sclip.variation, dir, m, q / qn, Qmean, nm, conf.goodq);
                                        }

                                    }

                                }
                                n += m;
                                offset = 0;
                                start = Integer.parseInt(a[3]);  // had to reset the start due to softclipping adjustment
                                continue;
                            case "H":
                                continue;
                            case "I": {
                                StringBuilder s = new StringBuilder(substr(querySEQ, n, m));
                                StringBuilder q = new StringBuilder(substr(quality, n, m));
                                StringBuilder ss = new StringBuilder();

                                // For multiple indels within 10bp
                                int multoffs = 0;
                                int multoffp = 0;

                                if (cigar.size() > ci + 5
                                        && Integer.parseInt(cigar.get(ci + 2)) <= conf.vext
                                        && cigar.get(ci + 3).contains("M")
                                        && (cigar.get(ci + 5).contains("I") || cigar.get(ci + 5).contains("D"))) {

                                    int ci2 = Integer.parseInt(cigar.get(ci + 2));
                                    int ci4 = Integer.parseInt(cigar.get(ci + 4));
                                    s.append("#").append(substr(querySEQ, n + m, ci2));
                                    q.append(substr(quality, n + m, ci2));
                                    s.append('^').append(cigar.get(ci + 5).equals("I") ? substr(querySEQ, n + m + ci2, ci4) : ci4);
                                    q.append(cigar.get(ci + 5).equals("I") ? substr(quality, n + m + ci2, ci4) : quality.charAt(n + m + ci2));
                                    multoffs += ci2 + (cigar.get(ci + 5).equals("D") ? ci4 : 0);
                                    multoffp += ci2 + (cigar.get(ci + 5).equals("I") ? ci4 : 0);
                                    ci += 4;
                                } else {
                                    if (cigar.size() > ci + 3 && cigar.get(ci + 3).contains("M")) {
                                        int ci2 = Integer.parseInt(cigar.get(ci + 2));
                                        for (int vi = 0; vi < conf.vext && vi < ci2; vi++) {
                                            if (querySEQ.charAt(n + m + vi) == 'N') {
                                                break;
                                            }
                                            if (quality.charAt(n + m + vi) - 33 < conf.goodq) {
                                                break;
                                            }
                                            if (ref.containsKey(start + vi) && String.valueOf(querySEQ.charAt(n + m + vi)).equals(ref.get(start + vi))) {
                                                offset = vi + 1;
                                            }
                                        }
                                        if (offset != 0) {
                                            ss.append(substr(querySEQ, n+m, offset));
                                            q.append(substr(quality, n+m, offset));
                                            for(int osi = 0; osi < offset; osi++ ) {
                                                incCnt(cov, start + osi, 1);
                                            }
                                        }
                                    }
                                }
                                if ( offset > 0) {
                                    s.append("&").append(ss);
                                }

                                if (start - 1 >= s_start && start - 1 <= s_end && !s.toString().contains("N")) {
                                    incCnt(getOrElse(inc, start - 1, new HashMap<String, Integer>()), "+" + s, 1);
                                    Variation hv = getVariation(iHash, start - 1, "+" + s);
                                    hv.incDir(dir);
                                    hv.cnt++;
                                    int tp = p < rlen - p ? p + 1 : rlen - p;
                                    int tmpq = 0;
                                    for (int i = 0; i < q.length(); i++) {
                                        tmpq += q.charAt(i) - 33;
                                    }
                                    tmpq /= q.length();
                                    if (hv.pstd == false && hv.pp != 0 && tp != hv.pp) {
                                        hv.pstd = true;
                                    }
                                    if (hv.qstd == false && hv.pq != 0 && tmpq != hv.pq) {
                                        hv.qstd = true;
                                    }
                                    hv.pmean += tp;
                                    hv.qmean += tmpq;
                                    hv.Qmean += Qmean;
                                    hv.pp = tp;
                                    hv.pq = tmpq;
                                    if (tmpq >= conf.goodq) {
                                        hv.hicnt++;
                                    } else {
                                        hv.locnt++;
                                    }
                                    hv.nm += nm;

                                    // Adjust the reference count for insertion reads
                                    if ( ref.containsKey(start - 1)
                                            && hash.containsKey(start - 1)
                                            && hash.get(start - 1).containsKey(ref.get(start - 1))
                                            && String.valueOf(querySEQ.charAt(n-1)).equals(ref.get(start - 1))) {

//                                        subCnt(getVariation(hash, start - 1, ref.get(start - 1 ).toString()), dir, tp, tmpq, Qmean, nm, conf);
                                        subCnt(getVariation(hash, start - 1, String.valueOf(querySEQ.charAt(n - 1))), dir, tp, quality.charAt(n - 1) - 33, Qmean, nm, conf);
                                    }
                                    // Adjust count if the insertion is at the edge so that the AF won't > 1
                                    if (ci == 2 && (cigar.get(1).contains("S") || cigar.get(1).contains("H"))) {
                                        Variation ttref = getVariation(hash, start - 1, ref.get(start - 1).toString());
                                        ttref.incDir(dir);
                                        ttref.cnt++;
                                        ttref.pstd = hv.pstd;
                                        ttref.qstd = hv.qstd;
                                        ttref.pmean += tp;
                                        ttref.qmean += tmpq;
                                        ttref.Qmean += Qmean;
                                        ttref.pp = tp;
                                        ttref.pq = tmpq;
                                        ttref.nm += nm;
                                        incCnt(cov, start - 1, 1);
                                    }
                                }
                                n += m + offset + multoffp;
                                p += m + offset + multoffp;
                                start += offset + multoffs;
                                }
                                continue;
                            case "D":
                                {
                                    StringBuilder s = new StringBuilder("-").append(m);
                                    StringBuilder ss = new StringBuilder();
                                    char q1 = quality.charAt(n - 1);
                                    StringBuilder q = new StringBuilder();

                                    // For multiple indels within $VEXT bp
                                    int multoffs = 0;
                                    int multoffp = 0;
                                    if (cigar.size() > ci + 5
                                            && Integer.parseInt(cigar.get(ci + 2)) <= conf.vext
                                            && cigar.get(ci + 3).contains("M")
                                            && (cigar.get(ci + 5).contains("I") || cigar.get(ci + 5).contains("D"))) {

                                        int ci2 = Integer.parseInt(cigar.get(ci + 2));
                                        int ci4 = Integer.parseInt(cigar.get(ci + 4));

                                        s.append("#").append(substr(querySEQ, n, ci2));
                                        q.append(substr(quality, n, ci2));
                                        s.append('^').append(cigar.get(ci + 5).equals("I") ? substr(querySEQ, n + ci2, ci4) : ci4);
                                        q.append(cigar.get(ci + 5).equals("I") ? substr(quality, n + ci2, ci4) : "");
                                        multoffs += ci2 + (cigar.get(ci + 5).equals("D") ? ci4 : 0);
                                        multoffp += ci2 + (cigar.get(ci + 5).equals("I") ? ci4 : 0);
                                        ci += 4;
                                    } else if (cigar.size() > ci + 3 && cigar.get(ci + 3).equals("I")) {
                                       int ci2 = Integer.parseInt(cigar.get(ci + 2));
                                       s.append("^").append(substr(querySEQ, n, ci2));
                                       q.append(substr(quality, n, ci2));
                                       multoffp += ci2;
                                       ci += 2;
                                    } else {
                                        if (cigar.size() > ci + 3 && cigar.get(ci + 3).contains("M")) {
                                            int ci2 = Integer.parseInt(cigar.get(ci + 2));
                                            for (int vi = 0; vi < conf.vext && vi < ci2; vi++) {
                                                if (querySEQ.charAt(n + vi) == 'N') {
                                                    break;
                                                }
                                                if (quality.charAt(n + vi) - 33 < conf.goodq) {
                                                    break;
                                                }
                                                if (ref.containsKey(start + m + vi) && String.valueOf(querySEQ.charAt(n + vi)).equals(ref.get(start + m + vi))) {
                                                    offset = vi + 1;
                                                }
                                            }
                                            if (offset != 0) {
                                                ss.append(substr(querySEQ, n + m, offset));
                                                q.append(substr(quality, n + m, offset));
                                                for (int osi = 0; osi < offset; osi++) {
                                                    incCnt(cov, start + osi, 1);
                                                }
                                            }
                                        }
                                    }
                                    if ( offset > 0 ) {
                                        s.append("&").append(ss);
                                    }
                                    char q2 = quality.charAt(n + offset);
                                    q.append(q1 > q2 ? q1 : q2);
                                    if ( start >= region.start && start <= region.end ) {
                                        Variation hv = getVariation(hash, start, s.toString());
                                        hv.incDir(dir);
                                        hv.cnt++;

                                        int tp = p < rlen - p ? p + 1 : rlen - p;
                                        int tmpq = 0;

                                        for (int i = 0; i < q.length(); i++) {
                                            tmpq += q.charAt(i) - 33;
                                        }

                                        tmpq /= q.length();
                                        if (hv.pstd == false && hv.pp != 0  && tp != hv.pp) {
                                            hv.pstd = true;
                                        }

                                        if (hv.qstd == false && hv.pq != 0  && tmpq != hv.pq) {
                                            hv.qstd = true;
                                        }
                                        hv.pmean += tp;
                                        hv.qmean += tmpq;
                                        hv.Qmean += Qmean;
                                        hv.pp = tp;
                                        hv.pq = tmpq;
                                        hv.nm += nm;
                                        if (tmpq >= conf.goodq) {
                                            hv.hicnt++;
                                        } else {
                                            hv.locnt++;
                                        }
                                        for (int i = 0; i < m; i++) {
                                            incCnt(cov, start + i, 1);
                                        }
                                    }
                                    start += m + offset + multoffs;
                                    n +=  offset + multoffp;
                                    p +=  offset + multoffp;
                                    continue;
                                }
                        }
                        for(int i = offset; i < m; i++) {
                            boolean trim = false;
                            if ( conf.trimBasesAfter > 0) {
                                if (dir == false) {
                                    if (n > conf.trimBasesAfter) {
                                        trim = true;
                                    }
                                } else {
                                    if (rlen2 - n > conf.trimBasesAfter) {
                                        trim = true;
                                    }
                                }
                            }
                            String s = String.valueOf(querySEQ.charAt(n));
                            if (s.equals("N")) {
                                start++;
                                n++;
                                p++;
                                continue;
                            }
                            int q = quality.charAt(n) - 33;
                            int qbases = 1;
                            // for more than one nucleotide mismatch
                            StringBuilder ss = new StringBuilder();
                            // More than one mismatches will only perform when all nucleotides have quality > $GOODQ
                            // Update: Forgo the quality check.  Will recover later
                            while((start + 1) >= region.start
                                    && (start + 1) <= region.end && (i + 1) < m
                                    && ref.containsKey(start) && !ref.get(start).equals(querySEQ.charAt(n))) {

                                if (querySEQ.charAt(n + 1) == 'N' ) {
                                    break;
                                }
                                if ( ref.containsKey(start + 1) && !ref.get(start + 1).equals(querySEQ.charAt(n +  1))) {
                                    ss.append(querySEQ.charAt(n +  1));
                                    q += quality.charAt(n + 1) - 33;
                                    qbases++;
                                    n++;
                                    p++;
                                    i++;
                                    start++;
                                } else {
                                    break;
                                }
                            }
                            if (ss.length() > 0) {
                                s += "&" + ss;
                            }
                            if (m - i <= conf.vext
                                    && cigar.size() > ci + 3 && "D".equals(cigar.get(ci + 3))
                                    && (ss.length() > 0 || Character.valueOf(querySEQ.charAt(n)).equals(ref.get(start)))
                                    && quality.charAt(n) - 33 > conf.goodq) {
                                while (i + 1 < m) {
                                    s += querySEQ.charAt(n + 1);
                                    q += quality.charAt(n + 1) - 33;
                                    qbases++;
                                    i++;
                                    n++;
                                    p++;
                                    start++;
                                }
                                s = s.replace("&", "");
                                s = "-" + cigar.get(ci + 2) + "&" + s;

                            }
                            if(trim == false) {
                                if (start - qbases + 1 >=region.start && start - qbases + 1 <= region.end) {
                                    Variation hv = getVariation(hash, start - qbases + 1, s);
                                    hv.incDir(dir);
                                    if (s.matches("^[ATGC]&[ATGC]+$")) {
                                        increment(mnp, start - qbases + 1, s);
                                    }
                                    hv.cnt++;
                                    int tp = p < rlen - p ? p + 1 : rlen - p;
                                    q = q / qbases;
                                    if(hv.pstd == false && hv.pp != 0 && tp != hv.pp) {
                                        hv.pstd = true;
                                    }
                                    if(hv.qstd == false && hv.pq != 0 && tp != hv.pq) {
                                        hv.qstd = true;
                                    }
                                    hv.pmean += tp;
                                    hv.qmean += q;
                                    hv.Qmean += Qmean;
                                    hv.pp = tp;
                                    hv.pq = q;
                                    hv.nm = nm;
                                    if (q >= conf.goodq) {
                                        hv.hicnt++;
                                    } else {
                                        hv.locnt++;
                                    }
                                    for (int qi = 0; qi < qbases; qi++) {
                                        incCnt(cov, start - qi + 1, 1);
                                    }
                                    if (s.contains("-")) {
                                        increment(dels5, start - qbases + 1, s);
                                        for (int qi = 0; qi < cigar.get(ci + 2).length(); qi++) {
                                            incCnt(cov, start + qi, 1);
                                        }
                                        start += cigar.get(ci + 2).length();
                                        ci += 2;
                                    }
                                }
                            }
                            if (operation.equals("I")) {
                                start++;
                            } else if (operation.equals("D")) {
                                n++;
                                p++;
                            }
                        }
                        offset = 0;

                    }

                }
            } //TODO abcall

        }
        adjMNP(hash, mnp, cov);

        if (conf.performLocalRealignment) {

        }
        return null;
    }


    private static void realigndel(Map<Integer, Map<String, Variation>> hash,
            Map<Integer, Map<String, Integer>> dels5) {

        int longmm = 3; //Longest continued mismatches typical aligned at the end
        List<Object[]> tmp = new ArrayList<>();
        for (Entry<Integer, Map<String, Integer>> entDel : dels5.entrySet()) {
            Integer p = entDel.getKey();
            Map<String, Integer> dv = entDel.getValue();
            for (Entry<String, Integer> entDv : dv.entrySet()) {
                String vn = entDv.getKey();
                Integer dcnt = entDv.getValue();
                int ecnt = 0;


            }


        }

    }

    private static void adjMNP(Map<Integer, Map<String, Variation>> hash,
            Map<Integer, Map<String, Integer>> mnp,
            Map<Integer, Integer> cov) {

        for (Map.Entry<Integer, Map<String, Integer>> entry: mnp.entrySet()) {
            Integer p = entry.getKey();
            Map<String, Integer> v = entry.getValue();

            for (Map.Entry<String, Integer> en: v.entrySet()) {
                String vn = en.getKey();
                String mnt = vn.replace("&", "");
                for (int i = 0; i < mnt.length() - 1; i++) {
                    String left = substr(mnt, 0, i + 1);
                    String right = substr(mnt, -(mnt.length() - i - 1));
                    Variation vref = getVariation(hash, p, vn);
                    if (hash.containsKey(p) && hash.get(p).containsKey(left)) {
                        Variation tref = getVariation(hash, p, left);
                        if (tref.cnt < vref.cnt && tref.pmean / tref.cnt <= i + 1) {
                            adjCnt(vref, tref);
                            hash.get(p).remove(left);
                        }
                    }
                    if (hash.containsKey(p + i + 1) && hash.get(p + i + 1).containsKey(right)) {
                        Variation tref = getVariation(hash, p + i + 1, right);
                        if (tref.cnt < vref.cnt && tref.pmean / tref.cnt <= mnt.length() - i - 1) {
                            adjCnt(vref, tref);
                            incCnt(cov, p, tref.cnt);
                            hash.get(p + i + 1).remove(right);
                        }
                    }

                }

            }
        }

    }

    private static void adjCnt(Variation vref, Variation tv) {
        adjCnt(vref, tv, null);
    }

    private static void adjCnt(Variation vref, Variation tv, Variation ref) {
        vref.cnt += tv.cnt;
        vref.extracnt += tv.cnt;
        vref.hicnt += tv.hicnt;
        vref.locnt += tv.locnt;
        vref.pmean += tv.pmean;
        vref.qmean += tv.qmean;
        vref.Qmean += tv.Qmean;
        vref.nm += tv.nm;
        vref.pstd = true;
        vref.qstd = true;
        vref.addDir(true, tv.getDir(true));
        vref.addDir(false, tv.getDir(false));

        if (ref == null)
            return;

        ref.cnt -= tv.cnt;
        ref.hicnt -= tv.hicnt;
        ref.locnt -= tv.locnt;
        ref.pmean -= tv.pmean;
        ref.qmean -= tv.qmean;
        ref.Qmean -= tv.Qmean;
        ref.nm -= tv.nm;
        ref.subDir(true, tv.getDir(true));
        ref.subDir(false, tv.getDir(false));
        if (ref.cnt < 0)
            ref.cnt = 0;
        if (ref.hicnt < 0)
            ref.hicnt = 0;
        if (ref.locnt < 0)
            ref.locnt = 0;
        if (ref.pmean < 0)
            ref.pmean = 0;
        if (ref.qmean < 0)
            ref.qmean = 0;
        if (ref.getDir(true) < 0)
            ref.addDir(true, -ref.getDir(true));
        if (ref.getDir(false) < 0)
            ref.addDir(false, -ref.getDir(false));
    }

    private void increment(Map<Integer, Map<String, Integer>> counters, int idx, String s) {
        Map<String, Integer> map = counters.get(idx);
        if (map == null) {
            map = new HashMap<>();
            counters.put(idx, map);
        }
        incCnt(map, s, 1);
    }

    private static final BedRowFormat DEFAULT_BED_ROW_FORMAT = new BedRowFormat(2, 6, 7, 9, 10, 12);
    private static final BedRowFormat CUSTOM_BED_ROW_FORMAT = new BedRowFormat(0, 1, 2, 3, 1, 2);

    public static class BedRowFormat {
        public final int chrColumn;
        public final int startColumn;
        public final int endColumn;
        public final int geneColumn;
        public final int thickStartColumn;
        public final int thickEndColumn;

        public BedRowFormat(int chrColumn, int startColumn, int endColumn, int geneColumn, int thickStartColumn, int thickEndColumn) {
            this.chrColumn = chrColumn;
            this.startColumn = startColumn;
            this.endColumn = endColumn;
            this.geneColumn = geneColumn;
            this.thickStartColumn = thickStartColumn;
            this.thickEndColumn = thickEndColumn;
        }

    }

    private static void ampVardict(List<List<Region>> segs) {
        // TODO Auto-generated method stub

    }

    public static List<String> globalFind(Pattern pattern, String string) {
        List<String> result = new LinkedList<>();
        Matcher matcher = pattern.matcher(string);
        while(matcher.find()) {
            result.add(matcher.group(1));
        }
        return result;

    }

    public static class ToVarsContext {
        Map<Integer, Character> ref = new HashMap<>();
        Map<Integer, Map<String, Variation>> hash = new HashMap<>();
        Map<Integer, Map<String, Variation>> iHash = new HashMap<>();
        Map<Integer, Integer> cov = new HashMap<>();
        Map<Integer, Sclip> sclip3 = new HashMap<>();
        Map<Integer, Sclip> sclip5 = new HashMap<>();
        Map<Integer, Map<String, Integer>> inc = new HashMap<>();
    }

    public static int sum(List<?> list) {
        int result = 0;
        for (Object object : list) {
            result += Integer.parseInt(String.valueOf(object));
        }
        return result;
    }

    public static int toInt(String intStr) {
        return Integer.parseInt(intStr);
    }

    public static String substr(String string, int idx) {
        if(idx >= 0) {
            return string.substring(idx);
        } else {
            return string.substring(Math.max(0, string.length() + idx));
        }
    }

    public static String substr(String string, int begin, int len) {
        if (len > 0) {
            return string.substring(begin, Math.min(begin + len, string.length()));
        } else if (len == 0) {
            return "";
        } else {
            int end = string.length() + len;
            if (end < begin) {
                return "";
            }
            return string.substring(begin, end);
        }
    }

    // TODO validate region format chr:start[-end][:gene]
    public static List<Region> buildRegions(String region, final int numberNucleotideToExtend, final boolean zeroBased) {
        List<Region> segs = new ArrayList<>();
        String[] split = region.split(":");
        String chr = split[0];
        String gene = split.length < 3 ? chr : split[2];
        String[] range = split[1].split("-");
        int start = Integer.parseInt(range[0].replaceAll(",", ""));
        int end = range.length < 2 ? start : Integer.parseInt(range[1].replaceAll(",", ""));
        start -= numberNucleotideToExtend;
        end += numberNucleotideToExtend;
        if (zeroBased)
            start++;
        if (start > end)
            start = end;
        segs.add(new Region(chr, start, end, gene));

        return segs;
    }
}
