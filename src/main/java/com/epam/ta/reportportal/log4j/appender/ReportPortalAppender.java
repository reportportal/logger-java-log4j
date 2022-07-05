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
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Layout;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.spi.LoggingEvent;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.UUID;

import static com.epam.reportportal.service.ReportPortal.emitLog;

/**
 * Log4j appender for report portal
 *
 * @author Andrei Varabyeu
 */
public class ReportPortalAppender extends AppenderSkeleton {

	@Override
	protected void append(final LoggingEvent event) {

		if (null == event.getMessage()) {
			return;
		}

		//make sure we are not logging themselves
		if (Util.isInternal(event.getLoggerName())) {
			return;
		}

		emitLog(itemUuid -> {
			SaveLogRQ request = new SaveLogRQ();
			request.setLevel(event.getLevel().toString());
			request.setLogTime(new Date(event.getTimeStamp()));
			request.setItemUuid(itemUuid);

			String logMessage = null;
			try {
				ReportPortalMessage message = null;

				/*
				 * If additional parameter used in logger, for example:
				 * org.apache.log4j.Logger.debug("message", new Throwable()); Then add
				 * stack-trace to logged message string
				 */
				StringBuilder throwable = new StringBuilder();
				if (null != event.getThrowableInformation() && null != event.getThrowableInformation().getThrowable()) {
					throwable.append(ExceptionUtils.getStackTrace(event.getThrowableInformation().getThrowable()));
				}

				// ReportPortalMessage is reported
				if (event.getMessage() instanceof ReportPortalMessage) {
					message = (ReportPortalMessage) event.getMessage();

					// File is reported
				} else if (event.getMessage() instanceof File) {
					message = new ReportPortalMessage((File) event.getMessage(), "Binary data reported");

					// Parsable String is reported
				} else if (event.getMessage() instanceof String && Util.MESSAGE_PARSER.supports((String) event.getMessage())) {
					message = Util.MESSAGE_PARSER.parse((String) event.getMessage());
				}

				// There is some binary data reported
				if (null != message && null != message.getData()) {
					logMessage = message.getMessage();

					SaveLogRQ.File file = new SaveLogRQ.File();
					file.setContentType(message.getData().getMediaType());
					file.setContent(message.getData().read());
					file.setName(UUID.randomUUID().toString());
					request.setFile(file);

				} else {
					// Plain string message is reported
					Layout myLayout = getLayout();
					if (myLayout == null) {
						logMessage = event.getRenderedMessage();
					} else {
						if (myLayout instanceof PatternLayout) {
							// a Log4j 1.2 multi-threaded bug workaround
							myLayout = new PatternLayout(((PatternLayout) myLayout).getConversionPattern());
						}
						logMessage = myLayout.format(event).concat(throwable.toString());
					}
				}

			} catch (IOException e) {
				//do nothing
			}

			request.setMessage(logMessage);
			return request;
		});
	}

	@Override
	public void close() {
	}

	@Override
	public boolean requiresLayout() {
		return true;
	}

}
