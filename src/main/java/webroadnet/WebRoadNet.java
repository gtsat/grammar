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

package webroadnet;

import java.io.*;
import java.net.*;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.ArrayDeque;
import java.text.NumberFormat;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import setbased.ComputeMeetingLocation;
import shortestpath.ScatterMap;
import shortestpath.Point2D;
import grammar.DirectedGraph;
import grammar.Graph;
import oscp.Group;

public final class WebRoadNet {

	private Graph<Long> graph;
	private ScatterMap<Long> map;
        //private Graph<Long> graph = new UndirectedGraph<Long>("roadnets/USA-road-d.NW.gr",NumberFormat.getNumberInstance(),true);
        //private Graph<Long> graph = new UndirectedGraph<Long>("roadnets/manhattan.gr",NumberFormat.getNumberInstance(),true);
        //private Graph<Long> graph = new UndirectedGraph<Long>("roadnets/USA-road-d.NY.gr.half",NumberFormat.getNumberInstance(),true);
        //private Graph<Long> graph = new UndirectedGraph<Long>("roadnets/USA-road-d.NY.gr",NumberFormat.getNumberInstance(),true);
        //private ScatterMap<Long> map = new ScatterMap<Long>("roadnets/USA-road-d.NY.coord",NumberFormat.getNumberInstance());

	private String predestination;
	public void setPredestination (String _predestination) {predestination = _predestination;}
	public String getPredestination () {return predestination;}
	public String getSelectedPredestination (String _option) {return predestination.compareTo(_option)==0?"SELECTED":"";}

	private String destination;
	public void setDestination (String _destination) {destination = _destination;}
	public String getDestination () {return destination;}

	private String ratings;
	public void setRatings (String _ratings) {ratings = _ratings;}
	public String getRatings () {return ratings;}

	private String lats;
	public void setLats (String _lats) {lats = _lats;}
	public String getLats () {return lats;}

	private String lngs;
	public void setLngs (String _lngs) {lngs = _lngs;}
	public String getLngs () {return lngs;}

	private String option;
	public void setOption (String _option) {option = _option;}
	public String getOption () {return option;}
	public String getCheckOption (String _option) {return option.compareTo(_option)==0?"CHECKED":"";}

	private String mapname;
	public void setMapname (String _mapname) {mapname = _mapname;}
	public String getMapname () {return mapname;}

	final private ArrayList<Float> Qlat ;
	final private ArrayList<Float> Qlng ;
	final private ArrayList<Float> SWlat ;
	final private ArrayList<Float> SWlng ;
	final private ArrayList<Float> NElat ;
	final private ArrayList<Float> NElng ;

	private Long[] travelers;
	private Long target;

	private String resultsize;
	public String getResultsize () {return resultsize;}
	public void setResultsize (String _resultsize) {resultsize=_resultsize;}

	private String loadedmap;

	public WebRoadNet () {
		lats = ""; 
		lngs = "";
		ratings = "";
		mapname = "";
		destination = "";
		predestination = "";
		resultsize = "10";
		option = "minsum";
		loadedmap = "";
		Qlat = new ArrayList<>();
		Qlng = new ArrayList<>();
		SWlat = new ArrayList<>();
		SWlng = new ArrayList<>();
		NElat = new ArrayList<>();
		NElng = new ArrayList<>();
	}

	public void loadSanFrancisco() {
		if (loadedmap.compareTo("francisco")!=0) {
			lats = ""; 
			lngs = "";
			ratings = "";
			mapname = "";
			destination = "";
			predestination = "";
			resultsize = "10";
			option = "minsum";
			graph = new DirectedGraph<>("roadnets/USA-road-d.BAY.gr",NumberFormat.getNumberInstance(),true);
			map = new ScatterMap<>("roadnets/USA-road-d.BAY.coord",NumberFormat.getNumberInstance());
			loadedmap = "francisco";
		}
	}

