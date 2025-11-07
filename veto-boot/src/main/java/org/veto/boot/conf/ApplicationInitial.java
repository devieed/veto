package org.veto.boot.conf;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.veto.core.common.ServiceConfig;
import org.veto.shared.Util;

@Component
@Slf4j
public class ApplicationInitial implements ApplicationRunner, DisposableBean {
    @Resource
    private ServiceConfig serviceConfig;

    @Resource
    private ServiceScheduled serviceScheduled;

    @Override
    public void destroy(){

    }

    @Override
    public void run(ApplicationArguments args){
        chacking();
        log.info("initial success");
        serviceScheduled.updateContests();
    }

    private void chacking(){
        if (Util.isAnyBlank(serviceConfig.getALCHEMY_API_KEY().getVal(),  serviceConfig.getALCHEMY_AUTH_TOKEN().getVal(), serviceConfig.getSYSTEM_DOMAIN().getVal())) {
            throw new RuntimeException("initial error");
        }
    }
}
