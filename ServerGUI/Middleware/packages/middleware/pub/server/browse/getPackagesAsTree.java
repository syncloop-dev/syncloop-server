package packages.middleware.pub.server.browse;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.eka.middleware.service.DataPipeline;
import com.eka.middleware.service.ServiceUtils;
import com.eka.middleware.template.SnippetException;
public final class getPackagesAsTree{
	public static final void main(DataPipeline dataPipeline) throws SnippetException{
try {
			String packageDir = ServiceUtils.getServerProperty("middleware.server.home.dir")+"packages/";
			File file = new File(packageDir);
			List<Map<String, Object>> children = new ArrayList<Map<String, Object>>();
			File fileList[] = file.listFiles();
			for (File fyle : fileList) {
				children.add(getTreeMap(fyle,"package"));
			}

			dataPipeline.clear();
			dataPipeline.put("text", "packages");
			dataPipeline.put("type", "root");
			dataPipeline.put("children", children);
		} catch (Exception e) {
			dataPipeline.clear();
			dataPipeline.put("response", e.getStackTrace());
		}
	}
private static Map<String, Object> getTreeMap(File file,String type) {
		Map<String, Object> map = new HashMap<String, Object>();
		String allowedTypes="service,map,doc,flow,package,root,folder,properties,jar,jdbc,sql"; 
		int indx = file.getName().lastIndexOf(".")+1;
		String fileType=file.getName().substring(indx);
		map.put("text", file.getName().replace("."+fileType, ""));
		if (file.isDirectory()) {
			map.put("type", type);
			List<Map<String, Object>> children = new ArrayList<Map<String, Object>>();
			File fileList[] = file.listFiles();
			for (File fyle : fileList) {
				int indx2 = fyle.getName().lastIndexOf(".")+1;
				String fileType2=fyle.getName().substring(indx2);
				if(allowedTypes.contains(fileType2) || fyle.isDirectory())
					children.add(getTreeMap(fyle,"folder"));
			}
			map.put("children", children);
		}else {
			if(indx!=0 && allowedTypes.contains(fileType))
				map.put("type", fileType);
		}
		return map;
	}
}