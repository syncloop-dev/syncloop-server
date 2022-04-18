package packages.middleware.pub.service;
import com.eka.middleware.service.DataPipeline;
import com.eka.middleware.service.ServiceUtils;
import com.eka.middleware.template.SnippetException;
public final class debugLog{
	public static final void main(DataPipeline dataPipeline) throws SnippetException{
String msg=dataPipeline.getString("msg");
String log=dataPipeline.getString("log");

if(msg!=null)
	dataPipeline.log(msg);
if(log!=null)
	dataPipeline.log(log);



	}

}