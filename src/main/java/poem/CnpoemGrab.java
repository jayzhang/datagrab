package poem;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.commons.lang.StringUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.gbdata.common.hc.Content;
import com.gbdata.common.hc.HttpClient;
import com.gbdata.common.json.JSONObject;
import com.gbdata.common.mongo.DBParam;
import com.gbdata.common.mongo.MongoEntityClient;
import com.gbdata.common.util.StringUtil;

public class CnpoemGrab {

	
	MongoEntityClient mongo = new MongoEntityClient(DBParam.LocalMongo().withDatabase("test"));
	
	HttpClient httpClient = new HttpClient();
	
	public void grab()
	{
		
		
		
		String url = "http://www.cnpoem.net/";
		
		
		Content cont = httpClient.get(url);
		
		Document doc = cont.getDocument();
		
		Element table = doc.select("table#AutoNumber5").first();
		
		for(Element a : table.select("td a"))
		{
			String time = StringUtil.trim(a.text());
			
			if(time.equals("首页"))
				continue;
			
			String link = a.absUrl("href");
			
			String query = StringUtils.substringAfter(link, "Sclass='");
			
			query = query.substring(0, query.length() - 1);
			
			try {
				query = URLEncoder.encode(query, "gbk");
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			query = StringUtils.substringBefore(link, "Sclass='") + "Sclass='" + query + "'";
			
			JSONObject obj = new JSONObject();
			
			obj.put("time", time);
			
			obj.put("url", query);
			
			grabTime(obj);
			
			System.out.println(time);
		}
		
	}
	
	
	public void grabTime(JSONObject obj)
	{
		String url = obj.getString("url");
		
		Content cont = httpClient.get(url);
		
		Document doc = cont.getDocument();
		
		Elements fonts = doc.select("font[face^=Verdana,");
		
//		for(Element font : )
//		{
//			
//		}
		
//		System.out.println(font);
	}
	
	public static void main(String[] args) {
		CnpoemGrab w = new CnpoemGrab();
		w.grab();
		
		
//		try {
//			System.out.println(URLEncoder.encode("周", "gbk"));
//		} catch (UnsupportedEncodingException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}

	}

}
