package com.app.gamehub.task;

import com.app.gamehub.entity.Dynasty;
import com.app.gamehub.entity.DynastyPosition;
import com.app.gamehub.entity.PositionReservation;
import com.app.gamehub.entity.Alliance;
import com.app.gamehub.entity.WarType;
import com.app.gamehub.repository.DynastyPositionRepository;
import com.app.gamehub.repository.DynastyRepository;
import com.app.gamehub.repository.PositionReservationRepository;
import com.app.gamehub.repository.AllianceRepository;
import com.app.gamehub.repository.WarApplicationRepository;
import com.app.gamehub.repository.WarArrangementRepository;
import com.app.gamehub.repository.WarGroupRepository;
import com.app.gamehub.service.ArenaNotificationService;
import com.app.gamehub.service.CarriageQueueService;
import com.app.gamehub.service.PositionReservationService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class SystemTask {

  private final PositionReservationService positionReservationService;
  private final DynastyPositionRepository dynastyPositionRepository;
  private final ArenaNotificationService arenaNotificationService;
  private final PositionReservationRepository positionReservationRepository;
  private final CarriageQueueService carriageQueueService;
  private final DynastyRepository dynastyRepository;
  private final AllianceRepository allianceRepository;
  private final WarApplicationRepository warApplicationRepository;
  private final WarArrangementRepository warArrangementRepository;
  private final WarGroupRepository warGroupRepository;

  /** 定时自动配置官职预约设置 - 每天凌晨3:00执行 */
  @Transactional
  @Scheduled(cron = "0 0 3 * * ?")
  public void autoConfigurePositionReservation() {
    try {
      log.info("开始执行自动配置官职预约设置任务");
      LocalDate today = LocalDate.now();
      LocalDate yesterday = today.minusDays(1);

      // 1. 查找开启了官职预约且开启了自动配置的王朝
      List<Dynasty> dynasties = dynastyRepository.findByReservationEnabledTrueAndAutoConfigureReservationTrue();

      for (Dynasty dynasty : dynasties) {
        // 2. 获取该王朝的所有官职配置
        List<DynastyPosition> positions =
            dynastyPositionRepository.findByDynastyId(dynasty.getId());

        for (DynastyPosition position : positions) {
          // 3. 如果当天的前一天是官职预约结束时间
          if (position.getReservationEndTime() != null
              && position.getReservationEndTime().toLocalDate().equals(yesterday)) {
            // 4. 将任职日期、预约开始时间、预约结束时间都加一天
            if (position.getDutyDate() != null) {
              position.setDutyDate(position.getDutyDate().plusDays(1));
            }
            if (position.getReservationStartTime() != null) {
              position.setReservationStartTime(position.getReservationStartTime().plusDays(1));
            }
            if (position.getReservationEndTime() != null) {
              position.setReservationEndTime(position.getReservationEndTime().plusDays(1));
            }
            dynastyPositionRepository.save(position);
            log.info(
                "王朝 {} ({}) 的官职 {} 配置已自动顺延一天",
                dynasty.getName(),
                dynasty.getId(),
                position.getPositionType());
          }
        }
      }
      log.info("自动配置官职预约设置任务执行完成");
    } catch (Exception e) {
      log.error("执行自动配置官职预约设置任务时发生异常", e);
    }
  }

  /** 定时清理已结束预约活动的锁 每小时执行一次，清理结束时间在当前时间之前的预约活动对应的锁 */
  @Scheduled(cron = "0 0 * * * ?")
  public void cleanPositionReservationLock() {
    try {
      LocalDateTime now = LocalDateTime.now();

      // 查找已结束的预约活动（结束时间在当前时间之前）
      List<DynastyPosition> endedPositions = dynastyPositionRepository.findByReservationEndTimeBefore(now);

      if (endedPositions.isEmpty()) {
        log.debug("没有找到需要清理锁的已结束预约活动");
        return;
      }

      Set<Long> dynastyIds =
          endedPositions.stream().map(DynastyPosition::getDynastyId).collect(Collectors.toSet());

      log.info("开始清理 {} 个王朝的预约锁，王朝ID: {}", dynastyIds.size(), dynastyIds);

      int removedCount = positionReservationService.removeLock(dynastyIds);

      log.info("成功清理了 {} 个预约锁", removedCount);

    } catch (Exception e) {
      log.error("清理预约锁时发生异常", e);
    }
  }

  /** 定时清理王朝官职预约结果 */
  @Transactional
  @Scheduled(cron = "0 0 3 * * ?")
  public void cleanDynastyPosition() {
    try {
      LocalDate localDate = LocalDate.now();
      List<PositionReservation> endedPositions =
          positionReservationRepository.findByDutyDateBefore(localDate.minusDays(3));
      if (!endedPositions.isEmpty()) {
        positionReservationRepository.deleteAll(endedPositions);
        log.info("成功清理了 {} 个王朝官职预约结果", endedPositions.size());
      }
    } catch (Exception e) {
      log.error("清理王朝官职预约结果时发生异常", e);
    }
  }

  /** 定时发送演武场通知 - 每天晚上21:55发送 */
  @Scheduled(cron = "0 55 21 * * ?")
  public void sendArenaNotifications() {
    try {
      log.info("开始执行演武场通知定时任务");
      arenaNotificationService.sendArenaNotifications();
      log.info("演武场通知定时任务执行完成");
    } catch (Exception e) {
      log.error("执行演武场通知定时任务时发生异常", e);
    }
  }

  /** 定时检查并分配马车车主 - 每天30分钟执行一次 */
  @Transactional
  @Scheduled(cron = "0 0/30 0-5 * * ?")
  @Scheduled(cron = "0 0 6 * * ?")
  public void checkAndAssignCarriageDriver() {
    try {
      log.info("开始执行马车车主检查任务");
      carriageQueueService.checkAndAssignTodayDriver();
      log.info("马车车主检查任务执行完成");
    } catch (Exception e) {
      log.error("执行马车车主检查任务时发生异常", e);
    }
  }

  /** 定时清空官渡安排和人员报名信息 - 每天00:00执行 */
  @Transactional
  @Scheduled(cron = "0 0 0 * * ?")
  public void clearGuanduWarData() {
    try {
      log.info("开始执行清空官渡安排和人员报名信息任务");
      
      java.time.LocalDateTime now = java.time.LocalDateTime.now();
      int currentDayOfWeek = now.getDayOfWeek().getValue(); // 1=星期一, 7=星期日
      
      // 查找设置了官渡报名起始时间为当天的联盟
      List<Alliance> alliances = allianceRepository.findByGuanduRegistrationStartDay(currentDayOfWeek);
      
      for (Alliance alliance : alliances) {
        log.info("清空联盟 {} ({}) 的官渡安排和人员报名信息", alliance.getName(), alliance.getId());
        
        // 删除官渡战事分组
        warGroupRepository.deleteByAllianceIdAndWarTypeIn(alliance.getId(), 
            List.of(WarType.GUANDU_ONE, WarType.GUANDU_TWO));
        
        // 删除官渡战事安排
        warArrangementRepository.deleteByAllianceIdAndWarTypeIn(alliance.getId(), 
            List.of(WarType.GUANDU_ONE, WarType.GUANDU_TWO));
        
        // 删除官渡战事申请
        warApplicationRepository.deleteByAllianceIdAndWarTypeIn(alliance.getId(), 
            List.of(WarType.GUANDU_ONE, WarType.GUANDU_TWO));
        
        log.info("联盟 {} 的官渡数据清空完成", alliance.getId());
      }
      
      log.info("清空官渡安排和人员报名信息任务执行完成，共处理 {} 个联盟", alliances.size());
    } catch (Exception e) {
      log.error("执行清空官渡安排和人员报名信息任务时发生异常", e);
    }
  }
}
