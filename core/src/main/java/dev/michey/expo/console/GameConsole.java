package dev.michey.expo.console;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Align;
import dev.michey.expo.command.CommandResolver;
import dev.michey.expo.console.command.*;
import dev.michey.expo.render.RenderContext;

import java.util.*;

import static dev.michey.expo.log.ExpoLogger.log;
import static dev.michey.expo.log.ExpoLogger.logc;

public class GameConsole {

    // Commands
    private final ConsoleCommandResolver resolver;
    // Console message history
    private final List<ConsoleMessage> messageHistory;
    private final String ALLOWED_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789 _-:?!;<>[]#(){}./,&äöüÄÖÜß+=";
    private final HashMap<Integer, ConsoleMessage> indexToMessageMap;
    private int historyLinesTotal;
    // Console command line
    private int currentUpDownIndex;
    private String fullConsoleLine = "";
    private String displayedConsoleLine = "";
    private int cursorPos;
    private int selectionA;
    private int selectionB;
    private int lastCursorPos;
    private int lastSelectionA;
    private int lastSelectionB;
    private String lastFullConsoleLine = "";
    private int viewportA;
    private int viewportB;
    private int indicatorDisplacement;
    private float leftRightDelta;
    private int selectionOffset;
    private int selectionWidth;
    private int selectionHeight;
    private boolean lastDirectionRight = true;
    // Render related members
    private boolean visible;
    public final ShapeRenderer shapeRenderer;
    private final SpriteBatch batch;
    public BitmapFont consoleFont;
    private BitmapFont consoleFontNoMarkup;
    private int CONSOLE_TOTAL_WIDTH = 800;//1248;
    private int CONSOLE_TOTAL_HEIGHT = 238+66;
    public int CONSOLE_HISTORY_TARGET_WIDTH = 695;
    private int CONSOLE_COMMAND_TARGET_WIDTH = 768;//768;
    private final GlyphLayout historyGlyph;
    private final GlyphLayout calcGlyph;
    private int dragX;
    private int dragY;
    private int lastDragX;
    private int lastDragY;
    private int lastClickX;
    private int lastClickY;
    private int displayEntries = 17;
    public int displayFromLineIndex = 0;
    public int displayToLineIndex = displayEntries - 1;
    private int[] queuedResize = null;
    // Keys
    private final List<Integer> activeKeyList;
    // Colors
    private final Color COLOR_OUTER = Color.valueOf("0c0c12");
    private final Color COLOR_BASE = Color.valueOf("0f0f14");
    private final Color COLOR_INNER = Color.valueOf("16161c");
    private final Color COLOR_SELECTION = Color.valueOf("0e3666");
    // Sync lock
    public static final Object CONSOLE_LOCK = new Object();

    public GameConsole() {
        resolver = new ConsoleCommandResolver();
        resolver.addCommand(new CommandClear());
        resolver.addCommand(new CommandHelp());
        resolver.addCommand(new CommandResize());
        resolver.addCommand(new CommandResizelines());
        resolver.addCommand(new CommandConnect());
        resolver.addCommand(new CommandWorld());
        resolver.addCommand(new CommandQuit());
        resolver.addCommand(new CommandUsername());
        resolver.addCommand(new CommandNoise());
        resolver.addCommand(new CommandZoom());
        resolver.addCommand(new CommandRiver());
        resolver.addCommand(new CommandSpeed());
        resolver.addCommand(new CommandFps());
        resolver.addCommand(new CommandSaves());
        resolver.addCommand(new CommandSpawn());
        resolver.addCommand(new CommandTp());
        resolver.addCommand(new CommandTime());
        resolver.addCommand(new CommandVolume());
        resolver.addCommand(new CommandReload());
        resolver.addCommand(new CommandUiscale());
        resolver.addCommand(new CommandDelsave());
        resolver.addCommand(new CommandDump());

        messageHistory = new LinkedList<>();
        shapeRenderer = new ShapeRenderer();
        batch = new SpriteBatch();
        activeKeyList = new LinkedList<>();
        historyGlyph = new GlyphLayout();
        calcGlyph = new GlyphLayout();
        indexToMessageMap = new HashMap<>();
        generateFonts();
    }

