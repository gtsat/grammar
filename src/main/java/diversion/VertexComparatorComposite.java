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
import java.util.Map.Entry;
import java.util.Comparator;

abstract class VertexComparatorComposite<NodeT> implements Comparator<NodeT> {

    final protected Map<NodeT,Float> scores = new HashMap<>();
    final protected NodeT q;


    final protected Graph<NodeT> graph;
    final protected ContentSimilarity<NodeT> sim;

    final protected float lambda;
    final protected float alpha;
    final protected float beta;

    public VertexComparatorComposite (NodeT q, Graph<NodeT>graph, ContentSimilarity<NodeT>sim,float l,float a,float b) {
        this.graph = graph;
        this.sim = sim;
        this.q = q;

        lambda = l;
        alpha = a;
        beta = b;
    }

    public void overrideStoreScore (NodeT vertex, Float score) {
        if (score==null) scores.remove(vertex);
        else scores.put(vertex,score);
    }

    abstract public float computeScore (NodeT newvertex, Map<NodeT,Float> vertexdists);
    abstract public float computeScore (Map<NodeT,Float> vertexdists);

    @Override public int compare (NodeT x, NodeT y) {
        if (!scores.containsKey(x) && !scores.containsKey(y)) return 0;
        else if (!scores.containsKey(y)) return -1;
        else if (!scores.containsKey(x)) return 1;
        else{
            float balance = scores.get(x) - scores.get(y);
            if (balance<0) return -1;
            else if (balance>0) return 1;
            else return 0; 
        }
    }
}

final class VertexSumComparatorComposite<NodeT> extends VertexComparatorComposite<NodeT> {

    public VertexSumComparatorComposite(NodeT q,Graph<NodeT>graph,ContentSimilarity<NodeT>sim,float l,float a,float b) {super(q,graph,sim,l,a,b);}

    @Override public float computeScore (Map<NodeT,Float> vertexscores) {
        float score = 0.0f;
        for (Entry<NodeT,Float> entry : vertexscores.entrySet())
            if (!entry.getKey().equals(q))
                score -= entry.getValue();

        Float sigmaq = vertexscores.get(q);
        return sigmaq!=null?lambda*sigmaq+2*(1-lambda)*score/(vertexscores.size()-1):2*(1-lambda)*score/vertexscores.size();
    }

    @Override public float computeScore (NodeT u, Map<NodeT,Float> vertexdists) {
        float score = 0.0f;
        for (Entry<NodeT,Float> entry : vertexdists.entrySet())
            if (!entry.getKey().equals(q))
                score -= beta*entry.getValue() + (1-beta)*graph.numberNodes()*(1-sim.similarity(u,entry.getKey()));
        score *= (1-lambda);
        Float dqs = vertexdists.get(q);
        if (dqs!=null) {
            score /= vertexdists.size()-1;
            score += lambda * (alpha*dqs + 
                    (1-alpha)*graph.numberNodes()*(1-sim.similarity(u,q)));
        }else score /= vertexdists.size();

        scores.put(u,score);
        return score;
    }
}

final class VertexMaxComparatorComposite<NodeT> extends VertexComparatorComposite<NodeT> {

    public VertexMaxComparatorComposite(NodeT q,Graph<NodeT>graph,ContentSimilarity<NodeT>sim,float l,float a,float b) {super(q,graph,sim,l,a,b);}

    @Override public float computeScore (Map<NodeT,Float> vertexscores) {
        float mindist = Float.MAX_VALUE;
        for (Entry<NodeT,Float> entry : vertexscores.entrySet())
            if (!entry.getKey().equals(q))
                if (entry.getValue()<mindist)
                    mindist = entry.getValue();

        Float dqs = vertexscores.get(q);
        return dqs!=null?lambda*dqs-(1-lambda)*mindist:-(1-lambda)*mindist;
    }

    @Override public float computeScore (NodeT u, Map<NodeT,Float> vertexdists) {
        float mindist = Float.MAX_VALUE;
        for (Entry<NodeT,Float> entry : vertexdists.entrySet()) {
            if (!entry.getKey().equals(q)) {
                float tempdist = beta*entry.getValue() + (1-beta)*graph.numberNodes()*(1-sim.similarity(u,entry.getKey()));
                if (tempdist<mindist) mindist = tempdist;
            }
        }
        Float dqs = vertexdists.get(q);
        float score = dqs!=null?lambda*(alpha*dqs+
                                 (1-alpha)*graph.numberNodes()*(1-sim.similarity(u,q)))-(1-lambda)*mindist
                                 :-(1-lambda)*mindist;
        scores.put(u,score);
        return score;
    }
}

