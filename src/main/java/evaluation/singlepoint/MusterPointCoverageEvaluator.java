package evaluation.singlepoint;

import oscp.Group;
import osmp.MinSumComparator;
import osmp.MinMaxComparator;
import shortestpath.ScatterMap;
import shortestpath.ShortestPath;
import grammar.UndirectedGraph;
import java.text.NumberFormat;
import java.util.Comparator;
import java.util.ArrayDeque;
import java.io.IOException;
import java.util.Scanner;
import java.io.File;

final public class MusterPointCoverageEvaluator {
    final static private int[] travelers = {1,2,3,4,5,6,7,8,9};
    final static private float[] coverages = {.1f,.2f,.3f,.4f,.5f,.6f,.7f,.8f,.9f};
    final static private float[] distances = {.1f,.2f,.3f,.4f,.5f,.6f,.7f,.8f,.9f};

    final static private String[] networks = {"OL","CA","TG","NA","SF"};

    static private boolean useMinSumRanking;

    public static void main (String[] args) throws IOException {
        int maxQueries = args.length>0?Integer.parseInt(args[0]):Integer.MAX_VALUE;
        if (maxQueries<0) {maxQueries=-maxQueries; useMinSumRanking=true;}
        else useMinSumRanking=false;
        System.out.println(" ---------------------------");
        System.out.println("   osmp Point Evaluation  ");
        System.out.println(" ---------------------------");

        for (String network : networks) {
            System.out.println ("** Evaluation of coverage area scalability using the "+network
                               +" road network and "+(useMinSumRanking?new String("MinSum"):new String("MinMax"))+" ranking.");

            UndirectedGraph<Long> graph = new UndirectedGraph<Long>("../data/lifeifei/"+network+".graph",NumberFormat.getNumberInstance(),true).kruskal();
            UndirectedGraph<Long> apsp = graph; //new UndirectedGraph<>("../data/lifeifei/"+network+".mst.apsp.graph",NumberFormat.getNumberInstance(),true);
            ScatterMap<Long> map = new ScatterMap<>("../data/lifeifei/"+network+".cnode",NumberFormat.getNumberInstance());
            ShortestPath<Long> sp = new ShortestPath<>(graph,map);

            long mu;
            long startTime;

            int n = travelers [2];
            float target = distances [2];

            float lambdas[][] = new float[n][n];
            for (int i=0; i<n; ++i)
                for (int j=0; j<n; ++j)
                    if (i==j) lambdas[i][j] = .5f;
                    else lambdas[i][j] = .5f/(n-1);


            for (float area : coverages) {
                ArrayDeque<Long[]> queries = loadQueries("../queries/new/"+network+"/QueryN"+n+"A"+(int)(area*100)+"T"+(int)(target*100)+".txt");

                int Qid = 0;

                long osmpTime = 0;
                long convexTime = 0;
                long centroidTime = 0;
                long centroidTimeBFS = 0;

                long osmpMem = 0;
                long convexMem = 0;
                long centroidMem = 0;
                long centroidMemBFS = 0;

                float osmpScore = .0f;
                float convexScore = .0f;
                float centroidScore = .0f;
                float centroidScoreBFS = .0f;

                for (Long[] query : queries) {
                    if (Qid>=maxQueries) break;

                    Group<Long> grouping = new Group<>(null,query,lambdas,graph,useMinSumRanking);
                    Comparator<Long> cmp = useMinSumRanking?new MinSumComparator<>(grouping,apsp):new MinMaxComparator<>(grouping,apsp);

                    startTime = System.nanoTime();
                    mu = centroid.ComputeMusterPoint.compute (grouping,graph,map,cmp,false);
                    centroidTime += System.nanoTime() - startTime;
                    centroidScore += useMinSumRanking?((MinSumComparator<Long>)cmp).computeScore(mu):((MinMaxComparator<Long>)cmp).computeScore(mu);
                    centroidMem += centroid.ComputeMusterPoint.getMaxMemRequirements() + map.getMaxMemRequirements();
                    centroid.ComputeMusterPoint.resetStats();

                    cmp = useMinSumRanking?new MinSumComparator<>(grouping,apsp):new MinMaxComparator<>(grouping,apsp);

                    startTime = System.nanoTime();
                    mu = centroid.ComputeMusterPoint.compute (grouping,graph,map,cmp,true);
                    centroidTimeBFS += System.nanoTime() - startTime;
                    centroidScoreBFS += useMinSumRanking?((MinSumComparator<Long>)cmp).computeScore(mu):((MinMaxComparator<Long>)cmp).computeScore(mu);
                    centroidMemBFS += centroid.ComputeMusterPoint.getMaxMemRequirements() + map.getMaxMemRequirements();
                    centroid.ComputeMusterPoint.resetStats();

                    cmp = useMinSumRanking?new MinSumComparator<>(grouping,apsp,sp):new MinMaxComparator<>(grouping,apsp,sp);

                    startTime = System.nanoTime();
                    mu = osmp.ComputeMusterPoint.compute (grouping,graph,cmp);
                    osmpTime += System.nanoTime() - startTime;
                    osmpScore += useMinSumRanking?((MinSumComparator<Long>)cmp).computeScore(mu):((MinMaxComparator<Long>)cmp).computeScore(mu);
                    osmpMem += osmp.ComputeMusterPoint.getMaxMemRequirements() + sp.getMaxMemRequirements() + map.getMaxMemRequirements()>>1;
                    osmp.ComputeMusterPoint.resetStats();
/**
                    cmp = useMinSumRanking?new MinSumComparator<>(grouping,apsp):new MinMaxComparator<>(grouping,apsp);

                    startTime = System.nanoTime();
                    mu = convexhull.ComputeMusterPoint.compute (grouping,graph,map,cmp);
                    convexTime += System.nanoTime() - startTime;
                    convexScore += useMinSumRanking?((MinSumComparator<Long>)cmp).computeScore(mu):((MinMaxComparator<Long>)cmp).computeScore(mu);
                    convexMem += convexhull.ComputeMusterPoint.getMaxMemRequirements() + map.getMaxMemRequirements();
                    convexhull.ComputeMusterPoint.resetStats();
**/
                    ++Qid;
                }
                System.out.print ("["+n+"\t"+osmpTime/Math.pow(10,9)/Qid+"\t"+convexTime/Math.pow(10,9)/Qid+"\t"+centroidTime/Math.pow(10,9)/Qid+"\t"+centroidTimeBFS/Math.pow(10,9)/Qid+"\t"
                       +osmpScore/(float)Qid+"\t"+convexScore/(float)Qid+"\t"+centroidScore/(float)Qid+"\t"+centroidScoreBFS/(float)Qid+"\t"
                        +osmpMem/(float)Qid+"\t"+convexMem/(float)Qid+"\t"+centroidMem/(float)Qid+"\t"+centroidMemBFS/(float)Qid+"]\n");
            }
        }
    }

    private static ArrayDeque<Long[]> loadQueries (String queryfile) throws IOException {
        ArrayDeque<Long[]> queries = new ArrayDeque<>();
        Scanner in = new Scanner (new File (queryfile)) ;
        while (in.hasNextLine()) {
            String[] line = in.nextLine().split("\\s+");
            Long[] query = new Long [line.length-1];
            int i=0;
            for (String element : line)
                if (i<line.length-1)
                    query[i++] = Long.parseLong(element);
            queries.add(query);
        }
        System.out.println ("!! Loaded "+queries.size()+" queries from file '"+queryfile+"'!!");
        return queries;
    }
}

