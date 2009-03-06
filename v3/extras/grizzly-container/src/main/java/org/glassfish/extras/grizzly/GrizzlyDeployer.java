/*
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2007-2008 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License. You can obtain
 * a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
 * or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.  If applicable, add the following below the License
 * Header, with the fields enclosed by brackets [] replaced by your own
 * identifying information: "Portions Copyrighted [year]
 * [name of copyright owner]"
 *
 * Contributor(s):
 *
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 *
 */
package org.glassfish.extras.grizzly;

import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.annotations.Inject;
import org.glassfish.api.deployment.Deployer;
import org.glassfish.api.deployment.MetaData;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.container.RequestDispatcher;

import java.util.Map;
import java.util.LinkedList;
import java.util.logging.Level;

import com.sun.logging.LogDomains;

/**
 * @author Jerome Dochez
 */
@Service(name="grizzly")
public class GrizzlyDeployer implements Deployer<GrizzlyContainer, GrizzlyApp> {

    @Inject
    RequestDispatcher dispatcher;
    
    public MetaData getMetaData() {
        return new MetaData(false, new Class[] { GrizzlyModuleDescriptor.class}, null);
    }

    public <V> V loadMetaData(Class<V> type, DeploymentContext context) {
        return type.cast(new GrizzlyModuleDescriptor(context.getSource(), context.getLogger()));
    }

    public boolean prepare(DeploymentContext context) {
        return true;
    }

    public GrizzlyApp load(GrizzlyContainer container, DeploymentContext context) {

        GrizzlyModuleDescriptor configs = context.getModuleMetaData(GrizzlyModuleDescriptor.class);

        LinkedList<GrizzlyApp.Adapter> modules = new LinkedList<GrizzlyApp.Adapter>();
        for (Map.Entry<String, String> config : configs.getAdapters().entrySet()) {
            com.sun.grizzly.tcp.Adapter adapter;
            try {
                Class adapterClass = context.getClassLoader().loadClass(config.getValue());
                adapter = com.sun.grizzly.tcp.Adapter.class.cast(adapterClass.newInstance());
            } catch(Exception e) {
                context.getLogger().log(Level.SEVERE, e.getMessage(),e);
                return null;
            }
            modules.add(new GrizzlyApp.Adapter(config.getKey(), adapter));
        }
        return new GrizzlyApp(modules, dispatcher, context.getClassLoader());

    }

    public void unload(GrizzlyApp appContainer, DeploymentContext context) {
    }

    public void clean(DeploymentContext context) {
    }
}
