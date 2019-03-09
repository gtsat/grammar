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

import grammar.Graph;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Comparator;

abstract class DivSetComparatorSymmetric<NodeT> implements Comparator<ArrayList<NodeT>> {

    final protected Map<ArrayList<NodeT>,Float> cache = new HashMap<>();
    final protected ContentSimilarity<NodeT> context;
    final protected Graph<NodeT> graph;
    final protected NodeT q;

    final protected float lambda;
    final protected float alpha;
    final protected float beta;

    public DivSetComparatorSymmetric (NodeT q,Graph<NodeT> graph,ContentSimilarity<NodeT> context,float l,float a,float b) {
        this.context = context;
        this.graph = graph;
        this.q = q;

        lambda = l;
        alpha = a;
        beta = b; 
    }
    @Override public int compare (ArrayList<NodeT> setx, ArrayList<NodeT> sety) {
        if (setx==sety) return 0;
        else if (sety==null) return -1;
        else if (setx==null) return 1;
        else{
            float balance = computeScore(setx) - computeScore(sety);
            if (balance<0) return -1;
            else if (balance>0) return 1;
            else return 0;
        }
    }
    abstract public float computeScore (ArrayList<NodeT> set);
}

final class SumDivSetComparatorSymmetric<NodeT> extends DivSetComparatorSymmetric<NodeT> {
    public SumDivSetComparatorSymmetric (NodeT q,Graph<NodeT> graph,ContentSimilarity<NodeT> context,float l,float a,float b) {super(q,graph,context,l,a,b);}
    @Override public float computeScore (ArrayList<NodeT> set) {
        Float cached = cache.get(set);
        if (cached!=null) return cached;
        else{
            float qfactor = 0.0f;
            float Sfactor = 0.0f;
            for (NodeT s : set) {
                qfactor += .5*alpha*(graph.getPathCost(q,s)+graph.getPathCost(s,q)) + 
                           (1-alpha)*graph.numberNodes()*(1-context.similarity(q,s));
                for (NodeT recipr : set) {
                    if (!recipr.equals(s)) {
                        Sfactor += beta*graph.getPathCost(s,recipr) 
                                + (1-beta)*graph.numberNodes()*(1-context.similarity(s,recipr));
                    }
                }
            }
            float score = (lambda*qfactor - (1-lambda)*Sfactor /(set.size()-1)) / set.size();
            cache.put (set,score);
            return score;
        }
    }
}

final class MaxDivSetComparatorSymmetric<NodeT> extends DivSetComparatorSymmetric<NodeT> {
    public MaxDivSetComparatorSymmetric (NodeT q,Graph<NodeT> graph,ContentSimilarity<NodeT> context,float l,float a,float b) {super(q,graph,context,l,a,b);}
    @Override public float computeScore (ArrayList<NodeT> set) {
        Float cached = cache.get(set);
        if (cached!=null) return cached;
        else{
            float qfactor = 0.0f;
            float Sfactor = Float.MAX_VALUE;
            for (NodeT s : set) {
                float qdist = .5f*alpha*(graph.getPathCost(q,s)+graph.getPathCost(s,q)) + 
                               (1-alpha)*graph.numberNodes()*(1-context.similarity(q,s));
                if (qdist>qfactor) qfactor = qdist;
                for (NodeT recipr : set) {
                    if (!recipr.equals(s)) {
                        float Sdist = beta*graph.getPathCost(s,recipr) + (1-beta)*graph.numberNodes()*(1-context.similarity(s,recipr));
                        if (Sdist<Sfactor) Sfactor = Sdist;
                    }
                }
            }
            float score = lambda*qfactor - (1-lambda)*Sfactor;
            cache.put (set,score);
            return score;
        }
    }
}
