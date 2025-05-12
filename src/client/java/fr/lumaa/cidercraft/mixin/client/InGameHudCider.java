package fr.lumaa.cidercraft.mixin.client;

import fr.lumaa.cidercraft.CiderCraftClient;
import fr.lumaa.cidercraft.websocket.CiderData;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudCider {
	@Shadow @Nullable private Text title;
	@Unique
	private final int x = 10;
	@Unique
	private final int y = 10;

	@Inject(at = @At("HEAD"), method = "render")
	private void renderCider(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
		if (CiderCraftClient.websocket.currentTrack != null) {
			this.renderBg(context);
			this.renderText(context);
			this.renderProgress(context);
		}
	}

	private void renderBg(DrawContext context) {
		CiderData.Track currentTrack = CiderCraftClient.websocket.currentTrack;

		context.fill(this.x, this.y, this.getWidth(currentTrack), this.getHeight(), 0x4D000000);
	}

	private void renderText(DrawContext context) {
		CiderData.Track currentTrack = CiderCraftClient.websocket.currentTrack;
		TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
		int spacing = textRenderer.fontHeight;

		context.drawText(textRenderer, Text.literal(currentTrack.title).formatted(Formatting.BOLD), this.x + 5, this.y + 5, 0xFFFFFF, false);
		context.drawText(textRenderer, Text.literal(currentTrack.artist), this.x + 5, this.y + 5 + spacing, 0xFFFFFF, false);
	}

	private void renderProgress(DrawContext context) {
		CiderData.Track currentTrack = CiderCraftClient.websocket.currentTrack;

		if (currentTrack.timePourcentage > 0.0f) {
			int progressX = (int) (currentTrack.timePourcentage * (this.getWidth(currentTrack) - this.x)) + this.x;
			context.fill(this.x, this.getHeight(), progressX, this.getHeight() + 2, 0xFFFFFFFF);
		}
	}

	private int getWidth(CiderData.Track currentTrack) {
		TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;

		int titleW = (int) (textRenderer.getWidth(Text.literal(currentTrack.title).formatted(Formatting.BOLD)) * 1.25);
		int artistW = textRenderer.getWidth(Text.literal(currentTrack.artist));

		return Math.max(titleW, artistW) + 5;
	}

	private int getHeight() {
		TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
		int spacing = textRenderer.fontHeight;

		return this.y + spacing*2 + 8;
	}
}