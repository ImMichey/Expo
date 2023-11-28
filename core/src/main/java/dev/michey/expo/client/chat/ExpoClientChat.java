package dev.michey.expo.client.chat;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Align;
import dev.michey.expo.audio.AudioEngine;
import dev.michey.expo.console.GameConsole;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.ui.PlayerUI;
import dev.michey.expo.util.ClientPackets;
import dev.michey.expo.util.ClientStatic;
import dev.michey.expo.util.Pair;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import static dev.michey.expo.log.ExpoLogger.logch;

public class ExpoClientChat {

    // Parent
    private final PlayerUI ui;
    private float chatUiScale;
    public BitmapFont chatUseFont;
    // Console message history
    private final List<ChatMessage> messageHistory;
    private final String ALLOWED_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789 _-:?!;<>[]#(){}./,&äöüÄÖÜß+=~";
    private final HashMap<Integer, ChatMessage> indexToMessageMap;
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
    private boolean focused;
    private int CHAT_TOTAL_WIDTH;
    private int CHAT_TOTAL_HEIGHT;
    public int CHAT_HISTORY_TARGET_WIDTH;
    private int CHAT_INPUT_TARGET_WIDTH;
    public int drawAtX;
    public int drawAtY;
    private final GlyphLayout historyGlyph;
    private final GlyphLayout calcGlyph;
    private int displayEntries;
    public int displayFromLineIndex = 0;
    public int displayToLineIndex = displayEntries - 1;
    private int[] queuedResize = null;
    // Keys
    private final List<Integer> activeKeyList;
    // Sync lock
    public static final Object CHAT_LOCK = new Object();
    // Colors
    private final Color COLOR_SELECTION = Color.valueOf("0e3666");
    // Player history map
    public final HashMap<String, Pair<String, Pair<Float, Float>>> playerHistoryMap;

    public ExpoClientChat(PlayerUI ui) {
        this.ui = ui;
        messageHistory = new LinkedList<>();
        activeKeyList = new LinkedList<>();
        historyGlyph = new GlyphLayout();
        calcGlyph = new GlyphLayout();
        indexToMessageMap = new HashMap<>();
        playerHistoryMap = new HashMap<>();
        INSTANCE = this;
    }

    public boolean isActiveKey(int keycode) {
        return activeKeyList.contains(keycode);
    }

    private int calculatePredictedTextWidth(String s) {
        calcGlyph.setText(chatUseFont, s);
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
                    if(!ALLOWED_CHARS.contains(c + "")) {
                        converted = converted.replace(c + "", "");
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
            ChatMessage next = findNextConsoleSuggestion(keycode == Input.Keys.UP);

            if(next != null) {
                fullConsoleLine = next.message;
                cursorPos = fullConsoleLine.length();
                resetSelection();
            }
        }
    }

