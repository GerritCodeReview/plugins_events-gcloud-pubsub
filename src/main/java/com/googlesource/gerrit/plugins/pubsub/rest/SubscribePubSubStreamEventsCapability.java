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

import com.google.gerrit.extensions.config.CapabilityDefinition;

public class SubscribePubSubStreamEventsCapability extends CapabilityDefinition {

  public static final String ID = "subscribePubSub";

  @Override
  public String getDescription() {
    return "Subscribe to stream events in PubSub";
  }
}