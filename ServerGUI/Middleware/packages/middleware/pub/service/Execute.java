package packages.middleware.pub.service;

import com.eka.middleware.service.DataPipeline;

public class Execute {
public static void main(DataPipeline dataPipeline) throws Exception{
	String fqn=dataPipeline.getPathParameters().get("fqn").toString();
	//dataPipeline.clear();
	dataPipeline.apply(fqn);
}
}
