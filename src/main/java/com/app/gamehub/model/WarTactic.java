package com.app.gamehub.model;

import com.app.gamehub.dto.TacticTemplateConfig;
import com.app.gamehub.entity.GameAccount;
import com.app.gamehub.entity.WarArrangement;
import com.app.gamehub.entity.WarGroup;
import com.app.gamehub.entity.WarType;
import com.app.gamehub.exception.BusinessException;
import com.app.gamehub.repository.TacticTemplateRepository;
import com.app.gamehub.util.RankExpressionParser;
import com.app.gamehub.util.SpringContextUtils;
import com.app.gamehub.util.TacticTemplateUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.util.CollectionUtils;

@Getter
@RequiredArgsConstructor
public enum WarTactic {
  /** 战术一：参见 WarTactic.md */
  TACTIC_ONE(Set.of(WarType.GUANDU_ONE, WarType.GUANDU_TWO)) {
    @Override
    protected List<TacticalArrangement> arrangementSorted(List<GameAccount> accounts) {
      List<TacticalArrangement> arrangements = new ArrayList<>();

      // 初始化6个小组
      for (int i = 0; i < 6; i++) {
        TacticalArrangement arrangement = new TacticalArrangement();
        WarGroup warGroup = new WarGroup();
        arrangement.setWarGroup(warGroup);
        arrangement.setWarArrangements(new ArrayList<>());
        arrangements.add(arrangement);
      }

      // 按照 WarTactic.md 中战术一的成员加成排名（1-based）
      int[][] groupMembers = {
        {1, 4, 8, 15, 16, 21, 23}, // 兵器坊（车头：1）
        {2, 5, 10, 17, 18, 22, 24}, // 工匠坊（车头：2）
        {3, 11, 14, 19, 25}, // 对方上路小粮仓（车头：3）
        {6, 12, 13, 28}, // 我方上路小粮仓（车头：6）
        {7, 20, 27, 29}, // 我方下路小粮仓（车头：7）
        {9, 26, 30} // 我方中路小粮仓（车头：9）
      };

      // 标记已分配账号，避免重复分配
      boolean[] assignedFlag = new boolean[accounts.size()];
      for (int i = 0; i < groupMembers.length; i++) {
        int[] members = groupMembers[i];
        TacticalArrangement arrangement = arrangements.get(i);
        for (int rank : members) {
          int idx = rank - 1;
          if (idx >= 0 && idx < accounts.size() && !assignedFlag[idx]) {
            GameAccount account = accounts.get(idx);
            WarArrangement warArrangement = new WarArrangement();
            warArrangement.setAccountId(account.getId());
            warArrangement.setAccount(account);
            arrangement.getWarArrangements().add(warArrangement);
            assignedFlag[idx] = true;
          }
        }
      }

      // 确保所有账号都被分配一次：如果有未被分配的账号，按轮询依次补入各组
      {
        // reuse assignedFlag from above to find unassigned accounts
        int groupCount = arrangements.size();
        int nextGroup = 0;
        for (int i = 0; i < accounts.size(); i++) {
          if (i >= assignedFlag.length || !assignedFlag[i]) {
            GameAccount acc = accounts.get(i);
            WarArrangement warArrangement = new WarArrangement();
            warArrangement.setAccountId(acc.getId());
            warArrangement.setAccount(acc);
            arrangements.get(nextGroup % groupCount).getWarArrangements().add(warArrangement);
            nextGroup++;
          }
        }
      }

      // 构建任务文本（按照 WarTactic.md 中的描述，用账号名替换排名数字）
      for (int i = 0; i < arrangements.size(); i++) {
        TacticalArrangement arrangement = arrangements.get(i);
        if (arrangement.getWarArrangements().isEmpty()) continue;

        // 组长为该组第一个成员（按加成排名顺序）
        GameAccount leader = arrangement.getWarArrangements().get(0).getAccount();
        String groupNamePrefix;
        switch (i) {
          case 0:
            groupNamePrefix = "兵器坊（车头：" + leader.getAccountName() + "）";
            break;
          case 1:
            groupNamePrefix = "工匠坊（车头：" + leader.getAccountName() + "）";
            break;
          case 2:
            groupNamePrefix = "对方上路小粮仓（车头：" + leader.getAccountName() + "）";
            break;
          case 3:
            groupNamePrefix = "我方上路小粮仓（车头：" + leader.getAccountName() + "）";
            break;
          case 4:
            groupNamePrefix = "我方下路小粮仓（车头：" + leader.getAccountName() + "）";
            break;
          case 5:
            groupNamePrefix = "我方中路小粮仓（车头：" + leader.getAccountName() + "）";
            break;
          default:
            groupNamePrefix = leader.getAccountName() + "的小分队";
            break;
        }

        arrangement.getWarGroup().setGroupName(groupNamePrefix);

        // 根据 md 的任务描述构建具体任务，使用可能存在的账号名替换排名
        String task;
        switch (i) {
          case 0:
            {
              List<String> parts = new ArrayList<>();
              String wuchao = namesForRanks(accounts, new int[] {4});
              if (!wuchao.isEmpty()) {
                parts.add(wuchao + " 去乌巢");
              }
              parts.add("官渡开放后：所有成员抢官渡");
              task = String.join("，", parts);
            }
            break;
          case 1:
            {
              List<String> parts = new ArrayList<>();
              String wuchao = namesForRanks(accounts, new int[] {5});
              if (!wuchao.isEmpty()) {
                parts.add(wuchao + " 去乌巢");
              }
              List<String> after = new ArrayList<>();
              String pilei = namesForRanks(accounts, new int[] {2, 5, 10});
              if (!pilei.isEmpty()) after.add(pilei + " 去霹雳车");
              String liyang = namesForRanks(accounts, new int[] {17});
              if (!liyang.isEmpty()) after.add(liyang + " 去黎阳");
              String aocang = namesForRanks(accounts, new int[] {18});
              if (!aocang.isEmpty()) after.add(aocang + " 去敖仓");
              if (!after.isEmpty()) parts.add("官渡开放后：" + String.join("，", after));
              task = parts.isEmpty() ? "官渡开放后：待分配" : String.join("，", parts);
            }
            break;
          case 2:
            {
              List<String> parts = new ArrayList<>();
              String wuchao = namesForRanks(accounts, new int[] {3});
              if (!wuchao.isEmpty()) {
                parts.add(wuchao + " 去乌巢");
              }
              List<String> after = new ArrayList<>();
              String liyang = namesForRanks(accounts, new int[] {3, 11, 14});
              if (!liyang.isEmpty()) after.add(liyang + " 去黎阳");
              String aocang = namesForRanks(accounts, new int[] {25});
              if (!aocang.isEmpty()) after.add(aocang + " 去敖仓");
              if (!after.isEmpty()) parts.add("官渡开放后：" + String.join("，", after));
              task = parts.isEmpty() ? "官渡开放后：待分配" : String.join("，", parts);
            }
            break;
          case 3:
            {
              List<String> after = new ArrayList<>();
              String aocang = namesForRanks(accounts, new int[] {6, 12});
              if (!aocang.isEmpty()) after.add(aocang + " 去敖仓");
              String weapon = namesForRanks(accounts, new int[] {28});
              if (!weapon.isEmpty()) after.add(weapon + " 去兵器坊");
              task =
                  after.isEmpty()
                      ? "官渡开放后：待分配"
                      : "官渡开放后：" + String.join("，", after);
            }
            break;
          case 4:
            {
              List<String> after = new ArrayList<>();
              String weapon = namesForRanks(accounts, new int[] {7, 20});
              if (!weapon.isEmpty()) after.add(weapon + " 去兵器坊");
              String craft = namesForRanks(accounts, new int[] {29});
              if (!craft.isEmpty()) after.add(craft + " 去工匠坊");
              task =
                  after.isEmpty()
                      ? "官渡开放后：待分配"
                      : "官渡开放后：" + String.join("，", after);
            }
            break;
          case 5:
            {
              String craft = namesForRanks(accounts, new int[] {9, 26});
              task =
                  craft.isEmpty() ? "官渡开放后：待分配" : "官渡开放后：" + craft + " 去工匠坊";
            }
            break;
          default:
            task = "官渡开放后：待分配";
        }

        arrangement.getWarGroup().setGroupTask(task);
      }

      return arrangements;
    }
  },

