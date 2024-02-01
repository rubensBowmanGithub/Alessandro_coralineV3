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
package com.streamsets.pipeline.lib.googlecloud;

import com.streamsets.pipeline.api.ConfigDef;
import com.streamsets.pipeline.api.ConfigDefBean;
import com.streamsets.pipeline.api.Dependency;
import com.streamsets.pipeline.api.ValueChooserModel;

import java.util.List;

public class GoogleCloudConfig {

  public static final String DATAPROC_IMAGE_VERSION_DEFAULT = "1.4-ubuntu18";
  @ConfigDef(
    required = true,
    type = ConfigDef.Type.MODEL,
    label = "Region",
    group = "DATAPROC",
    displayPosition = 15
  )
  @ValueChooserModel(GoogleCloudRegionChooserValues.class)
  public GoogleCloudRegion region;

  @ConfigDef(
    required = true,
    defaultValue = "",
    type = ConfigDef.Type.STRING,
    label = "Custom Region",
    group = "DATAPROC",
    dependsOn = "region",
    triggeredByValue = "CUSTOM",
    displayPosition = 18
  )
  public String customRegion;

  @ConfigDef(
    required = true,
    defaultValue = "",
    type = ConfigDef.Type.STRING,
    label = "GCS Staging URI",
    description =
      "GCS URI where Transformer resources are staged for pipeline execution. Use the format: gs://<path>.",
    group = "DATAPROC",
    displayPosition = 60
  )
  public String gcsStagingUri;

  @ConfigDef(
    required = false,
    type = ConfigDef.Type.BOOLEAN,
    label = "Create Cluster",
    group = "DATAPROC",
    defaultValue = "false",
    displayPosition = 70
  )
  public boolean create;

  @ConfigDef(
    required = true,
    type = ConfigDef.Type.STRING,
    label = "Cluster Prefix",
    description = "Cluster name is generated by suffixing the pipeline ID to this prefix",
    group = "DATAPROC",
    displayPosition = 80,
    dependsOn = "create",
    triggeredByValue = "true"
  )
  public String clusterPrefix;

  @ConfigDef(
    required = false,
    type = ConfigDef.Type.STRING,
    label = "Image Version",
    description = "The image version to use for the Dataproc cluster. Example: 1.5-ubuntu18. " +
                    "If not specified, Dataproc default is used",
    group = "DATAPROC",
    defaultValue = DATAPROC_IMAGE_VERSION_DEFAULT,
    displayPosition = 85,
    dependsOn = "create",
    displayMode = ConfigDef.DisplayMode.ADVANCED,
    triggeredByValue = "true"
  )
  public String version;

  @ConfigDef(
    required = true,
    type = ConfigDef.Type.MODEL,
    label = "Master Machine Type",
    group = "DATAPROC",
    displayPosition = 90,
    dependsOn = "create",
    triggeredByValue = "true"
  )
  @ValueChooserModel(MachineTypeChooserValues.class)
  public MachineType masterType;

  @ConfigDef(
    required = true,
    type = ConfigDef.Type.MODEL,
    label = "Worker Machine Type",
    group = "DATAPROC",
    displayPosition = 100,
    dependsOn = "create",
    triggeredByValue = "true"
  )
  @ValueChooserModel(MachineTypeChooserValues.class)
  public MachineType workerType;

  @ConfigDef(
    required = true,
    type = ConfigDef.Type.MODEL,
    label = "Network Type",
    group = "DATAPROC",
    displayPosition = 110,
    dependsOn = "create",
    triggeredByValue = "true"
  )
  @ValueChooserModel(NetworkChooserValues.class)
  public Network networkType;

  @ConfigDef(
    required = true,
    type = ConfigDef.Type.STRING,
    label = "Network Name",
    group = "DATAPROC",
    displayPosition = 120,
    dependsOn = "networkType",
    triggeredByValue = "AUTO"
  )
  public String network;

  @ConfigDef(
    required = true,
    type = ConfigDef.Type.STRING,
    label = "Subnet Name",
    group = "DATAPROC",
    displayPosition = 130,
    dependsOn = "networkType",
    triggeredByValue = "CUSTOM"
  )
  public String subnet;

  @ConfigDef(
    required = false,
    type = ConfigDef.Type.LIST,
    label = "Network Tags",
    defaultValue = "[]",
    group = "DATAPROC",
    displayPosition = 140,
    dependsOn = "create",
    triggeredByValue = "true"
  )
  public List<String> tags;

  @ConfigDef(
    required = true,
    type = ConfigDef.Type.NUMBER,
    min = 2,
    label = "Worker Count",
    group = "DATAPROC",
    displayPosition = 150,
    defaultValue = "2",
    dependsOn = "create",
    triggeredByValue = "true"
  )
  public int workerCount;

  @ConfigDef(
    required = true,
    type = ConfigDef.Type.STRING,
    label = "Cluster Name",
    group = "DATAPROC",
    displayPosition = 160,
    dependsOn = "create",
    triggeredByValue = "false"
  )
  public String clusterName;

  @ConfigDef(
    required = false,
    type = ConfigDef.Type.BOOLEAN,
    label = "Terminate Cluster",
    group = "DATAPROC",
    description = "Terminates the cluster when the pipeline stops",
    displayPosition = 170,
    displayMode = ConfigDef.DisplayMode.ADVANCED,
    dependsOn = "create",
    triggeredByValue = "true"
  )
  public boolean terminate;

}