    public boolean isActiveKey(int keycode) {
        return activeKeyList.contains(keycode);
    }

    private int calculatePredictedTextWidth(String s) {
        calcGlyph.setText(consoleFontNoMarkup, s);
        return (int) calcGlyph.width;
    }

    private String getCurrentSelection() {
        return fullConsoleLine.substring(selectionA, selectionB);
    }

    private String getUntilSelectionA() {
        return fullConsoleLine.substring(0, selectionA);
    }

    private String getAfterSelectionB() {
        return fullConsoleLine.substring(selectionB);
    }

    private String getUntilCursor() {
        return fullConsoleLine.substring(0, cursorPos);
    }

    private String getAfterCursor() {
        return fullConsoleLine.substring(cursorPos);
    }

    private boolean consoleLineIsEmpty() {
        return fullConsoleLine.length() == 0;
    }

    private void resetSelection() {
        selectionA = 0;
        selectionB = 0;
    }

    public void onKeyCombo(int keycode1, int keycode2) {
        boolean ctrl = keycode1 == Input.Keys.CONTROL_LEFT;
        boolean shift = keycode1 == Input.Keys.SHIFT_LEFT;

        if(ctrl) {
            if(keycode2 == Input.Keys.A) { // Ctrl+A
                selectionA = 0;
                selectionB = fullConsoleLine.length();
            } else if(keycode2 == Input.Keys.C) { // Ctrl+C
                Gdx.app.getClipboard().setContents(getCurrentSelection());
            } else if(keycode2 == Input.Keys.V && Gdx.app.getClipboard().hasContents()) { // Ctrl+V
                String converted = Gdx.app.getClipboard().getContents().replace(System.lineSeparator(), "");

                for(char c : converted.toCharArray()) {
                    if(!ALLOWED_CHARS.contains(String.valueOf(c))) {
                        converted = converted.replace(String.valueOf(c), "");
                    }
                }

                if(isTextSelected()) {
                    fullConsoleLine = getUntilSelectionA() + converted + getAfterSelectionB();
                    cursorPos = selectionA + converted.length();
                    resetSelection();
                } else {
                    if(consoleLineIsEmpty()) {
                        fullConsoleLine = converted;
                        cursorPos = converted.length();
                    } else {
                        fullConsoleLine = getUntilCursor() + converted + getAfterCursor();
                        cursorPos += converted.length();
                    }
                }
            } else if(keycode2 == Input.Keys.X) { // Ctrl+X
                if(isTextSelected()) {
                    Gdx.app.getClipboard().setContents(getCurrentSelection());
                    fullConsoleLine = getUntilSelectionA() + getAfterSelectionB();
                    cursorPos = selectionA;
                    resetSelection();
                }
            } else if(keycode2 == Input.Keys.LEFT) { // Ctrl+LEFT
                cursorPos = 0;
            } else if(keycode2 == Input.Keys.RIGHT) { // Ctrl+RIGHT
                cursorPos = fullConsoleLine.length();
            }
        } else if(shift) {
            if(keycode2 == Input.Keys.LEFT) { // Shift+LEFT
                onShiftLeft();
            } else if(keycode2 == Input.Keys.RIGHT) { // Shift+RIGHT
                onShiftRight();
            }
        }
    }

    public void setActiveKey(int keycode, boolean flag) {
        if(flag) {
            activeKeyList.add(keycode);
        } else {
            activeKeyList.remove(Integer.valueOf(keycode));
        }
    }

