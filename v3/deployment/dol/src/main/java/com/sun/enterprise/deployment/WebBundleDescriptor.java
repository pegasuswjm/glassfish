/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
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
 */
package com.sun.enterprise.deployment;

import com.sun.enterprise.deployment.node.web.WebBundleNode;
import com.sun.enterprise.deployment.runtime.web.SunWebApp;
import com.sun.enterprise.deployment.types.EjbReference;
import com.sun.enterprise.deployment.types.*;
import com.sun.enterprise.deployment.util.DescriptorVisitor;
import com.sun.enterprise.deployment.util.WebBundleVisitor;
import com.sun.enterprise.deployment.util.XModuleType;
import com.sun.enterprise.deployment.web.*;
import com.sun.enterprise.util.LocalStringManagerImpl;

import javax.enterprise.deploy.shared.ModuleType;
import java.util.*;

/**
 * I am an object that represents all the deployment information about
 * a web app [{0}]lication.
 *
 * @author Danny Coward
 */

public class WebBundleDescriptor extends BundleDescriptor
        implements WritableJndiNameEnvironment,
        ResourceReferenceContainer,
        ResourceEnvReferenceContainer,
        EjbReferenceContainer,
        MessageDestinationReferenceContainer,
        ServiceReferenceContainer

{


    private Set<WebComponentDescriptor> webComponentDescriptors;
    private int sessionTimeout;
    private Set<MimeMapping> mimeMappings;
    private Set<String> welcomeFiles;
    private Set<ErrorPageDescriptor> errorPageDescriptors;
    private Vector<AppListenerDescriptor> appListenerDescriptors;
    private Set<ContextParameter> contextParameters;
    private Set<EjbReference> ejbReferences;
    private Set<ResourceReferenceDescriptor> resourceReferences;
    private Set<JmsDestinationReferenceDescriptor> jmsDestReferences;
    private Set<MessageDestinationReferenceDescriptor> messageDestReferences;
    private Set<ServiceReferenceDescriptor> serviceReferences;

    private Set<LifecycleCallbackDescriptor> postConstructDescs =
            new HashSet<LifecycleCallbackDescriptor>();
    private Set<LifecycleCallbackDescriptor> preDestroyDescs =
            new HashSet<LifecycleCallbackDescriptor>();

    private Set<EntityManagerFactoryReferenceDescriptor>
            entityManagerFactoryReferences =
            new HashSet<EntityManagerFactoryReferenceDescriptor>();

    private Set<EntityManagerReferenceDescriptor>
            entityManagerReferences =
            new HashSet<EntityManagerReferenceDescriptor>();

    private boolean isDistributable;
    private Set<SecurityRoleDescriptor> securityRoles;
    private Set<SecurityConstraint> securityConstraints;
    private String contextRoot;
    private LoginConfiguration loginConfiguration;
    private Set<EnvironmentEntry> environmentEntries;
    private LocaleEncodingMappingListDescriptor localeEncodingMappingDesc = null;
    private JspConfigDescriptor jspConfigDescriptor = null;

    private Vector<ServletFilter> servletFilters = null;
    private Vector<ServletFilterMapping> servletFilterMappings = null;

    public static final int SESSION_TIMEOUT_DEFAULT = 30;
    private final static String DEPLOYMENT_DESCRIPTOR_DIR = "WEB-INF";

    private static LocalStringManagerImpl localStrings =
            new LocalStringManagerImpl(WebBundleDescriptor.class);

    private SunWebApp sunWebApp = null;

    /**
     * Constrct an empty web app [{0}].
     */
    public WebBundleDescriptor() {
        sessionTimeout = SESSION_TIMEOUT_DEFAULT;
    }

    /**
     * This method will not merge the contents of webComponents. It will take only one of them.
     * @param webBundleDescriptor
     */
    public void addWebBundleDescriptor(WebBundleDescriptor webBundleDescriptor) {
        super.addBundleDescriptor(webBundleDescriptor);

        for (WebComponentDescriptor webComponentDesc :webBundleDescriptor.getWebComponentDescriptors())
        {
            WebComponentDescriptor webComponentDescriptor =
                new WebComponentDescriptor(webComponentDesc);
            webComponentDescriptor.setWebBundleDescriptor(this);
            this.getWebComponentDescriptors().add(webComponentDescriptor);
        }

        addOtherInfo(webBundleDescriptor);
    }

    /**
     * This method will merge the contents of webComponents, too.
     * @param webBundleDescriptor
     */
    public void mergeWebBundleDescriptor(WebBundleDescriptor webBundleDescriptor) {
        super.addBundleDescriptor(webBundleDescriptor);

        for (WebComponentDescriptor webComponentDesc : webBundleDescriptor.getWebComponentDescriptors()) {
            WebComponentDescriptor webCompDesc =
                    this.getWebComponentByCanonicalName(webComponentDesc.getCanonicalName());
            if (webCompDesc == null) {
                this.getWebComponentDescriptors().add(new WebComponentDescriptor(webComponentDesc));
            } else {
                webCompDesc.add(webComponentDesc);
            }
        }

        addOtherInfo(webBundleDescriptor);
    }

    /**
     * This internal method add all info of given webBundleDescriptor except
     * webComponentDescriptors.
     * @param webBundleDescriptor
     */
    private void addOtherInfo(WebBundleDescriptor webBundleDescriptor) {

        getMimeMappingsSet().addAll(webBundleDescriptor.getMimeMappingsSet());
        getWelcomeFilesSet().addAll(webBundleDescriptor.getWelcomeFilesSet());
        getErrorPageDescriptorsSet().addAll(webBundleDescriptor.getErrorPageDescriptorsSet());
        getAppListeners().addAll(webBundleDescriptor.getAppListeners());
        getContextParametersSet().addAll(webBundleDescriptor.getContextParametersSet());
        getEjbReferenceDescriptors().addAll(webBundleDescriptor.getEjbReferenceDescriptors());
        getResourceReferenceDescriptors().addAll(webBundleDescriptor.getResourceReferenceDescriptors());
        getMessageDestinationReferenceDescriptors().addAll(webBundleDescriptor.getMessageDestinationReferenceDescriptors());
        getServiceReferenceDescriptors().addAll(webBundleDescriptor.getServiceReferenceDescriptors());
        getEnvironmentProperties().addAll(webBundleDescriptor.getEnvironmentProperties());
        getSecurityConstraintsSet().addAll(webBundleDescriptor.getSecurityConstraintsSet());

        // ServletFilters
        getServletFilters().addAll(webBundleDescriptor.getServletFilters());
        getServletFilterMappings().addAll(webBundleDescriptor.getServletFilterMappings());
        setLocaleEncodingMappingListDescriptor(webBundleDescriptor.getLocaleEncodingMappingListDescriptor());
        setJspConfigDescriptor(webBundleDescriptor.getJspConfigDescriptor());

        // WebServices
        WebServicesDescriptor thisWebServices = this.getWebServices();
        WebServicesDescriptor otherWebServices = webBundleDescriptor.getWebServices();
        for (WebService ws : otherWebServices.getWebServices()) {
            thisWebServices.addWebService(new WebService(ws));
        }

    }

    /**
     * @return the default version of the deployment descriptor
     *         loaded by this descriptor
     */
    public String getDefaultSpecVersion() {
        return WebBundleNode.SPEC_VERSION;
    }

    /**
     * Return the set of named descriptors that I have.
     */
    public Collection getNamedDescriptors() {
        return super.getNamedDescriptorsFrom(this);
    }

    /**
     * Return the saet of NamedReferencePairs that I have.
     */
    public Vector<NamedReferencePair> getNamedReferencePairs() {
        return super.getNamedReferencePairsFrom(this);
    }

    /**
     * return the name of my context root
     */
    public String getContextRoot() {
        if (getModuleDescriptor() != null && getModuleDescriptor().getContextRoot() != null) {
            return getModuleDescriptor().getContextRoot();
        }
        if (contextRoot == null) {
            contextRoot = "";
        }
        return contextRoot;
    }

    /**
     * Set the name of my context root.
     */
    public void setContextRoot(String contextRoot) {
        if (getModuleDescriptor() != null) {
            getModuleDescriptor().setContextRoot(contextRoot);
        }
        this.contextRoot = contextRoot;
    }

    /**
     * Return the Set of Web COmponent Descriptors (JSP or JavaServlets) in me.
     */
    public Set<WebComponentDescriptor> getWebComponentDescriptors() {
        if (webComponentDescriptors == null) {
            webComponentDescriptors = new OrderedSet<WebComponentDescriptor>();
        }
        return webComponentDescriptors;
    }

    /**
     * Adds a new Web Component Descriptor to me.
     */

    public void addWebComponentDescriptor(WebComponentDescriptor webComponentDescriptor) {

        webComponentDescriptor.setWebBundleDescriptor(this);
        for (WebComponentDescriptor wbd : getWebComponentDescriptors()) {
            if (wbd.getCanonicalName().equals(
                    webComponentDescriptor.getCanonicalName())) {
                // combine the contents of the two
                webComponentDescriptor.add(wbd);
                // remove the original one from the set
                // so we can add the new one
                getWebComponentDescriptors().remove(wbd);
                break;
            }
        }
        this.getWebComponentDescriptors().add(webComponentDescriptor);
    }

    /**
     * Remove the given web component from me.
     */

    public void removeWebComponentDescriptor(WebComponentDescriptor webComponentDescriptor) {
        webComponentDescriptor.setWebBundleDescriptor(null);
        getWebComponentDescriptors().remove(webComponentDescriptor);
    }

    /**
     * WEB SERVICES REF APIS
     */
    public boolean hasServiceReferenceDescriptors() {
        if (serviceReferences == null)
            return false;
        return serviceReferences.size() != 0;
    }

    public Set<ServiceReferenceDescriptor> getServiceReferenceDescriptors() {
        if (serviceReferences == null) {
            serviceReferences = new OrderedSet<ServiceReferenceDescriptor>();
        }
        return serviceReferences;
    }

    public void addServiceReferenceDescriptor(ServiceReferenceDescriptor
            serviceRef) {
        serviceRef.setBundleDescriptor(this);
        getServiceReferenceDescriptors().add(serviceRef);
    }

    public void removeServiceReferenceDescriptor(ServiceReferenceDescriptor
            serviceRef) {
        serviceRef.setBundleDescriptor(null);
        getServiceReferenceDescriptors().remove(serviceRef);
    }

    /**
     * Looks up an service reference with the given name.
     * Throws an IllegalArgumentException if it is not found.
     */
    public ServiceReferenceDescriptor getServiceReferenceByName(String name) {
        for (ServiceReferenceDescriptor srd : getServiceReferenceDescriptors()) {
            if (srd.getName().equals(name)) {
                return srd;
            }

        }
        throw new IllegalArgumentException(localStrings.getLocalString(
                "enterprise.deployment.exceptionwebapphasnoservicerefbyname",
                "This web app [{0}] has no service reference by the name of [{1}]",
                new Object[]{getName(), name}));
    }

    /**
     * @return the set of JMS destination references this ejb declares.
     */
    public Set<JmsDestinationReferenceDescriptor> getJmsDestinationReferenceDescriptors() {
        if (jmsDestReferences == null) {
            jmsDestReferences = new OrderedSet<JmsDestinationReferenceDescriptor>();
        }
        return jmsDestReferences;
    }

    /**
     * adds a JMS destination reference to the bundle
     */
    public void addJmsDestinationReferenceDescriptor(JmsDestinationReferenceDescriptor jmsDestReference) {
        getJmsDestinationReferenceDescriptors().add(jmsDestReference);
    }

    /**
     * removes a existing JMS destination reference from the bundle
     */
    public void removeJmsDestinationReferenceDescriptor(JmsDestinationReferenceDescriptor jmsDestReference) {
        getJmsDestinationReferenceDescriptors().remove(jmsDestReference);
    }

    /**
     * @return a JMS destination reference by the same name or throw an IllegalArgumentException.
     */
    public JmsDestinationReferenceDescriptor getJmsDestinationReferenceByName(String name) {
        for (JmsDestinationReferenceDescriptor jdr : getJmsDestinationReferenceDescriptors()) {
            if (jdr.getName().equals(name)) {
                return jdr;
            }
        }
        throw new IllegalArgumentException(localStrings.getLocalString(
                "enterprise.deployment.exceptionwebapphasnojmsdestrefbyname",
                "This web app [{0}] has no resource environment reference by the name of [{1}]", new Object[]{getName(), name}));
    }

    /**
     * @return the value in seconds of when requests should time out.
     */
    public int getSessionTimeout() {
        return sessionTimeout;
    }

    /**
     * Sets thew value in seconds after sessions should timeout.
     */
    public void setSessionTimeout(int sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }

    private Set<MimeMapping> getMimeMappingsSet() {
        if (mimeMappings == null) {
            mimeMappings = new HashSet<MimeMapping>();
        }
        return mimeMappings;
    }

    /**
     * Sets the Set of Mime Mappings for this web application.
     */
    public void setMimeMappings(Set<MimeMapping> mimeMappings) {
        this.mimeMappings = mimeMappings;
    }


    /**
     * @returns an enumeration of my mime mappings.
     */
    public Enumeration<MimeMapping> getMimeMappings() {
        return (new Vector(this.getMimeMappingsSet())).elements();
    }

    /**
     * @add the given mime mapping to my list.
     */
    public void addMimeMapping(MimeMapping mimeMapping) {
        // always override
        // Since Set.add API doesn't replace
        // remove the element first if it's already contained
        for (Iterator<MimeMapping> itr = getMimeMappingsSet().iterator(); itr.hasNext();) {
            MimeMapping mm = itr.next();
            if (mm.getExtension().equals(mimeMapping.getExtension())) {
                getMimeMappingsSet().remove(mm);
                break;
            }
        }
        getMimeMappingsSet().add(mimeMapping);
    }

    /**
     * add the given mime mapping to my list.
     */
    public void addMimeMapping(MimeMappingDescriptor mimeMapping) {
        addMimeMapping((MimeMapping) mimeMapping);
    }

    public void addLocaleEncodingMappingListDescriptor(LocaleEncodingMappingListDescriptor lemDesc) {
        localeEncodingMappingDesc = lemDesc;
    }

    public LocaleEncodingMappingListDescriptor getLocaleEncodingMappingListDescriptor() {
        return localeEncodingMappingDesc;
    }

    public void setLocaleEncodingMappingListDescriptor(LocaleEncodingMappingListDescriptor lemDesc) {
        localeEncodingMappingDesc = lemDesc;
    }

    /**
     * Removes the given mime mapping from my list.
     */
    public void removeMimeMapping(MimeMapping mimeMapping) {
        getMimeMappingsSet().remove(mimeMapping);
    }

    /**
     * Return an enumeration of the welcome files I have..
     */
    public Enumeration<String> getWelcomeFiles() {
        return (new Vector(this.getWelcomeFilesSet())).elements();
    }

    public Set<String> getWelcomeFilesSet() {
        if (welcomeFiles == null) {
            welcomeFiles = new OrderedSet<String>();
        }
        return welcomeFiles;
    }

    /**
     * Adds a new welcome file to my list.
     */
    public void addWelcomeFile(String fileUri) {
        getWelcomeFilesSet().add(fileUri);
    }

    /**
     * Removes a welcome file from my list.
     */
    public void removeWelcomeFile(String fileUri) {
        getWelcomeFilesSet().remove(fileUri);
    }

    /**
     * Sets the collection of my welcome files.
     */
    public void setWelcomeFiles(Set<String> welcomeFiles) {
        this.welcomeFiles = welcomeFiles;
    }

    private Set<ErrorPageDescriptor> getErrorPageDescriptorsSet() {
        if (errorPageDescriptors == null) {
            errorPageDescriptors = new HashSet<ErrorPageDescriptor>();
        }
        return errorPageDescriptors;
    }

    /**
     * Returns an enumeration of the error pages I have.
     */
    public Enumeration<ErrorPageDescriptor> getErrorPageDescriptors() {
        return (new Vector(getErrorPageDescriptorsSet())).elements();
    }

    /**
     * Adds a new error page to my list.
     */
    public void addErrorPageDescriptor(ErrorPageDescriptor errorPageDescriptor) {
        getErrorPageDescriptorsSet().add(errorPageDescriptor);
    }

    /**
     * Removes the given error page from my list.
     */
    public void removeErrorPageDescriptor(ErrorPageDescriptor errorPageDescriptor) {
        getErrorPageDescriptorsSet().remove(errorPageDescriptor);
    }

    /**
     * Search my error pages for one with thei given signifier or null if there isn't one.
     */
    public ErrorPageDescriptor getErrorPageDescriptorBySignifier(String signifier) {
        for (ErrorPageDescriptor next : getErrorPageDescriptorsSet()) {
            if (next.getErrorSignifierAsString().equals(signifier)) {
                return next;
            }
        }
        return null;
    }

    /**
     * @return the Set of my Context Parameters.
     */
    public Set<ContextParameter> getContextParametersSet() {
        if (contextParameters == null) {
            contextParameters = new OrderedSet<ContextParameter>();
        }
        return contextParameters;
    }

    /**
     * @return my Context Parameters in an enumeration.
     */
    public Enumeration<ContextParameter> getContextParameters() {
        return (new Vector(getContextParametersSet())).elements();
    }

    /**
     * Adds a new context parameter to my list.
     */
    public void addContextParameter(ContextParameter contextParameter) {
        getContextParametersSet().add(contextParameter);
    }

    /**
     * Adds a new context parameter to my list.
     */
    public void addContextParameter(EnvironmentProperty contextParameter) {
        addContextParameter((ContextParameter) contextParameter);
    }

    /**
     * Removes the given context parameter from my list.
     */
    public void removeContextParameter(ContextParameter contextParameter) {
        getContextParametersSet().remove(contextParameter);
    }

    /**
     * Return true if this web app [{0}] can be distributed across different processes.
     */

    public boolean isDistributable() {
        return isDistributable;
    }

    /**
     * Sets whether this web app [{0}] can be distributed across different processes.
     */
    public void setDistributable(boolean isDistributable) {
        this.isDistributable = isDistributable;
    }

    /**
     * Returns the enumeration of my references to Enterprise Beans.
     */

    public Enumeration<EjbReference> getEjbReferences() {
        return (new Vector(this.getEjbReferenceDescriptors())).elements();
    }

    /**
     * Returns the Set of my references to Enterprise Beans.
     */

    public Set<EjbReference> getEjbReferenceDescriptors() {
        if (ejbReferences == null) {
            ejbReferences = new OrderedSet<EjbReference>();
        }
        return ejbReferences;
    }

    /**
     * @return an Enterprise Bean with the matching name or throw.
     */

    public EjbReferenceDescriptor getEjbReferenceByName(String name) {
        return (EjbReferenceDescriptor) getEjbReference(name);
    }

    public EjbReference getEjbReference(String name) {
        for (EjbReference er : getEjbReferenceDescriptors()) {
            if (er.getName().equals(name)) {
                return er;
            }
        }
        throw new IllegalArgumentException(localStrings.getLocalString(
                "enterprise.deployment.exceptionwebapphasnoejbrefbyname",
                "This web app [{0}] has no ejb reference by the name of [{1}] ", new Object[]{getName(), name}));
    }

    /**
     * @return a reource reference with the matching name or throw.
     */

    public ResourceReferenceDescriptor getResourceReferenceByName(String name) {
        for (ResourceReference next : getResourceReferenceDescriptors()) {
            if (next.getName().equals(name)) {
                return (ResourceReferenceDescriptor) next;
            }
        }
        throw new IllegalArgumentException(localStrings.getLocalString(
                "enterprise.deployment.exceptionwebapphasnoresourcerefbyname",
                "This web app [{0}] has no resource reference by the name of [{1}]", new Object[]{getName(), name}));
    }

    /**
     * @returns my Set of references to resources.
     */

    public Set<ResourceReferenceDescriptor> getResourceReferenceDescriptors() {
        if (resourceReferences == null) {
            resourceReferences = new OrderedSet<ResourceReferenceDescriptor>();
        }
        return resourceReferences;
    }

    public Set<EntityManagerFactoryReferenceDescriptor>
    getEntityManagerFactoryReferenceDescriptors() {
        return entityManagerFactoryReferences;
    }

    /**
     * Return the entity manager factory reference descriptor corresponding to
     * the given name.
     */
    public EntityManagerFactoryReferenceDescriptor
    getEntityManagerFactoryReferenceByName(String name) {
        for (EntityManagerFactoryReferenceDescriptor next :
                getEntityManagerFactoryReferenceDescriptors()) {

            if (next.getName().equals(name)) {
                return next;
            }
        }
        throw new IllegalArgumentException(localStrings.getLocalString(
                "exceptionwebapphasnoentitymgrfactoryrefbyname",
                "This web app [{0}] has no entity manager factory reference by the name of [{1}]",
                new Object[]{getName(), name}));
    }

    public void addEntityManagerFactoryReferenceDescriptor
            (EntityManagerFactoryReferenceDescriptor reference) {
        reference.setReferringBundleDescriptor(this);
        this.getEntityManagerFactoryReferenceDescriptors().add(reference);
    }

    public Set<EntityManagerReferenceDescriptor>
    getEntityManagerReferenceDescriptors() {
        return entityManagerReferences;
    }

    /**
     * Return the entity manager factory reference descriptor corresponding to
     * the given name.
     */
    public EntityManagerReferenceDescriptor
    getEntityManagerReferenceByName(String name) {
        for (EntityManagerReferenceDescriptor next :
                getEntityManagerReferenceDescriptors()) {

            if (next.getName().equals(name)) {
                return next;
            }
        }
        throw new IllegalArgumentException(localStrings.getLocalString(
                "exceptionwebapphasnoentitymgrrefbyname",
                "This web app [{0}] has no entity manager reference by the name of [{1}]",
                new Object[]{getName(), name}));
    }

    public void addEntityManagerReferenceDescriptor
            (EntityManagerReferenceDescriptor reference) {
        reference.setReferringBundleDescriptor(this);
        getEntityManagerReferenceDescriptors().add(reference);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<? extends PersistenceUnitDescriptor>
           findReferencedPUs() {
        Collection<PersistenceUnitDescriptor> pus =
                new HashSet<PersistenceUnitDescriptor>(
                        findReferencedPUsViaPURefs(this));
        pus.addAll(findReferencedPUsViaPCRefs(this));
        if (extensions.containsKey(EjbBundleDescriptor.class)) {
            for (RootDeploymentDescriptor extension : extensions.get(EjbBundleDescriptor.class)) {
                pus.addAll(((EjbBundleDescriptor) extension).findReferencedPUs());
            }
        }
        return pus;
    }

    /**
     * Return my set of environment properties.
     */

    public Set getEnvironmentProperties() {
        return getEnvironmentEntrySet();
    }

    /**
     * Adds a new reference to an ejb.
     */

    public void addEjbReferenceDescriptor(EjbReference ejbReference) {
        getEjbReferenceDescriptors().add(ejbReference);
        ejbReference.setReferringBundleDescriptor(this);
    }

    /**
     * Adds a new reference to an ejb.
     */

    public void addEjbReferenceDescriptor(EjbReferenceDescriptor ejbReferenceDescriptor) {
        addEjbReferenceDescriptor((EjbReference) ejbReferenceDescriptor);
    }

    /**
     * Removes a reference to an ejb.
     */
    public void removeEjbReferenceDescriptor(EjbReferenceDescriptor ejbReferenceDescriptor) {
        removeEjbReferenceDescriptor((EjbReference) ejbReferenceDescriptor);
    }

    public void removeEjbReferenceDescriptor(EjbReference ejbReferenceDescriptor) {
        getEjbReferenceDescriptors().remove(ejbReferenceDescriptor);
        ejbReferenceDescriptor.setReferringBundleDescriptor(null);
    }

    /**
     * Return an enumeration of references to resources that I have.
     */
    public Enumeration<ResourceReferenceDescriptor> getResourceReferences() {
        return (new Vector(getResourceReferenceDescriptors())).elements();
    }

    /**
     * adds a new reference to a resource.
     */
    public void addResourceReferenceDescriptor(ResourceReferenceDescriptor resourceReference) {
        getResourceReferenceDescriptors().add(resourceReference);
    }

    /**
     * removes a reference to a resource.
     */
    public void removeResourceReferenceDescriptor(ResourceReferenceDescriptor resourceReference) {
        getResourceReferenceDescriptors().remove(resourceReference);
    }

    public Set<MessageDestinationReferenceDescriptor> getMessageDestinationReferenceDescriptors() {
        if (messageDestReferences == null) {
            messageDestReferences = new OrderedSet<MessageDestinationReferenceDescriptor>();
        }
        return messageDestReferences;
    }

    public void addMessageDestinationReferenceDescriptor
            (MessageDestinationReferenceDescriptor messageDestRef) {
        messageDestRef.setReferringBundleDescriptor(this);
        getMessageDestinationReferenceDescriptors().add(messageDestRef);
    }

    public void removeMessageDestinationReferenceDescriptor
            (MessageDestinationReferenceDescriptor msgDestRef) {
        getMessageDestinationReferenceDescriptors().remove(msgDestRef);
    }

    /**
     * Looks up an message destination reference with the given name.
     * Throws an IllegalArgumentException if it is not found.
     */
    public MessageDestinationReferenceDescriptor
    getMessageDestinationReferenceByName(String name) {
        for (MessageDestinationReferenceDescriptor mdr :
                getMessageDestinationReferenceDescriptors()) {
            if (mdr.getName().equals(name)) {
                return mdr;
            }
        }
        throw new IllegalArgumentException(localStrings.getLocalString(
                "exceptionwebapphasnomsgdestrefbyname",
                "This web app [{0}] has no message destination reference by the name of [{1}]",
                new Object[]{getName(), name}));
    }

    public Set<LifecycleCallbackDescriptor>
    getPostConstructDescriptors() {
        return postConstructDescs;
    }

    public void addPostConstructDescriptor(LifecycleCallbackDescriptor
            postConstructDesc) {
        String className = postConstructDesc.getLifecycleCallbackClass();
        boolean found = false;
        for (LifecycleCallbackDescriptor next :
                getPostConstructDescriptors()) {
            if (next.getLifecycleCallbackClass().equals(className)) {
                found = true;
                break;
            }
        }
        if (!found) {
            getPostConstructDescriptors().add(postConstructDesc);
        }
    }

    public LifecycleCallbackDescriptor getPostConstructDescriptorByClass(String className) {
        return getPostConstructDescriptorByClass(className, this);
    }

    public Set<LifecycleCallbackDescriptor> getPreDestroyDescriptors() {
        return preDestroyDescs;
    }

    public void addPreDestroyDescriptor(LifecycleCallbackDescriptor
            preDestroyDesc) {
        String className = preDestroyDesc.getLifecycleCallbackClass();
        boolean found = false;
        for (LifecycleCallbackDescriptor next :
                getPreDestroyDescriptors()) {
            if (next.getLifecycleCallbackClass().equals(className)) {
                found = true;
                break;
            }
        }
        if (!found) {
            getPreDestroyDescriptors().add(preDestroyDesc);
        }
    }

    public LifecycleCallbackDescriptor getPreDestroyDescriptorByClass(String className) {
        return getPreDestroyDescriptorByClass(className, this);
    }

    protected List<InjectionCapable> getInjectableResourcesByClass(String className,
                                  JndiNameEnvironment jndiNameEnv) {
        List<InjectionCapable> injectables =
                new LinkedList<InjectionCapable>();

        for (InjectionCapable next : getInjectableResources(jndiNameEnv)) {
            if (next.isInjectable()) {
                for (InjectionTarget target : next.getInjectionTargets()) {
                    if (target.getClassName().equals(className)) {
                        injectables.add(next);
                    }
                }
            }
        }

        if (((WebBundleDescriptor) jndiNameEnv).hasWebServices()) {
            // Add @Resource WebServiceContext present in endpoint impl class to the list of
            // injectable resources; We do this for servelt endpoint only because the actual 
            // endpoint impl class gets replaced by JAXWSServlet in web.xml and hence
            // will never be added as an injectable resource
            for (InjectionCapable next : getInjectableResources(this)) {
                if (next.isInjectable()) {
                    for (InjectionTarget target : next.getInjectionTargets()) {
                        Iterator<WebServiceEndpoint> epIter = getWebServices().getEndpoints().iterator();
                        while (epIter.hasNext()) {
                            String servletImplClass = epIter.next().getServletImplClass();
                            if (target.getClassName().equals(servletImplClass)) {
                                injectables.add(next);
                            }
                        }
                    }
                }
            }
        }
        return injectables;
    }

    public List<InjectionCapable> getInjectableResourcesByClass(String className) {
        return (getInjectableResourcesByClass(className, this));
    }

    public InjectionInfo getInjectionInfoByClass(String className) {
        return (getInjectionInfoByClass(className, this));
    }

    /**
     * Returns an Enumeration of my SecurityRole objects.
     */
    public Enumeration<SecurityRoleDescriptor> getSecurityRoles() {
        Vector<SecurityRoleDescriptor> securityRoles = new Vector<SecurityRoleDescriptor>();
        for (Iterator itr = super.getRoles().iterator(); itr.hasNext();) {
            Role r = (Role) itr.next();
            SecurityRoleDescriptor srd = new SecurityRoleDescriptor(r);
            securityRoles.add(srd);
        }
        return securityRoles.elements();
    }

    /**
     * Add a new abstrct role to me.
     */
    public void addSecurityRole(SecurityRole securityRole) {
        Role r = new Role(securityRole.getName());
        r.setDescription(securityRole.getDescription());
        super.addRole(r);
    }

    /**
     * Add a new abstrct role to me.
     */
    public void addSecurityRole(SecurityRoleDescriptor securityRole) {
        addSecurityRole((SecurityRole) securityRole);
    }

    /**
     * Return all the references by a given component (by name) to the given rolename.
     */
    public SecurityRoleReference getSecurityRoleReferenceByName(String compName, String roleName) {
        for (WebComponentDescriptor comp : getWebComponentDescriptors()) {
            if (!comp.getCanonicalName().equals(compName))
                continue;

            SecurityRoleReference r = comp.getSecurityRoleReferenceByName(roleName);
            if (r != null)
                return r;
        }

        return null;
    }


    private Set<SecurityConstraint> getSecurityConstraintsSet() {
        if (securityConstraints == null) {
            securityConstraints = new HashSet<SecurityConstraint>();
        }
        return securityConstraints;
    }

    /**
     * My list of security constraints.
     */
    public Enumeration<SecurityConstraint> getSecurityConstraints() {
        return (new Vector(this.getSecurityConstraintsSet())).elements();
    }

    public Collection<SecurityConstraint> getSecurityConstraintsForUrlPattern(String urlPattern) {
        Collection<SecurityConstraint> constraints = new HashSet<SecurityConstraint>();
        for (Iterator<SecurityConstraint> i = getSecurityConstraintsSet().iterator(); i.hasNext();) {
            SecurityConstraint next = i.next();
            boolean include = false;
            for (Enumeration wrc = next.getWebResourceCollections();
                 wrc.hasMoreElements();) {
                WebResourceCollection nextCol = (WebResourceCollection)
                        wrc.nextElement();
                for (Enumeration up = nextCol.getUrlPatterns();
                     up.hasMoreElements();) {
                    String nextPattern = (String) up.nextElement();
                    if ((urlPattern != null) && urlPattern.equals(nextPattern)) {
                        include = true;
                        break;
                    }
                }
                if (include) {
                    break;
                }
            }
            if (include) {
                constraints.add(next);
            }
        }
        return constraints;
    }

    /**
     * Add a new security constraint.
     */
    public void addSecurityConstraint(SecurityConstraint securityConstraint) {
        getSecurityConstraintsSet().add(securityConstraint);
    }

    /**
     * Add a new security constraint.
     */
    public void addSecurityConstraint(SecurityConstraintImpl securityConstraint) {
        addSecurityConstraint((SecurityConstraint) securityConstraint);
    }

    /**
     * Remove the given security constraint.
     */
    public void removeSecurityConstraint(SecurityConstraint securityConstraint) {
        getSecurityConstraintsSet().remove(securityConstraint);
    }



    public JspConfigDescriptor getJspConfigDescriptor() {
        return jspConfigDescriptor;
    }

    public void setJspConfigDescriptor(JspConfigDescriptor jspC) {
        jspConfigDescriptor = jspC;
    }

    /*
     * @return my set of servlets
    */

    public Set<WebComponentDescriptor> getServletDescriptors() {
        Set<WebComponentDescriptor> servletDescriptors = new HashSet<WebComponentDescriptor>();
        for (WebComponentDescriptor next : getWebComponentDescriptors()) {
            if (next.isServlet()) {
                servletDescriptors.add(next);
            }
        }
        return servletDescriptors;
    }

    /**
     * @return my Set of jsps.
     */
    public Set<WebComponentDescriptor> getJspDescriptors() {
        Set<WebComponentDescriptor> jspDescriptors = new HashSet<WebComponentDescriptor>();
        for (WebComponentDescriptor next : getWebComponentDescriptors()) {
            if (!next.isServlet()) {
                jspDescriptors.add(next);
            }
        }
        return jspDescriptors;
    }

    public Set<EnvironmentEntry> getEnvironmentEntrySet() {
        if (environmentEntries == null) {
            environmentEntries = new OrderedSet<EnvironmentEntry>();
        }
        return environmentEntries;
    }

    /**
     * Return my set of environment properties.
     */
    public Enumeration<EnvironmentEntry> getEnvironmentEntries() {
        return (new Vector(this.getEnvironmentEntrySet())).elements();
    }

    /**
     * Adds this given environment property to my list.
     */
    public void addEnvironmentEntry(EnvironmentEntry environmentEntry) {
        getEnvironmentEntrySet().add(environmentEntry);
    }

    /**
     * Returns the environment property object searching on the supplied key.
     * throws an illegal argument exception if no such environment property exists.
     */
    public EnvironmentProperty getEnvironmentPropertyByName(String name) {
        for (EnvironmentEntry ev : getEnvironmentEntrySet()) {
            if (ev.getName().equals(name)) {
                return (EnvironmentProperty) ev;
            }
        }
        throw new IllegalArgumentException(localStrings.getLocalString(
                "enterprise.deployment.exceptionwebapphasnoenvpropertybyname",
                "This web app [{0}] has no environment property by the name of [{1}]",
                new Object[]{getName(), name}));
    }

    /**
     * Removes this given environment property from my list.
     */
    public void removeEnvironmentProperty(EnvironmentProperty environmentProperty) {
        getEnvironmentEntrySet().remove(environmentProperty);
    }

    /**
     * Adds this given environment property to my list.
     */
    public void addEnvironmentProperty(EnvironmentProperty environmentProperty) {
        getEnvironmentEntrySet().add(environmentProperty);
    }

    /**
     * Removes this given environment property from my list.
     */
    public void removeEnvironmentEntry(EnvironmentEntry environmentEntry) {
        getEnvironmentEntrySet().remove(environmentEntry);
    }

    /**
     * Return the information about how I should log in.
     */
    public LoginConfiguration getLoginConfiguration() {
        return loginConfiguration;
    }

    /**
     * Specifies the information about how I should log in.
     */
    public void setLoginConfiguration(LoginConfiguration loginConfiguration) {
        this.loginConfiguration = loginConfiguration;
    }

    public void setLoginConfiguration(LoginConfigurationImpl loginConfiguration) {
        setLoginConfiguration((LoginConfiguration) loginConfiguration);
    }

    /**
     * Search for a web component that I have by name.
     */
    public WebComponentDescriptor getWebComponentByName(String name) {
        for (WebComponentDescriptor next : getWebComponentDescriptors()) {
            if (next.getName().equals(name)) {
                return (WebComponentDescriptor) next;
            }
        }
        return null;
    }

    /**
     * Search for a web component that I have by name.
     */
    public WebComponentDescriptor getWebComponentByCanonicalName(String name) {
        for (WebComponentDescriptor next : getWebComponentDescriptors()) {
            if (next.getCanonicalName().equals(name)) {
                return (WebComponentDescriptor) next;
            }
        }
        return null;
    }

    /**
     * @return a set of web component descriptor of given impl name.
     */
    public WebComponentDescriptor[] getWebComponentByImplName(String name) {
        ArrayList<WebComponentDescriptor> webCompList =
                new ArrayList<WebComponentDescriptor>();
        for (WebComponentDescriptor webComp : getWebComponentDescriptors()) {
            if (webComp.getWebComponentImplementation().equals(name)) {
                webCompList.add(webComp);
            }
        }
        return webCompList.toArray(new WebComponentDescriptor[webCompList.size()]);
    }

    /* ----
    */

    /**
     * @return a Vector of servlet filters that I have.
     */
    public Vector<ServletFilter> getServletFilters() {
        if (servletFilters == null) {
            servletFilters = new Vector<ServletFilter>();
        }
        return servletFilters;
    }

    /**
     * @return a Vector of servlet filters that I have.
     */
    public Vector getServletFilterDescriptors() {
        return (Vector) getServletFilters().clone();
    }

    /**
     * Adds a servlet filter to this web component.
     */
    public void addServletFilter(ServletFilter ref) {
        if (!getServletFilters().contains(ref)) {
            getServletFilters().addElement(ref);
        }
    }

    public void addServletFilter(ServletFilterDescriptor ref) {
        addServletFilter((ServletFilter) ref);
    }

    /**
     * Removes the given servlet filter from this web component.
     */
    public void removeServletFilter(ServletFilter ref) {
        removeVectorItem(getServletFilters(), ref);
    }

    /* ----
    */

    /**
     * Return a Vector of servlet filters that I have.
     */
    public Vector<ServletFilterMapping> getServletFilterMappings() {
        if (servletFilterMappings == null) {
            servletFilterMappings = new Vector<ServletFilterMapping>();
        }
        return servletFilterMappings;
    }

    /**
     * Return a Vector of servlet filter mappings that I have.
     */
    public Vector<ServletFilterMapping> getServletFilterMappingDescriptors() {
        return (Vector<ServletFilterMapping>) getServletFilterMappings().clone();
    }

    /**
     * Adds a servlet filter mapping to this web component.
     */
    public void addServletFilterMapping(ServletFilterMapping ref) {
        if (!getServletFilterMappings().contains(ref)) {
            getServletFilterMappings().addElement(ref);
        }
    }

    /**
     * Adds a servlet filter mapping to this web component.
     */
    public void addServletFilterMapping(ServletFilterMappingDescriptor ref) {
        addServletFilterMapping((ServletFilterMapping) ref);
    }

    /**
     * Removes the given servlet filter mapping from this web component.
     */
    public void removeServletFilterMapping(ServletFilterMapping ref) {
        removeVectorItem(getServletFilterMappings(), ref);
    }

    /**
     * * Moves the given servlet filter mapping to a new relative location in
     * * the list
     */
    public void moveServletFilterMapping(ServletFilterMapping ref, int relPos) {
        moveVectorItem(getServletFilterMappings(), ref, relPos);
    }

    /* ----
    */

    private Vector<AppListenerDescriptor> getAppListeners() {
        if (appListenerDescriptors == null) {
            appListenerDescriptors = new Vector<AppListenerDescriptor>();
        }
        return appListenerDescriptors;
    }

    public Vector<AppListenerDescriptor> getAppListenerDescriptors() {
        return (Vector<AppListenerDescriptor>) getAppListeners().clone();
    }

    public void setAppListeners(Collection<? extends AppListenerDescriptor> c) {
        getAppListeners().clear();
        getAppListeners().addAll(c);
    }

    public void addAppListenerDescriptor(AppListenerDescriptor ref) {
        if (!getAppListeners().contains(ref)) {
            getAppListeners().addElement(ref);
        }
    }

    public void addAppListenerDescriptor(AppListenerDescriptorImpl ref) {
        addAppListenerDescriptor((AppListenerDescriptor) ref);
    }

    public void removeAppListenerDescriptor(AppListenerDescriptor ref) {
        removeVectorItem(getAppListeners(), ref);
    }

    public void moveAppListenerDescriptor(AppListenerDescriptor ref,
                                          int relPos) {
        this.moveVectorItem(this.getAppListeners(), ref, relPos);
    }

    /**
     * @return true if this bundle descriptor defines web service clients
     */
    public boolean hasWebServiceClients() {
        return !getServiceReferenceDescriptors().isEmpty();
    }

    /**
     * End of Web-Services related API
     */

    /* ----
    */

    /**
     * remove a specific object from the given list (does not rely on 'equals')
     */
    protected boolean removeVectorItem(Vector<? extends Object> list, Object ref) {
        for (Iterator<? extends Object> i = list.iterator(); i.hasNext();) {
            if (ref == i.next()) {
                i.remove();
                return true;
            }
        }
        return false;
    }

    /**
     * Moves the given object to a new relative location in the specified list
     */
    protected void moveVectorItem(Vector list, Object ref, int rpos) {

        /* get current position of ref */
        // 'indexOf' is not used because it is base on 'equals()' which may
        // not be unique.
        int size = list.size(), old_pos = size - 1;
        for (; old_pos >= 0; old_pos--) {
            if (ref == list.elementAt(old_pos)) {
                break;
            }
        }
        if (old_pos < 0) {
            return; // not found
        }

        /* limit up/down movement */
        int new_pos = old_pos + rpos;
        if (new_pos < 0) {
            new_pos = 0; // limit movement
        } else if (new_pos >= size) {
            new_pos = size - 1; // limit movement
        }

        /* is it really moving? */
        if (new_pos == old_pos) {
            return; // it's not moving
        }

        /* move it */
        list.removeElementAt(old_pos);
        list.insertElementAt(ref, new_pos);


    }

    /**
     * visitor API implementation
     */
    public void visit(DescriptorVisitor aVisitor) {
        if (aVisitor instanceof WebBundleVisitor) {
            visit((WebBundleVisitor) aVisitor);
        } else {
            super.visit(aVisitor);
        }
    }

    /**
     * visitor API implementation
     */
    public void visit(WebBundleVisitor aVisitor) {
        super.visit(aVisitor);
        aVisitor.accept(this);

        // Visit all injectables first.  In some cases, basic type information
        // has to be derived from target inject method or inject field.
        for (InjectionCapable injectable : getInjectableResources(this)) {
            aVisitor.accept(injectable);
        }

        for (Iterator<WebComponentDescriptor> i = getWebComponentDescriptors().iterator(); i.hasNext();) {
            WebComponentDescriptor aWebComp = i.next();
            aVisitor.accept(aWebComp);
        }
        for (Iterator<WebService> itr = getWebServices().getWebServices().iterator();
             itr.hasNext();) {
            WebService aWebService = itr.next();
            aVisitor.accept(aWebService);
        }

        for (Iterator<EjbReference> itr = getEjbReferenceDescriptors().iterator(); itr.hasNext();) {
            EjbReference aRef = itr.next();
            aVisitor.accept(aRef);
        }
        for (Iterator<ResourceReferenceDescriptor> itr = getResourceReferenceDescriptors().iterator();
             itr.hasNext();) {
            ResourceReferenceDescriptor next =
                    itr.next();
            aVisitor.accept(next);
        }
        for (Iterator<JmsDestinationReferenceDescriptor> itr = getJmsDestinationReferenceDescriptors().iterator();
             itr.hasNext();) {
            JmsDestinationReferenceDescriptor next =
                    itr.next();
            aVisitor.accept(next);
        }
        for (Iterator<MessageDestinationReferenceDescriptor> itr = getMessageDestinationReferenceDescriptors().iterator();
             itr.hasNext();) {
            MessageDestinationReferencer next =
                    itr.next();
            aVisitor.accept(next);
        }
        for (Iterator itr = getMessageDestinations().iterator();
             itr.hasNext();) {
            MessageDestinationDescriptor msgDestDescriptor =
                    (MessageDestinationDescriptor) itr.next();
            aVisitor.accept(msgDestDescriptor);
        }
        for (Iterator<ServiceReferenceDescriptor> itr = getServiceReferenceDescriptors().iterator();
             itr.hasNext();) {
            aVisitor.accept(itr.next());
        }
        for (Iterator itr = getServletFilterDescriptors().iterator();
             itr.hasNext();) {
            ServletFilterDescriptor servletFilterDescriptor =
                    (ServletFilterDescriptor) itr.next();
            aVisitor.accept(servletFilterDescriptor);
        }
    }

    /* ----
    */

    /**
     * Return a formatted version as a String.
     */
    public void print(StringBuffer toStringBuffer) {
        toStringBuffer.append("\nWeb Bundle descriptor");
        toStringBuffer.append("\n");
        super.print(toStringBuffer);
        toStringBuffer.append("\n context root ").append(getContextRoot());
        toStringBuffer.append("\n sessionTimeout ").append(sessionTimeout);
        toStringBuffer.append("\n mimeMappings ").append(mimeMappings);
        toStringBuffer.append("\n welcomeFiles ").append(welcomeFiles);
        toStringBuffer.append("\n errorPageDescriptors ").append(errorPageDescriptors);
        toStringBuffer.append("\n appListenerDescriptors ").append(appListenerDescriptors);
        toStringBuffer.append("\n contextParameters ").append(contextParameters);
        toStringBuffer.append("\n ejbReferences ");
        if (ejbReferences != null)
            printDescriptorSet(ejbReferences, toStringBuffer);
        toStringBuffer.append("\n jmsDestReferences ");
        if (jmsDestReferences != null)
            printDescriptorSet(jmsDestReferences, toStringBuffer);
        toStringBuffer.append("\n messageDestReferences ");
        if (messageDestReferences != null)
            printDescriptorSet(messageDestReferences, toStringBuffer);
        toStringBuffer.append("\n resourceReferences ");
        if (resourceReferences != null)
            printDescriptorSet(resourceReferences, toStringBuffer);
        toStringBuffer.append("\n serviceReferences ");
        if (serviceReferences != null)
            printDescriptorSet(serviceReferences, toStringBuffer);
        toStringBuffer.append("\n isDistributable ").append(isDistributable);
        toStringBuffer.append("\n securityRoles ").append(securityRoles);
        toStringBuffer.append("\n securityConstraints ").append(securityConstraints);
        toStringBuffer.append("\n contextRoot ").append(contextRoot);
        toStringBuffer.append("\n loginConfiguration ").append(this.loginConfiguration);
        toStringBuffer.append("\n webComponentDescriptors ");
        if (webComponentDescriptors != null)
            printDescriptorSet(webComponentDescriptors, toStringBuffer);
        toStringBuffer.append("\n environmentEntries ");
        if (environmentEntries != null)
            printDescriptorSet(environmentEntries, toStringBuffer);
        if (sunWebApp != null) {
            toStringBuffer.append("\n ========== Runtime Descriptors =========");
            toStringBuffer.append("\n").append(sunWebApp.toString());
        }
    }

    private void printDescriptorSet(Set descSet, StringBuffer sbuf) {
        if (descSet == null)
            return;
        for (Iterator itr = descSet.iterator(); itr.hasNext();) {
            Object obj = itr.next();
            if (obj instanceof Descriptor)
                ((Descriptor) obj).print(sbuf);
            else
                sbuf.append(obj);
        }
    }

    /**
     * @return the module type for this bundle descriptor
     */
    public XModuleType getModuleType() {
        return XModuleType.WAR;
    }

    /**
     * @return the deployment descriptor directory location inside
     *         the archive file
     */
    public String getDeploymentDescriptorDir() {
        return DEPLOYMENT_DESCRIPTOR_DIR;
    }

    /***********************************************************************************************
     * START
     * Deployment Consolidation to Suppport Multiple Deployment API Clients
     * Methods: setSunDescriptor, getSunDescriptor
     ***********************************************************************************************/

    /**
     * This returns the extra web sun specific info not in the RI DID.
     *
     * @return object representation of web deployment descriptor
     */
    public SunWebApp getSunDescriptor() {
        if (sunWebApp == null) {
            sunWebApp = new SunWebApp();
        }
        return sunWebApp;
    }

    /**
     * This sets the extra web sun specific info not in the RI DID.
     *
     * @param webApp SunWebApp object representation of web deployment descriptor
     */
    public void setSunDescriptor(SunWebApp webApp) {
        this.sunWebApp = webApp;
    }

    /*******************************************************************************************
     * END
     * Deployment Consolidation to Suppport Multiple Deployment API Clients
     *******************************************************************************************/ 
}
    
