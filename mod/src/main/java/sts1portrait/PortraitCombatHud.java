package sts1portrait;

import basemod.ReflectionHacks;
import basemod.ReflectionHacks.RMethod;
import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.FileTextureData;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.megacrit.cardcrawl.blights.AbstractBlight;
import com.esotericsoftware.spine.Skeleton;
import com.esotericsoftware.spine.SkeletonMeshRenderer;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.daily.mods.AbstractDailyMod;
import javassist.CannotCompileException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;
import com.evacipated.cardcrawl.modthespire.lib.SpireInstrumentPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireField;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.MathUtils;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.ModHelper;
import com.megacrit.cardcrawl.helpers.TipHelper;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.monsters.ending.CorruptHeart;
import com.megacrit.cardcrawl.monsters.exordium.Hexaghost;
import com.megacrit.cardcrawl.monsters.exordium.HexaghostBody;
import com.megacrit.cardcrawl.monsters.exordium.HexaghostOrb;
import com.megacrit.cardcrawl.vfx.GhostlyFireEffect;
import com.megacrit.cardcrawl.vfx.GhostlyWeakFireEffect;
import javassist.expr.NewExpr;
import com.megacrit.cardcrawl.ui.panels.TopPanel;
import com.megacrit.cardcrawl.ui.panels.DrawPilePanel;
import com.megacrit.cardcrawl.ui.panels.DiscardPilePanel;
import com.megacrit.cardcrawl.ui.panels.PotionPopUp;
import com.megacrit.cardcrawl.ui.buttons.EndTurnButton;
import com.megacrit.cardcrawl.ui.panels.AbstractPanel;
import com.megacrit.cardcrawl.ui.panels.EnergyPanel;
import com.megacrit.cardcrawl.ui.panels.ExhaustPanel;
import com.megacrit.cardcrawl.potions.AbstractPotion;
import com.megacrit.cardcrawl.potions.PotionSlot;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.screens.stats.CharStat;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.megacrit.cardcrawl.vfx.ObtainKeyEffect;
import com.megacrit.cardcrawl.vfx.combat.HealPanelEffect;
import com.megacrit.cardcrawl.vfx.combat.PingHpEffect;
import com.megacrit.cardcrawl.vfx.ExhaustPileParticle;
import java.util.ArrayList;
import java.util.WeakHashMap;

public final class PortraitCombatHud {
    private static final float ENEMY_SCALE = 2f;
    private static final float TOP_ICON_SCALE = 2.12f;
    private static final float HEALTH_ICON_SCALE = TOP_ICON_SCALE * .93f;
    private static final float HEALTH_PULSE_PERIOD = 1.25f;
    private static final float GOLD_ICON_SCALE = TOP_ICON_SCALE * .88f;
    private static final float HUD_NUMBER_SCALE = 1.35f;
    private static final float HP_X = 0.068f;
    private static final float GOLD_X = 0.228f;
    private static final float FLOOR_X = 0.359f;
    private static final float FLOOR_ICON_SIZE = 0.075f;
    private static final float ASCENSION_X = 0.590f;
    private static final float MAP_X = 0.702f;
    private static final float DECK_X = 0.818f;
    private static final float SETTINGS_X = 0.94f;
    private static final float INTENT_SCALE = 1.46f;
    private static final float BOTTOM_ICON_SCALE = 1.5f;
    private static final float ENERGY_SCALE = 1.88f;
    private static final float EXHAUST_SCALE = 2f;
    private static final float END_TURN_SCALE = 2f;
    private static final float PILE_SCALE = 1.29f * BOTTOM_ICON_SCALE;
    private static final int DRAW_PILE_SLOT = 1;
    private static final int DISCARD_PILE_SLOT = 3;

    private static final float RELIC_SCALE = 1.82f;
    private static final int RELICS_PER_PAGE = 4;
    private static final Matrix4 original = new Matrix4();
    private static final Matrix4 moved = new Matrix4();
    private static final Matrix4 poseOriginal = new Matrix4();
    private static final Matrix4 poseScaled = new Matrix4();
    private static final Matrix4 healthEffectOriginal = new Matrix4();
    private static final Matrix4 healthEffectMoved = new Matrix4();
    private static final Matrix4 keyEffectOriginal = new Matrix4();
    private static final Matrix4 keyEffectMoved = new Matrix4();
    private static final RMethod hp = method("renderHP");
    private static final RMethod gold = method("renderGold");
    private static final RMethod map = method("renderMapIcon");
    private static final RMethod deck = method("renderDeckIcon");
    private static final RMethod options = method("renderSettingsIcon");
    private static final RMethod potions = method("renderPotions");
    private static Hitbox previousRelics;
    private static Hitbox nextRelics;
    private static int relicPage;
    private static AbstractRelic selectedRelic;
    private static AbstractDungeon.CurrentScreen relicScreen;
    private static TopPanel previousPanel;
    private static int lastClockSecond = -1;
    private static String clockLabel;
    private static AbstractPotion measuredFirstPotion;
    private static float firstPotionLeft;
    private static float heartLeft = Float.NaN;
    private static float healthPulseTime;

    private PortraitCombatHud() {}

    private static RMethod method(String name) {
        return ReflectionHacks.privateMethod(TopPanel.class, name, SpriteBatch.class);
    }

    static boolean active() {
        return topBarActive() && PortraitBattle.active();
    }

    static boolean topBarActive() {
        return !Settings.isControllerMode && PortraitViewport.active()
                && AbstractDungeon.player != null && AbstractDungeon.currMapNode != null;
    }

    static float toolsY() {
        float width = PortraitViewport.referenceWidth();
        return Settings.HEIGHT - width * 0.068f;
    }

    private static float toolbarX(float fraction) {
        // The HUD uses the full drawable width. Content and popups retain the safe viewport.
        return Settings.WIDTH * fraction;
    }

    enum Tool { HEALTH, GOLD, FLOOR, ASCENSION, MAP, DECK, SETTINGS }

    private static float baseToolX(Tool tool) {
        switch (tool) {
            case HEALTH: return toolbarX(HP_X);
            case GOLD: return toolbarX(GOLD_X);
            case FLOOR: return toolbarX(FLOOR_X);
            case ASCENSION: return toolbarX(ASCENSION_X);
            case MAP: return toolbarX(MAP_X);
            case DECK: return toolbarX(DECK_X);
            case SETTINGS: return toolbarX(SETTINGS_X);
            default: throw new AssertionError(tool);
        }
    }

    static float toolWidth(Tool tool) {
        return (toolLeft(tool) + toolRight(tool)) * toolUnit(tool);
    }

    static float toolHeight(Tool tool) {
        return toolUnit(tool) * 0.10f;
    }

    static float toolCenterX(Tool tool) {
        return toolX(tool) + (toolRight(tool) - toolLeft(tool)) * toolUnit(tool) * 0.5f;
    }

