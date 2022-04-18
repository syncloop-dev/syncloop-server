package packages.ekaScheduler.cronJob.adapter.services.DML;

import com.eka.middleware.service.DataPipeline;
import com.eka.middleware.service.ServiceUtils;
import com.eka.middleware.template.SnippetException;
import com.eka.middleware.adapter.SqlResolver;
import java.io.File;
import java.io.FileInputStream;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import java.util.Properties;

public final class insertScheduler {

	static JsonObject mainSqlJsonObject=null;
	static final String syncBlock=new String("sync");
	public static final void main(DataPipeline dataPipeline) throws SnippetException{
//		String path="D:/Middleware/packages/testFlowServices/services/flow/simpleFlow.flow";
		try{
		  if(mainSqlJsonObject==null)
			synchronized(syncBlock){
			  String location = ServiceUtils.getPackagesPath();
			  String flowRef = location+"packages/ekaScheduler/cronJob/adapter/services/DML/insertScheduler.sql";
			  if(mainSqlJsonObject==null)
				  mainSqlJsonObject = Json.createReader(new FileInputStream(new File(flowRef))).readObject();
			}
		  Properties myProps=dataPipeline.getMyProperties();
		  dataPipeline.put("sqlLocalProperties",myProps);
		  SqlResolver.execute(dataPipeline,mainSqlJsonObject);
		}catch(Throwable e) {
			dataPipeline.clear();
			dataPipeline.put("error", e.getMessage());
			dataPipeline.setResponseStatus(500);
			dataPipeline.put("status", "SQL Service error");
			new SnippetException(dataPipeline,"Failed to execute Sql Java", new Exception(e));
		}
	}
}
