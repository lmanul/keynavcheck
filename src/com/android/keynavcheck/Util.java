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
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

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
    return rawImagesAreEqual(a, b, false /* debug */);
  }

  public static boolean rawImagesAreEqual(RawImage a, RawImage b, boolean debug) {
    if (a.data.length != b.data.length) {
      return false;
    }
    if (debug) {
      saveImageOnDisk(a, "a");
      saveImageOnDisk(b, "b");
    }
    // Stop at the last pixel (with three channels) to prevent index overflow errors.
    for (int i = 0; i < a.data.length - 3; i += 3) {
      if (a.getARGB(i) != b.getARGB(i)) {
        return false;
      }
    }
    return true;
  }

  public static void printIdentifiableNodeInfo(ViewNode node) {
    System.err.println("* ID: " + node.id);
    System.err.println("* Position on screen: " + Cyborg.getRectForNode(node));
    if (node.namedProperties.containsKey("text:text")) {
      System.err.println("* Text: " + node.namedProperties.get("text:text").value);
    }
    if (node.namedProperties.containsKey("accessibility:contentDescription")) {
      System.err.println("* Content desc: " + node.namedProperties.get("accessibility:contentDescription").value);
    }
  }

  public static void saveImageOnDisk(RawImage img, String fileNameSuffix) {
    BufferedImage buffered = rawImageToBufferedImage(img);
    saveImageOnDisk(buffered, fileNameSuffix);
  }

  public static void saveImageOnDisk(BufferedImage buffered, String fileNameSuffix) {
    new Thread(() -> {
      long nowMs = System.currentTimeMillis();
      String fileName = nowMs + fileNameSuffix + ".png";
      File out = new File(fileName);
      try {
        ImageIO.write(buffered, "png", out);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }).start();
  }

  public static BufferedImage rawImageToBufferedImage(RawImage raw) {
    BufferedImage image = new BufferedImage(
        raw.width, raw.height, BufferedImage.TYPE_INT_ARGB);

    int i = 0;
    int increment = raw.bpp >> 3;
    for (int y = 0 ; y < raw.height ; y++) {
      for (int x = 0 ; x < raw.width ; x++) {
        int value = raw.getARGB(i);
        i += increment;
        image.setRGB(x, y, value);
      }
    }
    return image;
  }

  public static void paintVisibleOutlineOnImage(Rect r, int color, BufferedImage img) {
    int thickness = 5;
    // TODO: Find contrasting color instead of just white.
    int contrastingColor = 0xffffffff;
    r.shrink(1);
    paintRectOnImage(r, contrastingColor, img);
    r.grow(2);
    for (int i = 0; i < thickness; i++) {
      paintRectOnImage(r, color, img);
      r.grow(1);
    }
    paintRectOnImage(r, contrastingColor, img);
  }

  public static void paintRectOnImage(Rect r, int color, BufferedImage img) {
    // Top and bottom border
    for (int x = r.x; x < r.x + r.w; x++) {
      if (pointIsWithinImageBounds(x, r.y, img)) {
        img.setRGB(x, r.y, color);
      }
      if (pointIsWithinImageBounds(x, r.y + r.h, img)) {
        img.setRGB(x, r.y + r.h, color);
      }
    }

    // Left and right border
    for (int y = r.y; y < r.y + r.h; y++) {
      if (pointIsWithinImageBounds(r.x, y, img)) {
        img.setRGB(r.x, y, color);
      }
      if (pointIsWithinImageBounds(r.x + r.w, y, img)) {
        img.setRGB(r.x + r.w, y, color);
      }
    }
  }

  private static boolean pointIsWithinImageBounds(int x, int y, BufferedImage img) {
    return x >= 0 && x < img.getWidth() && y >= 0 && y < img.getHeight();
  }
}