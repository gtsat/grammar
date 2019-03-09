<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1//EN" "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd">
<%@page contentType="text/html;charset=UTF-8"%>
<%@page language="java" import="webroadnet.WebRoadNetOMCP" %>
<jsp:useBean id="roadnet" class="webroadnet.WebRoadNetOMCP" scope="application"/>
<jsp:setProperty name="roadnet" property="*"/>
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="el">
<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
<title>RoadNet - k Optimal Connecting Points</title>
<link rel="shortcut icon" href="jsp/omcp/images/favicon.ico" type="image/x-icon">

<link href='http://leafletjs.com/docs/css/normalize.css' rel='stylesheet' type='text/css' />
<link href='http://leafletjs.com/docs/css/main.css' rel='stylesheet' type='text/css' />
<link rel="stylesheet" href="https://unpkg.com/leaflet@1.3.4/dist/leaflet.css" integrity="sha512-puBpdR0798OZvTTbP4A8Ix/l+A4dHDD0DGqYW6RQ+9jxkRFclaxxQb/SJAWZfWAkuyeQUytO7+7N4QKrDh+drA==" crossorigin=""/>
<script src="https://unpkg.com/leaflet@1.3.4/dist/leaflet.js" integrity="sha512-nMMmRyTVoLYqjP9hrbed9S+FzjZHW5gY1TWCHA5ckwXZBadntCNs8kEqAWdrb9O7rxbCaA4lKTIWjDXZxflOcA==" crossorigin=""></script>

<style type="text/css">
	html, body {width: 100%; height: 100%}
	body {margin-top: 0px; margin-right: 0px; margin-left: 0px; margin-bottom: 0px}
	.map {height: 100%; width: 70%; margin: 0px; right: 0%; position: absolute}
</style>

<script type="text/javascript">
	child_open = function () {
		pop1 = window.open('affinity',"_blank","directories=no,status=no,menubar=no,scrollbars=yes,resizable=yes,width=20,height=20");
	};
	function test(v) {
		document.getElementById("clickRats").value = v;
		var m = document.getElementById('clickForm1');
		m.submit();
	};
</script>

</head>

<body>
<div id='map' class='map'></div>
<DIV ALIGN="left" STYLE="top: 0% ; left: 8% ; position: absolute; padding: 0 0 0 0 ; margin: 0 0 0 0; ">
	<a href="http://homegrownresearch.com"><IMG SRC="jsp/omcp/images/homegrown_small.png" onload="this.width*=.7;"/></a>
