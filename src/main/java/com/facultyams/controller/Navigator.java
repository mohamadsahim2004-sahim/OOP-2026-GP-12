package com.facultyams.controller;

import java.awt.CardLayout;
import java.util.ArrayDeque;
import java.util.Deque;
import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * Back-stack navigator for CardLayout view switching.
 * Supports push, pop, and reset-to-root operations.
 */
public class Navigator {
    private final JPanel container;
    private final CardLayout cardLayout;
    private final Deque<String> backStack = new ArrayDeque<>();
    private final String rootName;

    public Navigator(JPanel container, CardLayout cardLayout, String rootName) {
        this.container = container;
        this.cardLayout = cardLayout;
        this.rootName = rootName;
        backStack.push(rootName);
    }

    public void registerView(String name, JComponent view) {
        container.add(view, name);
    }

    public void pushView(String name) {
        if (!name.equals(backStack.peek())) {
            backStack.push(name);
        }
        cardLayout.show(container, name);
    }

    public boolean canPop() {
        return backStack.size() > 1;
    }

    public void popView() {
        if (canPop()) {
            backStack.pop();
            String prev = backStack.peek();
            if (prev != null) {
                cardLayout.show(container, prev);
            }
        }
    }

    public void resetToRoot() {
        backStack.clear();
        backStack.push(rootName);
        cardLayout.show(container, rootName);
    }

    public String getCurrentView() {
        return backStack.peek();
    }
}
