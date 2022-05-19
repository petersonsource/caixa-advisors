package br.gov.caixa.rtc.util;

import java.io.Reader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import javax.swing.text.html.parser.ParserDelegator;

import net.htmlparser.jericho.Renderer;
import net.htmlparser.jericho.Segment;
import net.htmlparser.jericho.Source;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;





public abstract class Formatters {
	
	public static final String ITSM_DATE_PATTERN = "dd/MM/yyyy hh:mm:ss";
	public static final String RTC_DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
	
	public static Date itsmToDate(String date) throws ParseException{
		Date dateObj = new SimpleDateFormat(ITSM_DATE_PATTERN).parse(date);
		return dateObj;
	}
	
	public static String dateToRTC(Date date){
		SimpleDateFormat sdf = new SimpleDateFormat(RTC_DATE_PATTERN);
		sdf.setTimeZone(TimeZone.getTimeZone("GMT+00:00"));
		String dateRTC = sdf.format(date);
		return dateRTC;
	}
	
	
		
	
	//TODO LPF
//verificar pendencia
	public static String parseHTML2String(String html){
		
		Document doc = Jsoup.parse(html);
		String text = doc.body().text();
		return text;
		
	}

	public static String parseHTML(String html){
	    		
	if(html == null || html == "")
		return "";
		
	Source htmlSource = new Source(html);
	Segment htmlSeg = new Segment(htmlSource, 0, htmlSource.length());
		Renderer htmlRend = new Renderer(htmlSeg);
	    return htmlRend.toString();
	}
	
		
	
	    public static String parseString2Html(String string) {
		    StringBuffer sb = new StringBuffer(string.length());
		    // true if last char was blank
		    boolean lastWasBlankChar = false;
		    int len = string.length();
		    char c;

		    for (int i = 0; i < len; i++) {
		        c = string.charAt(i);
		        if (c == ' ') {
		            // blank gets extra work,
		            // this solves the problem you get if you replace all
		            // blanks with &nbsp;, if you do that you loss 
		            // word breaking
		            if (lastWasBlankChar) {
		                lastWasBlankChar = false;
		                sb.append("&nbsp;");
		            } else {
		                lastWasBlankChar = true;
		                sb.append(' ');
		            }
		        } else {
		            lastWasBlankChar = false;
		            //
		            // HTML Special Chars
		            if (c == '"')
		                sb.append("&quot;");
		            else if (c == '&')
		                sb.append("&amp;");
		            else if (c == '<')
		                sb.append("&lt;");
		            else if (c == '>')
		                sb.append("&gt;");
		            else if (c == '\n')
		                // Handle Newline
		                sb.append("<br/>");
		            else {
		                int ci = 0xffff & c;
		                if (ci < 160)
		                    // nothing special only 7 Bit
		                    sb.append(c);
		                else {
		                    // Not 7 Bit use the unicode system
		                    sb.append("&#");
		                    sb.append(new Integer(ci).toString());
		                    sb.append(';');
		                }
		            }
		        }
		    }
		    return sb.toString();
		
	    
	}
	
	
}
