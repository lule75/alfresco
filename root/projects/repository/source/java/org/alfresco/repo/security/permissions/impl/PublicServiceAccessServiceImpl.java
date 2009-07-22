/*
 * Copyright (C) 2005-2007 Alfresco Software Limited.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.

 * As a special exception to the terms and conditions of version 2.0 of
 * the GPL, you may redistribute this Program in connection with Free/Libre
 * and Open Source Software ("FLOSS") applications as described in Alfresco's
 * FLOSS exception.  You should have recieved a copy of the text describing
 * the FLOSS exception, and it is also available here:
 * http://www.alfresco.com/legal/licensing
 */
package org.alfresco.repo.security.permissions.impl;

import java.lang.reflect.Method;

import org.alfresco.repo.security.permissions.impl.acegi.MethodSecurityInterceptor;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.cmr.security.PublicServiceAccessService;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.framework.ReflectiveMethodInvocation;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ListableBeanFactory;

public class PublicServiceAccessServiceImpl implements PublicServiceAccessService, BeanFactoryAware
{

    private ListableBeanFactory beanFactory;

    public AccessStatus hasAccess(String publicService, String methodName, Object... args)
    {
        Object interceptor = beanFactory.getBean(publicService + "_security");
        if (interceptor == null)
        {
            throw new UnsupportedOperationException("Unknown public service security implementation " + publicService);
        }
        if (interceptor instanceof AlwaysProceedMethodInterceptor)
        {
            return AccessStatus.ALLOWED;
        }

        if (interceptor instanceof MethodSecurityInterceptor)
        {
            MethodSecurityInterceptor msi = (MethodSecurityInterceptor) interceptor;

            MethodInvocation methodInvocation = null;
            Object publicServiceImpl = beanFactory.getBean(publicService);
            for (Method method : publicServiceImpl.getClass().getMethods())
            {
                if (method.getName().equals(methodName))
                {
                    if (method.getParameterTypes().length == args.length)
                    {
                        methodInvocation = new ReflectiveMethodInvocation(null, null, method, args, null, null);
                    }
                }
            }

            if (methodInvocation == null)
            {
                throw new UnsupportedOperationException("Unknown public service security implementation " + publicService + "." + methodName);
            }

            return msi.pre(methodInvocation);
        }
        throw new UnsupportedOperationException("Unknown security interceptor "+interceptor.getClass());
    }

    public void setBeanFactory(BeanFactory beanFactory) throws BeansException
    {
        this.beanFactory = (ListableBeanFactory) beanFactory;
    }

}