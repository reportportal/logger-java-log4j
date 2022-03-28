/*
 * Copyright 2019 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.epam.ta.reportportal.log4j.appender;

import com.epam.reportportal.message.ReportPortalMessage;
import com.epam.reportportal.message.TypeAwareByteSource;
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.ObjectMessage;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import static com.epam.reportportal.service.ReportPortal.emitLog;
import static com.epam.reportportal.utils.MimeTypeDetector.detect;
import static com.google.common.io.Files.asByteSource;

/**
 * Log4j2 appender for report portal
 *
 * @author Dzmitry_Kavalets
 */
@Plugin(name = "ReportPortalLog4j2Appender", category = "Core", elementType = "appender", printObject = true)
@SuppressWarnings("unused")
public class ReportPortalLog4j2Appender extends AbstractAppender {

	protected ReportPortalLog4j2Appender(String name, Filter filter, Layout<? extends Serializable> layout) {
		super(name, filter, layout, true, Property.EMPTY_ARRAY);
	}

	@PluginFactory
	public static ReportPortalLog4j2Appender createAppender(@PluginAttribute("name") String name, @PluginElement("filter") Filter filter,
			@PluginElement("layout") Layout<? extends Serializable> layout) {

		if (name == null) {
			LOGGER.error("No name provided for ReportPortalLog4j2Appender");
			return null;
		}

		if (layout == null) {
			LOGGER.error("No layout provided for ReportPortalLog4j2Appender");
			return null;
		}
		return new ReportPortalLog4j2Appender(name, filter, layout);
	}

	@Override
	public void append(final LogEvent logEvent) {

		final LogEvent event = logEvent.toImmutable();
		if (null == event.getMessage()) {
			return;
		}

		emitLog(itemUuid -> {
			SaveLogRQ request = new SaveLogRQ();
			request.setItemUuid(itemUuid);
			request.setLogTime(new Date(event.getTimeMillis()));
			request.setLevel(event.getLevel().name());

			Message eventMessage = event.getMessage();

			TypeAwareByteSource byteSource = null;
			String message = "";

			try {
				if ((eventMessage instanceof ObjectMessage) && (eventMessage.getParameters().length > 0)) {

					Object objectMessage = eventMessage.getParameters()[0];

					if (objectMessage instanceof ReportPortalMessage) {
						ReportPortalMessage rpMessage = (ReportPortalMessage) objectMessage;
						byteSource = rpMessage.getData();
						message = rpMessage.getMessage();
					} else if (objectMessage instanceof File) {
						final File file = (File) objectMessage;
						byteSource = new TypeAwareByteSource(asByteSource(file), detect(file));
						message = "File reported";

					} else {
						if (null != objectMessage) {
							message = objectMessage.toString();
						}
					}

				} else if (Util.MESSAGE_PARSER.supports(eventMessage.getFormattedMessage())) {
					ReportPortalMessage rpMessage = Util.MESSAGE_PARSER.parse(eventMessage.getFormattedMessage());
					message = rpMessage.getMessage();
					byteSource = rpMessage.getData();
				} else {
					message = new String(getLayout().toByteArray(event), StandardCharsets.UTF_8);
				}

				if (null != byteSource) {
					SaveLogRQ.File file = new SaveLogRQ.File();
					file.setName(UUID.randomUUID().toString());
					file.setContentType(byteSource.getMediaType());
					file.setContent(byteSource.read());

					request.setFile(file);
				}
			} catch (IOException e) {
				//skip an error. There is some issue with binary data reading
			}
			request.setMessage(message);

			return request;
		});
	}
}
