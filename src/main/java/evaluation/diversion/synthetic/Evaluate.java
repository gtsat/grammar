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

import java.util.ArrayList;
import diversion.ContentSimilarity;
import grammar.DirectedGraph;
import grammar.Graph;

abstract public class Evaluate {

    protected static final boolean symmetric = true;

    protected static final int defaultIndex   = 1;

    protected static final int[] resultsizes  = {5,10,20,40};
    protected static final int[] numberedges  = {5,10,20,40};
    protected static final int[] numbernodes  = {5000,10000,20000,40000};
    protected static final int[] numberlemmas = {50,100,200,400};
    protected static final int[] numberseeds  = {1,2,3,4};

    protected static final float[] lambdas   = {.2f,.4f,.6f,.8f};
    protected static final float[] betas     = {.2f,.4f,.6f,.8f};

    protected static final long[] timeouts    = {50,100,200,400};

    protected static final int[] radiuses     = {2,4,6,8};

    protected static final int zipfrange      = 1000;

    private static Graph<Integer> generateWebGraph (int vertices, int edges) {
        Graph<Integer> graph = new DirectedGraph<>();
        for (int i=0; i<vertices; ++i)
            graph.addPreferentialNode (i,edges,1.0f);
        return graph;
    }

    /* 
     * Generates graph and connects nodes with 
     * textual descriptions that are similar 
     * above the given threshold in [0,1]
     */
    private static Graph<Integer> generateWikiGraph (ContentSimilarity<Integer> context, float threshold) {
        if (context==null) throw new IllegalArgumentException ("\n!! ERROR - A collection of textual descriptions is required !!");
        if (threshold<0 || threshold>1) throw new RuntimeException("\n!! ERROR - Similarity threshold should be in [0,1] interval !!");

        Graph<Integer> graph = new DirectedGraph<>();
        for (int i : context.getIndexedNodes())
            for (int j : context.getIndexedNodes())
                if (i!=j && context.similarity(i,j)>threshold)
                    graph.setEdge(i, j, 1.0f);

        return graph;
    }

    static protected void checkResult (ArrayList<ArrayList<Integer>> result, int k, int n) {
        if (result.size()<k) throw new RuntimeException ("\n!! ERROR - Not the appropriate result cardinality ("+result.size()+"<"+k+") !!");
        for (ArrayList<Integer> resultElement : result) 
            if (resultElement.size()<n)
                throw new RuntimeException ("\n!! ERROR - Insufficient number of result elements !!");
    }
}
