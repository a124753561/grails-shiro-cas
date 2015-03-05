import grails.spring.BeanBuilder
import org.apache.shiro.cas.CasSubjectFactory
import org.apache.shiro.cas.grails.DynamicServerNameCasFilter
import org.apache.shiro.cas.grails.ShiroCasConfigUtils
import org.jasig.cas.client.session.SingleSignOutFilter
import org.jasig.cas.client.session.SingleSignOutHttpSessionListener
import org.jasig.cas.client.validation.Cas20ServiceTicketValidator

class ShiroCasGrailsPlugin {
    def version = "0.5.2-SNAPSHOT"
    def grailsVersion = "2.0 > *"
    def loadAfter = ["shiro"]
    def title = "Shiro CAS Plugin"
    def description = 'Enables Grails applications to use JASIG CAS for single sign-on with Apache Shiro'
    def documentation = "https://github.com/commercehub-oss/grails-shiro-cas/blob/master/README.md"
    def license = "APACHE"
    def developers = [
        [name: "David M. Carr", email: "dcarr@commercehub.com"],
        [name: "Ford Guo", email: "agile.guo@gmail.com"],
        [name: "Gavin Hogan", email: "gavin@commercehub.com"],
        [name: "Curt Hostetter", email: "hostetcl@gmail.com"],
        [name: "Jonathan Chapman", email: "jonathan@enablelabs.com"],
    ]
    def organization = [ name: "CommerceHub", url: "http://www.commercehub.com/" ]
    def issueManagement = [ system: "GitHub", url: "https://github.com/commercehub-oss/grails-shiro-cas/issues" ]
    def scm = [ url: "https://github.com/commercehub-oss/grails-shiro-cas/" ]

    def doWithSpring = {
        def securityConfig = application.config.security.shiro
        def beanBuilder = delegate as BeanBuilder
        ShiroCasConfigUtils.initialize(application.config)
        casTicketValidator(Cas20ServiceTicketValidator, ShiroCasConfigUtils.serverUrl) {
            encoding = "UTF-8"
        }
        casSubjectFactory(CasSubjectFactory)
        def shiroSecurityManager = beanBuilder.getBeanDefinition("shiroSecurityManager")
        shiroSecurityManager.propertyValues.add("subjectFactory", casSubjectFactory)
        if (!securityConfig.filter.config) {

            casFilter(DynamicServerNameCasFilter)

            if (!ShiroCasConfigUtils.singleSignOutDisabled) {
                singleSignOutFilter(SingleSignOutFilter) { bean ->
                    ignoreInitConfiguration = true
                    artifactParameterName = ShiroCasConfigUtils.singleSignOutArtifactParameterName
                    logoutParameterName = ShiroCasConfigUtils.singleSignOutLogoutParameterName
                }
            }

            def shiroFilter = beanBuilder.getBeanDefinition("shiroFilter")
            shiroFilter.propertyValues.addPropertyValue("filterChainDefinitions", ShiroCasConfigUtils.shiroCasFilter)
        }
    }

    def doWithWebDescriptor = { xml ->
        // This block is called only at build-time, not at run-time.
        // Thus, any externalized configuration will not be taken into account.
        if (!ShiroCasConfigUtils.singleSignOutDisabled) {
            def priorElement = xml."filter-mapping"
            priorElement[priorElement.size() - 1] + {
                listener {
                    "listener-class"(SingleSignOutHttpSessionListener.name)
                }
            }
        }
    }
}
