package com.epam.ta.reportportal.log4j.appender;

import com.epam.reportportal.message.ReportPortalMessage;
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.service.LoggingContext;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.reportportal.service.logs.LoggingSubscriber;
import com.epam.reportportal.util.test.CommonUtils;
import com.epam.reportportal.utils.files.ByteSource;
import com.epam.ta.reportportal.ws.model.BatchSaveOperatingRS;
import io.reactivex.Maybe;
import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;
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
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;

import static java.util.Optional.ofNullable;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ReportPortalLog4j2AppenderTest {
	@Mock
	private ReportPortalClient client;

	private final ExecutorService executor = CommonUtils.testExecutor();
	private final Scheduler scheduler = Schedulers.from(executor);

	@SuppressWarnings("resource")
	private static Logger createLoggerFor(Class<?> clazz) {
		ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder();
		builder.setPackages("com.epam.ta.reportportal.log4j.appender");
		builder.setStatusLevel(Level.DEBUG);
		builder.setConfigurationName("BuilderTest");
		AppenderComponentBuilder appenderBuilder = builder.newAppender("ReportPortalAppender",
				"ReportPortalLog4j2Appender"
		);
		appenderBuilder.add(builder.newLayout("PatternLayout")
				.addAttribute("pattern", "%date %level [%thread] %logger{10} [%file:%line] %msg%n"));
		appenderBuilder.add(builder.newFilter("ThresholdFilter", Filter.Result.ACCEPT, Filter.Result.NEUTRAL)
				.addAttribute("level", Level.DEBUG));
		builder.add(appenderBuilder);
		builder.add(builder.newRootLogger(Level.DEBUG).add(builder.newAppenderRef("ReportPortalAppender")));
		LoggerContext ctx = Configurator.initialize(builder.build());
		return ctx.getLogger(clazz);
	}

	@SuppressWarnings("unchecked")
	private static void mockBatchLogging(ReportPortalClient client) {
		when(client.log(any(List.class))).thenReturn(Maybe.just(new BatchSaveOperatingRS()));
	}

	@AfterEach
	public void tearDown() {
		CommonUtils.shutdownExecutorService(executor);
	}

	@Test
	@SuppressWarnings({ "unchecked", "ReactiveStreamsUnusedPublisher" })
	public void test_logger_append() {
		mockBatchLogging(client);
		LoggingContext.init(Maybe.just("launch_uuid"), Maybe.just("item_uuid"), client, scheduler);
		Logger logger = createLoggerFor(Launch.class);
		logger.info("test message");
		LoggingContext.complete();
		verify(client).log(any(List.class));
	}

	@Test
	@SuppressWarnings({ "unchecked", "ReactiveStreamsUnusedPublisher" })
	public void test_logger_skip() {
		LoggingContext.init(Maybe.just("launch_uuid"), Maybe.just("item_uuid"), client, scheduler);
		Logger logger = createLoggerFor(LoggingSubscriber.class);
		logger.info("test message");
		LoggingContext.complete();
		verify(client, timeout(100).times(0)).log(any(List.class));
	}

	@SuppressWarnings({ "unchecked" })
	private void verify_binary_logging(long contentLength) throws IOException {
		ArgumentCaptor<List<MultipartBody.Part>> captor = ArgumentCaptor.forClass(List.class);
		verify(client).log(captor.capture());

		List<MultipartBody.Part> request = captor.getValue();
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
		LoggingContext.init(Maybe.just("launch_uuid"), Maybe.just("item_uuid"), client, scheduler);
		String message = "test message";
		Logger logger = createLoggerFor(this.getClass());
		byte[] content;
		try (InputStream is = ofNullable(Thread.currentThread()
				.getContextClassLoader()
				.getResourceAsStream("pug/unlucky.jpg")).orElseThrow(() -> new IllegalStateException(
				"Unable to find test image file"))) {
			content = IOUtils.toByteArray(is);
		}
		logger.info(String.format("RP_MESSAGE#FILE#%s#%s", "src/test/resources/pug/unlucky.jpg", message));
		LoggingContext.complete();
		verify_binary_logging(content.length);
	}

	@Test
	@SuppressWarnings({ "ReactiveStreamsUnusedPublisher" })
	public void test_reportportal_message_logging() throws IOException {
		mockBatchLogging(client);
		LoggingContext.init(Maybe.just("launch_uuid"), Maybe.just("item_uuid"), client, scheduler);
		String messageText = "test message";
		Logger logger = createLoggerFor(this.getClass());
		byte[] content;
		try (InputStream is = ofNullable(Thread.currentThread()
				.getContextClassLoader()
				.getResourceAsStream("pug/unlucky.jpg")).orElseThrow(() -> new IllegalStateException(
				"Unable to find test image file"))) {
			content = IOUtils.toByteArray(is);
		}
		ReportPortalMessage message = new ReportPortalMessage(ByteSource.wrap(content), "image/jpeg", messageText);
		logger.info(new ObjectMessage(message));
		LoggingContext.complete();
		verify_binary_logging(content.length);
	}

	@Test
	@SuppressWarnings({ "ReactiveStreamsUnusedPublisher" })
	public void test_file_logging() throws IOException {
		mockBatchLogging(client);
		LoggingContext.init(Maybe.just("launch_uuid"), Maybe.just("item_uuid"), client, scheduler);
		Logger logger = createLoggerFor(this.getClass());
		logger.info(new ObjectMessage(new File("src/test/resources/pug/unlucky.jpg")));
		LoggingContext.complete();
		verify_binary_logging(90404L);
	}
}
