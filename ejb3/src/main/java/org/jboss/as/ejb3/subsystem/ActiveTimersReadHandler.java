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
package org.jboss.as.ejb3.subsystem;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.ejb3.timerservice.TimerServiceManager;
import org.jboss.as.ejb3.timerservice.TimerServiceService;
import org.jboss.as.ejb3.timerservice.mk2.CalendarTimer;
import org.jboss.as.ejb3.timerservice.mk2.TimerImpl;
import org.jboss.as.server.CurrentServiceContainer;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

import javax.ejb.Timer;

/**
 * @author Stuart Douglas
 */
public class ActiveTimersReadHandler implements OperationStepHandler {

    public static final ActiveTimersReadHandler INSTANCE = new ActiveTimersReadHandler();

    private ActiveTimersReadHandler() {
    }

    @Override
    public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
        ServiceController<?> controller = CurrentServiceContainer.getServiceContainer().getService(TimerServiceManager.SERVICE_NAME);
        ModelNode resultNode = context.getResult().get("timers").setEmptyList();
        if (controller == null) {
            context.completeStep();
            return;
        }
        final TimerServiceManager manager = (TimerServiceManager) controller.getValue();
        for (TimerServiceService service : manager.getTimerServices()) {
            for (Timer t : service.getTimers()) {
                TimerImpl timer = (TimerImpl) t;
                ModelNode r = new ModelNode();
                r.get("deployment").set(service.getDeploymentName());
                if (service.getSubDeploymentName() != null) {
                    r.get("sub-deployment").set(service.getSubDeploymentName());
                }
                r.get("component").set(service.getComponentName());
                r.get("id").set(timer.getId());
                if (t instanceof CalendarTimer) {
                    r.get("calendar").set(true);
                    CalendarTimer cal = (CalendarTimer) timer;
                    r.get("year").set(cal.getScheduleExpression().getYear());
                    r.get("dayOfMonth").set(cal.getScheduleExpression().getDayOfMonth());
                    r.get("dayOfWeek").set(cal.getScheduleExpression().getDayOfWeek());
                    r.get("hour").set(cal.getScheduleExpression().getHour());
                    r.get("minute").set(cal.getScheduleExpression().getMinute());
                    r.get("month").set(cal.getScheduleExpression().getMonth());
                    r.get("second").set(cal.getScheduleExpression().getSecond());
                    if (cal.getScheduleExpression().getStart() != null) {
                        r.get("start").set(cal.getScheduleExpression().getStart().getTime());
                    }
                    if (cal.getScheduleExpression().getEnd() != null) {
                        r.get("end").set(cal.getScheduleExpression().getEnd().getTime());
                    }
                    if (cal.getScheduleExpression().getTimezone() != null) {
                        r.get("timezone").set(cal.getScheduleExpression().getTimezone());
                    }
                } else {
                    r.get("interval").set(timer.getInterval());
                    if (timer.getInitialExpiration() != null) {
                        r.get("initialExpiration").set(timer.getInitialExpiration().getTime());
                    }
                }
                r.get("remainingTime").set(timer.getTimeRemaining());
                resultNode.add(r);
            }
        }

        context.completeStep();
    }
}
