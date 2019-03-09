package evaluation.trajectories;

import grammar.Edge;
import grammar.Graph;
import grammar.UndirectedGraph;
import partitioning.GraphPartition;
import partitioning.ClosestPairs;
import shortestpath.ScatterMap;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.ArrayList;
import java.io.IOException;
import java.util.Scanner;
import java.io.File;
import java.io.FileNotFoundException;

final public class EvaluateRsize {
    final static private int[] kappas = {10,20,40,80};
    final static private int[] parallelism = {2,4,6,8};
    final static private int[] partitions = {2,4,6,8,10,12,14,16,18};
    final static private float[] distances = {.01f,.02f,.04f,.08f};
    final static private float[] alphas = {0.00f,0.25f,0.50f,0.75f,1.00f};

    final static private String[] networks = {"NA","SF"};
    //final static private String[] networks = {"OL","TG","NA","SF"};

    final static private boolean readPartition = true;

    public static void main (String[] args) throws IOException {
        int Index = 2;
        for (String network : networks) {
            System.out.println ("** Evaluation using the '"+network+"' road network.");

            Graph<Long> graph = new UndirectedGraph<>("../data/lifeifei/"+network+".graph",NumberFormat.getNumberInstance(),true);
            ScatterMap<Long> map = new ScatterMap<>("../data/lifeifei/"+network+".cnode",NumberFormat.getNumberInstance());

            ArrayList<ArrayList<Long>> trajectoriesS = loadTrajectories ("../queries/trajectories/"+network+"/"+distances[Index],1000);

            GraphPartition<Long> gp = readPartition?
                    new GraphPartition<>(graph,"../data/lifeifei/A0.0/"+network+partitions[Index]+"/")
                    :new GraphPartition<>(graph,map,partitions[Index],alphas[Index]); 

            float duration = 0.0f;
            for (float distance : distances) {

                ArrayList<ArrayList<Long>> trajectoriesR = loadTrajectories ("../queries/trajectories/"+network+"/"+distance,100);

                for (int i=0; i<trajectoriesR.size();) {
                    Collection<Long> R = trajectoriesR.get(i++);
                    Collection<Long> S = trajectoriesS.get(i++);

                    long startTime = System.nanoTime();
                    Collection<Edge<Long>> result = ClosestPairs.computeMultiThreaded (gp,R,S,kappas[Index],Float.MAX_VALUE,parallelism[Index]);
                    long endTime = System.nanoTime();
                    duration += (endTime - startTime)*Math.pow(10,-9); // normalized in seconds
                }
                System.out.println("["+distance+" "+duration/(trajectoriesR.size()>>1)+"]");
            }   
        }
    }

    private static ArrayList<ArrayList<Long>> loadTrajectories (String foldername, int N) throws IOException, FileNotFoundException {
        ArrayList<ArrayList<Long>> trajectories = new ArrayList<>();
        for (int i=0; i<N; ++i) {
            ArrayList<Long> trajectory = new ArrayList<>();
            Scanner sc = new Scanner (new File (foldername+"/trajectory"+i));
            while (sc.hasNextLong()) trajectory.add (sc.nextLong());
            trajectories.add(trajectory);
        }
        return trajectories;
    }
}
