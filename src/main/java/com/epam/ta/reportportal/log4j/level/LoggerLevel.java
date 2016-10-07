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
package com.epam.ta.reportportal.log4j.level;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Level;

/**
 * Logging levels for logger {@link Logger}
 * 
 * @author Andrei Varabyeu
 */
public class LoggerLevel extends org.apache.log4j.Level {

	private static final long serialVersionUID = -4862897722711904220L;

	private static final Map<Integer, LoggerLevel> LOGGER_LEVELS;

	static {
		LOGGER_LEVELS = Collections.synchronizedMap(new HashMap<Integer, LoggerLevel>());
	}

	final static public int BINARY_INT = FATAL_INT - 1;

	final static public Level BINARY = new LoggerLevel(BINARY_INT, "HTML_OUTPUT", 0);

	public static Level toLoggerLevel(int val, Level defaultLevel) {
		Level level = toLevel(val, null);
		if (level != null) {
			return level;
		}

		level = LOGGER_LEVELS.get(val);
		if (level != null) {
			return level;
		}

		return defaultLevel;
	}

	protected LoggerLevel(int level, String levelStr, int syslogEquivalent) {
		super(level, levelStr, syslogEquivalent);
		LOGGER_LEVELS.put(level, this);
	}

	public static Level toLevel(int val) {
		return LOGGER_LEVELS.get(val);
	}
}
