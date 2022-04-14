package packages.middleware.pub.server.browse;
import com.eka.middleware.service.DataPipeline;
import com.eka.middleware.service.ServiceUtils;
import com.eka.middleware.template.SnippetException; 
public final class getURLAlias{
	public static final void main(DataPipeline dataPipeline) throws SnippetException{
try{
	String fqn=dataPipeline.getString("fqn");
	String alias=ServiceUtils.getURLAlias(fqn);
	dataPipeline.clear();
	if(alias==null){
		dataPipeline.put("status",404);
		dataPipeline.put("msg","Not found");
	}else{
		dataPipeline.put("alias",alias);
	}
}catch(Throwable e){
	dataPipeline.clear();
	dataPipeline.put("status",500);
	dataPipeline.put("msg",e.getMessage());
	dataPipeline.logException(e);
}
	}

}