    static float toolY(Tool tool) {
        float half = toolHeight(tool) * 0.5f;
        float ceiling = toolCeiling(tool);
        return MathUtils.clamp(toolBaseY(tool),
                PortraitViewport.y(0f) + half, ceiling - half);
    }

    private static float toolBaseY(Tool tool) {
        return Math.min(toolsY(), toolCeiling(tool) - toolHeight(tool) * .5f);
    }

    private static float toolCeiling(Tool tool) {
        float center = toolCenterX(tool);
        float halfWidth = toolWidth(tool) * .5f;
        // The launcher supplies a vertical inset. Reserve the central strip for the S25 Ultra camera.
        boolean clearOfCenter = center + halfWidth <= toolbarX(.44f)
                || center - halfWidth >= toolbarX(.56f);
        return clearOfCenter ? Settings.HEIGHT : PortraitViewport.y(1f);
    }

    static float toolOffsetX(Tool tool) {
        return (toolX(tool) - baseToolX(tool)) / PortraitViewport.contentWidth();
    }

    static float toolOffsetY(Tool tool) {
        return (toolY(tool) - toolBaseY(tool)) / PortraitViewport.contentHeight();
    }

    private static float toolX(Tool tool) {
        float unit = toolUnit(tool);
        return MathUtils.clamp(baseToolX(tool),
                toolbarX(0f) + toolLeft(tool) * unit,
                toolbarX(1f) - toolRight(tool) * unit);
    }

    private static float toolLeft(Tool tool) {
        switch (tool) {
            case FLOOR: return FLOOR_ICON_SIZE * 0.5f;
            case ASCENSION: return 0.028f;
            default: return 0.054f;
        }
    }

    private static float toolRight(Tool tool) {
        switch (tool) {
            case HEALTH: return 0.115f;
            case GOLD: return 0.104f;
            case ASCENSION: return 0.058f;
            case FLOOR:
            case DECK: return 0.073f;
            default: return 0.054f;
        }
    }

    private static float toolUnit(Tool tool) {
        float requested = PortraitViewport.referenceWidth();
        return Math.min(requested, PortraitViewport.contentWidth() / (toolLeft(tool) + toolRight(tool)));
    }

    private static float toolScale(Tool tool) {
        return toolUnit(tool) / PortraitViewport.referenceWidth();
    }

    private static float itemsY() {
        float width = PortraitViewport.referenceWidth();
        return Math.min(toolsY() - width * .075f, PortraitViewport.y(1f) - width * .045f);
    }

    private static float runInfoY() {
        return itemsY() - PortraitViewport.referenceWidth() * .061f;
    }

    static float panelBottom() {
        float depth = showRunInfo() ? .092f : .043f;
        return itemsY() - PortraitViewport.referenceWidth() * depth;
    }

    private static boolean showClock() {
        return AbstractDungeon.screen == AbstractDungeon.CurrentScreen.MAP || CardCrawlGame.stopClock;
    }

    private static boolean hasRunIcons() {
        return AbstractDungeon.player != null
                && (!ModHelper.enabledMods.isEmpty() || !AbstractDungeon.player.blights.isEmpty());
    }

    private static boolean showRunInfo() {
        return showClock() || hasRunIcons();
    }

    private static float potionScale() {
        return Math.min(1.88f, 1.88f * 4f / Math.max(1, AbstractDungeon.player.potions.size()));
    }

    static void prepareInput(TopPanel panel) {
        CombatTouchTargets.reset();
        if (!topBarActive()) return;
        if (previousPanel != panel) {
            previousPanel = panel;
            relicPage = 0;
            selectedRelic = null;
            measuredFirstPotion = null;
        }
        placeTool(panel.hpHb, Tool.HEALTH);
        placeTool(panel.goldHb, Tool.GOLD);
        placeTool(panel.mapHb, Tool.MAP);
        placeTool(panel.deckHb, Tool.DECK);
        placeTool(panel.settingsHb, Tool.SETTINGS);
        if (AbstractDungeon.isAscensionMode) {
            placeTool(panel.ascensionHb, Tool.ASCENSION);
        }
        ArrayList<AbstractPotion> items = AbstractDungeon.player.potions;
        if (!items.isEmpty()) {
            boolean compact = Sts2Preview.compact();
            float scale = compact ? Sts2Preview.potionIconScale() : potionScale();
            for (AbstractPotion potion : items) {
                float x = potionScreenX(potion);
                float width = potion.hb.width * scale;
                if (compact) width = Math.min(Sts2Preview.potionPitch(), width);
                CombatTouchTargets.place(potion.hb, x, Sts2Preview.potionY(itemsY()), width, potion.hb.height * scale);
            }
        }
        if (!active() || AbstractDungeon.overlayMenu == null) return;
        if (Sts2Preview.compact()) {
            float playerLeft = Sts2Preview.avatarLeft();
            float playerRight = PortraitCreatureHealth.playerBarRight(AbstractDungeon.player);
            float hitWidth = Math.max(Sts2Preview.avatarFrameSize(), playerRight - playerLeft);
            float hitHeight = Sts2Preview.avatarFrameSize() * 1.18f;
            CombatTouchTargets.place(AbstractDungeon.player.hb,
                    (playerLeft + playerRight) * .5f, PortraitBattle.playerBattleY(), hitWidth, hitHeight);
        }
        prepareEnemyTargets();
        placePile(AbstractDungeon.overlayMenu.combatDeckPanel, DrawPilePanel.class, DRAW_PILE_SLOT);
        placePile(AbstractDungeon.overlayMenu.discardPilePanel, DiscardPilePanel.class, DISCARD_PILE_SLOT);
        placeStatusPanel(AbstractDungeon.overlayMenu.energyPanel, EnergyPanel.class, "tipHitbox", ENERGY_SCALE);
        if (!AbstractDungeon.player.exhaustPile.isEmpty()) {
            placeStatusPanel(AbstractDungeon.overlayMenu.exhaustPanel, ExhaustPanel.class, "hb", EXHAUST_SCALE);
        }

    }

    private static float potionScreenX(AbstractPotion potion) {
        if (Sts2Preview.compact()) return Sts2Preview.potionX(potion);
        AbstractPotion first = AbstractDungeon.player.potions.get(0);
        if (first != measuredFirstPotion) {
            Texture texture = first instanceof PotionSlot ? ImageMaster.POTION_PLACEHOLDER
                    : ReflectionHacks.getPrivate(first, AbstractPotion.class, "containerImg");
            firstPotionLeft = iconLeft(texture);
            measuredFirstPotion = first;
        }
        if (Float.isNaN(heartLeft)) heartLeft = iconLeft(ImageMaster.TP_HP);
        float healthX = Math.max(baseToolX(Tool.HEALTH),
                toolbarX(0f) + toolLeft(Tool.HEALTH) * PortraitViewport.referenceWidth());
        float edge = healthX + heartLeft * Settings.scale * HEALTH_ICON_SCALE;
        return edge + (potion.hb.cX - first.hb.cX - firstPotionLeft * Settings.scale) * potionScale();
    }