  /** 战术二：参见 WarTactic.md */
  TACTIC_TWO(Set.of(WarType.GUANDU_ONE, WarType.GUANDU_TWO)) {
    @Override
    protected List<TacticalArrangement> arrangementSorted(List<GameAccount> accounts) {
      // Try DB-driven template first
      List<TacticalArrangement> templateResult = tryTemplateLoad(accounts, this.name());
      if (templateResult != null) return templateResult;

      // Fallback to existing hardcoded logic
      List<TacticalArrangement> arrangements = new ArrayList<>();

      // 初始化6个小组
      for (int i = 0; i < 6; i++) {
        TacticalArrangement arrangement = new TacticalArrangement();
        WarGroup warGroup = new WarGroup();
        arrangement.setWarGroup(warGroup);
        arrangement.setWarArrangements(new ArrayList<>());
        arrangements.add(arrangement);
      }

      int[][] groupMembers = {
        {1, 3, 8, 15, 16, 21, 23}, // 兵器坊（车头：1） (removed duplicate rank 6)
        {2, 4, 10, 17, 18, 22, 24}, // 工匠坊（车头：2）
        {5, 11, 14, 19, 25}, // 对方上路小粮仓（车头：5）
        {6, 26, 27, 30}, // 对方下路小粮仓（车头：6）
        {7, 12, 13, 20}, // 我方上路小粮仓（车头：7）
        {9, 28, 29} // 我方下路小粮仓（车头：9)
      };

      // 标记已分配账号，避免重复分配
      boolean[] assignedFlag = new boolean[accounts.size()];
      for (int i = 0; i < groupMembers.length; i++) {
        int[] members = groupMembers[i];
        TacticalArrangement arrangement = arrangements.get(i);
        for (int rank : members) {
          int idx = rank - 1;
          if (idx >= 0 && idx < accounts.size() && !assignedFlag[idx]) {
            GameAccount account = accounts.get(idx);
            WarArrangement warArrangement = new WarArrangement();
            warArrangement.setAccountId(account.getId());
            warArrangement.setAccount(account);
            arrangement.getWarArrangements().add(warArrangement);
            assignedFlag[idx] = true;
          }
        }
      }

      // 确保所有账号都被分配一次：如果有未被分配的账号，按轮询依次补入各组
      {
        // reuse assignedFlag from above to find unassigned accounts
        int groupCount = arrangements.size();
        int nextGroup = 0;
        for (int i = 0; i < accounts.size(); i++) {
          if (i >= assignedFlag.length || !assignedFlag[i]) {
            GameAccount acc = accounts.get(i);
            WarArrangement warArrangement = new WarArrangement();
            warArrangement.setAccountId(acc.getId());
            warArrangement.setAccount(acc);
            arrangements.get(nextGroup % groupCount).getWarArrangements().add(warArrangement);
            nextGroup++;
          }
        }
      }

      for (int i = 0; i < arrangements.size(); i++) {
        TacticalArrangement arrangement = arrangements.get(i);
        if (arrangement.getWarArrangements().isEmpty()) continue;

        GameAccount leader = arrangement.getWarArrangements().get(0).getAccount();
        String groupNamePrefix;
        switch (i) {
          case 0:
            groupNamePrefix = "兵器坊（车头：" + leader.getAccountName() + "）";
            break;
          case 1:
            groupNamePrefix = "工匠坊（车头：" + leader.getAccountName() + "）";
            break;
          case 2:
            groupNamePrefix = "对方上路小粮仓（车头：" + leader.getAccountName() + "）";
            break;
          case 3:
            groupNamePrefix = "对方下路小粮仓（车头：" + leader.getAccountName() + "）";
            break;
          case 4:
            groupNamePrefix = "我方上路小粮仓（车头：" + leader.getAccountName() + "）";
            break;
          case 5:
            groupNamePrefix = "我方下路小粮仓（车头：" + leader.getAccountName() + "）";
            break;
          default:
            groupNamePrefix = leader.getAccountName() + "的小分队";
            break;
        }

        arrangement.getWarGroup().setGroupName(groupNamePrefix);

        String task;
        switch (i) {
          case 0:
            task = "官渡开放后：所有成员抢官渡";
            break;
          case 1:
            {
              String pilei = namesForRanks(accounts, new int[] {2, 4, 10, 17, 18});
              task =
                  pilei.isEmpty()
                      ? "官渡开放后：待分配"
                      : "官渡开放后：" + pilei + " 抢霹雳车";
            }
            break;
          case 2:
            {
              List<String> after = new ArrayList<>();
              String liyang = namesForRanks(accounts, new int[] {5, 11, 14});
              if (!liyang.isEmpty()) after.add(liyang + " 抢黎阳");
              String aocang = namesForRanks(accounts, new int[] {25});
              if (!aocang.isEmpty()) after.add(aocang + " 抢敖仓");
              task =
                  after.isEmpty()
                      ? "官渡开放后：待分配"
                      : "官渡开放后：" + String.join("，", after);
            }
            break;
          case 3:
            {
              String craft = namesForRanks(accounts, new int[] {6, 26});
              task =
                  craft.isEmpty()
                      ? "官渡开放后：待分配"
                      : "官渡开放后：" + craft + " 去工匠坊";
            }
            break;
          case 4:
            {
              List<String> after = new ArrayList<>();
              String aocang = namesForRanks(accounts, new int[] {7, 12});
              if (!aocang.isEmpty()) after.add(aocang + " 抢敖仓");
              String weapon = namesForRanks(accounts, new int[] {13});
              if (!weapon.isEmpty()) after.add(weapon + " 去兵器坊");
              task =
                  after.isEmpty()
                      ? "官渡开放后：待分配"
                      : "官渡开放后：" + String.join("，", after);
            }
            break;
          case 5:
            {
              List<String> after = new ArrayList<>();
              String weapon = namesForRanks(accounts, new int[] {9, 28});
              if (!weapon.isEmpty()) after.add(weapon + " 去兵器坊");
              String craft = namesForRanks(accounts, new int[] {29});
              if (!craft.isEmpty()) after.add(craft + " 去工匠坊");
              task =
                  after.isEmpty()
                      ? "官渡开放后：待分配"
                      : "官渡开放后：" + String.join("，", after);
            }
            break;
          default:
            task = "官渡开放后：待分配";
        }

        arrangement.getWarGroup().setGroupTask(task);
      }

      return arrangements;
    }

    // Try load a tactic template from DB and build arrangement; returns null if not found or failed
    private List<TacticalArrangement> tryTemplateLoad(
        List<GameAccount> accounts, String tacticKey) {
      try {
        TacticTemplateRepository repo = SpringContextUtils.getBean(TacticTemplateRepository.class);
        if (repo == null) return null;
        return repo.findByTacticKey(tacticKey)
            .map(
                t -> {
                  try {
                    ObjectMapper om = SpringContextUtils.getBean(ObjectMapper.class);
                    if (om == null) om = new ObjectMapper();
                    TacticTemplateConfig cfg =
                        om.readValue(t.getConfigJson(), TacticTemplateConfig.class);
                    if (cfg == null || cfg.getGroups() == null) return null;

                    List<TacticalArrangement> arrangements = new ArrayList<>();
                    for (TacticTemplateConfig.GroupConfig g : cfg.getGroups()) {
                      TacticalArrangement ta = new TacticalArrangement();
                      WarGroup wg = new WarGroup();
                      // Replace rank placeholders in name/task using actual account names
                      String groupName =
                          TacticTemplateUtils.replaceRankPlaceholders(g.getName(), accounts);
                      String groupTask =
                          TacticTemplateUtils.replaceRankPlaceholders(g.getTask(), accounts);
                      wg.setGroupName(groupName);
                      wg.setGroupTask(groupTask);
                      ta.setWarGroup(wg);
                      ta.setWarArrangements(new ArrayList<>());
                      List<Integer> ranks = RankExpressionParser.parse(g.getRanks());
                      for (int rank : ranks) {
                        int idx = rank - 1;
                        if (idx >= 0 && idx < accounts.size()) {
                          GameAccount acc = accounts.get(idx);
                          WarArrangement wa = new WarArrangement();
                          wa.setAccountId(acc.getId());
                          wa.setAccount(acc);
                          ta.getWarArrangements().add(wa);
                        }
                      }
                      arrangements.add(ta);
                    }

                    // fill remaining accounts not assigned
                    boolean[] assigned = new boolean[accounts.size()];
                    for (TacticalArrangement a : arrangements) {
                      for (WarArrangement wa : a.getWarArrangements()) {
                        if (wa.getAccount() != null && wa.getAccount().getId() != null) {
                          long id = wa.getAccount().getId();
                          if (id >= 1 && id <= accounts.size()) assigned[(int) id - 1] = true;
                        }
                      }
                    }
                    int groupCount = arrangements.size() == 0 ? 1 : arrangements.size();
                    int nextGroup = 0;
                    for (int i = 0; i < accounts.size(); i++) {
                      if (!assigned[i]) {
                        GameAccount acc = accounts.get(i);
                        WarArrangement wa = new WarArrangement();
                        wa.setAccountId(acc.getId());
                        wa.setAccount(acc);
                        arrangements.get(nextGroup % groupCount).getWarArrangements().add(wa);
                        nextGroup++;
                      }
                    }

                    // ensure group names include leader name if empty
                    for (TacticalArrangement a : arrangements) {
                      if (a.getWarGroup().getGroupName() == null
                          || a.getWarGroup().getGroupName().isEmpty()) {
                        if (!a.getWarArrangements().isEmpty()
                            && a.getWarArrangements().get(0).getAccount() != null) {
                          a.getWarGroup()
                              .setGroupName(
                                  a.getWarArrangements().get(0).getAccount().getAccountName()
                                      + "的小分队");
                        }
                      }
                      if (a.getWarGroup().getGroupTask() == null)
                        a.getWarGroup().setGroupTask("官渡开放后：待分配");
                    }

                    return arrangements;
                  } catch (Exception ex) {
                    return null;
                  }
                })
            .orElse(null);
      } catch (Exception ex) {
        return null;
      }
    }
  };

