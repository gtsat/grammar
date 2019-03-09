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

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Scanner;

abstract public class ConnectingPointEvaluator {

    final static protected boolean directed = false;
    final static protected boolean targetless = false;

    final static protected int[] travelers = {2,3,4,5};
    final static protected float[] coverages = {.05f,.10f,.15f};
    final static protected float[] distances = {.05f,.10f,.15f};
    final static protected float[] selflambdas = {1-3*.05f,1-3*.10f,1-3*.15f};

    //final static protected String[] networks = {"OL","CA","TG","NA","SF"};
    final static protected String[] networks = {"NY","BAY"};

    static protected boolean useMinSumRanking;

    protected static ArrayDeque<Long[]> loadQueries (String queryfile) {
        ArrayDeque<Long[]> queries = new ArrayDeque<>();
        try{
        Scanner in = new Scanner (new File (queryfile)) ;
        while (in.hasNextLine()) {
            String[] line = in.nextLine().split("\\s+");
            Long[] query = new Long [line.length];
            int i=0;
            for (String element : line)
                query[i++] = Long.parseLong(element);
            queries.add(query);
        }
        }catch (IOException e){
            e.printStackTrace();
        }
        System.out.println ("!! Loaded "+queries.size()+" queries from file '"+queryfile+"'!!");
        return queries;
    }
}