    // Measure the original 64 px sprite once without consuming the texture's upload data.
    private static float iconLeft(Texture texture) {
        if (texture == null || !(texture.getTextureData() instanceof FileTextureData)) return -32f;
        FileTextureData source = (FileTextureData) texture.getTextureData();
        try {
            Pixmap pixels = new Pixmap(source.getFileHandle());
            try {
                for (int x = 0; x < Math.min(64, pixels.getWidth()); x++) {
                    for (int y = 0; y < Math.min(64, pixels.getHeight()); y++) {
                        if ((pixels.getPixel(x, y) & 0xff) >= 32) return x - 32f;
                    }
                }
            } finally {
                pixels.dispose();
            }
        } catch (RuntimeException unavailable) {
            // Some launchers and mods supply runtime-only textures.
        }
        return -32f;
    }

    private static void placeTool(Hitbox hitbox, Tool tool) {
        CombatTouchTargets.place(hitbox, toolCenterX(tool), toolY(tool), toolWidth(tool), toolHeight(tool));
    }

    private static void prepareEnemyTargets() {
        if (AbstractDungeon.getCurrRoom().monsters == null) return;
        ArrayList<AbstractMonster> monsters = SlimeSplitLayout.roster(AbstractDungeon.getCurrRoom().monsters);
        float width = PortraitViewport.referenceWidth();
        for (AbstractMonster monster : monsters) {
            if (monster.isDeadOrEscaped()) continue;
            Hitbox body = monster.hb;
            float neighborSpace = SlimeSplitLayout.isSplitChild(monster) ? .41f : monsters.size() > 2 ? .21f : .27f;
            boolean awakened = AwakenedFormation.contains(monster);
            float limit = width * (awakened ? AwakenedFormation.targetWidthFraction(monster)
                    : monsters.size() == 1 ? 0.63f : neighborSpace);
            float targetWidth = Math.max(body.width, Math.min(limit, body.width * bodyScale(monster)));
            float targetHeight = Math.max(body.height, Math.min(
                    width * (!awakened && monsters.size() > 2 ? 0.162f : 0.34f), body.height * bodyScale(monster)));
            BodyTop bounds = IntentBounds.TOP.get(monster);
            float upper = Math.max(body.y + targetHeight, visibleBodyTop(monster) + width * .012f);
            float lower = body.y;
            if (Sts2Preview.aiming() && Sts2Preview.canUseBand(monsters)) {
                lower = Math.min(lower, Sts2Preview.bandY() - Sts2Preview.bandHeight() * .5f);
                float bandWidth = Sts2Preview.bandWidth(monsters, monster);
                targetWidth = SlimeSplitLayout.isSplitChild(monster) ? Math.max(targetWidth, bandWidth) : bandWidth;
            }
            float left = body.cX - targetWidth * .5f;
            float right = body.cX + targetWidth * .5f;
            if (Float.isFinite(bounds.left) && Float.isFinite(bounds.right)) {
                left = Math.min(left, monster.drawX + bounds.left);
                right = Math.max(right, monster.drawX + bounds.right);
            }
            // Animated wings can cross a neighbor's sprite. Split the shared area at the midpoint.
            for (AbstractMonster other : monsters) {
                if (other == monster || other.isDeadOrEscaped()
                        || Math.abs(other.hb.y - body.y) > width * .12f) continue;
                float midpoint = (body.cX + other.hb.cX) * .5f;
                if (other.hb.cX < body.cX) left = Math.max(left, midpoint);
                else if (other.hb.cX > body.cX) right = Math.min(right, midpoint);
            }
            CombatTouchTargets.place(body, (left + right) * .5f, (upper + lower) * .5f,
                    right - left, upper - lower);
            Hitbox intent = monster.intentHb;
            CombatTouchTargets.place(intent, intentX(monster), intentY(monster),
                    intent.width * INTENT_SCALE, intent.height * INTENT_SCALE);
        }
    }

    private static void placePile(Object panel, Class<?> type, int slot) {
        Hitbox anchor = ReflectionHacks.getPrivate(panel, type, "hb");
        placeControl(anchor, PortraitBattle.bottomControlX(slot), PortraitBattle.pileY(), PILE_SCALE);
    }

    private static void placeStatusPanel(AbstractPanel panel, Class<?> type, String fieldName, float scale) {
        Hitbox anchor = ReflectionHacks.getPrivate(panel, type, fieldName);
        placeControl( anchor, panel.current_x, panel.current_y, scale);
    }

    private static void placeControl(Hitbox anchor, float x, float y, float scale) {
        CombatTouchTargets.place(anchor, x, y,
                anchor.width * scale, anchor.height * scale);
    }

    private static float relicX(int index) {
        return toolbarX((Sts2Preview.compact() ? 0.065f : 0.55f) + index * 0.095f);
    }

    private static int relicsPerPage() {
        return Sts2Preview.compact() ? 7 : RELICS_PER_PAGE;
    }

    private static void ensureRelicControls() {
        if (previousRelics == null) {
            float size = PortraitViewport.referenceWidth() * 0.064f;
            previousRelics = new Hitbox(size, size);
            nextRelics = new Hitbox(size, size);
        }
        previousRelics.move(toolbarX(Sts2Preview.compact() ? 0.87f : 0.48f), itemsY());
        nextRelics.move(toolbarX(0.935f), itemsY());
    }

    static void updateRelics() {
        if (relicScreen != AbstractDungeon.screen || Settings.hideRelics || CardCrawlGame.isPopupOpen) {
            relicScreen = AbstractDungeon.screen;
            selectedRelic = null;
        }
        if (Settings.hideRelics || CardCrawlGame.isPopupOpen) return;
        int total = AbstractDungeon.player.relics.size();
        int lastPage = Math.max(0, (total - 1) / relicsPerPage());
        relicPage = Math.min(lastPage, relicPage);
        if (lastPage > 0) {
            ensureRelicControls();
            previousRelics.update();
            nextRelics.update();
            if (InputHelper.justClickedLeft && previousRelics.hovered && relicPage > 0) {
                relicPage--;
                selectedRelic = null;
                InputHelper.justClickedLeft = false;
            } else if (InputHelper.justClickedLeft && nextRelics.hovered && relicPage < lastPage) {
                relicPage++;
                selectedRelic = null;
                InputHelper.justClickedLeft = false;
            }
        }
        if (InputHelper.justClickedLeft) {
            AbstractRelic tapped = relicAtPointer();
            selectedRelic = selectedRelic == tapped ? null : tapped;
            if (tapped != null) InputHelper.justClickedLeft = false;
        }
    }

    private static AbstractRelic relicAtPointer() {
        float hit = Math.min(PortraitViewport.referenceWidth() * .046f, Settings.WIDTH * .046f);
        if (Math.abs(InputHelper.mY - itemsY()) >= hit) return null;
        ArrayList<AbstractRelic> relics = AbstractDungeon.player.relics;
        int perPage = relicsPerPage();
        int first = relicPage * perPage;
        for (int i = first; i < Math.min(relics.size(), first + perPage); i++) {
            if (Math.abs(InputHelper.mX - relicX(i - first)) < hit) return relics.get(i);
        }
        return null;
    }

