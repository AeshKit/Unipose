package aesh.kai.utils;

import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;

public class StringUtils {
    public static String delete(String s, int from, int to) {
        return s.substring(0, from) + s.substring(to);
    }

    public static String insert(String s, int pos, String text) {
        return s.substring(0, pos) + text + s.substring(pos);
    }

    public static boolean isTrigger(KeyEvent keyEvent) {
        return keyEvent.key() == GLFW.GLFW_KEY_U
                && keyEvent.hasControlDown()
                && keyEvent.hasShiftDown();
    }
}