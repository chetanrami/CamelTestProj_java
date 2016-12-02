package argo.cameltestproject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import argo.cameltestproject.emplrecord.Record;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Message;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.model.dataformat.BindyType;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import com.google.common.base.Joiner;

import static org.apache.camel.component.stax.StAXBuilder.stax;

public class CriticalPathTest extends ArgoCamelTestSupport {
	//region dataDeclaration
	static final String DEMONSTRATION_FILE = Joiner.on("\n").join(
			"<Envelope xmlns='http://schemas.xmlsoap.org/soap/envelope/'>", "<Body>",
			"<QAS_GETQUERYRESULTS_RESP_MSG xmlns='http://xmlns.oracle.com/Enterprise/Tools/schemas/QAS_GETQUERYRESULTS_RESP_MSG.VERSION_1'>",
			"<query numrows='5' queryname='DEMONSTRATION_QUERY' xmlns='http://xmlns.oracle.com/Enterprise/Tools/schemas/QAS_QUERYRESULTS_XMLP_RESP.VERSION_1'>",
			"<row rownumber='1'><EMPLID>10000000000</EMPLID><NAME>Frodo Baggins</NAME></row>",
			"<row rownumber='2'><EMPLID>20000000000</EMPLID><NAME>Samwise Gamgee</NAME></row>",
			"<row rownumber='3'><EMPLID>30000000000</EMPLID><NAME>Legolas</NAME></row>",
			"<row rownumber='4'><EMPLID>40000000000</EMPLID><NAME>Gimli</NAME></row>",
			"<row rownumber='5'><EMPLID>50000000000</EMPLID><NAME>Boromir</NAME></row>", "</query>",
			"</QAS_GETQUERYRESULTS_RESP_MSG>", "</Body>", "</Envelope>");
	static final String EXPECTED_RESULT = Joiner.on("\n").join("EMPLOYEE_NAME,EMPLOYEE_ID",
			"\"Frodo Baggins\",\"10000000000\"", "\"Samwise Gamgee\",\"20000000000\"", "\"Legolas\",\"30000000000\"",
			"\"Gimli\",\"40000000000\"", "\"Boromir\",\"50000000000\"");
	//endregion dataDeclaration

