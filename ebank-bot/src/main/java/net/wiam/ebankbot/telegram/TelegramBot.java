
package net.wiam.ebankbot.telegram;

import jakarta.annotation.PostConstruct;
import net.wiam.ebankbot.agents.EbankAIAgent;

import org.springframework.ai.content.Media;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.ActionType;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@Component
public class TelegramBot extends TelegramLongPollingBot {

    @Value("${telegram.token}")
    private String telegramBotToken;

    private final EbankAIAgent aiAgent;

    public TelegramBot(EbankAIAgent aiAgent) {
        this.aiAgent = aiAgent;
    }

    @PostConstruct
    public void registerTelegramBot() {
        try {
            TelegramBotsApi api =
                    new TelegramBotsApi(DefaultBotSession.class);
            api.registerBot(this);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getBotUsername() {
        return "TELEGRAM_BOT";
    }

    @Override
    public String getBotToken() {
        return telegramBotToken;
    }

    @Override
    public void onUpdateReceived(Update telegramRequest) {
        try {
            if (!telegramRequest.hasMessage()) {
                return;
            }

            var telegramMessage = telegramRequest.getMessage();
            String messageText = telegramMessage.getText();
            Long chatId = telegramMessage.getChatId();

            List<PhotoSize> photos = telegramMessage.getPhoto();
            List<Media> mediaList = new ArrayList<>();
            String caption = telegramMessage.getCaption();

            if (photos != null && !photos.isEmpty()) {
                if (caption == null) {
                    caption = "What do you see in this image?";
                }

                // Telegram fournit plusieurs tailles de la même photo.
                // On utilise la dernière, généralement la plus grande.
                PhotoSize photo = photos.get(photos.size() - 1);
                String fileId = photo.getFileId();

                GetFile getFile = new GetFile();
                getFile.setFileId(fileId);

                org.telegram.telegrambots.meta.api.objects.File file =
                        execute(getFile);

                String fileUrl =
                        "https://api.telegram.org/file/bot"
                                + getBotToken()
                                + "/"
                                + file.getFilePath();

                URL url = new URL(fileUrl);

                mediaList.add(
                        Media.builder()
                                .id(fileId)
                                .mimeType(MimeTypeUtils.IMAGE_JPEG)
                                .data(new UrlResource(url))
                                .build()
                );
            }

            String query = messageText != null
                    ? messageText
                    : caption;

            if (query == null || query.isBlank()) {
                query = "Please describe this image.";
            }

            UserMessage userMessage = UserMessage.builder()
                    .text(query)
                    .media(mediaList)
                    .build();

            sendTypingQuestion(chatId);

            String answer = aiAgent.chat(new Prompt(userMessage));
//            String answer = aiAgent.chat(query);

            sendTextMessage(chatId, answer);

        } catch (Exception e) {
            throw new RuntimeException("Erreur lors du traitement du message Telegram", e);
        }
    }

    private void sendTextMessage(Long chatId, String text)
            throws TelegramApiException {
        SendMessage sendMessage =
                new SendMessage(String.valueOf(chatId), text);
        execute(sendMessage);
    }

    private void sendTypingQuestion(Long chatId)
            throws TelegramApiException {
        SendChatAction sendChatAction = new SendChatAction();
        sendChatAction.setChatId(String.valueOf(chatId));
        sendChatAction.setAction(ActionType.TYPING);
        execute(sendChatAction);
    }
}
