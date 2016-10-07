/*
 * Copyright 2016 EPAM Systems
 *
 *
 * This file is part of EPAM Report Portal.
 * https://github.com/reportportal/logger-java-log4j
 *
 * Report Portal is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Report Portal is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Report Portal.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.epam.ta.reportportal.log4j.appender;


import java.io.File;
import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.ObjectMessage;
import org.apache.logging.log4j.message.SimpleMessage;

import com.epam.reportportal.guice.Injector;
import com.epam.reportportal.listeners.ReportPortalListenerContext;
import com.epam.reportportal.message.MessageParser;
import com.epam.reportportal.message.ReportPortalMessage;
import com.epam.reportportal.service.BatchedReportPortalService;
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.io.ByteSource;

/**
 * Log4j2 appender for report portal
 *
 * @author Dzmitry_Kavalets
 */
@Plugin(name = "ReportPortalLog4j2Appender", category = "Core", elementType = "appender", printObject = true)
public class ReportPortalLog4j2Appender extends AbstractAppender {

	private Supplier<BatchedReportPortalService> reportPortalService;
	private Supplier<MessageParser> messageParser;

	protected ReportPortalLog4j2Appender(String name, Filter filter, Layout<? extends Serializable> layout) {
		super(name, filter, layout);
		this.reportPortalService = Suppliers.memoize(new Supplier<BatchedReportPortalService>() {
			@Override
			public BatchedReportPortalService get() {
				return Injector.getInstance().getBean(BatchedReportPortalService.class);
			}
		});
		this.messageParser = Suppliers.memoize(new Supplier<MessageParser>() {
			@Override
			public MessageParser get() {
				return Injector.getInstance().getBean(MessageParser.class);
			}
		});

	}

	@PluginFactory
	public static ReportPortalLog4j2Appender createAppender(@PluginAttribute("name") String name,
			@PluginElement("filter") Filter filter, @PluginElement("layout") Layout<? extends Serializable> layout) {
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
	public void append(LogEvent event) {
		String currentItemId = ReportPortalListenerContext.getRunningNowItemId();
		if (null == currentItemId) {
			return;
		}

		BatchedReportPortalService reportPortalService;
		MessageParser messageParser;
		try {

			reportPortalService = this.reportPortalService.get();
			messageParser = this.messageParser.get();
			ReportPortalMessage message = null;
			String newLogMessage = null;

			Message eventMessage = event.getMessage();

			if ((eventMessage instanceof ObjectMessage) && (eventMessage.getParameters().length > 0)) {

				Object objectMessage = eventMessage.getParameters()[0];

				if (objectMessage instanceof ReportPortalMessage) {
					message = (ReportPortalMessage) objectMessage;
					newLogMessage = message.getMessage();
				} else if (objectMessage instanceof File) {
					message = new ReportPortalMessage((File) event.getMessage(), "Binary data reported");
					newLogMessage = message.getMessage();
				} else {
					newLogMessage = objectMessage == null ? null : objectMessage.toString();
				}
			} else if (eventMessage instanceof SimpleMessage && messageParser.supports(eventMessage.getFormattedMessage())) {
				message = messageParser.parse(eventMessage.getFormattedMessage());
				newLogMessage = message.getMessage();
			} else if (eventMessage instanceof SimpleMessage) {
				newLogMessage = new String(getLayout().toByteArray(event));
			}

			SaveLogRQ saveLogRQ = buildSaveLogRQ(event, currentItemId, newLogMessage,
					message == null ? null : message.getData());
			AppenderUtils.sendLogToRP(reportPortalService, saveLogRQ);

		} catch (RuntimeException e) {
			/*
			 * Try to find out initialization problems
			 */
			e.printStackTrace(); //NOSONAR
			throw e;
		}
	}

	private static SaveLogRQ buildSaveLogRQ(LogEvent event, String currentItemId, String message,
			final ByteSource data) {
		SaveLogRQ saveLogRQ = new SaveLogRQ();
		saveLogRQ.setMessage(message);
		saveLogRQ.setLogTime(new Date(event.getTimeMillis()));
		saveLogRQ.setTestItemId(currentItemId);
		saveLogRQ.setLevel(event.getLevel().toString());

		if (null != data) {
			SaveLogRQ.File file = new SaveLogRQ.File();
			file.setContent(data);
			file.setName(UUID.randomUUID().toString());
			saveLogRQ.setFile(file);
		}
		return saveLogRQ;
	}
}
