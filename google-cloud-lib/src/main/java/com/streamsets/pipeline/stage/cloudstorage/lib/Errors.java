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

package com.streamsets.pipeline.stage.cloudstorage.lib;

import com.streamsets.pipeline.api.ErrorCode;

public enum Errors implements ErrorCode {
  GCS_00("An error occurred while processing the records from object: '{}' at offset: '{}'. Reason: {}"),
  GCS_01("Error validating permissions: '{}'"),
  GCS_02("Error writing record '{}'. Reason : {}"),
  GCS_03("File Path '{}' already exists"),
  GCS_04("Error evaluating EL. Reason {}"),
  GCS_05("Object Name Suffix contains '/' or starts with '.'"),
  GCS_06("Error handling failed for {}.Reason: {}"),
  GCS_07("Batch size greater than maximal batch size allowed in cor.properties, maxBatchSize: {}"),
  GCS_08("Error happened when creating Output stream"),
  GCS_09("Error happened when writing to Output stream"),
  ;

  private final String msg;

  Errors(String msg) {
    this.msg = msg;
  }

  /** {@inheritDoc} */
  @Override
  public String getCode() {
    return name();
  }

  /** {@inheritDoc} */
  @Override
  public String getMessage() {
    return msg;
  }
}
