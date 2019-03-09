package webroadnet;

import java.io.*;
import java.net.*;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.text.NumberFormat;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import topk.ComputeClusteredGroupings;
import topk.ComputeExtensiveGroupings;
import topk.ComputeHeuristicGroupings;
import shortestpath.ScatterMap;
import shortestpath.Point2D;
import grammar.DirectedGraph;
import grammar.Graph;
import grammar.Edge;
import oscp.Group;


public final class WebRoadNetOMCP {

	static {
		System.setProperty("file.encoding", "UTF-8");
		System.setProperty("encoding", "UTF-8");
		System.setProperty("user.language", "el_gr.UTF-8");
		System.setProperty("user.country", "el_gr.UTF-8");
		System.setProperty("sun.jnu.encoding", "UTF8");
	}

	private static final Logger logger = Logger.getLogger (WebRoadNetOMCP.class);

	private static Graph<Long> newyorkGraph=null;//=new DirectedGraph<Long>("roadnets/USA-road-d.NY.gr",NumberFormat.getNumberInstance(),true);
	private static Graph<Long> sanfranciscoGraph=null;//=new DirectedGraph<Long>("roadnets/USA-road-d.BAY.gr",NumberFormat.getNumberInstance(),true);
	private static Graph<Long> washingtonGraph=null;//=new DirectedGraph<Long>("roadnets/USA-road-d.NW.gr",NumberFormat.getNumberInstance(),true);

	private static ScatterMap<Long> newyorkMap=null;//=new ScatterMap<Long>("roadnets/USA-road-d.NY.coord",NumberFormat.getNumberInstance());
	private static ScatterMap<Long> sanfranciscoMap=null;//=new ScatterMap<Long>("roadnets/USA-road-d.BAY.coord",NumberFormat.getNumberInstance());
	private static ScatterMap<Long> washingtonMap=null;//=new ScatterMap<Long>("roadnets/USA-road-d.NW.co",NumberFormat.getNumberInstance());

	private Graph<Long> graph;
	private ScatterMap<Long> map;

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

	private String procoption;
	public void setProcoption (String option) {procoption = option;}
	public String getProcoption () {return procoption;}
	public String getCheckProcoption (String _option) {return procoption.equalsIgnoreCase(_option)?"CHECKED":"";}
	public boolean isGreedy () {return procoption.equalsIgnoreCase("greedy");}

	private String rankoption;
	public void setRankoption (String option) {rankoption = option;}
	public String getRankoption () {return rankoption;}
	public String getCheckRankoption (String _option) {return rankoption.equalsIgnoreCase(_option)?"CHECKED":"";}
	private boolean isMinSum () {return rankoption.equalsIgnoreCase("minsum");}

	private String mapname;
	public void setMapname (String _mapname) {mapname = _mapname;}
	public String getMapname () {return mapname;}

	private String maxmeetups;
	public void setMaxmeetups (String meetups) {maxmeetups = meetups;}
	public String getMaxmeetups () {return maxmeetups;}

	private ArrayList<Float> Qlat ;
	private ArrayList<Float> Qlng ;

	private Long[] travelers;
	private Long target;

	private String resultsize;
	public String getResultsize () {return resultsize;}
	public void setResultsize (String _resultsize) {resultsize=_resultsize;}

	private String loadedmap;

	public WebRoadNetOMCP () {
		lats = ""; 
		lngs = "";
		ratings = "";
		mapname = "";
		loadedmap = "";
		destination = "";
		predestination = "";
		resultsize = "5";
		maxmeetups = "1";
		procoption = "greedy";
		rankoption = "minsum";
		Qlat = new ArrayList<Float>();
		Qlng = new ArrayList<Float>();
	}

        public synchronized void loadWashington() {
                if (loadedmap.compareTo("washington")!=0) {
                        lats = "";
                        lngs = "";
                        ratings = "";
                        destination = "";
                        predestination = "";

                        if (washingtonGraph==null) 
                        washingtonGraph = new DirectedGraph<Long>("roadnets/USA-road-d.NW.gr",NumberFormat.getNumberInstance(),true);
                        if (washingtonMap==null)
                        washingtonMap = new ScatterMap<Long>("roadnets/USA-road-d.NW.co",NumberFormat.getNumberInstance());

                        graph = washingtonGraph;
                        map = washingtonMap;

                        loadedmap = "washington";
                        mapname = "washington";
                }
        }

