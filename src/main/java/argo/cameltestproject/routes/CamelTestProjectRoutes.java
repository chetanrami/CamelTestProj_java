package argo.cameltestproject.routes;

import static org.apache.camel.component.stax.StAXBuilder.stax;

import java.io.InputStream;

import org.apache.camel.CamelContext;
import org.apache.camel.LoggingLevel;
import org.apache.camel.PropertyInject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.bindy.csv.BindyCsvDataFormat;
import org.apache.camel.impl.DefaultCamelContext;

import argo.cameltestproject.AggregateIntoList;
import argo.cameltestproject.emplrecord.Record;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class CamelTestProjectRoutes extends RouteBuilder {

	String recordPkg = "argo.cameltestproject.emplrecord";
	public static final String LOG_NAME = "argo.cameltestproject.cameltestroutes";
	@Override
	public void configure() throws Exception {

		String control_bus_filePoll_suspend = "controlbus:route?routeId=fileSource&action=suspend&async=true";
		String control_bus_filePoll_resume = "controlbus:route?routeId=fileSource&action=resume&async=true";

		onException(Exception.class)
				.log("error trace : ${exception.stacktrace}").handled(true);

		restConfiguration()
				.component("servlet").port(8181)
				.endpointProperty("servletName", "camelServletJavaTestProj")
				.contextPath("/restjava");

		rest("/route/").get("/{token}")
				.route().startupOrder(200).routeId("restRouteID")
				.from("direct:rest")
				.setBody(simple("${in.header.token}")).id("setRestBody")
				.log(LoggingLevel.INFO, LOG_NAME, "body:----- ${body}")
				.to("direct:checkAuth").id("restAuthEnd");

		from("direct:checkAuth").routeId("forLogging")
				.log(LoggingLevel.INFO, LOG_NAME, "logging body:----- ${body}");

		rest("/auth2/").post("/{token}")
				.route().startupOrder(202).routeId("restRouteID2")
				.from("direct:rest2")
				.setBody(simple("${in.header.token}")).id("setRestBody2")
				.log(LoggingLevel.INFO, LOG_NAME, "body:----- ${body}")
				.to("direct:checkAuth2").id("restAuthEnd2");

		from("direct:checkAuth2").routeId("forLogging2")
				.log(LoggingLevel.INFO, LOG_NAME, "logging body:----- ${body}");

		from(cronExp)
				.routeId("cronTimerCamelTestingRoutes_RouteId")
				.log(LoggingLevel.INFO, LOG_NAME, "CamelTestingRoutes: Started via Quartz Timer")
				.to(control_bus_filePoll_resume).id("control_bus_filePoll_resume_id");

		from(sourceSftp).routeId("fileSource")
				.errorHandler(deadLetterChannel("direct:errors"))
				.log(LoggingLevel.INFO, LOG_NAME, "Headers before pickup URI: ${headers}")
				.log(LoggingLevel.INFO, "sftp picked body: ${body} ${body.class}")
				.choice()
					.when(simple("${body} == ${null}"))
						.log(LoggingLevel.INFO, LOG_NAME, "Suspending File Poll")
					.endChoice()
					.otherwise()
						.convertBodyTo(InputStream.class)
						.split(stax(Record.class), new AggregateIntoList())
							.log(LoggingLevel.INFO, "built instance of ${body.class}")
						.end()
						.log(LoggingLevel.INFO, "aggregated body ${body.class}")
						.marshal(new BindyCsvDataFormat(Record.class)).id("marshaller")
						.to(targetSftp).id("archiveSystem")
						.log(LoggingLevel.INFO, "archiveSystem Body ${body.class}").id("archiveFile")
					.endChoice()
				.end()
				.to(control_bus_filePoll_suspend).id("control_bus_filePoll_suspend_id");

		from("direct:errors").routeId("errorHandler")
				.log(LoggingLevel.ERROR, "failed to process\n${body}").id("errorWeavingHook");
	}

	public static void main(String[] args) throws Exception {
		final CamelContext camelContext = new DefaultCamelContext();
		camelContext.addRoutes(new CamelTestProjectRoutes());
		camelContext.start();
		Thread.sleep(3000);
		camelContext.stop();
	}

	@PropertyInject("sftp://{{source.hostname}}:22{{source.path}}?username={{source.username}}&privateKeyUri={{source.privateKeyUri}}&delete=true&include={{source.fileName}}&idempotent=true&readLock=changed&readLockTimeout=30000&readLockCheckInterval=20000&stepwise=false&sendEmptyMessageWhenIdle=true")
	String sourceSftp;

	@PropertyInject("sftp://{{target.hostname}}:22{{target.path}}?username={{target.username}}&privateKeyUri={{target.privateKeyUri}}")
	String targetSftp;

	@PropertyInject("quartz2://CTP_Timer?cron={{source.cron.expression}}")
	String cronExp;

	@PropertyInject(value = "source.cron.autoStart", defaultValue = "true")
	String cronStart;
}
