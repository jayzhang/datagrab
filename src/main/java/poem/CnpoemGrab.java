package poem;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import com.gbdata.common.hc.Content;
import com.gbdata.common.hc.HttpClient;
import com.gbdata.common.json.JSONObject;
import com.gbdata.common.json.JSONValue;
import com.gbdata.common.mongo.DBParam;
import com.gbdata.common.mongo.Entity;
import com.gbdata.common.mongo.MongoEntityClient;
import com.gbdata.common.mq.IMQWorker;
import com.gbdata.common.mq.RabbitMQConsumer;
import com.gbdata.common.util.JsoupUtil;
import com.gbdata.common.util.Logger;
import com.gbdata.common.util.StringUtil;

public class CnpoemGrab implements IMQWorker{

	String URL = "http://cnpoem.net/dq/";
	
	MongoEntityClient mongo = new MongoEntityClient(DBParam.LocalMongoDev().withDatabase("cnpoem"));
	
	String TableAuthor = "author";
	
	String TablePoem = "poem";
	
	String Queue = "CnpoemGrab";
	
	HttpClient httpClient = new HttpClient();
	
	public void grabAuthor()
	{
		String url = URL;
		
		Content cont = httpClient.get(url);
		
		if(cont == null)
		{
			System.err.println("error:" + url);
			System.exit(0);
		}
		
		Document doc = cont.getDocument("GBK");
		
		Element table = doc.select("table#AutoNumber5").first();
		
		for(Element a : table.select("td a"))
		{
			String name = StringUtil.trim(a.text());
			
			if(name.equals("首页"))
				continue;
			
			String link = a.absUrl("href");
			
			link = encodeURL(link);
			
			JSONObject time = new JSONObject();
			
			time.put("name", name);
			
			time.put("url", link);
			
			grabTime(time);
		}
		
	}
	
	
	private String encodeURL(String raw)
	{
		String cn = StringUtils.substringBetween(raw, "'", "'");
		
		try {
			cn = URLEncoder.encode(cn, "gbk");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		String url = StringUtils.substringBefore(raw, "'") + "'" + cn + "'" + StringUtils.substringAfterLast(raw, "'");
		
		return url;
	}
	
	public void grabTime(JSONObject time)
	{
		String url = time.getString("url");
		
		String timename = time.getString("name");
		
		Logger.logger.info("=============================grab time: " + timename);
		
		Content cont = httpClient.get(url);
		
		if(cont == null)
		{
			System.err.println("error:" + time);
			System.exit(0);
		}
		
		Document doc = cont.getDocument("GBK");
		
		Elements fonts = doc.select("font[face^=Verdana,");
		
		for(int i = 0 ; i < fonts.size(); ++ i)
		{
			Element font = fonts.get(i);
			
			String alph = font.text();
			
			Logger.logger.info("------------------------grab Alph:" + alph);
			
			Element fontNext = null;
			
			if(i + 1 < fonts.size())
				fontNext = fonts.get(i + 1);
			
			Element tr = JsoupUtil.findParent(font, "tr");
			Element trNext = null;
			
			if(fontNext != null)
				trNext = JsoupUtil.findParent(fontNext, "tr");
			
			if(tr != null)
			{
				tr = tr.nextElementSibling();
				while(tr != null && tr != trNext)
				{
					
					for(Element a : tr.select("a"))
					{
						String authorurl = a.absUrl("href");
						
						authorurl = encodeURL(authorurl);
						
						String author = a.text();
						
						JSONObject authorObj = new JSONObject();
						
						authorObj.put("name", author);
						
						authorObj.put("url", authorurl);
						
						authorObj.put("time", time);
						
						authorObj.put("alph", alph);
						
						grabAuthor(authorObj);
					}
					
					tr = tr.nextElementSibling();
				}
			}

		}
	}
	
	
	private void grabAuthor(JSONObject author)
	{
		Logger.logger.info("------grab author:" + author);
		
		String name = author.getString("name");
		
		String url = author.getString("url");
		
		Content cont = httpClient.get(url);
		
		if(cont == null)
		{
			System.err.println("error:" + author);
//			System.exit(0);
			
			return;
		}
		
		Document doc = cont.getDocument("GBK");
		
		Element center = doc.select("td>b>center:containsOwn(" + name + ")").first();
		
		if(center != null)
		{
			Element b = JsoupUtil.findParent(center, "b");
			if(b != null)
			{
				String html = b.toString();
				
				author.put("detail", html);
				
				Element font = b.nextElementSibling();
				if(font != null)
				{
					Element p = font.select("p[align=center]:containsOwn(条/页)").first();
					if(p != null)
					{
						String pageTxt = p.text();
						
						String txt = StringUtils.substringBetween(pageTxt, "共 " , " 篇作品");
						
						if(!StringUtil.isEmpty(txt) && StringUtils.isNumeric(txt))
						{
							int total = Integer.valueOf(txt);
							author.put("poemnum", total);
						}
						
						Entity e = new Entity(name, TableAuthor, author);
						
						mongo.save(e);
					}
				}
			}
		}
	}
	
	
	public void grabPoem()
	{
//		RabbitMQProducer prod = new RabbitMQProducer(Queue);
//		for(Entity e: mongo.iterator(TableAuthor))
//			prod.send(e.getJSONObjectContent().toJSONString());
//		prod.close();
		
		
		ExecutorService threadPool = Executors.newCachedThreadPool();
		
		for (int i = 0; i < 20; i++) 
		{
			threadPool.submit(new Runnable() 
			{
				public void run() 
				{
					CnpoemGrab g = new CnpoemGrab();
					new RabbitMQConsumer(Queue).pollNonBlock(g);
				}
			});
		}
		
		threadPool.shutdown();
		
		try {
			while (!threadPool.awaitTermination(10000, TimeUnit.MILLISECONDS)) {
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	
	@Override
	public boolean execTask(String message) throws Exception {
		
		JSONObject obj = JSONValue.parseJSONObject(message);
		
		grabPoem(obj);
		
		return true;
	}
	
	private void grabPoem(JSONObject author)
	{
		String name = author.getString("name");
		
		String url = author.getString("url");
		
		Content cont = httpClient.get(url);
		
		if(cont == null)
		{
			System.err.println("error:" + author);
			return;
		}
		
		Document doc = cont.getDocument("GBK");
		
		
		Element p = doc.select("p[align=center]:containsOwn(条/页)").first();
		
		int totalPage = 0;
		
		if(p != null)
		{
			String pageTxt = p.text();
			
			pageTxt = StringUtils.substringAfter(pageTxt, "条/页");
			
			String tmp = StringUtils.substringBetween(pageTxt, "共", "页");
			
			if(!StringUtil.isEmpty(tmp) && StringUtils.isNumeric(tmp))
				totalPage = Integer.valueOf(tmp);
		}
		
		parsePoemList(name, doc);
		
//		int page = 2;
		
		for(int page = 2; page<= totalPage; ++ page)
		{
			Logger.logger.info("Page:" + page);
			
			String nexturl = url + "&page=" + page;
			
			cont = httpClient.get(nexturl);
			
			if(cont != null)
			{
				doc = cont.getDocument("GBK");
				parsePoemList(name, doc);
			}
		}
	}
	
	
	private void parsePoemList(String author, Document doc)
	{
		for(Element a : doc.select("td li a[href^=MusicList.asp?Specialid=]"))
		{
			String poemurl = a.absUrl("href");
			
			String title = a.text();
			
			JSONObject poem = new JSONObject();
			
			poem.put("author", author);
			
			poem.put("url", poemurl);
			
			poem.put("title", title);
			
			grabPoemDetail(poem);
//			
		}
	}
	
	public void grabPoemDetail(JSONObject poem)
	{
		String poemurl = poem.getString("url");
		
		String poemid = StringUtils.substringAfter(poemurl, "Specialid=");
		
		Content cont = httpClient.get(poemurl);
		
		if(cont == null)
			return;
		
		Document doc = cont.getDocument("GBK");
		
		Element div = doc.select("div#content_article").first();
		
		if(div != null)
			poem.put("content", div.toString());
		
		
		Element td = doc.select("td:containsOwn(【注释】：)").first();
		
		if(td != null)
		{
			String notes = "";
			List<Node> nodes = td.childNodes();
			int i = 0;
			for(; i < nodes.size(); ++ i)
			{
				Node node = nodes.get(i);
				if(node instanceof TextNode)
				{
					TextNode tn = (TextNode)node;
					if(tn.text().contains("【注释】"))
						break;
				}
			}
			if(i < nodes.size())
			{
				for(i++ ; i < nodes.size(); ++ i)
				{
					Node node = nodes.get(i);
					if(node instanceof TextNode)
					{
						TextNode tn = (TextNode)node;
						String txt = StringUtil.trim(tn.text());
						if(!StringUtil.isEmpty(txt))
							notes = notes + tn.text() + "\n";
					}
				}
			}
			
			poem.put("notes", notes);
		}
		
		Logger.logger.info("update poem:" + poem.getString("title") + ", author:" + poem.getString("author"));
		
		Entity e = new Entity(poemid, TablePoem, poem);
		
		mongo.save(e);
	}
	
	public void test()
	{
		Entity e = mongo.get(TableAuthor, "李白");
		
		try {
			this.execTask(e.getJSONObjectContent().toJSONString());
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		CnpoemGrab w = new CnpoemGrab();
		
//		JSONObject obj = new JSONObject();
//		obj.put("url", "http://cnpoem.net/dq/MusicList.asp?Specialid=2435");
//		w.grabPoemDetail(obj);
		
//		w.grabAuthor();
		
		w.grabPoem();
		
//		try {
//			System.out.println(URLEncoder.encode("周", "gbk"));
//		} catch (UnsupportedEncodingException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		w.test();
	}




}
