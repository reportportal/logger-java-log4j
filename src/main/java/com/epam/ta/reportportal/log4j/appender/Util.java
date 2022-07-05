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

import com.epam.reportportal.message.HashMarkSeparatedMessageParser;
import com.epam.reportportal.message.MessageParser;

import java.util.Collections;
import java.util.List;

/**
 * @author Andrei Varabyeu
 */
final class Util {
	static final MessageParser MESSAGE_PARSER = new HashMarkSeparatedMessageParser();
	private static final List<String> LOGGING_ISSUE = Collections.singletonList(
			"com.epam.reportportal.service.logs.LoggingSubscriber");

	private Util() {
		//statics only
	}

	static boolean isInternal(String loggerName) {
		if (null == loggerName) {
			return false;
		}

		for (String packagePrefix : LOGGING_ISSUE) {
			if (loggerName.startsWith(packagePrefix)) {
				return true;
			}
		}
		return false;
	}
}
