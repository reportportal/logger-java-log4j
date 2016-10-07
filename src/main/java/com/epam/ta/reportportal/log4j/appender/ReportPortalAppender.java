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

import com.epam.reportportal.guice.Injector;
import com.epam.reportportal.listeners.ReportPortalListenerContext;
import com.epam.reportportal.message.MessageParser;
import com.epam.reportportal.message.ReportPortalMessage;
import com.epam.reportportal.service.BatchedReportPortalService;
import com.google.common.base.Supplier;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;


import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;
import com.google.common.base.Suppliers;

/**
 * Log4j appender for report portal
 * 
 * @author Andrei Varabyeu
 * 
 */
public class ReportPortalAppender extends AppenderSkeleton {

	private static final Logger LOGGER = Logger.getLogger(ReportPortalAppender.class);

	private Supplier<BatchedReportPortalService> reportPortalService;
	private Supplier<MessageParser> messageParser;

	/**
	 * Dirty hack. Do not call to singleton injector in moment of creation this
	 * object. Some kind of lazy initialiation to avoid creating new singleton
	 * instance for another classloader
	 */
	public ReportPortalAppender() {
		this(new Supplier<BatchedReportPortalService>() {
			@Override
			public BatchedReportPortalService get() {
				return Injector.getInstance().getBean(BatchedReportPortalService.class);
			}
		}, new Supplier<MessageParser>() {
			@Override
			public MessageParser get() {
				return Injector.getInstance().getBean(MessageParser.class);
			}
		});

	}

	public ReportPortalAppender(Supplier<BatchedReportPortalService> reportPortalService, Supplier<MessageParser> messageParser) {
		/**
		 * USE ReportPortal Services only via suppliers! <br>
		 * If not, log4j forces creatition of provided objects via his own
		 * classloader which is reason of having several singleton objects (one
		 * object for one classloader) <br>
		 * Also, We have to memoize suppliers to avoid creating new instances
		 */
		this.reportPortalService = Suppliers.memoize(reportPortalService);
		this.messageParser = Suppliers.memoize(messageParser);
	}

	@Override
	protected void append(LoggingEvent event) {

		String currentItemId = ReportPortalListenerContext.getRunningNowItemId();
		StringBuilder throwable = new StringBuilder();
		if (null == currentItemId) {
			return;
		}

		if (null == event.getMessage()) {
			return;
		}

		/*
		 * If additional parameter used in logger, for example:
		 * org.apache.log4j.Logger.debug("message", new Throwable()); Then add
		 * stack-trace to logged message string
		 */
		if (null != event.getThrowableInformation()) {
			for (String oneLine : event.getThrowableStrRep())
				throwable.append(oneLine);
		}

		MessageParser messageParser;
		BatchedReportPortalService reportPortalService;
		try {
			messageParser = this.messageParser.get();
			reportPortalService = this.reportPortalService.get();

			ReportPortalMessage message = null;

			if (ReportPortalMessage.class.equals(event.getMessage().getClass())) {
				message = (ReportPortalMessage) event.getMessage();
				event = AppenderUtils.buildNewEvent(event, message.getMessage());
			} else if (File.class.equals(event.getMessage().getClass())) {
				message = new ReportPortalMessage((File) event.getMessage(), "Binary data reported");
				event = AppenderUtils.buildNewEvent(event, message.getMessage());

			} else if (String.class.equals(event.getMessage().getClass()) && messageParser.supports((String) event.getMessage())) {
				message = messageParser.parse((String) event.getMessage());
				event = AppenderUtils.buildNewEvent(event, message.getMessage());
			}

			String logMessage;
			if (this.layout == null) {
				logMessage = event.getRenderedMessage();
			} else {
				logMessage = this.layout.format(event);
			}

			logMessage = logMessage.concat(throwable.toString());
			SaveLogRQ saveLogRQ = AppenderUtils
					.buildSaveLogRQ(event, currentItemId, logMessage, message == null ? null : message.getData());

			AppenderUtils.sendLogToRP(reportPortalService, saveLogRQ);

		} catch (RuntimeException e) {
			/* DO NOT use printStackTrace! */
			// e.printStackTrace();
			LOGGER.error("ReportPortalAppender.append() exception.", e);
			throw e;
		}
	}

	@Override
	public void close() {
	}

	@Override
	public boolean requiresLayout() {
		return true;
	}

}
