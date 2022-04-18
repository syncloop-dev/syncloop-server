package packages.ekaScheduler.cronJob.handler;

import com.eka.middleware.service.DataPipeline;
import com.eka.middleware.service.ServiceUtils;
import com.eka.middleware.template.SnippetException;
import com.eka.middleware.flow.FlowResolver;
import java.io.File;
import java.io.FileInputStream;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;

public final class startJob {

	static JsonObject mainflowJsonObject=null;
	static final String syncBlock=new String("sync");
	public static final void main(DataPipeline dataPipeline) throws SnippetException{
//		String path="D:/Middleware/packages/testFlowServices/services/flow/simpleFlow.flow";
		try{
		  if(mainflowJsonObject==null)
			synchronized(syncBlock){
			  String location = ServiceUtils.getPackagesPath();
			  String flowRef = location+"packages/ekaScheduler/cronJob/handler/startJob.flow";
			  if(mainflowJsonObject==null)
				  mainflowJsonObject = Json.createReader(new FileInputStream(new File(flowRef))).readObject();
			}
		  FlowResolver.execute(dataPipeline,mainflowJsonObject);
		}catch(Throwable e) {
			dataPipeline.clear();
			dataPipeline.put("error", e.getMessage());
			dataPipeline.setResponseStatus(500);
			dataPipeline.put("status", "Service error");
			new SnippetException(dataPipeline,"Failed to execute simpleFlowJava", new Exception(e));
		}
	}
}
