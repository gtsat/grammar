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

package diversion;

import grammar.Edge;
import grammar.Graph;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Collection;

public class BestCoverage {

    private static long maxMemConsumption = 0L;
    public static long getMaxMemRequirements () {return maxMemConsumption;}


    @SuppressWarnings("unchecked")
    public static<NodeT> float compute (NodeT q, Collection<NodeT> R, 
                                         Graph<NodeT> graph, ContentSimilarity<NodeT> context,
                                         int n, int r, float l, float a, float b,
                                         boolean minsum, boolean symmetric) {

        ArrayList<NodeT> result = new ArrayList<>();

        while (result.size()<n) {
            NodeT next = next (q,result,R,graph,context,r,l,a,b,minsum);
            if (next==null) break;
            else result.add (next);
        }

        return minsum?
               (symmetric?
                new SumDivSetComparatorSymmetric<>(q,graph,context,l,a,b).computeScore(result)
                :new SumDivSetComparatorOrdered<>(q,graph,context,l,a,b).computeScore(result))
                :(symmetric?
                new MaxDivSetComparatorSymmetric<>(q,graph,context,l,a,b).computeScore(result)
                :new MaxDivSetComparatorOrdered<>(q,graph,context,l,a,b).computeScore(result));
    }

/*
    @SuppressWarnings("unchecked")
    public static<NodeT> ArrayList<NodeT> compute (NodeT q, Collection<NodeT> R, 
                                                   Graph<NodeT> graph, ContentSimilarity<NodeT> context,
                                                   int n, int r, float l, float a, float b,
                                                   boolean minsum) {

        ArrayList<NodeT> result = new ArrayList<>();

        while (result.size()<n) {
            NodeT next = next (q,result,R,graph,context,r,l,a,b,minsum);
            if (next==null) break;
            else result.add (next);
        }
        return result;
    }
*/

    private static<NodeT> NodeT next (NodeT q, Collection<NodeT> set, Collection<NodeT> R, 
                                      Graph<NodeT> graph, ContentSimilarity<NodeT> context, 
                                      int radius, float lambda, float alpha, float beta, 
                                      boolean minsum) {

        float annexscore = Float.MAX_VALUE;
        NodeT annex = null;

        maxMemConsumption = 0;
        long memConsumption = 40;

        Collection<NodeT> augmentedset = new ArrayList<>(set);
        augmentedset.add(q);

        HashSet<NodeT> marked = new HashSet<>();
        ArrayDeque<Edge<NodeT>> queue = new ArrayDeque<>();
            
        for (NodeT s : augmentedset) {
            queue.add (new Edge<>(s,s,0.0f));
            marked.add (s);
        }
        
        while (!queue.isEmpty()) {
            Edge<NodeT> top = queue.remove();

            memConsumption -= 16;

            NodeT candidate = top.to;

            float relevance = lambda*(alpha*graph.getPathCost(q,candidate)+(1-alpha)*context.similarity(q,candidate));
            float dissimilarity = minsum?0.0f:Float.MAX_VALUE;

            for (NodeT u : augmentedset) {
                float temp = (1-lambda)*(beta*graph.getPathCost(u,candidate)+(1-beta)*context.similarity(u,candidate));
                if (minsum) dissimilarity += temp;
                else if (temp<dissimilarity) dissimilarity = temp;
            }
            if (minsum) dissimilarity /= set.size();

            float score = relevance - dissimilarity;
            if (score<annexscore) {
                annexscore = score;
                annex = candidate;
            }

            if (top.weight<radius) {
                for (NodeT u : graph.getNeighbors(top.to)) {
                    if (!marked.contains(u) && (R==null || R.contains(u))) {
                        queue.add (new Edge<>(top.from,u,top.weight+1));
                        marked.add (u);

                        memConsumption += 16;
                    }
                }
            }

            if (memConsumption>maxMemConsumption) 
                maxMemConsumption = memConsumption;
            maxMemConsumption += (marked.size()<<2);
        }
        return annex;
    }
}
