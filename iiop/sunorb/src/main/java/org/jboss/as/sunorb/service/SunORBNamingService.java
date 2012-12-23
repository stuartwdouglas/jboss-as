package org.jboss.as.sunorb.service;

import com.sun.corba.se.impl.activation.NameServiceStartThread;
import com.sun.corba.se.impl.naming.pcosnaming.NameService;
import com.sun.corba.se.impl.orbutil.ORBConstants;
import org.jboss.as.controller.services.path.PathManager;
import org.jboss.as.iiop.service.CorbaNamingService;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import com.sun.corba.se.spi.orb.ORB;
import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.CosNaming.NamingContext;

import java.io.File;

/**
 * @author Stuart Douglas
 */
public class SunORBNamingService extends CorbaNamingService {

    @Override
    public void start(StartContext context) throws StartException {
        super.start(context);
        try {
            ((ORB)orbInjector.getValue()).register_initial_reference(ORBConstants.PERSISTENT_NAME_SERVICE_NAME, this.namingService);
        } catch (Exception e) {
            throw new StartException(e);
        }
    }
}
