package com.astatin3.nomorerender.mixin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.RunArgs;
import net.minecraft.client.gl.WindowFramebuffer;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.world.WorldListWidget;
import net.minecraft.client.gui.widget.*;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

@Mixin(MinecraftClient.class)
public class HeadlessMinecraftClient {

//    @Inject(method = "<init>", at = @At("RETURN"))
//    private void onInit(CallbackInfo ci) {
//        startCommandThread();
//        System.out.println("#########################################");
//    }

    @Shadow private Thread thread;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onGameLoaded(RunArgs args, CallbackInfo ci) {
//        AddonTemplate.LOG.info("Hello from ExampleMixin!");
        System.out.println("#########################################");
        startCommandThread();
    }


    private void startCommandThread() {
        Thread commandThread = new Thread(() -> {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            String line;
            try {
                while ((line = reader.readLine()) != null) {
                    parseCommand(line);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        commandThread.setName("Headless Command Thread");
        commandThread.setDaemon(true);
        commandThread.start();
    }

    MinecraftClient self = (MinecraftClient)(Object)this;


    private void parseCommand(String command) {
        String[] split = command.split(" ");
        try {
            switch (split[0].toLowerCase()) {
                case "quit":
                case "exit":
                    self.stop();
                    break;
                case "tick":
                    self.tick();
                    break;
                case "listelements":
                case "elems":
                    listElements();
                    break;
                case "clickelement":
                case "celem":
                    clickElement(Integer.parseInt(split[1]));
                    break;
                case "writeelement":
                case "welem":
                    setElemText(Integer.parseInt(split[1]), command.substring(split[0].length()+split[1].length()-2));
                    break;
                case "key":
                    if (split.length > 1) {
                        simulateKeyPress(split[1]);
                    }
                    break;
                case "keydown":
                    if (split.length > 1) {
                        simulateKeyDown(split[1]);
                    }
                    break;
                case "keyup":
                    if (split.length > 1) {
                        simulateKeyUp(split[1]);
                    }
                    break;
                default:
                    System.out.println("Unknown command: " + command);
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private void listElements() {
        Screen currentScreen = self.currentScreen;
        if (currentScreen == null) {
            System.out.println("No screen is currently open.");
            return;
        }

        AtomicInteger index = new AtomicInteger(0);
        printElements(currentScreen.children(), 0, index);
    }


    private void printElements(List<? extends Element> elements, int depth, AtomicInteger index) {
        String indent = "  ".repeat(depth);
        for (Element element : elements) {
            System.out.printf("%s%d: %s%n", indent, index.getAndIncrement(), describeElement(element));
            if (element instanceof ClickableWidget) {
                ClickableWidget widget = (ClickableWidget) element;
                System.out.printf("%s   Message: %s%n", indent, widget.getMessage().getString());
            }
            if (element instanceof net.minecraft.client.gui.ParentElement pe) {
                printElements(pe.children(), depth + 1, index);
            } else if (element instanceof WorldListWidget wlw) {
                printElements(wlw.children(), depth+1, index);
            }// else if (element instanceof WorldListWidget.WorldEntry elw) {
//                printElements(elw.get, depth+1, index);
//            }
        }
    }

    private String describeElement(Element element) {
        return String.format("%s@%s", element.getClass().getSimpleName(), Integer.toHexString(element.hashCode()));
    }

    private Element findElement(int targetIndex) {
        Screen currentScreen = self.currentScreen;
        if (currentScreen == null) {
            System.out.println("No screen is currently open.");
            return null;
        }

        AtomicInteger index = new AtomicInteger(0);
        return findElementByIndex(currentScreen.children(), targetIndex, index);
    }

    private void clickElement(int targetIndex) {
        Element targetElement = findElement(targetIndex);

        if (targetElement == null) {
            System.out.println("Invalid element index.");
            return;
        }

        if (targetElement instanceof ClickableWidget widget) {
            self.execute(() -> {
                widget.onClick(widget.getX(), widget.getY());
            });
            System.out.println("Clicked element: " + describeElement(widget));
            if (widget.getMessage() != null) {
                System.out.println("Message: " + widget.getMessage().getString());
            }
        } else {
            System.out.println("Element is not clickable: " + describeElement(targetElement));
        }
    }

    private void setElemText(int targetIndex, String text) {
        Element targetElement = findElement(targetIndex);

        if (targetElement == null) {
            System.out.println("Invalid element index.");
            return;
        }



        if (targetElement instanceof TextFieldWidget widget) {
            self.execute(() -> {
                widget.write(text);
            });
            System.out.println("Wrote in element: " + describeElement(widget));
        } else {
            System.out.println("Element is not a TextFieldWidget: " + describeElement(targetElement));
        }
    }

    private Element findElementByIndex(List<? extends Element> elements, int targetIndex, AtomicInteger currentIndex) {
        for (Element element : elements) {
            if (currentIndex.getAndIncrement() == targetIndex) {
                return element;
            }
            if (element instanceof net.minecraft.client.gui.ParentElement) {
                Element found = findElementByIndex(((net.minecraft.client.gui.ParentElement) element).children(), targetIndex, currentIndex);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }



    private void simulateKeyPress(String keyName) {
        int keyCode = getKeyCode(keyName);
        if (keyCode != GLFW.GLFW_KEY_UNKNOWN) {
            long handle = self.getWindow().getHandle();
            self.execute(() -> {
                self.keyboard.onKey(handle, keyCode, 0, GLFW.GLFW_PRESS, 0);
                self.keyboard.onKey(handle, keyCode, 0, GLFW.GLFW_RELEASE, 0);
            });
            System.out.println("Pressed key: " + keyName);
        } else {
            System.out.println("Unknown key: " + keyName);
        }
    }

    private void simulateKeyDown(String keyName) {
        int keyCode = getKeyCode(keyName);
        if (keyCode != GLFW.GLFW_KEY_UNKNOWN) {
            long handle = self.getWindow().getHandle();
            self.execute(() -> {
                self.keyboard.onKey(handle, keyCode, 0, GLFW.GLFW_PRESS, 0);
            });
            System.out.println("Key down: " + keyName);
        } else {
            System.out.println("Unknown key: " + keyName);
        }
    }

    private void simulateKeyUp(String keyName) {
        int keyCode = getKeyCode(keyName);
        if (keyCode != GLFW.GLFW_KEY_UNKNOWN) {
            long handle = self.getWindow().getHandle();
            self.execute(() -> {
                self.keyboard.onKey(handle, keyCode, 0, GLFW.GLFW_RELEASE, 0);
            });
            System.out.println("Key up: " + keyName);
        } else {
            System.out.println("Unknown key: " + keyName);
        }
    }

    private static int getKeyCode(String keyName) {
        // Handle special cases
        switch (keyName.toLowerCase()) {
            case "space": return GLFW.GLFW_KEY_SPACE;
            case "enter": return GLFW.GLFW_KEY_ENTER;
            case "tab": return GLFW.GLFW_KEY_TAB;
            case "escape": return GLFW.GLFW_KEY_ESCAPE;
            case "backspace": return GLFW.GLFW_KEY_BACKSPACE;
//            case "rightclick": return GLFW.CLI;
//            case "leftclick": return GLFW.GLFW_KEY_BACKSPACE;
            // Add more special cases as needed
        }

        // For single characters, use their ASCII value
        if (keyName.length() == 1) {
            char c = Character.toUpperCase(keyName.charAt(0));
            if (c >= 'A' && c <= 'Z') {
                return GLFW.GLFW_KEY_A + (c - 'A');
            }
            if (c >= '0' && c <= '9') {
                return GLFW.GLFW_KEY_0 + (c - '0');
            }
        }

        // If not found, return unknown key
        return GLFW.GLFW_KEY_UNKNOWN;
    }

}
