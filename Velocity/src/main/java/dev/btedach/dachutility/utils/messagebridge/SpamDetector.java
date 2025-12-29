package dev.btedach.dachutility.utils.messagebridge;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

public class SpamDetector {
    private static final int MAX_REPETITION_THRESHOLD = 15;
    private static final int MAX_CONSECUTIVE_THRESHOLD = 10;
    private static final double MAX_CAPITAL_LETTERS_THRESHOLD_PERCENTAGE = 0.3;

    private static final int MAX_NUMBERS_THRESHOLD_PERCENTAGE = 2;

    private static final int MAX_MESSAGES_PER_PLAYER = 5;  // Adjust the limit as needed
    private static final int MESSAGE_HISTORY_SIZE = 2;     // Adjust the history size as needed

    @Getter
    private static Map<String, Integer> messageCountByPlayer = new HashMap<>();
    private static final Map<String, String[]> messageHistoryByPlayer = new HashMap<>();


    public static SpamType isSpam(String message, String player) {
        String normalizedMessage = message.toUpperCase();
        boolean hasExcessiveRepetition = checkForExcessiveRepetition(normalizedMessage);
        boolean hasExcessiveConsecutive = checkForExcessiveConsecutive(normalizedMessage);
        boolean hasRandomCharacterSpam = checkForRandomCharacterSpam(normalizedMessage);
        boolean hasCapitalLetterSpam = checkForCapitalLetterSpam(message);
        boolean hasNumberSpam = checkForNumberSpam(normalizedMessage);
        boolean hasNumberLengthSpam = checkForNumberLengthSpam(normalizedMessage);
        // Store the message in the history for the player
        if (!messageHistoryByPlayer.containsKey(player)) {
            messageHistoryByPlayer.put(player, new String[MESSAGE_HISTORY_SIZE]);
        }
        String[] history = messageHistoryByPlayer.get(player);
        System.arraycopy(history, 0, history, 1, history.length - 1);
        history[0] = message;
        // Check if the player has sent the same message multiple times recently
        for (int i = 1; i < history.length; i++) {
            if (history[i] != null && history[i].equals(message)) {
                if(message.equalsIgnoreCase("hi")||message.equalsIgnoreCase("hallo")||message.equalsIgnoreCase("moin")||message.equalsIgnoreCase("hey")||
                        message.equalsIgnoreCase("ok")||message.equalsIgnoreCase("okay")||message.equalsIgnoreCase("ja")||message.equalsIgnoreCase("nein")||
                        message.equalsIgnoreCase("hey!")||message.equalsIgnoreCase("heyy")||message.equalsIgnoreCase("heyy!")){
                    continue;
                }
                return SpamType.REPEATED_MESSAGE;  // Block the message as spam
            }
        }

        if(hasExcessiveRepetition){
            return SpamType.EXCESSIVE_REPETITION;
        }else if(hasExcessiveConsecutive){
            return SpamType.EXCESSIVE_CONSECUTIVE;
        }else if(hasRandomCharacterSpam){
            return SpamType.RANDOM_CHARACTER_SPAM;
        }else if(hasCapitalLetterSpam){
            return SpamType.CAPITAL_LETTER_SPAM;
        }else if(hasNumberSpam){
            return SpamType.NUMBER_SPAM;
        }else if(hasNumberLengthSpam){
            return SpamType.NUMBER_LENGTH_SPAM;
        }else{
            return SpamType.NO_SPAM;
        }
    }

    private static boolean checkForExcessiveRepetition(String message) {
        boolean hasWordRepetition = checkForWordRepetition(message);
        boolean hasLetterRepetition = checkForLetterRepetition(message);
        return hasWordRepetition || hasLetterRepetition;
    }

    private static boolean checkForWordRepetition(String message) {
        String[] words = message.split("\\s+");
        String previousWord = null;
        int consecutiveCount = 0;
        for (String word : words) {
            if (word.equalsIgnoreCase(previousWord)) {
                consecutiveCount++;
                if (consecutiveCount >= MAX_REPETITION_THRESHOLD) {
                    return true;
                }
            } else {
                consecutiveCount = 1;
            }
            previousWord = word;
        }
        return false;
    }

    private static boolean checkForLetterRepetition(String message) {
        char[] chars = message.toCharArray();
        for (int i = 0; i < chars.length - MAX_REPETITION_THRESHOLD; i++) {
            char currentChar = chars[i];
            boolean isRepetition = true;
            for (int j = 1; j <= MAX_REPETITION_THRESHOLD; j++) {
                if (chars[i + j] != currentChar) {
                    isRepetition = false;
                    break;
                }
            }
            if (isRepetition) {
                return true;
            }
        }
        return false;
    }

    private static boolean  checkForCapitalLetterSpam(String message) {
        int totalCharacters = message.length();
        int capitalLetterCount = message.replaceAll("[^A-Z]", "").length();
        double capitalLetterPercentage = (double) capitalLetterCount / totalCharacters;

        if(message.length() <= 6) {
            return false;
        }else if(message.length() <= 32){
            return capitalLetterPercentage > 0.5;
        }else{
            return capitalLetterPercentage > MAX_CAPITAL_LETTERS_THRESHOLD_PERCENTAGE;
        }

    }

    private static boolean checkForNumberSpam(String message) {
        String pattern = "\\b\\d{32,}\\b";
        return message.matches(".*" + pattern + ".*");
    }

    private static boolean checkForNumberLengthSpam(String message) {
        int count = message.length() / MAX_NUMBERS_THRESHOLD_PERCENTAGE;
        if(message.length() <= 16){
            count = 14;
        }else if(message.length() <= 32){
            count = 30;
        }
        String pattern = "[0-9]{" +  count + ",}";
        return message.matches(".*" + pattern + ".*");
    }

    private static boolean checkForRandomCharacterSpam(String message) {
        int totalCharacters = message.length();
        int nonAlphanumericCount = message.replaceAll("[\\p{Alnum} ]", "").length();
        double nonAlphanumericPercentage = (double) nonAlphanumericCount / totalCharacters;

        if(message.length() <= 12){
            return false;
        }else if(message.length() <= 32){
            return nonAlphanumericPercentage > 0.4;
        } else{
            return nonAlphanumericPercentage > 0.2;
        }

    }
    private static boolean checkForExcessiveConsecutive(String message) {
        char[] chars = message.toCharArray();
        int consecutiveCount = 1;
        for (int i = 0; i < chars.length - 1; i++) {
            if (chars[i] == chars[i + 1]) {
                consecutiveCount++;
                if (consecutiveCount >= MAX_CONSECUTIVE_THRESHOLD) {
                    return true;
                }
            } else {
                consecutiveCount = 1;
            }
        }
        return false;
    }

    public enum SpamType{
        NO_SPAM,
        BAD_WORD,
        REPEATED_MESSAGE,
        EXCESSIVE_REPETITION,
        EXCESSIVE_CONSECUTIVE,
        RANDOM_CHARACTER_SPAM,
        CAPITAL_LETTER_SPAM,
        NUMBER_SPAM,
        NUMBER_LENGTH_SPAM;
    }
}