    private static void drawRelics(SpriteBatch batch) {
        if (Settings.hideRelics) return;
        ArrayList<AbstractRelic> items = AbstractDungeon.player.relics;
        float offset = ReflectionHacks.getPrivateStatic(AbstractRelic.class, "offsetX");
        int perPage = relicsPerPage();
        int first = Math.min(relicPage, Math.max(0, (items.size() - 1) / perPage)) * perPage;
        for (int i = first; i < Math.min(items.size(), first + perPage); i++) {
            AbstractRelic relic = items.get(i);
            float x = relicX(i - first);
            transform(batch, relic.currentX + offset, relic.currentY, x, itemsY(), RELIC_SCALE);
            try {
                relic.renderInTopPanel(batch);
            } finally {
                batch.setTransformMatrix(original);
            }
        }
        if (!CardCrawlGame.isPopupOpen) {
            AbstractRelic detail = selectedRelic;
            if (detail != null && !items.contains(detail)) detail = null;
            if (detail == null && !Settings.isTouchScreen && Gdx.app.getType() != Application.ApplicationType.Android
                    && System.getProperty("amethyst.bridge.mode") == null) detail = relicAtPointer();
            if (detail != null) PortraitRelicDetails.queueTopBar(detail);
        }
        if (items.size() > perPage) {
            ensureRelicControls();
            if (relicPage > 0) drawRelicArrow(batch, previousRelics, "<");
            if (first + perPage < items.size()) drawRelicArrow(batch, nextRelics, ">");
        }
    }

    private static void drawRelicArrow(SpriteBatch batch, Hitbox area, String symbol) {
        FontHelper.renderFontCentered(batch, FontHelper.buttonLabelFont, symbol,
                area.cX, area.cY, area.hovered ? Settings.GOLD_COLOR : Color.WHITE);
    }

    static void render(TopPanel panel, SpriteBatch batch) {
        float width = PortraitViewport.referenceWidth();
        float row = toolsY();
        float mid = (row + itemsY()) * 0.5f;
        float bottom = panelBottom();
        PortraitRelicDetails.recordToolbar(batch, bottom);
        float old = batch.getPackedColor();
        batch.setColor(0.08f, 0.105f, 0.13f, 1f);
        batch.draw(ImageMaster.WHITE_SQUARE_IMG, 0, bottom, Settings.WIDTH, Settings.HEIGHT - bottom);
        batch.setColor(0.18f, 0.23f, 0.26f, 0.86f);
        batch.draw(ImageMaster.WHITE_SQUARE_IMG, 0, mid, Settings.WIDTH, 1.5f);
        batch.setColor(0.72f, 0.57f, 0.34f, 0.88f);
        batch.draw(ImageMaster.WHITE_SQUARE_IMG, 0, bottom, Settings.WIDTH, 2f);
        batch.setColor(Color.WHITE);

        drawLowHealthPulse(batch);
        drawTool(panel, batch, hp, panel.hpHb, Tool.HEALTH);
        drawTool(panel, batch, gold, panel.goldHb, Tool.GOLD);
        drawFloorInfo(batch, Tool.FLOOR, ImageMaster.TP_FLOOR,
                Math.max(0, AbstractDungeon.floorNum), Settings.CREAM_COLOR);
        if (AbstractDungeon.isAscensionMode) {
            drawFloorInfo(batch, Tool.ASCENSION, ImageMaster.TP_ASCENSION, AbstractDungeon.ascensionLevel,
                    AbstractDungeon.ascensionLevel == 20 ? Settings.GOLD_COLOR : Settings.RED_TEXT_COLOR);
        }
        drawTool(panel, batch, map, panel.mapHb, Tool.MAP);
        drawTool(panel, batch, deck, panel.deckHb, Tool.DECK);
        float deckScale = toolScale(Tool.DECK);
        drawNumberAt(batch, AbstractDungeon.player.masterDeck.size(),
                toolX(Tool.DECK) + width * 0.035f * deckScale,
                toolY(Tool.DECK) - width * 0.023f * deckScale,
                Settings.CREAM_COLOR, HUD_NUMBER_SCALE * deckScale);
        drawTool(panel, batch, options, panel.settingsHb, Tool.SETTINGS);
        if (Sts2Preview.compact()) {
            Sts2Preview.drawPotions(batch);
            Sts2Preview.drawBand(batch);
        } else if (!AbstractDungeon.player.potions.isEmpty()) {
            AbstractPotion first = AbstractDungeon.player.potions.get(0);
            drawAt(panel, batch, potions, first.hb, potionScreenX(first), itemsY(), potionScale());
        }
        batch.setColor(Color.WHITE);
        float keySize = keyIconSize(width);
        float keyCenter = keyCenterX();
        if (Settings.isFinalActAvailable || Settings.hasRubyKey || Settings.hasEmeraldKey || Settings.hasSapphireKey) {
            drawIcon(batch, ImageMaster.KEY_SLOTS_ICON, keyCenter, itemsY(), keySize);
        }
        if (Settings.hasRubyKey) drawIcon(batch, ImageMaster.RUBY_KEY, keyCenter, itemsY(), keySize);
        if (Settings.hasEmeraldKey) drawIcon(batch, ImageMaster.EMERALD_KEY, keyCenter, itemsY(), keySize);
        if (Settings.hasSapphireKey) drawIcon(batch, ImageMaster.SAPPHIRE_KEY, keyCenter, itemsY(), keySize);
        drawRelics(batch);
        if (showRunInfo()) drawRunInfo(batch);
        batch.setColor(old);
        panel.potionUi.render(batch);
    }

    private static void drawLowHealthPulse(SpriteBatch batch) {
        AbstractPlayer player = AbstractDungeon.player;
        if (AbstractDungeon.isScreenUp || AbstractDungeon.getCurrRoom().phase != AbstractRoom.RoomPhase.COMBAT
                || player.currentHealth >= player.maxHealth / 4) {
            healthPulseTime = 0f;
            return;
        }
        healthPulseTime = (healthPulseTime + Gdx.graphics.getDeltaTime()) % HEALTH_PULSE_PERIOD;
        float pulse = .5f - .5f * MathUtils.cos(MathUtils.PI2 * healthPulseTime / HEALTH_PULSE_PERIOD);
        Tool health = Tool.HEALTH;
        float size = 64f * Settings.scale * HEALTH_ICON_SCALE * toolScale(health) * (1f + .55f * pulse);
        batch.setColor(1f, 1f, 1f, .90f * pulse);
        batch.draw(ImageMaster.TP_HP, toolX(health) - size * .5f, toolY(health) - size * .5f, size, size);
        batch.setColor(Color.WHITE);
    }

