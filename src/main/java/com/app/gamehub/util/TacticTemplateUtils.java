package com.app.gamehub.util;

import com.app.gamehub.entity.GameAccount;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Utilities for replacing rank expressions in tactic template texts with account names.
 */
public class TacticTemplateUtils {
  // matches expressions like "1" or "3-5" or "1,3-5,10"
  private static final Pattern RANK_EXPR = Pattern.compile("(?<!\\d)(\\d+(?:-\\d+)?(?:,\\d+(?:-\\d+)?)*)(?!\\d)");

  public static String replaceRankPlaceholders(String text, List<GameAccount> accounts) {
    if (text == null || text.isEmpty() || accounts == null || accounts.isEmpty()) return text;
    Matcher m = RANK_EXPR.matcher(text);
    StringBuffer sb = new StringBuffer();
    while (m.find()) {
      String expr = m.group(1);
      List<Integer> ranks = RankExpressionParser.parse(List.of(expr));
      String replacement = ranks.stream()
          .map(r -> {
            int idx = r - 1;
            if (idx >= 0 && idx < accounts.size()) {
              GameAccount acc = accounts.get(idx);
              return acc != null && acc.getAccountName() != null ? acc.getAccountName() : String.valueOf(r);
            }
            return String.valueOf(r);
          })
          .collect(Collectors.joining(","));
      // escape $ signs in replacement to avoid issues with Matcher.appendReplacement
      replacement = replacement.replace("$", "\\$");
      m.appendReplacement(sb, replacement);
    }
    m.appendTail(sb);
    return sb.toString();
  }
}

