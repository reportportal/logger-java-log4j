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

import java.util.Date;
import java.util.UUID;

import org.apache.log4j.spi.LoggingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epam.reportportal.guice.Injector;
import com.epam.reportportal.service.BatchedReportPortalService;
import com.epam.reportportal.utils.files.ImageConverter;
import com.epam.reportportal.utils.properties.ListenerProperty;
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;
import com.google.common.io.ByteSource;

/**
 * Some useful method for logging
 * 
 * @author Andrei Varabyeu
 * 
 */
class AppenderUtils {

	private final static Logger LOGGER = LoggerFactory.getLogger(AppenderUtils.class);

	private static final boolean CONVERT_IMAGE = isConvertImage();

	/**
	 * Save log to report portal
	 * 
	 * @param reportPortalService
	 * @param saveLogRQ
	 */
	public static void sendLogToRP(BatchedReportPortalService reportPortalService, SaveLogRQ saveLogRQ) {
		try {
			reportPortalService.log(saveLogRQ);
		} catch (Exception e) {
			LOGGER.error("Unable to send log message to Report Portal.", e);
		}
	}

	/**
	 * Build {@link SaveLogRQ} object using current test item id and<br>
	 * current {@link LoggingEvent} object.
	 * 
	 * @param event
	 * @param currentItemId
	 * @return
	 */
	public static SaveLogRQ buildSaveLogRQ(LoggingEvent event, String currentItemId, String message, final ByteSource data) {
		SaveLogRQ saveLogRQ = new SaveLogRQ();
		saveLogRQ.setMessage(message);
		saveLogRQ.setLogTime(new Date(event.getTimeStamp()));
		saveLogRQ.setTestItemId(currentItemId);
		saveLogRQ.setLevel(event.getLevel().toString());

		if (null != data) {
			SaveLogRQ.File file = new SaveLogRQ.File();
			file.setContent(CONVERT_IMAGE ? ImageConverter.convertIfImage(data) : data);
			file.setName(UUID.randomUUID().toString());
			saveLogRQ.setFile(file);
		}
		return saveLogRQ;
	}

	/**
	 * Build {@link LoggingEvent} object using existing object and set new
	 * message.<br>
	 * This method can be used for creating new {@link LoggingEvent} object with
	 * out message wrapper.<br>
	 * 
	 * @param event
	 * @param message
	 * @return
	 */
	public static LoggingEvent buildNewEvent(LoggingEvent event, String message) {
		return new LoggingEvent(event.getFQNOfLoggerClass(), event.getLogger(), event.getTimeStamp(), event.getLevel(), message,
				event.getThreadName(), event.getThrowableInformation(), event.getNDC(), event.getLocationInformation(),
				event.getProperties());
	}

	private static boolean isConvertImage() {
		String sIsConvertImage = Injector.getInstance().getProperty(ListenerProperty.IS_CONVERT_IMAGE);
		return Boolean.parseBoolean(sIsConvertImage == null ? "false" : sIsConvertImage);
	}

}
