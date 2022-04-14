package packages.middleware.pub.client.database;
import com.eka.middleware.service.DataPipeline;
import com.eka.middleware.service.ServiceUtils;
import com.eka.middleware.template.SnippetException;
import java.sql.Connection;
import com.eka.middleware.adapter.SQL;
public final class rollbackTransaction{
	public static final void main(DataPipeline dataPipeline) throws SnippetException{
try{
    Connection txConn=(Connection)dataPipeline.get("txConn");
    SQL.rollbackTransaction(txConn);
  }catch(Exception e){
	dataPipeline.clear();
  	dataPipeline.put("error",e.getMessage());
    new SnippetException(dataPipeline,"Sneppet exception", new Exception(e));
}
	}

}