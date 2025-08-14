package org.mobitti.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import com.google.genai.types.ThinkingConfig;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mobitti.controllers.AiController;
import org.mobitti.dtos.AppUserDto;
import org.mobitti.dtos.ClientChatMessage;
import org.mobitti.dtos.MobittiChatMessage;
import org.mobitti.dtos.MobittiChatResponse;
import org.mobitti.helpers.ContextItem;
import org.mobitti.helpers.DateTimeUtils;
import org.mobitti.helpers.SystemPrompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class MobittiChatService {

    @Autowired
    private GeminiRagService geminiRagService;

    @Autowired
    private GenAiClient genAiClient;

    @Autowired
    private SystemPrompt systemPrompt;

    private static ObjectMapper mapper = new ObjectMapper();
    public  static SimpleDateFormat dateTimeDbFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    private final Map<String,CircularFifoQueue<MobittiChatMessage>> chatMemory = new HashMap<>();
    private static final Logger logger = LogManager.getLogger(MobittiChatService.class);

    public void chat(AppUserDto appUserDto, ClientChatMessage mobittiChatMessage,MobittiChatResponse res) throws IOException, InterruptedException {
        long requestStart = System.currentTimeMillis();
        String requestId = generateRequestId();
        String userKey = appUserDto.getClubId() + "_" + appUserDto.getUserId();


        logger.info("CHAT_REQUEST_START - RequestId: {} User: {} Club: {} Role: {} InputLength: {}",
                requestId, appUserDto.getUserId(), appUserDto.getClubId(),
                appUserDto.getRoleName(), mobittiChatMessage.getMessage().length());


        try {
            String dateTime = DateTimeUtils.getCurrentDateTimeDb();
            long ragStart = System.currentTimeMillis();
            logger.info("RAG_START - RequestId: {} StorageId: {} Query: '{}'",
                    requestId, appUserDto.getStorageId(), truncateMessage(mobittiChatMessage.getMessage(), 50));

            String rawJson = geminiRagService.generate(mobittiChatMessage.getMessage(), appUserDto.getStorageId());
            long ragEnd = System.currentTimeMillis();
            logger.info("RAG_COMPLETE - RequestId: {} Duration: {}ms JsonLength: {}",
                    requestId, (ragEnd - ragStart), rawJson.length());
            long contextStart = System.currentTimeMillis();
            String context = extractRagContext(rawJson);
            long contextEnd = System.currentTimeMillis();
            if (context.isEmpty()) {
                logger.warn("RAG_NO_CONTEXT - RequestId: {} Query: '{}'",
                        requestId, truncateMessage(mobittiChatMessage.getMessage(), 50));
            } else {
                logger.info("RAG_CONTEXT_EXTRACTED - RequestId: {} ContextLength: {} Duration: {}ms",
                        requestId, context.length(), (contextEnd - contextStart));
            }
            // === GEMINI CLIENT & CONFIG ===
            logger.debug("GEMINI_INIT - RequestId: {} SuperClubId: {}", requestId, appUserDto.getSuperClubId());

            Client client = genAiClient.getClient(appUserDto.getSuperClubId());
            GenerateContentConfig config = GenerateContentConfig.builder()
                    .temperature(1F)
                    .topP(0.9F)
                    .maxOutputTokens(1024)
                    .thinkingConfig(ThinkingConfig.builder().includeThoughts(true).thinkingBudget(512).build())
                    .build();
            String chatHistory = getChatHistory(appUserDto);
            String fullPrompt = systemPrompt.getSystemPrompt() + "\n\n"
                    + "### User Profile: \n"
                    + " USER ID: " + appUserDto.getUserId() + "\n"
                    + " FIRST NAME: " + appUserDto.getFirstName() + "\n"
                    + " LAST NAME: " + appUserDto.getLastName() + "\n"
                    + " Role: " + appUserDto.getRoleName() + "\n"
                    + " CLUB ID: " + appUserDto.getClubId() + "\n"
                    + " DEPARTMENT: " + appUserDto.getDepartment() + "\n"
                    + " BIRTHDAY: " + appUserDto.getBirthday() + "\n"
                    + "### 🗓️ Current Date and Time: " + dateTime + "\n\n"
                    + "### 💬 Chat History:\n" + chatHistory  + "\n\n"
                    + "### 📄 Retrieved Context (from documents):\n" + context + "\n\n"
                    + "### ❓ User Question:\n" + mobittiChatMessage.getMessage();
            logger.info("PROMPT_BUILT - RequestId: {} SystemPromptLen: {} HistoryLen: {} ContextLen: {} TotalLen: {}",
                    requestId, systemPrompt.getSystemPrompt().length(), chatHistory.length(),
                    context.length(), fullPrompt.length());

            // === GEMINI API CALL ===
            long geminiStart = System.currentTimeMillis();
            logger.info("GEMINI_CALL_START - RequestId: {} Model: gemini-2.5-flash-lite EstimatedTokens: {}",
                    requestId, fullPrompt.length() / 4);

            Content fullContent = Content.fromParts(Part.fromText(fullPrompt));
            GenerateContentResponse response = client.models.generateContent("gemini-2.5-flash-lite", fullContent, config);
            long geminiEnd = System.currentTimeMillis();


            // === Token usage tracking ===
            int inputTokens = response.usageMetadata().flatMap(meta -> meta.promptTokenCount()).orElse(0);
            int outputTokens = response.usageMetadata().flatMap(meta -> meta.candidatesTokenCount()).orElse(0);
            int reasoningTokens = response.usageMetadata().flatMap(meta -> meta.thoughtsTokenCount()).orElse(0);
            int totalTokens = response.usageMetadata().flatMap(meta -> meta.totalTokenCount()).orElse(0);
            double cost = (inputTokens / 1000000.0) * 0.10 + ((outputTokens + reasoningTokens) / 1000000.0) * 0.40;

            logger.info("GEMINI_CALL_COMPLETE - RequestId: {} Duration: {}ms InputTokens: {} OutputTokens: {} ThoughtsTokens: {} TotalTokens: {} Cost: ${}",
                    requestId, (geminiEnd - geminiStart), inputTokens, outputTokens, reasoningTokens, totalTokens, String.format("%.6f", cost));

            // === RESPONSE PROCESSING ===
            String responseText = response.text();
            logger.info("RESPONSE_RECEIVED - RequestId: {} ResponseLength: {} Preview: '{}'",
                    requestId, responseText.length(), truncateMessage(responseText, 100));
            // === MEMORY UPDATE ===
            CircularFifoQueue<MobittiChatMessage> userChatMemory = this.chatMemory.getOrDefault(userKey, new CircularFifoQueue<>(10));
            int memoryBefore = userChatMemory.size();
            userChatMemory.add(new MobittiChatMessage(dateTime, mobittiChatMessage.getMessage(), MobittiChatMessage.MessageType.User));
            userChatMemory.add(new MobittiChatMessage(dateTime, responseText, MobittiChatMessage.MessageType.Assistant));
            this.chatMemory.put(userKey, userChatMemory);
            logger.debug("MEMORY_UPDATED - RequestId: {} UserKey: {} Before: {} After: {}",
                    requestId, userKey, memoryBefore, userChatMemory.size());
            // === REQUEST COMPLETION ===
            long requestEnd = System.currentTimeMillis();
            long totalDuration = requestEnd - requestStart;

            logger.info("CHAT_REQUEST_SUCCESS - RequestId: {} User: {} TotalDuration: {}ms RAGTime: {}ms GeminiTime: {}ms ResponseLen: {} TokensUsed: {} Cost: ${}",
                    requestId, appUserDto.getUserId(), totalDuration, (ragEnd - ragStart), (geminiEnd - geminiStart),
                    responseText.length(), totalTokens, String.format("%.6f", cost));

            // Performance warning for slow requests
            if (totalDuration > 5000) {
                logger.warn("SLOW_REQUEST - RequestId: {} Duration: {}ms User: {} InputLen: {} OutputLen: {}",
                        requestId, totalDuration, appUserDto.getUserId(),
                        mobittiChatMessage.getMessage().length(), responseText.length());
            }

            res.setData(response.text());

        }catch (Exception e) {
            long requestEnd = System.currentTimeMillis();
            long totalDuration = requestEnd - requestStart;

            logger.error("CHAT_REQUEST_FAILED - RequestId: {} User: {} Duration: {}ms ErrorType: {} Message: {} InputLen: {}",
                    requestId, appUserDto.getUserId(), totalDuration, e.getClass().getSimpleName(),
                    e.getMessage(), mobittiChatMessage.getMessage().length(), e);

            throw e;
        }
    }


    private String getChatHistory(AppUserDto appUserDto) {
        String userKey = appUserDto.getClubId() + "_" + appUserDto.getUserId();

        StringBuilder chatHistory = new StringBuilder();
        CircularFifoQueue<MobittiChatMessage> userChatMemory = this.chatMemory.getOrDefault(appUserDto.getClubId() +"_"+appUserDto.getUserId(),
                new CircularFifoQueue<>(10));
        logger.debug("HISTORY_RETRIEVE - UserKey: {} HistorySize: {}", userKey, userChatMemory.size());

        for (MobittiChatMessage message : userChatMemory) {
            chatHistory.append("["+message.getDateTime() +"] "+ message.getMessageType()).append(": ").append(message.getMessage()).append("\n");
        }
        return chatHistory.toString();
    }


    public String extractRagContext(String jsonArrayText) {
        long extractStart = System.currentTimeMillis();

        try {
            logger.debug("CONTEXT_EXTRACT_START - JsonLength: {}", jsonArrayText.length());

            JsonNode root = mapper.readTree(jsonArrayText);

            // Normalize to array
            ArrayNode items = root.isArray() ? (ArrayNode) root : mapper.createArrayNode().add(root);

            // LinkedHashMap preserves insertion order & allows dedup by key
            Map<String, ContextItem> dedup = new LinkedHashMap<>();

            for (JsonNode item : items) {
                JsonNode candidates = item.path("candidates");
                if (!candidates.isArray()) continue;

                for (JsonNode candidate : candidates) {
                    JsonNode grounding = candidate.path("groundingMetadata");
                    JsonNode chunks = grounding.path("groundingChunks");

                    // Build order using groundingSupports if available
                    List<Integer> orderedIndices = new ArrayList<>();
                    JsonNode supports = grounding.path("groundingSupports");
                    if (supports.isArray() && supports.size() > 0) {
                        // Collect (idx, confidence) for sorting by confidence desc while preserving original order on ties
                        List<int[]> idxWithConf = new ArrayList<>();
                        for (JsonNode s : supports) {
                            JsonNode idxArr = s.path("groundingChunkIndices");
                            JsonNode confArr = s.path("confidenceScores");
                            if (idxArr.isArray()) {
                                for (int i = 0; i < idxArr.size(); i++) {
                                    int idx = idxArr.get(i).asInt();
                                    double conf = (confArr.isArray() && i < confArr.size()) ? confArr.get(i).asDouble() : 0.0;
                                    // Optional threshold: if (conf < 0.5) continue;
                                    idxWithConf.add(new int[]{idx, (int) Math.round(conf * 1_000_000)}); // keep sortable
                                }
                            }
                        }
                        // sort by confidence desc, stable for same idx
                        idxWithConf.sort((a,b) -> Integer.compare(b[1], a[1]));
                        for (int[] pair : idxWithConf) orderedIndices.add(pair[0]);
                    } else if (chunks.isArray()) {
                        // Fallback: natural order
                        for (int i = 0; i < chunks.size(); i++) orderedIndices.add(i);
                    }

                    if (!chunks.isArray()) continue;

                    // Take top-K to avoid bloat
                    final int TOP_K = 5;
                    int taken = 0;

                    for (Integer idx : orderedIndices) {
                        if (idx < 0 || idx >= chunks.size()) continue;
                        JsonNode chunk = chunks.get(idx);

                        JsonNode retrieved = chunk.path("retrievedContext");
                        String text = optText(retrieved.at("/ragChunk/text"));
                        if (text.isEmpty()) text = optText(retrieved.path("text"));
                        if (text.isEmpty()) continue;

                        String title = optText(retrieved.path("title"));
                        String uri   = optText(retrieved.path("uri"));
                        String page  = "";
                        JsonNode span = retrieved.path("ragChunk").path("pageSpan");
                        if (span.isObject()) {
                            String first = optText(span.path("firstPage"));
                            String last  = optText(span.path("lastPage"));
                            if (!first.isEmpty()) {
                                page = first.equals(last) ? ("p." + first) : ("pp." + first + "-" + last);
                            }
                        }

                        // Dedup key: uri + page + normalized text
                        String key = (uri + "|" + page + "|" + normalize(text));
                        dedup.putIfAbsent(key, new ContextItem(title, uri, page, text));

                        if (++taken >= TOP_K) break;
                    }
                }
            }

            long extractEnd = System.currentTimeMillis();

            logger.debug("CONTEXT_EXTRACT_COMPLETE - UniqueContexts: {} Duration: {}ms",
                     dedup.size(), (extractEnd - extractStart));


            // If nothing was retrieved, DO NOT pull model text from content.parts as "context".
            if (dedup.isEmpty()) {
                logger.warn("CONTEXT_EXTRACT_EMPTY ");

                return ""; // or return a special marker you can check upstream
            }

            // Build formatted context with
            StringBuilder out = new StringBuilder();
            for (ContextItem item : dedup.values()) {
                String header = "SOURCE: " + (item.getTitle().isEmpty() ? "Untitled" : item.getTitle())
                        + (item.getPage().isEmpty() ? "" : " (" + item.getPage() + ")")
                        + (item.getUri().isEmpty() ? "" : "\nURI: " + item.getUri())
                        + "\n";
                String block = header + item.getText().trim() + "\n\n";
                out.append(block);
            }
            String finalContext = out.toString().trim();
            logger.info("CONTEXT_EXTRACT_SUCCESS - ContextItems: {} FinalLength: {} Duration: {}ms",
                    dedup.size(), finalContext.length(), (extractEnd - extractStart));

            return finalContext;

        } catch (Exception e) {
            long extractEnd = System.currentTimeMillis();
            logger.error("CONTEXT_EXTRACT_FAILED - Duration: {}ms InputLength: {} Error: {}",
                    (extractEnd - extractStart), jsonArrayText.length(), e.getMessage(), e);
            return "Failed to extract RAG context: " + e.getMessage();
        }
    }
    private static String optText(JsonNode node) {
        return (node != null && node.isTextual()) ? node.asText() : "";
    }

    private static String normalize(String s) {
        // Minimal normalization for dedup; keep RTL chars intact
        return s.replaceAll("\\s+", " ").trim();
    }

    private String generateRequestId() {
        return "REQ_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
    }

    private String truncateMessage(String message, int maxLength) {
        if (message == null) return "null";
        if (message.length() <= maxLength) return message;
        return message.substring(0, maxLength) + "...";
    }
}
