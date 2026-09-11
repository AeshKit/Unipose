package aesh.kai;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Unipose implements ModInitializer {
	public static final String MOD_ID = "unipose";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	// 8 for parity, even though 0x10FFFF is the biggest value
	public static final int MAX_UNICODE_HEX_DIGITS = 8;

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
				return handleKeyEvent(keyEvent);
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

			ScreenMouseEvents.beforeMouseClick(screen).register((_, mouseButtonEvent) -> {
				if(!isComposing || target == null) return;

				if(
						mouseButtonEvent.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT ||
						mouseButtonEvent.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT ||
						mouseButtonEvent.button() == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
					reset(); // keep literal string
				}
			});
		}));

		LOGGER.info("Unipose [ Unicode Composition ] init");
	}

	private static boolean handleKeyEvent(KeyEvent keyEvent) {
		if(keyEvent.isEscape() || (keyEvent.hasControlDown() && keyEvent.key() == GLFW.GLFW_KEY_BACKSPACE)) {
			stopComposing();
			return false;
		}

		// confirmation = enter
		if(keyEvent.isConfirmation() || keyEvent.key() == GLFW.GLFW_KEY_SPACE) {
			if(target != null) commitHexValue();
			return false;
		}

		if(keyEvent.key() == GLFW.GLFW_KEY_BACKSPACE) {
			if(target != null)
				deleteChar();
			return false;
		}

		if(keyEvent.isRight()) {
			reset();
			return false;
		}

		if(keyEvent.isCut()) {
			if(target != null) {
				String hex = target.getValue().substring(startPos + 1, target.getCursorPosition());
				Minecraft.getInstance().keyboardHandler.setClipboard(parse(hex));
			}
			stopComposing();
			return false;
		}

		if(keyEvent.isCopy()) {
			if(target != null) {
				String hex = target.getValue().substring(startPos + 1, target.getCursorPosition());
				Minecraft.getInstance().keyboardHandler.setClipboard(parse(hex));
			}
			return false;
		}

		if(keyEvent.isPaste()) {
			if(target != null) commitHexValue();
			return true;
		}

		if(keyEvent.isSelectAll()) {
			reset();
			return true;
		}

		return false;
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

	private static void commitHexValue() {
		if(target == null) {
			reset();
			return;
		}

		final String out = parse(target.getValue().substring(startPos + 1, target.getCursorPosition()));
		target.deleteCharsToPos(startPos);
		target.insertText(out);
		reset();
	}

	private static String parse(String hex) {
		try {
			final int codepoint = Integer.parseInt(hex, 16);
			final boolean isLoneSurrogate = codepoint >= 0xD800 && codepoint <= 0xDFFF;

			if(Character.isValidCodePoint(codepoint) && !isLoneSurrogate)
				return new String(Character.toChars(codepoint));
		} catch(NumberFormatException _) {}
		return "";
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