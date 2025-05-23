package computer.heather.advancedbackups.client;

import computer.heather.advancedbackups.core.config.ClientConfigManager;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class BackupToast implements Toast {
        
    public static boolean starting;
    public static boolean started;
    public static boolean failed;
    public static boolean finished;
    public static boolean cancelled;

    public static int progress;
    public static int max;

    public static boolean exists = false;

    private static long time;
    private static boolean timeSet = false;

    public static final ItemStack stack = new ItemStack(Items.PAPER);
    private static final ResourceLocation TEXTURE = ResourceLocation.parse("toast/advancement");
    
    private int textColour;
    private String title = "You shouldn't see this!";

    private Visibility visibility = Visibility.SHOW;


    @Override
    public void render(GuiGraphics graphics, Font font, long delta) {
        graphics.blitSprite(RenderType::guiTextured, TEXTURE, 0, ClientConfigManager.darkMode.get() ? 0 : this.height(), this.width(), this.height());
        graphics.renderFakeItem(stack, 8, 8);

        
        float percent = finished ? 100 : (float) progress / (float) max;
        
        graphics.fill(4, 28, 156, 29, ColourHelper.colour
            (255, (int) ClientConfigManager.progressBackgroundRed.get(), (int) ClientConfigManager.progressBackgroundGreen.get(), (int) ClientConfigManager.progressBackgroundBlue.get()));

        float f = Math.min(156, (
            156 * percent
        ));

        if (!exists) { 
            if (title.equals(I18n.get("advancedbackups.backup_finished"))){
                textColour = ColourHelper.colour(255, (int) ClientConfigManager.progressTextRed.get(), (int) ClientConfigManager.progressTextGreen.get(), (int) ClientConfigManager.progressTextBlue.get());
                graphics.drawString(font, I18n.get(title), 25, 11, textColour);
                graphics.fill(3, 28, 156, 29, ColourHelper.colour
                    (255, (int) ClientConfigManager.progressBarRed.get(), (int) ClientConfigManager.progressBarGreen.get(), (int) ClientConfigManager.progressBarBlue.get()));
            }
            else {
                textColour = ColourHelper.colour(255, (int) ClientConfigManager.errorTextRed.get(), (int) ClientConfigManager.errorTextGreen.get(), (int) ClientConfigManager.errorTextBlue.get());
                graphics.drawString(font, I18n.get(title), 25, 11, textColour);
            }
            visibility = Visibility.HIDE;
            return;
        }

        
        if (starting) {
            textColour = ColourHelper.colour(255, (int) ClientConfigManager.progressTextRed.get(), (int) ClientConfigManager.progressTextGreen.get(), (int) ClientConfigManager.progressTextBlue.get());
            title = I18n.get("advancedbackups.backup_starting");
        }
        else if (started) {
            textColour = ColourHelper.colour(255, (int) ClientConfigManager.progressTextRed.get(), (int) ClientConfigManager.progressTextGreen.get(), (int) ClientConfigManager.progressTextBlue.get());
            title = I18n.get("advancedbackups.progress", round(percent * 100));
        }
        else if (failed) {
            textColour = ColourHelper.colour(255, (int) ClientConfigManager.errorTextRed.get(), (int) ClientConfigManager.errorTextGreen.get(), (int) ClientConfigManager.errorTextBlue.get());
            title = I18n.get("advancedbackups.backup_failed");
            if (!timeSet) {
                time = System.currentTimeMillis();
                timeSet = true;
            }
        }
        else if (finished) {
            textColour = ColourHelper.colour(255, (int) ClientConfigManager.progressTextRed.get(), (int) ClientConfigManager.progressTextGreen.get(), (int) ClientConfigManager.progressTextBlue.get());
            title = I18n.get("advancedbackups.backup_finished");
            if (!timeSet) {
                time = System.currentTimeMillis();
                timeSet = true;
            }
        }
        else if (cancelled) {
            textColour = ColourHelper.colour(255, (int) ClientConfigManager.errorTextRed.get(), (int) ClientConfigManager.errorTextGreen.get(), (int) ClientConfigManager.errorTextBlue.get());
            title = I18n.get("advancedbackups.backup_cancelled");
            if (!timeSet) {
                time = System.currentTimeMillis();
                timeSet = true;
            }
        }

        graphics.drawString(font, title, 25, 11, textColour);

        if (timeSet && System.currentTimeMillis() >= time + 5000) {
            starting = false;
            started = false;
            failed = false;
            finished = false;
            progress = 0;
            max = 0;
            timeSet = false;
            exists = false;
            visibility = Visibility.HIDE;
            return;
        }
        
        graphics.fill(4, 28, Math.max(4, (int) f), 29, ColourHelper.colour
        (255, (int) ClientConfigManager.progressBarRed.get(), (int) ClientConfigManager.progressBarGreen.get(), (int) ClientConfigManager.progressBarBlue.get()));
        
        visibility = Visibility.SHOW;
        return;
        
    }

    
    private static String round (float value) {
        return String.format("%.1f", value);
    }


    @Override
    public Visibility getWantedVisibility() {
        return visibility;
    }


    @Override
    public void update(ToastManager p_361832_, long p_362759_) {
        // unused...
    }

}
