package fr.lumaa.cidercraft.hud;

import fr.lumaa.cidercraft.CiderCraft;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.gui.DrawContext;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

public class OnlineImageRenderer {
    @Nullable public NativeImage image = null;

    public void fetchImage(String imageUrl) {
        new Thread(() -> {
            if (this.image != null) return;
            try {
                URL url = new URI(imageUrl).toURL();
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("User-Agent", "Minecraft");
                InputStream in = conn.getInputStream();

                BufferedImage buffered = ImageIO.read(in); // handles JPEG
                if (buffered == null) {
                    CiderCraft.LOGGER.error("Could not read image (null BufferedImage)");
                    return;
                }

                int width = buffered.getWidth();
                int height = buffered.getHeight();
                NativeImage nativeImage = new NativeImage(NativeImage.Format.RGBA, width, height, false);

                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        int argb = buffered.getRGB(x, y);
                        nativeImage.setColor(x, y, argb | 0xFF000000); // force alpha to 255
                    }
                }

                this.image = nativeImage;
            } catch (Exception e) {
                CiderCraft.LOGGER.error(e.getLocalizedMessage());
            }
        }).start();
    }

    public void render(DrawContext context, int x, int y, int width, int height) {
        if (this.image == null) return;

        int[] colors = this.image.copyPixelsArgb();
        for (int i = 0; i < colors.length; i++) {
            int color = colors[i];
            int line = (int) (double) (i / this.image.getWidth());

            context.fill(x + i * width, y + line * height, width, height, color);
        }
    }
}