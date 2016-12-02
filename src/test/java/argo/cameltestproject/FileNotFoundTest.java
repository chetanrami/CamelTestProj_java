package argo.cameltestproject;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.ProcessorDefinition;
import org.junit.Test;

import com.google.common.base.Joiner;

public class FileNotFoundTest extends ArgoCamelTestSupport{
	static final String DEMONSTRATION_FILE = Joiner.on("\n").join(
			"<Envelope xmlns='http://schemas.xmlsoap.org/soap/envelope/'>", "<Body>",
			"<QAS_GETQUERYRESULTS_RESP_MSG xmlns='http://xmlns.oracle.com/Enterprise/Tools/schemas/QAS_GETQUERYRESULTS_RESP_MSG.VERSION_1'>",
			"<query numrows='0' queryname='DEMONSTRATION_QUERY' xmlns='http://xmlns.oracle.com/Enterprise/Tools/schemas/QAS_QUERYRESULTS_XMLP_RESP.VERSION_1'/>",
			"</QAS_GETQUERYRESULTS_RESP_MSG>", "</Body>", "</Envelope>");

	@Override
	protected void addAdvice() {
		// TODO: replace the live SFTP routes with injected source, target, and
		// error endpoints as appropriate
		try {
			context.getRouteDefinitions().get(0).adviceWith(context, new AdviceWithRouteBuilder() {
				@Override
				public void configure() throws Exception {
					replaceFromWith("direct:errs");
				}
			});
			context.start();
			// we must manually start when we are done with all the advice with
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

//	@Test
	public void ensureCriticalPathIsSatisfied() {
		// TODO: send the string DEMONSTRATION_FILE to source
		// TODO: ensure 0 messages are received by the target endpoint
		// TODO: ensure 1 message are received by the error endpoint
		addAdvice();

		template.sendBody(source, DEMONSTRATION_FILE);

		target.expectedMessageCount(0);
		errors.expectedMessageCount(1);
		try {
			assertMockEndpointsSatisfied();
		} catch (InterruptedException e) {
			/*
			 * Do something like: Send an Email that Route itself 
			 * is not working or Error Queue is dead.
			 */
			e.printStackTrace();
		}
	}

	// This is the route we want to test
	@Override
	protected RouteBuilder createRouteBuilder() throws Exception {
		return new RouteBuilder() {
			@Override
			public void configure() throws Exception {
				from(source).routeId("fileSource").to(errors).routeId("errorHandler2")
						.log(LoggingLevel.ERROR, "failed to process\n${body}").id("errorWeavingHook2");

			}
		};
	}

	// ----------------Ignore this please---------------------------------------
	private int oneMessage = 0;
	private boolean debugBeforeMethodCalled;
	private boolean debugAfterMethodCalled;

	@Override
	public boolean isUseDebugger() {
		// must enable debugger
		return true;
	}

	@Override
	protected void debugBefore(Exchange exchange, org.apache.camel.Processor processor,
			ProcessorDefinition<?> definition, String id, String label) {
		log.info("Before " + definition + " with body " + exchange.getIn().getBody());
		debugBeforeMethodCalled = true;
	}

	@Override
	protected void debugAfter(Exchange exchange, org.apache.camel.Processor processor,
			ProcessorDefinition<?> definition, String id, String label, long timeTaken) {
		log.info("After " + definition + " with body " + exchange.getIn().getBody());
		oneMessage++;
		debugAfterMethodCalled = true;
	}
	// -------------------------------------------------------------------------------
}