package evaluation.partitioning;

import grammar.Edge;
import grammar.Graph;
import grammar.UndirectedGraph;
import partitioning.GraphPartition;
import partitioning.ClosestPairs;
import shortestpath.ScatterMap;
import java.text.NumberFormat;
import java.util.ArrayDeque;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;
import java.io.File;

final public class EvaluateParallelism {
    final static private int[] kappas = {20,40,60,80,100,120,140,160};
    final static private int[] parallelism = {2,4,6,8,10,12,14,16}; // values greater than 1 for multi-threading
    final static private int[] partitions = {2,4,16,8,10,12,14,16};
    final static private float[] datasizes = {.02f,.04f,.06f,.08f,.10f,.12f,.14f,.16f};
    final static private float[] alphas = {0.00f,0.25f,0.50f,0.75f,1.00f};

    final static private String[] networks = {"NA","SF"};
    //final static private String[] networks = {"OL","CA","TG","NA","SF"};

    final static private boolean readPartition = true;

    public static void main (String[] args) throws IOException {
        int Index = 2;
        for (String network : networks) {
            System.out.println ("** Evaluation using the '"+network+"' road network.");

            Graph<Long> graph = new UndirectedGraph<>("../data/lifeifei/"+network+".graph",NumberFormat.getNumberInstance(),true);
            ScatterMap<Long> map = new ScatterMap<>("../data/lifeifei/"+network+".cnode",NumberFormat.getNumberInstance());
            GraphPartition<Long> gp = readPartition?
                    new GraphPartition<>(graph,"../data/lifeifei/A0.0/"+network+partitions[Index]+"/")
                    :new GraphPartition<>(graph,map,partitions[Index],alphas[Index]);

            ArrayDeque<Long> R = loadset (network+"Rids.txt");
            ArrayDeque<Long> S = loadset (network+"Sids.txt");
            int originalRsize = R.size()<<1;
            int originalSsize = S.size()<<1;
            while (datasizes[Index]*originalRsize>R.size()) R.remove();
            while (datasizes[Index]*originalSsize>S.size()) S.remove();
            for (int p : parallelism) {
                long startTime = System.nanoTime();
                ArrayDeque<Edge<Long>> result = ClosestPairs.computeMultiThreaded (gp,R,S,kappas[Index],Float.MAX_VALUE,p);
                long endTime = System.nanoTime();
                float duration = (endTime - startTime) * (float) Math.pow(10,-9);
                System.out.println("["+p+" "+duration+"]");
            }
        }
    }

    private static ArrayDeque<Long> loadset (String filename) throws IOException, FileNotFoundException {
        ArrayDeque<Long> ids = new ArrayDeque<>();
        Scanner sc = new Scanner (new File (filename));
        while (sc.hasNextLong()) ids.add (sc.nextLong());
        return ids;
    }
}
