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

import com.google.auto.value.AutoValue;
import com.google.pubsub.v1.Subscription;

@AutoValue
public abstract class SubscriptionInfo {

  public static SubscriptionInfo create(Subscription subscription, String verificationToken) {
    return new AutoValue_SubscriptionInfo(
        subscription.getName(),
        subscription.getPushConfig().getOidcToken().getAudience(),
        verificationToken,
        subscription.getPushConfig().getOidcToken().getServiceAccountEmail());
  }

  public abstract String subscriptionName();

  public abstract String audience();

  public abstract String verificationToken();

  public abstract String serviceAccoutnEmail();
}