	public void loadNewYork() {
		if (mapname.compareTo("newyork")!=0) {
			lats = ""; 
			lngs = "";
			ratings = "";
			mapname = "";
			destination = "";
			predestination = "";
			resultsize = "10";
			option = "minsum";
			graph = new DirectedGraph<>("roadnets/USA-road-d.NY.gr",NumberFormat.getNumberInstance(),true);
			map = new ScatterMap<>("roadnets/USA-road-d.NY.coord",NumberFormat.getNumberInstance());
			loadedmap = "newyork";
		}
	}

        public void loadWashington() {
                if (mapname.compareTo("washington")!=0) {
                        lats = "";
                        lngs = "";
                        ratings = "";
                        mapname = "";
                        destination = "";
                        predestination = "";
                        resultsize = "10";
                        option = "minsum";
                        graph = new DirectedGraph<>("roadnets/USA-road-d.NW.gr",NumberFormat.getNumberInstance(),true);
                        //graph = new DirectedGraph<Long>("roadnets/seattle.graph",NumberFormat.getNumberInstance(),true);
                        map = new ScatterMap<>("roadnets/USA-road-d.NW.co",NumberFormat.getNumberInstance());
                        //map = new ScatterMap<Long>("roadnets/seattle.cnode",NumberFormat.getNumberInstance());
                        loadedmap = "washington";
                }
        }

	public String printStatistics () {return map.getVertices().size()+"\t"+graph.numberNodes()+"\t"+graph.numberEdges()+"\n";}

        public String printMeetingLocation () {
		String dataString = "\n\t\t\t locations = new Array();\n" ;
                dataString += "\t\t\t paths = new Array();\n";
                dataString += "\t\t\t colors = new Array();\n\n";

                int color = 1;
                int step = 100/travelers.length;
                for (Long traveler : travelers) {
                    int r = (255*color)/100;
                    int g = (255*(100-color))/100;
                    String ghex = Integer.toHexString(r);
                    String rhex = Integer.toHexString(g);
                    if (ghex.length()<2) ghex="0"+ghex;
                    if (rhex.length()<2) rhex="0"+rhex;
                    color += step;
                    dataString += "\t\t\tcolors.push('#"+rhex+ghex+"00');\n";
                }
                dataString += "\t\t\tcolors.push('#0000ff');\n";

		if (travelers==null || target==null) return dataString;
		for (Long traveler : travelers)
			if (traveler==null)
				return dataString; //throw new IllegalArgumentException("!! ERROR - Invalid null traveler !!");

		float[][] lambdas = new float [travelers.length][travelers.length];
		for (int i=0;i<travelers.length;++i)
			for (int j=0;j<travelers.length;++j)
				lambdas[i][j]=5;

		String[] ratsArr = ratings.split(" ");
		for (int i=0; i<ratsArr.length;) {
			if (i+1>=ratsArr.length) break;
			String[] pos = ratsArr[i++].split("x");
			int x = Integer.parseInt(pos[0]);
			int y = Integer.parseInt(pos[1]);
			lambdas[x][y] -= Integer.parseInt(ratsArr[i++]);
			if (x==y && lambdas[x][y]==0.0f) lambdas[x][y] = .1f;

			System.out.println(lambdas[x][y]+"@"+x+","+y);
		}

		for (int i=0;i<travelers.length;++i) {
			float sumline = 0.0f;
			for (int j=0;j<travelers.length;++j)
				sumline += lambdas[i][j];

                        if (sumline>0)
                                for (int j=0;j<travelers.length;++j)
                                        lambdas[i][j] /= sumline;
                        else
                                for (int j=0;j<travelers.length;++j)
                                        lambdas[i][j] = 1.0f/travelers.length;

		}

		int k = tryParseInt (resultsize);
		if (k<=0) {k = 10; resultsize = "10";}
		if (k>100) {k = 100; resultsize = "100";}

		LinkedList<Long> results;
		if (travelers.length==1) {
			results = new LinkedList<>();
			results.add(target);
		}else results = ComputeMeetingLocation.computeOMPfiltered(new Group<Long>(target,travelers,lambdas,
						graph,getOption().compareTo("minsum")==0),
						graph,getOption().compareTo("minsum")==0,k);

                if (!results.isEmpty()) {
			for (Long mu : results) {
				Point2D vertex = map.getSpatialPosition (mu);
				dataString += "\n\t\t\tlocations.push(new google.maps.LatLng("+vertex.x()+","+vertex.y()+"));\n\n";

				for (Long traveler : travelers) {
					dataString += "\t\t\tpaths.push([";
					for (Long post : graph.getPath(traveler,mu)) {
						vertex = map.getSpatialPosition(post);
						dataString += "new google.maps.LatLng("+vertex.x()+","+vertex.y()+")";
						if (!post.equals(mu)) dataString += ",";
					}
					dataString += "]);\n";
				}

				dataString += "\t\t\tpaths.push([";
				for (Long post : graph.getPath(mu,target)) {
					vertex = map.getSpatialPosition(post);
					dataString += "new google.maps.LatLng("+vertex.x()+","+vertex.y()+")";
					if (!post.equals(target)) dataString += ",";
				}
				dataString += "])\n";
			}
		}
		return dataString;
	}