    private static void drawIcon(SpriteBatch batch, Texture texture, float x, float y, float size) {
        batch.draw(texture, toolbarX(x) - size * 0.5f, y - size * 0.5f, size, size);
    }

    private static void drawNumber(SpriteBatch batch, int value, float x, float y, Color color) {
        drawNumberAt(batch, value, toolbarX(x), y, color, HUD_NUMBER_SCALE);
    }

    private static void drawNumberAt(SpriteBatch batch, int value, float x, float y, Color color, float scale) {
        FontHelper.renderFontCentered(batch, FontHelper.topPanelInfoFont, Integer.toString(value),
                x, y, color, scale);
    }

    private static void drawFloorInfo(SpriteBatch batch, Tool tool, Texture image, int value, Color color) {
        float x = toolX(tool);
        float y = toolY(tool);
        float width = toolUnit(tool);
        boolean floor = tool == Tool.FLOOR;
        float size = width * (floor ? FLOOR_ICON_SIZE : 0.058f);
        batch.draw(image, x - size * 0.5f, y - size * 0.5f, size, size);
        float fontScale = GOLD_ICON_SCALE * toolScale(tool);
        drawNumberAt(batch, value, x + width * (floor ? 0.045f : 0.036f), y, color, fontScale);
    }

    private static void drawRunInfo(SpriteBatch batch) {
        batch.setColor(Color.WHITE);
        float width = PortraitViewport.referenceWidth();
        float y = runInfoY();
        if (showClock()) drawClock(batch, y, hasRunIcons());
        ArrayList<AbstractDailyMod> mods = ModHelper.enabledMods;
        ArrayList<AbstractBlight> blights = AbstractDungeon.player.blights;
        int count = mods.size() + blights.size();
        if (count == 0) return;
        float start = showClock() ? .31f : .03f;
        float step = Math.min(0.075f, (0.98f - start) / count);
        float size = Math.min(width * 0.049f, step * width * 0.86f);
        for (int i = 0; i < mods.size(); i++) {
            AbstractDailyMod mod = mods.get(i);
            float x = start + (i + 0.5f) * step;
            drawRunIcon(batch, mod.img, mod.name, mod.description, x, y, size);
        }
        for (int i = 0; i < blights.size(); i++) {
            AbstractBlight blight = blights.get(i);
            float x = start + (mods.size() + i + 0.5f) * step;
            if (blight.img == null || !blight.isObtained || blight.discarded) continue;
            drawRunIcon(batch, blight.img, blight.name, blight.description, x, y, size);
            if (blight.counter >= 0) {
                drawNumber(batch, blight.counter, x + size / width * 0.40f, y - size * 0.25f, Settings.CREAM_COLOR);
            }
        }
    }

    private static void drawRunIcon(SpriteBatch batch, Texture image, String name, String description,
                                    float x, float y, float size) {
        drawIcon(batch, image, x, y, size);
        if (Math.abs(InputHelper.mX - toolbarX(x)) < size * 0.5f
                && Math.abs(InputHelper.mY - y) < size * 0.5f) {
            TipHelper.renderGenericTip(PortraitViewport.x(0.04f), panelBottom() - size * 0.12f, name, description);
        }
    }

    private static void drawClock(SpriteBatch batch, float y, boolean shareRow) {
        int seconds = (int) CardCrawlGame.playtime;
        if (seconds != lastClockSecond) {
            lastClockSecond = seconds;
            clockLabel = CharStat.formatHMSM(seconds);
        }
        drawIcon(batch, ImageMaster.TIMER_ICON, shareRow ? 0.065f : 0.445f, y,
                PortraitViewport.referenceWidth() * 0.043f);
        FontHelper.renderFontCentered(batch, FontHelper.topPanelInfoFont, clockLabel,
                toolbarX(shareRow ? 0.147f : 0.518f), y,
                CardCrawlGame.stopClock ? Settings.GREEN_TEXT_COLOR : Settings.GOLD_COLOR,
                1.36f);
    }

    private static void drawTool(TopPanel panel, SpriteBatch batch, RMethod method, Hitbox anchor, Tool tool) {
        float scale = TOP_ICON_SCALE * toolScale(tool);
        float x = toolX(tool);
        if (tool == Tool.HEALTH || tool == Tool.GOLD) {
            scale = (tool == Tool.HEALTH ? HEALTH_ICON_SCALE : GOLD_ICON_SCALE) * toolScale(tool);
            // The native renderers add 32 scaled pixels to the hitbox's left edge before drawing.
            x += (anchor.width * .5f - 32f * Settings.scale) * scale;
        }
        drawAt(panel, batch, method, anchor, x, toolY(tool), scale);
    }

    private static void drawAt(TopPanel panel, SpriteBatch batch, RMethod method, Hitbox anchor,
                               float x, float y, float scale) {
        transform(batch, anchor.cX, anchor.cY, x, y, scale);
        try {
            method.invoke(panel, batch);
        } finally {
            batch.setTransformMatrix(original);
        }
    }

    @SpirePatch(clz = PingHpEffect.class, method = "render", paramtypez = {SpriteBatch.class})
    public static class NativeHealthPulse {
        @SpirePrefixPatch
        public static SpireReturn<Void> before(PingHpEffect __instance) {
            return topBarActive() ? SpireReturn.Return() : SpireReturn.Continue();
        }
    }

    @SpirePatch(clz = HealPanelEffect.class, method = "render", paramtypez = {SpriteBatch.class})
    public static class HealingEffectDrawing {
        @SpirePrefixPatch
        public static void before(Object __instance, SpriteBatch batch, float ___x) {
            if (!topBarActive()) return;
            Tool health = Tool.HEALTH;
            float scale = HEALTH_ICON_SCALE * toolScale(health);
            float nativeInset = 32f * Settings.scale;
            healthEffectOriginal.set(batch.getTransformMatrix());
            healthEffectMoved.set(healthEffectOriginal)
                    .translate(toolX(health) - (___x + nativeInset) * scale,
                            toolY(health) - (Settings.HEIGHT - nativeInset) * scale, 0f)
                    .scale(scale, scale, 1f);
            batch.setTransformMatrix(healthEffectMoved);
        }

        @SpirePostfixPatch
        public static void after(Object __instance, SpriteBatch batch) {
            if (topBarActive()) batch.setTransformMatrix(healthEffectOriginal);
        }
    }

    private static float keyCenterX() {
        return Sts2Preview.compact() ? 0.755f : 0.405f;
    }

    private static float keyIconSize(float width) {
        return width * 0.056f;
    }

