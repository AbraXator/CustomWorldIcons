package net.iamaprogrammer.customworldicons.gui.widgets;

import com.mojang.blaze3d.systems.RenderSystem;
import net.iamaprogrammer.customworldicons.gui.screen.WorldIconScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.MultilineText;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Language;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;


public class WorldIconListWidget extends AlwaysSelectedEntryListWidget<net.iamaprogrammer.customworldicons.gui.widgets.WorldIconListWidget.WorldIconEntry> {
    static final Identifier RESOURCE_PACKS_TEXTURE = new Identifier("textures/gui/resource_packs.png");
    private final Text title;
    private final WorldIconScreen screen;

    public WorldIconListWidget(MinecraftClient client, WorldIconScreen screen, int width, int height, Text title) {
        super(client, width, height, 32, height - 55 + 4, 36);
        this.screen = screen;
        this.title = title;
        this.centerListVertically = false;
        Objects.requireNonNull(client.textRenderer);
        this.setRenderHeader(true, (int)(9.0f * 1.5f));
    }

    @Override
    protected void renderHeader(MatrixStack matrices, int x, int y) {
        MutableText text = Text.empty().append(this.title).formatted(Formatting.UNDERLINE, Formatting.BOLD);
        this.client.textRenderer.draw(matrices, text, (float)(x + this.width / 2 - this.client.textRenderer.getWidth(text) / 2), (float)Math.min(this.top + 3, y), 0xFFFFFF);
    }

    @Override
    public int getRowWidth() {
        return this.width;
    }

    @Override
    protected int getScrollbarPositionX() {
        return this.right - 6;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.getSelectedOrNull() != null) {
            switch (keyCode) {
                case 32, 257 -> {
                    return true;
                }
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }


    public static class WorldIconEntry extends Entry<net.iamaprogrammer.customworldicons.gui.widgets.WorldIconListWidget.WorldIconEntry> {
        private final Path worldIconsFolder;
        private static final String ELLIPSIS = "...";
        private final net.iamaprogrammer.customworldicons.gui.widgets.WorldIconListWidget widget;
        protected final MinecraftClient client;
        private final OrderedText displayName;
        private final MultilineText description;
        private final File file;
        private final String fullPath;
        private final double fileSize;
        private final boolean isToLarge;


        public WorldIconEntry(MinecraftClient client, net.iamaprogrammer.customworldicons.gui.widgets.WorldIconListWidget widget, File path) throws IOException {
            this.client = client;
            this.widget = widget;
            this.file = path;
            this.worldIconsFolder = Path.of(new File(client.runDirectory, "worldicons\\").toURI());
            this.fullPath = worldIconsFolder + "\\" + this.file.getName();
            this.fileSize = (float)(Files.size(Path.of(fullPath)))/1000;
            this.description = createMultilineText(client, Text.of(String.format("%.3f", this.fileSize) + "KB"));
            this.isToLarge = this.fileSize > 50;
            this.displayName = net.iamaprogrammer.customworldicons.gui.widgets.WorldIconListWidget.WorldIconEntry.trimTextToWidth(client, Text.literal(file.getName()));
        }

        private static OrderedText trimTextToWidth(MinecraftClient client, Text text) {
            int i = client.textRenderer.getWidth(text);
            if (i > 157) {
                StringVisitable stringVisitable = StringVisitable.concat(client.textRenderer.trimToWidth(text, 157 - client.textRenderer.getWidth(ELLIPSIS)), StringVisitable.plain(ELLIPSIS));
                return Language.getInstance().reorder(stringVisitable);
            }
            return text.asOrderedText();
        }

        private static MultilineText createMultilineText(MinecraftClient client, Text text) {
            return MultilineText.create(client.textRenderer, (StringVisitable)text, 157, 2);
        }

        @Override
        public Text getNarration() {
            return Text.translatable("narrator.select", this.displayName);
        }

        @Override
        public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            InputStream inputStream = null;
            NativeImageBackedTexture texture = null;
            if (!this.isToLarge) {
                try {
                    File f = new File(fullPath);
                    inputStream = new ByteArrayInputStream(Files.readAllBytes(f.toPath()));
                    inputStream.close();
                    texture = new NativeImageBackedTexture(NativeImage.read(inputStream));
                    Identifier iconTexture = client.getTextureManager().registerDynamicTexture("worldicontexture", texture);

                    RenderSystem.setShaderTexture(0, iconTexture);
                } catch (Exception e) {
                    RenderSystem.setShaderTexture(0, new Identifier("minecraft", "textures/misc/unknown_pack.png"));
                }
            } else {
                DrawableHelper.fill(matrices, x - 1, y - 1, x + entryWidth - 9, y + entryHeight + 1, -8978432);
                RenderSystem.setShaderTexture(0, new Identifier("minecraft", "textures/misc/unknown_pack.png"));
            }
            DrawableHelper.drawTexture(matrices, x, y, 0.0f, 0.0f, 32, 32, 32, 32);

            // DO NOT REMOVE THIS BLOCK, it stops the icons from eating up your memory.
            if (texture != null) {
                texture.close();
            }
            OrderedText orderedText = this.displayName;
            MultilineText multilineText = this.description;
            if (this.isToLarge) {
                multilineText = createMultilineText(client, Text.translatable("world.create.icon.filetoolarge", String.format("%.3f", this.fileSize)));
            }
            if (this.isSelectable() && (this.client.options.getTouchscreen().getValue().booleanValue() || hovered || this.widget.getSelectedOrNull() == this && this.widget.isFocused())) {
                RenderSystem.setShaderTexture(0, RESOURCE_PACKS_TEXTURE);
                DrawableHelper.fill(matrices, x, y, x + 32, y + 32, -1601138544);
            }
            this.client.textRenderer.drawWithShadow(matrices, orderedText, (float)(x + 32 + 2), (float)(y + 1), 0xFFFFFF);
            multilineText.drawWithShadow(matrices, x + 32 + 2, y + 12, 10, 0x808080);
        }

        public String getName() {
            return this.file.getName();
        }

        private boolean isSelectable() {
            return !isToLarge;
        }


        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button != 0) {
                return false;
            }
            if (this.isSelectable()) {
                this.widget.screen.clearSelection();
                WorldIconScreen.SELECTED_ICON = this.fullPath;
                return true;
            }
            return false;
        }
    }
}
