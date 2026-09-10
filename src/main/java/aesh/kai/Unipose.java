package aesh.kai;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;
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
				else if(keyEvent.isRight()) {
					reset(); // keep literal string
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

				if(isHexDigit && hexDigits < MAX_UNICODE_HEX_DIGITS)
					target.insertText(Character.toString(Character.toLowerCase(c)));

				return false;
			});

			ScreenMouseEvents.allowMouseClick(screen).register((_, mouseButtonEvent) -> {
				if(!isComposing || target == null) return true;

				if(
						mouseButtonEvent.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT ||
						mouseButtonEvent.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT ||
						mouseButtonEvent.button() == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
					reset(); // keep literal string
					return false;
				}
				return true;
			});
		}));

		LOGGER.info("Unipose [ Unicode Composition ] init");
	}

	private static boolean startComposing(Screen screen) {
		if(!(screen.getFocused() instanceof EditBox editBox)) return false;

		target = editBox;
		isComposing = true;
		editBox.insertText("u");
		startPos = editBox.getCursorPosition() - 1;

		editBox.addFormatter((s, firstChar) -> {
			if(!isComposing) return FormattedCharSequence.forward(s, Style.EMPTY);

			final int start = Math.clamp(startPos - firstChar, 0, s.length());
			final int end = Math.clamp(target.getCursorPosition() - firstChar, 0, s.length());

			final String textBefore = s.substring(0, start);
			final String hexCode = s.substring(start, end);
			final String textAfter = s.substring(end);

			return Component.literal(textBefore)
					.append(Component.literal(hexCode).withStyle(ChatFormatting.UNDERLINE))
					.append(Component.literal(textAfter))
					.getVisualOrderText();
		});

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
		target = null;
		startPos = -1;
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}