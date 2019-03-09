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

import java.util.Map;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Comparator;

abstract class VertexComparator<NodeT> implements Comparator<NodeT> {
    final protected Map<NodeT,Float> scores = new HashMap<>();
    final protected NodeT q;

    final protected float lambda = .5f;

    public VertexComparator (NodeT q) {this.q=q;}
    public void overrideStoreScore (NodeT vertex,Float score) {
        if (score==null) scores.remove(vertex);
        else scores.put(vertex,score);
    }
    abstract public float computeScore (Map<NodeT,Float> vertexdists);
    public float computeStoreScore (NodeT newvertex, Map<NodeT,Float> vertexdists) {
        float score = computeScore(vertexdists);
        scores.put (newvertex,score);
        return score;
    }

    @Override public int compare (NodeT x,NodeT y) {
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

final class VertexSumComparator<NodeT> extends VertexComparator<NodeT> {
    public VertexSumComparator (NodeT q) {super(q);}
    @Override public float computeScore (Map<NodeT,Float> vertexdists) {
        float score = 0.0f;
        for (Entry<NodeT,Float> entry : vertexdists.entrySet())
            if (!entry.getKey().equals(q))
                score -= entry.getValue();
        Float dqs = vertexdists.get(q);
        return dqs!=null?lambda*dqs+(1-lambda)*score/(vertexdists.size()-1):(1-lambda)*score/vertexdists.size();
    }
}

final class VertexMaxComparator<NodeT> extends VertexComparator<NodeT> {
    public VertexMaxComparator (NodeT q) {super(q);}
    @Override public float computeScore (Map<NodeT,Float> vertexdists) {
        float mindist = Float.MAX_VALUE;
        for (Entry<NodeT,Float> entry : vertexdists.entrySet()) {
            if (!entry.getKey().equals(q)) {
                float tempdist = entry.getValue();
                if (tempdist<mindist) mindist=tempdist;
            }
        }
        Float dqs = vertexdists.get(q);
        return dqs!=null?lambda*dqs-(1-lambda)*mindist:-(1-lambda)*mindist;
    }
}
