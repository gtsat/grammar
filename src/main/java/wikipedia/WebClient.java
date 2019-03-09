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

package wikipedia;

import java.util.ArrayList;
import java.util.ArrayDeque;
import org.apache.log4j.Logger;

public class WebClient {

    private static final Logger logger = Logger.getLogger (WebClient.class);

    static {
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("encoding", "UTF-8");
        System.setProperty("user.language", "el_gr.UTF-8");
        System.setProperty("user.country", "el_gr.UTF-8");
        System.setProperty("sun.jnu.encoding", "UTF8");
    }

    private String qcenter = null;

    private String query = "";
    public void setQuery (String q) {query=q;};
    public String getQuery () {return query;}

    private String ranking = "minsum";
    public void setRanking (String r) {ranking=r;};
    public String getRanking () {return ranking;}

    private String mechanism = "ordered";
    public void setMechanism (String m) {mechanism=m;};
    public String getMechanism () {return mechanism;}

    private String language = "greek";
    public void setLanguage (String l) {language=l;};
    public String getLanguage () {return language;}

    public String getMechanismSelection (String input) {return mechanism.compareTo(input)==0?"SELECTED":"";}
    public String getLanguageSelection (String input) {return language.compareTo(input)==0?"SELECTED":"";}
    public String getRankingSelection (String input) {return mechanism.compareTo(input)==0?"SELECTED":"";}
    public boolean isMinSum () {return ranking.equalsIgnoreCase("minsum");}

    private ArrayList<String> result = null;

    private String red = "120";
    public void setRed (String r) {red=r;}
    public String getRed () {return red;}

    private String green = "0";
    public void setGreen (String g) {green=g;}
    public String getGreen () {return green;}

    private String blue = "255";
    public void setBlue (String b) {blue=b;}
    public String getBlue () {return blue;}

    private String offset = "0";
    public String getOffset () {return offset;}
    public void setOffset (String newoffset) {offset=newoffset;}
    public void incrementOffset (int increment) {offset=new Integer(Integer.parseInt(offset)+increment).toString();}
    public void decrementOffset (int decrement) {offset=new Integer(Integer.parseInt(offset)-decrement).toString();}


    private static DiversePedia dp = null;

    public void reset () {offset="0";qcenter=null;result = null;}
    public WebClient (String edgelist, String model, String titles) {
        if (dp==null) {
            dp = new DiversePedia (edgelist,model,titles);
            dp.setMaxCachingRadius (Integer.MAX_VALUE);
        }
    }
    public WebClient () {
        if (dp==null) {
            dp = new DiversePedia ("diversepedia/edgelist.filtered.txt","diversepedia/model.txt","diversepedia/titles.txt");
            dp.setMaxCachingRadius (Integer.MAX_VALUE);
        }
    }