    private ChatMessage findNextConsoleSuggestion(boolean up) {
        if(messageHistory.isEmpty()) return null;
        ChatMessage found = null;

        if(up) {
            while(found == null) {
                currentUpDownIndex--;

                if(currentUpDownIndex < 0) {
                    currentUpDownIndex = 0;
                    break;
                }

                ChatMessage next = messageHistory.get(currentUpDownIndex);

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

                ChatMessage next = messageHistory.get(currentUpDownIndex);

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

    public void addServerMessage(String message) {
        addChatMessage(new ChatMessage(message, "SERVER", false));
    }

    public void addChatMessage(ChatMessage message) {
        if(!message.message.startsWith("/")) {
            AudioEngine.get().playSoundGroup("pop");
        }

        synchronized(CHAT_LOCK) {
            messageHistory.add(message);
            if(message.byUser) {
                fullConsoleLine = "";
                cursorPos = 0;
                resetSelection();
            }
            currentUpDownIndex = messageHistory.size();

            int cover = message.lines;
            int startIndex = historyLinesTotal;
            message.startIndex = startIndex;

            for(int i = 0; i < cover; i++) {
                indexToMessageMap.put(startIndex + i, message);
            }

            historyLinesTotal += message.lines;
            message.endIndex = historyLinesTotal - 1;

            if(!message.sender.equals("SYSTEM")) {
                int msgLength = message.message.length();
                float displayLength = 3.0f + msgLength / 16.0f;
                playerHistoryMap.put(message.sender, new Pair<>(message.message, new Pair<>(displayLength, displayLength)));
            }

            logch(message.message);

            if(message.byUser) {
                ClientPackets.p25ChatMessage(message.message);
            }
        }
    }

    public void updateAllMessages() {
        indexToMessageMap.clear();
        historyLinesTotal = 0;

        for(ChatMessage iteration : messageHistory) {
            int startIndex = historyLinesTotal;

            iteration.generateRenderData();
            int cover = iteration.lines;
            iteration.startIndex = startIndex;

            for(int j = 0; j < cover; j++) {
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
        if(character == '\b') {
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
        boolean requiresViewport = width > CHAT_INPUT_TARGET_WIDTH;

        if(requiresViewport) {
            boolean hasFit = false;

            if(cursorPos == fullConsoleLine.length()) {
                // Start from end and go to begin
                int beginIndex = fullConsoleLine.length() - 1;
                String useToDisplay = "";

                while(!hasFit) {
                    String checkFor = fullConsoleLine.substring(beginIndex);
                    int currentWidth = calculatePredictedTextWidth(checkFor);

                    if(currentWidth < CHAT_INPUT_TARGET_WIDTH) {
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

                    if(currentWidth < CHAT_INPUT_TARGET_WIDTH) {
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

                        if(currentWidth < CHAT_INPUT_TARGET_WIDTH) {
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

                        if(currentWidth < CHAT_INPUT_TARGET_WIDTH) {
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

                    calcGlyph.setText(chatUseFont, fullConsoleLine.substring(selectionStart, selectionEnd));
                    selectionWidth = (int) calcGlyph.width;
                    selectionHeight = (int) calcGlyph.height;

                    if(selectionStart > 0) {
                        calcGlyph.setText(chatUseFont, displayedConsoleLine.substring(0, selectionStart - viewportA));
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
        ShapeRenderer shapeRenderer = GameConsole.get().shapeRenderer;
        SpriteBatch batch = RenderContext.get().hudBatch;

        synchronized(CHAT_LOCK) {
            if(queuedResize != null) {
                CHAT_TOTAL_WIDTH = queuedResize[0];
                CHAT_TOTAL_HEIGHT = queuedResize[1];
                CHAT_HISTORY_TARGET_WIDTH = queuedResize[2];
                CHAT_INPUT_TARGET_WIDTH = queuedResize[3];
                displayEntries = queuedResize[4];
                displayFromLineIndex = 0;
                displayToLineIndex = displayEntries - 1;

                updateAllMessages();
                readjust();

                queuedResize = null;
            }

            updatePlayerConsoleLine();

            int textStartX = (int) (drawAtX + 4 * chatUiScale);
            int textStartY = (int) (drawAtY + 4 * chatUiScale + 7 * chatUiScale);

            // Draw console borders
            Gdx.gl.glEnable(GL30.GL_BLEND);
            Gdx.gl.glBlendFunc(GL30.GL_SRC_ALPHA, GL30.GL_ONE_MINUS_SRC_ALPHA);

            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

            float a1 = 0.50f;
            float a2 = 0.25f;
            if(focused) {
                a1 += 0.25f;
                a2 += 0.25f;
            }
            float tbh = 15 * chatUiScale;

            shapeRenderer.setColor(0f, 0f, 0f, a1);
            shapeRenderer.rect(drawAtX, drawAtY, CHAT_TOTAL_WIDTH, tbh);
            shapeRenderer.setColor(0f, 0f, 0f, a2);
            shapeRenderer.rect(drawAtX, drawAtY + tbh, CHAT_TOTAL_WIDTH, CHAT_TOTAL_HEIGHT - tbh);
            shapeRenderer.setColor(Color.WHITE);
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
            chatUseFont.draw(batch, displayedConsoleLine, textStartX, textStartY);
            batch.end();

            // Draw indicator
            if(focused) {
                shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
                shapeRenderer.setColor(Color.LIGHT_GRAY);
                shapeRenderer.rect(textStartX + indicatorDisplacement, textStartY - 7 * chatUiScale - 2 * chatUiScale, 1, 7 * chatUiScale + 4 * chatUiScale);
                shapeRenderer.rect(textStartX + indicatorDisplacement - 1, textStartY - 7 * chatUiScale - 2 * chatUiScale, 3, 1);
                shapeRenderer.rect(textStartX + indicatorDisplacement - 1, textStartY + 2 * chatUiScale, 3, 1);
                shapeRenderer.end();
            }

            // Draw console history
            if(!messageHistory.isEmpty()) {
                batch.begin();

                float yOffset = 0;
                int linesDisplayed = 0;

                for(int i = 0; i < displayEntries; i++) {
                    if(linesDisplayed >= displayEntries) break;
                    if(linesDisplayed >= historyLinesTotal) break;

                    int currentIndex = historyLinesTotal - 1 - displayFromLineIndex - i;
                    ChatMessage entry = indexToMessageMap.get(currentIndex);

                    // Console message timer
                    String colorPrefix = entry.sender.equals("SERVER") ? "[CYAN]" : entry.byUser ? "[YELLOW]" : "[ORANGE]";
                    historyGlyph.setText(chatUseFont, "[" + entry.sender + "] ");
                    float timerWidth = historyGlyph.width;
                    float timerHeight = historyGlyph.height;
                    //float borderShadowAdjustment = 0;

                    if(entry.lines == 1) {
                        // Single liner, easy handling.
                        yOffset += historyGlyph.height;
                        linesDisplayed += 1;

                        // Draw console message timer
                        chatUseFont.draw(batch, colorPrefix + "[" + entry.sender + "] ", textStartX, (int) (textStartY + 8 * chatUiScale + yOffset));

                        // Draw console message body
                        chatUseFont.draw(batch, entry.message, (int) (textStartX + timerWidth), (int) (textStartY + 8 * chatUiScale + yOffset));
                        yOffset += 4 * chatUiScale;
                    } else {
                        // Multi line, could possibly be cut up on top/bottom.
                        int topCap = historyLinesTotal - 1 - displayFromLineIndex;
                        int bottomCap = topCap - displayEntries + 1;

                        boolean entirelyVisible = entry.endIndex <= topCap && entry.startIndex >= bottomCap;

                        if(entirelyVisible) {
                            historyGlyph.setText(chatUseFont, entry.message, Color.WHITE, CHAT_HISTORY_TARGET_WIDTH - timerWidth, Align.left, true);
                            float bodyHeight = historyGlyph.height;
                            yOffset += bodyHeight;
                            linesDisplayed += entry.lines;

                            // Draw console message timer
                            chatUseFont.draw(batch, colorPrefix + "[" + entry.sender + "] ", textStartX, (int) (textStartY + 8 * chatUiScale + yOffset));

                            // Draw console message body
                            chatUseFont.draw(batch, entry.message, (int) (textStartX + timerWidth), (int) (textStartY + 8 * chatUiScale + yOffset), (int) (CHAT_HISTORY_TARGET_WIDTH - timerWidth), Align.left, true);
                            i += entry.lines - 1;
                            yOffset += (4 * chatUiScale);
                        } else {
                            if (entry.endIndex > topCap && entry.startIndex < bottomCap) {
                                // Bottom line(s) and top line(s) are cut off.
                                int h1 = (entry.endIndex - topCap);
                                int h2 = (bottomCap - entry.startIndex);
                                int hidden = h1 + h2;
                                int toShow = entry.lines - hidden;

                                for (int x = 0; x < toShow; x++) {
                                    yOffset += timerHeight;
                                    chatUseFont.draw(batch, entry.strLines[x + h1], (int) (textStartX + timerWidth), (int) (textStartY + 8 * chatUiScale + yOffset));
                                    yOffset += (4 * chatUiScale);
                                }
                                //yOffset -= (int) borderShadowAdjustment;

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
                                        chatUseFont.draw(batch, colorPrefix + "[" + entry.sender + "] ", textStartX, (int) (textStartY + 8 * chatUiScale + yOffset));
                                    }
                                    chatUseFont.draw(batch, entry.strLines[hidden + x], (int) (textStartX + timerWidth), (int) (textStartY + 8 * chatUiScale + yOffset));
                                    yOffset += (4 * chatUiScale);
                                }

                                linesDisplayed += toShow - 1;
                                i += toShow - 1;
                            } else {
                                // Top line(s) is/are cut off.
                                int hidden = bottomCap - entry.startIndex;
                                int toShow = entry.lines - hidden;

                                for (int x = 0; x < toShow; x++) {
                                    yOffset += timerHeight;
                                    chatUseFont.draw(batch, entry.strLines[x], (int) (textStartX + timerWidth), (int) (textStartY + 8 * chatUiScale + yOffset));
                                    yOffset += (4 * chatUiScale);
                                }
                                //yOffset -= borderShadowAdjustment;

                                linesDisplayed += toShow;
                                i += toShow;
                            }
                        }
                    }
                }

                batch.end();
            }

            /*
            batch.begin();
            ui.drawBorderAt(RenderContext.get(), (int) (drawAtX - 4 * chatUiScale), (int) (drawAtY - 4 * chatUiScale), CHAT_TOTAL_WIDTH - 4 * chatUiScale, CHAT_TOTAL_HEIGHT - 4 * chatUiScale);
            batch.end();
            */
        }
    }

    public boolean isFocused() {
        return focused;
    }

    public void toggleFocus(boolean send) {
        focused = !focused;
        if(!focused && !consoleLineIsEmpty() && send) {
            addChatMessage(new ChatMessage(fullConsoleLine, ClientStatic.PLAYER_USERNAME, true));
        }
    }

    public void readjust() {
        drawAtX = 8;
        drawAtY = Gdx.graphics.getHeight() - 8 - CHAT_TOTAL_HEIGHT;
    }

    public void resize(int width, int height, float chatUiScale) {
        this.chatUiScale = chatUiScale;
        this.chatUseFont = RenderContext.get().m5x7_border_all[(int) chatUiScale - 1];

        queuedResize = new int[5];
        queuedResize[0] = width;                                    // CONSOLE_TOTAL_WIDTH
        queuedResize[1] = height;                                   // CONSOLE_TOTAL_HEIGHT
        queuedResize[2] = (int) (queuedResize[0] - 8 * chatUiScale);
        queuedResize[3] = (int) (queuedResize[0] - 8 * chatUiScale);
        float h = queuedResize[1] - 23 * chatUiScale;
        int lines = 0;
        while(h >= 7 * chatUiScale) {
            h -= 7 * chatUiScale;
            lines++;
            h -= 4 * chatUiScale;
        }
        queuedResize[4] = lines;                      // displayEntries
    }

    // global singleton
    private static ExpoClientChat INSTANCE;

    public static ExpoClientChat get() {
        return INSTANCE;
    }

}