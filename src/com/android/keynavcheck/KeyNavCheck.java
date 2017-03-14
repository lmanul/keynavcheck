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
import com.android.cyborg.CyborgTest;
import com.android.cyborg.CyborgTestOptions;
import com.android.cyborg.Filter;
import com.android.cyborg.ViewNode;

import com.android.ddmlib.RawImage;
import java.security.Key;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class KeyNavCheck extends CyborgTest {

  private static final Set<String> WHITELISTED_INACCESSIBLE_IDS = new HashSet<>();
  private static final CyborgTestOptions OPTIONS = new CyborgTestOptions();
  static {
    WHITELISTED_INACCESSIBLE_IDS.add("id/back");  // Main back button
    WHITELISTED_INACCESSIBLE_IDS.add("id/g_icon");  // Pixel launcher
    WHITELISTED_INACCESSIBLE_IDS.add("id/home");  // Main home button
    WHITELISTED_INACCESSIBLE_IDS.add("id/now_header_doodle_view");
    WHITELISTED_INACCESSIBLE_IDS.add("id/recent_apps");  // Main recents button

    OPTIONS.printStackTrace = false;
  }

  private static final int MAX_NUMBER_OF_CYCLABLE_ELEMENTS_AT_TOP_LEVEL = 15;
  private static final int MAX_EXPLORATION_CYCLE_LENGTH = 40;

  private final String pkg;
  private final String activity;

  public KeyNavCheck(String pkg, String activity) {
    super(OPTIONS);
    this.pkg = pkg;
    this.activity = activity;
  }

  @Override
  public void runTests(CyborgTest testObject) {
    killActivityUnderTest();
    super.runTests(testObject);
  }

  @Override
  public void setUp() {
    System.err.println("\n###################################################################\n");
    launchActivityUnderTest();
  }

  @Override
  public void tearDown() {
    killActivityUnderTest();
  }

  private ViewNode getFocusedNode() {
    List<ViewNode> focusedNodes = this.cyborg.getNodesForObjectsWithFilter(Filter.isFocused());
    if (focusedNodes.size() != 1) {
      return null;
    }
    return focusedNodes.get(0);
  }

  private void launchActivityUnderTest() {
    if (!pkg.isEmpty() && !activity.isEmpty()) {
      cyborg.runShellCommand("am start -n " + this.pkg + "/" + this.activity);
      cyborg.onAfterUserInteraction(5000);
    }
  }

  private void killActivityUnderTest() {
    if (!pkg.isEmpty() && !activity.isEmpty()) {
      // This seems to cause problems for discovering UI elements later on. Need to find a less
      // aggressive way of closing the application.
      // cyborg.runShellCommand("am force-stop " + this.pkg);
      cyborg.pressHome();
      cyborg.onAfterUserInteraction(2000);
    }
  }

  private void printIdentifiableNodeInfo(ViewNode node) {
    System.err.println("* ID: " + node.id);
    System.err.println("* Position on screen: " + Cyborg.getRectForNode(node));
    if (node.namedProperties.containsKey("text:text")) {
      System.err.println("* Text: " + node.namedProperties.get("text:text").value);
    }
    if (node.namedProperties.containsKey("accessibility:contentDescription")) {
      System.err.println("* Content desc: " + node.namedProperties.get("accessibility:contentDescription").value);
    }
  }

  private boolean clickable(ViewNode node) {
    return (node.namedProperties.containsKey("misc:clickable") &&
        node.namedProperties.get("misc:clickable").value.equals("true"));
  }

  private void cycle() {
    pressKeyWithCode(61, 10);
  }

  public void testCycleLengthAtTopLevel() throws Exception {
    moveToFirstFocusableNode();
    Set<String> visitedNodeIds = new HashSet<>();
    ViewNode initiallyFocusedNode = getFocusedNode();
    String initiallyFocusedElementId = Util.getUniqueId(initiallyFocusedNode);
    visitedNodeIds.add(initiallyFocusedElementId);

    int numberOfNodesInTopLevelCycle = 1;
    while (numberOfNodesInTopLevelCycle < MAX_EXPLORATION_CYCLE_LENGTH) {
      cycle();
      ViewNode focusedNode = getFocusedNode();
      String focusedNodeId = Util.getUniqueId(focusedNode);
      if (visitedNodeIds.contains(focusedNodeId)) {
        // We're back at a visited node.
        break;
      }
      numberOfNodesInTopLevelCycle++;
    }
    assertTrue("There should be fewer than " + MAX_NUMBER_OF_CYCLABLE_ELEMENTS_AT_TOP_LEVEL
            + " elements to tab through at the top level of the activity, but found "
            + numberOfNodesInTopLevelCycle,
        numberOfNodesInTopLevelCycle <= MAX_NUMBER_OF_CYCLABLE_ELEMENTS_AT_TOP_LEVEL);
  }

  public void testScreenshotIsSelfConsistent() throws Exception {
    // Wait for the UI to stabilize first.
    cyborg.onAfterUserInteraction(1000);
    RawImage a = cyborg.getScreenshot();
    cyborg.wait(200);
    RawImage b = cyborg.getScreenshot();
    assertTrue("Two screenshots taken within a short interval are not identical. Are you using "
        + "a live wallpaper?", Util.rawImagesAreEqual(a, b));
  }

  public void testFocusVisualIndicator() throws Exception {
    moveToFirstFocusableNode();
    Set<String> visitedNodeIds = new HashSet<>();
    ViewNode initiallyFocusedNode = getFocusedNode();
    String initiallyFocusedElementId = Util.getUniqueId(initiallyFocusedNode);
    visitedNodeIds.add(initiallyFocusedElementId);

    int visited = 1;
    RawImage previousScreen = null, currentScreen = null;
    while (visited < MAX_EXPLORATION_CYCLE_LENGTH) {
      cycle();
      visited++;
      ViewNode focusedNode = getFocusedNode();
      String focusedNodeId = Util.getUniqueId(focusedNode);
      if (visitedNodeIds.contains(focusedNodeId)) {
        // We're back at a visited node.
        break;
      }
      currentScreen = cyborg.getScreenshot();
      if (previousScreen != null) {
        assertFalse(Util.rawImagesAreEqual(previousScreen, currentScreen));
      }
      previousScreen = currentScreen;
    }
  }

  public void testAllClickableElementsCanBeAccessed() throws Exception {
    boolean testPassed = true;

    moveToFirstFocusableNode();
    Set<String> visitedNodeIds = new HashSet<>();
    Set<ViewNode> visitedNodes = new HashSet<>();
    ViewNode initiallyFocusedNode = getFocusedNode();
    String initiallyFocusedElementId = Util.getUniqueId(initiallyFocusedNode);
    visitedNodeIds.add(initiallyFocusedElementId);

    // Gather clickable nodes anywhere.
    List<ViewNode> clickableNodes =
        this.cyborg.getNodesForObjectsWithFilter(Filter.clickable());

    // Exploration.
    while(true) {
      cycle();
      ViewNode focusedNode = getFocusedNode();
      if (focusedNode == null) {
        continue;
      }
      String focusedNodeId = Util.getUniqueId(focusedNode);
      if (visitedNodeIds.contains(focusedNodeId)) {
        // We're back at a visited node.
        if (!focusedNodeId.equals(initiallyFocusedElementId)) {
          System.err.println("\nBack at previously visited node but not at initial state. " +
              "Is there a short cycle in keyboard-navigable elements?");
        }
        break;
      }
      visitedNodes.add(focusedNode);
      visitedNodeIds.add(focusedNodeId);
    }

    // Analysis.
    System.err.println("\nCycled through " + visitedNodes.size() + " elements.");
    for (ViewNode node : visitedNodes) {
      if (!clickable(node)) {
        testPassed = false;
        System.err.println(
            "\n\n!!! This element can be focused with the keyboard but isn't clickable:");
        printIdentifiableNodeInfo(node);
      }
    }

    for (ViewNode node : clickableNodes) {
      String nodeId = Util.getUniqueId(node);
      if (!visitedNodeIds.contains(nodeId) && !WHITELISTED_INACCESSIBLE_IDS.contains(node.id)) {
        testPassed = false;
        System.err.println("\n\n!!! This element is clickable but not accessible via keyboard:");
        printIdentifiableNodeInfo(node);
      }
    }

    if (!testPassed) {
      fail("\n\nSome issues were found.");
    }
  }

  private void moveToFirstFocusableNode() {
    ViewNode initiallyFocusedNode = getFocusedNode();
    while (initiallyFocusedNode == null) {
      cycle();
      initiallyFocusedNode = getFocusedNode();
    }
  }

  public static void main(String[] args) {
    String pkg = "", activity = "";
    if (args.length > 0 && args[0].contains("/")) {
      String[] parts = args[0].split("/");
      pkg = parts[0];
      activity = parts[1];
    } else {
      System.err.println("No activity specified in argument (e.g. 'package/activity'), "
          + "using activity currently on top of the stack.");
    }
    new KeyNavCheck(pkg, activity).init();
  }
}
