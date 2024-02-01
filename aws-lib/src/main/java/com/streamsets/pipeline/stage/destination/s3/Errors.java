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
package com.streamsets.pipeline.stage.destination.s3;

import com.streamsets.pipeline.api.ErrorCode;
import com.streamsets.pipeline.api.GenerateResourceBundle;

@GenerateResourceBundle
public enum Errors implements ErrorCode {

  S3_01("Bucket name is empty for record {}"),
  S3_02("Bucket '{}' does not exist"),
  S3_03("Invalid partition template expression '{}': {}"),
  S3_04("Invalid time basis expression '{}': {}"),
  S3_05("File Name Prefix cannot be empty"),
  S3_06("File Name Suffix contains '/' or starts with '.'"),

  S3_10("A problem occurred while generating JSON for the security context"),

  S3_20("Cannot connect to Amazon S3, reason : {}"),
  S3_21("Unable to write object to Amazon S3, reason : {}"),

  S3_30("Unsupported data format '{}'"),
  S3_31("Field cannot be empty"),
  S3_32("Error serializing record '{}': {}"),

  S3_40("Internal Error {}"),

  S3_50("Compression Option not supported for Whole file Data format"),
  S3_51("Object Key {} already exists"),
  S3_52("Cannot Write Record : {}"),

  ;

  private final String msg;
  Errors(String msg) {
    this.msg = msg;
  }

  @Override
  public String getCode() {
    return name();
  }

  @Override
  public String getMessage() {
    return msg;
  }

}