	@Override
	protected void addAdvice() {
		// TODO: replace the live SFTP routes with injected source, target, and error endpoints as appropriate
		try {
			context.getRouteDefinition("restRouteID").adviceWith(context, new AdviceWithRouteBuilder() {
				@Override
				public void configure() throws Exception {
					// intercept valid messages in this route and replace them with mock
					replaceFromWith(source2);
					weaveById("setRestBody").replace().setBody(constant("http://localhost:8181/rest/auth/asdasdasd"));
//					("http://localhost:8181/rest/auth/c638db5e-5e6c-11e6-84d7-000d3a90c693"));
				}
			});
			context.getRouteDefinition("forLogging").adviceWith(context, new AdviceWithRouteBuilder() {
				@Override
				public void configure() throws Exception {
					// intercept valid messages in this route and replace them with mock
				}
			});

			context.getRouteDefinition("cronTimerCamelTestingRoutes_RouteId").adviceWith(context, new AdviceWithRouteBuilder() {
				@Override
				public void configure() throws Exception {
					// intercept valid messages in this route and replace them with mock
					replaceFromWith(cronSource);
				}
			});
			context.getRouteDefinition("fileSource").adviceWith(context, new AdviceWithRouteBuilder() {
				@Override
				public void configure() throws Exception {
					// intercept valid messages in this route and replace them with mock
					replaceFromWith(source);
					weaveById("marshaller").replace().to(target2);
					weaveById("archiveSystem").replace().to(target);
					weaveById("archiveFile").replace().convertBodyTo(String.class)
							.to("file:src/test/test-output/sftparchive");

				}
			});
			context.getRouteDefinition("errorHandler").adviceWith(context, new AdviceWithRouteBuilder() {
				@Override
				public void configure() throws Exception {
					// intercept valid messages in this route and replace them with mock
					weaveById("errorWeavingHook").replace().
							setHeader(Exchange.FILE_NAME, constant("Errors.log"))
							.to(errors).to("file:src/test/test-output/sftparchive")
							.to("file:src/test/test-output/sftparchive/");
				}
			});
			// we must manually start when we are done with all the advice with
			context.startAllRoutes();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void ensureCriticalPathIsSatisfied() {
		// TODO: send the string DEMONSTRATION_FILE to source
		// TODO: ensure 1 message is received by the target endpoint, containing
		// EXPECTED_RESULT as its message contents and
		// 'FELLOWSHIP_OF_THE_RING.txt' as its file name only header
		// TODO: ensure 0 messages are received by the error endpoint
		// hint, use base class's template field to send messages to source

		Map<String, Object> myMap = new HashMap<>();
		myMap.put(Exchange.FILE_NAME,"DemonstrationFileRenamed.txt");
		myMap.put(Exchange.FILE_NAME_ONLY,"DemonstrationFileRenamed.txt");
		String demonstrationTxt = "";
		try {
			FileInputStream fisTargetFile = new FileInputStream(new File("src\\test\\resources\\DemonstrationFile.txt"));
			demonstrationTxt  = IOUtils.toString(fisTargetFile, "UTF-8");
		}catch(IOException ioe){
			ioe.printStackTrace();
		}
		template.sendBody(source2, "http://localhost:8181/rest/auth/asdasdasd");
		template.sendBodyAndHeaders(source, demonstrationTxt, myMap);
		target2.expectedMessageCount(1);
		target.expectedMessageCount(1);
		checkHeadersForValidationGradStudents(target.getExchanges());
		errors.expectedMessageCount(0);
		try {
			assertMockEndpointsSatisfied();
		} catch (InterruptedException e) {
			/*
			 * Do something like: Send an Email that something is wrong with
			 * Target endpoint the processors (eg: the route itself, convertto,
			 * split, Marshal, etc) are might not be working as intended
			 */
			e.printStackTrace();
		}

	}

	@SuppressWarnings("unchecked")
	static void checkHeadersForValidationGradStudents(List<Exchange> listOfExchanges) {

		Object msgBody = listOfExchanges.get(0).getIn().getBody();

		Record record1  = ((List<Record>)msgBody).get(0);
		Record record2  = ((List<Record>)msgBody).get(1);

		assertEquals("10000000000", record1.getEmployeeId());
		assertEquals("Frodo Baggins", record1.getEmployeeName());

		assertEquals("20000000000", record2.getEmployeeId());
		assertEquals("Samwise Gamgee", record2.getEmployeeName());

	}

	//region commenting: bad practice
	// This is the route we want to test
/*	@Override
	protected RouteBuilder createRouteBuilder() throws Exception {
		return new RouteBuilder() {
			@Override
			public void configure() throws Exception {
				// we do not have activemq on the classpath
				// but the route has it included
				from("direct:source").routeId("fileSource2").errorHandler(deadLetterChannel("direct:errors"))

				.convertBodyTo(InputStream.class).split(stax(Record.class), new AggregateIntoList())
						.log(LoggingLevel.INFO, "built instance of ${body.class.name}").end().marshal()
						.bindy(BindyType.Csv, Record.class).id("marshaller2").convertBodyTo(String.class)
						.transform(simple("${body.trim()}"))
						.setHeader(Exchange.FILE_NAME_ONLY, constant("FELLOWSHIP_OF_THE_RING.txt")).to("mock:target")
						.id("archiveSystem2");
			}
		};
	}

	private int oneMessage = 0;
	private String bodyOfResult = "";
	private String fileNameHeader = "";

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
		if (exchange.getIn().getBody().toString().startsWith("EMPLOYEE_NAME,EMPLOYEE_ID")) {
			bodyOfResult = exchange.getIn().getBody().toString();
		}
		if (definition.toString().indexOf("{FELLOWSHIP_OF_THE_RING.txt}") >= 0) {
			fileNameHeader = "{FELLOWSHIP_OF_THE_RING.txt}";
		}
		if (definition.toString().indexOf(
				"To[sftp://UNCONFIGURED.target.example.org?username=TARGET&privateKeyUri=file:///dev/null]") >= 0) {
			oneMessage++;
		}
		debugAfterMethodCalled = true;
	}*/
	// endregion
}