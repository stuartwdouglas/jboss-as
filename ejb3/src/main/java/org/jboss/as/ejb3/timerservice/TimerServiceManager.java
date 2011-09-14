/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.ejb3.timerservice;

import org.jboss.msc.service.ServiceName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Manager class for active timers that is used by management operations.
 *
 * @author Stuart Douglas
 */
public class TimerServiceManager {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("ejb", "timerServiceManager");

    private final List<TimerServiceService> timerServices = new ArrayList<TimerServiceService>();

    public void addTimerService(TimerServiceService service) {
        synchronized (this) {
            timerServices.add(service);
            Collections.sort(timerServices, new Comparator<TimerServiceService>() {
                @Override
                public int compare(final TimerServiceService o1, final TimerServiceService o2) {
                    int val = o1.getDeploymentName().compareTo(o2.getDeploymentName());
                    if (val != 0) return val;
                    if (o1.getSubDeploymentName() != null && o2.getSubDeploymentName() != null) {
                        val = o1.getSubDeploymentName().compareTo(o2.getSubDeploymentName());
                        if (val != 0) return val;
                    } else if (o1.getSubDeploymentName() == null && o2.getSubDeploymentName() != null) {
                        return 1;
                    } else if (o2.getSubDeploymentName() == null && o1.getSubDeploymentName() != null) {
                        return -1;
                    }
                    return o1.getComponentName().compareTo(o2.getComponentName());
                }
            });
        }
    }

    public void removeTimerService(TimerServiceService service) {
        synchronized (this) {
            timerServices.remove(service);
        }
    }

    public List<TimerServiceService> getTimerServices() {
        synchronized (this) {
            return new ArrayList<TimerServiceService>(timerServices);
        }
    }
}
