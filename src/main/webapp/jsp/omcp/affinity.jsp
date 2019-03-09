<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1//EN" "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd">
<%@page contentType="text/html;charset=UTF-8"%>
<%@page language="java" import="webroadnet.WebRoadNetOMCP"%>
<jsp:useBean id="roadnet" class="webroadnet.WebRoadNetOMCP" scope="application" />
<jsp:setProperty name="roadnet" property="*" />
<HTML xmlns="http://www.w3.org/1999/xhtml" xml:lang="el">
<META http-equiv="Content-Type" content="text/html; charset=utf-8" />
<style type="text/css">
.rating {
  float: left;
  width: 100px;
}

.rating span {
  float: right;
  position: relative;
}

.rating span input {
  position: absolute;
  top: 0px;
  left: 0px;
  opacity: 0;
}

.rating span label {
  display: inline-block;
  width: 15px;
  height: 15px;
  text-align: center;
  color: #FFF;
  background: #ccc;
  font-size: 30px;
  margin-right: 2px;
  line-height: 30px;
  border-radius: 50%;
  -webkit-border-radius: 50%;
}

.rating span:hover ~ span label, .rating span:hover label, .rating span.checked label,
  .rating span.checked ~ span label {
  background: #F90;
  color: #FFF;
}
</style>

<script type="text/javascript"
  src="http://ajax.googleapis.com/ajax/libs/jquery/1.4/jquery.min.js"></script>
<script type="text/javascript">
  userRatings = new Map();

  $(document).ready(
          function() {
            $('.rating input').click(function() {
              $(this).parent().siblings().removeClass('checked');
              $(this).parent().addClass('checked');
            });

            $('input:radio').change(
              function() {
                userRatings.set(this.name,this.value);
                var ratingName = this.name;
                var ratingValue = this.value;

                var ratings = document.getElementById("clickRats").value;//this.value;

                var ratingsArray = ratings.split(" ");
                var index_ratings = ratingsArray.indexOf(this.name);

                document.getElementById("clickRats").value += this.name + " " + this.value + " ";
              });
          });

  function returnYourChoice(choice) {
    opener.setSearchResult(targetField, choice);
    window.close();
  };

  closeChild = function() {
    opener.test(document.getElementById("clickRats").value);
    window.close();
  };
</script>
<script type="text/javascript">
  $(function() {
    $( "#reset" ).click(function() {
      alert("User ratings have been reset.");
      document.getElementById("clickRats").value="";
      $('.rating input').parent().removeClass('checked');
    });
  });
</script>
</HEAD>
<BODY>

  <FORM ID="clickForm" METHOD="POST">
    <script>
      lats = window.opener.document.getElementById('clickLats').value;
      latsLen = lats.split(" ").length;
    
      size = latsLen - 1;
      document.write ("\t<table style='width:" + (size * 100) + "px'>\n\n");

      document.write ("\t\t<tr>\n\n");
      document.write ("\t\t<td></td>\n");

      for (var i = 0; i < size; ++i)
        document.write ("\t\t<td><span style='display:inline-block; font-weight: bold; width: 100px;'>User "
                       + String.fromCharCode(65 + i)
                       + "</span></td>\n");

      document.write("\t</tr>\n\n");
      for (var i = 0; i < size; ++i) {
        document.write ("\t\t<tr>\n\t\t<td>\n\t\t<span style='display:inline-block; font-weight: bold; width: 100px;'>User "
                       + String.fromCharCode(65 + i)
                       + "</span>\n\t\t</td>");
        for (var j = 0; j < size; ++j) {
          document.write ("\t\t<td>\n\t\t<div class='rating'>\n");
          document.write ("\t\t\t<span><input type='radio' name='"+i+"x"+j+"' id='str5' value='5'><label for='str5'></label></span>\n");
          document.write ("\t\t\t<span><input type='radio' name='"+i+"x"+j+"' id='str4' value='4'><label for='str4'></label></span>\n");
          document.write ("\t\t\t<span><input type='radio' name='"+i+"x"+j+"' id='str3' value='3'><label for='str3'></label></span>\n");
          document.write ("\t\t\t<span><input type='radio' name='"+i+"x"+j+"' id='str2' value='2'><label for='str2'></label></span>\n");
          document.write ("\t\t\t<span><input type='radio' name='"+i+"x"+j+"' id='str1' value='1'><label for='str1'></label></span>\n");
          document.write ("\t\t</div>\n\t\t</td>\n");
        }
        document.write("\t\t</tr>\n\t\t\n\n");
      }
      document.write("\t</table>\n");
      window.resizeTo(130 + (size * 110), 100 + (35 * size));
    </script>

    <br>
      <DIV ALIGN="left">
        <INPUT TYPE=button VALUE="Done!" ONCLICK="/*alert('Processing you request.\nThis may take a few seconds...');*/closeChild();" />
        <INPUT TYPE=button ID="reset"  VALUE="Reset"/>
      </DIV> <INPUT ID="clickRats" TYPE=HIDDEN NAME=ratings VALUE="">
  </FORM>
</BODY>
</HTML>

