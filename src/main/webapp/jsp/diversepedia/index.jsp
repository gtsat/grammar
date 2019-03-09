<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1//EN" "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd">
<%@page contentType="text/html;charset=UTF-8"%>
<%@page language="java" import="wikipedia.WebClient"%>
<jsp:useBean id="pedia" class="wikipedia.WebClient" scope="session" />
<jsp:setProperty name="pedia" property="*" />
<HTML xmlns="http://www.w3.org/1999/xhtml" xml:lang="el">
<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
  <title>DiversePedia - Wikipedia's Diverse Search Engine</title>
  <link rel="shortcut icon" href="jsp/diversepedia/images/UAlberta_Icon.png" type="image/x-icon">
  <meta name="viewport" content="initial-scale=1.0, user-scalable=yes" />

  <link href="http://code.google.com//apis/maps/documentation/javascript/examples/default.css" rel="stylesheet">
  <style type="text/css">
    html, body {width: 100%; height: 100%}
    body {margin-top: 0px; margin-right: 0px; margin-left: 0px; margin-bottom: 0px}
  </style>

  <link rel="stylesheet" href="//code.jquery.com/ui/1.11.4/themes/smoothness/jquery-ui.css">
  <script src="//code.jquery.com/jquery-1.10.2.js"></script>
  <script src="//code.jquery.com/ui/1.11.4/jquery-ui.js"></script>
  <link rel="stylesheet" href="/resources/demos/style.css">
  <style>
    #red, #green, #blue {
      float: left;
      clear: left;
      width: 250px;
      margin: 10px;
    }
    #swatch {
      width: 100px;
      height: 100px;
      margin-top: 25px;
      margin-left: 300px;
      background-image: none;
    }
    #red .ui-slider-range {background: #ef2929;}
    #red .ui-slider-handle {border-color: #ef2929;}
    #green .ui-slider-range {background: #8ae234;}
    #green .ui-slider-handle {border-color: #8ae234;}
    #blue .ui-slider-range {background: #729fcf;}
    #blue .ui-slider-handle {border-color: #729fcf;}
  </style>

  <script>
  function hexFromRGB (r,g,b) {
    var hex = [
      r.toString(16),
      g.toString(16),
      b.toString(16)
    ];
    $.each(hex,function(nr,val) {
      if (val.length===1) {
        hex[nr] = "0"+val;
      }
    });
    return hex.join("").toUpperCase();
  }
  function refreshSwatch() {
    var red = $("#red").slider("value");
    var green = $("#green").slider("value");
    var blue = $("#blue").slider("value");
    var hex = hexFromRGB(red,green,blue);
    $("#swatch").css("background-color","#"+hex);

    document.getElementById("ired").value = red;
    document.getElementById("igreen").value =green;
    document.getElementById("iblue").value = blue;

    document.getElementById("lambda").value =red/255.0;
    document.getElementById("alpha").value = green/255.0;
    document.getElementById("beta").value = blue/255.0;

    document.getElementById("1-lambda").value = 1-red/255.0;
    document.getElementById("1-alpha").value = 1-green/255.0;
    document.getElementById("1-beta").value = 1-blue/255.0;
  }
  $(function() {
    $("#red,#green,#blue").slider({
      orientation: "horizontal",
      range: "min",
      max: 255,
      value: 127,
      slide: refreshSwatch,
      change: refreshSwatch
    });
    $("#red").slider("value",<%=pedia.getRed()%>);
    $("#green").slider("value",<%=pedia.getGreen()%>);
    $("#blue").slider("value",<%=pedia.getBlue()%>);

    document.getElementById("ired").value = <%=pedia.getRed()%>;
    document.getElementById("igreen").value = <%=pedia.getGreen()%>;
    document.getElementById("iblue").value = <%=pedia.getBlue()%>;
  });
  </script>
