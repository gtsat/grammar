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

import java.util.Map;
import java.util.HashMap;
import diversion.BestCoverage;
import diversion.ContentSimilarity;
import grammar.DirectedGraph;
import grammar.Graph;

final class EvaluateRadius extends Evaluate {

    public static void main (String[] args) {
        int I = defaultIndex;
        int Q = Integer.parseInt (args[0]);
        boolean minsum = true;
        if (Q<0) {Q=-Q; minsum=false;}

        Zipf zipf = new Zipf (zipfrange,false);

        Graph<Integer> graph = new DirectedGraph<>();
        ContentSimilarity<Integer> context = new ContentSimilarity<>();
        for (int i=0; i<numbernodes[I]; ++i) {
            graph.addNode (i);

            Map<String,Float> description = new HashMap<>();
            for (int j=0;j<numberlemmas[I];++j) 
                description.put ("lemma"+j, zipf.nextZipf()/(float)zipfrange);

            context.insertNode (i,description);
        }

        System.out.println ("** Running diversion evaluation for "+Q+" queries .");

        graph.setErdosRenyiGraph ((float)numberedges[I]/(graph.numberNodes()-1));
        System.out.println ("** Processing graph of " + graph.numberNodes() 
                           +" vertices and "+graph.numberEdges()+" edges.");


        for (int RADIUS : radiuses) {

            float summedscoreCoverage = 0.0f;
            long summeddurationCoverage = 0L;
            long summedmemoryCoverage = 0L;

            for (int q=0; q<Q; ++q) 
            try{
                int qcenter = (int)(Math.random()*graph.numberNodes());


                long start = System.currentTimeMillis();
                summedscoreCoverage += BestCoverage.compute (qcenter,null,graph,context,resultsizes[I],RADIUS,.8f,.0f,.8f,minsum,symmetric);
                summeddurationCoverage += System.currentTimeMillis() - start;
                summedmemoryCoverage += BestCoverage.getMaxMemRequirements();

            }catch (RuntimeException e){
                e.printStackTrace();
                --q;
            }

            System.out.println ("[ "+ RADIUS + "\t " 
                                    + "\t " + summeddurationCoverage/1000.0f/Q 
                                    + "\t " + summedscoreCoverage/Q 
                                    + "\t " + ((summedmemoryCoverage/Q)>>10) + " ]");
        }
    }
}

