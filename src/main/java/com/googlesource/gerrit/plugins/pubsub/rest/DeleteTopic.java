// Copyright (C) 2023 The Android Open Source Project
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

package com.googlesource.gerrit.plugins.pubsub.rest;

import com.google.api.gax.rpc.NotFoundException;
import com.google.common.flogger.FluentLogger;
import com.google.gerrit.entities.Account;
import com.google.gerrit.extensions.annotations.RequiresCapability;
import com.google.gerrit.extensions.common.Input;
import com.google.gerrit.extensions.restapi.Response;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.account.AccountResource;
import com.google.gerrit.server.permissions.PermissionBackend;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.pubsub.v1.TopicName;
import com.googlesource.gerrit.plugins.pubsub.TopicProvider;
import com.googlesource.gerrit.plugins.pubsub.user.PubSubRegistrationHandle;
import com.googlesource.gerrit.plugins.pubsub.user.PubSubUserEventListenerHandlers;
import java.io.IOException;
import java.util.concurrent.ConcurrentMap;

@Singleton
@RequiresCapability(SubscribePubSubStreamEventsCapability.ID)
public class DeleteTopic extends PubSubRestModifyView {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final TopicProvider topicProvider;
  private final ConcurrentMap<Account.Id, PubSubRegistrationHandle>
      pubSubUserStreamEventListenerHandlers;

  @Inject
  public DeleteTopic(
      Provider<CurrentUser> userProvider,
      PermissionBackend permissionBackend,
      TopicProvider topicProvider,
      @PubSubUserEventListenerHandlers
          ConcurrentMap<Account.Id, PubSubRegistrationHandle>
              pubSubUserStreamEventListenerHandlers) {
    super(userProvider, permissionBackend);
    this.topicProvider = topicProvider;
    this.pubSubUserStreamEventListenerHandlers = pubSubUserStreamEventListenerHandlers;
  }

  @Override
  public Response<?> applyImpl(AccountResource rsrc, Input input) throws IOException {
    Account.Id accountId = rsrc.getUser().getAccountId();
    pubSubUserStreamEventListenerHandlers.get(accountId).remove();
    pubSubUserStreamEventListenerHandlers.remove(accountId);
    try {
      TopicName topicName = topicProvider.deleteForAccount(rsrc.getUser().getAccountId());
      logger.atInfo().log("Deleted pubsub topic: %s", topicName.toString());
    } catch (NotFoundException e) {
    }
    return Response.none();
  }
}