	public synchronized void loadSanFrancisco() {
		if (loadedmap.compareTo("francisco")!=0) {
			lats = ""; 
			lngs = "";
			ratings = "";
			destination = "";
			predestination = "";

                        if (sanfranciscoGraph==null)
			sanfranciscoGraph = new DirectedGraph<Long>("roadnets/USA-road-d.BAY.gr",NumberFormat.getNumberInstance(),true);
                        if (sanfranciscoMap==null)
			sanfranciscoMap = new ScatterMap<Long>("roadnets/USA-road-d.BAY.co",NumberFormat.getNumberInstance());
			graph = sanfranciscoGraph;
			map = sanfranciscoMap;

			loadedmap = "francisco";
			mapname = "francisco";
		}
	}

	public synchronized void loadNewYork() {
		if (loadedmap.compareTo("newyork")!=0) {
			lats = ""; 
			lngs = "";
			ratings = "";
			destination = "";
			predestination = "";

			if (newyorkGraph==null)
				newyorkGraph = new DirectedGraph<Long>("roadnets/USA-road-d.NY.gr",NumberFormat.getNumberInstance(),true);
			if (newyorkMap==null)
				newyorkMap = new ScatterMap<Long>("roadnets/USA-road-d.NY.co",NumberFormat.getNumberInstance());

			graph = newyorkGraph;
			map = newyorkMap;

			loadedmap = "newyork";
			mapname = "newyork";
		}
	}

	public String printStatistics () {return map.getVertices().size()+"\t"+graph.numberNodes()+"\t"+graph.numberEdges()+"\n";}

	public String printOMCPs () {
		String datastring = "\n\t\t\t locations = new Array();\n" ;
                datastring += "\t\t\t paths = new Array();\n";
                datastring += "\t\t\t colors = new Array();\n";
                datastring += "\t\t\t locsize = new Array();\n";
                datastring += "\t\t\t plansize = new Array();\n\n";

		///if (destination.compareTo("")==0 && predestination.compareTo("")==0) return datastring;
		///if (travelers==null || target==null || travelers.length==0) return datastring;
		if (destination.compareTo("")==0 && predestination.compareTo("")==0) target=null;
		if (travelers==null || travelers.length==0) return datastring;

		int k = tryParseInt (resultsize);
		if (k<0) {k=10;resultsize="10";}
		if (k>100) {k=10;resultsize="10";}

		int M = tryParseInt (maxmeetups);
		if (M<1) {M=1;maxmeetups="1";}
		if (M>=travelers.length) {M=travelers.length-1;maxmeetups=new Integer(travelers.length-1).toString();}
/**
		Group<Long> optrouteplan = isGreedy()?
					ComputeHeuristicGrouping.compute(new Group<Long>(target,travelers,getLambdas(),graph,isMinSum()),graph,isMinSum())
					:ComputeOptimalGrouping.compute(new Group<Long>(target,travelers,getLambdas(),graph,isMinSum()),graph,isMinSum());
					//:ComputeExtensiveGrouping.compute(new Group<Long>(target,travelers,getLambdas(),graph,isMinSum()),graph,isMinSum());

                ArrayList<Group<Long>> routeplans = new ArrayList<Group<Long>>();
                routeplans.add(optrouteplan);
**/
/**/
		ArrayList<Group<Long>> routeplans = isGreedy()?
					ComputeHeuristicGroupings.compute(new Group<Long>(target,travelers,getLambdas(),graph,isMinSum()),graph,k,M,isMinSum())
					//:ComputeClusteredGroupings.compute(new Group<Long>(target,travelers,getLambdas(),graph,isMinSum()),graph,k,M,isMinSum());
					:ComputeExtensiveGroupings.compute(new Group<Long>(target,travelers,getLambdas(),graph,isMinSum()),graph,k,M,isMinSum());
/**/
		for (Group<Long> routeplan : routeplans) {
			if (routeplan.getTarget()==null) throw new RuntimeException ("\n!! ERROR - Destination node should not be null !!");

			ArrayDeque<Edge<Long>> routes = new ArrayDeque<>();
			formRoutePlan (routeplan,routes);

			datastring += "\n\t\t\t plansize.push("+routes.size()+");\n";

			int color = 0;
			int step = 100/routes.size();
			for (int i=0; i<routes.size(); ++i) {
				int r = (255*color)/100;
				int b = (255*(100-color))/100;
				String rhex = Integer.toHexString(r);
				String bhex = Integer.toHexString(b);
				if (rhex.length()<2) rhex="0"+rhex;
				if (bhex.length()<2) bhex="0"+bhex;
				color += step;
				datastring += "\t\t\t colors.push('#"+rhex+"00"+bhex+"');\n";
			}

			int locationcount = 0;
			if (routeplan.getSubgroups().size()>1) {
				Point2D post = map.getSpatialPosition(routeplan.getTarget());
				datastring += "\t\t\t locations.push(L.latLng("+post.x()+","+post.y()+"));\n";
				++locationcount;
			}
			for (Edge<Long> route : routes) {
				if (!routeplan.getTarget().equals(route.to)) {
					Point2D post = map.getSpatialPosition(route.to);
					datastring += "\t\t\t locations.push(L.latLng("+post.x()+","+post.y()+"));\n";
					++locationcount;
				}
				datastring += "\t\t\t paths.push([";
				ArrayDeque<Long> path = graph.getPath(route.from,route.to);
				for (Long vertex : path) {
					Point2D point = map.getSpatialPosition(vertex);
					datastring += "L.latLng(" + point.x() + "," + point.y() + ")";
					if (!vertex.equals(route.to)) datastring += ",";
				}
				datastring += "]);\n";
			}
			datastring += "\t\t\t locsize.push("+locationcount+");\n";
		}
		return datastring;
	}

