package com.epam.ta.reportportal.log4j.appender;

import com.epam.reportportal.listeners.ItemType;
import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.message.ReportPortalMessage;
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.reportportal.service.logs.LoggingSubscriber;
import com.epam.reportportal.util.test.CommonUtils;
import com.epam.reportportal.utils.files.ByteSource;
import com.epam.ta.reportportal.ws.model.*;
import com.epam.ta.reportportal.ws.model.item.ItemCreatedRS;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRQ;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRS;
import io.reactivex.Maybe;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.apache.logging.log4j.message.ObjectMessage;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;

import static com.epam.reportportal.util.test.CommonUtils.generateUniqueId;
import static java.util.Optional.ofNullable;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ReportPortalLog4j2AppenderTest {
	private final ReportPortalClient client = mock(ReportPortalClient.class);
	private final ExecutorService executor = CommonUtils.testExecutor();
	private final ReportPortal rp = ReportPortal.create(client, standardParameters(), executor);
	private Launch launch;
	private Maybe<String> id;

	public static ListenerParameters standardParameters() {
		ListenerParameters result = new ListenerParameters();
		result.setClientJoin(false);
		result.setBatchLogsSize(1);
		result.setLaunchName("My-test-launch" + generateUniqueId());
		result.setProjectName("test-project");
		result.setEnable(true);
		result.setBaseUrl("http://localhost:8080");
		return result;
	}

	@SuppressWarnings("unchecked")
	private static void mockBatchLogging(ReportPortalClient client) {
		when(client.log(any(List.class))).thenReturn(Maybe.just(new BatchSaveOperatingRS()));
	}

	@SuppressWarnings("ResultOfMethodCallIgnored")
	@BeforeEach
	public void setUp() {
		when(client.startLaunch(any(StartLaunchRQ.class))).thenReturn(Maybe.just(new StartLaunchRS("launch_uuid", 1L)));
		when(client.startTestItem(any(StartTestItemRQ.class))).thenReturn(Maybe.just(new ItemCreatedRS("item_uuid", "unique_item_uuid")));
		when(client.finishTestItem(any(), any(FinishTestItemRQ.class))).thenReturn(Maybe.just(new OperationCompletionRS()));
		when(client.finishLaunch(any(), any(FinishExecutionRQ.class))).thenReturn(Maybe.just(new OperationCompletionRS()));
		StartLaunchRQ launchRQ = new StartLaunchRQ();
		launchRQ.setStartTime(Calendar.getInstance().getTime());
		launchRQ.setName("My-test-launch");
		launch = rp.newLaunch(launchRQ);
		launch.start().blockingGet();
		StartTestItemRQ rq = new StartTestItemRQ();
		rq.setName("My-test-item");
		rq.setStartTime(Calendar.getInstance().getTime());
		rq.setType(ItemType.STEP.name());
		id = launch.startTestItem(rq);
	}

	@SuppressWarnings("resource")
	private static Logger createLoggerFor(Class<?> clazz) {
		ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder();
		builder.setPackages("com.epam.ta.reportportal.log4j.appender");
		builder.setStatusLevel(Level.DEBUG);
		builder.setConfigurationName("BuilderTest");
		AppenderComponentBuilder appenderBuilder = builder.newAppender("ReportPortalAppender", "ReportPortalLog4j2Appender");
		appenderBuilder.add(builder.newLayout("PatternLayout")
				.addAttribute("pattern", "%date %level [%thread] %logger{10} [%file:%line] %msg%n"));
		appenderBuilder.add(builder.newFilter("ThresholdFilter", Filter.Result.ACCEPT, Filter.Result.NEUTRAL)
				.addAttribute("level", Level.DEBUG));
		builder.add(appenderBuilder);
		builder.add(builder.newRootLogger(Level.DEBUG).add(builder.newAppenderRef("ReportPortalAppender")));
		LoggerContext ctx = Configurator.initialize(builder.build());
		return ctx.getLogger(clazz);
	}

	@AfterEach
	public void tearDown() {
		CommonUtils.shutdownExecutorService(executor);
	}

	@Test
	@SuppressWarnings({ "unchecked", "ReactiveStreamsUnusedPublisher" })
	public void test_logger_append() {
		mockBatchLogging(client);
		Logger logger = createLoggerFor(Launch.class);
		logger.info("test message");
		launch.finishTestItem(id, new FinishTestItemRQ());
		launch.finish(new FinishExecutionRQ());
		verify(client, times(2)).log(any(List.class));
	}

	@Test
	@SuppressWarnings({ "unchecked", "ReactiveStreamsUnusedPublisher" })
	public void test_logger_skip() {
		Logger logger = createLoggerFor(LoggingSubscriber.class);
		logger.info("test message");
		launch.finishTestItem(id, new FinishTestItemRQ());
		launch.finish(new FinishExecutionRQ());
		verify(client, timeout(100).times(1)).log(any(List.class));
	}

	@SuppressWarnings({ "unchecked" })
	private void verify_binary_logging(long contentLength) throws IOException {
		ArgumentCaptor<List<MultipartBody.Part>> captor = ArgumentCaptor.forClass(List.class);
		verify(client, times(2)).log(captor.capture());

		List<MultipartBody.Part> request = captor.getAllValues().get(0);
		assertThat(request, hasSize(2));

		RequestBody jsonPart = request.get(0).body();
		MediaType jsonPartType = jsonPart.contentType();
		assertThat(jsonPartType, notNullValue());
		assertThat(jsonPartType.toString(), Matchers.startsWith("application/json"));

		RequestBody binaryPart = request.get(1).body();
		MediaType binaryPartType = binaryPart.contentType();
		assertThat(binaryPartType, notNullValue());
		assertThat(binaryPartType.toString(), equalTo("image/jpeg"));
		assertThat(binaryPart.contentLength(), equalTo(contentLength));
	}

	@Test
	@SuppressWarnings({ "ReactiveStreamsUnusedPublisher" })
	public void test_binary_file_message_encoding() throws IOException {
		mockBatchLogging(client);
		String message = "test message";
		Logger logger = createLoggerFor(this.getClass());
		byte[] content;
		try (InputStream is = ofNullable(Thread.currentThread().getContextClassLoader().getResourceAsStream("pug/unlucky.jpg")).orElseThrow(
				() -> new IllegalStateException("Unable to find test image file"))) {
			content = IOUtils.toByteArray(is);
		}
		logger.info(String.format("RP_MESSAGE#FILE#%s#%s", "src/test/resources/pug/unlucky.jpg", message));
		launch.finishTestItem(id, new FinishTestItemRQ());
		launch.finish(new FinishExecutionRQ());
		verify_binary_logging(content.length);
	}

	@Test
	@SuppressWarnings({ "ReactiveStreamsUnusedPublisher" })
	public void test_reportportal_message_logging() throws IOException {
		mockBatchLogging(client);
		String messageText = "test message";
		Logger logger = createLoggerFor(this.getClass());
		byte[] content;
		try (InputStream is = ofNullable(Thread.currentThread().getContextClassLoader().getResourceAsStream("pug/unlucky.jpg")).orElseThrow(
				() -> new IllegalStateException("Unable to find test image file"))) {
			content = IOUtils.toByteArray(is);
		}
		ReportPortalMessage message = new ReportPortalMessage(ByteSource.wrap(content), "image/jpeg", messageText);
		logger.info(new ObjectMessage(message));
		launch.finishTestItem(id, new FinishTestItemRQ());
		launch.finish(new FinishExecutionRQ());
		verify_binary_logging(content.length);
	}

	@Test
	@SuppressWarnings({ "ReactiveStreamsUnusedPublisher" })
	public void test_file_logging() throws IOException {
		mockBatchLogging(client);
		Logger logger = createLoggerFor(this.getClass());
		logger.info(new ObjectMessage(new File("src/test/resources/pug/unlucky.jpg")));
		launch.finishTestItem(id, new FinishTestItemRQ());
		launch.finish(new FinishExecutionRQ());
		verify_binary_logging(90404L);
	}
}
