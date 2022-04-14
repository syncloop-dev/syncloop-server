package packages.middleware.pub.server.browse;
import com.eka.middleware.service.DataPipeline;
import com.eka.middleware.service.ServiceUtils; 
import com.eka.middleware.template.SnippetException;
public final class registerURLAlias{
	public static final void main(DataPipeline dataPipeline) throws SnippetException{
String resp=null;
	try{
		String fqn=dataPipeline.getString("fqn");
		String alias=dataPipeline.getString("alias");
		resp=ServiceUtils.registerURLAlias(fqn,alias);
		//dataPipeline.clear();
		dataPipeline.put("status",200);
		dataPipeline.put("msg",resp);
	}catch(Exception e){
		dataPipeline.clear();
		dataPipeline.put("status",500);
		if(resp==null)
			resp=e.getMessage();
		dataPipeline.put("msg",resp);
	}
	}

}