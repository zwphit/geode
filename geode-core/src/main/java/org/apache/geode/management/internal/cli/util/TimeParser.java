/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.geode.management.internal.cli.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.apache.geode.management.internal.cli.commands.ExportLogsCommand;

/**
 * Extracted from {@link org.apache.geode.management.internal.cli.functions.ExportLogsFunction}.
 */
public class TimeParser {

  public static LocalDateTime parseTime(String dateString) {
    if (dateString == null) {
      return null;
    }

    try {
      SimpleDateFormat df = new SimpleDateFormat(ExportLogsCommand.FORMAT);
      return df.parse(dateString).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    } catch (ParseException e) {
      try {
        SimpleDateFormat df = new SimpleDateFormat(ExportLogsCommand.ONLY_DATE_FORMAT);
        return df.parse(dateString).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
      } catch (ParseException e1) {
        return null;
      }
    }
  }
}