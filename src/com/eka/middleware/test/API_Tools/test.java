package com.eka.middleware.test.API_Tools;

import com.eka.middleware.auth.AuthAccount;
import com.eka.middleware.service.DataPipeline;
import com.eka.middleware.service.PropertyManager;
import com.eka.middleware.service.RuntimePipeline;
import com.eka.middleware.template.Tenant;
import com.eka.middleware.test.TestConfigReader;

import java.util.UUID;

public class test {
    public static void main(String[] args) throws Exception {
//        PropertyManager.initConfig(new String[]{"/Volumes/Work/Syncloop_Code/syncloop-distributions/resources/config/"});
        PropertyManager.initConfig(new String[]{TestConfigReader.getProperty("middleware.server.test")});
        UUID coId = UUID.randomUUID();
        RuntimePipeline rp = RuntimePipeline.create(Tenant.getTempTenant("default"), "sfgwe4e5t", coId.toString(), null, "standalone",
                null);
        assert rp != null;
        DataPipeline dataPipeline = rp.dataPipeLine;
        String location = PropertyManager.getPackagePath(dataPipeline.rp.getTenant());
        System.out.println("fss" + location);
        AuthAccount authAccount = dataPipeline.getCurrentRuntimeAccount();
        System.out.println(authAccount);
    }
}
