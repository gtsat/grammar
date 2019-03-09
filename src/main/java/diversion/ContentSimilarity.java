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

import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Collection;

public class ContentSimilarity<NodeT> {

    private final Map<NodeT,Map<String,Float>> map; // one description per vertex

    public ContentSimilarity (Map<NodeT,Map<String,Float>> initmap) {map = initmap;}
    public ContentSimilarity () {map = new HashMap<>();}


    public Map<NodeT,Map<String,Float>> getModel () {return map;}
    public Set<NodeT> getIndexedNodes () {return map.keySet();}
    public void removeNode (NodeT u) {map.remove(u);}
    public void insertNode (NodeT u, Map<String,Float> content) {map.put(u,content);}
    public Float similarity (NodeT u, NodeT v) {return similarity (map.get(u),v);}

    public Float similarity (Collection<String> query, NodeT u) {
        Map<String,Float> vec = map.get(u);
        if (vec==null) return 0.0f; //throw new RuntimeException ("\n!! ERROR - There is no description for '"+u+"' !!");
        else{
            float sum = 0.0f;
            float norm = 0.0f, norm2 = 0.0f;
            for (String lemma : query) {
                sum += vec.get(lemma);
                norm += vec.get(lemma)*vec.get(lemma);
            }
            return sum / (float) (Math.sqrt(norm * query.size()));
        }
    }

    public Float similarity (Map<String,Float> query, NodeT u) {
        Map<String,Float> vec = map.get(u);
        if (query==null || vec==null) return 0.0f; //throw new RuntimeException ("\n!! ERROR - There is no description for '"+u+"' !!");
        else{
            float sum = 0.0f;
            float normq = 0.0f, normu = 0.0f;
            if (query.size()<vec.size()) {
                for (Entry<String,Float> entry : query.entrySet()) {
                    Float otherval = vec.get(entry.getKey());
                    if (otherval!=null) sum += entry.getValue() * otherval;
                    normq += entry.getValue() * entry.getValue();
                }
                for (Entry<String,Float> entry : vec.entrySet()) 
                    normu += entry.getValue() * entry.getValue();
            }else{
                for (Entry<String,Float> entry : vec.entrySet()) {
                    Float otherval = query.get(entry.getKey());
                    if (otherval!=null) sum += entry.getValue() * otherval;
                    normu += entry.getValue() * entry.getValue();
                }
                for (Entry<String,Float> entry : query.entrySet()) 
                    normq += entry.getValue() * entry.getValue();
            }
            return sum / (float) Math.sqrt(normq * normu);
        }
    }
}