    @SpirePatch(clz = ObtainKeyEffect.class, method = "render", paramtypez = {SpriteBatch.class})
    public static class ObtainKeyEffectDrawing {
        @SpirePrefixPatch
        public static void before(ObtainKeyEffect __instance, SpriteBatch batch, float ___x, float ___y) {
            if (!topBarActive()) return;
            float width = PortraitViewport.referenceWidth();
            float keySize = keyIconSize(width);
            float effectScale = ReflectionHacks.<Float>getPrivate(__instance, AbstractGameEffect.class, "scale");
            float rawRatio = effectScale / Math.max(0.01f, 1.1f * Settings.scale);
            float progress = MathUtils.clamp((rawRatio - 1f) / (5.0f / 1.1f - 1f), 0f, 1f);
            float maxMultiplier = Math.min(3.0f, Math.max(1.8f, (Settings.HEIGHT - itemsY()) / (keySize * 0.60f)));
            float desiredSize = keySize * (1f + (maxMultiplier - 1f) * progress);
            float scale = desiredSize / Math.max(1f, 64f * effectScale);
            float sourceX = ___x + 32f;
            float sourceY = ___y + 32f;
            float targetX = toolbarX(keyCenterX());
            float targetY = itemsY();
            keyEffectOriginal.set(batch.getTransformMatrix());
            keyEffectMoved.set(keyEffectOriginal)
                    .translate(targetX - sourceX * scale, targetY - sourceY * scale, 0f)
                    .scale(scale, scale, 1f);
            batch.setTransformMatrix(keyEffectMoved);
        }

        @SpirePostfixPatch
        public static void after(ObtainKeyEffect __instance, SpriteBatch batch) {
            if (topBarActive()) batch.setTransformMatrix(keyEffectOriginal);
        }
    }

    private static void transform(SpriteBatch batch, float sourceX, float sourceY, float x, float y, float scale) {
        original.set(batch.getTransformMatrix());
        moved.set(original).translate(x - sourceX * scale, y - sourceY * scale, 0f).scale(scale, scale, 1f);
        batch.setTransformMatrix(moved);
    }

    public static boolean usesLargerSprites() {
        return active();
    }

    public static boolean usesLargerSprites(Object creature) {
        if (active()) return true;
        return creature instanceof AbstractPlayer
                && (OpeningPatches.neowActive() || PortraitEvents.roomSceneActive());
    }

    private static float arenaEnemyScale() {
        float w = Math.max(1f, PortraitViewport.contentWidth());
        float arena = PortraitBattle.enemyArenaHeight();
        return MathUtils.clamp((float) Math.sqrt(arena / (w * 0.80f)), 0.60f, 1.0f);
    }

    private static float bodyScale(AbstractCreature creature) {
        if (creature instanceof AbstractPlayer) return 1.43f;
        float previewScale = Math.max(1.12f, Math.min(1.65f, 1.08f + 58f / Math.max(1f, creature.hb.height)));
        return SlimeSplitLayout.fitBody(creature, previewScale * ENEMY_SCALE * arenaEnemyScale())
                * AwakenedFormation.bodyScale(creature) * PortraitFinalAct.bodyScale(creature);
    }

    @SpirePatch(clz = AbstractMonster.class, method = SpirePatch.CLASS)
    public static class IntentBounds {
        public static final SpireField<BodyTop> TOP = new SpireField<>(BodyTop::new);
    }

    public static final class BodyTop {
        long sampleAfter;
        float offset = Float.NaN;
        float textureReach = Float.NaN;
        float left = Float.NaN;
        float right = Float.NaN;
    }

    private static final Vector2 boundsOffset = new Vector2();
    private static final Vector2 boundsSize = new Vector2();
    private static final WeakHashMap<Texture, Float> textureTops = new WeakHashMap<>();

    private static void captureBodyTop(AbstractMonster monster, Skeleton skeleton, float scale) {
        BodyTop bounds = IntentBounds.TOP.get(monster);
        long frame = Gdx.graphics.getFrameId();
        if (frame < bounds.sampleAfter) return;
        bounds.sampleAfter = frame + 8;
        skeleton.getBounds(boundsOffset, boundsSize);
        if (Float.isFinite(boundsSize.y) && boundsSize.y > 0f) {
            float top = skeleton.getY() + (boundsOffset.y + boundsSize.y - skeleton.getY()) * scale;
            bounds.offset = top - monster.drawY;
        }
        if (Float.isFinite(boundsOffset.x) && Float.isFinite(boundsSize.x) && boundsSize.x > 0f) {
            float left = skeleton.getX() + (boundsOffset.x - skeleton.getX()) * scale;
            bounds.left = left - monster.drawX;
            bounds.right = bounds.left + boundsSize.x * scale;
        }
    }

    public static void captureTextureTop(AbstractCreature creature, Texture texture, float y, float height, boolean flipY) {
        if (!(creature instanceof AbstractMonster) || creature instanceof Hexaghost) return;
        float fraction = flipY ? 1f : textureTops.computeIfAbsent(texture, PortraitCombatHud::opaqueTop);
        float top = y + height * fraction;
        float reach = top - creature.drawY;
        BodyTop bounds = IntentBounds.TOP.get((AbstractMonster) creature);
        bounds.textureReach = reach;
        float scale = fittedTextureScale(creature, reach);
        bounds.offset = reach * scale;
    }

    private static float fittedTextureScale(AbstractCreature creature, float reach) {
        float scale = bodyScale(creature);
        if (creature instanceof AbstractMonster && Float.isFinite(reach) && reach > 0f) {
            float margin = PortraitViewport.referenceWidth() * .072f;
            float room = panelBottom() - creature.drawY - margin;
            if (room > 0f) scale = Math.min(scale, Math.max(.35f, room / reach));
        }
        return scale;
    }

    private static float opaqueTop(Texture texture) {
        if (!(texture.getTextureData() instanceof FileTextureData)) return 1f;
        Pixmap pixels = new Pixmap(((FileTextureData) texture.getTextureData()).getFileHandle());
        try {
            for (int row = 0; row < pixels.getHeight(); row++) {
                for (int column = 0; column < pixels.getWidth(); column++) {
                    if ((pixels.getPixel(column, row) & 255) > 50) return 1f - row / (float) pixels.getHeight();
                }
            }
            return 1f;
        } finally {
            pixels.dispose();
        }
    }

    public static void drawBody(AbstractCreature creature, SkeletonMeshRenderer renderer,
                                PolygonSpriteBatch batch, Skeleton skeleton) {
        float x = skeleton.getX();
        float y = skeleton.getY();
        float scale = bodyScale(creature);
        if (creature instanceof AbstractMonster) {
            skeleton.getBounds(boundsOffset, boundsSize);
            float reach = boundsOffset.y + boundsSize.y - y;
            if (Float.isFinite(reach) && reach > 0f) {
                float margin = PortraitViewport.referenceWidth() * (creature instanceof CorruptHeart ? .045f : .072f);
                float room = panelBottom() - y - margin;
                if (room > 0f) scale = Math.min(scale, Math.max(.35f, room / reach));
            }
            captureBodyTop((AbstractMonster) creature, skeleton, scale);
        }
        poseOriginal.set(batch.getTransformMatrix());
        poseScaled.set(poseOriginal).translate(x, y, 0).scale(scale, scale, 1f).translate(-x, -y, 0);
        batch.setTransformMatrix(poseScaled);
        try {
            renderer.draw(batch, skeleton);
        } finally {
            batch.setTransformMatrix(poseOriginal);
        }
    }

