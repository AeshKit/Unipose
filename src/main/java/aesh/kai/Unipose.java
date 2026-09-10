package aesh.kai;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.resources.Identifier;

import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Unipose implements ModInitializer {
	public static final String MOD_ID = "unipose";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	// 0x10FFFF as highest valid Unicode hex
	public static final int MAX_UNICODE_HEX_DIGITS = 6;

	private static boolean isComposing = false;
	private static EditBox target = null;
	private static int startPos = -1;

	@Override
	public void onInitialize() {

		ScreenEvents.BEFORE_INIT.register(((_, screen, _, _) -> {
			reset();

			// Checks if a key should be allowed to be pressed.
			// I.E. whether we want vanilla logic to dictate the key press
			ScreenKeyboardEvents.allowKeyPress(screen).register((thisScreen, keyEvent) -> {
				if(!isComposing) return !isTrigger(keyEvent) || !startComposing(thisScreen);

				if(keyEvent.isEscape() || (keyEvent.hasControlDown() && keyEvent.input() == GLFW.GLFW_KEY_BACKSPACE)) {
					stopComposing();
				}
				else if(keyEvent.isConfirmation()) { // enter
					// parse
				}
				else if(keyEvent.input() == GLFW.GLFW_KEY_BACKSPACE) {
					deleteChar();
				}
				else if(keyEvent.isCopy()) {
					// copy hex value of code so far
				}

				return false;
			});

			ScreenKeyboardEvents.allowCharType(screen).register((_, characterEvent) -> {
				if(!isComposing || target == null) return true;

				final int c = characterEvent.codepoint();
				final boolean isHexDigit =
						(c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');

				final int hexDigits = target.getCursorPosition() - startPos - 1;
				return isHexDigit && hexDigits < MAX_UNICODE_HEX_DIGITS;
			});
		}));

		LOGGER.info("Unipose [ Unicode Composition ] init");
	}

	private static boolean startComposing(Screen screen) {
		if(!(screen.getFocused() instanceof EditBox editBox)) return false;

		target = editBox;
		startPos = editBox.getCursorPosition();
		isComposing = true;

		editBox.insertText("u");
		editBox.setCursorPosition(startPos + 1);
		return true;
	}

	private static void stopComposing() {
		if(target != null) target.deleteCharsToPos(startPos);
		reset();
	}

	private static void deleteChar() {
		if(target.getCursorPosition() == startPos + 1) {
			stopComposing();
			return;
		}

		target.deleteCharsToPos(target.getCursorPosition() - 1);
	}

	private static boolean isTrigger(KeyEvent keyEvent) {
		return keyEvent.key() == GLFW.GLFW_KEY_U
				&& keyEvent.hasControlDown()
				&& keyEvent.hasShiftDown();
	}

	private static void reset() {
		isComposing = false;
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}