</HEAD>
<BODY class="ui-widget-content" style="border:0;">

  <%pedia.reset();%>

  <DIV STYLE="top:3%;left:3%;position:absolute;padding:5px;">
  <IMG SRC="jsp/diversepedia/images/wikipedialogo.jpg" ALT="wikipedia-logo" STYLE="width:300px;height:150px;horizontal-align:middle;">
  <H1><SPAN STYLE="width:300px;height:150px;horizontal-align:middle;margin:200px;">DiversePedia</SPAN></H1>
  <FORM ID="Form" METHOD=POST ACTION="response">

  <TABLE>
  <tr>
    <td></td>
    <td>
    <DIV ALIGN="left">
    <INPUT TYPE=TEXT ID=query NAME=query SIZE=25>
    <INPUT TYPE=SUBMIT VALUE="Search" ONCLICK="/*alert('Processing your request.\nThis may take a few seconds');*/">

    <SPAN STYLE="display:inline-block;font-weight:bold;width:10px;"></SPAN>

    <SELECT ID="ranking" NAME="mechanism">
      <OPTION VALUE="minsum" <%=pedia.getRankingSelection("minsum")%>>minsum</OPTION>
      <OPTION VALUE="minmax" <%=pedia.getRankingSelection("minmax")%>>minmax</OPTION>
    </SELECT>

    <SELECT ID="mechanism" NAME="mechanism">
      <OPTION VALUE="symmetric" <%=pedia.getMechanismSelection("symmetric")%>>symmetric</OPTION>
      <OPTION VALUE="ordered" <%=pedia.getMechanismSelection("ordered")%>>ordered</OPTION>
    </SELECT>

    <SELECT ID="language" NAME="language">
      <!--OPTION VALUE="english" <%=pedia.getLanguageSelection("english")%>>en</option>
      <OPTION VALUE="espanol" <%=pedia.getLanguageSelection("espanol")%>>es</option>
      <OPTION VALUE="french" <%=pedia.getLanguageSelection("french")%>>fr</option>
      <OPTION VALUE="german" <%=pedia.getLanguageSelection("german")%>>ge</option-->
      <OPTION VALUE="greek" <%=pedia.getLanguageSelection("greek")%>>gr</option>
    </SELECT>
    </DIV>
    </td>
  </tr>

  <tr>
    <td>
    <DIV>
    <tr>
      <td><SPAN STYLE="width:50px">relevance/dissimilarity trade-off</SPAN></td>
      <td><DIV ID="red"></DIV></td>
    </tr>
    <tr>
      <td><SPAN STYLE="width:50px">relevance network/content trade-off</SPAN></td>
      <td><DIV ID="green"></DIV></td>
    </tr>
    <tr>
      <td><SPAN STYLE="width:50px">dissimilarity network/content trafe-off</SPAN></td>
      <td><DIV ID="blue"></DIV></td>
    </tr>
    </DIV>
    </td>
  </tr>
  </TABLE>

    <INPUT ID="ired" TYPE=HIDDEN NAME="red">
    <INPUT ID="igreen" TYPE=HIDDEN NAME="green">
    <INPUT ID="iblue" TYPE=HIDDEN NAME="blue">

    <INPUT ID='offset' NAME="offset" TYPE=HIDDEN VALUE="0">


    <br/>
    Ranking_Fuction = min (</br>
    <INPUT ID="lambda" TYPE="label" SIZE="4"> &#215; max(
    <INPUT ID="alpha" TYPE="label" SIZE="4"> &#215;
    network_distance from query center +
    <INPUT ID="1-alpha" TYPE="label" SIZE="4"> &#215;
    textual_distance from query center) - </br>
    <INPUT ID="1-lambda" TYPE="label" SIZE="4"> &#215; min(
    <INPUT ID="beta" TYPE="label" SIZE="4"> &#215;
    network_distance in result_set +
    <INPUT ID="1-beta" TYPE="label" SIZE="4"> &#215;
    textual_distance in result_set)</FORM></br>
    )

    <DIV ID="swatch" class="ui-widget-content ui-corner-all"></DIV>
  </FORM>
  </DIV>

  <DIV STYLE="bottom:1%;left:0%;position:absolute;padding:5px;">
    <H4>Powered by</H4>
    <IMG SRC="jsp/diversepedia/images/java.png" ALT="java-logo" STYLE="height:70px;">
    <IMG SRC="jsp/diversepedia/images/GlassFish_logo.png" ALT="topcat-logo" STYLE="height:60px;">
    <IMG SRC="jsp/diversepedia/images/Freebsd_logo.svg.png" ALT="topcat-logo" STYLE="height:70px;">
  </DIV>
</BODY>
</html>