package argo.cameltestproject;

import java.util.Set;

import javax.annotation.Nonnull;

import org.apache.camel.Endpoint;
import org.apache.camel.EndpointInject;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.RouteDefinition;

import com.google.common.collect.Sets;
import org.apache.camel.test.blueprint.CamelBlueprintTestSupport;

/**
 * Base class for blueprint testing.
 */
abstract public class ArgoCamelTestSupport extends CamelBlueprintTestSupport {
    @EndpointInject(uri = "direct:source")
    Endpoint source;
    @EndpointInject(uri = "direct:source2")
    Endpoint source2;
    @EndpointInject(uri = "direct:cronSource")
    Endpoint cronSource;

    @EndpointInject(uri = "mock:target")
    MockEndpoint target;
    @EndpointInject(uri = "mock:target2")
    MockEndpoint target2;
    @EndpointInject(uri = "mock:errors")
    MockEndpoint errors;

    @Override
    final public boolean isUseAdviceWith() {
        return true;
    }

    @Override
    final protected String getBlueprintDescriptor() {
        return "/OSGI-INF/blueprint/testDependencies.xml,/OSGI-INF/blueprint/CTP_Java.xml";
    }

    @Override
    final protected String getBundleFilter() {
        return "(!(Bundle-Name=argo-cameltestproject))";
    }

    /**
     * If non-empty, all routes whose ids are <em>not</em> contained in this set will be <strong>removed</strong> from the test bundle.
     */
    @Nonnull
    protected Set<String> routesUnderTest = Sets.newHashSet();

    @Override
    final protected void doPostSetup() throws Exception {
        addAdvice();
        if (!routesUnderTest.isEmpty()) {
            final Set<String> routesToRemove = Sets.newHashSet();
            for ( RouteDefinition route : context.getRouteDefinitions() ) {
                routesToRemove.add(route.getId());
            }
            routesToRemove.removeAll(routesUnderTest);
            for (String routeId : routesToRemove) {
                context.removeRouteDefinition(
                    context.getRouteDefinition(routeId)
                );
            }
        }
    }
/**
 * Extension point for sub classes to add advice to routes.
 */
    abstract protected void addAdvice();
    
}
