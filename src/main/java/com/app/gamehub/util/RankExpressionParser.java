package com.app.gamehub.util;

import java.util.ArrayList;
import java.util.List;

// Parse rank expressions like "1", "1-3", "5,7,9", "1,3-5" into integer 1-based ranks
public class RankExpressionParser {
  public static List<Integer> parse(List<String> expressions) {
    List<Integer> result = new ArrayList<>();
    if (expressions == null) return result;
    for (String expr : expressions) {
      if (expr == null) continue;
      String[] parts = expr.split(",");
      for (String p : parts) {
        p = p.trim();
        if (p.isEmpty()) continue;
        if (p.contains("-")) {
          String[] range = p.split("-");
          try {
            int start = Integer.parseInt(range[0].trim());
            int end = Integer.parseInt(range[1].trim());
            if (start <= end) {
              for (int i = start; i <= end; i++) result.add(i);
            } else {
              for (int i = start; i >= end; i--) result.add(i);
            }
          } catch (NumberFormatException ignored) {
          }
        } else {
          try {
            result.add(Integer.parseInt(p));
          } catch (NumberFormatException ignored) {
          }
        }
      }
    }
    return result;
  }
}

