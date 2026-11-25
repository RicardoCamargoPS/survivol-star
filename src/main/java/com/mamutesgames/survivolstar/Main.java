package com.mamutesgames.survivolstar;

import com.mamutesgames.survivolstar.core.Game;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

public final class Main {

    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;

    private Main() {
    }

    public static void main(String[] args) {
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) {
            throw new IllegalStateException("Não foi possível inicializar o GLFW.");
        }

        long window = 0L;
        Game game = null;
        try {
            glfwDefaultWindowHints();
            glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 2);
            glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 1);
            glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

            window = glfwCreateWindow(WIDTH, HEIGHT, "Survivol Star", 0L, 0L);
            if (window == 0L) {
                throw new IllegalStateException("Não foi possível criar a janela do jogo.");
            }

            glfwMakeContextCurrent(window);
            glfwSwapInterval(1);
            GL.createCapabilities();

            game = new Game(window);
            Game activeGame = game;
            glfwSetFramebufferSizeCallback(window, (handle, width, height) -> {
                glViewport(0, 0, width, height);
                activeGame.resize(width, height);
            });
            glfwSetKeyCallback(window, (handle, key, scancode, action, mods) -> {
                if (action == GLFW_PRESS || action == GLFW_REPEAT) {
                    activeGame.keyPressed(key, action == GLFW_REPEAT);
                }
            });
            glfwSetCursorPosCallback(window, (handle, x, y) -> {
                // O cursor vem em coordenadas da janela; as telas desenham em pixels do framebuffer
                int[] windowWidth = new int[1];
                int[] windowHeight = new int[1];
                int[] bufferWidth = new int[1];
                int[] bufferHeight = new int[1];
                glfwGetWindowSize(handle, windowWidth, windowHeight);
                glfwGetFramebufferSize(handle, bufferWidth, bufferHeight);
                double scaleX = windowWidth[0] > 0 ? (double) bufferWidth[0] / windowWidth[0] : 1.0;
                double scaleY = windowHeight[0] > 0 ? (double) bufferHeight[0] / windowHeight[0] : 1.0;
                activeGame.mouseMoved(x * scaleX, y * scaleY);
            });
            glfwSetScrollCallback(window, (handle, xOffset, yOffset) -> activeGame.scrolled(yOffset));
            glfwSetWindowFocusCallback(window, (handle, focused) -> {
                if (!focused) {
                    activeGame.focusLost();
                }
            });
            glfwSetMouseButtonCallback(window, (handle, button, action, mods) -> {
                if (action != GLFW_PRESS) {
                    return;
                }
                if (button == GLFW_MOUSE_BUTTON_1) {
                    activeGame.mouseClicked();
                } else if (button == GLFW_MOUSE_BUTTON_2) {
                    activeGame.mouseRightClicked();
                }
            });

            int[] framebufferWidth = new int[1];
            int[] framebufferHeight = new int[1];
            glfwGetFramebufferSize(window, framebufferWidth, framebufferHeight);
            glViewport(0, 0, framebufferWidth[0], framebufferHeight[0]);
            game.resize(framebufferWidth[0], framebufferHeight[0]);

            double previousTime = glfwGetTime();
            while (!glfwWindowShouldClose(window)) {
                double currentTime = glfwGetTime();
                float delta = (float) Math.min(currentTime - previousTime, 0.1);
                previousTime = currentTime;

                game.update(delta);
                game.render((float) currentTime);
                glfwSwapBuffers(window);
                glfwPollEvents();
            }
        } finally {
            if (game != null) {
                game.close();
            }
            if (window != 0L) {
                glfwDestroyWindow(window);
            }
            glfwTerminate();
            GLFWErrorCallback callback = glfwSetErrorCallback(null);
            if (callback != null) {
                callback.free();
            }
        }
    }
}