        private void formRoutePlan (Group<Long> group, ArrayDeque<Edge<Long>> routes) {
                for (Group<Long> subgroup : group.getSubgroups()) {
                        routes.add (new Edge<Long>(subgroup.getTarget(),group.getTarget(),0.0f));
                        formRoutePlan (subgroup,routes);
                }
        }

	private int tryParseInt (String value) {
		try{return Integer.parseInt(value);}
		catch(NumberFormatException nfe) {return 0;}
	}

	private float[][] getLambdas () {
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
                        //if (x==y && lambdas[x][y]==0.0f) lambdas[x][y] = .1;
                        if (lambdas[x][y]==0.0f) lambdas[x][y] = .1f;

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
		return lambdas;
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
				dataString += "\t\t\tclicklocations.push(new Array(L.latLng("+vertex.x()+","+vertex.y()+"),\"\",\"\"));\n";
				//dataString += "\t\t\tclicklocations.push(new Array(L.latLng("+lat+","+lng+"),\"\",\"\"));\n";
				travelers [j] = vertices.get(0);
			}
		return dataString;
	}

	public String printTextQueries () throws IOException, SAXException {
		String dataString = "var textlocations = new Array();\n\n" ;
		if (predestination.length()>0) retrieveTextQueries (predestination,false);
		else if (destination.length()>0) retrieveTextQueries (destination,false);
		else return dataString;

		int size = Math.min (Qlat.size(), Qlng.size());
		for (int j=0;j<size;++j) {
			float lat = Qlat.get(j);
			float lng = Qlng.get(j);
			ArrayList<Long> vertices = map.getNearestVertices(new Point2D(lat,lng),1);
			if (vertices.isEmpty()) continue;
			target = vertices.get(0);
			Point2D targetco = map.getSpatialPosition(target);
			dataString += "\t\t\ttextlocations.push(new Array(L.latLng("+targetco.x()+","+targetco.y()+"),\"\",\"\"));\n";
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
		}catch(ParserConfigurationException e) {e.printStackTrace(System.err);}

		String xmlString = processAddress(address);
		logger.info("Geocoded result for '"+address+"':\n"+xmlString);
		MySAXHandler handler = new MySAXHandler(); 
		parser.parse(new InputSource(new StringReader(xmlString)),handler);

		ArrayList<Float> data = handler.getData();
		Qlat.clear(); Qlng.clear();
		Qlat.add(data.get(0)); Qlng.add(data.get(1));
	}

	public String processAddress () throws IOException {return processAddress(this.destination);}
	private String processAddress (String destination) throws IOException {
		//URL google = new URL("http://maps.googleapis.com/maps/api/geocode/xml?address="+destination.replaceAll(" ","+")+"&sensor=false");
		//URL url = new URL("https://nominatim.openstreetmap.org/search?q="+destination.replaceAll(" ","+")+"&format=xml");
		//URL url = new URL("https://nominatim.openstreetmap.org/search?q="+destination.replaceAll(" ","+")+"&format=xml");
		URL url = new URL("http://www.mapquestapi.com/geocoding/v1/address?key=lYrP4vF3Uk5zgTiGGuEzQGwGIVDGuy24&outFormat=xml&location="+destination.replaceAll(" ","+"));
		URLConnection conn = url.openConnection();
		BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		StringBuilder serverResponse = new StringBuilder();
		for (String inputLine; (inputLine = in.readLine()) != null; serverResponse.append(inputLine)) 
			;
		in.close();
		return serverResponse.toString();
	}

	public String associateAddresses (String filename) throws IOException, SAXException, InterruptedException {
		ArrayList<String> destinationes = readAddresses (filename) ;
		Qlat.clear(); Qlng.clear();
		String pinpoint = "";
		for (String destination : destinationes) {
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

	private ArrayList<String> readAddresses (String filename) throws IOException {
		FileInputStream fstream = new FileInputStream(filename);
		DataInputStream in = new DataInputStream(fstream);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		ArrayList<String> allAddresses = new ArrayList<String>();
		String strLine = "";
		while ((strLine = br.readLine()) != null)
			allAddresses . add (strLine) ;
		in.close();
		return allAddresses;
	}
}


class MySAXHandler extends DefaultHandler {

	private ArrayList<Float> data ;

	String value ;
	private String prefix ;

	MySAXHandler() {
		value = "";
		prefix = "    " ;
		data = new ArrayList<Float>();
	}

	public ArrayList<Float> getData () {return data;}

	public void startDocument() {
		//System.err.println("START DOCUMENT ");
	}

	public void endDocument ()  {
		//System.err.println("END DOCUMENT ");
	}

	public void startElement (String uri, String localName, String qname, Attributes attr) {
		System.err.println(prefix + "START ELEMENT '" + localName + "'");
		int attrCount = attr.getLength();
		if(attrCount>0) {
			//System.out.println("Attributes:");
			for(int i = 0 ; i<attrCount ; i++) {/*
				System.err.println("  Name : " + attr.getQName(i)); 
				System.err.println("  Type : " + attr.getType(i)); 
				System.err.println("  Value: " + attr.getValue(i));*/
				if (attr.getQName(i).compareTo("lat")==0 || attr.getQName(i).compareTo("lng")==0)
					data.add(Float.parseFloat(attr.getValue(i)));
			}
		}
		prefix += "    ";
	}

	public void endElement (String uri, String localName, String qname) {
		prefix = prefix.substring(4);
		if (localName.compareTo("lat")==0 || localName.compareTo("lng")==0)
			data.add(Float.parseFloat(value));
		System.err.println(prefix + "END ELEMENT '" + localName + "'");
		////System.err.println(prefix + "END ELEMENT local name: '" + localName + "', qname: '" + qname + "'.");
	}
	
	public void characters (char[] ch, int start, int length) {
		value = new String(ch, start, length);
		//System.err.println (prefix + "Characters: '" + value + "'");
	}
	
	public void ignorableWhitespace (char[] ch, int start, int length) {
		//System.err.println (prefix + "Ignorable whitespace: '" + new String(ch, start, length) + "'");
	}
	
	public void startPrefixMapping (String prefix, String uri) {
		//System.err.println (prefix + "Start '" + prefix + "' namespace scope."); 
	}
	
	public void endPrefixMapping (String prefix) {
		//System.err.println(prefix + "End '" + prefix + "' namespace scope."); 
	}
	
	public void warning (SAXParseException spe) {
		System.err.println("Warning at line "+spe.getLineNumber());
		System.err.println(spe.getMessage());
	}
	
	public void fatalError(SAXParseException spe) throws SAXException {
		System.err.println("Fatal error at line "+spe.getLineNumber());
		System.err.println(spe.getMessage());
		throw spe;
	}
}