	private int tryParseInt (String value) { 
		try{return Integer.parseInt(value);}
		catch(NumberFormatException nfe) {return 0;}  
	}

	public String printRatingsInterface () {
		int counter = 0;
		int size = Math.min (lats.split(" ").length,lngs.split(" ").length);
		StringBuilder rats = new StringBuilder();
		rats.append("\t<table style='width:"+(size*100)+"px'>\n\n");
		rats.append("\t\t<tr>\n\n");
		rats.append("\t\t<td></td>\n");
		for (int i=0;i<size;++i)
			rats.append("\t\t<td><span style='display:inline-block; font-weight: bold; width: 100px;'>User "+(char)('A'+i)+"</span></td>\n");
		rats.append("\t</tr>\n\n");
		for (int i=0;i<size;++i) {
			rats.append("\t\t<tr>\n\t\t<td>\n\t\t<span style='display:inline-block; font-weight: bold; width: 100px;'>User "+(char)('A'+i)+"</span>\n\t\t</td>");
			for (int j=0;j<size;++j) {
				++counter;
				rats.append("\t\t<td>\n\t\t<div class='rating'>\n");
				rats.append("\t\t\t<span><input type='radio' name='"+i+"x"+j+"' id='str5' value='5'><label for='str5'></label></span>\n");
				rats.append("\t\t\t<span><input type='radio' name='"+i+"x"+j+"' id='str4' value='4'><label for='str4'></label></span>\n");
				rats.append("\t\t\t<span><input type='radio' name='"+i+"x"+j+"' id='str3' value='3'><label for='str3'></label></span>\n");
				rats.append("\t\t\t<span><input type='radio' name='"+i+"x"+j+"' id='str2' value='2'><label for='str2'></label></span>\n");
				rats.append("\t\t\t<span><input type='radio' name='"+i+"x"+j+"' id='str1' value='1'><label for='str1'></label></span>\n");
				rats.append("\t\t</div>\n\t\t</td>\n");
			}
			rats.append("\t\t</tr>\n\t\t\n\n");
		}
		rats.append("\t</table>\n");
		return rats.toString();
	}

	public String printClickedQueries () {
		String[] processedLats = lats.split(" ");
		String[] processedLngs = lngs.split(" ");
		int size = Math.min (processedLats.length, processedLngs.length);
		String dataString = "var clicklocations = new Array();\n\n" ;
                travelers = new Long [size];
		if (lats.length()>0 || lngs.length()>0)
			for (int j=0;j<size;++j) {
				float lat = Float.parseFloat(processedLats[j]);
				float lng = Float.parseFloat(processedLngs[j]);
                                ArrayList<Long> vertices = map.getNearestVertices(new Point2D(lat,lng),1);
                                if (vertices.isEmpty()) continue;
                                Point2D vertex = map.getSpatialPosition(vertices.get(0));
				dataString += "\t\t\tclicklocations.push(new Array(new google.maps.LatLng("+vertex.x()+","+vertex.y()+"),\"\",\"\"));\n";
				//dataString += "\t\t\tclicklocations.push(new Array(new google.maps.LatLng("+lat+","+lng+"),\"\",\"\"));\n";
                                travelers [j] = vertices.get(0);
			}
		return dataString;
	}

