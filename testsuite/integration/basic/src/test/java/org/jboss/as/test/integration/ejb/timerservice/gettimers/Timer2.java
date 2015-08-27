package org.jboss.as.test.integration.ejb.timerservice.gettimers;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.ejb.TimedObject;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * @author Stuart Douglas
 */
@Stateless
public class Timer2 implements TimedObject {

    public static final LinkedBlockingDeque<Object> DATA = new LinkedBlockingDeque<>();

    @Resource
    public TimerService timerService;

    public void start(String msg) {
        for(Timer t : timerService.getTimers()) {
            t.cancel();
        }
        timerService.createSingleActionTimer(1000, new TimerConfig(msg, false));
    }

    @Override
    public void ejbTimeout(Timer timer) {
        DATA.add(timer.getInfo());
    }
}
