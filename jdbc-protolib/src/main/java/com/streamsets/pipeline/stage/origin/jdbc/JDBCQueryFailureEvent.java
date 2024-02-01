/*
 * Copyright 2019 StreamSets Inc.
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
package com.streamsets.pipeline.stage.origin.jdbc;

import com.streamsets.pipeline.api.EventDef;
import com.streamsets.pipeline.api.EventFieldDef;
import com.streamsets.pipeline.lib.event.EventCreator;

@EventDef(
    type = JDBCQueryFailureEvent.QUERY_FAILURE_TAG,
    description = "Generated when origin completes processing the data returned from a query.",
    version = JDBCQueryFailureEvent.VERSION
)
public class JDBCQueryFailureEvent {
    public static final String QUERY_FAILURE_TAG = "jdbc-query-failure";
    public static final int VERSION = 1;

    @EventFieldDef(
        name = JDBCQueryFailureEvent.QUERY,
        description = "Query that completed successfully."
    )
    public static final String QUERY = JdbcSource.QUERY;

    @EventFieldDef(
        name = JDBCQueryFailureEvent.TIMESTAMP,
        description = "Timestamp when the query completed."
    )
    public static final String TIMESTAMP = JdbcSource.TIMESTAMP;

    @EventFieldDef(
        name = JDBCQueryFailureEvent.ROW_COUNT,
        description = "Number of processed rows."
    )
    public static final String ROW_COUNT = JdbcSource.ROW_COUNT;

    @EventFieldDef(
        name = JDBCQueryFailureEvent.SOURCE_OFFSET,
        description = "Offset after the query completed."
    )
    public static final String SOURCE_OFFSET = JdbcSource.SOURCE_OFFSET;

    @EventFieldDef(
        name = JDBCQueryFailureEvent.ERROR,
        description = "First error message.",
        optional = true
    )
    public static final String ERROR = JdbcSource.ERROR;

    public static final EventCreator EVENT_CREATOR = new EventCreator.Builder(QUERY_FAILURE_TAG, VERSION)
        .withRequiredField(QUERY)
        .withRequiredField(TIMESTAMP)
        .withRequiredField(ROW_COUNT)
        .withRequiredField(SOURCE_OFFSET)
        .withOptionalField(ERROR)
        .build();
}