    public String query (int kc, int increment) {
        dp.setSymmetric(mechanism.compareTo("symmetric")==0);

        if (qcenter==null) {
            ArrayDeque<String> queryterms = new ArrayDeque<>();
            for (String term : query.split("\\s+")) queryterms.add(term);

            qcenter = dp.qcenterByTitle (queryterms);
            if (qcenter==null) qcenter = dp.qcenterByContent (queryterms);
            if (qcenter==null) throw new RuntimeException ("\n!! ERROR - Unable to retrieve a center for the query !!");

            float lambda = Integer.parseInt(red)/255.0f;
            float alpha = Integer.parseInt(green)/255.0f;
            float beta = Integer.parseInt(blue)/255.0f;
            dp.setWeightParameters (lambda,alpha,beta);

            logger.info (Thread.currentThread().getName()+"<"+Thread.currentThread().getId()+"> Query center: "+qcenter);
            logger.info (Thread.currentThread().getName()+"<"+Thread.currentThread().getId()+"> lambda: "+lambda);
            logger.info (Thread.currentThread().getName()+"<"+Thread.currentThread().getId()+"> alpha: "+alpha);
            logger.info (Thread.currentThread().getName()+"<"+Thread.currentThread().getId()+"> beta: "+beta);
        }


        logger.info (Thread.currentThread().getName()+"<"+Thread.currentThread().getId()+"> Result-size: "+(result==null?0:result.size()));
        if (result==null || Integer.parseInt(offset)+increment>=result.size()) {
            ArrayList<Float> scores = new ArrayList<>();

            logger.info (Thread.currentThread().getName()+"<"+Thread.currentThread().getId()+"> Requesting for new items: "
                                                +(Integer.parseInt(offset)+1)+" -- "+(Integer.parseInt(offset)+increment));
            try{
            //result = result==null?dp.processGreedy(qcenter,kc,increment,isMinSum(),scores)
            result = result==null?dp.processSwap(qcenter,kc,kc,increment,isMinSum(),scores)
                    :dp.processGreedyIncremental(result,qcenter,result.size()+increment,isMinSum(),scores);
                    //:dp.processGreedySwapIncremental(qcenter,result,kc,increment,isMinSum(),scores);
            }catch(RuntimeException e){
                result = new ArrayList<>();
                qcenter = "";
            }
        }

        StringBuilder webresponse = new StringBuilder ("\n\t\tqcenter = \"" + qcenter + "\";\n\n");

        webresponse.append ("\t\tlinks = [");
        for (String resultItem : result) {
            if (resultItem==null) break;
            webresponse.append ("\"");
            webresponse.append(resultItem);
            webresponse.append ("\",\n\t\t");
        }
        if (!qcenter.isEmpty()){
            webresponse.deleteCharAt (webresponse.length()-1);
            webresponse.deleteCharAt (webresponse.length()-1);
            webresponse.deleteCharAt (webresponse.length()-1);
            webresponse.deleteCharAt (webresponse.length()-1);
        }
        webresponse.append ("];\n\n");

        webresponse.append ("\t\tsimilarities = [");
        for (String resultItem : result) {
            if (resultItem==null) break;
            webresponse.append ((int)(Math.round(100*dp.getContextSimilarity(qcenter,resultItem))));
            webresponse.append (",\n\t\t");
        }
        if (!qcenter.isEmpty()){
            webresponse.deleteCharAt (webresponse.length()-1);
            webresponse.deleteCharAt (webresponse.length()-1);
            webresponse.deleteCharAt (webresponse.length()-1);
            webresponse.deleteCharAt (webresponse.length()-1);
        }
        webresponse.append ("];\n\n");

        webresponse.append ("\t\tnetdistances = [");
        for (String resultItem : result) {
            if (resultItem==null) break;
            webresponse.append (dp.getGraphDistance (qcenter,resultItem));
            webresponse.append (",\n\t\t");
        }
        if (!qcenter.isEmpty()){
            webresponse.deleteCharAt (webresponse.length()-1);
            webresponse.deleteCharAt (webresponse.length()-1);
            webresponse.deleteCharAt (webresponse.length()-1);
            webresponse.deleteCharAt (webresponse.length()-1);
        }
        webresponse.append ("];\n\n");

        webresponse.append ("\t\ttitles = [");
        webresponse.append ("\"");
        webresponse.append (qcenter.isEmpty()?"":dp.getTitle (qcenter));
        webresponse.append ("\",\n\t\t");
        for (String resultItem : result) {
            if (resultItem==null) break;
            webresponse.append ("\"");
            webresponse.append (dp.getTitle (resultItem).replace("'", "").replace("\"", ""));
            webresponse.append ("\",\n\t\t");
        }
        webresponse.deleteCharAt (webresponse.length()-1);
        webresponse.deleteCharAt (webresponse.length()-1);
        webresponse.deleteCharAt (webresponse.length()-1);
        webresponse.deleteCharAt (webresponse.length()-1);
        webresponse.append ("];\n\n");

        webresponse.append ("\t\tparagraphs = [");
        webresponse.append ("\"");
        if (!qcenter.isEmpty()){
            webresponse.append (ProcessWikipedia.getParagraph("/home/gtsat/Documents/"+qcenter).replace("'", "").replace("\"", ""));
        }
        webresponse.append ("\",\n\t\t");
        for (String resultItem : result) {
            if (resultItem==null) break;
            webresponse.append ("\"");
            webresponse.append (ProcessWikipedia.getParagraph("/home/gtsat/Documents/"+resultItem).replace("'","").replace("\"", ""));
            webresponse.append ("\",\n\t\t");
        }
        webresponse.deleteCharAt (webresponse.length()-1);
        webresponse.deleteCharAt (webresponse.length()-1);
        webresponse.deleteCharAt (webresponse.length()-1);
        webresponse.deleteCharAt (webresponse.length()-1);
        webresponse.append ("];\n\n");

        logger.info (Thread.currentThread().getName()+"<"+Thread.currentThread().getId()+"> RESPONSE:\n"+webresponse.toString());

        return webresponse.toString();
    }


    public static void main (String[] args) {
        WebClient client = new WebClient("/tmp/edgelist.filtered.txt",
                                         "/tmp/model.txt",
                                         "/tmp/titles.txt");

        client.red = "0";
        client.green = "0";
        client.blue = "255";

        client.query = "unix";
        System.out.println (client.query(1,3)+"--------------");
        System.out.println (client.query(1,3)+"--------------");
        System.out.println (client.query(1,3));
        client.reset();
    }
}

