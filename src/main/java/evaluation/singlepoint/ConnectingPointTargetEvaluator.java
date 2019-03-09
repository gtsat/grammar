package evaluation.singlepoint;

import oscp.Group;
import topk.ConnectingLocationsIterable;
import oscp.LocationComparatorMinSum;
import oscp.LocationComparatorMinMax;
import shortestpath.ScatterMap;
import grammar.UndirectedGraph;
import java.text.NumberFormat;
import java.util.Comparator;
import java.util.ArrayDeque;
import java.util.LinkedList;
import java.io.IOException;
import java.util.Scanner;
import java.io.File;

final public class ConnectingPointTargetEvaluator {
    final static private int[] travelers = {3,5,7,9};
    final static private float[] coverages = {.3f,.5f,.7f,.9f};
    final static private float[] distances = {.3f,.5f,.7f,.9f};
    final static private float[] selflambdas = {.3f,.5f,.7f,.9f};
    final static private int[] kappas = {3,5,7,9};

    final static private String[] networks = {"OL","CA","TG","NA","SF"};

    static private boolean useMinSumRanking = false;

    public static void main (String[] args) throws IOException {
        int maxQueries = args.length>0?Integer.parseInt(args[0]):Integer.MAX_VALUE;
        if (maxQueries<0) {maxQueries=-maxQueries; useMinSumRanking=true;}
        System.out.println(" ---------------------------------------");
        System.out.println("   Optimal Connecting Point Evaluation  ");
        System.out.println(" ---------------------------------------");

        for (String network : networks) {
            System.out.println ("** Evaluation of target distance scalability using the "+network
                               +" road network and "+(useMinSumRanking?new String("MinSum"):new String("MinMax"))+" ranking.");

            UndirectedGraph<Long> graph = new UndirectedGraph<Long>("../data/lifeifei/"+network+".graph",NumberFormat.getNumberInstance(),true).kruskal();
            UndirectedGraph<Long> apsp = graph; //new UndirectedGraph<>("../data/lifeifei/"+network+".mst.apsp.graph",NumberFormat.getNumberInstance(),true);
            ScatterMap<Long> map = new ScatterMap<>("../data/lifeifei/"+network+".cnode",NumberFormat.getNumberInstance());
            //ShortestPath<Long> sp = new ShortestPath<>(graph,map);

            long startTime;

            int k = kappas [0];
            int n = travelers [0];
            float area = coverages [0];
            float selflambda = selflambdas [0];

            float lambdas[][] = new float[n][n];
            for (int i=0; i<n; ++i)
                for (int j=0; j<n; ++j)
                    if (i==j) lambdas[i][j] = selflambda;
                    else lambdas[i][j] = (1-selflambda)/(n-1);


            LinkedList<Long> result = new LinkedList<>();
            for (float target : distances) {
                ArrayDeque<Long[]> queries = loadQueries("../queries/demo/"+network+"/QueryN"+n+"A"+(int)(area*100)+"T"+(int)(target*100)+".txt");

                int Qid = 0;

                long ceMem = 0;
                long bnbMem = 0;
                long cliMem = 0;
                long relMem = 0;
                long setMem = 0;
                long postMem = 0;
                long setrecMem = 0;
                long setdijMem = 0;

                long ceTime = 0;
                long bnbTime = 0;
                long cliTime = 0;
                long relTime = 0;
                long setTime = 0;
                long postTime = 0;
                long setrecTime = 0;
                long setdijTime = 0;

                float ceBestScore = .0f;
                float bnbBestScore = .0f;
                float cliBestScore = .0f;
                float relBestScore = .0f;
                float setBestScore = .0f;
                float postBestScore = .0f;
                float setrecBestScore = .0f;
                float setdijBestScore = .0f;

                float ceWorstScore = .0f;
                float bnbWorstScore = .0f;
                float cliWorstScore = .0f;
                float relWorstScore = .0f;
                float setWorstScore = .0f;
                float postWorstScore = .0f;
                float setrecWorstScore = .0f;
                float setdijWorstScore = .0f;

                for (Long[] query : queries) {
                    if (Qid>=maxQueries) break;

                    int i=0;
                    Long[] travelers = new Long [n];
                    for (Long q : query) if (i<n) travelers[i++] = q;
                    Group<Long> grouping = new Group<>(query[n],travelers,lambdas,graph,useMinSumRanking);
                    Comparator<Long> cmp = useMinSumRanking?new LocationComparatorMinSum<>(grouping,apsp,null,false):new LocationComparatorMinMax<>(grouping,apsp,null,false);

                    startTime = System.nanoTime();
                    result = setbased.ComputeMeetingLocation.computeOMPfiltered (grouping,graph,useMinSumRanking,k);
                    setTime += System.nanoTime() - startTime;
                    setBestScore += useMinSumRanking?((LocationComparatorMinSum<Long>)cmp).computeScore(result.getFirst()):((LocationComparatorMinMax<Long>)cmp).computeScore(result.getFirst());
                    setWorstScore += useMinSumRanking?((LocationComparatorMinSum<Long>)cmp).computeScore(result.getLast()):((LocationComparatorMinMax<Long>)cmp).computeScore(result.getLast());
                    setMem += setbased.ComputeMeetingLocation.getMaxMemRequirements();
                    setbased.ComputeMeetingLocation.resetStats();
                    result.clear();

                    startTime = System.nanoTime();
                    result = setbased.ComputeMeetingLocation.computeOMP_Dijkstra (grouping,graph,useMinSumRanking,k);
                    setdijTime += System.nanoTime() - startTime;
                    setdijBestScore += useMinSumRanking?((LocationComparatorMinSum<Long>)cmp).computeScore(result.getFirst()):((LocationComparatorMinMax<Long>)cmp).computeScore(result.getFirst());
                    setdijWorstScore += useMinSumRanking?((LocationComparatorMinSum<Long>)cmp).computeScore(result.getLast()):((LocationComparatorMinMax<Long>)cmp).computeScore(result.getLast());
                    setdijMem += setbased.ComputeMeetingLocation.getMaxMemRequirements();
                    setbased.ComputeMeetingLocation.resetStats();
                    result.clear();
/*
                    i=0;
                    startTime = System.nanoTime();
                    ConnectingLocationsIterable<Long> cli = new ConnectingLocationsIterable<>(grouping,graph,useMinSumRanking);
                    for (long mu : cli) {
                        if (++i>k) break;
                        result.addLast(mu);
                    }
                    cliTime += System.nanoTime() - startTime;
                    cliBestScore += useMinSumRanking?((LocationComparatorMinSum<Long>)cmp).computeScore(result.getFirst()):((LocationComparatorMinMax<Long>)cmp).computeScore(result.getFirst());
                    cliWorstScore += useMinSumRanking?((LocationComparatorMinSum<Long>)cmp).computeScore(result.getLast()):((LocationComparatorMinMax<Long>)cmp).computeScore(result.getLast());
                    cliMem += cli.getMaxMemRequirements();
                    result.clear();
*/
                    ++Qid;
                }
                System.out.print ("["+target+"\t"+bnbTime/Math.pow(10,9)/Qid+"\t"+ceTime/Math.pow(10,9)/Qid+"\t"+relTime/Math.pow(10,9)/Qid+"\t"+postTime/Math.pow(10,9)/Qid+"\t"
                    +cliTime/Math.pow(10,9)/Qid+"\t"+setTime/Math.pow(10,9)/Qid+"\t"+setrecTime/Math.pow(10,9)/Qid+"\t"+setdijTime/Math.pow(10,9)/Qid+"\t"
                    +bnbBestScore/Qid+"\t"+ceBestScore/Qid+"\t"+relBestScore/Qid+"\t"+postBestScore/Qid+"\t"+cliBestScore/Qid+"\t"+
                    +setBestScore/Qid+"\t"+setrecBestScore/Qid+"\t"+setdijBestScore/Qid+"\t"
                    +bnbWorstScore/Qid+"\t"+ceWorstScore/Qid+"\t"+relWorstScore/Qid+"\t"+postWorstScore/Qid+"\t"+cliWorstScore/Qid+"\t"
                    +setWorstScore/Qid+"\t"+setrecWorstScore/Qid+"\t"+setdijWorstScore/Qid+"\t"
                    +bnbMem/(float)Qid+"\t"+ceMem/(float)Qid+"\t"+relMem/(float)Qid+"\t"+postMem/(float)Qid+"\t"+cliMem/(float)Qid+"\t"
                    +setMem/(float)Qid+"\t"+setrecMem/(float)Qid+"\t"+setdijMem/(float)Qid+"]\n");
            }
        }
    }

    private static ArrayDeque<Long[]> loadQueries (String queryfile) throws IOException {
        ArrayDeque<Long[]> queries = new ArrayDeque<>();
        Scanner in = new Scanner (new File (queryfile)) ;
        while (in.hasNextLine()) {
            String[] line = in.nextLine().split("\\s+");
            Long[] query = new Long [line.length];
            int i=0;
            for (String element : line)
                query[i++] = Long.parseLong(element);
            queries.add(query);
        }
        System.out.println ("!! Loaded "+queries.size()+" queries from file '"+queryfile+"'!!");
        return queries;
    }
}
