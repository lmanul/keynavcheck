/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.keynavcheck;

import com.android.cyborg.Cyborg;
import com.android.cyborg.Rect;
import com.android.cyborg.ViewNode;
import com.android.ddmlib.RawImage;

public class Util {

  public static String getUniqueId(ViewNode node) {
    if (node == null) {
      return "[null node]";
    }
    Rect rect = Cyborg.getRectForNode(node);
    String id = node.id;
    String text = "";
    String contentDesc = "";
    if (node.namedProperties.containsKey("text:text")) {
      text = node.namedProperties.get("text:text").value;
    }
    if (node.namedProperties.containsKey("accessibility:contentDescription")) {
      contentDesc = node.namedProperties.get("accessibility:contentDescription").value;
    }
    return id + ":" + rect.toString() + ":" + text + ":" + contentDesc;
  }

  public static boolean rawImagesAreEqual(RawImage a, RawImage b) {
    if (a.data.length != b.data.length) {
      return false;
    }
    for (int i = 0; i < a.data.length; i++) {
      if (a.getARGB(i) != b.getARGB(i)) {
        return false;
      }
    }
    return true;
  }
}