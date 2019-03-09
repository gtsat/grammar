
package wikipedia;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HTMLLinkExtractor {

	private Pattern patternTag, patternLink;
	private Matcher matcherTag, matcherLink;

	private static final String HTML_A_TAG_PATTERN = "(?i)<a([^>]+)>(.+?)</a>";
	private static final String HTML_A_HREF_TAG_PATTERN = "\\s*(?i)href\\s*=\\s*(\"([^\"]*\")|'[^']*'|([^'\">\\s]+))";


	public HTMLLinkExtractor() {
		patternTag = Pattern.compile (HTML_A_TAG_PATTERN);
		patternLink = Pattern.compile (HTML_A_HREF_TAG_PATTERN);
	}

	public ArrayList<HtmlLink> grabHTMLLinks (final String html) {
		ArrayList<HtmlLink> result = new ArrayList<>();

		matcherTag = patternTag.matcher (html);
		while (matcherTag.find()) {
			String href = matcherTag.group (1);
			String linkText = matcherTag.group (2);

			matcherLink = patternLink.matcher (href);

			while (matcherLink.find()) {
				String link = matcherLink.group (1);
				HtmlLink obj = new HtmlLink ();
				obj.setLink (link);
				obj.setLinkText (linkText);

				result.add (obj);
			}
		}
		return result;
	}
}

class HtmlLink {

    private String link;
    private String linkText;

    public HtmlLink () {};
    public String getLink () {return link;}
    public String getLinkText () {return linkText;}
    public void setLinkText (String linkText) {this.linkText = linkText;}
    public void setLink (String link) {this.link = replaceInvalidChar(link);}
    @Override public String toString () {return new StringBuffer("Link : ").append(this.link).append(" Link Text : ").append(this.linkText).toString();}

    private String replaceInvalidChar (String link) {
        link = link.replaceAll ("'", "");
        link = link.replaceAll ("\"", "");
        return link;
    }
}