    public static void beginBodyTexture(AbstractCreature creature, SpriteBatch batch) {
        if (creature instanceof Hexaghost) {
            Hexaghost boss = (Hexaghost) creature;
            float s = hexaghostScale(boss);
            transform(batch, boss.drawX, hexaghostSourceY(boss), boss.drawX, hexaghostTargetY(boss, s), s);
            return;
        }
        float reach = creature instanceof AbstractMonster
                ? IntentBounds.TOP.get((AbstractMonster) creature).textureReach
                : Float.NaN;
        float scale = fittedTextureScale(creature, reach);
        transform(batch, creature.drawX, creature.drawY, creature.drawX, creature.drawY, scale);
    }

    public static void endBodyTexture(SpriteBatch batch) {
        batch.setTransformMatrix(original);
    }

    @SpirePatch(clz = AbstractPlayer.class, method = "renderPlayerImage", paramtypez = {SpriteBatch.class})
    @SpirePatch(clz = AbstractMonster.class, method = "render", paramtypez = {SpriteBatch.class})
    public static class BodyDrawing {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override
                public void edit(MethodCall call) throws CannotCompileException {
                    if (!"draw".equals(call.getMethodName())) return;
                    if (SkeletonMeshRenderer.class.getName().equals(call.getClassName())) {
                        call.replace("{ if (sts1portrait.PortraitCombatHud.usesLargerSprites(this))"
                                + " sts1portrait.PortraitCombatHud.drawBody(this, $0, $1, $2); else $proceed($$); }");
                    } else if (SpriteBatch.class.getName().equals(call.getClassName())) {
                        call.replace("{ if (sts1portrait.PortraitCombatHud.usesLargerSprites(this)) {"
                                + ("(Lcom/badlogic/gdx/graphics/Texture;FFFFIIIIZZ)V".equals(call.getSignature())
                                        ? " sts1portrait.PortraitCombatHud.captureTextureTop(this, $1, $3, $5, $11);" : "")
                                + " sts1portrait.PortraitCombatHud.beginBodyTexture(this, $0);"
                                + " try { $proceed($$); } finally { sts1portrait.PortraitCombatHud.endBodyTexture($0); }"
                                + " } else $proceed($$); }");
                    }
                }
            };
        }
    }

    private static Hexaghost updatingHexaghost;

    private static float hexaghostSourceY(Hexaghost boss) {
        return boss.drawY + 256f * Settings.scale;
    }

    private static float hexaghostScale(Hexaghost boss) {
        float scale = 1.55f * arenaEnemyScale();
        float totalReach = 385f * Settings.scale;
        float room = panelBottom() - boss.drawY - PortraitViewport.referenceWidth() * .085f;
        if (room > 0f && totalReach > 0f) {
            scale = Math.min(scale, Math.max(.60f, room / totalReach));
        }
        return scale;
    }

    private static float hexaghostTargetY(Hexaghost boss, float scale) {
        return boss.drawY + 205f * Settings.scale * scale;
    }

    public static void drawHexaghostBody(Hexaghost boss, HexaghostBody body, SpriteBatch batch) {
        float x = boss.drawX;
        float s = hexaghostScale(boss);
        transform(batch, x, hexaghostSourceY(boss), x, hexaghostTargetY(boss, s), s);
        try {
            body.render(batch);
        } finally {
            batch.setTransformMatrix(original);
        }
    }

    public static float hexaghostOrbX(float rawX) {
        if (updatingHexaghost == null || !active()) return rawX;
        float s = hexaghostScale(updatingHexaghost);
        return updatingHexaghost.drawX + (rawX - updatingHexaghost.drawX) * s;
    }

    public static float hexaghostOrbY(float rawY) {
        if (updatingHexaghost == null || !active()) return rawY;
        float s = hexaghostScale(updatingHexaghost);
        return hexaghostTargetY(updatingHexaghost, s) + (rawY - hexaghostSourceY(updatingHexaghost)) * s;
    }

    @SpirePatch(clz = Hexaghost.class, method = "update", paramtypez = {})
    public static class HexaghostUpdateContext {
        @SpirePrefixPatch
        public static void before(Hexaghost __instance) {
            if (active()) updatingHexaghost = __instance;
        }

        @SpirePostfixPatch
        public static void after(Hexaghost __instance) {
            updatingHexaghost = null;
        }
    }

    @SpirePatch(clz = HexaghostOrb.class, method = "update", paramtypez = {float.class, float.class})
    public static class HexaghostOrbEffects {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override
                public void edit(NewExpr expr) throws CannotCompileException {
                    String name = expr.getClassName();
                    if (GhostlyFireEffect.class.getName().equals(name)
                            || GhostlyWeakFireEffect.class.getName().equals(name)) {
                        expr.replace("{ $1 = sts1portrait.PortraitCombatHud.hexaghostOrbX($1);"
                                + " $2 = sts1portrait.PortraitCombatHud.hexaghostOrbY($2);"
                                + " $_ = $proceed($$); }");
                    }
                }
            };
        }
    }

    @SpirePatch(clz = Hexaghost.class, method = "render", paramtypez = {SpriteBatch.class})
    public static class HexaghostDrawing {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override
                public void edit(MethodCall call) throws CannotCompileException {
                    if (HexaghostBody.class.getName().equals(call.getClassName()) && "render".equals(call.getMethodName())) {
                        call.replace("{ if (sts1portrait.PortraitCombatHud.usesLargerSprites())"
                                + " sts1portrait.PortraitCombatHud.drawHexaghostBody(this, $0, $1); else $proceed($$); }");
                    }
                }
            };
        }
    }

    @SpirePatch(clz = AbstractMonster.class, method = "renderIntent", paramtypez = {SpriteBatch.class})
    @SpirePatch(clz = AbstractMonster.class, method = "renderDamageRange", paramtypez = {SpriteBatch.class})
    public static class IntentDrawing {
        @SpirePrefixPatch
        public static void before(AbstractMonster __instance, SpriteBatch batch) {
            if (active()) {
                Hitbox anchor = __instance.intentHb;
                transform(batch, anchor.cX, anchor.cY, intentX(__instance), intentY(__instance), INTENT_SCALE);
            }
        }

        @SpirePostfixPatch
        public static void after(AbstractMonster __instance, SpriteBatch batch) {
            if (active()) batch.setTransformMatrix(original);
        }
    }

    private static float intentX(AbstractMonster monster) {
        return monster.hb.cX;
    }

    private static float visibleBodyTop(AbstractMonster monster) {
        if (monster instanceof Hexaghost) {
            Hexaghost boss = (Hexaghost) monster;
            float s = hexaghostScale(boss);
            return hexaghostTargetY(boss, s) + 185f * Settings.scale * s;
        }
        float offset = IntentBounds.TOP.get(monster).offset;
        return Float.isNaN(offset)
                ? monster.hb.y + monster.hb.height * bodyScale(monster)
                : monster.drawY + offset;
    }

    private static float intentY(AbstractMonster monster) {
        float bodyTop = visibleBodyTop(monster);
        float bodyHeight = bodyTop - monster.hb.y;
        float gap = PortraitViewport.referenceWidth() * (bodyHeight < PortraitViewport.referenceWidth() * .18f
                ? .008f : .018f);
        float intentReach = 32f * Settings.scale * INTENT_SCALE;
        return Math.min(bodyTop + gap + intentReach, panelBottom() - intentReach - gap);
    }

    @SpirePatch(clz = PotionPopUp.class, method = "open", paramtypez = {int.class, AbstractPotion.class})
    public static class PotionMenu {
        @SpirePostfixPatch
        public static void after(PotionPopUp __instance, int slot, AbstractPotion potion) {
            if (!topBarActive()) return;
            Hitbox top = ReflectionHacks.getPrivate(__instance, PotionPopUp.class, "hbTop");
            Hitbox bottom = ReflectionHacks.getPrivate(__instance, PotionPopUp.class, "hbBot");
            float x = potionScreenX(potion);
            float y = Sts2Preview.compact() ? Sts2Preview.potionY(itemsY())
                    + PortraitViewport.contentWidth() * .047f + 169f * Settings.scale
                    : panelBottom() - top.height * 0.5f - 56f * Settings.scale;
            ReflectionHacks.setPrivate(__instance, PotionPopUp.class, "x", x);
            ReflectionHacks.setPrivate(__instance, PotionPopUp.class, "y", y);
            float rowOffset = PortraitPopupMenus.potionRowOffset();
            top.move(x, y + 44f * Settings.scale + rowOffset);
            bottom.move(x, y - 76f * Settings.scale + rowOffset);
        }
    }

    @SpirePatch(clz = AbstractMonster.class, method = "renderIntentVfxBehind", paramtypez = {SpriteBatch.class})
    @SpirePatch(clz = AbstractMonster.class, method = "renderIntentVfxAfter", paramtypez = {SpriteBatch.class})
    public static class IntentEffects {
        @SpirePrefixPatch
        public static void before(AbstractMonster __instance, SpriteBatch batch) {
            IntentDrawing.before(__instance, batch);
        }

        @SpirePostfixPatch
        public static void after(AbstractMonster __instance, SpriteBatch batch) {
            IntentDrawing.after(__instance, batch);
        }
    }

    @SpirePatch(clz = DrawPilePanel.class, method = "render", paramtypez = {SpriteBatch.class})
    public static class DrawPileDrawing {
        @SpirePrefixPatch
        public static void before(DrawPilePanel __instance, SpriteBatch batch) {
            if (active()) drawPile(batch, ReflectionHacks.getPrivate(__instance, DrawPilePanel.class, "hb"),
                    DRAW_PILE_SLOT);
        }

        @SpirePostfixPatch
        public static void after(DrawPilePanel __instance, SpriteBatch batch) {
            if (active()) batch.setTransformMatrix(original);
        }
    }

    @SpirePatch(clz = DiscardPilePanel.class, method = "render", paramtypez = {SpriteBatch.class})
    public static class DiscardPileDrawing {
        @SpirePrefixPatch
        public static void before(DiscardPilePanel __instance, SpriteBatch batch) {
            if (active()) drawPile(batch, ReflectionHacks.getPrivate(__instance, DiscardPilePanel.class, "hb"),
                    DISCARD_PILE_SLOT);
        }

        @SpirePostfixPatch
        public static void after(DiscardPilePanel __instance, SpriteBatch batch) {
            if (active()) batch.setTransformMatrix(original);
        }
    }

    @SpirePatch(clz = EndTurnButton.class, method = "render", paramtypez = {SpriteBatch.class})
    @SpirePatch(clz = EnergyPanel.class, method = "render", paramtypez = {SpriteBatch.class})
    @SpirePatch(clz = ExhaustPanel.class, method = "render", paramtypez = {SpriteBatch.class})
    public static class BottomControlDrawing {
        @SpirePrefixPatch
        public static void before(Object __instance, SpriteBatch batch) {
            if (!active()) return;
            if (__instance instanceof EndTurnButton) {
                Hitbox anchor = ReflectionHacks.getPrivate(__instance, EndTurnButton.class, "hb");
                // The portrait End Turn hitbox already covers the enlarged button.
                drawControl(batch, anchor, anchor.cX, anchor.cY,
                        anchor.cX, anchor.cY, END_TURN_SCALE);
            } else {
                AbstractPanel panel = (AbstractPanel) __instance;
                boolean energy = panel instanceof EnergyPanel;
                Hitbox anchor = energy ? ReflectionHacks.getPrivate(panel, EnergyPanel.class, "tipHitbox")
                        : ReflectionHacks.getPrivate(panel, ExhaustPanel.class, "hb");
                float scale = energy ? ENERGY_SCALE : EXHAUST_SCALE;
                drawControl(batch, anchor,
                        panel.current_x, panel.current_y, panel.current_x, panel.current_y,
                        scale);
            }
        }

        @SpirePostfixPatch
        public static void after(Object __instance, SpriteBatch batch) {
            if (active()) batch.setTransformMatrix(original);
        }
    }

    @SpirePatch(clz = ExhaustPileParticle.class, method = "render", paramtypez = {SpriteBatch.class})
    public static class ExhaustParticleDrawing {
        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                @Override public void edit(MethodCall call) throws CannotCompileException {
                    if (SpriteBatch.class.getName().equals(call.getClassName()) && "draw".equals(call.getMethodName())) {
                        call.replace("{ sts1portrait.PortraitCombatHud.drawExhaustParticle($0, $$); }");
                    }
                }
            };
        }
    }

    public static void drawExhaustParticle(SpriteBatch batch, TextureRegion region, float x, float y,
            float originX, float originY, float width, float height, float scaleX, float scaleY, float rotation) {
        if (active() && AbstractDungeon.overlayMenu != null) {
            ExhaustPanel panel = AbstractDungeon.overlayMenu.exhaustPanel;
            x = panel.current_x - originX;
            y = panel.current_y - originY;
            float scale = EXHAUST_SCALE;
            scaleX *= scale;
            scaleY *= scale;
        }
        batch.draw(region, x, y, originX, originY, width, height, scaleX, scaleY, rotation);
    }

    private static void drawPile(SpriteBatch batch, Hitbox anchor, int slot) {
        drawControl(batch, anchor, anchor.cX, anchor.cY,
                PortraitBattle.bottomControlX(slot), PortraitBattle.pileY(),
                PILE_SCALE);
    }

    private static void drawControl(SpriteBatch batch, Hitbox anchor,
            float fromX, float fromY, float x, float y, float drawScale) {
        transform(batch, fromX, fromY, x, y, drawScale);
    }

}
