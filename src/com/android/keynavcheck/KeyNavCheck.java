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
import com.android.cyborg.Filter;
import com.android.cyborg.ViewNode;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class KeyNavCheck extends CyborgTest {

  private static final Set<String> WHITELISTED_INACCESSIBLE_IDS = new HashSet<>();
  static {
    WHITELISTED_INACCESSIBLE_IDS.add("id/back");
    WHITELISTED_INACCESSIBLE_IDS.add("id/home");
    WHITELISTED_INACCESSIBLE_IDS.add("id/recent_apps");
  }

  private static final int MAX_NUMBER_OF_CYCLABLE_ELEMENTS_AT_TOP_LEVEL = 15;
  private static final int MAX_EXPLORATION_CYCLE_LENGTH = 40;

  @Override
  public void setUp() {
  }

  @Override
  public void tearDown() {
  }

  private ViewNode getFocusedNode() {
    List<ViewNode> focusedNodes = this.cyborg.getNodesForObjectsWithFilter(Filter.isFocused());
    if (focusedNodes.size() != 1) {
      System.err.println("\n\nExpecting one focused element, but got " +
          focusedNodes.size() + ", something wrong?");
      return null;
    }
    return focusedNodes.get(0);
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
    pressKeyWithCode(61, 50);
  }

  public void testCycleLengthAtTopLevel() {
    ViewNode initiallyFocusedNode = getFocusedNode();
    Set<String> visitedNodeIds = new HashSet<>();
    while (initiallyFocusedNode == null) {
      System.err.println("Tabbing until we get a focused element...");
      cycle();
      initiallyFocusedNode = getFocusedNode();
    }
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
    System.err.println("Top level cycle has " + numberOfNodesInTopLevelCycle + " elements.");
    assertTrue("There should be fewer than " + MAX_NUMBER_OF_CYCLABLE_ELEMENTS_AT_TOP_LEVEL
            + " elements to tab through at the top level of the activity, but found "
            + numberOfNodesInTopLevelCycle,
        numberOfNodesInTopLevelCycle <= MAX_NUMBER_OF_CYCLABLE_ELEMENTS_AT_TOP_LEVEL);
  }

  public void disabledTestKeyboardNavigation() {
    boolean testPassed = true;

    Set<String> visitedNodeIds = new HashSet<>();
    Set<ViewNode> visitedNodes = new HashSet<>();
    ViewNode initiallyFocusedNode = getFocusedNode();
    while (initiallyFocusedNode == null) {
      System.err.println("Tabbing until we get a focused element...");
      cycle();
      initiallyFocusedNode = getFocusedNode();
    }
    String initiallyFocusedElementId = Util.getUniqueId(initiallyFocusedNode);
    visitedNodeIds.add(initiallyFocusedElementId);

    // Gather clickable nodes anywhere.
    List<ViewNode> clickableNodes =
        this.cyborg.getNodesForObjectsWithFilter(Filter.clickable());

    // Exploration.
    System.err.print("\nScanning screen");
    while(true) {
      System.err.print(".");
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

  public static void main(String[] args) {
    new KeyNavCheck().init();
  }
}
