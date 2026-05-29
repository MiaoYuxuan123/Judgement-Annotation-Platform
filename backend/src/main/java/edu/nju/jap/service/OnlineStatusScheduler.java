package edu.nju.jap.service;

import edu.nju.jap.mapper.SysUserMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OnlineStatusScheduler {
    private final SysUserMapper sysUserMapper;

    public OnlineStatusScheduler(SysUserMapper sysUserMapper) {
        this.sysUserMapper = sysUserMapper;
    }

    @Scheduled(fixedDelay = 30_000)
    public void markIdleUsersOffline() {
        sysUserMapper.updateOfflineByTimeout();
    }
}