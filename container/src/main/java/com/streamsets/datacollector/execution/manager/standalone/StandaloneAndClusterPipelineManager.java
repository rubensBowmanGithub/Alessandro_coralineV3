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
package com.streamsets.datacollector.execution.manager.standalone;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.streamsets.datacollector.config.ConnectionConfiguration;
import com.streamsets.datacollector.event.dto.PipelineStartEvent;
import com.streamsets.datacollector.event.handler.remote.RemoteDataCollector;
import com.streamsets.datacollector.execution.EventListenerManager;
import com.streamsets.datacollector.execution.Manager;
import com.streamsets.datacollector.execution.PipelineState;
import com.streamsets.datacollector.execution.PipelineStateStore;
import com.streamsets.datacollector.execution.PipelineStatus;
import com.streamsets.datacollector.execution.PreviewStatus;
import com.streamsets.datacollector.execution.Previewer;
import com.streamsets.datacollector.execution.PreviewerListener;
import com.streamsets.datacollector.execution.Runner;
import com.streamsets.datacollector.execution.StateEventListener;
import com.streamsets.datacollector.execution.StatsCollectorPreviewer;
import com.streamsets.datacollector.execution.StatsCollectorRunner;
import com.streamsets.datacollector.execution.manager.PipelineManagerException;
import com.streamsets.datacollector.execution.manager.PreviewerProvider;
import com.streamsets.datacollector.execution.manager.RunnerProvider;
import com.streamsets.datacollector.main.RuntimeInfo;
import com.streamsets.datacollector.metrics.MetricsCache;
import com.streamsets.datacollector.metrics.MetricsConfigurator;
import com.streamsets.datacollector.security.GroupsInScope;
import com.streamsets.datacollector.security.kafka.KafkaKerberosUtil;
import com.streamsets.datacollector.stagelibrary.StageLibraryTask;
import com.streamsets.datacollector.store.PipelineInfo;
import com.streamsets.datacollector.store.PipelineStoreException;
import com.streamsets.datacollector.store.PipelineStoreTask;
import com.streamsets.datacollector.task.AbstractTask;
import com.streamsets.datacollector.usagestats.StatsCollector;
import com.streamsets.datacollector.util.Configuration;
import com.streamsets.datacollector.util.ContainerError;
import com.streamsets.datacollector.util.PipelineException;
import com.streamsets.datacollector.validation.ValidationError;
import com.streamsets.dc.execution.manager.standalone.ResourceManager;
import com.streamsets.pipeline.api.ExecutionMode;
import com.streamsets.pipeline.api.impl.Utils;
import com.streamsets.pipeline.lib.executor.SafeScheduledExecutorService;
import com.streamsets.pipeline.lib.util.ExceptionUtils;
import dagger.ObjectGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class StandaloneAndClusterPipelineManager extends AbstractTask implements Manager, PreviewerListener {

  private static final Logger LOG = LoggerFactory.getLogger(StandaloneAndClusterPipelineManager.class);
  private static final String PIPELINE_MANAGER = "PipelineManager";

  @VisibleForTesting
  static final Set<PosixFilePermission> GLOBAL_ALL_PERM = PosixFilePermissions.fromString(
      "rwxrwxrwx"
  );

  @VisibleForTesting
  static final Set<PosixFilePermission> USER_ONLY_PERM = PosixFilePermissions.fromString(
      "rwx------"
  );

  private final ObjectGraph objectGraph;

  @Inject RuntimeInfo runtimeInfo;
  @Inject Configuration configuration;
  @Inject PipelineStoreTask pipelineStore;
  @Inject PipelineStateStore pipelineStateStore;
  @Inject StageLibraryTask stageLibrary;
  @Inject @Named("managerExecutor") SafeScheduledExecutorService managerExecutor;
  @Inject RunnerProvider runnerProvider;
  @Inject PreviewerProvider previewerProvider;
  @Inject ResourceManager resourceManager;
  @Inject EventListenerManager eventListenerManager;
  @Inject StatsCollector statsCollector;

  private Cache<String, RunnerInfo> runnerCache;
  private Cache<String, Previewer> previewerCache;
  static final boolean DEFAULT_RUNNER_RESTART_PIPELINES = true;
  static final String RUNNER_RESTART_PIPELINES = "runner.boot.pipeline.restart";
  private ScheduledFuture<?> previewCleanerFuture;
  private static final String NAME_AND_REV_SEPARATOR = "::";

  private final KafkaKerberosUtil kafkaKerberosUtil;

  public StandaloneAndClusterPipelineManager(ObjectGraph objectGraph) {
    super(PIPELINE_MANAGER);
    this.objectGraph = objectGraph;
    this.objectGraph.inject(this);
    eventListenerManager.addStateEventListener(resourceManager);
    MetricsConfigurator.registerJmxMetrics(runtimeInfo.getMetrics());
    kafkaKerberosUtil = new KafkaKerberosUtil(configuration);
  }

  @Override
  public void addStateEventListener(StateEventListener listener) {
    eventListenerManager.addStateEventListener(listener);
  }

  @Override
  public Previewer createPreviewer(
      String user,
      String name,
      String rev,
      List<PipelineStartEvent.InterceptorConfiguration> interceptorConfs,
      Function<Object, Void> afterActionsFunction,
      boolean remote,
      Map<String, ConnectionConfiguration> connections
  ) throws PipelineException {
    if (!pipelineStore.hasPipeline(name)) {
      throw new PipelineStoreException(ContainerError.CONTAINER_0200, name);
    }
    Previewer previewer = previewerProvider.createPreviewer(
        user,
        name,
        rev,
        this,
        objectGraph,
        interceptorConfs,
        afterActionsFunction,
        remote,
        connections
    );
    previewer = new StatsCollectorPreviewer(previewer, statsCollector);
    previewerCache.put(previewer.getId(), previewer);
    return previewer;
  }

  @Override
  public Previewer getPreviewer(String previewerId) {
    Utils.checkNotNull(previewerId, "previewerId");
    Previewer previewer = previewerCache.getIfPresent(previewerId);
    if (previewer == null) {
      LOG.warn("Cannot find the previewer in cache for id: '{}'", previewerId);
    }
    return previewer;
  }

  @Override
  @SuppressWarnings("deprecation")
  public Runner getRunner(final String name, final String rev) throws PipelineException {
    if (!pipelineStore.hasPipeline(name)) {
      throw new PipelineStoreException(ContainerError.CONTAINER_0200, name);
    }
    final String nameAndRevString = getNameAndRevString(name, rev);
    RunnerInfo runnerInfo;
    try {
      runnerInfo = runnerCache.get(nameAndRevString, () -> {
        ExecutionMode executionMode = pipelineStateStore.getState(name, rev).getExecutionMode();
        Runner runner = getRunner(name, rev, executionMode);
        return new RunnerInfo(runner, executionMode);
      });
      ExecutionMode cachedExecutionMode = runnerInfo.executionMode;
      ExecutionMode persistentExecutionMode = pipelineStateStore.getState(name, rev).getExecutionMode();
      if (cachedExecutionMode == ExecutionMode.CLUSTER) {
        LOG.info("Upgrading execution mode from " + ExecutionMode.CLUSTER + " to " + persistentExecutionMode);
        runnerInfo.executionMode = persistentExecutionMode;
      }
      if (runnerInfo.executionMode != pipelineStateStore.getState(name, rev).getExecutionMode()) {
        LOG.info(Utils.format("Invalidate the existing runner for pipeline '{}::{}' as execution mode has changed",
          name, rev));
        if (!removeRunnerIfNotActive(runnerInfo.runner)) {
          throw new PipelineManagerException(ValidationError.VALIDATION_0082, pipelineStateStore.getState(name, rev).getExecutionMode(),
            runnerInfo.executionMode);
        } else {
          return getRunner(name, rev);
        }
      }
    } catch (ExecutionException ex) {
      if (ex.getCause() instanceof RuntimeException) {
        throw (RuntimeException) ex.getCause();
      } else if (ex.getCause() instanceof PipelineStoreException) {
        throw (PipelineStoreException) ex.getCause();
      } else {
        throw new PipelineStoreException(ContainerError.CONTAINER_0114, ex.toString(), ex);
      }
    }
    return runnerInfo.runner;
  }

  @Override
  public List<PipelineState> getPipelines() throws PipelineStoreException {
    List<PipelineInfo> pipelineInfoList = pipelineStore.getPipelines();
    List<PipelineState> pipelineStateList = new ArrayList<>();
    for (PipelineInfo pipelineInfo : pipelineInfoList) {
      String name = pipelineInfo.getPipelineId();
      String rev = pipelineInfo.getLastRev();
      try {
        PipelineState pipelineState = pipelineStateStore.getState(name, rev);
        pipelineStateList.add(pipelineState);
      } catch (Exception e) {
        LOG.error(Utils.format("State file not found for pipeline {}", name), e);
      }
    }
    return pipelineStateList;
  }

  @Override
  public PipelineState getPipelineState(String name, String rev) throws PipelineStoreException {
    return pipelineStateStore.getState(name, rev);
  }

  @Override
  public boolean isPipelineActive(String name, String rev) throws PipelineException {
    if (!pipelineStore.hasPipeline(name)) {
      throw new PipelineStoreException(ContainerError.CONTAINER_0200, name);
    }
    RunnerInfo runnerInfo = runnerCache.getIfPresent(getNameAndRevString(name, rev));
    return runnerInfo != null && runnerInfo.runner.getState().getStatus().isActive();
  }

  @Override
  public void runTask() {
    previewerCache = new MetricsCache<>(
      runtimeInfo.getMetrics(),
      "manager-previewer-cache",
        CacheBuilder.newBuilder()
          .expireAfterAccess(5, TimeUnit.MINUTES).removalListener((RemovalListener<String, Previewer>) removal -> {
            Previewer previewer = removal.getValue();
            LOG.warn(
                "Evicting idle previewer '{}::{}'::'{}' in status '{}'",
                previewer.getName(),
                previewer.getRev(),
                previewer.getId(),
                previewer.getStatus()
            );
            try {
              previewer.stop();
            } catch (Exception e) {
              LOG.warn(
                  "{} attempting to stop evicted previewer: {}",
                  e.getClass().getSimpleName(),
                  e.getMessage(),
                  e
              );
            }
          }).build()
    );

    // Create a background thread to cleanup the previewerCache because guava doesn't always want to do it on its own
    previewCleanerFuture = managerExecutor.scheduleWithFixedDelay(() -> {
      LOG.debug("Triggering previewer cache cleanup");
      previewerCache.cleanUp();
    }, 5, 5, TimeUnit.MINUTES);

    runnerCache = new MetricsCache<>(
      runtimeInfo.getMetrics(),
      "manager-runner-cache",
        CacheBuilder.newBuilder()
            .build());
    RunnerEvictionListener runnerEvictionListener = new RunnerEvictionListener(runnerCache);
    eventListenerManager.addStateEventListener(runnerEvictionListener);

    // On SDC start up we will try by default start all pipelines that were running at the time SDC was shut down. This
    // can however be disabled via cor.properties config. Especially helpful when starting all pipeline at once could
    // lead to troubles.
    boolean restartPipelines = configuration.get(RUNNER_RESTART_PIPELINES, DEFAULT_RUNNER_RESTART_PIPELINES);

    List<PipelineInfo> pipelineInfoList;
    try {
      pipelineInfoList = pipelineStore.getPipelines();
    } catch (PipelineStoreException ex) {
      throw new RuntimeException("Cannot load the list of pipelines from StateStore", ex);
    }
    for (PipelineInfo pipelineInfo : pipelineInfoList) {
      String name = pipelineInfo.getPipelineId();
      String rev = pipelineInfo.getLastRev();
      try {
        if (isRemotePipeline(name, rev) && !runtimeInfo.isDPMEnabled()) {
          LOG.info(Utils.format("Not activating remote pipeline'{}:{}' as DPM is disabled ", name, rev));
          continue;
        }
        PipelineState pipelineState = pipelineStateStore.getState(name, rev);
        // Create runner if active
        if (pipelineState.getStatus().isActive()) {
          ExecutionMode executionMode = pipelineState.getExecutionMode();
          Runner runner = getRunner(name, rev, executionMode);
          runner.prepareForDataCollectorStart(pipelineState.getUser());
          if (restartPipelines && runner.getState().getStatus() == PipelineStatus.DISCONNECTED) {
            runnerCache.put(getNameAndRevString(name, rev), new RunnerInfo(runner, executionMode));
            try {
              String user = pipelineState.getUser();
              // we need to skip enforcement user groups in scope.
              GroupsInScope.executeIgnoreGroups(() -> {
                runner.onDataCollectorStart(user);
                return null;
              });
            } catch (Exception ex) {
              ExceptionUtils.throwUndeclared(ex.getCause());
            }
          }
        }
      } catch (Exception ex) {
        LOG.error(Utils.format("Error while processing pipeline '{}::{}'", name, rev), ex);
      }
    }
  }

  @VisibleForTesting
  boolean isRunnerPresent(String name, String rev) {
     return runnerCache.getIfPresent(getNameAndRevString(name, rev)) != null;

  }

  private boolean removeRunnerIfNotActive(Runner runner) throws PipelineStoreException {
    if (!runner.getState().getStatus().isActive()) {
      // first invalidate the cache and then close the runner, so a closed runner can never
      // sit in cache
      runnerCache.invalidate(getNameAndRevString(runner.getName(), runner.getRev()));
      runner.close();
      LOG.info("Removing runner for pipeline '{}::'{}'", runner.getName(), runner.getRev());
      return true;
    } else {
      return false;
    }
  }

  @Override
  protected void initTask() {
    if (RuntimeInfo.SDC_PRODUCT.equals(runtimeInfo.getProductName())) {
      LOG.debug("Initializing task for Data Collector; attempting to clean any existing Kafka temp keytab directory");
      cleanUpKafkaKeytabDir();
    }
  }

  private void cleanUpKafkaKeytabDir() {
    try {
      kafkaKerberosUtil.cleanUpKeytabDirectory();
    } catch (IOException e) {
      LOG.error("IOException attempting to clean up temp keytab directory", e);
    }
  }

  @Override
  public void stopTask() {
    if(runnerCache != null) {
      for (RunnerInfo runnerInfo : runnerCache.asMap().values()) {
        Runner runner = runnerInfo.runner;
        try {
          runner.close();
          PipelineState pipelineState = pipelineStateStore.getState(runner.getName(), runner.getRev());
          runner.onDataCollectorStop(pipelineState.getUser());
        } catch (Exception e) {
          LOG.warn("Failed to stop the runner for pipeline: {} and rev: {} due to: {}", runner.getName(),
            runner.getRev(), e.toString(), e);
        }
      }
      runnerCache.invalidateAll();
      for (Previewer previewer : previewerCache.asMap().values()) {
        try {
          previewer.stop();
        } catch (Exception e) {
          LOG.warn("Failed to stop the previewer: {}::{}::{} due to: {}", previewer.getName(),
            previewer.getRev(), previewer.getId(), e.toString(), e);
        }
      }
    }
    if(previewerCache != null) {
      previewerCache.invalidateAll();
    }
    if (previewCleanerFuture != null) {
      previewCleanerFuture.cancel(true);
    }
    if (managerExecutor != null) {
      managerExecutor.shutdown();
    }
    if (RuntimeInfo.SDC_PRODUCT.equals(runtimeInfo.getProductName())) {
      /**
       * HACK ALERT: see SDC-15032
       */
      cleanUpKafkaKeytabDir();
    }
    LOG.info("Stopped Production Pipeline Manager");
  }

  @Override
  public void statusChange(String id, PreviewStatus status) {
    LOG.debug("Status of previewer with id: '{}' changed to status: '{}'", id, status);
    statsCollector.previewStatusChanged(status, getPreviewer(id));
  }

  @Override
  public void outputRetrieved(String id) {
    LOG.debug("Removing previewer with id:  '{}' from cache as output is retrieved", id);
    previewerCache.invalidate(id);
  }

  @Override
  public boolean isRemotePipeline(String name, String rev) throws PipelineStoreException {
    Object isRemote = pipelineStateStore.getState(name, rev).getAttributes().get(RemoteDataCollector.IS_REMOTE_PIPELINE);
    // remote attribute will be null for pipelines with version earlier than 1.3
    return (isRemote == null) ? false : (boolean) isRemote;
  }

  private Runner getRunner(String name, String rev, ExecutionMode executionMode) throws PipelineStoreException {
    if(executionMode == null) {
      executionMode = ExecutionMode.STANDALONE;
    }
    Runner runner = runnerProvider.createRunner(name, rev, objectGraph, executionMode);
    return new StatsCollectorRunner(runner, statsCollector);
  }

  static String getNameAndRevString(String name, String rev) {
    return name + NAME_AND_REV_SEPARATOR + rev;
  }

  static class RunnerInfo {
    private final Runner runner;
    private ExecutionMode executionMode;

    RunnerInfo(Runner runner, ExecutionMode executionMode) {
      this.runner = runner;
      this.executionMode = executionMode;
    }

    public Runner getRunner() {
      return runner;
    }
  }

}
