package poem;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import com.gbdata.common.json.JSONObject;
import com.gbdata.common.mongo.DBParam;
import com.gbdata.common.mongo.Entity;
import com.gbdata.common.mongo.MongoEntityClient;
import com.gbdata.common.util.FileUtil;

public class ExportPoemSQLite {

	MongoEntityClient mongo = new MongoEntityClient(DBParam.LocalMongoDev().withDatabase("cnpoem"));
	
	String sqliteFileName = "d:\\poem.db";
	
	Connection conn = null;
	
	
	{
		try {
			Class.forName("org.sqlite.JDBC");
			
			String sqliteUrl = "jdbc:sqlite:" + sqliteFileName;
			
			conn = DriverManager.getConnection(sqliteUrl);
			
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void initAuthorTable()
	{
		try {
			Statement initStatement = conn.createStatement();
			String initSQL = FileUtil.readLine("poem_author.sql");
			initStatement.executeUpdate(initSQL);
			initStatement.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void initPoemTable()
	{
		try {
			Statement initStatement = conn.createStatement();
			String initSQL = FileUtil.readLine("poem_poem.sql");
			initStatement.executeUpdate(initSQL);
			initStatement.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void insertAuthorTable()
	{
		try {
			PreparedStatement insertStatement = conn.prepareStatement("insert into author values (?,?,?,?,?,?);");
			for(Entity e : mongo.iterator("author").showProgress())
			{
				JSONObject o = e.getJSONObjectContent();
				
				JSONObject time = o.getJSONObject("time");
				
				insertStatement.setString(1, e.getId());
				insertStatement.setString(2, o.getString("name"));
				insertStatement.setString(3, o.getString("alph"));
				insertStatement.setString(4, time.getString("name"));
				insertStatement.setString(5, o.getString("detail"));
				insertStatement.setInt(6, o.getInt("poemnum"));
			
				insertStatement.addBatch();
			}
			
			insertStatement.executeBatch();
			
			insertStatement.close();
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	
	public void insertPoemTable()
	{
		try {
			PreparedStatement insertStatement = conn.prepareStatement("insert into poem values (?,?,?,?);");
			int cc = 0;
			for(Entity e : mongo.iterator("poem").showProgress())
			{
				JSONObject o = e.getJSONObjectContent();
				
				insertStatement.setString(1, e.getId());
				insertStatement.setString(2, o.getString("title"));
				insertStatement.setString(3, o.getString("author"));
				insertStatement.setString(4, o.getString("content"));
			
				insertStatement.addBatch();
				
				cc ++;
				
				if(cc == 10000)
					break;
			}
			
			insertStatement.executeBatch();
			
			insertStatement.close();
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	
	public void close()
	{
		if(conn != null)
			try {
				conn.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}
	
	public static void main(String[] args) {
		
		ExportPoemSQLite w = new ExportPoemSQLite();
		
		w.initPoemTable();
		
		w.insertPoemTable();

		w.close();
	}

}
