/*
 * Copyright 2017 StreamSets Inc.
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
package com.streamsets.pipeline.api.ext;

import java.io.Closeable;
import java.io.IOException;

/**
 * This is private interface for some internal Data Collector stages. This interface should not be used as it can change
 * in non-compatible ways at any time.
 */
public interface JsonObjectReader extends Closeable {

  public static Object EOF = new Object();

  Object read() throws IOException;

  long getReaderPosition();

  default Class<?> getExpectedClass() {
    return Object.class;
  }
}