</DIV>
<DIV ID="panel" STYLE="bottom: 2% ; left: 0% ; position: absolute ; padding: 5px ;">
	<FORM ID="clickForm1" METHOD=POST>
		<DIV ALIGN="left">
			<SPAN STYLE="display:inline-block; font-weight: bold; width: 150px;">Select map</SPAN>
			<SELECT ID="mapname" NAME="mapname" onchange="changemap()">
				<OPTION VALUE="newyork">New York</option>
				<OPTION VALUE="francisco">San Francisco</option>
				<OPTION VALUE="washington" SELECTED>Seattle</option>
			</SELECT>
		</DIV><br>

		<DIV ALIGN="left">
			<SPAN STYLE="display:inline-block; font-weight: bold; width: 150px;">Select POI</SPAN>
			<SELECT ID="predestination" NAME="predestination">
				<OPTION VALUE="">(POINTS LIST)</option>
				<OPTION VALUE="Burke Museum of Natural History and Culture, Seattle, Washington" <%=roadnet.getSelectedPredestination("Burke Museum of Natural History and Culture, Seattle, Washington")%>>Burke Museum</option>
				<OPTION VALUE="Ballard Locks, Seattle, Washington" <%=roadnet.getSelectedPredestination("Ballard Locks, Seattle, Washington")%>>Ballard Locks</option>
				<OPTION VALUE="Boeing tour, Seattle, Washington" <%=roadnet.getSelectedPredestination("Boeing tour, Seattle, Washington")%>>Boeing tour</option>
				<OPTION VALUE="Chihuly Garden and Glass, Seattle, Washington" <%=roadnet.getSelectedPredestination("Chihuly Garden and Glass, Seattle, Washington")%>>Chihuly Garden and Glass</option>
				<OPTION VALUE="Chinatown International District, Seattle, Washington" <%=roadnet.getSelectedPredestination("Chinatown International District, Seattle, Washington")%>>Chinatown District</option>
				<OPTION VALUE="Downtown waterfront, Seattle, Washington" <%=roadnet.getSelectedPredestination("Downtown waterfront, Seattle, Washington")%>>Downtown waterfront</option>
				<OPTION VALUE="EMP Museum, Seattle, Washington" <%=roadnet.getSelectedPredestination("EMP Museum, Seattle, Washington")%>>EMP Museum</option>
				<OPTION VALUE="Henry Art Gallery, Seattle, Washington" <%=roadnet.getSelectedPredestination("Henry Art Gallery, Seattle, Washington")%>>Henry Art Gallery</option>
				<OPTION VALUE="Klondike Goldrush National Historic Park, Seattle, Washington" <%=roadnet.getSelectedPredestination("Klondike Goldrush National Historic Park, Seattle, Washington")%>>Klondike Goldrush Historic Park</option>
				<OPTION VALUE="Museum of­History and Industry, Seattle, Washington" <%=roadnet.getSelectedPredestination("Museum of History and Industry, Seatttle, Washington")%>>Museum of­History and Industry </option>
				<OPTION VALUE="Museum of Flight, Seattle, Washington" <%=roadnet.getSelectedPredestination("Museum of Flight, Seattle, Washington")%>>Museum of Flight</option>
				<OPTION VALUE="Olympic Sculpture Park, Seattle, Washington" <%=roadnet.getSelectedPredestination("Olympic Sculpture Park, Seattle, Washington")%>>Olympic Sculpture Park</option>
				<OPTION VALUE="Pacific Science Center, Seattle, Washington" <%=roadnet.getSelectedPredestination("Pacific Science Center, Seattle, Washington")%>>Pacific Science Center</option>
				<OPTION VALUE="Pike Place Market, Seattle, Washington" <%=roadnet.getSelectedPredestination("Pike Place Market, Seattle, Washington")%>>Pike Place Market</option>
				<OPTION VALUE="Pioneer Square, Seattle, Washington" <%=roadnet.getSelectedPredestination("Pioneer Square, Seattle, Washington")%>>Pioneer Square</option>
				<OPTION VALUE="Seattle Art Museum, Seattle, Washington" <%=roadnet.getSelectedPredestination("Seattle Art Museum, Seattle, Washington")%>>Seattle Art Museum</option>
				<OPTION VALUE="Seattle Aquarium, Seattle, Washington" <%=roadnet.getSelectedPredestination("Seattle Aquarium, Seattle, Washington")%>>Seattle Aquarium</option>
				<OPTION VALUE="Seattle Great Wheel, Seattle, Washington" <%=roadnet.getSelectedPredestination("Seattle Great Wheel, Seattle, Washington")%>>Seattle Great Wheel</option>
				<OPTION VALUE="Smith Tower Observation Deck, Seattle, Washington" <%=roadnet.getSelectedPredestination("Smith Tower Observation Deck, Seattle, Washington")%>>Smith Tower Observation Deck</option>
				<OPTION VALUE="400 Broad St, Seattle, WA 98109" <%=roadnet.getSelectedPredestination("400 Broad St, Seattle, WA 98109")%>>Space Needle</option>
				<OPTION VALUE="Washington State Ferries, Seattle, Washington" <%=roadnet.getSelectedPredestination("Washington State Ferries, Seattle, Washington")%>>Washington State Ferries </option>
				<OPTION VALUE="Woodland Park Zoo, Seattle, Washington" <%=roadnet.getSelectedPredestination("Woodland Park Zoo, Seattle, Washington")%>>Woodland Park Zoo</option>
			</SELECT>
		</DIV>
		<DIV ALIGN="left">
			<SPAN STYLE="display:inline-block; font-weight: bold; width: 150px;">Or enter address</SPAN>
			<INPUT TYPE=TEXT ID=destination NAME=destination SIZE=18><br>
		</DIV><br>
		<DIV ALIGN="left">
			<SPAN STYLE="display:inline-block; font-weight: bold; width: 150px;">Max nr. of meetups</SPAN>
			<INPUT TYPE=TEXT ID=maxmeetups NAME=maxmeetups VALUE=<%= roadnet.getMaxmeetups() %> SIZE=5><br>
		</DIV>

		<DIV ALIGN="left">
			<SPAN STYLE="display:inline-block; font-weight: bold; width: 150px;">Result-size</SPAN>
			<INPUT TYPE=TEXT ID=resultsize NAME=resultsize VALUE=<%= roadnet.getResultsize() %> SIZE=5><br>
		</DIV><br>

		<!--DIV ALIGN="left">
                        <INPUT TYPE="radio" NAME="rankoption" VALUE="minsum" <%=roadnet.getCheckRankoption("minsum")%>>
                        <SPAN STYLE="display:inline-block; width: 200px;">Minimize avg distance</SPAN>
                        <INPUT TYPE="radio" NAME="rankoption" VALUE="minmax" <%=roadnet.getCheckRankoption("minmax")%>>Minimize max distance
                        </DIV-->

		<DIV ALIGN="left">
			<INPUT TYPE="radio" NAME="procoption" VALUE="greedy" <%=roadnet.getCheckProcoption("greedy")%>>
			<SPAN STYLE="display:inline-block; width: 150px;">Heuristic</SPAN>
			<INPUT TYPE="radio" NAME="procoption" VALUE="optimal" <%=roadnet.getCheckProcoption("optimal")%>>Optimal
		</DIV>

		<DIV ALIGN="left">
			<INPUT TYPE=button VALUE="Set affinities" ONCLICK="child_open();">
			<INPUT TYPE=SUBMIT VALUE="Uniform affinities" ONCLICK="/*alert('Processing your request.\nIt may take a few seconds');*/">
			<INPUT ONCLICK="clearForm();deleteOverlays()" TYPE=button VALUE="Reset">
		</DIV>

		<DIV ALIGN="left">
			<INPUT ONCLICK="showPrevious();" TYPE=button VALUE="Show previous">
			<INPUT ONCLICK="showNext();" TYPE=button VALUE="Show next">
		</DIV>

		<DIV ALIGN="left">
			<INPUT ID="clickLats" TYPE=HIDDEN NAME=lats>
			<INPUT ID="clickLngs" TYPE=HIDDEN NAME=lngs>
			<INPUT ID="clickRats" TYPE=HIDDEN NAME=ratings>
		</DIV>
	</FORM>
