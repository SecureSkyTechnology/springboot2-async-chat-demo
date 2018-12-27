package com.secureskytech.demochatasync.service;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import akka.actor.ActorSystem;

@Service
public class SpringManagedActorSystem {
    protected static final Logger LOG = LoggerFactory.getLogger(SpringManagedActorSystem.class);

    final ActorSystem system = ActorSystem.create();

    SpringManagedActorSystem() {
        LOG.info("spring-managed actor-system initialized.");
    }

    public ActorSystem getActorSystem() {
        return this.system;
    }

    @PreDestroy
    public void shutdown() {
        system.terminate();
        LOG.info("spring-managed actor-system terminated.");
    }
}
