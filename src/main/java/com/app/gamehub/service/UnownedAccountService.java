package com.app.gamehub.service;

import com.app.gamehub.entity.GameAccount;
import com.app.gamehub.repository.GameAccountRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UnownedAccountService {

    @Autowired
    private GameAccountRepository gameAccountRepository;

    /**
     * 根据联盟ID和查询字符串搜索无主账号
     * @param query 查询字符串
     * @param allianceId 联盟ID
     * @param limit 返回结果数量限制
     * @return 匹配的无主账号列表，按相似度排序
     */
    public List<GameAccount> searchUnownedAccounts(String query, Long allianceId, int limit) {
        if (query == null || query.trim().length() < 2) {
            return List.of();
        }

        String normalizedQuery = query.trim().toLowerCase();
        
        // 获取联盟中的所有无主账号
        List<GameAccount> unownedAccounts = gameAccountRepository.findUnownedAccountsByAlliance(allianceId);
        
        // 执行模糊匹配并按相似度排序
        return unownedAccounts.stream()
                .filter(account -> account.getAccountName() != null)
                .map(account -> new AccountWithSimilarity(account, calculateSimilarity(normalizedQuery, account.getAccountName().toLowerCase())))
                .filter(aws -> aws.similarity > 0.0)
                .sorted((a, b) -> Double.compare(b.similarity, a.similarity))
                .limit(limit)
                .map(aws -> aws.account)
                .collect(Collectors.toList());
    }

    /**
     * 计算两个字符串的相似度
     * 使用多种匹配策略：精确匹配、前缀匹配、包含匹配、编辑距离
     */
    private double calculateSimilarity(String input, String target) {
        if (input.equals(target)) {
            return 1.0; // 精确匹配
        }
        
        if (target.startsWith(input)) {
            return 0.9; // 前缀匹配
        }
        
        if (target.contains(input)) {
            return 0.7; // 包含匹配
        }
        
        // 计算编辑距离相似度
        int editDistance = calculateEditDistance(input, target);
        int maxLength = Math.max(input.length(), target.length());
        
        if (maxLength == 0) {
            return 1.0;
        }
        
        double similarity = 1.0 - (double) editDistance / maxLength;
        
        // 只返回相似度大于0.3的结果
        return similarity > 0.3 ? similarity : 0.0;
    }

    /**
     * 计算两个字符串的编辑距离（Levenshtein距离）
     */
    private int calculateEditDistance(String s1, String s2) {
        int m = s1.length();
        int n = s2.length();
        
        int[][] dp = new int[m + 1][n + 1];
        
        for (int i = 0; i <= m; i++) {
            dp[i][0] = i;
        }
        
        for (int j = 0; j <= n; j++) {
            dp[0][j] = j;
        }
        
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(Math.min(dp[i - 1][j], dp[i][j - 1]), dp[i - 1][j - 1]);
                }
            }
        }
        
        return dp[m][n];
    }

    /**
     * 内部类：账号与相似度的包装类
     */
    private static class AccountWithSimilarity {
        final GameAccount account;
        final double similarity;
        
        AccountWithSimilarity(GameAccount account, double similarity) {
            this.account = account;
            this.similarity = similarity;
        }
    }
}