    private void generateFonts() {
        //FreeTypeFontGenerator gen = new FreeTypeFontGenerator(Gdx.files.internal("fonts/JetBrainsMono-Bold.ttf"));
        FreeTypeFontGenerator gen = new FreeTypeFontGenerator(Gdx.files.internal("fonts/m5x7.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter param = new FreeTypeFontGenerator.FreeTypeFontParameter();
        param.size = 16;
        consoleFont = gen.generateFont(param);
        consoleFontNoMarkup = gen.generateFont(param);
        gen.dispose();
        consoleFont.getData().markupEnabled = true;
    }

    public void onArrow(int keycode) {
        if(keycode == Input.Keys.LEFT) {
            if(isActiveKey(Input.Keys.SHIFT_LEFT)) {
                onShiftLeft();
            } else {
                resetSelection();
                cursorPos--;
                if(cursorPos < 0) cursorPos = 0;
            }
        } else if(keycode == Input.Keys.RIGHT) {
            if(isActiveKey(Input.Keys.SHIFT_LEFT)) {
                onShiftRight();
            } else {
                resetSelection();
                cursorPos++;
                if(cursorPos > fullConsoleLine.length()) cursorPos = fullConsoleLine.length();
            }
        } else if(keycode == Input.Keys.UP || keycode == Input.Keys.DOWN) {
            ConsoleMessage next = findNextConsoleSuggestion(keycode == Input.Keys.UP);

            if(next != null) {
                fullConsoleLine = next.message;
                cursorPos = fullConsoleLine.length();
                resetSelection();
            }
        }
    }

    private ConsoleMessage findNextConsoleSuggestion(boolean up) {
        if(messageHistory.isEmpty()) return null;
        ConsoleMessage found = null;

        if(up) {
            while(found == null) {
                currentUpDownIndex--;

                if(currentUpDownIndex < 0) {
                    currentUpDownIndex = 0;
                    break;
                }

                ConsoleMessage next = messageHistory.get(currentUpDownIndex);

                if(next.byUser) {
                    found = next;
                }
            }
        } else {
            while(found == null) {
                currentUpDownIndex++;

                if(currentUpDownIndex >= messageHistory.size()) {
                    currentUpDownIndex--;
                    break;
                }

                ConsoleMessage next = messageHistory.get(currentUpDownIndex);

                if(next.byUser) {
                    found = next;
                }
            }
        }

        return found;
    }

    private void deleteChar() {
        if(isEntireTextSelected()) {
            fullConsoleLine = "";
            cursorPos = 0;
        } else {
            String s1 = getUntilSelectionA();
            fullConsoleLine = s1 + getAfterSelectionB();
            cursorPos = s1.length();
        }

        resetSelection();
        currentUpDownIndex = messageHistory.size();
    }

    public void addSystemMessage(String message) {
        addConsoleMessage(new ConsoleMessage(message, false));
    }

    public void addSystemErrorMessage(String message) {
        addSystemMessage("[RED][ERROR] " + message);
    }

    public void addSystemWarningMessage(String message) {
        addSystemMessage("[YELLOW][WARN] " + message);
    }

    public void addSystemSuccessMessage(String message) {
        addSystemMessage("[GREEN][SUCCESS] " + message);
    }

    public void addConsoleMessage(ConsoleMessage message) {
        synchronized (CONSOLE_LOCK) {
            messageHistory.add(message);
            fullConsoleLine = "";
            cursorPos = 0;
            currentUpDownIndex = messageHistory.size();
            resetSelection();

            int cover = message.lines;
            int startIndex = historyLinesTotal;
            message.startIndex = startIndex;

            for(int i = 0; i < cover; i++) {
                indexToMessageMap.put(startIndex + i, message);
            }

            historyLinesTotal += message.lines;
            message.endIndex = historyLinesTotal - 1;

            logc(message.message);

            if(message.byUser) {
                // See if it's a user command
                if(message.message.charAt(0) == '/') {
                    resolver.resolveCommand(message.message);
                }
            }
        }
    }

    public void updateAllMessages() {
        indexToMessageMap.clear();
        historyLinesTotal = 0;

        for(ConsoleMessage iteration : messageHistory) {
            int startIndex = historyLinesTotal;

            iteration.generateRenderData();
            int cover = iteration.lines;
            iteration.startIndex = startIndex;

            for (int j = 0; j < cover; j++) {
                indexToMessageMap.put(startIndex + j, iteration);
            }

            historyLinesTotal += iteration.lines;
            iteration.endIndex = historyLinesTotal - 1;
        }
    }

    public void clearHistory() {
        indexToMessageMap.clear();
        messageHistory.clear();
        historyLinesTotal = 0;
        displayFromLineIndex = 0;
        displayToLineIndex = displayEntries - 1;
        currentUpDownIndex = 0;
    }

    public void onKeyTyped(char character) {
        if(character == '\n') {
            if(!consoleLineIsEmpty()) {
                addConsoleMessage(new ConsoleMessage(fullConsoleLine, true));
            }
        } else if(character == '\b') {
            if(!consoleLineIsEmpty()) {
                if(isTextSelected()) {
                    deleteChar();
                } else if(cursorPos > 0) {
                    fullConsoleLine = fullConsoleLine.substring(0, cursorPos - 1) + getAfterCursor();
                    cursorPos--;
                }
            }
        } else if(character == 127) { // entf
            if(!consoleLineIsEmpty()) {
                if(isTextSelected()) {
                    deleteChar();
                } else if(cursorPos < fullConsoleLine.length()) {
                    fullConsoleLine = getUntilCursor() + fullConsoleLine.substring(cursorPos + 1);
                }
            }
        } else if(ALLOWED_CHARS.contains(String.valueOf(character))) {
            if(consoleLineIsEmpty()) {
                fullConsoleLine = String.valueOf(character);
                cursorPos++;
            } else {
                if(isTextSelected()) {
                    if(isEntireTextSelected()) {
                        fullConsoleLine = String.valueOf(character);
                        cursorPos = 1;
                    } else {
                        fullConsoleLine = getUntilSelectionA() + character + getAfterSelectionB();
                        cursorPos = selectionA + 1;
                    }
                    resetSelection();
                    currentUpDownIndex = messageHistory.size();
                } else {
                    fullConsoleLine = getUntilCursor() + character + getAfterCursor();
                    cursorPos++;
                }
            }
        }
    }

    private void onShiftLeft() {
        if(cursorPos > 0) {
            cursorPos--;

            if(selectionA == 0 && selectionB > 0) {
                selectionB = cursorPos;
            } else {
                selectionA = cursorPos;
                if(selectionB == 0) {
                    selectionB = cursorPos + 1;
                }
            }
        }
    }

    private void onShiftRight() {
        if(cursorPos < fullConsoleLine.length()) {
            cursorPos++;

            if(selectionA == 0 && selectionB > 0) {
                selectionB = cursorPos;
            } else {
                selectionB = cursorPos;
                if(selectionA == 0) {
                    selectionA = cursorPos - 1;
                }
            }
        }
    }

    private void calculateVisibleConsoleLine() {
        int width = calculatePredictedTextWidth(fullConsoleLine);
        boolean requiresViewport = width > CONSOLE_COMMAND_TARGET_WIDTH;

        if(requiresViewport) {
            boolean hasFit = false;

            if(cursorPos == fullConsoleLine.length()) {
                // Start from end and go to begin
                int beginIndex = fullConsoleLine.length() - 1;
                String useToDisplay = "";

                while(!hasFit) {
                    String checkFor = fullConsoleLine.substring(beginIndex);
                    int currentWidth = calculatePredictedTextWidth(checkFor);

                    if(currentWidth < CONSOLE_COMMAND_TARGET_WIDTH) {
                        useToDisplay = checkFor;
                        beginIndex--;
                    } else {
                        hasFit = true;
                    }
                }

                displayedConsoleLine = useToDisplay;
                viewportA = beginIndex + 1;
                viewportB = fullConsoleLine.length();
            } else if(cursorPos == 0) {
                // Start from begin and go to end
                int beginIndex = 1;
                String useToDisplay = "";

                while(!hasFit) {
                    String checkFor = fullConsoleLine.substring(0, beginIndex);
                    int currentWidth = calculatePredictedTextWidth(checkFor);

                    if(currentWidth < CONSOLE_COMMAND_TARGET_WIDTH) {
                        useToDisplay = checkFor;
                        beginIndex++;
                    } else {
                        hasFit = true;
                    }
                }

                displayedConsoleLine = useToDisplay;
                viewportA = 0;
                viewportB = displayedConsoleLine.length();
            } else {
                // cursorPos > begin && cursorPos < end
                if(cursorPos > lastCursorPos && (cursorPos > viewportB || !lastFullConsoleLine.equals(fullConsoleLine))) {
                    // Went to the right
                    int endIndex;
                    int beginIndex;
                    String useToDisplay = "";

                    if(cursorPos >= viewportB) {
                        endIndex = cursorPos;
                        beginIndex = endIndex - 1;
                    } else {
                        beginIndex = viewportA;
                        endIndex = beginIndex + 1;
                    }

                    while(!hasFit) {
                        String checkFor = fullConsoleLine.substring(beginIndex, endIndex);
                        int currentWidth = calculatePredictedTextWidth(checkFor);

                        if(currentWidth < CONSOLE_COMMAND_TARGET_WIDTH) {
                            useToDisplay = checkFor;

                            if(cursorPos >= viewportB) {
                                beginIndex--;
                            } else {
                                endIndex++;
                            }
                        } else {
                            hasFit = true;
                        }
                    }

                    displayedConsoleLine = useToDisplay;
                    viewportA = beginIndex;
                    viewportB = endIndex;
                } else if(cursorPos < lastCursorPos && (cursorPos < viewportA || !lastFullConsoleLine.equals(fullConsoleLine))) {
                    // Went to the left
                    int endIndex;
                    int beginIndex;
                    String useToDisplay = "";

                    beginIndex = Math.min(cursorPos, viewportA);
                    endIndex = beginIndex + 1;

                    while(!hasFit) {
                        String checkFor = fullConsoleLine.substring(beginIndex, endIndex);
                        int currentWidth = calculatePredictedTextWidth(checkFor);

                        if(currentWidth < CONSOLE_COMMAND_TARGET_WIDTH) {
                            useToDisplay = checkFor;
                            endIndex++;
                        } else {
                            hasFit = true;
                        }
                    }

                    displayedConsoleLine = useToDisplay;
                    viewportA = beginIndex;
                    viewportB = endIndex;
                }
            }
        } else {
            displayedConsoleLine = fullConsoleLine;
            viewportA = 0;
            viewportB = displayedConsoleLine.length();
        }
    }

    private void updatePlayerConsoleLine() {
        boolean l = isActiveKey(Input.Keys.LEFT);
        boolean r = isActiveKey(Input.Keys.RIGHT);

        if(isActiveKey(Input.Keys.SHIFT_LEFT)) {
            if(l || r) {
                if(lastDirectionRight && l) {
                    lastDirectionRight = false;
                    leftRightDelta = -0.3f;
                } else if(!lastDirectionRight && r) {
                    lastDirectionRight = true;
                    leftRightDelta = -0.3f;
                } else {
                    leftRightDelta += RenderContext.get().delta;

                    if(leftRightDelta >= 0.05f) {
                        leftRightDelta -= 0.05f;

                        if(l) {
                            onShiftLeft();
                        } else {
                            onShiftRight();
                        }
                    }
                }
            } else {
                leftRightDelta = -0.3f;
            }
        } else {
            if(l || r) {
                resetSelection();
                leftRightDelta += RenderContext.get().delta;

                if(leftRightDelta >= 0.05f) {
                    leftRightDelta -= 0.05f;

                    if(l) {
                        cursorPos--;
                        if(cursorPos < 0) cursorPos = 0;
                    } else {
                        cursorPos++;
                        if(cursorPos > fullConsoleLine.length()) cursorPos = fullConsoleLine.length();
                    }
                }
            } else {
                leftRightDelta = -0.3f;
            }
        }

        boolean update = (lastSelectionA != selectionA) || (lastSelectionB != selectionB) || (lastCursorPos != cursorPos) || (!lastFullConsoleLine.equals(fullConsoleLine));

        if(update) {
            // Calculate viewport of console line text
            calculateVisibleConsoleLine();

            // Calculate indicator position
            int offsetIndicator = cursorPos - viewportA;
            if(offsetIndicator > displayedConsoleLine.length()) offsetIndicator = displayedConsoleLine.length();
            indicatorDisplacement = calculatePredictedTextWidth(displayedConsoleLine.substring(0, offsetIndicator));

            // Calculate selection viewport
            if(selectionA != 0 || selectionB != 0) {
                if(!(selectionA > viewportA && selectionB > viewportB)) {
                    int selectionStart = selectionA;
                    if(selectionStart < viewportA) selectionStart = viewportA;
                    int selectionEnd = selectionB;
                    if(selectionEnd > viewportB) selectionEnd = viewportB;

                    calcGlyph.setText(consoleFontNoMarkup, fullConsoleLine.substring(selectionStart, selectionEnd));
                    selectionWidth = (int) calcGlyph.width;
                    selectionHeight = (int) calcGlyph.height;

                    if(selectionStart > 0) {
                        calcGlyph.setText(consoleFontNoMarkup, displayedConsoleLine.substring(0, selectionStart - viewportA));
                        selectionOffset = (int) calcGlyph.width;
                    } else {
                        selectionOffset = 0;
                    }
                } else {
                    selectionOffset = 0;
                    selectionWidth = 0;
                    selectionHeight = 0;
                }
            }

            lastSelectionA = selectionA;
            lastSelectionB = selectionB;
            lastCursorPos = cursorPos;
            lastFullConsoleLine = fullConsoleLine;
        }
    }

    private boolean isTextSelected() {
        return selectionA != selectionB;
    }

    private boolean isEntireTextSelected() {
        return selectionA == 0 && selectionB == fullConsoleLine.length();
    }

    public void onScroll(float amountY) {
        if(historyLinesTotal > displayEntries) {
            int amount = (int) -amountY;

            if(amount > 0) {
                int newCap = displayToLineIndex + 1;

                if(newCap < historyLinesTotal) {
                    displayFromLineIndex++;
                    displayToLineIndex++;
                }
            } else {
                int newFloor = displayFromLineIndex - 1;

                if(newFloor >= 0) {
                    displayFromLineIndex--;
                    displayToLineIndex--;
                }
            }
        }
    }

    public void draw() {
        if(isVisible()) {
            synchronized (CONSOLE_LOCK) {
                if(queuedResize != null) {
                    CONSOLE_TOTAL_WIDTH = queuedResize[0];
                    CONSOLE_TOTAL_HEIGHT = queuedResize[1];
                    CONSOLE_HISTORY_TARGET_WIDTH = queuedResize[2];
                    CONSOLE_COMMAND_TARGET_WIDTH = queuedResize[3];
                    displayEntries = queuedResize[4];
                    displayFromLineIndex = 0;
                    displayToLineIndex = displayEntries - 1;

                    updateAllMessages();

                    queuedResize = null;
                }

                updatePlayerConsoleLine();

                int rectStartX = 16 + dragX;
                int rectStartY = 16 + dragY;
                int textStartX = 32 + dragX;
                int textStartY = 42 + dragY;

                // Draw console borders
                shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
                shapeRenderer.setColor(COLOR_OUTER);
                shapeRenderer.rect(rectStartX, rectStartY, CONSOLE_TOTAL_WIDTH, CONSOLE_TOTAL_HEIGHT);

                shapeRenderer.setColor(COLOR_BASE);
                shapeRenderer.rect(rectStartX + 1, rectStartY + 1, CONSOLE_TOTAL_WIDTH - 2, CONSOLE_TOTAL_HEIGHT - 2);

                shapeRenderer.setColor(COLOR_INNER);
                shapeRenderer.rect(rectStartX + 8, rectStartY + 8, CONSOLE_TOTAL_WIDTH - 16, 28);

                shapeRenderer.setColor(COLOR_OUTER);
                shapeRenderer.rect(rectStartX + 9, rectStartY + 9, CONSOLE_TOTAL_WIDTH - 18, 26);
                shapeRenderer.end();

                // Draw console line selection
                if(isTextSelected()) {
                    shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
                    shapeRenderer.setColor(COLOR_SELECTION);
                    shapeRenderer.rect(textStartX + selectionOffset - 1, textStartY - selectionHeight - 1, selectionWidth + 2, selectionHeight + 2);
                    shapeRenderer.end();
                }

                // Draw console line text
                batch.begin();
                consoleFontNoMarkup.draw(batch, displayedConsoleLine, textStartX, textStartY);
                batch.end();

                // Draw indicator
                shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
                shapeRenderer.setColor(Color.LIGHT_GRAY);
                shapeRenderer.rect(textStartX + indicatorDisplacement, textStartY - 12f, 1, 15);
                shapeRenderer.rect(textStartX + indicatorDisplacement - 1, textStartY - 12f, 3, 1);
                shapeRenderer.rect(textStartX + indicatorDisplacement - 1, textStartY + 3f, 3, 1);
                shapeRenderer.end();

                // Draw console history
                if(messageHistory.size() > 0) {
                    batch.begin();

                    int yOffset = 0;
                    int linesDisplayed = 0;

                    for (int i = 0; i < displayEntries; i++) {
                        if (linesDisplayed >= displayEntries) break;
                        if (linesDisplayed >= historyLinesTotal) break;

                        int currentIndex = historyLinesTotal - 1 - displayFromLineIndex - i;
                        ConsoleMessage entry = indexToMessageMap.get(currentIndex);

                        // Console message timer
                        historyGlyph.setText(consoleFont, "[" + entry.timeStr + "] ");
                        float timerWidth = historyGlyph.width;
                        float timerHeight = historyGlyph.height;

                        if(entry.lines == 1) {
                            // Single liner, easy handling.
                            yOffset += historyGlyph.height;
                            linesDisplayed += 1;

                            // Draw console message timer
                            consoleFont.draw(batch, "[GRAY][" + entry.timeStr + "] ", textStartX, textStartY + 29 + yOffset);

                            // Draw console message body
                            consoleFont.draw(batch, entry.message, textStartX + timerWidth, textStartY + 29 + yOffset);
                            yOffset += 7;
                        } else {
                            // Multi line, could possibly be cut up on top/bottom.
                            int topCap = historyLinesTotal - 1 - displayFromLineIndex;
                            int bottomCap = topCap - displayEntries + 1;

                            boolean entirelyVisible = entry.endIndex <= topCap && entry.startIndex >= bottomCap;

                            if (entirelyVisible) {
                                historyGlyph.setText(consoleFont, entry.message, Color.WHITE, CONSOLE_HISTORY_TARGET_WIDTH, Align.left, true);
                                float bodyHeight = historyGlyph.height;
                                yOffset += bodyHeight;
                                linesDisplayed += entry.lines;

                                // Draw console message timer
                                consoleFont.draw(batch, "[GRAY][" + entry.timeStr + "] ", textStartX, textStartY + 29 + yOffset);

                                // Draw console message body
                                consoleFont.draw(batch, entry.message, textStartX + timerWidth, textStartY + 29 + yOffset, CONSOLE_HISTORY_TARGET_WIDTH, Align.left, true);
                                i += entry.lines - 1;
                                yOffset += 7;
                            } else {
                                if (entry.endIndex > topCap && entry.startIndex < bottomCap) {
                                    // Bottom line(s) and top line(s) are cut off.
                                    int h1 = (entry.endIndex - topCap);
                                    int h2 = (bottomCap - entry.startIndex);
                                    int hidden = h1 + h2;
                                    int toShow = entry.lines - hidden;

                                    for (int x = 0; x < toShow; x++) {
                                        yOffset += timerHeight;
                                        consoleFont.draw(batch, entry.strLines[x + h1], textStartX + timerWidth, textStartY + 29 + yOffset);
                                        yOffset += 7;
                                    }

                                    linesDisplayed += toShow;
                                    i += toShow;
                                } else if (entry.endIndex > topCap) {
                                    // Bottom line(s) is/are cut off.
                                    int hidden = entry.endIndex - topCap;
                                    int toShow = entry.lines - hidden;

                                    for (int x = 0; x < toShow; x++) {
                                        yOffset += timerHeight;
                                        if (x == toShow - 1) {
                                            // Draw console message timer
                                            consoleFont.draw(batch, "[GRAY][" + entry.timeStr + "] ", textStartX, textStartY + 29 + yOffset);
                                        }
                                        consoleFont.draw(batch, entry.strLines[hidden + x], textStartX + timerWidth, textStartY + 29 + yOffset);
                                        yOffset += 7;
                                    }

                                    linesDisplayed += toShow - 1;
                                    i += toShow - 1;
                                } else {
                                    // Top line(s) is/are cut off.
                                    int hidden = bottomCap - entry.startIndex;
                                    int toShow = entry.lines - hidden;

                                    for (int x = 0; x < toShow; x++) {
                                        yOffset += timerHeight;
                                        consoleFont.draw(batch, entry.strLines[x], textStartX + timerWidth, textStartY + 29 + yOffset);
                                        yOffset += 7;
                                    }

                                    linesDisplayed += toShow;
                                    i += toShow;
                                }
                            }
                        }
                    }

                    batch.end();
                }
            }
        }
    }

    public void setDrag(int x, int y) {
        dragX = lastDragX + x;
        dragY = lastDragY - y;
    }

    public void setLastDrag(int x, int y) {
        lastDragX = x;
        lastDragY = y;
    }

    public void handleTouchDown(int screenX, int screenY) {
        lastClickX = screenX;
        lastClickY = screenY;
    }

    public void handleTouchUp() {
        setLastDrag(dragX, dragY);
    }

    public void handleDragged(int screenX, int screenY) {
        int dx = screenX - lastClickX;
        int dy = screenY - lastClickY;
        setDrag(dx, dy);
    }

    public void dispose() {
        shapeRenderer.dispose();
        batch.dispose();
        consoleFont.dispose();
    }

    public boolean isVisible() {
        return visible;
    }

    public void toggleVisibility() {
        visible = !visible;
    }

    public CommandResolver getResolver() {
        return resolver;
    }

    public void resize(int width, int height) {
        log("Resizing GameConsole to " + width + "x" + height);

        queuedResize = new int[5];
        queuedResize[0] = width;                                    // CONSOLE_TOTAL_WIDTH
        queuedResize[1] = height;                                   // CONSOLE_TOTAL_HEIGHT
        queuedResize[2] = queuedResize[0] - 105;                    // CONSOLE_HISTORY_TARGET_WIDTH
        queuedResize[3] = queuedResize[0] - 32;                     // CONSOLE_COMMAND_TARGET_WIDTH
        float calcLines = (queuedResize[1] - 66) / 14.0f;
        queuedResize[4] = (int) (calcLines);                        // displayEntries
    }

    public int[] resizeLines(int lines) {
        log("Resizing GameConsole to " + lines + " line(s)");
        int height = 66 + lines * 14;
        resize(CONSOLE_TOTAL_WIDTH, height);
        return new int[] {CONSOLE_TOTAL_HEIGHT, height};
    }

    // global singleton
    private static GameConsole INSTANCE;

    public static GameConsole get() {
        if(INSTANCE == null) {
            INSTANCE = new GameConsole();
        }

        return INSTANCE;
    }

    public void updateBatches(Matrix4 uiMatrix) {
        shapeRenderer.setProjectionMatrix(uiMatrix);
        batch.setProjectionMatrix(uiMatrix);
    }

}