/**
 *  The GRAph and Matrix MAnipulation Resource (GRA.M.MA.R) toolkit
 *  Copyright (C) 2015 George Tsatsanifos <gtsatsanifos@gmail.com>
 *
 *  The GRA.M.MA.R. toolkit is free software: you can redistribute it and/or 
 *  modify it under the terms of the GNU General Public License as published 
 *  by the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package evaluation.multipoint;

import oscp.Group;
import grammar.Graph;
import grammar.DirectedGraph;
import grammar.UndirectedGraph;
import omcp.ComputeClusteredGrouping;
import omcp.ComputeHeuristicGrouping;
import omcp.ComputeExtensiveGrouping;
import omcp.GroupingComparatorMinMax;
import omcp.GroupingComparatorMinSum;
import omcp.GroupingComparator;
import java.text.NumberFormat;
import java.util.ArrayDeque;
import java.io.IOException;

final public class ConnectingPointTravelersEvaluator extends ConnectingPointEvaluator {

    public static void main (String[] args) throws IOException {
        int maxQueries = args.length>0?Integer.parseInt(args[0]):Integer.MAX_VALUE;
        if (maxQueries<0) {maxQueries=-maxQueries; useMinSumRanking=false;}
        else useMinSumRanking=true;
        System.out.println("   ----------------------------------------");
        System.out.println("     Optimal Connecting Points Evaluation  ");
        System.out.println("   ----------------------------------------");

        for (String network : networks) {
            System.out.println ("** Evaluation of travelers scalability using the "+network
                               +" road network and "+(useMinSumRanking?new String("MinSum"):new String("MinMax"))+" ranking.");

            Graph<Long> graph = directed?
                                new DirectedGraph<Long>("../data/dimacs/USA-road-d."+network+".gr",NumberFormat.getNumberInstance(),true)
                                :new UndirectedGraph<Long>("../data/dimacs/USA-road-d."+network+".gr",NumberFormat.getNumberInstance(),true);
                                //:new UndirectedGraph<>("../data/lifeifei/"+network+".graph",NumberFormat.getNumberInstance(),true);

            long startTime;

            float area = coverages [1];
            float target = distances [1];
            float selflambda = selflambdas [1];

            Group<Long> result;
            for (int n : travelers) {
                float lambdas[][] = new float[n][n];
                for (int i=0; i<n; ++i)
                    for (int j=0; j<n; ++j)
                        if (i==j) lambdas[i][j] = selflambda;
                        else lambdas[i][j] = (1-selflambda)/(n-1);

                //ArrayDeque<Long[]> queries = loadQueries("../queries/T4/"+network+"/QueryN"+n+"A"+(int)(area*100)+"T"+(int)(target*100)+".txt");
                ArrayDeque<Long[]> queries = loadQueries("../queries/USnew/"+network+"/QueryN"+n+"A"+(int)(area*100)+"T"+(int)(target*100)+".txt");

                int Qid = 0;

                long cpMem = 0;
                long hpMem = 0;
                long epMem = 0;

                long cpTime = 0;
                long hpTime = 0;
                long epTime = 0;

                float cpScore = 0.0f;
                float hpScore = 0.0f;
                float epScore = 0.0f;

                for (Long[] query : queries) {
                    if (Qid>=maxQueries) break;

                    int i=0;
                    Long[] travelers = new Long [n];
                    for (Long q : query) if (i<n) travelers[i++] = q;
                    Group<Long> grouping = new Group<>(targetless?null:query[n],travelers,lambdas,graph,useMinSumRanking);
                    GroupingComparator<Long> cmp = useMinSumRanking?new GroupingComparatorMinSum<>(graph):new GroupingComparatorMinMax<>(graph);

                    try{
                    startTime = System.nanoTime();
                    result = ComputeHeuristicGrouping.compute (grouping,graph,useMinSumRanking);
                    hpTime += System.nanoTime() - startTime;
                    hpScore += useMinSumRanking?
                                  ((GroupingComparatorMinSum<Long>)cmp).computeScore(result)
                                  :((GroupingComparatorMinMax<Long>)cmp).computeScore(result);
                    hpMem += ComputeHeuristicGrouping.getMaxMemRequirements();
                    ComputeHeuristicGrouping.resetStats();

                    startTime = System.nanoTime();
                    result = ComputeExtensiveGrouping.compute (grouping,graph,useMinSumRanking);
                    epTime += System.nanoTime() - startTime;
                    epScore += useMinSumRanking?
                                    ((GroupingComparatorMinSum<Long>)cmp).computeScore(result)
                                    :((GroupingComparatorMinMax<Long>)cmp).computeScore(result);
                    epMem += ComputeExtensiveGrouping.getMaxMemRequirements();
                    ComputeExtensiveGrouping.resetStats();

                    startTime = System.nanoTime();
                    result = ComputeClusteredGrouping.compute (grouping,graph,useMinSumRanking);
                    cpTime += System.nanoTime() - startTime;
                    cpScore += useMinSumRanking?
                                ((GroupingComparatorMinSum<Long>)cmp).computeScore(result)
                                :((GroupingComparatorMinMax<Long>)cmp).computeScore(result);
                    cpMem += ComputeClusteredGrouping.getMaxMemRequirements();
                    ComputeClusteredGrouping.resetStats();

                    ++Qid;
                    }catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                System.out.print ("["+n+"\t"+cpTime/Math.pow(10,9)/Qid+"\t"+hpTime/Math.pow(10,9)/Qid+"\t"+epTime/Math.pow(10,9)/Qid
                                       +"\t"+cpScore/Qid+"\t"+hpScore/Qid+"\t"+epScore/Qid+"\t"
                                       +cpMem/(float)Qid+"\t"+hpMem/(float)Qid+"\t"+epMem/(float)Qid+"]\n");
            }
        }
    }
}