	public String printTextQueries () throws IOException, SAXException {
		String dataString = "var textlocations = new Array();\n\n" ;
		if (predestination.length()>0) retrieveTextQueries (predestination,false);
		else if (destination.length()>0) retrieveTextQueries (destination,false);

		int size = Math.min (Qlat.size(), Qlng.size());
		for (int j=0;j<size;++j) {
			float lat = Qlat.get(j);
			float lng = Qlng.get(j);
			ArrayList<Long> vertices = map.getNearestVertices(new Point2D(lat,lng),1);
			if (vertices.isEmpty()) continue;
			target = vertices.get(0);
			Point2D targetco = map.getSpatialPosition(target);
			dataString += "\t\t\ttextlocations.push(new Array(new google.maps.LatLng("+targetco.x()+","+targetco.y()+"),\"\",\"\"));\n";
		}
		Qlat.clear();
		Qlng.clear();
		return dataString;
	}

	private void retrieveTextQueries (String address,boolean useBox) throws IOException, SAXException {
		SAXParserFactory spf = SAXParserFactory.newInstance();
		SAXParser parser = null;
		spf.setNamespaceAware(true);
		spf.setValidating(true);
		try{
			spf.setFeature("http://xml.org/sax/features/namespace-prefixes",true);
			parser = spf.newSAXParser();
		}catch(SAXException e) {
			e.printStackTrace(System.err);
		}catch(ParserConfigurationException e) {
                        e.printStackTrace(System.err);
		}

		String xmlString = processGoogleAddress(address);
		MySAXHandler handler = new MySAXHandler(); 
		parser.parse(new InputSource(new StringReader(xmlString)),handler);
		
		ArrayList<Float> data = handler.getData();
		Qlat.clear(); Qlng.clear();
		SWlat.clear(); SWlng.clear();
		NElat.clear(); NElng.clear();
		for (int j=0;j<data.size(); j+=10) {
			Qlat.add(data.get(j)); Qlng.add(data.get(j+1));
			if (useBox) {
				SWlat.add(data.get(j+2)); SWlng.add(data.get(j+3));
				NElat.add(data.get(j+4)); NElng.add(data.get(j+5));
			}
		}
	}

	public String processGoogleAddress () throws IOException {return processGoogleAddress (this.destination);}
	private String processGoogleAddress (String destination) throws IOException {
		URL google = new URL("http://maps.googleapis.com/maps/api/geocode/xml?address="+destination.replaceAll(" ","+")+"&sensor=false");
		URLConnection googleConn = google.openConnection();
		BufferedReader in = new BufferedReader(new InputStreamReader(googleConn.getInputStream()));
		StringBuilder serverResponse = new StringBuilder();
		for (String inputLine; (inputLine = in.readLine()) != null; serverResponse.append(inputLine)) 
			;
		in.close();
		return serverResponse.toString();
	}

	public String associateAddresses (String filename) throws IOException, SAXException, InterruptedException {
		ArrayDeque<String> destinations = readAddresses (filename) ;
		Qlat.clear(); Qlng.clear();
		String pinpoint = "";
		for (String destination : destinations) {
			System.out.println (destination + ": ");

			this.destination = destination ;
			retrieveTextQueries (destination,false);
			Thread.sleep(4000);
			if (!Qlat.isEmpty() && !Qlng.isEmpty()) {
				System.out.println ( Qlat.get(0).toString() + ", " + Qlng.get(0).toString() );
				//pinpoint += Qlat.get(0).toString() + ", " + Qlng.get(0).toString() + "\n";
			}
		}
		return pinpoint;
	}

	private ArrayDeque<String> readAddresses (String filename) throws IOException {
		FileInputStream fstream = new FileInputStream(filename);
		DataInputStream in = new DataInputStream(fstream);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		ArrayDeque<String> allAddresses = new ArrayDeque<>();
		String strLine = "";
		while ((strLine = br.readLine()) != null)
			allAddresses . add (strLine) ;
		in.close();
		return allAddresses;
	}
}