</DIV>

<script>
	var osmmarkers = L.layerGroup();

	var mbUrl = 'https://api.tiles.mapbox.com/v4/{id}/{z}/{x}/{y}.png?access_token=pk.eyJ1IjoibWFwYm94IiwiYSI6ImNpejY4NXVycTA2emYycXBndHRqcmZ3N3gifQ.rJcFIG214AriISLbB6B5aw';
	var esriUrl = 'http://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}';
	var osmUrl = 'http://{s}.tile.osm.org/{z}/{x}/{y}.png';

	var osmLayer = L.tileLayer(osmUrl, {id: 'mapbox.light'});
	var esriLayer = L.tileLayer(esriUrl,{id: 'esri.satellite'});
	var mapboxLayer  = L.tileLayer(mbUrl, {id: 'mapbox.streets'});
	var map = L.map('map', {layers: [osmLayer, osmmarkers]}).setView([47.58,-122.3],12);

	var baseLayers = {
		'OSM': osmLayer,
		'Mapbox': mapboxLayer,
		'Satellite': esriLayer
	};

	var overlays = {'Markers': osmmarkers};
	L.control.layers(baseLayers, overlays).addTo(map);

	function addMarker(location,id,description,code) {
		var markAddress = "jsp/omcp/pins/marker.png";
		var anchor = [12, 28];
		var size = [18, 28];
		if (code=="2") {
			markAddress = "jsp/omcp/pins/arrow.png";
			anchor = [18,30];
			size = [30,30];
		}else if (code=="5") {
			markAddress = "jsp/omcp/pins/blue-dot.png";
			anchor = [18,30];
			size = [30,30];
		}
		var myIcon = L.icon({
			iconUrl: markAddress,
			iconSize: size,
			iconAnchor: anchor,
			popupAnchor: [-3, -26],
			shadowUrl: 'jsp/omcp/pins/marker-shadow.png',
			shadowSize: size,
			shadowAnchor: anchor
		});
		var marker = L.marker(location,{icon: myIcon}).bindTooltip(id).bindPopup(description).addTo(osmmarkers);
		markers.push (marker);
		return marker;
	}
	function onMapClick(e) {
		document.getElementById("clickLats").value += e.latlng.lat + " ";
		document.getElementById("clickLngs").value += e.latlng.lng + " ";
		addMarker(e.latlng,e.latlng.toString());
	}
	map.on('click', onMapClick);

	locations = new Array();
	plansize = new Array();
	meetups = new Array();
	markers = new Array();
	colors = new Array();
	routes = new Array();
	paths = new Array();

	function clearOverlays() {
		for (var i=0;i<markers.length;i++) map.removeLayer(markers[i]);
		for (var i=0;i<routes.length;i++) map.removeLayer(routes[i]);
		osmmarkers.clearLayers();
	}

	function showOverlays() {
		for (var i=0;i<markers.length;i++) markers[i].addTo(map);
		for (var i=0;i<routes.length;i++) routes[i].addTo(map);
	}

	// Deletes all markers in the array by removing references to them.
	function deleteOverlays() {
		clearOverlays();
		meetups = new Array();
		plansize = new Array();
		locations = new Array();
		markers = new Array();
		routes = new Array();
		paths = new Array();
		USER="A".charCodeAt();
		document.getElementById("clickLats").value = "";
		document.getElementById("clickLngs").value = "";
		document.getElementById("clickRats").value = "";
		//document.getElementById("clickForm").submit();
	}

	function clearForm() {
		document.getElementById("destination").value = "";
		document.getElementById("clickRats").value = "";
		<%roadnet.setRatings("");%>
	}

	function processTextQueries () {
		<%= roadnet.printTextQueries() %>
		printPins (textlocations,"2");
	}
	function processClickedQueries () {
		<%= roadnet.printClickedQueries() %>
		printPins (clicklocations,"13");
	}
	function processMeetingLocation () {
		<%= roadnet.printOMCPs() %>

		if (locations.length > 0) {
			meetups.push(addMarker(locations[0],locations[0].toString(), "", "5"));
		}
		printRoutePlan (0);
		Index = 0;
	}
	function printRoutePlan (index) {
		if (index>=plansize.length) return 0;
		var poffset=0, loffset=0;
		for (var i=0;i<index;++i) {
			loffset += locsize[i];
			poffset += plansize[i];
		}
		for (var i=0;i<locsize[index];++i) {
			if (loffset + i < locations.length) {
				meetups.push(addMarker(locations[loffset + i],locations[loffset + i].toString(),"","5"));
			}
		}
		for (var i=0;i<plansize[index];++i) {
			var route = L.polyline(paths[poffset+i], {color: colors[poffset+i]}).addTo(map);
			routes.push (route);
		}
	}
	function showNext () {
		if (Index<plansize.length-1) {
			while (meetups.length>0) {
				var marker = meetups.pop();
				map.removeLayer(marker);
				marker.removeFrom(osmmarkers);
			}
			while (routes.length>0) {
				map.removeLayer(routes.pop());
			}
			printRoutePlan (++Index);
		}
	}
	function showPrevious () {
		if (Index>0) {
			while (meetups.length>0) {
				var marker = meetups.pop();
				map.removeLayer(marker);
				marker.removeFrom(osmmarkers);
			}
			while (routes.length>0) {
				map.removeLayer(routes.pop());
			}
			printRoutePlan (--Index);
		}
	}
	function printPins (arglocations,mshape) {
		for(var i=0;i<arglocations.length;i++) {
			addMarker(arglocations[i][0],arglocations[i][0].toString(),"",mshape);
		}
	}

	function startUp () {
		document.getElementById("clickRats").value = "";
		<%roadnet.setRatings("");%>
		USER = "A".charCodeAt();
		<%roadnet.loadWashington();%>
		document.getElementById("destination").value = "<%=roadnet.getDestination()%>";
		document.getElementById("clickLats").value = "<%=roadnet.getLats()%>";
		document.getElementById("clickLngs").value = "<%=roadnet.getLngs()%>";
		document.getElementById("clickRats").value = "<%=roadnet.getRatings()%>";
		processClickedQueries();
		processTextQueries();
		processMeetingLocation();
	}
	startUp();

	function changemap() {
		deleteOverlays();
		document.getElementById("clickLats").value = "";
		document.getElementById("clickLngs").value = "";
		document.getElementById("clickRats").value = "";
		document.getElementById("destination").value = "";
		document.getElementById("predestination").value = "";
		<%roadnet.setPredestination("");%>
		<%roadnet.setDestination("");%>
		<%roadnet.setRatings("");%>
		<%roadnet.setLats("");%>
		<%roadnet.setLngs("");%>

		if (document.getElementById("mapname").value == "newyork") window.location.href = "newyork";
		else window.location.href = "francisco";
	}
</script>
</body>
</html>
