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

import com.google.gerrit.metrics.Description;
import com.google.gerrit.metrics.Field;
import com.google.gerrit.server.logging.PluginMetadata;

public abstract class PubSubEventsMetrics {

  public Field<String> stringField(String metadataKey, String description) {
    return Field.ofString(
            metadataKey,
            (metadataBuilder, fieldValue) ->
                metadataBuilder.addPluginMetadata(PluginMetadata.create(metadataKey, fieldValue)))
        .description(description)
        .build();
  }

  public Description rateDescription(String unit, String description) {
    return new Description(description).setRate().setUnit(unit);
  }
}
