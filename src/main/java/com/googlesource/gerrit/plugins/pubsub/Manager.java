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
import com.google.gerrit.extensions.events.LifecycleListener;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Set;

@Singleton
public class Manager implements LifecycleListener {

  private final Set<TopicSubscriber> consumers;
  private final BrokerApi brokerApi;
  private final PubSubEventListener pubSubEventListener;

  @Inject
  public Manager(
      Set<TopicSubscriber> consumers,
      BrokerApi brokerApi,
      PubSubEventListener pubSubEventListener) {
    this.consumers = consumers;
    this.brokerApi = brokerApi;
    this.pubSubEventListener = pubSubEventListener;
  }

  @Override
  public void start() {
    consumers.forEach(
        topicSubscriber ->
            brokerApi.receiveAsync(topicSubscriber.topic(), topicSubscriber.consumer()));
  }

  @Override
  public void stop() {
    brokerApi.disconnect();
    pubSubEventListener.disconnect();
  }
}
