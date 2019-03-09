<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1//EN" "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd">
<%@page contentType="text/html;charset=UTF-8"%>
<%@page language="java" import="wikipedia.WebClient"%>
<jsp:useBean id="pedia" class="wikipedia.WebClient" scope="session" />
<jsp:setProperty name="pedia" property="*" />
<HTML xmlns="http://www.w3.org/1999/xhtml" xml:lang="el">
<META http-equiv="Content-Type" content="text/html; charset=utf-8" />
  <TITLE>DiversePedia - Wikipedia's Diverse Search Engine</TITLE>
  <link rel="shortcut icon" href="jsp/diversepedia/images/UAlberta_Icon.png" type="image/x-icon">
  <meta name="viewport" content="initial-scale=1.0, user-scalable=yes" />

  <style TYPE='text/css'>
     html, body {width: 100%; height: 100%}
     body {margin-top: 0px; margin-right: 0px; margin-left: 0px; margin-bottom: 0px}
     table, th, td {border: 1px solid black;}
  </style>

  <script>
    function write (links, similarities, netdistances) {
      document.write ("\t<table>\n\n");

      document.write ("\t\t<tr>\n");
      document.write ("\t\t<td colspan='4' style='horizontal-align: middle; padding: 10px;'>\n");
      document.write ("\t\t<a href='file:/home/gtsat/Documents/"+qcenter+"'><span style='width:100%;'>"+titles[0]+"</span></a>");
      document.write ("\t\t</td>\n");
      document.write ("\t\t</tr>\n");

      document.write ("\t\t<tr>\n");
      document.write ("\t\t<td colspan='4' style='padding: 10px;'>\n");
      document.write ("\t\t<span style='display:inline-block;width:100%;'>"+paragraphs[0]+"</span>");
      document.write ("\t\t</td>\n");
      document.write ("\t\t</tr>\n");

      var offset = <%=pedia.getOffset()%>;
      for (var i = offset; i<offset+5; ++i) {

        document.write ("\t\t<tr>\n");
        document.write ("\t\t<div>\n");
        document.write ("\t\t<td><span style='display:inline-block;width:1%;horizontal-align:middle;'>"+(i+1)+"</span></td>\n");
        document.write ("\t\t<td style='horizontal-align:middle;padding:10px;'>\n");
        document.write ("\t\t<a href='file:/home/gtsat/Documents/"+links[i]+"'><span style='width:33%;'>"+titles[i+1]+"</span></a>");
        document.write ("\t\t</td>\n");

        document.write ("\t\t<td style='horizontal-align:middle;padding:5px;'>");
        document.write ("\t\t<span style='display:inline-block;width:33%;'>Similarity: "+similarities[i]+"%</span>");
        document.write ("\t\t</td>");

        document.write ("\t\t<td style='horizontal-align:middle;padding:5px;'>");
        document.write ("\t\t<span style='display:inline-block;width:33%;'>Distance: "+netdistances[i]+" clicks</span>");
        document.write ("\t\t</td>");
        document.write ("\t\t</div>\n");
        document.write ("\t\t</tr>\n");

        document.write ("\t\t<tr>\n");
        document.write ("\t\t<td colspan='4' style='padding:10px;'>\n");
        document.write ("\t\t<span style='display:inline-block;width:100%;horizontal-align:middle;'>"+paragraphs[i+1]+"</span>");
        document.write ("\t\t</td>\n");
        document.write ("\t\t</tr>\n");
      }
      document.write ("\t</table>\n");
    }

    function next () {
        var temp = 5 + <%=pedia.getOffset()%>;
        document.getElementById('offset').value = temp; //temp.toString();

        window.location.href='response.jsp';
    }

    function prev () {
        var temp = <%=pedia.getOffset()%>;
        if (temp>=5) {
            temp -= 5;
            document.getElementById('offset').value = temp; //temp.toString();

            window.location.href='response.jsp';
        }else{
            alert('No previous results to show.\nYou may try next instead!');
        }
    }
  </script>
</HEAD>
<BODY>
  <DIV STYLE='padding:15px;'>
    <div style="float:left; display:inline-block; ">
      <span style="float:left;"><IMG SRC='jsp/diversepedia/images/wikipedialogo.jpg' ALT='wikipedia-logo' STYLE='width:250px;height:125px;'></span>
      <span style="right:0%;position:absolute;padding:5px;;"><a href="http://homegrownresearch.com"><IMG SRC="jsp/omcp/images/homegrown_small.png"/></a></span>
    </div>
    </br>

    <div style="float:left; display:inline-block; ">
      <H1>Matched Results</H1>
      <SCRIPT>
        <%=pedia.query(1,5)%>
        write (links,similarities,netdistances);
      </SCRIPT>

      <br>
      <FORM METHOD='POST'>
      <DIV ALIGN='left'>
        <!--<BUTTON ONCLICK="window.location.href='index.jsp';">Return</BUTTON>-->
        <BUTTON ONCLICK='prev();'>Prev</BUTTON>
        <BUTTON ONCLICK='next();'>Next</BUTTON>
        <INPUT ID='offset' NAME='offset' TYPE=HIDDEN VALUE='<%=pedia.getOffset()%>'>
      </DIV>
      </FORM>

      <DIV STYLE='left:0%;position:absolute;padding:5px;'>
        <H4>Powered by</H4>
        <IMG SRC='jsp/diversepedia/images/java.png' ALT='java-logo' STYLE='height:70px;'>
        <IMG SRC="jsp/diversepedia/images/GlassFish_logo.png" ALT="topcat-logo" STYLE="height:60px;">
        <IMG SRC="jsp/diversepedia/images/Freebsd_logo.svg.png" ALT="topcat-logo" STYLE="height:70px;">
      </DIV>
    </div>
  </DIV>
</BODY>
</HTML>