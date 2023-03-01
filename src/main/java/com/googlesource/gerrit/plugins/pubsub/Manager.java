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
import com.google.api.gax.core.CredentialsProvider;
import com.google.cloud.pubsub.v1.TopicAdminClient;
import com.google.cloud.pubsub.v1.TopicAdminSettings;
import com.google.common.flogger.FluentLogger;
import com.google.gerrit.entities.Account;
import com.google.gerrit.extensions.events.LifecycleListener;
import com.google.gerrit.server.IdentifiedUser;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.pubsub.v1.ProjectName;
import com.google.pubsub.v1.Topic;
import com.googlesource.gerrit.plugins.pubsub.user.PubSubRegistrationHandle;
import com.googlesource.gerrit.plugins.pubsub.user.PubSubUserEventListenerHandlers;
import com.googlesource.gerrit.plugins.pubsub.user.PubSubUserScopedEventListener;
import com.googlesource.gerrit.plugins.pubsub.user.PubSubUserTopicNameFactory;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

@Singleton
public class Manager implements LifecycleListener {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final Set<TopicSubscriber> consumers;
  private final BrokerApi brokerApi;
  private final PubSubEventListener pubSubEventListener;
  private final ConcurrentMap<Account.Id, PubSubRegistrationHandle>
      pubSubUserStreamEventListenerHandlers;
  private final CredentialsProvider credentials;
  private final String gcpProjectId;
  private final PubSubUserTopicNameFactory topicNameFactory;
  private final PubSubUserScopedEventListener.Factory userScopedEventListenerFactory;
  private final IdentifiedUser.GenericFactory userFactory;

  @Inject
  public Manager(
      Set<TopicSubscriber> consumers,
      BrokerApi brokerApi,
      PubSubEventListener pubSubEventListener,
      @PubSubUserEventListenerHandlers
          ConcurrentMap<Account.Id, PubSubRegistrationHandle> pubSubUserStreamEventListenerHandlers,
      CredentialsProvider credentials,
      PubSubConfiguration config,
      PubSubUserTopicNameFactory topicNameFactory,
      PubSubUserScopedEventListener.Factory userScopedEventListenerFactory,
      IdentifiedUser.GenericFactory userFactory) {
    this.consumers = consumers;
    this.brokerApi = brokerApi;
    this.pubSubEventListener = pubSubEventListener;
    this.pubSubUserStreamEventListenerHandlers = pubSubUserStreamEventListenerHandlers;
    this.credentials = credentials;
    this.gcpProjectId = config.getGCloudProject();
    this.topicNameFactory = topicNameFactory;
    this.userScopedEventListenerFactory = userScopedEventListenerFactory;
    this.userFactory = userFactory;
  }

  @Override
  public void start() {
    consumers.forEach(
        topicSubscriber ->
            brokerApi.receiveAsync(topicSubscriber.topic(), topicSubscriber.consumer()));
    registerExistingUserTopics();
  }

  @Override
  public void stop() {
    brokerApi.disconnect();
    pubSubEventListener.disconnect();
    disconnectFromUserTopics();
  }

  private void registerExistingUserTopics() {
    logger.atInfo().log("Registering existing PubSub EventListeners.");
    try {
      TopicAdminSettings topicAdminSettings =
          TopicAdminSettings.newBuilder().setCredentialsProvider(credentials).build();
      try (TopicAdminClient topicAdminClient = TopicAdminClient.create(topicAdminSettings)) {
        topicAdminClient
            .listTopics(ProjectName.of(gcpProjectId).toString())
            .iterateAll()
            .forEach(this::registerUserTopic);
      }
    } catch (IOException e) {
      logger.atSevere().withCause(e).log("Failed to list PubSub topics.");
    }
  }

  // TODO: this hould happen asynchronuously, since this can take quite some time.
  private void registerUserTopic(Topic topic) {
    logger.atInfo().log("Registering topic %s.", topic.getName());
    Optional<Account.Id> optAccountId =
        topicNameFactory.getAccountId(topic.getName().split("/")[3]);
    if (optAccountId.isPresent()) {
      userScopedEventListenerFactory.create(userFactory.create(optAccountId.get()));
    }
  }

  private void disconnectFromUserTopics() {
    pubSubUserStreamEventListenerHandlers.forEach((accountId, handle) -> handle.remove());
  }
}
