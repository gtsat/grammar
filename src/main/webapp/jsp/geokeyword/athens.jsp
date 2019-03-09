<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1//EN" "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd">
<%@page contentType="text/html;charset=UTF-8"%>
<%@page language="java" import="geokeyword.GeoKeyword" %>
<jsp:useBean id="mygeo" class="geokeyword.GeoKeyword" scope="application"/>
<jsp:setProperty name="mygeo" property="*"/>
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="el">
<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
<title>GeoKeyword</title>
	<link rel="shortcut icon" href="jsp/omcp/images/favicon.ico" type="image/x-icon">
	<meta name='viewport' content='width=device-width, initial-scale=1.0' />
	<link href='http://leafletjs.com/docs/css/normalize.css' rel='stylesheet' type='text/css' />
	<link href='http://leafletjs.com/docs/css/main.css' rel='stylesheet' type='text/css' />
	<link rel="stylesheet" href="https://unpkg.com/leaflet@1.3.4/dist/leaflet.css" integrity="sha512-puBpdR0798OZvTTbP4A8Ix/l+A4dHDD0DGqYW6RQ+9jxkRFclaxxQb/SJAWZfWAkuyeQUytO7+7N4QKrDh+drA==" crossorigin=""/>
	<script src="https://unpkg.com/leaflet@1.3.4/dist/leaflet.js" integrity="sha512-nMMmRyTVoLYqjP9hrbed9S+FzjZHW5gY1TWCHA5ckwXZBadntCNs8kEqAWdrb9O7rxbCaA4lKTIWjDXZxflOcA==" crossorigin=""></script>
	<style>
		html, body {width: 100%; height: 100%}
		body {margin-top: 0px; margin-right: 0px; margin-left: 0px; margin-bottom: 0px}
		.map {height: 100%; width: 80%; margin: 0px; right: 0%; position: absolute}
	</style>
	<script>
		function clearForm() {
			document.getElementById("xkeyword").value = "";
			document.getElementById("ykeyword").value = "";
		}
		function encode (word) {
			var code = new BigDecimal("0");
			if (word) {
				for (var c in word) {
					var b = c - 96;
					code = code.multiply(new BigDecimal("27")).add(BigDecimal(b.toString()));
				}
			}
			return code.mod(BigInteger.valueOf(THRES));
		}
	</script>
</head>
<body>
<DIV ID="panel" STYLE="bottom:3%;left:1%;position:absolute;padding:5px; width:20%">
	<FORM ID="clickForm1" METHOD=POST>
		<DIV ALIGN="left">
			<SPAN STYLE="display:inline-block; font-weight: bold; width: 110px;">long-keyword</SPAN>
			<INPUT TYPE=TEXT ID=xkeyword NAME=xkeyword VALUE="<%=mygeo.getXkeyword()%>" SIZE=10><br>
		</DIV>
		<DIV ALIGN="left">
			<SPAN STYLE="display:inline-block; font-weight: bold; width: 110px;">latt-keyword</SPAN>
			<INPUT TYPE=TEXT ID=ykeyword NAME=ykeyword VALUE="<%=mygeo.getYkeyword()%>" SIZE=10><br>
		</DIV>
		<INPUT TYPE=SUBMIT VALUE="Encode"/>
		<INPUT TYPE=Button VALUE="Reset" ONCLICK="clearForm();"/>
	</FORM>
</DIV>

<div id='map' class='map'></div>
<script>
	var markers = L.layerGroup();
	var athens = L.latLng(37.983810, 23.727539);
	L.marker([37.983810, 23.727539]).bindTooltip('Hi There!').bindPopup('Athens').addTo(markers);

	var mbUrl = 'https://api.tiles.mapbox.com/v4/{id}/{z}/{x}/{y}.png?access_token=pk.eyJ1IjoibWFwYm94IiwiYSI6ImNpejY4NXVycTA2emYycXBndHRqcmZ3N3gifQ.rJcFIG214AriISLbB6B5aw';
	var osmUrl = 'http://{s}.tile.osm.org/{z}/{x}/{y}.png';

	var osmLayer = L.tileLayer(osmUrl, {id: 'mapbox.light'});
	var mapboxLayer  = L.tileLayer(mbUrl, {id: 'mapbox.streets'});
	var map = L.map('map', {layers: [osmLayer, markers]}).fitBounds(L.latLngBounds (athens,athens));

	var baseLayers = {
		'OSM': osmLayer,
		'Mapbox': mapboxLayer
	};

	var overlays = {'Markers': markers};
	L.control.layers(baseLayers, overlays).addTo(map);

	function addMarker(location,id,description) {
		console.log ('addMarker @ '+location);
		L.marker(location).bindTooltip(id).bindPopup(description).addTo(markers);
	}
	function onMapClick(e) {
		addMarker(e.latlng,e.latlng.toString());
	}
	map.on('click', onMapClick);
</script>
</body>
</html>
