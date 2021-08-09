// Copyright (C) 2021 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.googlesource.gerrit.plugins.pubsub;

import com.gerritforge.gerrit.eventbroker.BrokerApi;
import com.gerritforge.gerrit.eventbroker.TopicSubscriber;
import com.google.api.gax.core.ExecutorProvider;
import com.google.api.gax.core.FixedExecutorProvider;
import com.google.common.collect.Sets;
import com.google.gerrit.extensions.registration.DynamicItem;
import com.google.gerrit.lifecycle.LifecycleModule;
import com.google.gerrit.server.git.WorkQueue;
import com.google.inject.Inject;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import java.util.Set;

public class PubSubApiModule extends LifecycleModule {
  WorkQueue workQueue;
  PubSubConfiguration configuration;

  private Set<TopicSubscriber> activeConsumers = Sets.newHashSet();

  @Inject
  public PubSubApiModule(WorkQueue workQueue, PubSubConfiguration configuration) {
    this.workQueue = workQueue;
    this.configuration = configuration;
  }

  /**
   * Clients(for example multi-site plugin) of events-gcloud-pubsub library are registering
   * consumers. Because we cannot guarantee that client plugin is loaded after the
   * events-gcloud-pubsub we have to make sure that already registered consumers are reassigned to
   * the events-gcloud-pubsub. This injection is optional because if events-gcloud-pubsub plugin is
   * loaded before the client plugin no consumers are registered yet.
   *
   * @param previousBrokerApi
   */
  @Inject(optional = true)
  public void setPreviousBrokerApi(DynamicItem<BrokerApi> previousBrokerApi) {
    if (previousBrokerApi != null && previousBrokerApi.get() != null) {
      this.activeConsumers = previousBrokerApi.get().topicSubscribers();
    }
  }

  @Override
  protected void configure() {
    bind(ExecutorProvider.class)
        .annotatedWith(ConsumerExecutorProvider.class)
        .toInstance(
            FixedExecutorProvider.create(
                workQueue.createQueue(
                    configuration.getNumberOfSubscribers(), "pubsub-subscriber")));

    bind(ExecutorProvider.class)
        .annotatedWith(PublisherExecutorProvider.class)
        .toInstance(
            FixedExecutorProvider.create(
                workQueue.createQueue(configuration.getPublisherThreads(), "pubsub-publisher")));

    bind(new TypeLiteral<Set<TopicSubscriber>>() {}).toInstance(activeConsumers);
    DynamicItem.bind(binder(), BrokerApi.class).to(PubSubBrokerApi.class).in(Scopes.SINGLETON);
  }
}
