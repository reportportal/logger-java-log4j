/*
 * Copyright 2017 EPAM Systems
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

import com.epam.reportportal.message.HashMarkSeparatedMessageParser;
import com.epam.reportportal.message.MessageParser;

import java.util.Arrays;
import java.util.List;

/**
 * @author Andrei Varabyeu
 */
final class Util {

    static final MessageParser MESSAGE_PARSER = new HashMarkSeparatedMessageParser();
    private static final List<String> INTERNAL_PACKAGES = Arrays.asList("rp.", "com.epam.reportportal.");

    private Util() {
        //statics only
    }

    static boolean isInternal(String loggerName) {
        if (null == loggerName) {
            return false;
        }

        for (String packagePrefix : INTERNAL_PACKAGES) {
            if (loggerName.startsWith(packagePrefix)) {
                return true;
            }
        }
        return false;
    }
}
