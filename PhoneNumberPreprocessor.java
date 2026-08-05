package com.github.olga_yakovleva.rhvoice.android;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * RHVoice sintez qilishdan oldin matndagi raqamlarni o'qishga qulay
 * guruhlarga ajratadi. Har doim yoniq - o'chirib bo'lmaydi:
 *
 *   +998949835707      ->  +998 94 983 57 07        (telefon raqami)
 *   998941066607       ->  998 94 106 66 07         (+ siz davlat kodi)
 *   949835707          ->  94 983 57 07             (mahalliy 9 xonali)
 *   9860082546068113   ->  98 60 08 25 46 06 81 13  (faqat juft 16+ xonali)
 *
 * 4–14 xonali va toq uzunlikdagi raqamlar endi guruhlanmaydi.
 * Umumiy qoida faqat 16 va undan uzun JUFT raqamlar uchun:
 * boshidan boshlab ikkitadan guruhlanadi.
 */
final class PhoneNumberPreprocessor {

    // +998 bilan boshlangan telefon raqami
    private static final Pattern INTL_PATTERN =
        Pattern.compile("\\+998(\\d{2})(\\d{3})(\\d{2})(\\d{2})\\b");

    // 998 bilan boshlangan, + bo'lmagan 12 xonali (davlat kodi + 9 xona)
    private static final Pattern INTL_NO_PLUS_PATTERN =
        Pattern.compile("(?<!\\d)998(\\d{2})(\\d{3})(\\d{2})(\\d{2})(?!\\d)");

    // Mahalliy 9 xonali telefon raqami
    private static final Pattern LOCAL_9_PATTERN =
        Pattern.compile("(?<!\\d)(\\d{2})(\\d{3})(\\d{2})(\\d{2})(?!\\d)");

    // Faqat 16 va undan uzun JUFT raqam ketma-ketligi (toq va 4–14 olib tashlandi)
    private static final Pattern GENERIC_DIGITS_PATTERN =
        Pattern.compile("\\d{16,}");

    private PhoneNumberPreprocessor() {
    }

    static String process(String text) {
        if (text == null || text.isEmpty())
            return text;

        String result = applyGroups(text, INTL_PATTERN, "+998 ", 4);
        result = applyGroups(result, INTL_NO_PLUS_PATTERN, "998 ", 4);
        result = applyLocal9(result);
        result = applyGenericPairing(result);
        return result;
    }

    private static String applyGroups(String text, Pattern pattern, String prefix, int groupCount) {
        Matcher matcher = pattern.matcher(text);
        StringBuilder sb = new StringBuilder();
        int lastEnd = 0;
        while (matcher.find()) {
            sb.append(text, lastEnd, matcher.start());
            sb.append(prefix);
            for (int i = 1; i <= groupCount; i++) {
                sb.append(matcher.group(i));
                if (i < groupCount)
                    sb.append(' ');
            }
            lastEnd = matcher.end();
        }
        sb.append(text.substring(lastEnd));
        return sb.toString();
    }

    private static String applyLocal9(String text) {
        Matcher matcher = LOCAL_9_PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder();
        int lastEnd = 0;
        while (matcher.find()) {
            int start = matcher.start();
            // Allaqachon +998 yoki 998 bilan formatlangan bo'lsa o'tkazib yuboramiz
            if ((start >= 5 && text.startsWith("+998 ", start - 5)) ||
                (start >= 4 && text.startsWith("998 ", start - 4))) {
                sb.append(text, lastEnd, matcher.end());
                lastEnd = matcher.end();
                continue;
            }
            sb.append(text, lastEnd, start);
            sb.append(matcher.group(1)).append(' ')
              .append(matcher.group(2)).append(' ')
              .append(matcher.group(3)).append(' ')
              .append(matcher.group(4));
            lastEnd = matcher.end();
        }
        sb.append(text.substring(lastEnd));
        return sb.toString();
    }

    // Faqat 16+ xonali va JUFT uzunlikdagi raqamlarni ikkitadan guruhlaydi
    private static String applyGenericPairing(String text) {
        Matcher matcher = GENERIC_DIGITS_PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder();
        int lastEnd = 0;
        while (matcher.find()) {
            String digits = matcher.group();
            // Toq uzunlikdagi raqamlarni o'tkazib yuboramiz
            if (digits.length() % 2 != 0) {
                sb.append(text, lastEnd, matcher.end());
                lastEnd = matcher.end();
                continue;
            }
            sb.append(text, lastEnd, matcher.start());
            sb.append(pairify(digits));
            lastEnd = matcher.end();
        }
        sb.append(text.substring(lastEnd));
        return sb.toString();
    }

    private static String pairify(String digits) {
        // Bu yerga faqat juft uzunlikdagi raqamlar keladi
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < digits.length(); i += 2) {
            if (i > 0)
                sb.append(' ');
            sb.append(digits.charAt(i)).append(digits.charAt(i + 1));
        }
        return sb.toString();
    }
}
