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

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.rpc.NotFoundException;
import com.google.cloud.pubsub.v1.TopicAdminClient;
import com.google.cloud.pubsub.v1.TopicAdminSettings;
import com.google.common.flogger.FluentLogger;
import com.google.gerrit.extensions.common.Input;
import com.google.gerrit.extensions.restapi.AuthException;
import com.google.gerrit.extensions.restapi.BadRequestException;
import com.google.gerrit.extensions.restapi.ResourceConflictException;
import com.google.gerrit.extensions.restapi.Response;
import com.google.gerrit.extensions.restapi.RestModifyView;
import com.google.gerrit.server.account.AccountResource;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.pubsub.v1.TopicName;
import java.io.IOException;

@Singleton
public class DeleteTopic implements RestModifyView<AccountResource, Input> {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final TopicAdminSettings topicAdminSettings;
  private final PubsubTopicNameFactory topicNameFactory;

  @Inject
  public DeleteTopic(CredentialsProvider credentials, PubsubTopicNameFactory topicNameFactory)
      throws IOException {
    this.topicAdminSettings =
        TopicAdminSettings.newBuilder().setCredentialsProvider(credentials).build();
    this.topicNameFactory = topicNameFactory;
  }

  @Override
  public Response<?> apply(AccountResource rsrc, Input input)
      throws AuthException, BadRequestException, ResourceConflictException, IOException {
    try (TopicAdminClient topicAdminClient = TopicAdminClient.create(topicAdminSettings)) {
      TopicName topicName = topicNameFactory.createForAccount(rsrc.getUser().getAccountId());
      topicAdminClient.deleteTopic(topicName);
      logger.atInfo().log("Deleted pubsub topic: %s", topicName.toString());
    } catch (NotFoundException e) {
    }
    return Response.none();
  }
}