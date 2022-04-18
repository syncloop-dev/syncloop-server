package packages.middleware.pub.service;
import com.eka.middleware.service.DataPipeline;
import com.eka.middleware.service.ServiceUtils;
import com.eka.middleware.template.SnippetException;
public final class throwExecption{
	public static final void main(DataPipeline dataPipeline) throws SnippetException{
String msg=dataPipeline.getString("msg");
throw new SnippetException(dataPipeline,"Snippet exception", null);
	}

}