  /** 战术支持的战事 */
  private final Set<WarType> supportedWarTypes;

  // helper: 将排名数组映射为账号名的逗号分隔字符串（若排名超出可用账号，则保留数字文本）
  private static String namesForRanks(List<GameAccount> accounts, int[] ranks) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < ranks.length; i++) {
      int rank = ranks[i];
      int idx = rank - 1;
      if (idx >= 0 && idx < accounts.size()) {
        GameAccount acc = accounts.get(idx);
        String name =
            acc != null && acc.getAccountName() != null ? acc.getAccountName() : null;
        if (name != null && !name.isEmpty()) {
          if (sb.length() > 0) sb.append(",");
          sb.append(name);
        }
      }
    }
    return sb.toString();
  }

  public List<TacticalArrangement> arrangement(List<GameAccount> accounts) {
    if (CollectionUtils.isEmpty(accounts)) {
      return new ArrayList<>();
    }
    // 注意：对于官渡战事，账号列表已经在 WarService 中根据申请时间排序并限制为30个
    // 对于非官渡战事，仍然按伤害加成排序
    boolean isGuanDu =
        !accounts.isEmpty()
            && (supportedWarTypes.contains(WarType.GUANDU_ONE)
                || supportedWarTypes.contains(WarType.GUANDU_TWO));
    if (isGuanDu && accounts.size() > 30) {
      accounts = accounts.subList(0, 30);
    }

    return arrangementSorted(accounts);
  }

  protected List<TacticalArrangement> arrangementSorted(List<GameAccount> accounts) {
    throw new BusinessException("该战术暂未支持");
  }
}
