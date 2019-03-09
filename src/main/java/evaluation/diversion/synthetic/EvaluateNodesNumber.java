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

package evaluation.diversion.synthetic;

import grammar.Graph;
import grammar.DirectedGraph;
import diversion.SwapDiversion;
import diversion.GreedyDiversion;
import diversion.ContentSimilarity;
import diversion.BestCoverage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

final class EvaluateNodesNumber extends Evaluate {

    public static void main (String[] args) {
        int I = Evaluate.defaultIndex;
        int Q = Integer.parseInt (args[0]);
        boolean minsum = true;
        if (Q<0) {Q=-Q; minsum=false;}

        int RADIUS = radiuses[I];

        Zipf zipf = new Zipf (zipfrange,false);

        System.out.println ("** Running diversion evaluation for "+Q+" queries .");

        for (int nodes : Evaluate.numbernodes) {
            Graph<Integer> graph = new DirectedGraph<>();
            ContentSimilarity<Integer> context = new ContentSimilarity<>();
            for (int i=0; i<nodes; ++i) {
                graph.addNode (i);

                Map<String,Float> description = new HashMap<>();
                for (int j=0;j<Evaluate.numberlemmas[I];++j) 
                    description.put ("lemma"+j, zipf.nextZipf()/(float)zipfrange);

                context.insertNode (i,description);
            }

            graph.setErdosRenyiGraph ((float)Evaluate.numberedges[I]/(graph.numberNodes()-1));
            System.out.println ("** Processing graph of " + graph.numberNodes()
                               +" vertices and "+graph.numberEdges()+" edges.");


            long summeddurationCoverage = 0L;
            long summeddurationGreedy = 0L;
            long summeddurationSwap = 0L;

            long summedmemoryCoverage = 0L;
            long summedmemoryGreedy = 0L;
            long summedmemorySwap = 0L;

            float summedscoreCoverage = 0.0f;
            float summedscoreGreedy = 0.0f;
            float summedscoreSwap = 0.0f;

            for (int q=0; q<Q; ++q) 
            try{
                int qcenter = (int)(Math.random()*graph.numberNodes());


                long start = System.currentTimeMillis();
                summedscoreCoverage += BestCoverage.compute (qcenter,null,graph,context,resultsizes[I],RADIUS,.8f,.0f,.8f,minsum,symmetric);
                summeddurationCoverage += System.currentTimeMillis() - start;
                summedmemoryCoverage += BestCoverage.getMaxMemRequirements();


                ArrayList<Float> scores = new ArrayList<>();

                start = System.currentTimeMillis();
                ArrayList<ArrayList<Integer>> result = GreedyDiversion.compute (qcenter,null,graph,context,
                                                                                numberseeds[I],resultsizes[I],
                                                                                minsum,symmetric,
                                                                                .8f,.0f,.8f,scores);
                long durationGreedy = System.currentTimeMillis() - start;
                long memoryGreedy = GreedyDiversion.getMaxMemRequirements();
                float scoreGreedy = scores.get(0);

                checkResult (result,numberseeds[I],resultsizes[I]);


                start = System.currentTimeMillis();
                SwapDiversion.compute (qcenter,result,null,graph,context,numberseeds[I],minsum,symmetric,.8f,.0f,.8f,scores);
                summeddurationSwap += System.currentTimeMillis() - start;
                summedmemorySwap += SwapDiversion.getMaxMemRequirements();
                summedscoreSwap += scores.get(0);

                summeddurationGreedy += durationGreedy;
                summedmemoryGreedy += memoryGreedy;
                summedscoreGreedy += scoreGreedy;
            }catch (RuntimeException e){
                e.printStackTrace();
                --q;
            }
            System.out.println ("[ "+ nodes + "\t " + summeddurationGreedy/1000.0f/Q + "\t " + summeddurationSwap/1000.0f/Q + "\t " + summeddurationCoverage/1000.0f/Q 
                                    + "\t " + summedscoreGreedy/Q + "\t " + summedscoreSwap/Q + "\t " + summedscoreCoverage/Q 
                                    + "\t " + ((summedmemoryGreedy/Q)>>10) + "\t " + ((summedmemorySwap/Q)>>10) + "\t " + ((summedmemoryCoverage/Q)>>10) + " ]");
        }
    }
}

